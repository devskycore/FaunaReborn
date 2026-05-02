package io.github.devskycore.faunareborn.config.world;

import io.github.devskycore.faunareborn.core.FaunaRebornPlugin;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Locale;

public final class WorldFilterConfigReader {

    private final FaunaRebornPlugin plugin;

    public WorldFilterConfigReader(FaunaRebornPlugin plugin) {
        this.plugin = plugin;
    }

    public WorldFilter readWorldFilter(FileConfiguration config) {
        final String filterRoot = "chicken-hostility.world-filter";

        if (config.isConfigurationSection(filterRoot)) {
            WorldFilterMode mode = parseWorldFilterMode(config.getString(filterRoot + ".mode"));
            return new WorldFilter(mode, WorldFilter.normalizeWorldNames(config.getStringList(filterRoot + ".worlds")));
        }

        boolean hasEnabledLegacy = config.contains("chicken-hostility.enabled-worlds");
        boolean hasDisabledLegacy = config.contains("chicken-hostility.disabled-worlds");

        if (hasEnabledLegacy) {
            if (hasDisabledLegacy) {
                plugin.getLogger().warning(
                        "Both chicken-hostility.enabled-worlds and chicken-hostility.disabled-worlds are defined. " +
                                "Migrating to world-filter.mode=WHITELIST and ignoring disabled-worlds."
                );
            }

            return new WorldFilter(
                    WorldFilterMode.WHITELIST,
                    WorldFilter.normalizeWorldNames(config.getStringList("chicken-hostility.enabled-worlds"))
            );
        }

        if (hasDisabledLegacy) {
            return new WorldFilter(
                    WorldFilterMode.BLACKLIST,
                    WorldFilter.normalizeWorldNames(config.getStringList("chicken-hostility.disabled-worlds"))
            );
        }

        return new WorldFilter(WorldFilterMode.ALL, java.util.Set.of());
    }

    private WorldFilterMode parseWorldFilterMode(String rawMode) {
        if (rawMode == null || rawMode.trim().isEmpty()) {
            return WorldFilterMode.ALL;
        }

        try {
            return WorldFilterMode.valueOf(rawMode.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            plugin.getLogger().warning("Invalid chicken-hostility.world-filter.mode in config.yml. Falling back to ALL.");
            return WorldFilterMode.ALL;
        }
    }
}

