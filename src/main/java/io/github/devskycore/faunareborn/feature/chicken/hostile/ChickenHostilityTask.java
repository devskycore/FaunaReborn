package io.github.devskycore.faunareborn.feature.chicken.hostile;

import io.github.devskycore.faunareborn.core.FaunaRebornPlugin;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.bukkit.GameMode;
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

final class ChickenHostilityTask implements Listener {

    private static final double DETECTION_RADIUS = 8.0D;
    private static final double DETECTION_RADIUS_SQ = DETECTION_RADIUS * DETECTION_RADIUS;

    private static final double BABY_RADIUS = 7.0D;
    private static final double BABY_RADIUS_SQ = BABY_RADIUS * BABY_RADIUS;

    private static final double CHASE_BREAK_RADIUS = 18.0D;
    private static final double CHASE_BREAK_RADIUS_SQ = CHASE_BREAK_RADIUS * CHASE_BREAK_RADIUS;

    private static final double ATTACK_RANGE = 1.5D;
    private static final double ATTACK_RANGE_SQ = ATTACK_RANGE * ATTACK_RANGE;

    private static final double SPEED = 0.25D;

    private static final long COOLDOWN_TICKS = 18L;
    private static final long TICK_RATE = 1L;

    private static final long ALERT_DURATION_TICKS = 1L;
    private static final long ELIGIBILITY_CACHE_TICKS = 10L;

    private static final long TARGET_SEARCH_IDLE_INTERVAL_TICKS = 4L;
    private static final long TARGET_SEARCH_CHASE_INTERVAL_TICKS = 2L;
    private static final int MAX_SIMULTANEOUS_ATTACKERS_PER_TARGET = 3;

    private static final int IDLE_BUCKETS = 20;
    private static final long ACTIVE_TICK_INTERVAL = 2L;

    private static final double MAX_VERTICAL_GAP = 8.0D;
    private static final int NO_PROGRESS_RESET_TICKS = 30;
    private static final double MIN_PROGRESS_DELTA_SQ = 0.0001D;
    private static final double NO_PROGRESS_MIN_DISTANCE_SQ_2D = 9.0D;

    private final FaunaRebornPlugin plugin;

    private final Int2ObjectOpenHashMap<ChickenHostilityBrain> brains = new Int2ObjectOpenHashMap<>();
    private final Int2ObjectOpenHashMap<Chicken> trackedChickens = new Int2ObjectOpenHashMap<>();
    private final Vector scratchVelocity = new Vector();

    private BukkitTask task;
    private long currentTick;

    ChickenHostilityTask(FaunaRebornPlugin plugin) {
        this.plugin = plugin;
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

        List<Entity> nearby = chicken.getNearbyEntities(DETECTION_RADIUS, DETECTION_RADIUS, DETECTION_RADIUS);

        if (brain == null) {
            brain = new ChickenHostilityBrain(currentTick);
            brains.put(chickenId, brain);
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
        if (distSq <= ATTACK_RANGE_SQ) {
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

        if (failsSimplePathing(chicken, target, brain, distSq > ATTACK_RANGE_SQ)) {
            clearTargetAndIdle(brain);
            return;
        }

        move(chicken, target);

        if (distSq > ATTACK_RANGE_SQ) {
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
            if (distSq > DETECTION_RADIUS_SQ || distSq >= bestDist) continue;

            best = player;
            bestDist = distSq;

            if (distSq <= ATTACK_RANGE_SQ) break;
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

        return selfAlreadyAssigned || attackers < MAX_SIMULTANEOUS_ATTACKERS_PER_TARGET;
    }

    private boolean isValidTarget(Chicken chicken, Player player) {
        if (player == null) return false;
        if (!player.isOnline() || player.isDead()) return false;
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) return false;
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

    private void move(Chicken chicken, Player player) {
        double dx = player.getX() - chicken.getX();
        double dz = player.getZ() - chicken.getZ();
        double lenSq = dx * dx + dz * dz;

        if (lenSq < 0.0001D) return;

        double invLen = SPEED / Math.sqrt(lenSq);

        scratchVelocity.setX(dx * invLen);
        scratchVelocity.setY(chicken.getVelocity().getY());
        scratchVelocity.setZ(dz * invLen);

        chicken.setVelocity(scratchVelocity);
    }

    private void attack(Chicken chicken, Player player, ChickenHostilityBrain brain) {
        if (brain.lastAttackTick != Long.MIN_VALUE && currentTick - brain.lastAttackTick < COOLDOWN_TICKS) return;

        player.damage(2.0D, chicken);
        brain.lastAttackTick = currentTick;
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
            trackedChickens.put(chicken.getEntityId(), chicken);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private void onChunkLoad(ChunkLoadEvent event) {
        for (Entity entity : event.getChunk().getEntities()) {
            if (entity instanceof Chicken chicken) {
                trackedChickens.put(chicken.getEntityId(), chicken);
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
}
