package io.github.devskycore.faunareborn.animal.chicken.config;

import io.github.devskycore.faunareborn.config.common.ConfigNumbers;
import io.github.devskycore.faunareborn.core.FaunaRebornPlugin;
import org.bukkit.configuration.file.FileConfiguration;

public final class ProcessingLimitsConfigReader {

    private final FaunaRebornPlugin plugin;
    private final ConfigNumbers numbers;

    public ProcessingLimitsConfigReader(FaunaRebornPlugin plugin) {
        this.plugin = plugin;
        this.numbers = new ConfigNumbers(plugin);
    }

    public ProcessingLimitsConfigValues read(FileConfiguration config) {
        FileConfiguration globalConfig = plugin.getConfig();
        int maxActivePerChunk = numbers.intRange(
                globalConfig.getInt(
                        "max-active-hostile-chickens-per-chunk",
                        PluginConfigDefaults.MAX_ACTIVE_HOSTILE_CHICKENS_PER_CHUNK
                ),
                1,
                PluginConfigDefaults.MAX_ACTIVE_HOSTILE_CHICKENS_PER_CHUNK_LIMIT,
                PluginConfigDefaults.MAX_ACTIVE_HOSTILE_CHICKENS_PER_CHUNK,
                "Invalid max-active-hostile-chickens-per-chunk in config.yml. Falling back to 8",
                "max-active-hostile-chickens-per-chunk is too high. Clamped to 128"
        );
        int maxActivePerWorld = numbers.intRange(
                globalConfig.getInt(
                        "max-active-hostile-chickens-per-world",
                        PluginConfigDefaults.MAX_ACTIVE_HOSTILE_CHICKENS_PER_WORLD
                ),
                1,
                PluginConfigDefaults.MAX_ACTIVE_HOSTILE_CHICKENS_PER_WORLD_LIMIT,
                PluginConfigDefaults.MAX_ACTIVE_HOSTILE_CHICKENS_PER_WORLD,
                "Invalid max-active-hostile-chickens-per-world in config.yml. Falling back to 250",
                "max-active-hostile-chickens-per-world is too high. Clamped to 5000"
        );
        int maxProcessedPerTick = numbers.intRange(
                globalConfig.getInt(
                        "max-processed-chickens-per-tick",
                        PluginConfigDefaults.MAX_PROCESSED_CHICKENS_PER_TICK
                ),
                1,
                PluginConfigDefaults.MAX_PROCESSED_CHICKENS_PER_TICK_LIMIT,
                PluginConfigDefaults.MAX_PROCESSED_CHICKENS_PER_TICK,
                "Invalid max-processed-chickens-per-tick in config.yml. Falling back to 300",
                "max-processed-chickens-per-tick is too high. Clamped to 1000"
        );

        return new ProcessingLimitsConfigValues(maxActivePerChunk, maxActivePerWorld, maxProcessedPerTick);
    }
}





