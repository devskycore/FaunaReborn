package io.github.devskycore.faunareborn.animal.chicken.hostility;

import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent;
import io.github.devskycore.faunareborn.animal.chicken.config.ChickenHostilitySettings;
import io.github.devskycore.faunareborn.combat.deathmessage.HostileSpecies;
import io.github.devskycore.faunareborn.combat.deathmessage.HostilityCause;
import io.github.devskycore.faunareborn.combat.deathmessage.HostilityContextTracker;
import io.github.devskycore.faunareborn.core.FaunaRebornPlugin;
import io.github.devskycore.faunareborn.system.platform.RuntimePlatform;
import io.github.devskycore.faunareborn.system.scheduler.SchedulerAdapter;
import io.github.devskycore.faunareborn.system.scheduler.SchedulerAdapters;
import io.github.devskycore.faunareborn.system.scheduler.TaskHandle;
import io.github.devskycore.faunareborn.targeting.TargetEligibilityService;
import io.github.devskycore.faunareborn.targeting.TargetScoringService;
import io.github.devskycore.faunareborn.system.lod.LodResolver;
import io.github.devskycore.faunareborn.system.lod.LodSettings;
import io.github.devskycore.faunareborn.system.lod.LodTier;
import io.papermc.paper.event.world.WorldDifficultyChangeEvent;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.bukkit.Difficulty;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Chicken;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.block.BlockCookEvent;
import org.bukkit.event.inventory.FurnaceExtractEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.UUID;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

final class ChickenHostilityTask implements Listener {

    private static final long TICK_RATE = 1L;
    private static final long ALERT_DURATION_TICKS = 0L;
    private static final long ELIGIBILITY_CACHE_TICKS = 10L;
    private static final long TARGET_SEARCH_IDLE_INTERVAL_TICKS = 4L;
    private static final long TARGET_SEARCH_CHASE_INTERVAL_TICKS = 1L;
    private static final long TARGET_SEARCH_FAILURE_COOLDOWN_TICKS = 8L;
    private static final int MAX_IDLE_BUCKETS = 20;
    private static final long ACTIVE_TICK_INTERVAL = 1L;
    private static final long INVALID_CLEANUP_INTERVAL_TICKS = 20L;
    private static final int INVALID_CLEANUP_BATCH_SIZE = 32;
    private static final long PICKUP_COUNTER_CLEANUP_INTERVAL_TICKS = 100L;
    private static final long COOK_VALIDATION_WINDOW_TICKS = 20L * 180L;
    private static final Material RAW_MEAT = Material.CHICKEN;
    private static final Material COOKED_MEAT = Material.COOKED_CHICKEN;

    private final FaunaRebornPlugin plugin;
    private final SchedulerAdapter scheduler;
    private final ChickenTracker tracker = new ChickenTracker();
    private final ActivationPolicy activationPolicy;
    private final TargetEligibilityService targetEligibilityService;
    private final ChickenTargetingService targetingService;
    private final MovementController movementController;
    private final SocialAlertService socialAlertService;
    private final TerritorialPickupService territorialPickupService;
    private final ChickenDamageScaler damageScaler;
    private final ChickenHostilityVisualController visualController;
    private final WorldNightStateCache worldNightStateCache;
    private final Queue<Runnable> pendingStateMutations = new ConcurrentLinkedQueue<>();
    private final Map<UUID, PendingCookIntent> pendingCookIntentByPlayer = new HashMap<>();
    private final Map<String, CookedBatchReady> cookedBatchByCooker = new HashMap<>();
    private final Object stateLock = new Object();
    private final boolean folia;
    private final LodSettings lodSettings;
    private final Int2ObjectOpenHashMap<LodTier> lodTierByChickenId = new Int2ObjectOpenHashMap<>();

    private final int maxProcessedChickensPerTick;
    private final int attackCooldownTicks;
    private final int threatTimeoutTicks;
    private final int retargetGraceTicks;
    private final double processingRadius;

    private TaskHandle task;
    private long currentTick;
    ChickenHostilityTask(FaunaRebornPlugin plugin, ChickenHostilitySettings settings) {
        this.plugin = plugin;
        this.scheduler = SchedulerAdapters.create(plugin);
        this.folia = RuntimePlatform.isFolia();

        ChickenHostilitySettings.Combat combat = settings.combat();
        ChickenHostilitySettings.Limits limits = settings.limits();
        this.maxProcessedChickensPerTick = limits.maxProcessedChickensPerTick();
        this.attackCooldownTicks = combat.attackCooldownTicks();
        this.threatTimeoutTicks = combat.threatTimeoutTicks();
        this.retargetGraceTicks = combat.retargetGraceTicks();
        this.processingRadius = Math.max(combat.detectionRadius(), ChickenHostilityConstants.PLAYER_PROXIMITY_RADIUS);
        this.lodSettings = settings.lod();

        this.worldNightStateCache = new WorldNightStateCache(plugin, settings.environmentAggression());
        this.targetEligibilityService = new TargetEligibilityService(settings.targeting());
        this.activationPolicy = new ActivationPolicy(plugin, settings.activation(), settings.worldFilter(), targetEligibilityService);
        this.targetingService = new ChickenTargetingService(
                plugin,
                tracker,
                activationPolicy,
                targetEligibilityService,
                new TargetScoringService(settings.targeting().scoring()),
                combat,
                limits
        );
        this.movementController = new MovementController(combat, settings.movement());
        this.damageScaler = new ChickenDamageScaler(combat, settings.damageScaling(), worldNightStateCache);
        this.socialAlertService = new SocialAlertService(settings.socialAlert(), this::tryRecruitChicken);
        this.territorialPickupService = new TerritorialPickupService(
                settings.itemPickupTerritoriality(),
                targetingService.maxSimultaneousAttackersPerTarget(),
                worldNightStateCache,
                socialAlertService,
                this::tryRecruitChicken,
                activationPolicy::isInvalidTarget
        );
        this.visualController = new ChickenHostilityVisualController(
                settings.visuals().glowEnabled(),
                settings.visuals().particlesEnabled(),
                settings.visuals().particlesIntervalTicks(),
                settings.visuals().particlesVolume(),
                settings.visuals().soundEnabled(),
                settings.visuals().soundIntervalTicks(),
                settings.visuals().soundVolume()
        );
    }

