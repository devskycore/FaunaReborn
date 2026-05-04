package io.github.devskycore.faunareborn.module;

import io.github.devskycore.faunareborn.config.GlobalSettings;
import io.github.devskycore.faunareborn.config.entity.EntitySettings;
import io.github.devskycore.faunareborn.config.entity.EntitySettingsLoader;
import io.github.devskycore.faunareborn.core.FaunaRebornPlugin;

public interface FaunaFeature<T extends EntitySettings> {

    Class<T> settingsType();

    EntitySettingsLoader<T> createSettingsLoader(FaunaRebornPlugin plugin);

    FaunaModule createModule(FaunaRebornPlugin plugin, T settings, GlobalSettings globalSettings);
}
