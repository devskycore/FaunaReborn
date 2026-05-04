package io.github.devskycore.faunareborn.animal.cow;

import io.github.devskycore.faunareborn.core.FaunaRebornPlugin;
import io.github.devskycore.faunareborn.module.FaunaModule;
import io.github.devskycore.faunareborn.animal.cow.hostility.CowMilkProvocationTask;

public final class CowModule implements FaunaModule {

    private final CowSettings settings;
    private final boolean globalEnabled;
    private final CowMilkProvocationTask milkProvocationTask;

    public CowModule(FaunaRebornPlugin plugin, CowSettings settings, boolean globalEnabled) {
        this.settings = settings;
        this.globalEnabled = globalEnabled;
        this.milkProvocationTask = new CowMilkProvocationTask(plugin, settings.milkProvocation(), settings.globalHostility());
    }

    @Override
    public String id() {
        return "cow";
    }

    @Override
    public boolean isEnabledByConfig() {
        return globalEnabled && settings.enabled();
    }

    @Override
    public void enable() {
        milkProvocationTask.start();
    }

    @Override
    public void disable() {
        milkProvocationTask.stop();
    }
}
