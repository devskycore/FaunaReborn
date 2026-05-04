package io.github.devskycore.faunareborn.animal.chicken.config.night;

import io.github.devskycore.faunareborn.animal.chicken.config.PluginConfigDefaults;
import io.github.devskycore.faunareborn.core.FaunaRebornPlugin;
import org.bukkit.configuration.file.FileConfiguration;

public final class NightBehaviorConfigReader {

    private final FaunaRebornPlugin plugin;

    public NightBehaviorConfigReader(FaunaRebornPlugin plugin) {
        this.plugin = plugin;
    }

    public NightBehaviorConfigValues read(FileConfiguration config) {
        FileConfiguration globalConfig = plugin.getConfig();
        String root = "night-behavior";

        boolean nightBehaviorEnabled = globalConfig.getBoolean(root + ".enabled", true);
        boolean nightDamageToggleEnabled = globalConfig.getBoolean(root + ".damage.enabled", true);
        boolean nightDamageEnabled = nightBehaviorEnabled && nightDamageToggleEnabled;
        double nightDamageMultiplier = readNightDamageMultiplier(globalConfig, root);
        return new NightBehaviorConfigValues(nightDamageEnabled, nightDamageMultiplier);
    }

    private double readNightDamageMultiplier(FileConfiguration config, String root) {
        final String path = root + ".damage.multiplier";
        double configured = config.getDouble(path, PluginConfigDefaults.NIGHT_DAMAGE_MULTIPLIER);
        if (Double.isNaN(configured) || Double.isInfinite(configured)) {
            plugin.getLogger().warning("Invalid " + path + " in config.yml. Falling back to 1.2");
            return PluginConfigDefaults.NIGHT_DAMAGE_MULTIPLIER;
        }
        if (configured < PluginConfigDefaults.MIN_NIGHT_DAMAGE_MULTIPLIER) {
            plugin.getLogger().warning(path + " is too low. Clamped to 1.0");
            return PluginConfigDefaults.MIN_NIGHT_DAMAGE_MULTIPLIER;
        }
        if (configured > PluginConfigDefaults.MAX_NIGHT_DAMAGE_MULTIPLIER) {
            plugin.getLogger().warning(path + " is too high. Clamped to 1.5");
            return PluginConfigDefaults.MAX_NIGHT_DAMAGE_MULTIPLIER;
        }
        return configured;
    }
}


