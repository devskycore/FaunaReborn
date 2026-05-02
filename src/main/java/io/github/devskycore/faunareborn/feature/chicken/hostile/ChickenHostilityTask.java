package io.github.devskycore.faunareborn.feature.chicken.hostile;

import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent;
import io.papermc.paper.event.world.WorldDifficultyChangeEvent;
import io.github.devskycore.faunareborn.config.core.ChickenHostilitySettings;
import io.github.devskycore.faunareborn.config.world.WorldFilter;
import io.github.devskycore.faunareborn.core.FaunaRebornPlugin;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntIterator;
import org.bukkit.Difficulty;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.Chunk;
import org.bukkit.entity.Chicken;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ThreadLocalRandom;
import java.util.HashMap;

final class ChickenHostilityTask implements Listener {

    private static final double BABY_RADIUS = 7.0D;
    private static final double BABY_RADIUS_SQ = BABY_RADIUS * BABY_RADIUS;

    private static final double CHASE_BREAK_RADIUS = 18.0D;
    private static final double CHASE_BREAK_RADIUS_SQ = CHASE_BREAK_RADIUS * CHASE_BREAK_RADIUS;

    private static final long TICK_RATE = 1L;

    private static final long ALERT_DURATION_TICKS = 0L;
    private static final long ELIGIBILITY_CACHE_TICKS = 10L;

    private static final long TARGET_SEARCH_IDLE_INTERVAL_TICKS = 4L;
    private static final long TARGET_SEARCH_CHASE_INTERVAL_TICKS = 1L;
    private static final long TARGET_SEARCH_FAILURE_COOLDOWN_TICKS = 8L;
    private static final int IDLE_BUCKETS = 20;
    private static final long ACTIVE_TICK_INTERVAL = 1L;
    private static final long INVALID_CLEANUP_INTERVAL_TICKS = 20L;
    private static final int INVALID_CLEANUP_BATCH_SIZE = 32;

    private static final double MAX_VERTICAL_GAP = 8.0D;
    private static final int NO_PROGRESS_RESET_TICKS = 30;
    private static final double MIN_PROGRESS_DELTA_SQ = 0.0001D;
    private static final double NO_PROGRESS_MIN_DISTANCE_SQ_2D = 9.0D;
    private static final double BASE_CHASE_SPEED_PER_TICK = 0.12D;
    private static final double MIN_CHASE_SPEED_PER_TICK = 0.02D;
    private static final double MAX_CHASE_SPEED_PER_TICK = 0.35D;
    private static final double PLAYER_PROXIMITY_RADIUS = 16.0D;
    private static final byte TRUE_BYTE = 1;

    private final FaunaRebornPlugin plugin;
    private final double attackDamage;
    private final int maxSimultaneousAttackersPerTarget;
    private final int globalTargetCooldownTicks;
    private final int maxActiveHostileChickensPerChunk;
    private final int maxActiveHostileChickensPerWorld;
    private final int maxProcessedChickensPerTick;
    private final int attackCooldownTicks;
    private final int threatTimeoutTicks;
    private final int retargetGraceTicks;
    private final int noLineOfSightResetTicks;
    private final boolean socialAlertEnabled;
    private final boolean socialAlertOnDamage;
    private final boolean socialAlertOnNearbyDeath;
    private final boolean socialAlertResponderAdultsOnly;
    private final int socialAlertCooldownTicks;
    private final int socialAlertJoinCooldownTicks;
    private final int socialAlertMaxResponders;
    private final double detectionRadiusSq;
    private final double socialAlertRadius;
    private final double attackRangeSq;
    private final double chaseSpeedPerTick;
    private final double movementDistanceBoostStartDistanceSq;
    private final double movementDistanceBoostExtraSpeedPerBlock;
    private final double movementDistanceBoostMaxMultiplier;
    private final boolean movementTerrainJumpEnabled;
    private final double movementTerrainJumpVerticalBoost;
    private final int movementTerrainJumpCooldownTicks;
    private final double movementTerrainJumpTriggerHeightDelta;
    private final double activationChance;
    private final boolean onlyNaturalChickens;
    private final boolean ignoreNamed;
    private final WorldFilter worldFilter;
    private final Map<String, Double> worldDamageMultipliers;
    private final double peacefulDamageMultiplier;
    private final double easyDamageMultiplier;
    private final double normalDamageMultiplier;
    private final double hardDamageMultiplier;
    private final boolean nightDamageEnabled;
    private final double nightDamageMultiplier;
    private final double processingRadius;
    private final WorldNightStateCache worldNightStateCache;
    private final NamespacedKey nonNaturalChickenKey;

    private final Int2ObjectOpenHashMap<ChickenHostilityBrain> brains = new Int2ObjectOpenHashMap<>();
    private final Int2ObjectOpenHashMap<Chicken> trackedChickens = new Int2ObjectOpenHashMap<>();
    private final Int2ObjectOpenHashMap<Chicken> socialAlertTrackedChickens = new Int2ObjectOpenHashMap<>();
    private final Int2ObjectOpenHashMap<ActivationState> activationStates = new Int2ObjectOpenHashMap<>();
    private final ChickenHostilityVisualController visualController;
    private final Map<UUID, Long> globalTargetCooldownUntil = new HashMap<>();
    private final Int2ObjectOpenHashMap<Long> socialAlertCooldownUntilByChickenId = new Int2ObjectOpenHashMap<>();
    private final ConcurrentLinkedQueue<Runnable> pendingStateMutations = new ConcurrentLinkedQueue<>();
    private final Vector scratchVelocity = new Vector();

