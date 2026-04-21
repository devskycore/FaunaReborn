package io.github.devskycore.faunareborn.system.startup;

import io.github.devskycore.faunareborn.config.PluginConfigManager;
import io.github.devskycore.faunareborn.config.PluginSettings;
import io.github.devskycore.faunareborn.core.FaunaRebornPlugin;
import io.github.devskycore.faunareborn.feature.chicken.hostile.ChickenHostilityModule;

import java.util.ArrayList;
import java.util.List;

public final class StartupOrchestrator {

    private final FaunaRebornPlugin plugin;
    private final List<Runnable> steps = new ArrayList<>();

    public StartupOrchestrator(FaunaRebornPlugin plugin) {
        this.plugin = plugin;
        register();
    }

    public void run() {
        steps.forEach(Runnable::run);
    }

    private void register() {
        PluginSettings settings = new PluginConfigManager(plugin).load();
        if (!settings.chickenHostility().enabled()) {
            plugin.getLogger().info("Chicken hostility system is disabled by config.");
            return;
        }

        ChickenHostilityModule module = new ChickenHostilityModule(plugin, settings.chickenHostility());
        plugin.setChickenHostilityHooks(module::enable, module::disable);
        steps.add(plugin::enableChickenHostility);
    }
}
