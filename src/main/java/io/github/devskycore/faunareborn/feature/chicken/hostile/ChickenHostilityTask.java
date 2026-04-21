package io.github.devskycore.faunareborn.feature.chicken.hostile;

import io.github.devskycore.faunareborn.config.ChickenHostilitySettings;
import io.github.devskycore.faunareborn.config.WorldFilter;
import io.github.devskycore.faunareborn.core.FaunaRebornPlugin;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.entity.Chicken;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

final class ChickenHostilityTask implements Listener {

    private static final double BABY_RADIUS = 7.0D;
    private static final double BABY_RADIUS_SQ = BABY_RADIUS * BABY_RADIUS;

    private static final double CHASE_BREAK_RADIUS = 18.0D;
    private static final double CHASE_BREAK_RADIUS_SQ = CHASE_BREAK_RADIUS * CHASE_BREAK_RADIUS;

    private static final long TICK_RATE = 1L;

    private static final long ALERT_DURATION_TICKS = 1L;
    private static final long ELIGIBILITY_CACHE_TICKS = 10L;

    private static final long TARGET_SEARCH_IDLE_INTERVAL_TICKS = 4L;
    private static final long TARGET_SEARCH_CHASE_INTERVAL_TICKS = 2L;
    private static final int IDLE_BUCKETS = 20;
    private static final long ACTIVE_TICK_INTERVAL = 2L;

    private static final double MAX_VERTICAL_GAP = 8.0D;
    private static final int NO_PROGRESS_RESET_TICKS = 30;
    private static final double MIN_PROGRESS_DELTA_SQ = 0.0001D;
    private static final double NO_PROGRESS_MIN_DISTANCE_SQ_2D = 9.0D;
    private static final double BASE_CHASE_SPEED_PER_TICK = 0.10D;
    private static final double MIN_CHASE_SPEED_PER_TICK = 0.02D;
    private static final double MAX_CHASE_SPEED_PER_TICK = 0.35D;
    private static final double PLAYER_PROXIMITY_RADIUS = 16.0D;

    private final FaunaRebornPlugin plugin;
    private final double attackDamage;
    private final int maxSimultaneousAttackersPerTarget;
    private final int attackCooldownTicks;
    private final double detectionRadius;
    private final double detectionRadiusSq;
    private final double attackRange;
    private final double attackRangeSq;
    private final double chaseSpeedPerTick;
    private final double activationChance;
    private final boolean onlyNaturalChickens;
    private final boolean ignoreNamed;
    private final WorldFilter worldFilter;
    private final double processingRadius;

    private final Int2ObjectOpenHashMap<ChickenHostilityBrain> brains = new Int2ObjectOpenHashMap<>();
    private final Int2ObjectOpenHashMap<Chicken> trackedChickens = new Int2ObjectOpenHashMap<>();
    private final Int2ObjectOpenHashMap<ActivationState> activationStates = new Int2ObjectOpenHashMap<>();
    private final Vector scratchVelocity = new Vector();

    private BukkitTask task;
    private long currentTick;

    ChickenHostilityTask(FaunaRebornPlugin plugin, ChickenHostilitySettings settings) {
        this.plugin = plugin;
        this.attackDamage = settings.attackDamage();
        this.maxSimultaneousAttackersPerTarget = settings.maxSimultaneousAttackersPerPlayer();
        this.attackCooldownTicks = settings.attackCooldownTicks();
        this.detectionRadius = settings.detectionRadius();
        this.detectionRadiusSq = this.detectionRadius * this.detectionRadius;
        this.attackRange = settings.attackRange();
        this.attackRangeSq = this.attackRange * this.attackRange;
        this.activationChance = settings.activation().chance();
        this.onlyNaturalChickens = settings.activation().onlyNaturalChickens();
        this.ignoreNamed = settings.activation().ignoreNamed();
        this.chaseSpeedPerTick = clamp(
                BASE_CHASE_SPEED_PER_TICK * settings.movementSpeedMultiplier(),
                MIN_CHASE_SPEED_PER_TICK,
                MAX_CHASE_SPEED_PER_TICK
        );
        this.worldFilter = settings.worldFilter();
        this.processingRadius = Math.max(this.detectionRadius, PLAYER_PROXIMITY_RADIUS);
    }

