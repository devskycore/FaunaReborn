package io.github.devskycore.faunareborn.animal.chicken.hostility;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.bukkit.entity.Chicken;

import java.util.UUID;

final class TargetingIndex {

    private final Object2IntOpenHashMap<UUID> attackersByTarget = new Object2IntOpenHashMap<>();
    private final Object2IntOpenHashMap<UUID> activeByWorld = new Object2IntOpenHashMap<>();
    private final Object2ObjectOpenHashMap<UUID, Long2IntOpenHashMap> activeByChunkByWorld = new Object2ObjectOpenHashMap<>();

    TargetingIndex() {
        attackersByTarget.defaultReturnValue(0);
        activeByWorld.defaultReturnValue(0);
    }

    void clear() {
        attackersByTarget.clear();
        activeByWorld.clear();
        activeByChunkByWorld.clear();
    }

    int attackersForTarget(UUID targetId) {
        if (targetId == null) {
            return 0;
        }
        return attackersByTarget.getInt(targetId);
    }

    int activeInWorld(UUID worldId) {
        return worldId == null ? 0 : activeByWorld.getInt(worldId);
    }

    int activeInChunk(Chicken chicken) {
        if (chicken == null) {
            return 0;
        }
        UUID worldId = chicken.getWorld().getUID();
        Long2IntOpenHashMap activeByChunk = activeByChunkByWorld.get(worldId);
        if (activeByChunk == null) {
            return 0;
        }
        return activeByChunk.get(chunkKey(chicken.getChunk().getX(), chicken.getChunk().getZ()));
    }

    void registerActive(Chicken chicken, UUID targetUuid) {
        if (chicken == null) {
            return;
        }
        UUID worldId = chicken.getWorld().getUID();
        increment(activeByWorld, worldId);
        incrementChunk(worldId, chicken.getChunk().getX(), chicken.getChunk().getZ());
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
        decrementChunk(worldId, chicken.getChunk().getX(), chicken.getChunk().getZ());
        if (targetUuid != null) {
            decrement(attackersByTarget, targetUuid);
        }
    }

    void replaceTarget(UUID previousTarget, UUID nextTarget) {
        if (previousTarget != null && previousTarget.equals(nextTarget)) {
            return;
        }
        if (previousTarget != null) {
            decrement(attackersByTarget, previousTarget);
        }
        if (nextTarget != null) {
            increment(attackersByTarget, nextTarget);
        }
    }

    private void incrementChunk(UUID worldId, int chunkX, int chunkZ) {
        Long2IntOpenHashMap activeByChunk = activeByChunkByWorld.get(worldId);
        if (activeByChunk == null) {
            activeByChunk = new Long2IntOpenHashMap();
            activeByChunk.defaultReturnValue(0);
            activeByChunkByWorld.put(worldId, activeByChunk);
        }
        activeByChunk.addTo(chunkKey(chunkX, chunkZ), 1);
    }

    private void decrementChunk(UUID worldId, int chunkX, int chunkZ) {
        Long2IntOpenHashMap activeByChunk = activeByChunkByWorld.get(worldId);
        if (activeByChunk == null) {
            return;
        }
        long chunkKey = chunkKey(chunkX, chunkZ);
        int current = activeByChunk.get(chunkKey);
        if (current <= 1) {
            activeByChunk.remove(chunkKey);
            if (activeByChunk.isEmpty()) {
                activeByChunkByWorld.remove(worldId);
            }
            return;
        }
        activeByChunk.put(chunkKey, current - 1);
    }

    private static void increment(Object2IntOpenHashMap<UUID> map, UUID key) {
        if (key != null) {
            map.addTo(key, 1);
        }
    }

    private static void decrement(Object2IntOpenHashMap<UUID> map, UUID key) {
        if (key == null) {
            return;
        }
        int current = map.getInt(key);
        if (current <= 1) {
            map.removeInt(key);
            return;
        }
        map.put(key, current - 1);
    }

    private static long chunkKey(int chunkX, int chunkZ) {
        return ((long) chunkX << 32) ^ (chunkZ & 0xFFFF_FFFFL);
    }
}