    private ScheduledTask task;
    private IntIterator processingCursor;
    private IntIterator cleanupCursor;
    private long currentTick;

    ChickenHostilityTask(FaunaRebornPlugin plugin, ChickenHostilitySettings settings) {
        this.plugin = plugin;
        this.attackDamage = settings.attackDamage();
        this.globalTargetCooldownTicks = settings.globalTargetCooldownTicks();
        this.maxSimultaneousAttackersPerTarget = settings.maxSimultaneousAttackersPerPlayer();
        this.maxActiveHostileChickensPerChunk = settings.maxActiveHostileChickensPerChunk();
        this.maxActiveHostileChickensPerWorld = settings.maxActiveHostileChickensPerWorld();
        this.maxProcessedChickensPerTick = settings.maxProcessedChickensPerTick();
        this.attackCooldownTicks = settings.attackCooldownTicks();
        this.threatTimeoutTicks = settings.threatTimeoutTicks();
        this.retargetGraceTicks = settings.retargetGraceTicks();
        this.noLineOfSightResetTicks = settings.noLineOfSightResetTicks();
        this.socialAlertEnabled = settings.socialAlertEnabled();
        this.socialAlertOnDamage = settings.socialAlertOnDamage();
        this.socialAlertOnNearbyDeath = settings.socialAlertOnNearbyDeath();
        this.socialAlertResponderAdultsOnly = settings.socialAlertResponderAdultsOnly();
        this.socialAlertRadius = settings.socialAlertRadius();
        this.socialAlertCooldownTicks = settings.socialAlertCooldownTicks();
        this.socialAlertJoinCooldownTicks = settings.socialAlertJoinCooldownTicks();
        this.socialAlertMaxResponders = settings.socialAlertMaxResponders();
        this.visualController = new ChickenHostilityVisualController(
                settings.visualGlowEnabled(),
                settings.visualParticlesEnabled(),
                settings.visualParticlesIntervalTicks(),
                settings.visualParticlesVolume(),
                settings.visualSoundEnabled(),
                settings.visualSoundIntervalTicks(),
                settings.visualSoundVolume()
        );
        double detectionRadius = settings.detectionRadius();
        this.detectionRadiusSq = detectionRadius * detectionRadius;
        double attackRange = settings.attackRange();
        this.attackRangeSq = attackRange * attackRange;
        this.activationChance = settings.activation().chance();
        this.onlyNaturalChickens = settings.activation().onlyNaturalChickens();
        this.ignoreNamed = settings.activation().ignoreNamed();
        this.chaseSpeedPerTick = Math.clamp(
                BASE_CHASE_SPEED_PER_TICK * settings.movementSpeedMultiplier(),
                MIN_CHASE_SPEED_PER_TICK,
                MAX_CHASE_SPEED_PER_TICK
        );
        double movementDistanceBoostStartDistance = settings.movementDistanceBoostStartDistance();
        this.movementDistanceBoostStartDistanceSq = movementDistanceBoostStartDistance * movementDistanceBoostStartDistance;
        this.movementDistanceBoostExtraSpeedPerBlock = settings.movementDistanceBoostExtraSpeedPerBlock();
        this.movementDistanceBoostMaxMultiplier = settings.movementDistanceBoostMaxMultiplier();
        this.movementTerrainJumpEnabled = settings.movementTerrainJumpEnabled();
        this.movementTerrainJumpVerticalBoost = settings.movementTerrainJumpVerticalBoost();
        this.movementTerrainJumpCooldownTicks = settings.movementTerrainJumpCooldownTicks();
        this.movementTerrainJumpTriggerHeightDelta = settings.movementTerrainJumpTriggerHeightDelta();
        this.worldFilter = settings.worldFilter();
        this.peacefulDamageMultiplier = settings.peacefulDamageMultiplier();
        this.easyDamageMultiplier = settings.easyDamageMultiplier();
        this.normalDamageMultiplier = settings.normalDamageMultiplier();
        this.hardDamageMultiplier = settings.hardDamageMultiplier();
        this.worldDamageMultipliers = settings.worldDamageMultipliers();
        this.nightDamageEnabled = settings.nightDamageEnabled();
        this.nightDamageMultiplier = settings.nightDamageMultiplier();
        this.processingRadius = Math.max(detectionRadius, PLAYER_PROXIMITY_RADIUS);
        this.worldNightStateCache = new WorldNightStateCache(plugin);
        this.nonNaturalChickenKey = new NamespacedKey(plugin, "non_natural_chicken");
    }

    void start() {
        if (task != null) return;

        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        worldNightStateCache.start();
        trackLoadedChickens();
        task = plugin.getServer()
                .getGlobalRegionScheduler()
                .runAtFixedRate(plugin, scheduledTask -> tick(), 1L, TICK_RATE);
    }

    void stop() {
        if (task != null) task.cancel();
        task = null;
        worldNightStateCache.stop();

        HandlerList.unregisterAll(this);

        visualController.clearAll(trackedChickens);
        trackedChickens.clear();
        socialAlertTrackedChickens.clear();
        brains.clear();
        activationStates.clear();
        globalTargetCooldownUntil.clear();
        socialAlertCooldownUntilByChickenId.clear();
        pendingStateMutations.clear();
        processingCursor = null;
        cleanupCursor = null;
        currentTick = 0L;
    }