    void start() {
        if (task != null) return;

        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        worldNightStateCache.start();
        trackLoadedChickens();
        task = scheduler.runAtFixedRate(this::tick, 1L, TICK_RATE);
    }

    void stop() {
        if (task != null) task.cancel();
        task = null;
        worldNightStateCache.stop();

        HandlerList.unregisterAll(this);

        visualController.clearAll(tracker.trackedChickens());
        lodTierByChickenId.clear();
        tracker.clear();
        activationPolicy.clear();
        targetEligibilityService.clearExpired(Long.MAX_VALUE);
        targetingService.clear();
        socialAlertService.clear();
        territorialPickupService.clear();
        pendingCookIntentByPlayer.clear();
        cookedBatchByCooker.clear();
        pendingStateMutations.clear();
        currentTick = 0L;
    }

    private void tick() {
        if (!folia) {
            tickLegacy();
            return;
        }

        List<Integer> toProcess = new ArrayList<>(maxProcessedChickensPerTick);
        long tickSnapshot;
        synchronized (stateLock) {
            drainPendingStateMutations();
            currentTick++;
            tickSnapshot = currentTick;
            targetEligibilityService.clearExpired(currentTick);
            targetingService.cleanupGlobalTargetCooldowns(currentTick);
            socialAlertService.cleanupCooldowns(currentTick);
            if (currentTick % PICKUP_COUNTER_CLEANUP_INTERVAL_TICKS == 0L) {
                territorialPickupService.cleanupCounters(currentTick);
            }
            if (currentTick % INVALID_CLEANUP_INTERVAL_TICKS == 0L) {
                cleanupInvalidTrackedChickensBatch();
            }

            int idleBucketModulo = resolveIdleBucketModulo();
            int processed = 0;
            int scanned = 0;
            int scanBudget = Math.max(maxProcessedChickensPerTick * 3, idleBucketModulo * 64);
            while (processed < maxProcessedChickensPerTick && scanned < scanBudget && tracker.hasTrackedChickens()) {
                int chickenId = tracker.nextProcessingChickenId();
                if (chickenId == Integer.MIN_VALUE) break;
                scanned++;

                Chicken chicken = tracker.chicken(chickenId);
                if (chicken == null) continue;
                ChickenHostilityBrain brain = tracker.brain(chickenId);
                if (brain == null || brain.state == ChickenHostilityState.IDLE) {
                    if (Math.floorMod(chickenId, idleBucketModulo) != Math.floorMod(currentTick, idleBucketModulo)) {
                        continue;
                    }
                } else {
                    if (currentTick < brain.nextProcessTick) continue;
                }
                toProcess.add(chickenId);
                processed++;
            }
        }

        for (int chickenId : toProcess) {
            Chicken chicken;
            synchronized (stateLock) {
                chicken = tracker.chicken(chickenId);
            }
            if (chicken == null) {
                continue;
            }
            scheduler.runForEntity(chicken, () -> processChickenFolia(chickenId, tickSnapshot));
        }
    }

    private void tickLegacy() {
        drainPendingStateMutations();
        currentTick++;
        targetEligibilityService.clearExpired(currentTick);
        targetingService.cleanupGlobalTargetCooldowns(currentTick);
        socialAlertService.cleanupCooldowns(currentTick);
        if (currentTick % PICKUP_COUNTER_CLEANUP_INTERVAL_TICKS == 0L) {
            territorialPickupService.cleanupCounters(currentTick);
        }
        if (currentTick % INVALID_CLEANUP_INTERVAL_TICKS == 0L) {
            cleanupInvalidTrackedChickensBatch();
        }
        int idleBucketModulo = resolveIdleBucketModulo();
        int processed = 0;
        int scanned = 0;
        int scanBudget = Math.max(maxProcessedChickensPerTick * 3, idleBucketModulo * 64);

        while (processed < maxProcessedChickensPerTick && scanned < scanBudget && tracker.hasTrackedChickens()) {
            int chickenId = tracker.nextProcessingChickenId();
            if (chickenId == Integer.MIN_VALUE) break;
            scanned++;

            Chicken chicken = tracker.chicken(chickenId);
            if (chicken == null) continue;
            if (!chicken.isValid() || chicken.isDead()) {
                removeTrackedChicken(chickenId, chicken);
                continue;
            }

            ChickenHostilityBrain brain = tracker.brain(chickenId);
            if (brain == null || brain.state == ChickenHostilityState.IDLE) {
                if (Math.floorMod(chickenId, idleBucketModulo) != Math.floorMod(currentTick, idleBucketModulo))
                    continue;
            } else {
                if (currentTick < brain.nextProcessTick) continue;
            }

            processChicken(chicken, chickenId, brain);
            visualController.sync(chickenId, chicken, tracker.brain(chickenId), currentTick, lodTierByChickenId.get(chickenId));
            processed++;
        }

        visualController.tick(currentTick, tracker.trackedChickens(), tracker.brains());
    }

