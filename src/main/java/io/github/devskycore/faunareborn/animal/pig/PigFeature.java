package io.github.devskycore.faunareborn.animal.pig;

import io.github.devskycore.faunareborn.config.GlobalSettings;
import io.github.devskycore.faunareborn.config.entity.EntitySettingsLoader;
import io.github.devskycore.faunareborn.core.FaunaRebornPlugin;
import io.github.devskycore.faunareborn.module.FaunaFeature;
import io.github.devskycore.faunareborn.module.FaunaModule;

public final class PigFeature implements FaunaFeature<PigSettings> {

    @Override
    public Class<PigSettings> settingsType() {
        return PigSettings.class;
    }

    @Override
    public EntitySettingsLoader<PigSettings> createSettingsLoader(FaunaRebornPlugin plugin) {
        return new PigSettingsLoader(plugin);
    }

    @Override
    public FaunaModule createModule(FaunaRebornPlugin plugin, PigSettings settings, GlobalSettings globalSettings) {
        return new PigModule(plugin, settings, globalSettings.globalEnabled());
    }
}

