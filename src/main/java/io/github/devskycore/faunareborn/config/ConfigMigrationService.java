package io.github.devskycore.faunareborn.config;

import io.github.devskycore.faunareborn.config.entity.EntityType;
import io.github.devskycore.faunareborn.core.FaunaRebornPlugin;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;

public final class ConfigMigrationService {

    private static final int TARGET_CONFIG_VERSION = 2;
    private static final String CONFIG_RESOURCE_PATH = "config.yml";
    private static final String CONFIG_VERSION_PATH = "config-version";
    private static final String LEGACY_ONLY_NATURAL_PATH = "activation.only-natural";
    private static final String NATURAL_SPAWNS_ONLY_PATH = "activation.natural-spawns-only";
    private static final String CHICKEN_ENTITY_RESOURCE_PATH = "entities/chicken.yml";

    private static final Map<String, String> LANGUAGE_RESOURCE_FILES = Map.of(
            "english.yml", "lang/english.yml",
            "spanish.yml", "lang/spanish.yml",
            "portuguese.yml", "lang/portuguese.yml",
            "italian.yml", "lang/italian.yml",
            "french.yml", "lang/french.yml"
    );

    private final FaunaRebornPlugin plugin;

    public ConfigMigrationService(FaunaRebornPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    public void migrateIfNeeded() {
        ensureConfigDefaultsAndMigrate();
        ensureEntityFilesAndMigrate();
        ensureLanguageFilesAndMergeMissingKeys();
    }

    private void ensureConfigDefaultsAndMigrate() {
        plugin.saveDefaultConfig();
        File configFile = new File(plugin.getDataFolder(), CONFIG_RESOURCE_PATH);
        YamlConfiguration existing = YamlConfiguration.loadConfiguration(configFile);
        YamlConfiguration defaults = loadBundledYaml(CONFIG_RESOURCE_PATH);
        boolean migratedNaturalSpawnsOnly = migrateNaturalSpawnsOnlyAlias(existing);

        int currentVersion = existing.getInt(CONFIG_VERSION_PATH, 1);
        if (currentVersion < TARGET_CONFIG_VERSION) {
            int addedKeys = mergeMissingKeys(existing, defaults);
            existing.set(CONFIG_VERSION_PATH, TARGET_CONFIG_VERSION);
            saveWithBackup(configFile, existing);
            plugin.getLogger().info("Migrated config.yml from schema v" + currentVersion + " to v" + TARGET_CONFIG_VERSION + " (added " + addedKeys + " missing keys).");
            return;
        }

        int addedKeys = mergeMissingKeys(existing, defaults);
        if (migratedNaturalSpawnsOnly || addedKeys > 0) {
            saveWithBackup(configFile, existing);
            plugin.getLogger().info("Updated config.yml with " + addedKeys + " newly introduced default keys.");
        }
    }

    private boolean migrateNaturalSpawnsOnlyAlias(YamlConfiguration existing) {
        if (existing.isSet(NATURAL_SPAWNS_ONLY_PATH) || !existing.isSet(LEGACY_ONLY_NATURAL_PATH)) {
            return false;
        }
        existing.set(NATURAL_SPAWNS_ONLY_PATH, existing.getBoolean(LEGACY_ONLY_NATURAL_PATH));
        return true;
    }

    private void ensureEntityFilesAndMigrate() {
        File entitiesDirectory = new File(plugin.getDataFolder(), "entities");
        if (!entitiesDirectory.exists() && !entitiesDirectory.mkdirs()) {
            throw new IllegalStateException("Could not create entities directory: " + entitiesDirectory.getAbsolutePath());
        }

        for (EntityType entityType : EntityType.values()) {
            String resourcePath = entityType.resourcePath();
            File targetFile = new File(plugin.getDataFolder(), resourcePath);

            if (!targetFile.exists()) {
                plugin.saveResource(resourcePath, false);
                plugin.getLogger().info("Created missing entity config: " + entityType.id() + ".yml");
                continue;
            }

            YamlConfiguration defaults = loadBundledYaml(resourcePath);
            YamlConfiguration existing = YamlConfiguration.loadConfiguration(targetFile);
            boolean migratedNaturalSpawnsOnly = CHICKEN_ENTITY_RESOURCE_PATH.equals(resourcePath)
                    && migrateNaturalSpawnsOnlyAlias(existing);
            int addedKeys = mergeMissingKeys(existing, defaults);
            if (migratedNaturalSpawnsOnly || addedKeys > 0) {
                saveWithBackup(targetFile, existing);
                plugin.getLogger().info("Updated " + entityType.id() + ".yml with " + addedKeys + " newly introduced default keys.");
            }
        }
    }

    private void ensureLanguageFilesAndMergeMissingKeys() {
        File languageDirectory = new File(plugin.getDataFolder(), "lang");
        if (!languageDirectory.exists() && !languageDirectory.mkdirs()) {
            throw new IllegalStateException("Could not create language directory: " + languageDirectory.getAbsolutePath());
        }

        for (Map.Entry<String, String> entry : LANGUAGE_RESOURCE_FILES.entrySet()) {
            String fileName = entry.getKey();
            String resourcePath = entry.getValue();
            File targetFile = new File(languageDirectory, fileName);

            if (!targetFile.exists()) {
                plugin.saveResource(resourcePath, false);
                plugin.getLogger().info("Created missing language file: " + fileName);
                continue;
            }

            YamlConfiguration defaults = loadBundledYaml(resourcePath);
            YamlConfiguration existing = loadOrRecoverUserLanguageYaml(targetFile, defaults);
            int addedKeys = mergeMissingKeys(existing, defaults);
            if (addedKeys > 0) {
                saveWithBackup(targetFile, existing);
                plugin.getLogger().info("Updated " + fileName + " with " + addedKeys + " missing translation keys.");
            }
        }
    }

    private YamlConfiguration loadBundledYaml(String resourcePath) {
        try (InputStream input = plugin.getResource(resourcePath)) {
            if (input == null) {
                throw new IllegalStateException("Bundled resource not found: " + resourcePath);
            }
            return YamlConfiguration.loadConfiguration(new InputStreamReader(input, StandardCharsets.UTF_8));
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to load bundled resource: " + resourcePath, exception);
        }
    }

    private YamlConfiguration loadOrRecoverUserLanguageYaml(File targetFile, YamlConfiguration defaults) {
        String originalText;
        try {
            originalText = Files.readString(targetFile.toPath(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            plugin.getLogger().log(Level.WARNING, "Failed reading " + targetFile.getName() + ". Replacing it with bundled defaults.");
            saveWithBackup(targetFile, defaults);
            return defaults;
        }

        String sanitized = sanitizeInvalidYamlCharacters(originalText);
        YamlConfiguration parsed = new YamlConfiguration();
        try {
            parsed.loadFromString(sanitized);
            if (!sanitized.equals(originalText)) {
                createBackupIfMissing(targetFile);
                Files.writeString(targetFile.toPath(), sanitized, StandardCharsets.UTF_8);
                plugin.getLogger().warning("Sanitized invalid control characters in " + targetFile.getName() + ".");
            }
            return parsed;
        } catch (Exception exception) {
            plugin.getLogger().log(Level.WARNING, "Invalid YAML in " + targetFile.getName() + ". Backing up and restoring bundled defaults.", exception);
            saveWithBackup(targetFile, defaults);
            return defaults;
        }
    }

    private int mergeMissingKeys(YamlConfiguration target, YamlConfiguration source) {
        int additions = 0;
        for (String key : source.getKeys(true)) {
            if (!source.isSet(key) || source.isConfigurationSection(key)) {
                continue;
            }
            if (!target.contains(key, true)) {
                target.set(key, source.get(key));
                additions++;
            }
        }
        return additions;
    }

    private void saveWithBackup(File targetFile, YamlConfiguration content) {
        createBackupIfMissing(targetFile);
        try {
            content.save(targetFile);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to save migrated file: " + targetFile.getAbsolutePath(), exception);
        }
    }

    private void createBackupIfMissing(File targetFile) {
        File backup = new File(targetFile.getParentFile(), targetFile.getName() + ".bak-" + plugin.getPluginMeta().getVersion());
        if (backup.exists()) {
            return;
        }
        try {
            Files.copy(targetFile.toPath(), backup.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            plugin.getLogger().log(Level.WARNING, "Could not create backup for " + targetFile.getName() + ": " + exception.getMessage());
        }
    }

    private String sanitizeInvalidYamlCharacters(String text) {
        StringBuilder sanitized = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (ch == '\n' || ch == '\r' || ch == '\t' || ch >= 0x20) {
                sanitized.append(ch);
            }
        }
        return sanitized.toString();
    }
}
