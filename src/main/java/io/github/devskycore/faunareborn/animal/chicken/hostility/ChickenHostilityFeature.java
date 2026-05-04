package io.github.devskycore.faunareborn.animal.chicken.hostility;

import io.github.devskycore.faunareborn.animal.chicken.config.ChickenHostilitySettings;
import io.github.devskycore.faunareborn.animal.chicken.config.ChickenSettingsLoader;
import io.github.devskycore.faunareborn.config.GlobalSettings;
import io.github.devskycore.faunareborn.config.entity.EntitySettingsLoader;
import io.github.devskycore.faunareborn.core.FaunaRebornPlugin;
import io.github.devskycore.faunareborn.module.FaunaFeature;
import io.github.devskycore.faunareborn.module.FaunaModule;

public final class ChickenHostilityFeature implements FaunaFeature<ChickenHostilitySettings> {

    @Override
    public Class<ChickenHostilitySettings> settingsType() {
        return ChickenHostilitySettings.class;
    }

    @Override
    public EntitySettingsLoader<ChickenHostilitySettings> createSettingsLoader(FaunaRebornPlugin plugin) {
        return new ChickenSettingsLoader(plugin);
    }

    @Override
    public FaunaModule createModule(
            FaunaRebornPlugin plugin,
            ChickenHostilitySettings settings,
            GlobalSettings globalSettings
    ) {
        return new ChickenHostilityModule(plugin, settings, globalSettings.globalEnabled());
    }
}
