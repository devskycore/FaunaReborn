package io.github.devskycore.faunareborn.config;

import io.github.devskycore.faunareborn.config.core.ChickenHostilitySettings;
import io.github.devskycore.faunareborn.config.core.ChickenHostilitySettingsLoader;
import io.github.devskycore.faunareborn.config.core.PluginSettings;
import io.github.devskycore.faunareborn.core.FaunaRebornPlugin;
import org.bukkit.configuration.file.FileConfiguration;

public final class PluginConfigManager {

    private final FaunaRebornPlugin plugin;
    private final ChickenHostilitySettingsLoader chickenHostilitySettingsLoader;

    public PluginConfigManager(FaunaRebornPlugin plugin) {
        this.plugin = plugin;
        this.chickenHostilitySettingsLoader = new ChickenHostilitySettingsLoader(plugin);
    }

    public PluginSettings load() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();

        FileConfiguration config = plugin.getConfig();
        ChickenHostilitySettings chickenHostilitySettings = chickenHostilitySettingsLoader.load(config);
        return new PluginSettings(chickenHostilitySettings);
    }
}

