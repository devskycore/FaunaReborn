package io.github.devskycore.faunareborn.core;

import io.github.devskycore.faunareborn.system.lifecycle.PluginLifecycleLogger;
import io.github.devskycore.faunareborn.system.shutdown.ShutdownOrchestrator;
import io.github.devskycore.faunareborn.system.startup.StartupOrchestrator;
import org.bukkit.plugin.java.JavaPlugin;

public final class FaunaRebornPlugin extends JavaPlugin {
    @Override
    public void onEnable() {
        final long startedAt = System.nanoTime();

        StartupOrchestrator startupOrchestrator = new StartupOrchestrator(this);
        startupOrchestrator.run();

        PluginLifecycleLogger.onEnable(this, startedAt);
    }

    @Override
    public void onDisable() {
        final long startedAt = System.nanoTime();

        ShutdownOrchestrator shutdownOrchestrator = new ShutdownOrchestrator(this);
        shutdownOrchestrator.run();

        PluginLifecycleLogger.onDisable(this, startedAt);
    }
}
