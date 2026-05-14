package io.github.devskycore.faunareborn.animal.pig;

import io.github.devskycore.faunareborn.core.FaunaRebornPlugin;
import io.github.devskycore.faunareborn.module.FaunaModule;
import io.github.devskycore.faunareborn.animal.pig.hostility.PigProvocationTask;

public final class PigModule implements FaunaModule {

    private final PigSettings settings;
    private final boolean globalEnabled;
    private PigProvocationTask rodProvocationTask;

    public PigModule(FaunaRebornPlugin plugin, PigSettings settings, boolean globalEnabled) {
        this.settings = settings;
        this.globalEnabled = globalEnabled;
        if (settings.rodProvocation().enabled()) {
            this.rodProvocationTask = new PigProvocationTask(
                    plugin,
                    settings.rodProvocation(),
                    settings.resourceProvocation(),
                    settings.socialAlert(),
                    settings.globalHostility(),
                    settings.environmentAggression(),
                    settings.lod()
            );
        }
    }

    @Override
    public String id() {
        return "pig";
    }

    @Override
    public boolean isEnabledByConfig() {
        return globalEnabled
                && settings.enabled()
                && (settings.rodProvocation().enabled() || settings.resourceProvocation().enabled());
    }

    @Override
    public void enable() {
        if (!settings.rodProvocation().enabled() && !settings.resourceProvocation().enabled()) {
            return;
        }
        if (rodProvocationTask == null) {
            throw new IllegalStateException("Pig rod provocation task was not initialized.");
        }
        rodProvocationTask.start();
    }

    @Override
    public void disable() {
        if (rodProvocationTask != null) {
            rodProvocationTask.stop();
        }
    }
}