    private void processChickenFolia(int chickenId, long tickSnapshot) {
        synchronized (stateLock) {
            if (currentTick != tickSnapshot) {
                return;
            }
            Chicken chicken = tracker.chicken(chickenId);
            if (chicken == null) {
                return;
            }
            if (!chicken.isValid() || chicken.isDead()) {
                removeTrackedChicken(chickenId, chicken);
                return;
            }
            ChickenHostilityBrain brain = tracker.brain(chickenId);
            processChicken(chicken, chickenId, brain);
            visualController.sync(chickenId, chicken, tracker.brain(chickenId), currentTick, lodTierByChickenId.get(chickenId));
        }
    }

    private void processChicken(Chicken chicken, int chickenId, ChickenHostilityBrain brain) {
        if (chicken.isDead() || !chicken.isValid()) {
            removeBrain(chickenId, chicken);
            return;
        }

        if (activationPolicy.isWorldDisallowed(chicken.getWorld())) {
            if (brain != null) {
                clearTargetAndIdle(chicken, brain);
            }
            return;
        }
        if (activationPolicy.isPeacefulWorld(chicken.getWorld())) {
            if (brain != null) {
                clearTargetAndIdle(chicken, brain);
            }
            return;
        }

        List<Entity> nearby = chicken.getNearbyEntities(processingRadius, processingRadius, processingRadius);

        if (activationPolicy.isActivationBlocked(chicken, nearby)) {
            if (brain != null) {
                clearTargetAndIdle(chicken, brain);
            }
            return;
        }

        if (brain == null) {
            if (!isEligiblePassive(chicken, nearby)) {
                return;
            }
            lodTierByChickenId.put(chickenId, LodTier.HIGH);
            handleIdle(chicken, null, nearby);
            return;
        }
        updateLod(chickenId, chicken, brain, nearby);
        boolean requireBabyNearby = !brain.socialAlertOverrideEligibility;
        if (isIneligible(chicken, brain, nearby, requireBabyNearby, true)) {
            clearTargetAndIdle(chicken, brain);
            return;
        }

        switch (brain.state) {
            case IDLE -> handleIdle(chicken, brain, nearby);
            case ALERT -> handleAlert(chicken, brain);
            case CHASE -> handleChase(chicken, brain, nearby);
            case ATTACK -> handleAttack(chicken, brain);
        }
        applyNextProcessTick(brain, lodTierByChickenId.get(chickenId));
    }

    private void updateLod(int chickenId, Chicken chicken, ChickenHostilityBrain brain, List<Entity> nearby) {
        boolean forceHigh = brain.state != ChickenHostilityState.IDLE;
        double nearestPlayerDistanceSq = nearestPlayerDistanceSq(chicken, nearby, lodSettings.lowDistanceSq());
        LodTier nextTier = LodResolver.resolveTier(lodSettings, lodTierByChickenId.get(chickenId), nearestPlayerDistanceSq, forceHigh);
        lodTierByChickenId.put(chickenId, nextTier);
    }

    private static double nearestPlayerDistanceSq(Chicken chicken, List<Entity> nearby, double fallbackDistanceSq) {
        double best = fallbackDistanceSq + 1.0D;
        for (Entity entity : nearby) {
            if (!(entity instanceof Player player)) {
                continue;
            }
            double distanceSq = HostilityDistances.distanceSq(chicken, player);
            if (distanceSq < best) {
                best = distanceSq;
            }
        }
        return best;
    }

    private void applyNextProcessTick(ChickenHostilityBrain brain, LodTier tier) {
        if (brain == null) {
            return;
        }
        LodTier effectiveTier = tier == null ? LodTier.HIGH : tier;
        int interval = lodSettings.intervalFor(effectiveTier);
        brain.nextProcessTick = currentTick + Math.max(ACTIVE_TICK_INTERVAL, interval);
    }

    private boolean isIneligible(
            Chicken chicken,
            ChickenHostilityBrain brain,
            List<Entity> nearby,
            boolean requireBabyNearby,
            boolean requireAdult
    ) {
        if (requireBabyNearby && requireAdult && currentTick < brain.nextEligibilityRefreshTick) {
            return !brain.eligible;
        }

        boolean isAdult = chicken.isAdult();
        if (!requireBabyNearby) {
            brain.eligible = isAdult;
            brain.nextEligibilityRefreshTick = currentTick + ELIGIBILITY_CACHE_TICKS;
            return requireAdult && !isAdult;
        }

        boolean hasBabyNearby = hasBabyNearby(chicken, nearby);
        brain.eligible = isAdult && hasBabyNearby;
        brain.nextEligibilityRefreshTick = currentTick + ELIGIBILITY_CACHE_TICKS;

        if (requireAdult && !isAdult) {
            return true;
        }
        return !hasBabyNearby;
    }

    private void handleIdle(Chicken chicken, ChickenHostilityBrain brain, List<Entity> nearby) {
        if (brain == null) {
            Player target = targetingService.findTarget(chicken, null, nearby, chicken.getEntityId(), currentTick);
            if (target == null) {
                return;
            }
            ChickenHostilityBrain createdBrain = new ChickenHostilityBrain(currentTick);
            createdBrain.hostilityCause = HostilityCause.BABY_PROTECTION;
            setTarget(createdBrain, target);
            tracker.putBrain(chicken.getEntityId(), createdBrain);
            transition(chicken, createdBrain, ChickenHostilityState.ALERT);
            return;
        }
        if (currentTick < brain.nextTargetSearchTick) return;

        brain.nextTargetSearchTick = currentTick + TARGET_SEARCH_IDLE_INTERVAL_TICKS;

        Player target = targetingService.findTarget(chicken, brain, nearby, chicken.getEntityId(), currentTick);
        if (target == null) {
            brain.nextTargetSearchTick = currentTick + Math.max(TARGET_SEARCH_IDLE_INTERVAL_TICKS, TARGET_SEARCH_FAILURE_COOLDOWN_TICKS);
            return;
        }

        setTarget(brain, target);
        transition(chicken, brain, ChickenHostilityState.ALERT);
    }

