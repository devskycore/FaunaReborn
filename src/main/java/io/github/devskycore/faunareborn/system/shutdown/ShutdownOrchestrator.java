package io.github.devskycore.faunareborn.system.shutdown;

import io.github.devskycore.faunareborn.core.FaunaRebornPlugin;
import io.github.devskycore.faunareborn.module.ModuleManager;

public final class ShutdownOrchestrator {

    private final FaunaRebornPlugin plugin;

    public ShutdownOrchestrator(FaunaRebornPlugin plugin) {
        this.plugin = plugin;
    }

    public void run() {
        ModuleManager moduleManager = plugin.moduleManager();
        if (moduleManager == null) {
            return;
        }
        moduleManager.disableAll();
        plugin.setModuleManager(null);
    }
}
