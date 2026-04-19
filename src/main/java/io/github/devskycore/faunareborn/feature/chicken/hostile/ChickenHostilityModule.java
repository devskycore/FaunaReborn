package io.github.devskycore.faunareborn.feature.chicken.hostile;

import io.github.devskycore.faunareborn.core.FaunaRebornPlugin;

public final class ChickenHostilityModule {

    private final ChickenHostilityTask task;

    public ChickenHostilityModule(FaunaRebornPlugin plugin) {
        this.task = new ChickenHostilityTask(plugin);
    }

    public void enable() {
        task.start();
    }

    public void disable() {
        task.stop();
    }
}