    private boolean isEligiblePassive(Chicken chicken, List<Entity> nearby) {
        return chicken.isAdult() && hasBabyNearby(chicken, nearby);
    }

    private void handleAlert(Chicken chicken, ChickenHostilityBrain brain) {
        if (hasThreatTimedOut(brain)) {
            calmDown(chicken, brain);
            return;
        }

        Player target = targetingService.resolveTarget(chicken, brain.targetUuid);
        if (target == null) {
            clearTargetAndIdle(chicken, brain);
            return;
        }

        double distSq = HostilityDistances.distanceSq(chicken, target);
        if (distSq > ChickenHostilityConstants.CHASE_BREAK_RADIUS_SQ) {
            clearTargetAndIdle(chicken, brain);
            return;
        }

        if (currentTick - brain.stateStartedTick < ALERT_DURATION_TICKS) return;

        transition(chicken, brain, ChickenHostilityState.CHASE);
    }

    private void handleChase(Chicken chicken, ChickenHostilityBrain brain, List<Entity> nearby) {
        if (hasThreatTimedOut(brain)) {
            calmDown(chicken, brain);
            return;
        }

        Player target = targetingService.resolveTarget(chicken, brain.targetUuid);
        if (target == null) {
            if (currentTick >= brain.nextTargetSearchTick) {
                brain.nextTargetSearchTick = currentTick + TARGET_SEARCH_CHASE_INTERVAL_TICKS;
                Player reacquired = targetingService.findTarget(chicken, brain, nearby, chicken.getEntityId(), currentTick);
                if (reacquired != null) {
                    setTarget(brain, reacquired);
                    target = reacquired;
                } else {
                    brain.nextTargetSearchTick = currentTick + Math.max(TARGET_SEARCH_CHASE_INTERVAL_TICKS, TARGET_SEARCH_FAILURE_COOLDOWN_TICKS);
                }
            }

            if (target == null) {
                // Keep CHASE state briefly until the next search slot to avoid jittery target drops.
                return;
            }
        }

        double distSq = HostilityDistances.distanceSq(chicken, target);
        if (distSq > ChickenHostilityConstants.CHASE_BREAK_RADIUS_SQ) {
            clearTargetAndIdle(chicken, brain);
            return;
        }
        if (movementController.hasLostLineOfSight(chicken, target, brain, currentTick)) {
            clearTargetAndIdle(chicken, brain);
            return;
        }

        if (movementController.failsSimplePathing(chicken, target, brain, true)) {
            clearTargetAndIdle(chicken, brain);
            return;
        }

        movementController.move(chicken, target, brain, currentTick);
        if (distSq <= targetingService.attackRangeSq()) {
            transition(chicken, brain, ChickenHostilityState.ATTACK);
            attack(chicken, target, brain);
        }
    }

    private void handleAttack(Chicken chicken, ChickenHostilityBrain brain) {
        if (hasThreatTimedOut(brain)) {
            calmDown(chicken, brain);
            return;
        }

        Player target = targetingService.resolveTarget(chicken, brain.targetUuid);
        if (target == null) {
            transition(chicken, brain, ChickenHostilityState.CHASE);
            return;
        }

        double distSq = HostilityDistances.distanceSq(chicken, target);
        if (distSq > ChickenHostilityConstants.CHASE_BREAK_RADIUS_SQ) {
            clearTargetAndIdle(chicken, brain);
            return;
        }
        if (movementController.hasLostLineOfSight(chicken, target, brain, currentTick)) {
            clearTargetAndIdle(chicken, brain);
            return;
        }

        if (movementController.failsSimplePathing(chicken, target, brain, distSq > targetingService.attackRangeSq())) {
            clearTargetAndIdle(chicken, brain);
            return;
        }

        movementController.move(chicken, target, brain, currentTick);

        if (distSq > targetingService.attackRangeSq()) {
            transition(chicken, brain, ChickenHostilityState.CHASE);
            return;
        }

        attack(chicken, target, brain);
    }

    private void transition(Chicken chicken, ChickenHostilityBrain brain, ChickenHostilityState nextState) {
        if (brain.state == nextState) return;

        ChickenHostilityState previousState = brain.state;
        if (previousState == ChickenHostilityState.IDLE) {
            targetingService.registerActiveTracking(chicken, brain.targetUuid);
        } else if (nextState == ChickenHostilityState.IDLE) {
            targetingService.unregisterActiveTracking(chicken, brain.targetUuid);
        }

        brain.state = nextState;
        brain.stateStartedTick = currentTick;
        brain.nextProcessTick = currentTick;
        // Revalidate eligibility on next cycle when state changes.
        brain.nextEligibilityRefreshTick = currentTick;
    }

    private void clearTargetAndIdle(Chicken chicken, ChickenHostilityBrain brain) {
        targetingService.applyGlobalTargetCooldown(brain.targetUuid, currentTick);
        transition(chicken, brain, ChickenHostilityState.IDLE);
        brain.targetUuid = null;
        brain.socialAlertOverrideEligibility = false;
        brain.nextTargetSearchTick = currentTick;
        movementController.resetProgressTracking(brain);
    }

    private void calmDown(Chicken chicken, ChickenHostilityBrain brain) {
        UUID previousTargetUuid = brain.targetUuid;
        clearTargetAndIdle(chicken, brain);
        if (previousTargetUuid != null && retargetGraceTicks > 0) {
            brain.ignoreTargetUuid = previousTargetUuid;
            brain.ignoreTargetUntilTick = currentTick + retargetGraceTicks;
        }
    }

