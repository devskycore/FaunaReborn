package io.github.devskycore.faunareborn.animal.chicken.hostility;

import org.bukkit.entity.Chicken;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

final class TargetingIndex {

    private final Map<UUID, Integer> attackersByTarget = new HashMap<>();
    private final Map<UUID, Integer> activeByWorld = new HashMap<>();
    private final Map<WorldChunkKey, Integer> activeByChunk = new HashMap<>();

    void clear() {
        attackersByTarget.clear();
        activeByWorld.clear();
        activeByChunk.clear();
    }

    int attackersForTarget(UUID targetId) {
        return attackersByTarget.getOrDefault(targetId, 0);
    }

    int activeInWorld(UUID worldId) {
        return activeByWorld.getOrDefault(worldId, 0);
    }

    int activeInChunk(Chicken chicken) {
        return activeByChunk.getOrDefault(chunkKey(chicken), 0);
    }

    void registerActive(Chicken chicken, UUID targetUuid) {
        if (chicken == null) {
            return;
        }
        UUID worldId = chicken.getWorld().getUID();
        increment(activeByWorld, worldId);
        increment(activeByChunk, chunkKey(chicken));
        if (targetUuid != null) {
            increment(attackersByTarget, targetUuid);
        }
    }

    void unregisterActive(Chicken chicken, UUID targetUuid) {
        if (chicken == null) {
            return;
        }
        UUID worldId = chicken.getWorld().getUID();
        decrement(activeByWorld, worldId);
        decrement(activeByChunk, chunkKey(chicken));
        if (targetUuid != null) {
            decrement(attackersByTarget, targetUuid);
        }
    }

    void replaceTarget(UUID previousTarget, UUID nextTarget) {
        if (previousTarget != null) {
            decrement(attackersByTarget, previousTarget);
        }
        increment(attackersByTarget, nextTarget);
    }

    private static WorldChunkKey chunkKey(Chicken chicken) {
        return new WorldChunkKey(chicken.getWorld().getUID(), chicken.getChunk().getX(), chicken.getChunk().getZ());
    }

    private static <K> void increment(Map<K, Integer> map, K key) {
        map.merge(key, 1, Integer::sum);
    }

    private static <K> void decrement(Map<K, Integer> map, K key) {
        Integer current = map.get(key);
        if (current == null || current <= 1) {
            map.remove(key);
            return;
        }
        map.put(key, current - 1);
    }

    private record WorldChunkKey(UUID worldId, int chunkX, int chunkZ) {
    }
}
