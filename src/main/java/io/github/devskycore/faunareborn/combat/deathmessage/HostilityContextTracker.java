package io.github.devskycore.faunareborn.combat.deathmessage;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class HostilityContextTracker {

    private static final long CONTEXT_TTL_MS = 30_000L;
    private static final Map<UUID, ContextEntry> ENTRIES = new ConcurrentHashMap<>();

    private HostilityContextTracker() {
    }

    public static void record(UUID playerId, HostileSpecies species, HostilityCause cause) {
        if (playerId == null || species == null || cause == null) {
            return;
        }
        ENTRIES.put(playerId, new ContextEntry(species, cause, System.currentTimeMillis()));
    }

    public static ContextEntry find(UUID playerId) {
        if (playerId == null) {
            return null;
        }
        ContextEntry entry = ENTRIES.get(playerId);
        if (entry == null) {
            return null;
        }
        if (System.currentTimeMillis() - entry.recordedAtMs() > CONTEXT_TTL_MS) {
            ENTRIES.remove(playerId);
            return null;
        }
        return entry;
    }

    public static void clear(UUID playerId) {
        if (playerId != null) {
            ENTRIES.remove(playerId);
        }
    }

    public record ContextEntry(HostileSpecies species, HostilityCause cause, long recordedAtMs) {
    }
}
