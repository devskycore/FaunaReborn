package io.github.devskycore.faunareborn.animal.chicken.config;

import io.github.devskycore.faunareborn.core.FaunaRebornPlugin;
import org.bukkit.configuration.file.FileConfiguration;

final class ActivationConfigReader {

    private final FaunaRebornPlugin plugin;

    ActivationConfigReader(FaunaRebornPlugin plugin) {
        this.plugin = plugin;
    }

    ActivationConfig read(FileConfiguration entityConfig, FileConfiguration globalConfig) {
        String root = "activation";

        double chance = sanitizeActivationChance(
                readDouble(entityConfig, globalConfig, root + ".chance", PluginConfigDefaults.ACTIVATION_CHANCE)
        );
        boolean onlyNaturalChickens = readNaturalSpawnsOnly(entityConfig, globalConfig, root);
        boolean ignoreNamed = readBoolean(entityConfig, globalConfig, root + ".ignore-named", PluginConfigDefaults.IGNORE_NAMED);
        int adultWithoutBabyGraceTicks = sanitizeAdultWithoutBabyGraceTicks(
                readDouble(
                        entityConfig,
                        globalConfig,
                        root + ".adult-without-baby-grace-seconds",
                        PluginConfigDefaults.ADULT_WITHOUT_BABY_GRACE_SECONDS
                )
        );
        return new ActivationConfig(chance, onlyNaturalChickens, ignoreNamed, adultWithoutBabyGraceTicks);
    }

    private boolean readNaturalSpawnsOnly(FileConfiguration entityConfig, FileConfiguration globalConfig, String root) {
        String currentPath = root + ".natural-spawns-only";
        if (entityConfig.isSet(currentPath)) {
            return entityConfig.getBoolean(currentPath, PluginConfigDefaults.ONLY_NATURAL_CHICKENS);
        }
        if (globalConfig.isSet(currentPath)) {
            return globalConfig.getBoolean(currentPath, PluginConfigDefaults.ONLY_NATURAL_CHICKENS);
        }
        String legacyPath = root + ".only-natural";
        if (entityConfig.isSet(legacyPath)) {
            return entityConfig.getBoolean(legacyPath, PluginConfigDefaults.ONLY_NATURAL_CHICKENS);
        }
        return globalConfig.getBoolean(legacyPath, PluginConfigDefaults.ONLY_NATURAL_CHICKENS);
    }

    private boolean readBoolean(FileConfiguration entityConfig, FileConfiguration globalConfig, String path, boolean defaultValue) {
        if (entityConfig.isSet(path)) {
            return entityConfig.getBoolean(path, defaultValue);
        }
        return globalConfig.getBoolean(path, defaultValue);
    }

    private double readDouble(FileConfiguration entityConfig, FileConfiguration globalConfig, String path, double defaultValue) {
        if (entityConfig.isSet(path)) {
            return entityConfig.getDouble(path, defaultValue);
        }
        return globalConfig.getDouble(path, defaultValue);
    }

    private double sanitizeActivationChance(double configuredChance) {
        if (Double.isNaN(configuredChance) || Double.isInfinite(configuredChance)) {
            plugin.getLogger().warning("Invalid activation.chance in config.yml. Falling back to 1.0");
            return PluginConfigDefaults.ACTIVATION_CHANCE;
        }

        if (configuredChance < 0.0D) {
            plugin.getLogger().warning("activation.chance is too low. Clamped to 0.0");
            return 0.0D;
        }

        if (configuredChance > 1.0D) {
            plugin.getLogger().warning("activation.chance is too high. Clamped to 1.0");
            return 1.0D;
        }

        return configuredChance;
    }

    private int sanitizeAdultWithoutBabyGraceTicks(double configuredSeconds) {
        if (Double.isNaN(configuredSeconds) || Double.isInfinite(configuredSeconds)) {
            plugin.getLogger().warning("Invalid activation.adult-without-baby-grace-seconds in config.yml. Falling back to 1200.0 seconds");
            configuredSeconds = PluginConfigDefaults.ADULT_WITHOUT_BABY_GRACE_SECONDS;
        }

        if (configuredSeconds < 0.0D) {
            plugin.getLogger().warning("activation.adult-without-baby-grace-seconds is too low. Clamped to 0.0");
            configuredSeconds = 0.0D;
        } else if (configuredSeconds > PluginConfigDefaults.MAX_TIMER_SECONDS) {
            plugin.getLogger().warning("activation.adult-without-baby-grace-seconds is too high. Clamped to 86400.0");
            configuredSeconds = PluginConfigDefaults.MAX_TIMER_SECONDS;
        }

        return (int) Math.ceil(configuredSeconds * 20.0D);
    }
}


