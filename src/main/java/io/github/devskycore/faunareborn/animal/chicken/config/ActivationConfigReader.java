package io.github.devskycore.faunareborn.animal.chicken.config;

import io.github.devskycore.faunareborn.core.FaunaRebornPlugin;
import org.bukkit.configuration.file.FileConfiguration;

final class ActivationConfigReader {

    private final FaunaRebornPlugin plugin;

    ActivationConfigReader(FaunaRebornPlugin plugin) {
        this.plugin = plugin;
    }

    ActivationConfig read(FileConfiguration config) {
        String root = "activation";

        double chance = sanitizeActivationChance(
                config.getDouble(root + ".chance", PluginConfigDefaults.ACTIVATION_CHANCE)
        );
        boolean onlyNaturalChickens = readNaturalSpawnsOnly(config, root);
        boolean ignoreNamed = config.getBoolean(
                root + ".ignore-named",
                PluginConfigDefaults.IGNORE_NAMED
        );
        return new ActivationConfig(chance, onlyNaturalChickens, ignoreNamed);
    }

    private boolean readNaturalSpawnsOnly(FileConfiguration config, String root) {
        String currentPath = root + ".natural-spawns-only";
        if (config.isSet(currentPath)) {
            return config.getBoolean(currentPath, PluginConfigDefaults.ONLY_NATURAL_CHICKENS);
        }
        return config.getBoolean(root + ".only-natural", PluginConfigDefaults.ONLY_NATURAL_CHICKENS);
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
}


