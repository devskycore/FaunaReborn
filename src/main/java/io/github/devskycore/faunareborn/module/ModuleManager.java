package io.github.devskycore.faunareborn.module;

import io.github.devskycore.faunareborn.core.FaunaRebornPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.logging.Level;

public final class ModuleManager {

    private final FaunaRebornPlugin plugin;
    private final List<FaunaModule> modules;
    private final Map<String, FaunaModule> modulesById = new HashMap<>();
    private final List<FaunaModule> enabledModules = new ArrayList<>();

    public ModuleManager(FaunaRebornPlugin plugin, List<FaunaModule> modules) {
        this.plugin = plugin;
        this.modules = List.copyOf(modules);
        for (FaunaModule module : this.modules) {
            modulesById.put(module.id(), module);
        }
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

    public boolean setModuleEnabled(String moduleId, boolean enabled) {
        FaunaModule module = modulesById.get(moduleId);
        if (module == null) {
            return false;
        }

        boolean currentlyEnabled = enabledModules.contains(module);
        if (enabled == currentlyEnabled) {
            return true;
        }

        if (enabled) {
            try {
                module.enable();
                enabledModules.add(module);
                return true;
            } catch (Throwable throwable) {
                plugin.getLogger().log(Level.SEVERE, "Module enable failed for '" + module.id() + "'.", throwable);
                return false;
            }
        }

        try {
            module.disable();
        } catch (Throwable throwable) {
            plugin.getLogger().log(Level.SEVERE, "Module disable failed for '" + module.id() + "'.", throwable);
            return false;
        }
        enabledModules.remove(module);
        return true;
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