    private void setTarget(ChickenHostilityBrain brain, Player target) {
        setTarget(brain, target, threatTimeoutTicks);
    }

    private void setTarget(ChickenHostilityBrain brain, Player target, int aggressionDurationTicks) {
        UUID previousTarget = brain.targetUuid;
        UUID nextTarget = target.getUniqueId();
        if (!nextTarget.equals(previousTarget)) {
            movementController.resetProgressTracking(brain);
            if (brain.state != ChickenHostilityState.IDLE) {
                targetingService.replaceActiveTarget(previousTarget, nextTarget);
            }
        }
        brain.targetUuid = nextTarget;
        refreshThreat(brain, aggressionDurationTicks);
    }

    private void refreshThreat(ChickenHostilityBrain brain, int aggressionDurationTicks) {
        brain.lastThreatRefreshTick = currentTick;
        brain.threatExpiresTick = aggressionDurationTicks <= 0
                ? Long.MAX_VALUE
                : currentTick + aggressionDurationTicks;
    }

    private boolean hasThreatTimedOut(ChickenHostilityBrain brain) {
        return brain.threatExpiresTick != Long.MAX_VALUE && currentTick >= brain.threatExpiresTick;
    }

    private boolean hasBabyNearby(Chicken chicken, List<Entity> nearby) {
        for (Entity entity : nearby) {
            if (!(entity instanceof Chicken nearbyChicken)) continue;
            if (nearbyChicken.isAdult() || nearbyChicken.isDead()) continue;
            if (HostilityDistances.distanceSq2D(chicken, nearbyChicken) <= ChickenHostilityConstants.BABY_RADIUS_SQ) return true;
        }
        return false;
    }

    private void attack(Chicken chicken, Player player, ChickenHostilityBrain brain) {
        if (brain.lastAttackTick != Long.MIN_VALUE && currentTick - brain.lastAttackTick < attackCooldownTicks) return;

        HostilityContextTracker.record(player.getUniqueId(), HostileSpecies.CHICKEN, brain.hostilityCause);
        player.damage(damageScaler.resolveScaledDamage(chicken.getWorld()), chicken);
        brain.lastAttackTick = currentTick;
    }

    private void removeBrain(int chickenId, Chicken chicken) {
        ChickenHostilityBrain brain = tracker.brain(chickenId);
        Chicken trackedChicken = chicken != null ? chicken : tracker.chicken(chickenId);
        if (brain != null && brain.state != ChickenHostilityState.IDLE && trackedChicken != null) {
            targetingService.unregisterActiveTracking(trackedChicken, brain.targetUuid);
        }
        tracker.removeBrain(chickenId);
        activationPolicy.forget(chickenId);
        visualController.deactivate(chickenId, chicken);
        lodTierByChickenId.remove(chickenId);
    }

    private void enqueueStateMutation(Runnable mutation) {
        pendingStateMutations.offer(mutation);
    }

    private void drainPendingStateMutations() {
        Runnable mutation;
        while ((mutation = pendingStateMutations.poll()) != null) {
            mutation.run();
        }
    }

    private void cleanupInvalidTrackedChickensBatch() {
        tracker.prepareCleanupCursor();
        int checked = 0;
        while (checked < INVALID_CLEANUP_BATCH_SIZE && tracker.hasCleanupCandidate()) {
            int chickenId = tracker.nextCleanupChickenId();
            if (chickenId == Integer.MIN_VALUE) {
                return;
            }
            checked++;
            Chicken chicken = tracker.chicken(chickenId);
            if (chicken == null || !chicken.isValid() || chicken.isDead()) {
                removeTrackedChicken(chickenId, chicken);
            }
        }
    }

    private int resolveIdleBucketModulo() {
        int tracked = tracker.size();
        if (tracked <= 0) {
            return 1;
        }
        int idleTargetPerTick = Math.max(maxProcessedChickensPerTick * 2, 128);
        int computed = (int) Math.ceil((double) tracked / (double) idleTargetPerTick);
        return Math.clamp(computed, 1, MAX_IDLE_BUCKETS);
    }

    private void removeTrackedChicken(int chickenId, Chicken chicken) {
        removeBrain(chickenId, chicken);
        tracker.untrack(chickenId);
        socialAlertService.removeChicken(chickenId);
    }

    private boolean tryRecruitChicken(
            Chicken chicken,
            Player aggressor,
            int aggressionDurationTicks,
            boolean applyJoinCooldown,
            HostilityCause hostilityCause
    ) {
        if (chicken == null || !chicken.isValid() || chicken.isDead()) {
            return false;
        }
        if (activationPolicy.isInvalidTarget(chicken, aggressor)) {
            return false;
        }
        int chickenId = chicken.getEntityId();
        if (!tracker.isTracked(chickenId)) {
            trackChicken(chicken, false);
        }

        ChickenHostilityBrain brain = tracker.brain(chickenId);
        if (brain == null) {
            brain = new ChickenHostilityBrain(currentTick);
            tracker.putBrain(chickenId, brain);
        }

        if (applyJoinCooldown && socialAlertService.isJoinBlocked(brain, currentTick)) {
            return false;
        }

        if (activationPolicy.isActivationBlockedForAggressor(chicken, aggressor)) {
            return false;
        }
        if (isIneligible(
                chicken,
                brain,
                List.of(),
                false,
                socialAlertService.responderAdultsOnly()
        )) {
            return false;
        }
        if (targetingService.isAreaActivationBlocked(chicken, chickenId)) {
            return false;
        }
        UUID aggressorUuid = aggressor.getUniqueId();
        if (targetingService.isRetargetBlocked(brain, aggressorUuid, currentTick)) {
            return false;
        }
        if (targetingService.isOnGlobalTargetCooldown(aggressorUuid, chickenId, currentTick)) {
            return false;
        }
        if (targetingService.isAggressorSlotUnavailable(aggressorUuid, chickenId)) {
            return false;
        }

        boolean alreadyTargeting = aggressorUuid.equals(brain.targetUuid)
                && brain.state != ChickenHostilityState.IDLE;
        setTarget(brain, aggressor, aggressionDurationTicks);
        brain.hostilityCause = hostilityCause;
        brain.socialAlertOverrideEligibility = true;
        if (!alreadyTargeting) {
            transition(chicken, brain, ChickenHostilityState.ALERT);
        } else {
            brain.nextProcessTick = currentTick;
        }
        if (applyJoinCooldown) {
            socialAlertService.applyJoinCooldown(brain, currentTick);
        }
        return true;
    }

