package io.github.devskycore.faunareborn.config;

import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class WorldFilter {

    private final WorldFilterMode mode;
    private final Set<String> worlds;

    public WorldFilter(WorldFilterMode mode, Set<String> worlds) {
        this.mode = mode == null ? WorldFilterMode.ALL : mode;
        this.worlds = worlds == null ? Set.of() : Set.copyOf(worlds);
    }

    public WorldFilterMode mode() {
        return mode;
    }

    public Set<String> worlds() {
        return worlds;
    }

    public boolean isWorldAllowed(String worldName) {
        if (mode == WorldFilterMode.ALL) {
            return true;
        }
        if (worldName == null || worldName.trim().isEmpty()) {
            return mode != WorldFilterMode.WHITELIST;
        }

        String normalizedName = normalize(worldName);
        boolean listed = worlds.contains(normalizedName);
        return switch (mode) {
            case WHITELIST -> listed;
            case BLACKLIST -> !listed;
            case ALL -> true;
        };
    }

    public static Set<String> normalizeWorldNames(List<String> worlds) {
        if (worlds == null || worlds.isEmpty()) {
            return Set.of();
        }
        return worlds.stream()
                .filter(name -> name != null && !name.trim().isEmpty())
                .map(WorldFilter::normalize)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    private static String normalize(String worldName) {
        return worldName.trim().toLowerCase(Locale.ROOT);
    }
}
