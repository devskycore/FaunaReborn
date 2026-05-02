package io.github.devskycore.faunareborn.core;

import io.github.devskycore.faunareborn.command.FaunaCommand;
import io.github.devskycore.faunareborn.system.lifecycle.PluginBanner;
import io.github.devskycore.faunareborn.system.shutdown.ShutdownOrchestrator;
import io.github.devskycore.faunareborn.system.startup.StartupOrchestrator;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class FaunaRebornPlugin extends JavaPlugin {

    private Runnable chickenHostilityEnableHook;
    private Runnable chickenHostilityDisableHook;

    @Override
    public void onEnable() {
        final long startedAt = System.nanoTime();

        boolean startupOk = new StartupOrchestrator(this).run();
        if (!startupOk || !isEnabled()) {
            return;
        }

        registerCommands();
        PluginBanner.printEnable(this, startedAt);
    }

    @Override
    public void onDisable() {
        final long startedAt = System.nanoTime();

        new ShutdownOrchestrator(this).run();

        PluginBanner.printDisable(this, startedAt);
    }

    public void setChickenHostilityHooks(Runnable onEnable, Runnable onDisable) {
        this.chickenHostilityEnableHook = onEnable;
        this.chickenHostilityDisableHook = onDisable;
    }

    public void clearChickenHostilityHooks() {
        this.chickenHostilityEnableHook = null;
        this.chickenHostilityDisableHook = null;
    }

    public void enableChickenHostility() {
        if (chickenHostilityEnableHook != null) {
            chickenHostilityEnableHook.run();
        }
    }

    public void disableChickenHostility() {
        if (chickenHostilityDisableHook != null) {
            chickenHostilityDisableHook.run();
        }
    }

    private void registerCommands() {
        PluginCommand faunaCommand = getCommand("fauna");
        if (faunaCommand == null) {
            getLogger().severe("Command 'fauna' is not defined in paper-plugin.yml.");
            return;
        }

        FaunaCommand executor = new FaunaCommand(this);
        faunaCommand.setExecutor(executor);
        faunaCommand.setTabCompleter(executor);
    }
}
