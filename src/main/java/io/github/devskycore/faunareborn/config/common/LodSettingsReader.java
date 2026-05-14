package io.github.devskycore.faunareborn.config.common;

import io.github.devskycore.faunareborn.core.FaunaRebornPlugin;
import io.github.devskycore.faunareborn.system.lod.LodSettings;
import org.bukkit.configuration.file.FileConfiguration;

public final class LodSettingsReader {

    private final ConfigNumbers numbers;

    public LodSettingsReader(FaunaRebornPlugin plugin) {
        this.numbers = new ConfigNumbers(plugin);
    }

    public LodSettings read(FileConfiguration config, String rootPath) {
        String root = rootPath == null || rootPath.isBlank() ? "lod" : rootPath;
        boolean enabled = config.getBoolean(root + ".enabled", true);
        double highDistance = numbers.finiteRange(
                config.getDouble(root + ".distances.high", 14.0D),
                1.0D,
                256.0D,
                14.0D,
                "Invalid " + root + ".distances.high in config.yml. Falling back to 14.0",
                root + ".distances.high is too high. Clamped to 256.0"
        );
        double mediumDistance = numbers.finiteRange(
                config.getDouble(root + ".distances.medium", 28.0D),
                1.0D,
                256.0D,
                28.0D,
                "Invalid " + root + ".distances.medium in config.yml. Falling back to 28.0",
                root + ".distances.medium is too high. Clamped to 256.0"
        );
        double lowDistance = numbers.finiteRange(
                config.getDouble(root + ".distances.low", 44.0D),
                1.0D,
                256.0D,
                44.0D,
                "Invalid " + root + ".distances.low in config.yml. Falling back to 44.0",
                root + ".distances.low is too high. Clamped to 256.0"
        );
        double hysteresisDistance = numbers.finiteRange(
                config.getDouble(root + ".hysteresis-distance", 3.0D),
                0.0D,
                64.0D,
                3.0D,
                "Invalid " + root + ".hysteresis-distance in config.yml. Falling back to 3.0",
                root + ".hysteresis-distance is too high. Clamped to 64.0"
        );

        int highInterval = numbers.intRange(
                config.getInt(root + ".interval-ticks.high", 1),
                1,
                200,
                1,
                "Invalid " + root + ".interval-ticks.high in config.yml. Falling back to 1",
                root + ".interval-ticks.high is too high. Clamped to 200"
        );
        int mediumInterval = numbers.intRange(
                config.getInt(root + ".interval-ticks.medium", 2),
                1,
                400,
                2,
                "Invalid " + root + ".interval-ticks.medium in config.yml. Falling back to 2",
                root + ".interval-ticks.medium is too high. Clamped to 400"
        );
        int lowInterval = numbers.intRange(
                config.getInt(root + ".interval-ticks.low", 5),
                1,
                600,
                5,
                "Invalid " + root + ".interval-ticks.low in config.yml. Falling back to 5",
                root + ".interval-ticks.low is too high. Clamped to 600"
        );
        int offInterval = numbers.intRange(
                config.getInt(root + ".interval-ticks.off", 10),
                1,
                1200,
                10,
                "Invalid " + root + ".interval-ticks.off in config.yml. Falling back to 10",
                root + ".interval-ticks.off is too high. Clamped to 1200"
        );

        return new LodSettings(
                enabled,
                highDistance,
                mediumDistance,
                lowDistance,
                hysteresisDistance,
                highInterval,
                mediumInterval,
                lowInterval,
                offInterval
        );
    }
}
