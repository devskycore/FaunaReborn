package io.github.devskycore.faunareborn.animal.cow.hostility;

import org.bukkit.entity.Cow;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

final class CowTargetingIndex {

    private final Map<UUID, Integer> attackersByTarget = new HashMap<>();
    private final Map<UUID, Integer> activeByWorld = new HashMap<>();
    private final Map<WorldChunkKey, Integer> activeByChunk = new HashMap<>();

    void clear() {
        attackersByTarget.clear();
        activeByWorld.clear();
        activeByChunk.clear();
    }

    int attackersForTarget(UUID targetId) {
        return targetId == null ? 0 : attackersByTarget.getOrDefault(targetId, 0);
    }

    int activeInWorld(UUID worldId) {
        return worldId == null ? 0 : activeByWorld.getOrDefault(worldId, 0);
    }

    int activeInChunk(Cow cow) {
        if (cow == null) {
            return 0;
        }
        return activeByChunk.getOrDefault(chunkKey(cow), 0);
    }

    void registerActive(Cow cow, UUID targetUuid) {
        if (cow == null) {
            return;
        }
        increment(activeByWorld, cow.getWorld().getUID());
        increment(activeByChunk, chunkKey(cow));
        increment(attackersByTarget, targetUuid);
    }

    void unregisterActive(Cow cow, UUID targetUuid) {
        if (cow == null) {
            return;
        }
        decrement(activeByWorld, cow.getWorld().getUID());
        decrement(activeByChunk, chunkKey(cow));
        decrement(attackersByTarget, targetUuid);
    }

    void replaceTarget(UUID previousTarget, UUID nextTarget) {
        if (previousTarget != null && previousTarget.equals(nextTarget)) {
            return;
        }
        decrement(attackersByTarget, previousTarget);
        increment(attackersByTarget, nextTarget);
    }

    private static WorldChunkKey chunkKey(Cow cow) {
        return new WorldChunkKey(cow.getWorld().getUID(), cow.getChunk().getX(), cow.getChunk().getZ());
    }

    private static <K> void increment(Map<K, Integer> map, K key) {
        if (key != null) {
            map.merge(key, 1, Integer::sum);
        }
    }

    private static <K> void decrement(Map<K, Integer> map, K key) {
        if (key == null) {
            return;
        }
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
