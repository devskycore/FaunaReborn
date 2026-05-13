package io.github.devskycore.faunareborn.config.entity;

import org.bukkit.configuration.file.FileConfiguration;

public interface EntitySettingsLoader<T extends EntitySettings> {

    EntityType entityType();

    Class<T> settingsType();

    T load(FileConfiguration globalConfig, FileConfiguration entityConfig);
}
