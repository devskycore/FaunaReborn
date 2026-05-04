package io.github.devskycore.faunareborn.config;

import io.github.devskycore.faunareborn.animal.chicken.config.ChickenSettingsLoader;
import io.github.devskycore.faunareborn.animal.cow.CowSettingsLoader;
import io.github.devskycore.faunareborn.animal.cow.CowSettings;
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

    public PluginConfigManager(FaunaRebornPlugin plugin) {
        this.plugin = plugin;
        this.entityLoaders = List.of(
                new ChickenSettingsLoader(plugin),
                new CowSettingsLoader(plugin)
        );
    }

    public PluginSettings load() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();

        FileConfiguration globalConfig = plugin.getConfig();
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
            loadAndRegisterEntitySettings(registry, loader, entitiesDirectory.toPath());
        }

        return new PluginSettings(
                globalSettings,
                registry.require(io.github.devskycore.faunareborn.animal.chicken.config.ChickenHostilitySettings.class),
                registry.require(CowSettings.class)
        );
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
            Path entitiesDirectory
    ) {
        File entityFile = ensureEntityConfigFile(loader, entitiesDirectory);
        YamlConfiguration entityConfig = YamlConfiguration.loadConfiguration(entityFile);
        T settings = loader.load(entityConfig);
        registry.register(loader.settingsType(), settings);
    }
}


