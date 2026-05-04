package io.github.devskycore.faunareborn.module;

import io.github.devskycore.faunareborn.core.FaunaRebornPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.logging.Level;

public final class ModuleManager {

    private final FaunaRebornPlugin plugin;
    private final List<FaunaModule> modules;
    private final List<FaunaModule> enabledModules = new ArrayList<>();

    public ModuleManager(FaunaRebornPlugin plugin, List<FaunaModule> modules) {
        this.plugin = plugin;
        this.modules = List.copyOf(modules);
    }

    public void enableAll() {
        List<FaunaModule> started = new ArrayList<>();
        try {
            for (FaunaModule module : modules) {
                if (!module.isEnabledByConfig()) {
                    plugin.getLogger().info("Module '" + module.id() + "' disabled by config.");
                    continue;
                }
                module.enable();
                started.add(module);
            }
            enabledModules.clear();
            enabledModules.addAll(started);
        } catch (Throwable throwable) {
            plugin.getLogger().log(Level.SEVERE, "Module startup failed. Rolling back started modules.", throwable);
            rollback(started);
            throw throwable;
        }
    }

    public void disableAll() {
        ListIterator<FaunaModule> reverse = enabledModules.listIterator(enabledModules.size());
        while (reverse.hasPrevious()) {
            FaunaModule module = reverse.previous();
            try {
                module.disable();
            } catch (Throwable throwable) {
                plugin.getLogger().log(Level.SEVERE, "Module shutdown failed for '" + module.id() + "'.", throwable);
            }
        }
        enabledModules.clear();
    }

    private void rollback(List<FaunaModule> started) {
        ListIterator<FaunaModule> reverse = started.listIterator(started.size());
        while (reverse.hasPrevious()) {
            FaunaModule module = reverse.previous();
            try {
                module.disable();
            } catch (Throwable rollbackError) {
                plugin.getLogger().log(Level.SEVERE, "Module rollback failed for '" + module.id() + "'.", rollbackError);
            }
        }
    }
}
