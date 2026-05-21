package io.github.devskycore.faunareborn.lang;

import io.github.devskycore.faunareborn.core.FaunaRebornPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public final class LanguageManager {

    private static final String DEFAULT_LANGUAGE_CODE = "en";
    private static final String SPANISH_LANGUAGE_CODE = "es";
    private static final String PORTUGUESE_LANGUAGE_CODE = "pt";
    private static final String ITALIAN_LANGUAGE_CODE = "it";
    private static final String FRENCH_LANGUAGE_CODE = "fr";
    private static final String DEFAULT_LANGUAGE_FILE = "english.yml";
    private static final String SPANISH_LANGUAGE_FILE = "spanish.yml";
    private static final String PORTUGUESE_LANGUAGE_FILE = "portuguese.yml";
    private static final String ITALIAN_LANGUAGE_FILE = "italian.yml";
    private static final String FRENCH_LANGUAGE_FILE = "french.yml";
    private static final String LANGUAGE_CONFIG_PATH = "language.file";
    private static final Pattern COMBINING_MARKS = Pattern.compile("\\p{M}+");
    private static final Map<String, String> KNOWN_LANGUAGE_FILES = Map.of(
            DEFAULT_LANGUAGE_CODE, DEFAULT_LANGUAGE_FILE,
            SPANISH_LANGUAGE_CODE, SPANISH_LANGUAGE_FILE,
            PORTUGUESE_LANGUAGE_CODE, PORTUGUESE_LANGUAGE_FILE,
            ITALIAN_LANGUAGE_CODE, ITALIAN_LANGUAGE_FILE,
            FRENCH_LANGUAGE_CODE, FRENCH_LANGUAGE_FILE
    );

    private final FaunaRebornPlugin plugin;
    private final Map<String, String> languageAliases;
    private final Set<String> missingKeyWarnings = ConcurrentHashMap.newKeySet();
    private volatile FileConfiguration activeLanguage;
    private volatile FileConfiguration defaultLanguage;

    public LanguageManager(FaunaRebornPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.languageAliases = createLanguageAliases();
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
        saveResourceIfAbsent("lang/" + PORTUGUESE_LANGUAGE_FILE);
        saveResourceIfAbsent("lang/" + ITALIAN_LANGUAGE_FILE);
        saveResourceIfAbsent("lang/" + FRENCH_LANGUAGE_FILE);
        syncLanguageFileWithBundledDefaults(DEFAULT_LANGUAGE_FILE);
        syncLanguageFileWithBundledDefaults(SPANISH_LANGUAGE_FILE);
        syncLanguageFileWithBundledDefaults(PORTUGUESE_LANGUAGE_FILE);
        syncLanguageFileWithBundledDefaults(ITALIAN_LANGUAGE_FILE);
        syncLanguageFileWithBundledDefaults(FRENCH_LANGUAGE_FILE);
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
        missingKeyWarnings.clear();
    }

    public List<String> availableLanguageCodes() {
        File languageDirectory = ensureLanguageDirectory();
        List<String> languages = new ArrayList<>();
        for (Map.Entry<String, String> knownLanguage : KNOWN_LANGUAGE_FILES.entrySet()) {
            if (new File(languageDirectory, knownLanguage.getValue()).exists()) {
                languages.add(knownLanguage.getKey());
            }
        }

        File[] languageFiles = languageDirectory.listFiles((dir, name) -> name.toLowerCase(Locale.ROOT).endsWith(".yml"));
        if (languageFiles != null) {
            for (File languageFile : languageFiles) {
                String fileName = languageFile.getName().toLowerCase(Locale.ROOT);
                String derivedCode = deriveCodeFromFileName(fileName);
                if (!derivedCode.isBlank() && !languages.contains(derivedCode)) {
                    languages.add(derivedCode);
                }
            }
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
                logMissingPathOnce(path, "active");
                return defaultValue;
            }
        }

        logMissingPathOnce(path, "active+default");
        return fallback;
    }

    public String text(String path, String fallback, Map<String, String> placeholders) {
        String formatted = text(path, fallback);
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            formatted = formatted.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return formatted;
    }

    public String textAny(String fallback, String... paths) {
        if (paths == null || paths.length == 0) {
            return fallback;
        }
        for (String path : paths) {
            if (path == null || path.isBlank()) {
                continue;
            }
            String value = textOrNull(path);
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return fallback;
    }

    public String textOrNull(String path) {
        if (path == null || path.isBlank()) {
            return null;
        }
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
        return null;
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

    private void syncLanguageFileWithBundledDefaults(String fileName) {
        String path = "lang/" + fileName;
        File target = new File(plugin.getDataFolder(), path);
        if (!target.exists()) {
            return;
        }

        YamlConfiguration bundled = loadBundledLanguage(path);
        if (bundled == null) {
            return;
        }

        YamlConfiguration existing = YamlConfiguration.loadConfiguration(target);
        boolean changed = false;
        for (String key : bundled.getKeys(true)) {
            if (bundled.isConfigurationSection(key)) {
                continue;
            }
            if (existing.contains(key)) {
                continue;
            }
            existing.set(key, bundled.get(key));
            changed = true;
        }

        if (!changed) {
            return;
        }

        try {
            existing.save(target);
        } catch (IOException exception) {
            plugin.getLogger().warning("Could not update language file '" + fileName + "' with missing keys: " + exception.getMessage());
        }
    }

    private YamlConfiguration loadBundledLanguage(String path) {
        try (InputStream input = plugin.getResource(path)) {
            if (input == null) {
                return null;
            }
            return YamlConfiguration.loadConfiguration(new InputStreamReader(input, StandardCharsets.UTF_8));
        } catch (IOException exception) {
            plugin.getLogger().warning("Could not read bundled language resource '" + path + "': " + exception.getMessage());
            return null;
        }
    }

    private String normalizeLanguageCode(String value) {
        String normalized = normalizeToken(value);
        if (normalized.isEmpty()) {
            return DEFAULT_LANGUAGE_CODE;
        }

        String alias = languageAliases.get(normalized);
        if (alias != null) {
            return alias;
        }

        String strippedAlias = languageAliases.get(stripDiacritics(normalized));
        if (strippedAlias != null) {
            return strippedAlias;
        }

        String repaired = normalizeToken(tryRepairMojibake(value));
        if (!repaired.equals(normalized)) {
            String repairedAlias = languageAliases.get(repaired);
            if (repairedAlias != null) {
                return repairedAlias;
            }
            String repairedStrippedAlias = languageAliases.get(stripDiacritics(repaired));
            if (repairedStrippedAlias != null) {
                return repairedStrippedAlias;
            }
        }

        return normalized;
    }

    private String resolveLanguageFile(String value) {
        String normalizedCode = normalizeLanguageCode(value);
        return KNOWN_LANGUAGE_FILES.getOrDefault(normalizedCode, normalizedCode + ".yml");
    }

    public String currentLanguageCode() {
        String configuredValue = plugin.getConfig().getString(LANGUAGE_CONFIG_PATH, DEFAULT_LANGUAGE_CODE);
        return normalizeLanguageCode(configuredValue);
    }

    private File ensureLanguageDirectory() {
        File languageDirectory = new File(plugin.getDataFolder(), "lang");
        if (!languageDirectory.exists() && !languageDirectory.mkdirs()) {
            throw new IllegalStateException("Could not create language directory: " + languageDirectory.getAbsolutePath());
        }
        return languageDirectory;
    }

    private static String deriveCodeFromFileName(String fileName) {
        if (!fileName.endsWith(".yml")) {
            return "";
        }
        String base = fileName.substring(0, fileName.length() - 4);
        return switch (base) {
            case "english" -> DEFAULT_LANGUAGE_CODE;
            case "spanish" -> SPANISH_LANGUAGE_CODE;
            case "portuguese" -> PORTUGUESE_LANGUAGE_CODE;
            case "italian" -> ITALIAN_LANGUAGE_CODE;
            case "french" -> FRENCH_LANGUAGE_CODE;
            default -> base;
        };
    }

    private static Map<String, String> createLanguageAliases() {
        Map<String, String> aliases = new LinkedHashMap<>();
        registerAliases(aliases, DEFAULT_LANGUAGE_CODE, "en", "english", "ingles", "inglés", "en_us", "en-us");
        registerAliases(aliases, SPANISH_LANGUAGE_CODE, "es", "spanish", "espanol", "español", "es_es", "es-es");
        registerAliases(aliases, PORTUGUESE_LANGUAGE_CODE, "pt", "portuguese", "portugues", "português", "pt_br", "pt-br", "pt_pt", "pt-pt");
        registerAliases(aliases, ITALIAN_LANGUAGE_CODE, "it", "italian", "italiano", "it_it", "it-it");
        registerAliases(aliases, FRENCH_LANGUAGE_CODE, "fr", "french", "francais", "français", "fr_fr", "fr-fr");
        return Map.copyOf(aliases);
    }

    private static void registerAliases(Map<String, String> aliases, String code, String... values) {
        for (String value : values) {
            String normalized = normalizeToken(value);
            if (normalized.isBlank()) {
                continue;
            }
            aliases.put(normalized, code);
            aliases.put(stripDiacritics(normalized), code);
        }
    }

    private static String normalizeToken(String value) {
        if (value == null) {
            return "";
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if (normalized.endsWith(".yml")) {
            normalized = normalized.substring(0, normalized.length() - 4);
        }
        return normalized.replace(' ', '-').replace('_', '-');
    }

    private static String stripDiacritics(String value) {
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD);
        return COMBINING_MARKS.matcher(normalized).replaceAll("");
    }

    private static String tryRepairMojibake(String value) {
        if (value == null || !(value.contains("Ã") || value.contains("â") || value.contains("�"))) {
            return value == null ? "" : value;
        }
        byte[] bytes = value.getBytes(StandardCharsets.ISO_8859_1);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private void logMissingPathOnce(String path, String scope) {
        String key = path + "|" + scope;
        if (!missingKeyWarnings.add(key)) {
            return;
        }
        if ("active".equals(scope)) {
            plugin.getLogger().warning("Missing language key in active file: '" + path + "'. Falling back to english.yml.");
            return;
        }
        plugin.getLogger().warning("Missing language key in active and english files: '" + path + "'. Using code fallback.");
    }
}
