package io.github.devskycore.faunareborn.feature.chicken.hostile;

import io.github.devskycore.faunareborn.config.core.ChickenHostilitySettings;
import io.github.devskycore.faunareborn.core.FaunaRebornPlugin;

public final class ChickenHostilityModule {

    private final ChickenHostilityTask task;

    public ChickenHostilityModule(FaunaRebornPlugin plugin, ChickenHostilitySettings settings) {
        this.task = new ChickenHostilityTask(plugin, settings);
    }

    public void enable() {
        task.start();
    }

    public void disable() {
        task.stop();
    }
}
