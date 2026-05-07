package io.github.devskycore.faunareborn.animal.cow;

import io.github.devskycore.faunareborn.core.FaunaRebornPlugin;
import io.github.devskycore.faunareborn.module.FaunaModule;
import io.github.devskycore.faunareborn.animal.cow.hostility.CowMilkProvocationTask;

public final class CowModule implements FaunaModule {

    private final CowSettings settings;
    private final boolean globalEnabled;
    private CowMilkProvocationTask milkProvocationTask;

    public CowModule(FaunaRebornPlugin plugin, CowSettings settings, boolean globalEnabled) {
        this.settings = settings;
        this.globalEnabled = globalEnabled;
        if (settings.milkProvocation().enabled()) {
            this.milkProvocationTask = new CowMilkProvocationTask(
                    plugin,
                    settings.milkProvocation(),
                    settings.socialAlert(),
                    settings.globalHostility()
            );
        }
    }

    @Override
    public String id() {
        return "cow";
    }

    @Override
    public boolean isEnabledByConfig() {
        return globalEnabled && settings.enabled() && settings.milkProvocation().enabled();
    }

    @Override
    public void enable() {
        if (!settings.milkProvocation().enabled()) {
            return;
        }
        if (milkProvocationTask == null) {
            throw new IllegalStateException("Cow milk provocation task was not initialized.");
        }
        milkProvocationTask.start();
    }

    @Override
    public void disable() {
        if (milkProvocationTask != null) {
            milkProvocationTask.stop();
        }
    }
}