    private void trackLoadedChickens() {
        for (World world : plugin.getServer().getWorlds()) {
            if (activationPolicy.isWorldDisallowed(world)) {
                continue;
            }
            for (Chicken chicken : world.getEntitiesByClass(Chicken.class)) {
                trackChicken(chicken, false);
            }
        }
        tracker.resetCursors();
    }

    private void trackChicken(Chicken chicken, boolean replaceActivationState) {
        if (chicken == null || !chicken.isValid() || chicken.isDead()) {
            return;
        }
        if (activationPolicy.isWorldDisallowed(chicken.getWorld())) {
            return;
        }

        tracker.track(chicken);
        activationPolicy.track(chicken, replaceActivationState);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private void onCreatureSpawn(CreatureSpawnEvent event) {
        if (event.getEntity() instanceof Chicken chicken) {
            CreatureSpawnEvent.SpawnReason spawnReason = event.getSpawnReason();
            activationPolicy.markNonNaturalChicken(chicken, spawnReason);
            enqueueStateMutation(() -> trackChicken(chicken, true));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getFinalDamage() <= 0.0D) return;

        if (event.getDamager() instanceof Chicken chicken && event.getEntity() instanceof Player) {
            int chickenId = chicken.getEntityId();
            enqueueStateMutation(() -> {
                ChickenHostilityBrain brain = tracker.brain(chickenId);
                if (brain == null) return;
                refreshThreat(brain, threatTimeoutTicks);
            });
        }

        if (!socialAlertService.onDamage()) {
            return;
        }
        if (!(event.getEntity() instanceof Chicken victimChicken)) {
            return;
        }

        Player aggressor = socialAlertService.resolveDamagingPlayer(event.getDamager());
        if (aggressor == null) {
            return;
        }

        List<Entity> nearby = new java.util.ArrayList<>(
                victimChicken.getNearbyEntities(socialAlertService.radius(), socialAlertService.radius(), socialAlertService.radius())
        );
        nearby.add(victimChicken);
        int victimChickenId = victimChicken.getEntityId();
        enqueueStateMutation(() -> socialAlertService.emit(
                victimChickenId,
                aggressor,
                nearby,
                threatTimeoutTicks,
                currentTick,
                HostilityCause.HERD_RETALIATION_DAMAGE
        ));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private void onEntityPickupItem(EntityPickupItemEvent event) {
        if (territorialPickupService.isDisabled()) {
            return;
        }
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (!targetEligibilityService.isEligible(player, activationPolicy.worldFilter(), currentTick)
                || activationPolicy.isPeacefulWorld(player.getWorld())) {
            return;
        }

        Item item = event.getItem();
        Material material = item.getItemStack().getType();
        if (material == COOKED_MEAT) {
            HostilityCause cookingCause = consumeCookValidation(player, item.getLocation(), COOKED_MEAT);
            if (cookingCause == null) {
                return;
            }
            enqueueStateMutation(() -> triggerCookedMeatAggression(player, item.getLocation(), cookingCause));
            return;
        }
        if (territorialPickupService.isNonTerritorialPickupMaterial(material)) {
            return;
        }
        if (territorialPickupService.maxItemAgeTicks() > 0 && item.getTicksLived() > territorialPickupService.maxItemAgeTicks()) {
            return;
        }

        int pickedUpAmount = item.getItemStack().getAmount() - event.getRemaining();
        if (pickedUpAmount <= 0) {
            return;
        }

        List<Entity> nearby = item.getNearbyEntities(
                territorialPickupService.detectionRadius(),
                territorialPickupService.detectionRadius(),
                territorialPickupService.detectionRadius()
        );
        if (!territorialPickupService.hasTerritorialWitness(player, item, nearby)) {
            return;
        }

        enqueueStateMutation(() -> territorialPickupService.recordPickup(player, material, pickedUpAmount, nearby, currentTick));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private void onBlockCook(BlockCookEvent event) {
        if (territorialPickupService.isDisabled()) {
            return;
        }
        Material cookerType = event.getBlock().getType();
        if (!isSupportedCooker(cookerType)) {
            return;
        }
        if (event.getSource().getType() != RAW_MEAT) {
            return;
        }
        World world = event.getBlock().getWorld();
        if (activationPolicy.isWorldDisallowed(world) || activationPolicy.isPeacefulWorld(world)) {
            return;
        }
        long tickSnapshot = currentTick;
        enqueueStateMutation(() -> {
            cookedBatchByCooker.put(
                    blockKey(event.getBlock().getLocation()),
                    new CookedBatchReady(
                            COOKED_MEAT,
                            tickSnapshot + COOK_VALIDATION_WINDOW_TICKS,
                            resolveCookingCause(cookerType)
                    )
            );
            cleanupCookValidationState(tickSnapshot);
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private void onFurnaceExtract(FurnaceExtractEvent event) {
        if (territorialPickupService.isDisabled() || event.getItemType() != COOKED_MEAT) {
            return;
        }
        Block block = event.getBlock();
        if (!isSupportedCooker(block.getType())) {
            return;
        }
        World world = block.getWorld();
        if (activationPolicy.isWorldDisallowed(world) || activationPolicy.isPeacefulWorld(world)) {
            return;
        }
        Player player = event.getPlayer();
        if (!targetEligibilityService.isEligible(player, activationPolicy.worldFilter(), currentTick)) {
            return;
        }
        Location outputLocation = block.getLocation().add(0.5D, 0.5D, 0.5D);
        HostilityCause cookingCause = consumeCookValidation(
                player,
                outputLocation,
                COOKED_MEAT,
                resolveCookingCause(block.getType())
        );
        if (cookingCause == null) {
            return;
        }
        enqueueStateMutation(() -> triggerCookedMeatAggression(player, outputLocation, cookingCause));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private void onInventoryClick(InventoryClickEvent event) {
        if (territorialPickupService.isDisabled()) {
            return;
        }
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        InventoryType topType = event.getView().getTopInventory().getType();
        if (topType != InventoryType.FURNACE && topType != InventoryType.SMOKER) {
            return;
        }
        if (!(event.getView().getTopInventory().getHolder() instanceof org.bukkit.inventory.BlockInventoryHolder holder)) {
            return;
        }
        ItemStack cursor = event.getCursor();
        ItemStack current = event.getCurrentItem();
        boolean placedRawOnInputSlot = event.getRawSlot() == 0 && cursor != null && cursor.getType() == RAW_MEAT;
        boolean shiftMovedRaw = event.isShiftClick()
                && event.getClickedInventory() != null
                && event.getClickedInventory().getType() == InventoryType.PLAYER
                && current != null
                && current.getType() == RAW_MEAT;
        if (!placedRawOnInputSlot && !shiftMovedRaw) {
            return;
        }
        long tickSnapshot = currentTick;
        enqueueStateMutation(() -> {
            pendingCookIntentByPlayer.put(
                    player.getUniqueId(),
                    new PendingCookIntent(
                            blockKey(holder.getBlock().getLocation()),
                            tickSnapshot + COOK_VALIDATION_WINDOW_TICKS,
                            resolveCookingCause(holder.getBlock().getType())
                    )
            );
            cleanupCookValidationState(tickSnapshot);
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private void onPlayerInteract(PlayerInteractEvent event) {
        if (territorialPickupService.isDisabled()) {
            return;
        }
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) {
            return;
        }
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Material blockType = event.getClickedBlock().getType();
        if (blockType != Material.CAMPFIRE && blockType != Material.SOUL_CAMPFIRE) {
            return;
        }
        ItemStack item = event.getItem();
        if (item == null || item.getType() != RAW_MEAT) {
            return;
        }
        Player player = event.getPlayer();
        long tickSnapshot = currentTick;
        enqueueStateMutation(() -> {
            pendingCookIntentByPlayer.put(
                    player.getUniqueId(),
                    new PendingCookIntent(
                            blockKey(event.getClickedBlock().getLocation()),
                            tickSnapshot + COOK_VALIDATION_WINDOW_TICKS,
                            HostilityCause.COOKING_CAMPFIRE
                    )
            );
            cleanupCookValidationState(tickSnapshot);
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private void onPlayerQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        enqueueStateMutation(() -> {
            targetingService.removeGlobalCooldown(playerId);
            territorialPickupService.removePlayer(playerId);
            pendingCookIntentByPlayer.remove(playerId);
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private void onChunkLoad(ChunkLoadEvent event) {
        if (activationPolicy.isWorldDisallowed(event.getWorld())) {
            return;
        }

        List<Chicken> chickens = new java.util.ArrayList<>();
        for (Entity entity : event.getChunk().getEntities()) {
            if (entity instanceof Chicken chicken) {
                chickens.add(chicken);
            }
        }
        if (chickens.isEmpty()) return;
        enqueueStateMutation(() -> {
            for (Chicken chicken : chickens) {
                trackChicken(chicken, false);
            }
        });
    }

    @EventHandler
    private void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity() instanceof Chicken chicken) {
            int chickenId = chicken.getEntityId();
            Player killer = socialAlertService.onNearbyDeath() ? chicken.getKiller() : null;
            List<Entity> nearby = killer == null
                    ? List.of()
                    : chicken.getNearbyEntities(socialAlertService.radius(), socialAlertService.radius(), socialAlertService.radius());
            enqueueStateMutation(() -> {
                if (killer != null) {
                    socialAlertService.emit(
                            chickenId,
                            killer,
                            nearby,
                            threatTimeoutTicks,
                            currentTick,
                            HostilityCause.HERD_RETALIATION_NEARBY_KILL
                    );
                }
                removeTrackedChicken(chickenId, chicken);
            });
        }
    }

    @EventHandler
    private void onEntityRemoveFromWorld(EntityRemoveFromWorldEvent event) {
        if (event.getEntity() instanceof Chicken chicken) {
            int chickenId = chicken.getEntityId();
            enqueueStateMutation(() -> removeTrackedChicken(chickenId, chicken));
        }
    }

    @EventHandler
    private void onChunkUnload(ChunkUnloadEvent event) {
        List<Chicken> chickens = new java.util.ArrayList<>();
        for (Entity entity : event.getChunk().getEntities()) {
            if (entity instanceof Chicken chicken) {
                chickens.add(chicken);
            }
        }
        if (chickens.isEmpty()) return;
        enqueueStateMutation(() -> {
            for (Chicken chicken : chickens) {
                removeTrackedChicken(chicken.getEntityId(), chicken);
            }
        });
    }

    @EventHandler
    private void onWorldUnload(WorldUnloadEvent event) {
        World world = event.getWorld();
        enqueueStateMutation(() -> {
            for (var iterator = tracker.trackedChickens().int2ObjectEntrySet().fastIterator(); iterator.hasNext(); ) {
                Int2ObjectMap.Entry<Chicken> entry = iterator.next();
                Chicken chicken = entry.getValue();
                if (chicken.getWorld() != world) continue;

                removeBrain(entry.getIntKey(), chicken);
                socialAlertService.removeChicken(entry.getIntKey());
                iterator.remove();
                tracker.resetCursors();
            }
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private void onWorldDifficultyChange(WorldDifficultyChangeEvent event) {
        if (event.getDifficulty() != Difficulty.PEACEFUL) {
            return;
        }

        World world = event.getWorld();
        enqueueStateMutation(() -> {
            for (var iterator = tracker.trackedChickens().int2ObjectEntrySet().fastIterator(); iterator.hasNext(); ) {
                Int2ObjectMap.Entry<Chicken> entry = iterator.next();
                Chicken chicken = entry.getValue();
                if (chicken.getWorld() != world) {
                    continue;
                }

                ChickenHostilityBrain brain = tracker.brain(entry.getIntKey());
                if (brain != null) {
                    clearTargetAndIdle(chicken, brain);
                }
            }
        });
    }

    private Player findNearestValidPlayer(Location origin, List<Entity> nearby) {
        Player nearest = null;
        double bestDistanceSq = Double.MAX_VALUE;
        for (Entity entity : nearby) {
            if (!(entity instanceof Player candidate)) {
                continue;
            }
            if (!targetEligibilityService.isEligible(candidate, activationPolicy.worldFilter(), currentTick)) {
                continue;
            }
            double distanceSq = candidate.getLocation().distanceSquared(origin);
            if (distanceSq >= bestDistanceSq) {
                continue;
            }
            bestDistanceSq = distanceSq;
            nearest = candidate;
        }
        return nearest;
    }

    private HostilityCause consumeCookValidation(Player player, Location outputLocation, Material cookedType) {
        return consumeCookValidation(player, outputLocation, cookedType, null);
    }

    private HostilityCause consumeCookValidation(
            Player player,
            Location outputLocation,
            Material cookedType,
            HostilityCause fallbackCause
    ) {
        PendingCookIntent intent = pendingCookIntentByPlayer.get(player.getUniqueId());
        String cookerKey = resolveCookerKey(outputLocation);
        if (cookerKey == null) {
            return null;
        }
        CookedBatchReady ready = cookedBatchByCooker.get(cookerKey);
        if (ready == null || currentTick > ready.expiresAtTick() || ready.material() != cookedType) {
            return null;
        }
        if (intent != null && currentTick <= intent.expiresAtTick() && intent.cookerKey().equals(cookerKey)) {
            pendingCookIntentByPlayer.remove(player.getUniqueId());
            cookedBatchByCooker.remove(cookerKey);
            return intent.cookingCause();
        }
        cookedBatchByCooker.remove(cookerKey);
        return ready.cookingCause() == null ? fallbackCause : ready.cookingCause();
    }

    private void triggerCookedMeatAggression(Player player, Location origin, HostilityCause cookingCause) {
        World world = origin.getWorld();
        if (world == null) {
            return;
        }
        double radius = territorialPickupService.detectionRadius();
        List<Entity> nearby = new ArrayList<>(world.getNearbyEntities(origin, radius, radius, radius));
        Chicken firstRecruit = null;
        for (Entity entity : nearby) {
            if (!(entity instanceof Chicken chicken)) {
                continue;
            }
            if (!tryRecruitChicken(chicken, player, threatTimeoutTicks, true, cookingCause)) {
                continue;
            }
            if (firstRecruit == null) {
                firstRecruit = chicken;
            }
        }
        if (firstRecruit != null) {
            socialAlertService.emit(
                    firstRecruit.getEntityId(),
                    player,
                    nearby,
                    threatTimeoutTicks,
                    currentTick,
                    cookingCause
            );
        }
    }

    private HostilityCause resolveCookingCause(Material cookerType) {
        return switch (cookerType) {
            case FURNACE -> HostilityCause.COOKING_FURNACE;
            case SMOKER -> HostilityCause.COOKING_SMOKER;
            case CAMPFIRE, SOUL_CAMPFIRE -> HostilityCause.COOKING_CAMPFIRE;
            default -> HostilityCause.TERRITORIAL_PICKUP;
        };
    }

    private String resolveCookerKey(Location location) {
        String direct = blockKey(location);
        if (cookedBatchByCooker.containsKey(direct)) {
            return direct;
        }
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    String candidate = blockKey(location.clone().add(dx, dy, dz));
                    if (cookedBatchByCooker.containsKey(candidate)) {
                        return candidate;
                    }
                }
            }
        }
        return null;
    }

    private String blockKey(Location location) {
        return location.getWorld().getUID() + ":" + location.getBlockX() + ":" + location.getBlockY() + ":" + location.getBlockZ();
    }

    private void cleanupCookValidationState(long tickSnapshot) {
        pendingCookIntentByPlayer.entrySet().removeIf(entry -> tickSnapshot > entry.getValue().expiresAtTick());
        cookedBatchByCooker.entrySet().removeIf(entry -> tickSnapshot > entry.getValue().expiresAtTick());
    }

    private boolean isSupportedCooker(Material material) {
        return material == Material.FURNACE
                || material == Material.SMOKER
                || material == Material.CAMPFIRE
                || material == Material.SOUL_CAMPFIRE;
    }

    private record PendingCookIntent(String cookerKey, long expiresAtTick, HostilityCause cookingCause) {}

    private record CookedBatchReady(Material material, long expiresAtTick, HostilityCause cookingCause) {}
}


