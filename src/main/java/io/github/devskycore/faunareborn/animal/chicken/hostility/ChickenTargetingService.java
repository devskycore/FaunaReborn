package io.github.devskycore.faunareborn.animal.chicken.hostility;

import io.github.devskycore.faunareborn.animal.chicken.config.ChickenHostilitySettings;
import io.github.devskycore.faunareborn.core.FaunaRebornPlugin;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.entity.Chicken;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

final class ChickenTargetingService {

    private final FaunaRebornPlugin plugin;
    private final ChickenTracker tracker;
    private final ActivationPolicy activationPolicy;
    private final TargetingIndex targetingIndex = new TargetingIndex();
    private final Object2LongOpenHashMap<UUID> globalTargetCooldownUntil = new Object2LongOpenHashMap<>();
    private final int globalTargetCooldownTicks;
    private final int maxSimultaneousAttackersPerTarget;
    private final int maxActiveHostileChickensPerChunk;
    private final int maxActiveHostileChickensPerWorld;
    private final double detectionRadiusSq;
    private final double attackRangeSq;

    ChickenTargetingService(
            FaunaRebornPlugin plugin,
            ChickenTracker tracker,
            ActivationPolicy activationPolicy,
            ChickenHostilitySettings.Combat combat,
            ChickenHostilitySettings.Limits limits
    ) {
        this.plugin = plugin;
        this.tracker = tracker;
        this.activationPolicy = activationPolicy;
        this.globalTargetCooldownTicks = combat.globalTargetCooldownTicks();
        this.maxSimultaneousAttackersPerTarget = combat.maxSimultaneousAttackersPerPlayer();
        this.maxActiveHostileChickensPerChunk = limits.maxActiveHostileChickensPerChunk();
        this.maxActiveHostileChickensPerWorld = limits.maxActiveHostileChickensPerWorld();
        this.detectionRadiusSq = combat.detectionRadius() * combat.detectionRadius();
        this.attackRangeSq = combat.attackRange() * combat.attackRange();
        this.globalTargetCooldownUntil.defaultReturnValue(Long.MIN_VALUE);
    }

    double attackRangeSq() {
        return attackRangeSq;
    }

    int maxSimultaneousAttackersPerTarget() {
        return maxSimultaneousAttackersPerTarget;
    }

    void clear() {
        globalTargetCooldownUntil.clear();
        targetingIndex.clear();
    }

    void removeGlobalCooldown(UUID targetId) {
        globalTargetCooldownUntil.removeLong(targetId);
    }

    void cleanupGlobalTargetCooldowns(long currentTick) {
        if (globalTargetCooldownUntil.isEmpty()) {
            return;
        }
        globalTargetCooldownUntil.object2LongEntrySet().removeIf(entry -> currentTick >= entry.getLongValue());
    }

    void registerActiveTracking(Chicken chicken, UUID targetUuid) {
        targetingIndex.registerActive(chicken, targetUuid);
    }

    void unregisterActiveTracking(Chicken chicken, UUID targetUuid) {
        targetingIndex.unregisterActive(chicken, targetUuid);
    }

    void replaceActiveTarget(UUID previousTarget, UUID nextTarget) {
        targetingIndex.replaceTarget(previousTarget, nextTarget);
    }

    void applyGlobalTargetCooldown(UUID targetUuid, long currentTick) {
        if (targetUuid != null && globalTargetCooldownTicks > 0) {
            globalTargetCooldownUntil.put(targetUuid, currentTick + globalTargetCooldownTicks);
        }
    }

    Player resolveTarget(Chicken chicken, UUID targetUuid) {
        if (targetUuid == null) return null;

        Player player = plugin.getServer().getPlayer(targetUuid);
        if (activationPolicy.isInvalidTarget(chicken, player)) return null;

        return player;
    }

    Player findTarget(Chicken chicken, ChickenHostilityBrain brain, List<Entity> nearby, int chickenId, long currentTick) {
        if (isAreaActivationBlocked(chicken, chickenId)) {
            return null;
        }

        Player best = null;
        double bestDist = Double.MAX_VALUE;

        for (Entity entity : nearby) {
            if (!(entity instanceof Player player)) continue;
            if (activationPolicy.isInvalidTarget(chicken, player)) continue;
            if (brain != null && isRetargetBlocked(brain, player.getUniqueId(), currentTick)) continue;
            if (isOnGlobalTargetCooldown(player.getUniqueId(), chickenId, currentTick)) continue;
            if (isAggressorSlotUnavailable(player.getUniqueId(), chickenId)) continue;

            double distSq = HostilityDistances.distanceSq(chicken, player);
            if (distSq > detectionRadiusSq || distSq >= bestDist) continue;

            best = player;
            bestDist = distSq;

            if (distSq <= attackRangeSq) break;
        }

        return best;
    }

    boolean isAreaActivationBlocked(Chicken chicken, int selfChickenId) {
        Chunk chunk = chicken.getChunk();
        World world = chicken.getWorld();
        int worldActives = targetingIndex.activeInWorld(world.getUID());
        int chunkActives = targetingIndex.activeInChunk(chicken);

        ChickenHostilityBrain selfBrain = tracker.brain(selfChickenId);
        Chicken selfChicken = tracker.chicken(selfChickenId);
        if (selfBrain != null
                && selfBrain.state != ChickenHostilityState.IDLE
                && selfChicken != null
                && selfChicken.isValid()
                && !selfChicken.isDead()
                && selfChicken.getWorld() == world) {
            worldActives = Math.max(0, worldActives - 1);
            Chunk selfChunk = selfChicken.getChunk();
            if (selfChunk.getX() == chunk.getX() && selfChunk.getZ() == chunk.getZ()) {
                chunkActives = Math.max(0, chunkActives - 1);
            }
        }
        return worldActives >= maxActiveHostileChickensPerWorld || chunkActives >= maxActiveHostileChickensPerChunk;
    }

    boolean isAggressorSlotUnavailable(UUID targetUuid, int selfChickenId) {
        ChickenHostilityBrain selfBrain = tracker.brain(selfChickenId);
        boolean selfAlreadyAssigned = selfBrain != null
                && selfBrain.state != ChickenHostilityState.IDLE
                && targetUuid.equals(selfBrain.targetUuid);
        if (selfAlreadyAssigned) {
            return false;
        }
        int attackers = targetingIndex.attackersForTarget(targetUuid);
        return attackers >= maxSimultaneousAttackersPerTarget;
    }

    boolean isOnGlobalTargetCooldown(UUID targetUuid, int selfChickenId, long currentTick) {
        ChickenHostilityBrain selfBrain = tracker.brain(selfChickenId);
        if (selfBrain != null && targetUuid.equals(selfBrain.targetUuid)) {
            return false;
        }

        long untilTick = globalTargetCooldownUntil.getLong(targetUuid);
        return currentTick < untilTick;
    }

    boolean isRetargetBlocked(ChickenHostilityBrain brain, UUID candidateTargetUuid, long currentTick) {
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
}
