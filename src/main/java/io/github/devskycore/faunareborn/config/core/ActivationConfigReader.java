package io.github.devskycore.faunareborn.config.core;

import io.github.devskycore.faunareborn.core.FaunaRebornPlugin;
import org.bukkit.configuration.file.FileConfiguration;

final class ActivationConfigReader {

    private final FaunaRebornPlugin plugin;

    ActivationConfigReader(FaunaRebornPlugin plugin) {
        this.plugin = plugin;
    }

    ActivationConfig read(FileConfiguration config) {
        double chance = sanitizeActivationChance(
                config.getDouble("chicken-hostility.activation.chance", PluginConfigDefaults.ACTIVATION_CHANCE)
        );
        boolean onlyNaturalChickens = config.getBoolean(
                "chicken-hostility.activation.only-natural-chickens",
                PluginConfigDefaults.ONLY_NATURAL_CHICKENS
        );
        boolean ignoreNamed = config.getBoolean(
                "chicken-hostility.activation.ignore-named",
                PluginConfigDefaults.IGNORE_NAMED
        );
        return new ActivationConfig(chance, onlyNaturalChickens, ignoreNamed);
    }

    private double sanitizeActivationChance(double configuredChance) {
        if (Double.isNaN(configuredChance) || Double.isInfinite(configuredChance)) {
            plugin.getLogger().warning("Invalid chicken-hostility.activation.chance in config.yml. Falling back to 1.0");
            return PluginConfigDefaults.ACTIVATION_CHANCE;
        }

        if (configuredChance < 0.0D) {
            plugin.getLogger().warning("chicken-hostility.activation.chance is too low. Clamped to 0.0");
            return 0.0D;
        }

        if (configuredChance > 1.0D) {
            plugin.getLogger().warning("chicken-hostility.activation.chance is too high. Clamped to 1.0");
            return 1.0D;
        }

        return configuredChance;
    }
}

