package io.github.devskycore.faunareborn.config.common;

import io.github.devskycore.faunareborn.core.FaunaRebornPlugin;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Locale;

public final class WorldFilterConfigReader {

    private final FaunaRebornPlugin plugin;

    public WorldFilterConfigReader(FaunaRebornPlugin plugin) {
        this.plugin = plugin;
    }

    public WorldFilter readWorldFilter(FileConfiguration config) {
        final String globalFilterRoot = "world-filter";
        if (config.isConfigurationSection(globalFilterRoot)) {
            WorldFilterMode mode = parseWorldFilterMode(config.getString(globalFilterRoot + ".mode"));
            return new WorldFilter(mode, WorldFilter.normalizeWorldNames(config.getStringList(globalFilterRoot + ".worlds")));
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
            plugin.getLogger().warning("Invalid world-filter.mode in config.yml. Falling back to ALL.");
            return WorldFilterMode.ALL;
        }
    }
}


