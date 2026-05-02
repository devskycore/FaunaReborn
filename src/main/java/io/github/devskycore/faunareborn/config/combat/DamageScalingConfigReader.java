package io.github.devskycore.faunareborn.config.combat;

import io.github.devskycore.faunareborn.config.core.PluginConfigDefaults;
import io.github.devskycore.faunareborn.core.FaunaRebornPlugin;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class DamageScalingConfigReader {

    private final FaunaRebornPlugin plugin;

    public DamageScalingConfigReader(FaunaRebornPlugin plugin) {
        this.plugin = plugin;
    }

    public double readDifficultyDamageMultiplier(FileConfiguration config, org.bukkit.Difficulty difficulty, double defaultValue) {
        String path = "chicken-hostility.damage-scaling.difficulty-multipliers." + difficulty.name().toLowerCase(Locale.ROOT);
        double multiplier = config.getDouble(path, defaultValue);
        if (Double.isNaN(multiplier) || Double.isInfinite(multiplier) || multiplier < 0.0D) {
            plugin.getLogger().warning("Invalid " + path + " in config.yml. Falling back to " + defaultValue);
            return defaultValue;
        }
        if (multiplier > PluginConfigDefaults.MAX_DAMAGE_MULTIPLIER) {
            plugin.getLogger().warning(path + " is too high. Clamped to 10.0");
            return PluginConfigDefaults.MAX_DAMAGE_MULTIPLIER;
        }
        return multiplier;
    }

    public Map<String, Double> readWorldDamageMultipliers(FileConfiguration config) {
        String root = "chicken-hostility.damage-scaling.world-multipliers";
        ConfigurationSection section = config.getConfigurationSection(root);
        if (section == null) {
            return Map.of();
        }

        Map<String, Double> multipliers = new HashMap<>();
        for (String key : section.getKeys(false)) {
            if (key == null || key.trim().isEmpty()) {
                continue;
            }

            double value = section.getDouble(key, 1.0D);
            if (Double.isNaN(value) || Double.isInfinite(value) || value < 0.0D) {
                plugin.getLogger().warning("Invalid " + root + "." + key + " in config.yml. Skipping.");
                continue;
            }
            if (value > PluginConfigDefaults.MAX_DAMAGE_MULTIPLIER) {
                plugin.getLogger().warning(root + "." + key + " is too high. Clamped to 10.0");
                value = PluginConfigDefaults.MAX_DAMAGE_MULTIPLIER;
            }

            multipliers.put(key.trim().toLowerCase(Locale.ROOT), value);
        }

        return Map.copyOf(multipliers);
    }
}

