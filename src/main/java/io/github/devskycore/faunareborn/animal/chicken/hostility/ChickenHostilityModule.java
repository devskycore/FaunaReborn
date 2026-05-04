package io.github.devskycore.faunareborn.animal.chicken.hostility;

import io.github.devskycore.faunareborn.animal.chicken.config.ChickenHostilitySettings;
import io.github.devskycore.faunareborn.core.FaunaRebornPlugin;
import io.github.devskycore.faunareborn.module.FaunaModule;

public final class ChickenHostilityModule implements FaunaModule {

    private final ChickenHostilityTask task;
    private final ChickenHostilitySettings settings;
    private final boolean globalEnabled;

    public ChickenHostilityModule(FaunaRebornPlugin plugin, ChickenHostilitySettings settings, boolean globalEnabled) {
        this.settings = settings;
        this.globalEnabled = globalEnabled;
        this.task = new ChickenHostilityTask(plugin, settings);
    }

    @Override
    public String id() {
        return "chicken-hostility";
    }

    @Override
    public boolean isEnabledByConfig() {
        return globalEnabled && settings.enabled();
    }

    @Override
    public void enable() {
        task.start();
    }

    @Override
    public void disable() {
        task.stop();
    }
}

