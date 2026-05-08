package io.github.devskycore.faunareborn.gui;

import io.github.devskycore.faunareborn.config.entity.EntityType;
import io.github.devskycore.faunareborn.core.FaunaRebornPlugin;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;

public final class PluginGuiConfigService {

    private final FaunaRebornPlugin plugin;
    private final List<EntityModuleToggle> moduleToggles;

    public PluginGuiConfigService(FaunaRebornPlugin plugin, List<EntityModuleToggle> moduleToggles) {
        this.plugin = plugin;
        this.moduleToggles = List.copyOf(moduleToggles);
    }

    public List<EntityModuleToggle> moduleToggles() {
        return moduleToggles;
    }

    public boolean isEnabled(EntityModuleToggle toggle) {
        File file = entityConfigFile(toggle.entityType());
        if (!file.exists()) {
            plugin.saveResource(toggle.entityType().resourcePath(), false);
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        return config.getBoolean(toggle.enabledPath(), true);
    }

    public boolean setEnabled(EntityModuleToggle toggle, boolean enabled) {
        File file = entityConfigFile(toggle.entityType());
        if (!file.exists()) {
            plugin.saveResource(toggle.entityType().resourcePath(), false);
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        config.set(toggle.enabledPath(), enabled);
        try {
            config.save(file);
            return true;
        } catch (IOException ioException) {
            plugin.getLogger().log(Level.SEVERE, "Failed to persist toggle for " + toggle.entityType().id(), ioException);
            return false;
        }
    }

    private File entityConfigFile(EntityType entityType) {
        return new File(plugin.getDataFolder(), entityType.resourcePath());
    }
}
