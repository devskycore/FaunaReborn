package io.github.devskycore.faunareborn.config;

import io.github.devskycore.faunareborn.config.entity.EntitySettings;
import io.github.devskycore.faunareborn.config.entity.EntitySettingsLoader;
import io.github.devskycore.faunareborn.config.entity.EntitySettingsRegistry;
import io.github.devskycore.faunareborn.config.entity.EntityType;
import io.github.devskycore.faunareborn.core.FaunaRebornPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

public final class PluginConfigManager {

    private final FaunaRebornPlugin plugin;
    private final List<EntitySettingsLoader<?>> entityLoaders;

    public PluginConfigManager(FaunaRebornPlugin plugin, List<EntitySettingsLoader<?>> entityLoaders) {
        this.plugin = plugin;
        this.entityLoaders = List.copyOf(entityLoaders);
    }

    public PluginSettings load() {
        plugin.saveDefaultConfig();
        File globalFile = new File(plugin.getDataFolder(), "config.yml");
        FileConfiguration globalConfig = YamlConfiguration.loadConfiguration(globalFile);
        return load(globalConfig);
    }

    public PluginSettings load(FileConfiguration globalConfig) {
        GlobalSettings globalSettings = new GlobalSettings(
                globalConfig.getInt("config-version", 1),
                globalConfig.getBoolean("global-enabled", true)
        );

        File entitiesDirectory = new File(plugin.getDataFolder(), "entities");
        if (!entitiesDirectory.exists() && !entitiesDirectory.mkdirs()) {
            throw new IllegalStateException("Could not create entities directory: " + entitiesDirectory.getAbsolutePath());
        }

        EntitySettingsRegistry registry = new EntitySettingsRegistry();
        for (EntitySettingsLoader<?> loader : entityLoaders) {
            loadAndRegisterEntitySettings(registry, loader, entitiesDirectory.toPath(), globalConfig);
        }

        return new PluginSettings(globalSettings, registry);
    }

    private File ensureEntityConfigFile(EntitySettingsLoader<?> loader, Path entitiesDirectory) {
        EntityType entityType = loader.entityType();
        File entityFile = entitiesDirectory.resolve(entityType.id() + ".yml").toFile();
        if (!entityFile.exists()) {
            plugin.saveResource(entityType.resourcePath(), false);
        }
        return entityFile;
    }

    private <T extends EntitySettings> void loadAndRegisterEntitySettings(
            EntitySettingsRegistry registry,
            EntitySettingsLoader<T> loader,
            Path entitiesDirectory,
            FileConfiguration globalConfig
    ) {
        File entityFile = ensureEntityConfigFile(loader, entitiesDirectory);
        try {
            YamlConfiguration entityConfig = YamlConfiguration.loadConfiguration(entityFile);
            T settings = loader.load(globalConfig, entityConfig);
            registry.register(loader.settingsType(), settings);
        } catch (Throwable throwable) {
            throw new IllegalStateException("Invalid entity config: " + entityFile.getAbsolutePath(), throwable);
        }
    }
}


