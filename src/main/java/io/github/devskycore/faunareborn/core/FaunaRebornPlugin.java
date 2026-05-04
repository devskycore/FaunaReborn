package io.github.devskycore.faunareborn.core;

import io.github.devskycore.faunareborn.command.FaunaCommand;
import io.github.devskycore.faunareborn.module.ModuleManager;
import io.github.devskycore.faunareborn.system.lifecycle.PluginBanner;
import io.github.devskycore.faunareborn.system.lifecycle.PluginLifecycleLogger;
import io.github.devskycore.faunareborn.system.shutdown.ShutdownOrchestrator;
import io.github.devskycore.faunareborn.system.startup.StartupOrchestrator;
import org.bukkit.plugin.java.JavaPlugin;

public final class FaunaRebornPlugin extends JavaPlugin {

    private ModuleManager moduleManager;

    @Override
    public void onEnable() {
        final long startedAt = System.nanoTime();

        boolean startupOk = new StartupOrchestrator(this).run();
        if (!startupOk || !isEnabled()) {
            return;
        }

        registerCommands();
        PluginBanner.printEnable(this);
        PluginLifecycleLogger.onEnable(this, startedAt);
    }

    @Override
    public void onDisable() {
        final long startedAt = System.nanoTime();

        new ShutdownOrchestrator(this).run();

        PluginBanner.printDisable(this);
        PluginLifecycleLogger.onDisable(this, startedAt);
    }

    public ModuleManager moduleManager() {
        return moduleManager;
    }

    public void setModuleManager(ModuleManager moduleManager) {
        this.moduleManager = moduleManager;
    }

    private void registerCommands() {
        registerCommand(
                "fauna",
                "Main command for FaunaReborn.",
                java.util.List.of("faunareborn"),
                new FaunaCommand(this)
        );
    }
}
