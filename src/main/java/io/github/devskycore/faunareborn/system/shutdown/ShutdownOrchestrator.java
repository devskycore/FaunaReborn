package io.github.devskycore.faunareborn.system.shutdown;

import io.github.devskycore.faunareborn.core.FaunaRebornPlugin;

import java.util.ArrayList;
import java.util.List;

public final class ShutdownOrchestrator {

    private final FaunaRebornPlugin plugin;
    private final List<Runnable> steps = new ArrayList<>();

    public ShutdownOrchestrator(FaunaRebornPlugin plugin) {
        this.plugin = plugin;
        register();
    }

    public void run() {
        steps.forEach(Runnable::run);
    }

    private void register() {
        steps.add(plugin::disableChickenHostility);
    }
}
