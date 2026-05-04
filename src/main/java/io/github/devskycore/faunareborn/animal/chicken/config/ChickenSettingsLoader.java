package io.github.devskycore.faunareborn.animal.chicken.config;

import io.github.devskycore.faunareborn.config.entity.EntitySettingsLoader;
import io.github.devskycore.faunareborn.config.entity.EntityType;
import io.github.devskycore.faunareborn.core.FaunaRebornPlugin;
import org.bukkit.configuration.file.FileConfiguration;

public final class ChickenSettingsLoader implements EntitySettingsLoader<ChickenHostilitySettings> {

    private final ChickenHostilitySettingsLoader delegate;

    public ChickenSettingsLoader(FaunaRebornPlugin plugin) {
        this.delegate = new ChickenHostilitySettingsLoader(plugin);
    }

    @Override
    public EntityType entityType() {
        return EntityType.CHICKEN;
    }

    @Override
    public Class<ChickenHostilitySettings> settingsType() {
        return ChickenHostilitySettings.class;
    }

    @Override
    public ChickenHostilitySettings load(FileConfiguration config) {
        return delegate.load(config);
    }
}
