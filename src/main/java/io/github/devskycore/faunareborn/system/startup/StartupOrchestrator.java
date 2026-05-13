package io.github.devskycore.faunareborn.system.startup;

import io.github.devskycore.faunareborn.config.PluginConfigManager;
import io.github.devskycore.faunareborn.config.PluginSettings;
import io.github.devskycore.faunareborn.combat.deathmessage.HostilityDeathMessageListener;
import io.github.devskycore.faunareborn.core.FaunaRebornPlugin;
import io.github.devskycore.faunareborn.module.FaunaFeatureRegistry;
import io.github.devskycore.faunareborn.module.ModuleManager;
import io.github.devskycore.faunareborn.system.platform.RuntimePlatform;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.PluginManager;

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
            if (RuntimePlatform.isFolia()) {
                plugin.getLogger().severe(
                        "Folia runtime detected. This build uses cross-entity global AI loops and is not safe for Folia thread ownership."
                );
                plugin.getLogger().severe(
                        "Use a Folia-specific build with region/entity scheduler architecture before enabling hostility modules."
                );
                if (disablePluginOnFailure) {
                    disablePluginSafely();
                }
                return false;
            }

            FaunaFeatureRegistry featureRegistry = FaunaFeatureRegistry.defaults();
            PluginSettings settings = new PluginConfigManager(plugin, featureRegistry.createSettingsLoaders(plugin)).load();
            ModuleManager moduleManager = new ModuleManager(
                    plugin,
                    featureRegistry.createModules(plugin, settings)
            );
            moduleManager.enableAll();
            if (plugin.deathMessageListener() != null) {
                HandlerList.unregisterAll(plugin.deathMessageListener());
            }
            HostilityDeathMessageListener deathMessageListener = new HostilityDeathMessageListener();
            plugin.getServer().getPluginManager().registerEvents(deathMessageListener, plugin);
            plugin.setDeathMessageListener(deathMessageListener);
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
