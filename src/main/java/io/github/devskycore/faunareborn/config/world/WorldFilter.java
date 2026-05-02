package io.github.devskycore.faunareborn.config.world;

import java.util.List;
import java.util.Locale;
import java.util.Set;

public record WorldFilter(WorldFilterMode mode, Set<String> worlds) {

    public WorldFilter {
        mode = mode == null ? WorldFilterMode.ALL : mode;
        worlds = worlds == null ? Set.of() : Set.copyOf(worlds);
    }

    public boolean isWorldAllowed(String worldName) {
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

