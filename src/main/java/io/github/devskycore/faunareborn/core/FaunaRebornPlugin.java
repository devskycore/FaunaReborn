package io.github.devskycore.faunareborn.core;

import io.github.devskycore.faunareborn.command.FaunaCommand;
import io.github.devskycore.faunareborn.command.FaunaReloadService;
import io.github.devskycore.faunareborn.combat.deathmessage.HostilityDeathMessageListener;
import io.github.devskycore.faunareborn.config.entity.EntityType;
import io.github.devskycore.faunareborn.gui.EntityModuleToggle;
import io.github.devskycore.faunareborn.gui.FaunaMainGui;
import io.github.devskycore.faunareborn.gui.PluginGuiConfigService;
import io.github.devskycore.faunareborn.module.ModuleManager;
import io.github.devskycore.faunareborn.system.lifecycle.PluginBanner;
import io.github.devskycore.faunareborn.system.lifecycle.PluginLifecycleLogger;
import io.github.devskycore.faunareborn.system.shutdown.ShutdownOrchestrator;
import io.github.devskycore.faunareborn.system.startup.StartupOrchestrator;
import org.bukkit.Material;
import org.bukkit.plugin.java.JavaPlugin;

public final class FaunaRebornPlugin extends JavaPlugin {

    private ModuleManager moduleManager;
    private HostilityDeathMessageListener deathMessageListener;
    private FaunaReloadService reloadService;

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

    public HostilityDeathMessageListener deathMessageListener() {
        return deathMessageListener;
    }

    public void setDeathMessageListener(HostilityDeathMessageListener deathMessageListener) {
        this.deathMessageListener = deathMessageListener;
    }

    public FaunaReloadService reloadService() {
        if (reloadService == null) {
            reloadService = new FaunaReloadService(this);
        }
        return reloadService;
    }

    private void registerCommands() {
        PluginGuiConfigService guiConfigService = createGuiConfigService();
        FaunaMainGui mainGui = new FaunaMainGui(this, guiConfigService, reloadService());
        getServer().getPluginManager().registerEvents(mainGui, this);
        registerCommand(
                "fauna",
                "Main command for FaunaReborn.",
                java.util.List.of("faunareborn", "fr"),
                new FaunaCommand(this, mainGui, guiConfigService)
        );
    }

    private PluginGuiConfigService createGuiConfigService() {
        return new PluginGuiConfigService(
                this,
                java.util.List.of(
                        new EntityModuleToggle(EntityType.CHICKEN, "chicken-hostility", "Chicken Hostility", "chicken-hostility.enabled", Material.CHICKEN_SPAWN_EGG),
                        new EntityModuleToggle(EntityType.COW, "cow", "Cow Hostility", "cow.enabled", Material.COW_SPAWN_EGG),
                        new EntityModuleToggle(EntityType.PIG, "pig", "Pig Hostility", "pig.enabled", Material.PIG_SPAWN_EGG)
                )
        );
    }
}