    private void tick() {
        drainPendingStateMutations();
        currentTick++;
        cleanupGlobalTargetCooldowns();
        cleanupSocialAlertCooldowns();
        if (currentTick % INVALID_CLEANUP_INTERVAL_TICKS == 0L) {
            cleanupInvalidTrackedChickensBatch();
        }
        int processed = 0;
        int scanned = 0;
        int scanBudget = Math.max(maxProcessedChickensPerTick * 2, 64);

        while (processed < maxProcessedChickensPerTick && scanned < scanBudget && !trackedChickens.isEmpty()) {
            int chickenId = nextProcessingChickenId();
            if (chickenId == Integer.MIN_VALUE) break;
            scanned++;

            Chicken chicken = trackedChickens.get(chickenId);
            if (chicken == null) continue;
            if (!chicken.isValid() || chicken.isDead()) {
                removeTrackedChicken(chickenId, chicken);
                continue;
            }

            ChickenHostilityBrain brain = brains.get(chickenId);
            if (brain == null || brain.state == ChickenHostilityState.IDLE) {
                if (Math.floorMod(chickenId, IDLE_BUCKETS) != Math.floorMod(currentTick, IDLE_BUCKETS))
                    continue;
            } else {
                if (currentTick < brain.nextProcessTick) continue;
                brain.nextProcessTick = currentTick + ACTIVE_TICK_INTERVAL;
            }

            processChicken(chicken, chickenId, brain);
            visualController.sync(chickenId, chicken, brains.get(chickenId), currentTick);
            processed++;
        }

        visualController.tick(currentTick, trackedChickens, brains);
    }

    private void processChicken(Chicken chicken, int chickenId, ChickenHostilityBrain brain) {
        if (chicken.isDead() || !chicken.isValid()) {
            removeBrain(chickenId, chicken);
            return;
        }

        if (brain == null) {
            brain = new ChickenHostilityBrain(currentTick);
            brains.put(chickenId, brain);
        }

        if (isWorldDisallowed(chicken.getWorld())) {
            clearTargetAndIdle(brain);
            return;
        }
        if (isPeacefulWorld(chicken.getWorld())) {
            clearTargetAndIdle(brain);
            return;
        }

        if (!isPlayerNearby(chicken)) {
            clearTargetAndIdle(brain);
            return;
        }

        List<Entity> nearby = chicken.getNearbyEntities(processingRadius, processingRadius, processingRadius);

        if (isActivationBlocked(chicken, nearby)) {
            clearTargetAndIdle(brain);
            return;
        }

        boolean requireBabyNearby = !brain.socialAlertOverrideEligibility;
        if (isIneligible(chicken, brain, nearby, requireBabyNearby, true)) {
            clearTargetAndIdle(brain);
            return;
        }

        switch (brain.state) {
            case IDLE -> handleIdle(chicken, brain, nearby);
            case ALERT -> handleAlert(chicken, brain);
            case CHASE -> handleChase(chicken, brain, nearby);
            case ATTACK -> handleAttack(chicken, brain);
        }
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

        boolean hasBabyNearby = hasBabyNearby(chicken, nearby);
        boolean isAdult = chicken.isAdult();
        brain.eligible = isAdult && hasBabyNearby;
        brain.nextEligibilityRefreshTick = currentTick + ELIGIBILITY_CACHE_TICKS;

        if (requireAdult && !isAdult) {
            return true;
        }
        return requireBabyNearby && !hasBabyNearby;
    }

    private void handleIdle(Chicken chicken, ChickenHostilityBrain brain, List<Entity> nearby) {
        if (currentTick < brain.nextTargetSearchTick) return;

        brain.nextTargetSearchTick = currentTick + TARGET_SEARCH_IDLE_INTERVAL_TICKS;

        Player target = findTarget(chicken, brain, nearby, chicken.getEntityId());
        if (target == null) {
            brain.nextTargetSearchTick = currentTick + Math.max(TARGET_SEARCH_IDLE_INTERVAL_TICKS, TARGET_SEARCH_FAILURE_COOLDOWN_TICKS);
            return;
        }

        setTarget(brain, target);
        transition(brain, ChickenHostilityState.ALERT);
    }

    private void handleAlert(Chicken chicken, ChickenHostilityBrain brain) {
        if (hasThreatTimedOut(brain)) {
            calmDown(brain);
            return;
        }

        Player target = resolveTarget(chicken, brain.targetUuid);
        if (target == null) {
            clearTargetAndIdle(brain);
            return;
        }

        double distSq = distanceSq(chicken, target);
        if (distSq > CHASE_BREAK_RADIUS_SQ) {
            clearTargetAndIdle(brain);
            return;
        }

        if (currentTick - brain.stateStartedTick < ALERT_DURATION_TICKS) return;

        transition(brain, ChickenHostilityState.CHASE);
    }

