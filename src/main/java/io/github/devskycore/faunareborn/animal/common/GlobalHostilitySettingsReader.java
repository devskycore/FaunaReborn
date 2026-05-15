package io.github.devskycore.faunareborn.animal.common;

import io.github.devskycore.faunareborn.animal.chicken.config.PluginConfigDefaults;
import io.github.devskycore.faunareborn.config.common.ConfigNumbers;
import io.github.devskycore.faunareborn.config.common.TargetingSettings;
import io.github.devskycore.faunareborn.config.common.TargetingSettingsReader;
import io.github.devskycore.faunareborn.config.common.WorldFilter;
import io.github.devskycore.faunareborn.config.common.WorldFilterConfigReader;
import io.github.devskycore.faunareborn.core.FaunaRebornPlugin;
import org.bukkit.Difficulty;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class GlobalHostilitySettingsReader {

    private final FaunaRebornPlugin plugin;
    private final ConfigNumbers numbers;
    private final WorldFilterConfigReader worldFilterConfigReader;
    private final TargetingSettingsReader targetingSettingsReader;

    public GlobalHostilitySettingsReader(
            FaunaRebornPlugin plugin,
            ConfigNumbers numbers,
            WorldFilterConfigReader worldFilterConfigReader,
            TargetingSettingsReader targetingSettingsReader
    ) {
        this.plugin = plugin;
        this.numbers = numbers;
        this.worldFilterConfigReader = worldFilterConfigReader;
        this.targetingSettingsReader = targetingSettingsReader;
    }

    public GlobalHostilitySettingsData read(FileConfiguration globalConfig) {
        WorldFilter worldFilter = worldFilterConfigReader.readWorldFilter(globalConfig);
        String visualRoot = "visual-effects";

        double activationChance = readActivationChance(globalConfig);
        boolean onlyNatural = globalConfig.getBoolean(
                "activation.only-natural",
                PluginConfigDefaults.ONLY_NATURAL_CHICKENS
        );
        boolean ignoreNamed = globalConfig.getBoolean("activation.ignore-named", PluginConfigDefaults.IGNORE_NAMED);

        int maxActivePerChunk = numbers.intRange(
                globalConfig.getInt("max-active-hostile-chickens-per-chunk", PluginConfigDefaults.MAX_ACTIVE_HOSTILE_CHICKENS_PER_CHUNK),
                1,
                PluginConfigDefaults.MAX_ACTIVE_HOSTILE_CHICKENS_PER_CHUNK_LIMIT,
                PluginConfigDefaults.MAX_ACTIVE_HOSTILE_CHICKENS_PER_CHUNK,
                "Invalid max-active-hostile-chickens-per-chunk in config.yml. Falling back to 8",
                "max-active-hostile-chickens-per-chunk is too high. Clamped to 128"
        );
        int maxActivePerWorld = numbers.intRange(
                globalConfig.getInt("max-active-hostile-chickens-per-world", PluginConfigDefaults.MAX_ACTIVE_HOSTILE_CHICKENS_PER_WORLD),
                1,
                PluginConfigDefaults.MAX_ACTIVE_HOSTILE_CHICKENS_PER_WORLD_LIMIT,
                PluginConfigDefaults.MAX_ACTIVE_HOSTILE_CHICKENS_PER_WORLD,
                "Invalid max-active-hostile-chickens-per-world in config.yml. Falling back to 250",
                "max-active-hostile-chickens-per-world is too high. Clamped to 5000"
        );
        int maxProcessedPerTick = numbers.intRange(
                globalConfig.getInt("max-processed-chickens-per-tick", PluginConfigDefaults.MAX_PROCESSED_CHICKENS_PER_TICK),
                1,
                PluginConfigDefaults.MAX_PROCESSED_CHICKENS_PER_TICK_LIMIT,
                PluginConfigDefaults.MAX_PROCESSED_CHICKENS_PER_TICK,
                "Invalid max-processed-chickens-per-tick in config.yml. Falling back to 300",
                "max-processed-chickens-per-tick is too high. Clamped to 1000"
        );

        boolean nightBehaviorEnabled = globalConfig.getBoolean("night-behavior.enabled", true);
        boolean nightDamageToggleEnabled = globalConfig.getBoolean("night-behavior.damage.enabled", true);
        boolean nightDamageEnabled = nightBehaviorEnabled && nightDamageToggleEnabled;
        double nightDamageMultiplier = readNightDamageMultiplier(globalConfig);

        double peacefulDamageMultiplier = readDifficultyDamageMultiplier(globalConfig, Difficulty.PEACEFUL, 0.0D);
        double easyDamageMultiplier = readDifficultyDamageMultiplier(globalConfig, Difficulty.EASY, 1.0D);
        double normalDamageMultiplier = readDifficultyDamageMultiplier(globalConfig, Difficulty.NORMAL, 1.0D);
        double hardDamageMultiplier = readDifficultyDamageMultiplier(globalConfig, Difficulty.HARD, 1.2D);

        GlobalHostilitySettingsData.VisualEffectsSettingsData visualEffectsSettings = new GlobalHostilitySettingsData.VisualEffectsSettingsData(
                globalConfig.getBoolean(visualRoot + ".glow.enabled", PluginConfigDefaults.VISUAL_GLOW_ENABLED),
                globalConfig.getBoolean(visualRoot + ".particles.enabled", PluginConfigDefaults.VISUAL_PARTICLES_ENABLED),
                numbers.intRange(
                        globalConfig.getInt(visualRoot + ".particles.interval-ticks", PluginConfigDefaults.VISUAL_PARTICLES_INTERVAL_TICKS),
                        1,
                        PluginConfigDefaults.VISUAL_PARTICLES_INTERVAL_TICKS_LIMIT,
                        PluginConfigDefaults.VISUAL_PARTICLES_INTERVAL_TICKS,
                        "Invalid visual-effects.particles.interval-ticks in config.yml. Falling back to 8",
                        "visual-effects.particles.interval-ticks is too high. Clamped to 200"
                ),
                numbers.finiteRange(
                        globalConfig.getDouble(visualRoot + ".particles.intensity", PluginConfigDefaults.VISUAL_PARTICLES_VOLUME),
                        0.0D,
                        PluginConfigDefaults.VISUAL_PARTICLES_VOLUME_LIMIT,
                        PluginConfigDefaults.VISUAL_PARTICLES_VOLUME,
                        "Invalid visual-effects.particles.intensity in config.yml. Falling back to 1.0",
                        "visual-effects.particles.intensity is too high. Clamped to 5.0"
                ),
                globalConfig.getBoolean(visualRoot + ".sound.enabled", PluginConfigDefaults.VISUAL_SOUND_ENABLED),
                numbers.intRange(
                        globalConfig.getInt(visualRoot + ".sound.interval-ticks", PluginConfigDefaults.VISUAL_SOUND_INTERVAL_TICKS),
                        1,
                        PluginConfigDefaults.VISUAL_SOUND_INTERVAL_TICKS_LIMIT,
                        PluginConfigDefaults.VISUAL_SOUND_INTERVAL_TICKS,
                        "Invalid visual-effects.sound.interval-ticks in config.yml. Falling back to 160",
                        "visual-effects.sound.interval-ticks is too high. Clamped to 1200"
                ),
                numbers.finiteRange(
                        globalConfig.getDouble(visualRoot + ".sound.volume", PluginConfigDefaults.VISUAL_SOUND_VOLUME),
                        0.0D,
                        PluginConfigDefaults.VISUAL_SOUND_VOLUME_LIMIT,
                        PluginConfigDefaults.VISUAL_SOUND_VOLUME,
                        "Invalid visual-effects.sound.volume in config.yml. Falling back to 0.18",
                        "visual-effects.sound.volume is too high. Clamped to 5.0"
                )
        );

        TargetingSettings targeting = targetingSettingsReader.read(globalConfig);
        return new GlobalHostilitySettingsData(
                activationChance,
                onlyNatural,
                ignoreNamed,
                worldFilter,
                maxActivePerChunk,
                maxActivePerWorld,
                maxProcessedPerTick,
                peacefulDamageMultiplier,
                easyDamageMultiplier,
                normalDamageMultiplier,
                hardDamageMultiplier,
                readWorldDamageMultipliers(globalConfig),
                nightDamageEnabled,
                nightDamageMultiplier,
                visualEffectsSettings,
                targeting
        );
    }

    private double readActivationChance(FileConfiguration globalConfig) {
        double configuredChance = globalConfig.getDouble("activation.chance", PluginConfigDefaults.ACTIVATION_CHANCE);
        if (Double.isNaN(configuredChance) || Double.isInfinite(configuredChance)) {
            plugin.getLogger().warning("Invalid activation.chance in config.yml. Falling back to 1.0");
            return PluginConfigDefaults.ACTIVATION_CHANCE;
        }
        if (configuredChance < 0.0D) {
            plugin.getLogger().warning("activation.chance is too low. Clamped to 0.0");
            return 0.0D;
        }
        if (configuredChance > 1.0D) {
            plugin.getLogger().warning("activation.chance is too high. Clamped to 1.0");
            return 1.0D;
        }
        return configuredChance;
    }

    private double readNightDamageMultiplier(FileConfiguration config) {
        String path = "night-behavior.damage.multiplier";
        double configured = config.getDouble(path, PluginConfigDefaults.NIGHT_DAMAGE_MULTIPLIER);
        if (Double.isNaN(configured) || Double.isInfinite(configured)) {
            plugin.getLogger().warning("Invalid " + path + " in config.yml. Falling back to 1.2");
            return PluginConfigDefaults.NIGHT_DAMAGE_MULTIPLIER;
        }
        if (configured < PluginConfigDefaults.MIN_NIGHT_DAMAGE_MULTIPLIER) {
            plugin.getLogger().warning(path + " is too low. Clamped to 1.0");
            return PluginConfigDefaults.MIN_NIGHT_DAMAGE_MULTIPLIER;
        }
        if (configured > PluginConfigDefaults.MAX_NIGHT_DAMAGE_MULTIPLIER) {
            plugin.getLogger().warning(path + " is too high. Clamped to 1.5");
            return PluginConfigDefaults.MAX_NIGHT_DAMAGE_MULTIPLIER;
        }
        return configured;
    }

    private double readDifficultyDamageMultiplier(FileConfiguration globalConfig, Difficulty difficulty, double defaultValue) {
        String path = "damage-scaling.difficulty-multipliers." + difficulty.name().toLowerCase(Locale.ROOT);
        double multiplier = globalConfig.getDouble(path, defaultValue);
        if (Double.isNaN(multiplier) || Double.isInfinite(multiplier) || multiplier < 0.0D) {
            plugin.getLogger().warning("Invalid " + path + " in config.yml. Falling back to " + defaultValue);
            return defaultValue;
        }
        if (multiplier > PluginConfigDefaults.MAX_DAMAGE_MULTIPLIER) {
            plugin.getLogger().warning(path + " is too high. Clamped to 10.0");
            return PluginConfigDefaults.MAX_DAMAGE_MULTIPLIER;
        }
        return multiplier;
    }

    private Map<String, Double> readWorldDamageMultipliers(FileConfiguration globalConfig) {
        ConfigurationSection section = globalConfig.getConfigurationSection("damage-scaling.world-multipliers");
        if (section == null) {
            return Map.of();
        }

        Map<String, Double> multipliers = new HashMap<>();
        for (String key : section.getKeys(false)) {
            if (key == null || key.trim().isEmpty()) {
                continue;
            }
            double value = section.getDouble(key, 1.0D);
            if (Double.isNaN(value) || Double.isInfinite(value) || value < 0.0D) {
                plugin.getLogger().warning("Invalid damage-scaling.world-multipliers." + key + " in config.yml. Skipping.");
                continue;
            }
            if (value > PluginConfigDefaults.MAX_DAMAGE_MULTIPLIER) {
                plugin.getLogger().warning("damage-scaling.world-multipliers." + key + " is too high. Clamped to 10.0");
                value = PluginConfigDefaults.MAX_DAMAGE_MULTIPLIER;
            }
            multipliers.put(key.trim().toLowerCase(Locale.ROOT), value);
        }

        return Map.copyOf(multipliers);
    }
}
