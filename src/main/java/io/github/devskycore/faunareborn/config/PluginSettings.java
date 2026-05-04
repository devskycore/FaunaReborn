package io.github.devskycore.faunareborn.config;

import io.github.devskycore.faunareborn.animal.chicken.config.ChickenHostilitySettings;
import io.github.devskycore.faunareborn.animal.cow.CowSettings;
import io.github.devskycore.faunareborn.config.entity.EntitySettings;
import io.github.devskycore.faunareborn.config.entity.EntitySettingsRegistry;

import java.util.Objects;

public record PluginSettings(
        GlobalSettings global,
        EntitySettingsRegistry entities
) {

    public PluginSettings {
        global = Objects.requireNonNull(global, "global");
        entities = Objects.requireNonNull(entities, "entities");
    }

    public <T extends EntitySettings> T require(Class<T> settingsType) {
        return entities.require(settingsType);
    }

    public ChickenHostilitySettings chickenHostility() {
        return require(ChickenHostilitySettings.class);
    }

    public CowSettings cow() {
        return require(CowSettings.class);
    }
}
