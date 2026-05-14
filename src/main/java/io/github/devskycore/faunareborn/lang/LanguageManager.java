package io.github.devskycore.faunareborn.lang;

import io.github.devskycore.faunareborn.core.FaunaRebornPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.Map;
import java.util.Objects;

public final class LanguageManager {

    private static final String DEFAULT_LANGUAGE_FILE = "english.yml";
    private static final String LANGUAGE_CONFIG_PATH = "language.file";

    private final FaunaRebornPlugin plugin;
    private volatile FileConfiguration activeLanguage;
    private volatile FileConfiguration defaultLanguage;

    public LanguageManager(FaunaRebornPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    public void reload() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        File languageDirectory = new File(plugin.getDataFolder(), "lang");
        if (!languageDirectory.exists() && !languageDirectory.mkdirs()) {
            throw new IllegalStateException("Could not create language directory: " + languageDirectory.getAbsolutePath());
        }

        saveResourceIfAbsent("lang/" + DEFAULT_LANGUAGE_FILE);
        File englishFile = new File(languageDirectory, DEFAULT_LANGUAGE_FILE);
        defaultLanguage = YamlConfiguration.loadConfiguration(englishFile);

        String configuredFile = plugin.getConfig().getString(LANGUAGE_CONFIG_PATH, DEFAULT_LANGUAGE_FILE);
        String normalizedFile = normalizeLanguageFile(configuredFile);
        saveResourceIfAbsent("lang/" + normalizedFile);
        File selectedLanguageFile = new File(languageDirectory, normalizedFile);
        if (!selectedLanguageFile.exists()) {
            plugin.getLogger().warning("Language file '" + normalizedFile + "' does not exist in /lang. Falling back to english.yml.");
            selectedLanguageFile = new File(languageDirectory, DEFAULT_LANGUAGE_FILE);
        }

        activeLanguage = YamlConfiguration.loadConfiguration(selectedLanguageFile);
    }

    public String text(String path, String fallback) {
        FileConfiguration currentActive = activeLanguage;
        if (currentActive != null) {
            String activeValue = currentActive.getString(path);
            if (activeValue != null && !activeValue.isBlank()) {
                return activeValue;
            }
        }

        FileConfiguration currentDefault = defaultLanguage;
        if (currentDefault != null) {
            String defaultValue = currentDefault.getString(path);
            if (defaultValue != null && !defaultValue.isBlank()) {
                return defaultValue;
            }
        }

        return fallback;
    }

    public String text(String path, String fallback, Map<String, String> placeholders) {
        String raw = text(path, fallback);
        String formatted = raw;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            formatted = formatted.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return formatted;
    }

    private void saveResourceIfAbsent(String path) {
        File target = new File(plugin.getDataFolder(), path);
        if (!target.exists()) {
            try {
                plugin.saveResource(path, false);
            } catch (IllegalArgumentException ignored) {
                // Ignore when the packaged resource does not exist yet.
            }
        }
    }

    private String normalizeLanguageFile(String value) {
        String trimmed = value == null ? DEFAULT_LANGUAGE_FILE : value.trim();
        if (trimmed.isEmpty()) {
            return DEFAULT_LANGUAGE_FILE;
        }
        return trimmed.endsWith(".yml") ? trimmed : trimmed + ".yml";
    }
}
