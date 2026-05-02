package io.github.devskycore.faunareborn.config.core;

import io.github.devskycore.faunareborn.core.FaunaRebornPlugin;
import org.bukkit.configuration.file.FileConfiguration;

public final class ProcessingLimitsConfigReader {

    private final ConfigNumbers numbers;

    public ProcessingLimitsConfigReader(FaunaRebornPlugin plugin) {
        this.numbers = new ConfigNumbers(plugin);
    }

    public ProcessingLimitsConfigValues read(FileConfiguration config) {
        int maxActivePerChunk = numbers.intRange(
                config.getInt(
                        "chicken-hostility.max-active-hostile-chickens-per-chunk",
                        PluginConfigDefaults.MAX_ACTIVE_HOSTILE_CHICKENS_PER_CHUNK
                ),
                1,
                PluginConfigDefaults.MAX_ACTIVE_HOSTILE_CHICKENS_PER_CHUNK_LIMIT,
                PluginConfigDefaults.MAX_ACTIVE_HOSTILE_CHICKENS_PER_CHUNK,
                "Invalid chicken-hostility.max-active-hostile-chickens-per-chunk in config.yml. Falling back to 8",
                "chicken-hostility.max-active-hostile-chickens-per-chunk is too high. Clamped to 128"
        );
        int maxActivePerWorld = numbers.intRange(
                config.getInt(
                        "chicken-hostility.max-active-hostile-chickens-per-world",
                        PluginConfigDefaults.MAX_ACTIVE_HOSTILE_CHICKENS_PER_WORLD
                ),
                1,
                PluginConfigDefaults.MAX_ACTIVE_HOSTILE_CHICKENS_PER_WORLD_LIMIT,
                PluginConfigDefaults.MAX_ACTIVE_HOSTILE_CHICKENS_PER_WORLD,
                "Invalid chicken-hostility.max-active-hostile-chickens-per-world in config.yml. Falling back to 250",
                "chicken-hostility.max-active-hostile-chickens-per-world is too high. Clamped to 5000"
        );
        int maxProcessedPerTick = numbers.intRange(
                config.getInt(
                        "chicken-hostility.max-processed-chickens-per-tick",
                        PluginConfigDefaults.MAX_PROCESSED_CHICKENS_PER_TICK
                ),
                1,
                PluginConfigDefaults.MAX_PROCESSED_CHICKENS_PER_TICK_LIMIT,
                PluginConfigDefaults.MAX_PROCESSED_CHICKENS_PER_TICK,
                "Invalid chicken-hostility.max-processed-chickens-per-tick in config.yml. Falling back to 300",
                "chicken-hostility.max-processed-chickens-per-tick is too high. Clamped to 1000"
        );

        return new ProcessingLimitsConfigValues(maxActivePerChunk, maxActivePerWorld, maxProcessedPerTick);
    }
}

