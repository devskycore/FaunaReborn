package io.github.devskycore.faunareborn.lang;

import io.github.devskycore.faunareborn.core.FaunaRebornPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public final class LanguageManager {

    private static final String DEFAULT_LANGUAGE_CODE = "en";
    private static final String SPANISH_LANGUAGE_CODE = "es";
    private static final String DEFAULT_LANGUAGE_FILE = "english.yml";
    private static final String SPANISH_LANGUAGE_FILE = "spanish.yml";
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
        saveResourceIfAbsent("lang/" + SPANISH_LANGUAGE_FILE);
        File englishFile = new File(languageDirectory, DEFAULT_LANGUAGE_FILE);
        defaultLanguage = YamlConfiguration.loadConfiguration(englishFile);

        String configuredValue = plugin.getConfig().getString(LANGUAGE_CONFIG_PATH, DEFAULT_LANGUAGE_CODE);
        String normalizedFile = resolveLanguageFile(configuredValue);
        saveResourceIfAbsent("lang/" + normalizedFile);
        File selectedLanguageFile = new File(languageDirectory, normalizedFile);
        if (!selectedLanguageFile.exists()) {
            plugin.getLogger().warning("Language file '" + normalizedFile + "' does not exist in /lang. Falling back to english.yml.");
            selectedLanguageFile = new File(languageDirectory, DEFAULT_LANGUAGE_FILE);
        }

        activeLanguage = YamlConfiguration.loadConfiguration(selectedLanguageFile);
    }

    public List<String> availableLanguageCodes() {
        File languageDirectory = ensureLanguageDirectory();
        List<String> languages = new ArrayList<>();
        if (new File(languageDirectory, DEFAULT_LANGUAGE_FILE).exists()) {
            languages.add(DEFAULT_LANGUAGE_CODE);
        }
        if (new File(languageDirectory, SPANISH_LANGUAGE_FILE).exists()) {
            languages.add(SPANISH_LANGUAGE_CODE);
        }
        if (languages.isEmpty()) {
            return List.of(DEFAULT_LANGUAGE_CODE);
        }
        languages.sort(Comparator.naturalOrder());
        return languages;
    }

    public boolean switchLanguage(String requestedLanguage) {
        String normalizedCode = normalizeLanguageCode(requestedLanguage);
        String normalizedFile = resolveLanguageFile(normalizedCode);
        File languageDirectory = ensureLanguageDirectory();
        saveResourceIfAbsent("lang/" + normalizedFile);
        File selected = new File(languageDirectory, normalizedFile);
        if (!selected.exists()) {
            return false;
        }

        plugin.getConfig().set(LANGUAGE_CONFIG_PATH, normalizedCode);
        plugin.saveConfig();
        reload();
        return true;
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

    private String normalizeLanguageCode(String value) {
        String trimmed = value == null ? DEFAULT_LANGUAGE_CODE : value.trim().toLowerCase(Locale.ROOT);
        if (trimmed.isEmpty()) {
            return DEFAULT_LANGUAGE_CODE;
        }
        if (trimmed.endsWith(".yml")) {
            trimmed = trimmed.substring(0, trimmed.length() - 4);
        }
        if (trimmed.equals("english")) {
            return DEFAULT_LANGUAGE_CODE;
        }
        if (trimmed.equals("spanish") || trimmed.equals("espanol") || trimmed.equals("español")) {
            return SPANISH_LANGUAGE_CODE;
        }
        return trimmed;
    }

    private String resolveLanguageFile(String value) {
        String normalizedCode = normalizeLanguageCode(value);
        return switch (normalizedCode) {
            case DEFAULT_LANGUAGE_CODE -> DEFAULT_LANGUAGE_FILE;
            case SPANISH_LANGUAGE_CODE -> SPANISH_LANGUAGE_FILE;
            default -> normalizedCode + ".yml";
        };
    }

    public String currentLanguageCode() {
        String configuredValue = plugin.getConfig().getString(LANGUAGE_CONFIG_PATH, DEFAULT_LANGUAGE_CODE);
        return normalizeLanguageCode(configuredValue);
    }

    private File ensureLanguageDirectory() {
        File languageDirectory = new File(plugin.getDataFolder(), "lang");
        if (!languageDirectory.exists()) {
            languageDirectory.mkdirs();
        }
        return languageDirectory;
    }

}