    void start() {
        if (task != null) return;

        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 0L, TICK_RATE);
    }

    void stop() {
        if (task != null) task.cancel();
        task = null;

        HandlerList.unregisterAll(this);

        trackedChickens.clear();
        brains.clear();
        activationStates.clear();
    }

    private void tick() {
        currentTick++;

        for (var iterator = trackedChickens.int2ObjectEntrySet().fastIterator(); iterator.hasNext(); ) {
            Int2ObjectMap.Entry<Chicken> entry = iterator.next();
            int chickenId = entry.getIntKey();
            Chicken chicken = entry.getValue();

            if (!chicken.isValid() || chicken.isDead()) {
                removeBrain(chickenId, chicken);
                iterator.remove();
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
        }
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

        if (!isWorldAllowed(chicken.getWorld())) {
            clearTargetAndIdle(brain);
            return;
        }

        List<Entity> nearby = chicken.getNearbyEntities(processingRadius, processingRadius, processingRadius);

        if (!shouldActivateChicken(chicken, nearby)) {
            clearTargetAndIdle(brain);
            return;
        }

        if (!isEligible(chicken, brain, nearby)) {
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

    private boolean isEligible(Chicken chicken, ChickenHostilityBrain brain, List<Entity> nearby) {
        if (currentTick < brain.nextEligibilityRefreshTick) {
            return brain.eligible;
        }

        boolean eligibleNow = chicken.isAdult() && hasBabyNearby(chicken, nearby);
        brain.eligible = eligibleNow;
        brain.nextEligibilityRefreshTick = currentTick + ELIGIBILITY_CACHE_TICKS;

        return eligibleNow;
    }

    private void handleIdle(Chicken chicken, ChickenHostilityBrain brain, List<Entity> nearby) {
        if (currentTick < brain.nextTargetSearchTick) return;

        brain.nextTargetSearchTick = currentTick + TARGET_SEARCH_IDLE_INTERVAL_TICKS;

        Player target = findTarget(chicken, nearby, chicken.getEntityId());
        if (target == null) return;

        setTarget(brain, target);
        transition(brain, ChickenHostilityState.ALERT);
    }

    private void handleAlert(Chicken chicken, ChickenHostilityBrain brain) {
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
        Player target = resolveTarget(chicken, brain.targetUuid);
        if (target == null) {
            if (currentTick >= brain.nextTargetSearchTick) {
                brain.nextTargetSearchTick = currentTick + TARGET_SEARCH_CHASE_INTERVAL_TICKS;
                Player reacquired = findTarget(chicken, nearby, chicken.getEntityId());
                if (reacquired != null) {
                    setTarget(brain, reacquired);
                    target = reacquired;
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

        if (failsSimplePathing(chicken, target, brain, true)) {
            clearTargetAndIdle(brain);
            return;
        }

        move(chicken, target);
        if (distSq <= attackRangeSq) {
            transition(brain, ChickenHostilityState.ATTACK);
            attack(chicken, target, brain);
        }
    }

    private void handleAttack(Chicken chicken, ChickenHostilityBrain brain) {
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

        if (failsSimplePathing(chicken, target, brain, distSq > attackRangeSq)) {
            clearTargetAndIdle(brain);
            return;
        }

        move(chicken, target);

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
        brain.targetUuid = null;
        brain.nextTargetSearchTick = currentTick;
        resetProgressTracking(brain);
        transition(brain, ChickenHostilityState.IDLE);
    }

    private void setTarget(ChickenHostilityBrain brain, Player target) {
        UUID nextTarget = target.getUniqueId();
        if (!nextTarget.equals(brain.targetUuid)) {
            resetProgressTracking(brain);
        }
        brain.targetUuid = nextTarget;
    }

    private Player resolveTarget(Chicken chicken, UUID targetUuid) {
        if (targetUuid == null) return null;

        Player player = plugin.getServer().getPlayer(targetUuid);
        if (!isValidTarget(chicken, player)) return null;

        return player;
    }

    private Player findTarget(Chicken chicken, List<Entity> nearby, int chickenId) {
        Player best = null;
        double bestDist = Double.MAX_VALUE;

        for (Entity entity : nearby) {
            if (!(entity instanceof Player player)) continue;
            if (!isValidTarget(chicken, player)) continue;
            if (!hasAggressorSlot(player.getUniqueId(), chickenId)) continue;

            double distSq = distanceSq(chicken, player);
            if (distSq > detectionRadiusSq || distSq >= bestDist) continue;

            best = player;
            bestDist = distSq;

            if (distSq <= attackRangeSq) break;
        }

        return best;
    }

    private boolean hasAggressorSlot(UUID targetUuid, int selfChickenId) {
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

        return selfAlreadyAssigned || attackers < maxSimultaneousAttackersPerTarget;
    }

    private boolean isValidTarget(Chicken chicken, Player player) {
        if (player == null) return false;
        if (!player.isOnline() || player.isDead()) return false;
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) return false;
        if (!isWorldAllowed(chicken.getWorld())) return false;
        return chicken.getWorld() == player.getWorld();
    }

    private boolean hasBabyNearby(Chicken chicken, List<Entity> nearby) {
        for (Entity entity : nearby) {
            if (!(entity instanceof Chicken nearbyChicken)) continue;
            if (nearbyChicken.isAdult() || nearbyChicken.isDead()) continue;
            if (distanceSq2D(chicken, nearbyChicken) <= BABY_RADIUS_SQ) return true;
        }
        return false;
    }

    private boolean shouldActivateChicken(Chicken chicken) {
        List<Entity> nearby = chicken.getNearbyEntities(processingRadius, processingRadius, processingRadius);
        return shouldActivateChicken(chicken, nearby);
    }

    private boolean shouldActivateChicken(Chicken chicken, List<Entity> nearbyEntities) {
        if (!isPlayerNearby(chicken, nearbyEntities)) {
            return false;
        }

        if (ignoreNamed && hasCustomName(chicken)) {
            return false;
        }

        ActivationState activationState = activationStates.get(chicken.getEntityId());
        if (activationState == null) {
            activationState = new ActivationState(null);
            activationStates.put(chicken.getEntityId(), activationState);
        }

        if (onlyNaturalChickens && !isNaturalChicken(activationState.spawnReason)) {
            return false;
        }

        if (!activationState.chanceRolled) {
            activationState.chanceRolled = true;
            activationState.chancePassed = rollActivationChance();
        }

        return activationState.chancePassed;
    }

    private boolean isPlayerNearby(Entity entity) {
        List<Entity> nearbyEntities = entity.getNearbyEntities(
                PLAYER_PROXIMITY_RADIUS,
                PLAYER_PROXIMITY_RADIUS,
                PLAYER_PROXIMITY_RADIUS
        );
        return isPlayerNearby(entity, nearbyEntities);
    }

    private boolean isPlayerNearby(Entity entity, List<Entity> nearbyEntities) {
        for (Entity nearbyEntity : nearbyEntities) {
            if (!(nearbyEntity instanceof Player player)) continue;
            if (!player.isOnline() || player.isDead()) continue;
            if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) continue;
            if (player.getWorld() != entity.getWorld()) continue;
            if (distanceSq(entity, player) <= PLAYER_PROXIMITY_RADIUS * PLAYER_PROXIMITY_RADIUS) return true;
        }
        return false;
    }

    private boolean hasCustomName(Chicken chicken) {
        String customName = chicken.getCustomName();
        return customName != null && !customName.trim().isEmpty();
    }

    private boolean isNaturalChicken(CreatureSpawnEvent.SpawnReason spawnReason) {
        if (spawnReason == null) {
            return true;
        }

        String reasonName = spawnReason.name();
        return !reasonName.equals("SPAWNER")
                && !reasonName.equals("SPAWNER_EGG")
                && !reasonName.equals("DISPENSE_EGG")
                && !reasonName.equals("EGG")
                && !reasonName.equals("BREEDING");
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

    private void move(Chicken chicken, Player player) {
        double dx = player.getX() - chicken.getX();
        double dz = player.getZ() - chicken.getZ();
        double lenSq = dx * dx + dz * dz;

        if (lenSq < 0.0001D) return;

        double invLen = chaseSpeedPerTick / Math.sqrt(lenSq);

        scratchVelocity.setX(dx * invLen);
        scratchVelocity.setY(chicken.getVelocity().getY());
        scratchVelocity.setZ(dz * invLen);

        chicken.setVelocity(scratchVelocity);
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private void attack(Chicken chicken, Player player, ChickenHostilityBrain brain) {
        if (brain.lastAttackTick != Long.MIN_VALUE && currentTick - brain.lastAttackTick < attackCooldownTicks) return;

        player.damage(attackDamage, chicken);
        brain.lastAttackTick = currentTick;
    }

    private boolean isWorldAllowed(World world) {
        return worldFilter.isWorldAllowed(world.getName());
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
    }

    private void removeBrain(int chickenId, Chicken chicken) {
        brains.remove(chickenId);
        activationStates.remove(chickenId);
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
            int chickenId = chicken.getEntityId();
            trackedChickens.put(chickenId, chicken);
            activationStates.put(chickenId, new ActivationState(event.getSpawnReason()));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private void onChunkLoad(ChunkLoadEvent event) {
        for (Entity entity : event.getChunk().getEntities()) {
            if (entity instanceof Chicken chicken) {
                int chickenId = chicken.getEntityId();
                trackedChickens.put(chickenId, chicken);
                if (!activationStates.containsKey(chickenId)) {
                    activationStates.put(chickenId, new ActivationState(null));
                }
            }
        }
    }

    @EventHandler
    private void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity() instanceof Chicken chicken) {
            int chickenId = chicken.getEntityId();
            removeBrain(chickenId, chicken);
            trackedChickens.remove(chickenId);
        }
    }

    @EventHandler
    private void onChunkUnload(ChunkUnloadEvent event) {
        for (Entity entity : event.getChunk().getEntities()) {
            if (entity instanceof Chicken chicken) {
                int chickenId = chicken.getEntityId();
                removeBrain(chickenId, chicken);
                trackedChickens.remove(chickenId);
            }
        }
    }

    @EventHandler
    private void onWorldUnload(WorldUnloadEvent event) {
        for (var iterator = trackedChickens.int2ObjectEntrySet().fastIterator(); iterator.hasNext(); ) {
            Int2ObjectMap.Entry<Chicken> entry = iterator.next();
            Chicken chicken = entry.getValue();
            if (chicken.getWorld() != event.getWorld()) continue;

            removeBrain(entry.getIntKey(), chicken);
            iterator.remove();
        }
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
