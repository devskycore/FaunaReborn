package io.github.devskycore.faunareborn.animal.cow;

import io.github.devskycore.faunareborn.config.GlobalSettings;
import io.github.devskycore.faunareborn.config.entity.EntitySettingsLoader;
import io.github.devskycore.faunareborn.core.FaunaRebornPlugin;
import io.github.devskycore.faunareborn.module.FaunaFeature;
import io.github.devskycore.faunareborn.module.FaunaModule;

public final class CowFeature implements FaunaFeature<CowSettings> {

    @Override
    public Class<CowSettings> settingsType() {
        return CowSettings.class;
    }

    @Override
    public EntitySettingsLoader<CowSettings> createSettingsLoader(FaunaRebornPlugin plugin) {
        return new CowSettingsLoader(plugin);
    }

    @Override
    public FaunaModule createModule(FaunaRebornPlugin plugin, CowSettings settings, GlobalSettings globalSettings) {
        return new CowModule(plugin, settings, globalSettings.globalEnabled());
    }
}