    private void handleChase(Chicken chicken, ChickenHostilityBrain brain, List<Entity> nearby) {
        if (hasThreatTimedOut(brain)) {
            calmDown(brain);
            return;
        }

        Player target = resolveTarget(chicken, brain.targetUuid);
        if (target == null) {
            if (currentTick >= brain.nextTargetSearchTick) {
                brain.nextTargetSearchTick = currentTick + TARGET_SEARCH_CHASE_INTERVAL_TICKS;
                Player reacquired = findTarget(chicken, brain, nearby, chicken.getEntityId());
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

        double distSq = distanceSq(chicken, target);
        if (distSq > CHASE_BREAK_RADIUS_SQ) {
            clearTargetAndIdle(brain);
            return;
        }
        if (hasLostLineOfSight(chicken, target, brain)) {
            clearTargetAndIdle(brain);
            return;
        }

        if (failsSimplePathing(chicken, target, brain, true)) {
            clearTargetAndIdle(brain);
            return;
        }

        move(chicken, target, brain);
        if (distSq <= attackRangeSq) {
            transition(brain, ChickenHostilityState.ATTACK);
            attack(chicken, target, brain);
        }
    }

    private void handleAttack(Chicken chicken, ChickenHostilityBrain brain) {
        if (hasThreatTimedOut(brain)) {
            calmDown(brain);
            return;
        }

        Player target = resolveTarget(chicken, brain.targetUuid);
        if (target == null) {
            transition(brain, ChickenHostilityState.CHASE);
            return;
        }

        double distSq = distanceSq(chicken, target);
        if (distSq > CHASE_BREAK_RADIUS_SQ) {
            clearTargetAndIdle(brain);
            return;
        }
        if (hasLostLineOfSight(chicken, target, brain)) {
            clearTargetAndIdle(brain);
            return;
        }

        if (failsSimplePathing(chicken, target, brain, distSq > attackRangeSq)) {
            clearTargetAndIdle(brain);
            return;
        }

        move(chicken, target, brain);

        if (distSq > attackRangeSq) {
            transition(brain, ChickenHostilityState.CHASE);
            return;
        }

        attack(chicken, target, brain);
    }

    private void transition(ChickenHostilityBrain brain, ChickenHostilityState nextState) {
        if (brain.state == nextState) return;

        brain.state = nextState;
        brain.stateStartedTick = currentTick;
        brain.nextProcessTick = currentTick;
        // Revalidate eligibility on next cycle when state changes.
        brain.nextEligibilityRefreshTick = currentTick;
    }

    private void clearTargetAndIdle(ChickenHostilityBrain brain) {
        if (brain.targetUuid != null && globalTargetCooldownTicks > 0) {
            globalTargetCooldownUntil.put(brain.targetUuid, currentTick + globalTargetCooldownTicks);
        }
        brain.targetUuid = null;
        brain.socialAlertOverrideEligibility = false;
        brain.nextTargetSearchTick = currentTick;
        resetProgressTracking(brain);
        transition(brain, ChickenHostilityState.IDLE);
    }

    private void calmDown(ChickenHostilityBrain brain) {
        UUID previousTargetUuid = brain.targetUuid;
        clearTargetAndIdle(brain);
        if (previousTargetUuid != null && retargetGraceTicks > 0) {
            brain.ignoreTargetUuid = previousTargetUuid;
            brain.ignoreTargetUntilTick = currentTick + retargetGraceTicks;
        }
    }

    private void setTarget(ChickenHostilityBrain brain, Player target) {
        UUID nextTarget = target.getUniqueId();
        if (!nextTarget.equals(brain.targetUuid)) {
            resetProgressTracking(brain);
        }
        brain.targetUuid = nextTarget;
        brain.lastThreatRefreshTick = currentTick;
    }

    private Player resolveTarget(Chicken chicken, UUID targetUuid) {
        if (targetUuid == null) return null;

        Player player = plugin.getServer().getPlayer(targetUuid);
        if (isInvalidTarget(chicken, player)) return null;

        return player;
    }

    private Player findTarget(Chicken chicken, ChickenHostilityBrain brain, List<Entity> nearby, int chickenId) {
        if (isAreaActivationBlocked(chicken, chickenId)) {
            return null;
        }

        Player best = null;
        double bestDist = Double.MAX_VALUE;

        for (Entity entity : nearby) {
            if (!(entity instanceof Player player)) continue;
            if (isInvalidTarget(chicken, player)) continue;
            if (isRetargetBlocked(brain, player.getUniqueId())) continue;
            if (isOnGlobalTargetCooldown(player.getUniqueId(), chickenId)) continue;
            if (isAggressorSlotUnavailable(player.getUniqueId(), chickenId)) continue;

            double distSq = distanceSq(chicken, player);
            if (distSq > detectionRadiusSq || distSq >= bestDist) continue;

            best = player;
            bestDist = distSq;

            if (distSq <= attackRangeSq) break;
        }

        return best;
    }

    private boolean isAggressorSlotUnavailable(UUID targetUuid, int selfChickenId) {
        int attackers = 0;
        boolean selfAlreadyAssigned = false;

        for (var iterator = brains.int2ObjectEntrySet().fastIterator(); iterator.hasNext(); ) {
            Int2ObjectMap.Entry<ChickenHostilityBrain> entry = iterator.next();
            ChickenHostilityBrain brain = entry.getValue();

            if (brain.state == ChickenHostilityState.IDLE || brain.targetUuid == null) continue;
            if (!targetUuid.equals(brain.targetUuid)) continue;

            attackers++;
            if (entry.getIntKey() == selfChickenId) {
                selfAlreadyAssigned = true;
            }
        }

        return !(selfAlreadyAssigned || attackers < maxSimultaneousAttackersPerTarget);
    }

    private boolean isAreaActivationBlocked(Chicken chicken, int selfChickenId) {
        int worldActives = 0;
        int chunkActives = 0;
        Chunk chunk = chicken.getChunk();
        int chunkX = chunk.getX();
        int chunkZ = chunk.getZ();
        World world = chicken.getWorld();

        for (var iterator = brains.int2ObjectEntrySet().fastIterator(); iterator.hasNext(); ) {
            Int2ObjectMap.Entry<ChickenHostilityBrain> entry = iterator.next();
            int chickenId = entry.getIntKey();
            ChickenHostilityBrain candidateBrain = entry.getValue();
            if (candidateBrain.state == ChickenHostilityState.IDLE) continue;
            if (chickenId == selfChickenId) continue;

            Chicken tracked = trackedChickens.get(chickenId);
            if (tracked == null || !tracked.isValid() || tracked.isDead()) continue;
            if (tracked.getWorld() != world) continue;

            worldActives++;
            Chunk trackedChunk = tracked.getChunk();
            if (trackedChunk.getX() == chunkX && trackedChunk.getZ() == chunkZ) {
                chunkActives++;
            }
        }

        return worldActives >= maxActiveHostileChickensPerWorld
                || chunkActives >= maxActiveHostileChickensPerChunk;
    }

    private boolean hasThreatTimedOut(ChickenHostilityBrain brain) {
        if (threatTimeoutTicks <= 0) {
            return false;
        }
        return currentTick - brain.lastThreatRefreshTick >= threatTimeoutTicks;
    }

    private boolean isRetargetBlocked(ChickenHostilityBrain brain, UUID candidateTargetUuid) {
        if (brain.ignoreTargetUuid == null) {
            return false;
        }
        if (currentTick >= brain.ignoreTargetUntilTick) {
            brain.ignoreTargetUuid = null;
            brain.ignoreTargetUntilTick = Long.MIN_VALUE;
            return false;
        }
        return brain.ignoreTargetUuid.equals(candidateTargetUuid);
    }

    private boolean isInvalidTarget(Chicken chicken, Player player) {
        if (player == null) return true;
        if (!player.isOnline() || player.isDead()) return true;
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) return true;
        if (isWorldDisallowed(chicken.getWorld())) return true;
        if (isPeacefulWorld(chicken.getWorld())) return true;
        return chicken.getWorld() != player.getWorld();
    }

    private boolean hasBabyNearby(Chicken chicken, List<Entity> nearby) {
        for (Entity entity : nearby) {
            if (!(entity instanceof Chicken nearbyChicken)) continue;
            if (nearbyChicken.isAdult() || nearbyChicken.isDead()) continue;
            if (distanceSq2D(chicken, nearbyChicken) <= BABY_RADIUS_SQ) return true;
        }
        return false;
    }

    private boolean isActivationBlocked(Chicken chicken, List<Entity> nearbyEntities) {
        if (!isPlayerNearby(chicken, nearbyEntities)) {
            return true;
        }

        if (ignoreNamed && hasCustomName(chicken)) {
            return true;
        }

        ActivationState activationState = activationStates.get(chicken.getEntityId());
        if (activationState == null) {
            activationState = new ActivationState(null);
            activationStates.put(chicken.getEntityId(), activationState);
        }

        if (onlyNaturalChickens && !isNaturalChicken(chicken, activationState.spawnReason)) {
            return true;
        }

        if (!activationState.chanceRolled) {
            activationState.chanceRolled = true;
            activationState.chancePassed = rollActivationChance();
        }

        return !activationState.chancePassed;
    }

    private boolean isPlayerNearby(Entity entity) {
        Location location = entity.getLocation();
        return !entity.getWorld().getNearbyPlayers(
                location,
                PLAYER_PROXIMITY_RADIUS,
                this::isEligibleNearbyPlayer
        ).isEmpty();
    }

    private boolean isPlayerNearby(Entity entity, List<Entity> nearbyEntities) {
        for (Entity nearbyEntity : nearbyEntities) {
            if (!(nearbyEntity instanceof Player player)) continue;
            if (!player.isOnline() || player.isDead()) continue;
            if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) continue;
            if (isPeacefulWorld(entity.getWorld())) continue;
            if (player.getWorld() != entity.getWorld()) continue;
            if (distanceSq(entity, player) <= PLAYER_PROXIMITY_RADIUS * PLAYER_PROXIMITY_RADIUS) return true;
        }
        return false;
    }

