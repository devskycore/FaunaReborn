package io.github.devskycore.faunareborn.animal.common.hostility;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.bukkit.entity.Entity;

import java.util.UUID;

public class SharedTargetingIndex {

    private final Object2IntOpenHashMap<UUID> attackersByTarget = new Object2IntOpenHashMap<>();
    private final Object2IntOpenHashMap<UUID> activeByWorld = new Object2IntOpenHashMap<>();
    private final Object2ObjectOpenHashMap<UUID, Long2IntOpenHashMap> activeByChunkByWorld = new Object2ObjectOpenHashMap<>();

    public SharedTargetingIndex() {
        attackersByTarget.defaultReturnValue(0);
        activeByWorld.defaultReturnValue(0);
    }

    public void clear() {
        attackersByTarget.clear();
        activeByWorld.clear();
        activeByChunkByWorld.clear();
    }

    public int attackersForTarget(UUID targetId) {
        return targetId == null ? 0 : attackersByTarget.getInt(targetId);
    }

    public int activeInWorld(UUID worldId) {
        return worldId == null ? 0 : activeByWorld.getInt(worldId);
    }

    public int activeInChunk(Entity entity) {
        if (entity == null) {
            return 0;
        }
        UUID worldId = entity.getWorld().getUID();
        Long2IntOpenHashMap activeByChunk = activeByChunkByWorld.get(worldId);
        if (activeByChunk == null) {
            return 0;
        }
        return activeByChunk.get(chunkKey(entity.getChunk().getX(), entity.getChunk().getZ()));
    }

    public void registerActive(Entity entity, UUID targetUuid) {
        if (entity == null) {
            return;
        }
        UUID worldId = entity.getWorld().getUID();
        increment(activeByWorld, worldId);
        incrementChunk(worldId, entity.getChunk().getX(), entity.getChunk().getZ());
        increment(attackersByTarget, targetUuid);
    }

    public void unregisterActive(Entity entity, UUID targetUuid) {
        if (entity == null) {
            return;
        }
        UUID worldId = entity.getWorld().getUID();
        decrement(activeByWorld, worldId);
        decrementChunk(worldId, entity.getChunk().getX(), entity.getChunk().getZ());
        decrement(attackersByTarget, targetUuid);
    }

    public void replaceTarget(UUID previousTarget, UUID nextTarget) {
        if (previousTarget != null && previousTarget.equals(nextTarget)) {
            return;
        }
        decrement(attackersByTarget, previousTarget);
        increment(attackersByTarget, nextTarget);
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
