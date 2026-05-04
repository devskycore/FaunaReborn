package io.github.devskycore.faunareborn.config.entity;

import java.util.HashMap;
import java.util.Map;

public final class EntitySettingsRegistry {

    private final Map<Class<? extends EntitySettings>, EntitySettings> settingsByType = new HashMap<>();

    public <T extends EntitySettings> void register(Class<T> settingsType, T settings) {
        settingsByType.put(settingsType, settings);
    }

    public <T extends EntitySettings> T require(Class<T> settingsType) {
        EntitySettings settings = settingsByType.get(settingsType);
        if (settings == null) {
            throw new IllegalStateException("Missing settings for " + settingsType.getSimpleName());
        }
        return settingsType.cast(settings);
    }
}
