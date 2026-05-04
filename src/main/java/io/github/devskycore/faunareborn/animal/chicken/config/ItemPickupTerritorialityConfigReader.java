package io.github.devskycore.faunareborn.animal.chicken.config;

import io.github.devskycore.faunareborn.config.common.ConfigNumbers;
import io.github.devskycore.faunareborn.core.FaunaRebornPlugin;
import org.bukkit.configuration.file.FileConfiguration;

public final class ItemPickupTerritorialityConfigReader {

    private final ConfigNumbers numbers;

    public ItemPickupTerritorialityConfigReader(FaunaRebornPlugin plugin) {
        this.numbers = new ConfigNumbers(plugin);
    }

    public ItemPickupTerritorialityConfig read(FileConfiguration config) {
        int eggThreshold = readThreshold(
                config,
                "egg-threshold",
                PluginConfigDefaults.ITEM_PICKUP_EGG_THRESHOLD
        );
        int featherThreshold = readThreshold(
                config,
                "feather-threshold",
                PluginConfigDefaults.ITEM_PICKUP_FEATHER_THRESHOLD
        );
        int rawChickenThreshold = readThreshold(
                config,
                "raw-chicken-threshold",
                PluginConfigDefaults.ITEM_PICKUP_RAW_CHICKEN_THRESHOLD
        );
        double detectionRadius = numbers.finiteRange(
                config.getDouble(
                        "chicken-hostility.item-pickup-territoriality.detection-radius",
                        PluginConfigDefaults.ITEM_PICKUP_DETECTION_RADIUS
                ),
                1.0D,
                PluginConfigDefaults.MAX_ITEM_PICKUP_DETECTION_RADIUS,
                PluginConfigDefaults.ITEM_PICKUP_DETECTION_RADIUS,
                "Invalid chicken-hostility.item-pickup-territoriality.detection-radius in config.yml. Falling back to 8.0",
                "chicken-hostility.item-pickup-territoriality.detection-radius is too high. Clamped to 32.0"
        );
        double timeWindowSeconds = numbers.finiteRange(
                config.getDouble(
                        "chicken-hostility.item-pickup-territoriality.time-window",
                        PluginConfigDefaults.ITEM_PICKUP_TIME_WINDOW_SECONDS
                ),
                0.05D,
                PluginConfigDefaults.MAX_TIMER_SECONDS,
                PluginConfigDefaults.ITEM_PICKUP_TIME_WINDOW_SECONDS,
                "Invalid chicken-hostility.item-pickup-territoriality.time-window in config.yml. Falling back to 12.0 seconds",
                "chicken-hostility.item-pickup-territoriality.time-window is too high. Clamped to 86400 seconds"
        );
        double aggressionDurationSeconds = numbers.finiteRange(
                config.getDouble(
                        "chicken-hostility.item-pickup-territoriality.aggression-duration",
                        PluginConfigDefaults.ITEM_PICKUP_AGGRESSION_DURATION_SECONDS
                ),
                0.0D,
                PluginConfigDefaults.MAX_TIMER_SECONDS,
                PluginConfigDefaults.ITEM_PICKUP_AGGRESSION_DURATION_SECONDS,
                "Invalid chicken-hostility.item-pickup-territoriality.aggression-duration in config.yml. Falling back to 10.0 seconds",
                "chicken-hostility.item-pickup-territoriality.aggression-duration is too high. Clamped to 86400 seconds"
        );
        double maxItemAgeSeconds = numbers.finiteRange(
                config.getDouble(
                        "chicken-hostility.item-pickup-territoriality.max-item-age",
                        PluginConfigDefaults.ITEM_PICKUP_MAX_ITEM_AGE_SECONDS
                ),
                0.0D,
                PluginConfigDefaults.MAX_TIMER_SECONDS,
                PluginConfigDefaults.ITEM_PICKUP_MAX_ITEM_AGE_SECONDS,
                "Invalid chicken-hostility.item-pickup-territoriality.max-item-age in config.yml. Falling back to 120.0 seconds",
                "chicken-hostility.item-pickup-territoriality.max-item-age is too high. Clamped to 86400 seconds"
        );
        double nightThresholdMultiplier = numbers.finiteRange(
                config.getDouble(
                        "chicken-hostility.item-pickup-territoriality.night.threshold-multiplier",
                        PluginConfigDefaults.ITEM_PICKUP_NIGHT_THRESHOLD_MULTIPLIER
                ),
                PluginConfigDefaults.MIN_ITEM_PICKUP_NIGHT_THRESHOLD_MULTIPLIER,
                PluginConfigDefaults.MAX_ITEM_PICKUP_NIGHT_THRESHOLD_MULTIPLIER,
                PluginConfigDefaults.ITEM_PICKUP_NIGHT_THRESHOLD_MULTIPLIER,
                "Invalid chicken-hostility.item-pickup-territoriality.night.threshold-multiplier in config.yml. Falling back to 0.75",
                "chicken-hostility.item-pickup-territoriality.night.threshold-multiplier is too high. Clamped to 4.0"
        );

        return new ItemPickupTerritorialityConfig(
                config.getBoolean(
                        "chicken-hostility.item-pickup-territoriality.enabled",
                        PluginConfigDefaults.ITEM_PICKUP_TERRITORIALITY_ENABLED
                ),
                eggThreshold,
                featherThreshold,
                rawChickenThreshold,
                detectionRadius,
                numbers.toTicks(timeWindowSeconds),
                numbers.toNonNegativeTicks(aggressionDurationSeconds),
                numbers.toNonNegativeTicks(maxItemAgeSeconds),
                config.getBoolean(
                        "chicken-hostility.item-pickup-territoriality.night.enabled",
                        PluginConfigDefaults.ITEM_PICKUP_NIGHT_MODIFIER_ENABLED
                ),
                nightThresholdMultiplier,
                config.getBoolean(
                        "chicken-hostility.item-pickup-territoriality.social-propagation.enabled",
                        PluginConfigDefaults.ITEM_PICKUP_SOCIAL_PROPAGATION_ENABLED
                )
        );
    }

    private int readThreshold(FileConfiguration config, String key, int fallback) {
        String path = "chicken-hostility.item-pickup-territoriality.thresholds." + key;
        return numbers.intRange(
                config.getInt(path, fallback),
                1,
                PluginConfigDefaults.ITEM_PICKUP_THRESHOLD_LIMIT,
                fallback,
                "Invalid " + path + " in config.yml. Falling back to " + fallback,
                path + " is too high. Clamped to 1024"
        );
    }
}




