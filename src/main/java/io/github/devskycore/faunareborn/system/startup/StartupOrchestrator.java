package io.github.devskycore.faunareborn.system.startup;

import io.github.devskycore.faunareborn.animal.chicken.hostility.ChickenHostilityModule;
import io.github.devskycore.faunareborn.animal.cow.CowModule;
import io.github.devskycore.faunareborn.config.PluginConfigManager;
import io.github.devskycore.faunareborn.config.PluginSettings;
import io.github.devskycore.faunareborn.core.FaunaRebornPlugin;
import io.github.devskycore.faunareborn.module.ModuleManager;
import org.bukkit.plugin.PluginManager;

import java.util.List;
import java.util.logging.Level;

public final class StartupOrchestrator {

    private final FaunaRebornPlugin plugin;
    private final boolean disablePluginOnFailure;

    public StartupOrchestrator(FaunaRebornPlugin plugin) {
        this(plugin, true);
    }

    public StartupOrchestrator(FaunaRebornPlugin plugin, boolean disablePluginOnFailure) {
        this.plugin = plugin;
        this.disablePluginOnFailure = disablePluginOnFailure;
    }

    public boolean run() {
        try {
            PluginSettings settings = new PluginConfigManager(plugin).load();
            ModuleManager moduleManager = new ModuleManager(
                    plugin,
                    List.of(
                            new ChickenHostilityModule(plugin, settings.chickenHostility(), settings.global().globalEnabled()),
                            new CowModule(plugin, settings.cow(), settings.global().globalEnabled())
                    )
            );
            moduleManager.enableAll();
            plugin.setModuleManager(moduleManager);
            return true;
        } catch (Throwable throwable) {
            plugin.getLogger().log(Level.SEVERE, "Failed to prepare plugin startup.", throwable);
            if (disablePluginOnFailure) {
                disablePluginSafely();
            }
            return false;
        }
    }

    private void disablePluginSafely() {
        PluginManager pluginManager = plugin.getServer().getPluginManager();
        if (plugin.isEnabled()) {
            pluginManager.disablePlugin(plugin);
        }
    }
}
