package io.github.devskycore.faunareborn.config;

import io.github.devskycore.faunareborn.animal.cow.CowSettings;
import io.github.devskycore.faunareborn.config.entity.EntitySettings;
import io.github.devskycore.faunareborn.config.entity.EntitySettingsRegistry;

import java.util.Objects;

public record PluginSettings(
        GlobalSettings global,
        EntitySettingsRegistry entities
) {

    public PluginSettings {
        Objects.requireNonNull(global, "global");
        Objects.requireNonNull(entities, "entities");
    }

    public <T extends EntitySettings> T require(Class<T> settingsType) {
        return entities.require(settingsType);
    }

    public CowSettings cow() {
        return require(CowSettings.class);
    }
}