    private boolean hasCustomName(Chicken chicken) {
        return chicken.customName() != null;
    }

    private boolean isEligibleNearbyPlayer(Player player) {
        if (!player.isOnline() || player.isDead()) return false;
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) return false;
        return !isPeacefulWorld(player.getWorld());
    }

    private boolean isNaturalChicken(Chicken chicken, CreatureSpawnEvent.SpawnReason spawnReason) {
        if (spawnReason == null) {
            return !isMarkedNonNaturalChicken(chicken);
        }

        return isNaturalSpawnReason(spawnReason);
    }

    private boolean isNaturalSpawnReason(CreatureSpawnEvent.SpawnReason spawnReason) {
        if (spawnReason == null) {
            return true;
        }

        return switch (spawnReason) {
            case SPAWNER, SPAWNER_EGG, DISPENSE_EGG, EGG, BREEDING -> false;
            default -> true;
        };
    }

    private boolean rollActivationChance() {
        if (activationChance <= 0.0D) {
            return false;
        }
        if (activationChance >= 1.0D) {
            return true;
        }
        return ThreadLocalRandom.current().nextDouble() < activationChance;
    }

    private void markNonNaturalChicken(Chicken chicken, CreatureSpawnEvent.SpawnReason spawnReason) {
        if (isNaturalSpawnReason(spawnReason)) {
            return;
        }
        chicken.getPersistentDataContainer().set(nonNaturalChickenKey, PersistentDataType.BYTE, TRUE_BYTE);
    }

    private boolean isMarkedNonNaturalChicken(Chicken chicken) {
        Byte marker = chicken.getPersistentDataContainer().get(nonNaturalChickenKey, PersistentDataType.BYTE);
        return marker != null && marker == TRUE_BYTE;
    }

    private void move(Chicken chicken, Player player, ChickenHostilityBrain brain) {
        double dx = player.getX() - chicken.getX();
        double dz = player.getZ() - chicken.getZ();
        double lenSq = dx * dx + dz * dz;

        if (lenSq < 0.0001D) return;
        faceTarget(chicken, dx, dz);

        double effectiveSpeedPerTick = resolveEffectiveSpeedPerTick(lenSq);

        double invLen = normalizedSpeed(lenSq, effectiveSpeedPerTick);

        scratchVelocity.setX(dx * invLen);
        scratchVelocity.setY(chicken.getVelocity().getY());
        scratchVelocity.setZ(dz * invLen);
        maybeApplyTerrainJump(chicken, player, brain, scratchVelocity);

        chicken.setVelocity(scratchVelocity);
    }

    private void faceTarget(Chicken chicken, double dx, double dz) {
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        chicken.setRotation(yaw, chicken.getLocation().getPitch());
    }

    private static double normalizedSpeed(double lenSq, double speedPerTick) {
        return speedPerTick / Math.sqrt(lenSq);
    }

    private double resolveEffectiveSpeedPerTick(double lenSq) {
        if (lenSq <= movementDistanceBoostStartDistanceSq) {
            return chaseSpeedPerTick;
        }

        double farDistanceMultiplier = Math.min(
                movementDistanceBoostMaxMultiplier,
                1.0D + ((Math.sqrt(lenSq) - Math.sqrt(movementDistanceBoostStartDistanceSq)) * movementDistanceBoostExtraSpeedPerBlock)
        );
        return Math.min(chaseSpeedPerTick * farDistanceMultiplier, MAX_CHASE_SPEED_PER_TICK);
    }

    private void maybeApplyTerrainJump(
            Chicken chicken,
            Player target,
            ChickenHostilityBrain brain,
            Vector velocity
    ) {
        if (!movementTerrainJumpEnabled) return;
        if (!chicken.isOnGround()) return;
        if (brain.lastJumpTick != Long.MIN_VALUE
                && currentTick - brain.lastJumpTick < movementTerrainJumpCooldownTicks) return;

        double verticalDelta = target.getY() - chicken.getY();
        boolean chasingUphill = verticalDelta >= movementTerrainJumpTriggerHeightDelta;
        boolean likelyStuck = brain.noProgressTicks >= Math.max(4, movementTerrainJumpCooldownTicks / 2);

        if (!chasingUphill && !likelyStuck) return;

        velocity.setY(Math.max(velocity.getY(), movementTerrainJumpVerticalBoost));
        brain.lastJumpTick = currentTick;
    }

    private void attack(Chicken chicken, Player player, ChickenHostilityBrain brain) {
        if (brain.lastAttackTick != Long.MIN_VALUE && currentTick - brain.lastAttackTick < attackCooldownTicks) return;

        player.damage(resolveScaledDamage(chicken.getWorld()), chicken);
        brain.lastAttackTick = currentTick;
    }

    private double resolveScaledDamage(World world) {
        double difficultyMultiplier = switch (world.getDifficulty()) {
            case PEACEFUL -> peacefulDamageMultiplier;
            case EASY -> easyDamageMultiplier;
            case NORMAL -> normalDamageMultiplier;
            case HARD -> hardDamageMultiplier;
        };
        double worldMultiplier = worldDamageMultipliers.getOrDefault(world.getName().toLowerCase(java.util.Locale.ROOT), 1.0D);
        double damage = attackDamage * difficultyMultiplier * worldMultiplier;
        if (nightDamageEnabled && worldNightStateCache.isNight(world)) {
            damage *= nightDamageMultiplier;
        }
        return Math.max(0.0D, damage);
    }

    private boolean isWorldDisallowed(World world) {
        return !worldFilter.isWorldAllowed(world.getName());
    }

    private boolean isPeacefulWorld(World world) {
        return world.getDifficulty() == Difficulty.PEACEFUL;
    }

    private boolean failsSimplePathing(Chicken chicken, Player target, ChickenHostilityBrain brain, boolean requireProgress) {
        double verticalGap = Math.abs(target.getY() - chicken.getY());
        if (verticalGap > MAX_VERTICAL_GAP) {
            resetProgressTracking(brain);
            return true;
        }

        if (!requireProgress) {
            resetProgressTracking(brain);
            return false;
        }

        double distSq2D = distanceSq2D(chicken, target);
        if (distSq2D < NO_PROGRESS_MIN_DISTANCE_SQ_2D) {
            resetProgressTracking(brain);
            return false;
        }

        if (!Double.isNaN(brain.lastTargetDistSq2D)
                && distSq2D > brain.lastTargetDistSq2D - MIN_PROGRESS_DELTA_SQ) {
            brain.noProgressTicks++;
        } else {
            brain.noProgressTicks = 0;
        }

        brain.lastTargetDistSq2D = distSq2D;
        return brain.noProgressTicks >= NO_PROGRESS_RESET_TICKS;
    }

    private void resetProgressTracking(ChickenHostilityBrain brain) {
        brain.lastTargetDistSq2D = Double.NaN;
        brain.noProgressTicks = 0;
        brain.noLineOfSightTicks = 0;
    }

    private boolean hasLostLineOfSight(Chicken chicken, Player target, ChickenHostilityBrain brain) {
        if (chicken.hasLineOfSight(target)) {
            brain.noLineOfSightTicks = 0;
            return false;
        }
        brain.noLineOfSightTicks++;
        return brain.noLineOfSightTicks >= noLineOfSightResetTicks;
    }

    private boolean isOnGlobalTargetCooldown(UUID targetUuid, int selfChickenId) {
        ChickenHostilityBrain selfBrain = brains.get(selfChickenId);
        if (selfBrain != null && targetUuid.equals(selfBrain.targetUuid)) {
            return false;
        }

        Long untilTick = globalTargetCooldownUntil.get(targetUuid);
        return untilTick != null && currentTick < untilTick;
    }

    private void cleanupGlobalTargetCooldowns() {
        if (globalTargetCooldownUntil.isEmpty()) {
            return;
        }
        globalTargetCooldownUntil.entrySet().removeIf(entry -> currentTick >= entry.getValue());
    }

    private void cleanupSocialAlertCooldowns() {
        if (socialAlertCooldownUntilByChickenId.isEmpty()) {
            return;
        }
        socialAlertCooldownUntilByChickenId.int2ObjectEntrySet().removeIf(entry -> currentTick >= entry.getValue());
    }

    private void removeBrain(int chickenId, Chicken chicken) {
        brains.remove(chickenId);
        activationStates.remove(chickenId);
        visualController.deactivate(chickenId, chicken);
    }

    private void enqueueStateMutation(Runnable mutation) {
        pendingStateMutations.add(mutation);
    }

    private void drainPendingStateMutations() {
        Runnable mutation;
        while ((mutation = pendingStateMutations.poll()) != null) {
            mutation.run();
        }
    }

    private int nextProcessingChickenId() {
        if (trackedChickens.isEmpty()) {
            return Integer.MIN_VALUE;
        }
        if (processingCursor == null || !processingCursor.hasNext()) {
            processingCursor = trackedChickens.keySet().iterator();
        }
        if (!processingCursor.hasNext()) {
            return Integer.MIN_VALUE;
        }
        return processingCursor.nextInt();
    }

    private void cleanupInvalidTrackedChickensBatch() {
        if (trackedChickens.isEmpty()) {
            cleanupCursor = null;
            return;
        }
        if (cleanupCursor == null || !cleanupCursor.hasNext()) {
            cleanupCursor = trackedChickens.keySet().iterator();
        }

        int checked = 0;
        while (checked < INVALID_CLEANUP_BATCH_SIZE && cleanupCursor != null && cleanupCursor.hasNext()) {
            int chickenId = cleanupCursor.nextInt();
            checked++;
            Chicken chicken = trackedChickens.get(chickenId);
            if (chicken == null || !chicken.isValid() || chicken.isDead()) {
                removeTrackedChicken(chickenId, chicken);
            }
        }
    }

    private void removeTrackedChicken(int chickenId, Chicken chicken) {
        removeBrain(chickenId, chicken);
        trackedChickens.remove(chickenId);
        socialAlertTrackedChickens.remove(chickenId);
        socialAlertCooldownUntilByChickenId.remove(chickenId);
        processingCursor = null;
        cleanupCursor = null;
    }

    private Player resolveDamagingPlayer(Entity damager) {
        if (damager instanceof Player player) {
            return player;
        }
        if (damager instanceof Projectile projectile) {
            ProjectileSource shooter = projectile.getShooter();
            if (shooter instanceof Player player) {
                return player;
            }
        }
        return null;
    }

    private void emitSocialAlert(int emitterChickenId, Player aggressor, List<Entity> nearbyEntities) {
        if (!socialAlertEnabled) {
            return;
        }
        if (aggressor == null || !aggressor.isOnline() || aggressor.isDead()) {
            return;
        }
        if (socialAlertMaxResponders <= 0) {
            return;
        }

        Long cooldownUntil = socialAlertCooldownUntilByChickenId.get(emitterChickenId);
        if (cooldownUntil != null && currentTick < cooldownUntil) {
            return;
        }

        int recruited = 0;
        for (Entity entity : nearbyEntities) {
            if (!(entity instanceof Chicken ally)) {
                continue;
            }
            if (ally.getEntityId() == emitterChickenId) {
                continue;
            }
            if (!ally.isValid() || ally.isDead()) {
                continue;
            }
            int allyId = ally.getEntityId();
            if (!socialAlertTrackedChickens.containsKey(allyId)) {
                trackChicken(ally, null, false);
            }

            ChickenHostilityBrain allyBrain = brains.get(allyId);
            if (allyBrain == null) {
                allyBrain = new ChickenHostilityBrain(currentTick);
                brains.put(allyId, allyBrain);
            }

            if (currentTick < allyBrain.socialAlertBlockedUntilTick) {
                continue;
            }
            if (isInvalidTarget(ally, aggressor)) {
                continue;
            }

            List<Entity> allyNearby = ally.getNearbyEntities(processingRadius, processingRadius, processingRadius);
            if (isActivationBlocked(ally, allyNearby)) {
                continue;
            }
            if (isIneligible(
                    ally,
                    allyBrain,
                    allyNearby,
                    false,
                    socialAlertResponderAdultsOnly
            )) {
                continue;
            }
            if (isAreaActivationBlocked(ally, allyId)) {
                continue;
            }
            UUID aggressorUuid = aggressor.getUniqueId();
            if (isRetargetBlocked(allyBrain, aggressorUuid)) {
                continue;
            }
            if (isOnGlobalTargetCooldown(aggressorUuid, allyId)) {
                continue;
            }
            if (isAggressorSlotUnavailable(aggressorUuid, allyId)) {
                continue;
            }

            setTarget(allyBrain, aggressor);
            allyBrain.socialAlertOverrideEligibility = true;
            transition(allyBrain, ChickenHostilityState.ALERT);
            if (socialAlertJoinCooldownTicks > 0) {
                allyBrain.socialAlertBlockedUntilTick = currentTick + socialAlertJoinCooldownTicks;
            }

            recruited++;
            if (recruited >= socialAlertMaxResponders) {
                break;
            }
        }

        if (recruited > 0 && socialAlertCooldownTicks > 0) {
            socialAlertCooldownUntilByChickenId.put(emitterChickenId, Long.valueOf(currentTick + socialAlertCooldownTicks));
        }
    }

    private void trackLoadedChickens() {
        for (World world : plugin.getServer().getWorlds()) {
            if (isWorldDisallowed(world)) {
                continue;
            }
            for (Chicken chicken : world.getEntitiesByClass(Chicken.class)) {
                trackChicken(chicken, null, false);
            }
        }
        processingCursor = null;
        cleanupCursor = null;
    }

    private void trackChicken(Chicken chicken, CreatureSpawnEvent.SpawnReason spawnReason, boolean replaceActivationState) {
        if (chicken == null || !chicken.isValid() || chicken.isDead()) {
            return;
        }
        if (isWorldDisallowed(chicken.getWorld())) {
            return;
        }

        int chickenId = chicken.getEntityId();
        trackedChickens.put(chickenId, chicken);
        socialAlertTrackedChickens.put(chickenId, chicken);
        if (replaceActivationState) {
            activationStates.put(chickenId, new ActivationState(spawnReason));
        } else {
            activationStates.putIfAbsent(chickenId, new ActivationState(spawnReason));
        }
    }

    private double distanceSq(Entity a, Entity b) {
        double dx = a.getX() - b.getX();
        double dy = a.getY() - b.getY();
        double dz = a.getZ() - b.getZ();
        return dx * dx + dy * dy + dz * dz;
    }

    private double distanceSq2D(Entity a, Entity b) {
        double dx = a.getX() - b.getX();
        double dz = a.getZ() - b.getZ();
        return dx * dx + dz * dz;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private void onCreatureSpawn(CreatureSpawnEvent event) {
        if (event.getEntity() instanceof Chicken chicken) {
            CreatureSpawnEvent.SpawnReason spawnReason = event.getSpawnReason();
            markNonNaturalChicken(chicken, spawnReason);
            enqueueStateMutation(() -> {
                trackChicken(chicken, spawnReason, true);
                processingCursor = null;
                cleanupCursor = null;
            });
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getFinalDamage() <= 0.0D) return;

        if (event.getDamager() instanceof Chicken chicken && event.getEntity() instanceof Player) {
            int chickenId = chicken.getEntityId();
            enqueueStateMutation(() -> {
                ChickenHostilityBrain brain = brains.get(chickenId);
                if (brain == null) return;
                brain.lastThreatRefreshTick = currentTick;
            });
        }

        if (!socialAlertOnDamage) {
            return;
        }
        if (!(event.getEntity() instanceof Chicken victimChicken)) {
            return;
        }

        Player aggressor = resolveDamagingPlayer(event.getDamager());
        if (aggressor == null) {
            return;
        }

        List<Entity> nearby = new java.util.ArrayList<>(
                victimChicken.getNearbyEntities(socialAlertRadius, socialAlertRadius, socialAlertRadius)
        );
        nearby.add(victimChicken);
        int victimChickenId = victimChicken.getEntityId();
        enqueueStateMutation(() -> emitSocialAlert(victimChickenId, aggressor, nearby));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private void onPlayerQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        enqueueStateMutation(() -> globalTargetCooldownUntil.remove(playerId));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private void onChunkLoad(ChunkLoadEvent event) {
        if (isWorldDisallowed(event.getWorld())) {
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
                trackChicken(chicken, null, false);
            }
            processingCursor = null;
            cleanupCursor = null;
        });
    }

    @EventHandler
    private void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity() instanceof Chicken chicken) {
            int chickenId = chicken.getEntityId();
            Player killer = socialAlertOnNearbyDeath ? chicken.getKiller() : null;
            List<Entity> nearby = killer == null
                    ? List.of()
                    : chicken.getNearbyEntities(socialAlertRadius, socialAlertRadius, socialAlertRadius);
            enqueueStateMutation(() -> {
                if (killer != null) {
                    emitSocialAlert(chickenId, killer, nearby);
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
            for (var iterator = trackedChickens.int2ObjectEntrySet().fastIterator(); iterator.hasNext(); ) {
                Int2ObjectMap.Entry<Chicken> entry = iterator.next();
                Chicken chicken = entry.getValue();
                if (chicken.getWorld() != world) continue;

                removeBrain(entry.getIntKey(), chicken);
                socialAlertTrackedChickens.remove(entry.getIntKey());
                socialAlertCooldownUntilByChickenId.remove(entry.getIntKey());
                iterator.remove();
                processingCursor = null;
                cleanupCursor = null;
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
            for (var iterator = trackedChickens.int2ObjectEntrySet().fastIterator(); iterator.hasNext(); ) {
                Int2ObjectMap.Entry<Chicken> entry = iterator.next();
                Chicken chicken = entry.getValue();
                if (chicken.getWorld() != world) {
                    continue;
                }

                ChickenHostilityBrain brain = brains.get(entry.getIntKey());
                if (brain != null) {
                    clearTargetAndIdle(brain);
                }
            }
        });
    }

    private static final class ActivationState {
        private final CreatureSpawnEvent.SpawnReason spawnReason;
        private boolean chanceRolled;
        private boolean chancePassed;

        private ActivationState(CreatureSpawnEvent.SpawnReason spawnReason) {
            this.spawnReason = spawnReason;
            this.chanceRolled = false;
            this.chancePassed = false;
        }
    }

}
