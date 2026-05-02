package io.github.devskycore.faunareborn.system.startup;

import io.github.devskycore.faunareborn.config.PluginConfigManager;
import io.github.devskycore.faunareborn.config.core.PluginSettings;
import io.github.devskycore.faunareborn.core.FaunaRebornPlugin;
import io.github.devskycore.faunareborn.feature.chicken.hostile.ChickenHostilityModule;
import org.bukkit.plugin.PluginManager;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.logging.Level;

public final class StartupOrchestrator {

    private final FaunaRebornPlugin plugin;
    private final boolean disablePluginOnFailure;
    private final List<StartupStep> steps = new ArrayList<>();

    public StartupOrchestrator(FaunaRebornPlugin plugin) {
        this(plugin, true);
    }

    public StartupOrchestrator(FaunaRebornPlugin plugin, boolean disablePluginOnFailure) {
        this.plugin = plugin;
        this.disablePluginOnFailure = disablePluginOnFailure;
    }

    public boolean run() {
        try {
            register();
        } catch (Throwable throwable) {
            plugin.getLogger().log(Level.SEVERE, "Failed to prepare plugin startup.", throwable);
            if (disablePluginOnFailure) {
                disablePluginSafely();
            }
            return false;
        }

        List<StartupStep> completedSteps = new ArrayList<>();
        for (StartupStep step : steps) {
            try {
                step.action().run();
                completedSteps.add(step);
            } catch (Throwable throwable) {
                plugin.getLogger().log(Level.SEVERE, "Startup failed at step '" + step.name() + "'.", throwable);
                rollback(completedSteps);
                if (disablePluginOnFailure) {
                    disablePluginSafely();
                }
                return false;
            }
        }

        return true;
    }

    private void register() {
        plugin.clearChickenHostilityHooks();

        PluginSettings settings = new PluginConfigManager(plugin).load();
        if (!settings.chickenHostility().enabled()) {
            plugin.getLogger().info("Chicken hostility system is disabled by config.");
            return;
        }

        ChickenHostilityModule module = new ChickenHostilityModule(plugin, settings.chickenHostility());
        plugin.setChickenHostilityHooks(module::enable, module::disable);
        steps.add(new StartupStep(
                "enable-chicken-hostility",
                plugin::enableChickenHostility,
                () -> {
                    plugin.disableChickenHostility();
                    plugin.clearChickenHostilityHooks();
                }
        ));
    }

    private void rollback(List<StartupStep> completedSteps) {
        ListIterator<StartupStep> reverse = completedSteps.listIterator(completedSteps.size());
        while (reverse.hasPrevious()) {
            StartupStep step = reverse.previous();
            try {
                step.rollback().run();
            } catch (Throwable rollbackError) {
                plugin.getLogger().log(Level.SEVERE, "Rollback failed for step '" + step.name() + "'.", rollbackError);
            }
        }
    }

    private void disablePluginSafely() {
        PluginManager pluginManager = plugin.getServer().getPluginManager();
        if (plugin.isEnabled()) {
            pluginManager.disablePlugin(plugin);
        }
    }

    private record StartupStep(String name, Runnable action, Runnable rollback) {
    }
}
