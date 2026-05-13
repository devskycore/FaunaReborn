package io.github.devskycore.faunareborn.animal.cow;

import io.github.devskycore.faunareborn.animal.chicken.config.PluginConfigDefaults;
import io.github.devskycore.faunareborn.config.common.ConfigNumbers;
import io.github.devskycore.faunareborn.config.common.WorldFilter;
import io.github.devskycore.faunareborn.config.common.WorldFilterConfigReader;
import io.github.devskycore.faunareborn.config.entity.EntitySettingsLoader;
import io.github.devskycore.faunareborn.config.entity.EntityType;
import io.github.devskycore.faunareborn.core.FaunaRebornPlugin;
import org.bukkit.Difficulty;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import io.github.devskycore.faunareborn.system.environment.EnvironmentAggressionSettings;

public final class CowSettingsLoader implements EntitySettingsLoader<CowSettings> {

    private final FaunaRebornPlugin plugin;
    private final ConfigNumbers numbers;
    private final WorldFilterConfigReader worldFilterConfigReader;

    public CowSettingsLoader(FaunaRebornPlugin plugin) {
        this.plugin = plugin;
        this.numbers = new ConfigNumbers(plugin);
        this.worldFilterConfigReader = new WorldFilterConfigReader(plugin);
    }

    @Override
    public EntityType entityType() {
        return EntityType.COW;
    }

    @Override
    public Class<CowSettings> settingsType() {
        return CowSettings.class;
    }

    @Override
    public CowSettings load(FileConfiguration config) {
        boolean moduleEnabled = config.getBoolean("cow.enabled", true);
        boolean provocationEnabled = config.getBoolean("cow-milk-provocation.enabled", true);

        double aggressionDurationSeconds = clampedDouble(
                config.getDouble("cow-milk-provocation.aggression-duration-seconds", 6.0D),
                1.0D,
                120.0D,
                6.0D
        );
        double forgetTargetAfterSeconds = clampedDouble(
                config.getDouble("cow-milk-provocation.targeting.forget-target-after-seconds", 8.0D),
                1.0D,
                180.0D,
                8.0D
        );
        double detectionRange = clampedDouble(
                config.getDouble("cow-milk-provocation.detection-range", 12.0D),
                2.0D,
                64.0D,
                12.0D
        );
        int milkingTriggerCooldownTicks = secondsToTicks(clampedDouble(
                config.getDouble("cow-milk-provocation.milking-trigger-cooldown-seconds", 2.0D),
                0.0D,
                60.0D,
                2.0D
        ), false);
        int warningDurationTicks = secondsToTicks(clampedDouble(
                config.getDouble("cow-milk-provocation.warning.duration-seconds", 0.25D),
                0.0D,
                5.0D,
                0.25D
        ), false);

        double attackDamage = clampedDouble(
                config.getDouble("cow-milk-provocation.attack.damage", 2.0D),
                0.1D,
                20.0D,
                2.0D
        );
        double attackCooldownSeconds = clampedDouble(
                config.getDouble("cow-milk-provocation.attack.cooldown", 1.2D),
                0.2D,
                10.0D,
                1.2D
        );
        double knockbackStrength = clampedDouble(
                config.getDouble("cow-milk-provocation.attack.knockback-strength", 1.1D),
                0.0D,
                4.0D,
                1.1D
        );

        double speedMultiplier = clampedDouble(
                config.getDouble("cow-milk-provocation.movement.speed-multiplier", 1.25D),
                0.5D,
                2.5D,
                1.25D
        );

        boolean requireLineOfSight = config.getBoolean("cow-milk-provocation.targeting.require-line-of-sight", true);
        int retargetGraceTicks = secondsToTicks(clampedDouble(
                config.getDouble("cow-milk-provocation.targeting.retarget-grace-seconds", 3.0D),
                0.0D,
                60.0D,
                3.0D
        ), false);
        boolean playAggressiveSounds = config.getBoolean("cow-milk-provocation.sounds.aggressive-enabled", true);
        boolean playWarningSound = config.getBoolean("cow-milk-provocation.sounds.warning-enabled", true);
        boolean playStompSound = config.getBoolean("cow-milk-provocation.sounds.stomp-enabled", true);
        boolean chargeEnabled = config.getBoolean("cow-milk-provocation.charge.enabled", true);
        int chargeMinIntervalTicks = secondsToTicks(clampedDouble(
                config.getDouble("cow-milk-provocation.charge.min-interval-seconds", 0.8D),
                0.05D,
                10.0D,
                0.8D
        ), true);
        int chargeMaxIntervalTicks = secondsToTicks(clampedDouble(
                config.getDouble("cow-milk-provocation.charge.max-interval-seconds", 2.5D),
                0.1D,
                15.0D,
                2.5D
        ), true);
        if (chargeMaxIntervalTicks < chargeMinIntervalTicks) {
            chargeMaxIntervalTicks = chargeMinIntervalTicks;
        }
        double chargeExtraPush = clampedDouble(
                config.getDouble("cow-milk-provocation.charge.extra-push", 0.17D),
                0.0D,
                1.0D,
                0.17D
        );

        CowSettings.MilkProvocationSettings milkProvocation = new CowSettings.MilkProvocationSettings(
                provocationEnabled,
                secondsToTicks(aggressionDurationSeconds, true),
                secondsToTicks(forgetTargetAfterSeconds, true),
                milkingTriggerCooldownTicks,
                detectionRange,
                detectionRange * detectionRange,
                requireLineOfSight,
                warningDurationTicks,
                attackDamage,
                secondsToTicks(attackCooldownSeconds, true),
                knockbackStrength,
                speedMultiplier,
                retargetGraceTicks,
                playAggressiveSounds,
                playWarningSound,
                playStompSound,
                chargeEnabled,
                chargeMinIntervalTicks,
                chargeMaxIntervalTicks,
                chargeExtraPush
        );
        double resourceDetectionRadius = clampedDouble(
                config.getDouble("cow-resource-provocation.detection-radius", 8.0D),
                2.0D,
                64.0D,
                8.0D
        );
        CowSettings.ResourceProvocationSettings resourceProvocation = new CowSettings.ResourceProvocationSettings(
                config.getBoolean("cow-resource-provocation.enabled", true),
                numbers.intRange(
                        config.getInt("cow-resource-provocation.thresholds.leather", 8),
                        1,
                        512,
                        8,
                        "Invalid cow-resource-provocation.thresholds.leather in cow.yml. Falling back to 8",
                        "cow-resource-provocation.thresholds.leather is too high. Clamped to 512"
                ),
                numbers.intRange(
                        config.getInt("cow-resource-provocation.thresholds.raw-beef", 6),
                        1,
                        512,
                        6,
                        "Invalid cow-resource-provocation.thresholds.raw-beef in cow.yml. Falling back to 6",
                        "cow-resource-provocation.thresholds.raw-beef is too high. Clamped to 512"
                ),
                numbers.intRange(
                        config.getInt("cow-resource-provocation.thresholds.bone", 4),
                        1,
                        512,
                        4,
                        "Invalid cow-resource-provocation.thresholds.bone in cow.yml. Falling back to 4",
                        "cow-resource-provocation.thresholds.bone is too high. Clamped to 512"
                ),
                resourceDetectionRadius,
                resourceDetectionRadius * resourceDetectionRadius,
                secondsToTicks(clampedDouble(
                        config.getDouble("cow-resource-provocation.time-window-seconds", 12.0D),
                        0.2D,
                        300.0D,
                        12.0D
                ), true),
                numbers.intRange(
                        config.getInt("cow-resource-provocation.max-item-age-ticks", 2400),
                        0,
                        12000,
                        2400,
                        "Invalid cow-resource-provocation.max-item-age-ticks in cow.yml. Falling back to 2400",
                        "cow-resource-provocation.max-item-age-ticks is too high. Clamped to 12000"
                ),
                config.getBoolean("cow-resource-provocation.night-modifier.enabled", true),
                clampedDouble(
                        config.getDouble("cow-resource-provocation.night-modifier.threshold-multiplier", 0.75D),
                        0.1D,
                        5.0D,
                        0.75D
                ),
                config.getBoolean("cow-resource-provocation.social-propagation-enabled", true),
                numbers.intRange(
                        config.getInt("cow-resource-provocation.max-responders", 3),
                        1,
                        32,
                        3,
                        "Invalid cow-resource-provocation.max-responders in cow.yml. Falling back to 3",
                        "cow-resource-provocation.max-responders is too high. Clamped to 32"
                ),
                secondsToTicks(clampedDouble(
                        config.getDouble("cow-resource-provocation.trigger-cooldown-seconds", 2.5D),
                        0.0D,
                        60.0D,
                        2.5D
                ), false),
                secondsToTicks(clampedDouble(
                        config.getDouble("cow-resource-provocation.aggression-duration-seconds", 10.0D),
                        1.0D,
                        180.0D,
                        10.0D
                ), true)
        );
        CowSettings.SocialAlertSettings socialAlert = new CowSettings.SocialAlertSettings(
                config.getBoolean("cow-hostility.social-alert.enabled", true),
                config.getBoolean("cow-hostility.social-alert.triggers.by-damage-to-cow", true),
                config.getBoolean("cow-hostility.social-alert.triggers.by-nearby-cow-death", true),
                config.getBoolean("cow-hostility.social-alert.responders.adults-only", true),
                clampedDouble(
                        config.getDouble("cow-hostility.social-alert.radius", 10.0D),
                        2.0D,
                        32.0D,
                        10.0D
                ),
                0.0D,
                secondsToTicks(clampedDouble(
                        config.getDouble("cow-hostility.social-alert.cooldown-seconds", 1.0D),
                        0.0D,
                        60.0D,
                        1.0D
                ), false),
                secondsToTicks(clampedDouble(
                        config.getDouble("cow-hostility.social-alert.join-cooldown-seconds", 2.0D),
                        0.0D,
                        60.0D,
                        2.0D
                ), false),
                numbers.intRange(
                        config.getInt("cow-hostility.social-alert.max-responders", 4),
                        0,
                        32,
                        4,
                        "Invalid cow-hostility.social-alert.max-responders in cow.yml. Falling back to 4",
                        "cow-hostility.social-alert.max-responders is too high. Clamped to 32"
                )
        );
        socialAlert = new CowSettings.SocialAlertSettings(
                socialAlert.enabled(),
                socialAlert.onDamage(),
                socialAlert.onNearbyDeath(),
                socialAlert.responderAdultsOnly(),
                socialAlert.radius(),
                socialAlert.radius() * socialAlert.radius(),
                socialAlert.cooldownTicks(),
                socialAlert.joinCooldownTicks(),
                socialAlert.maxResponders()
        );
        return new CowSettings(moduleEnabled, milkProvocation, resourceProvocation, socialAlert, loadGlobalHostilitySettings(),
                EnvironmentAggressionSettings.fromConfig(config, ""));
    }

    private CowSettings.GlobalHostilitySettings loadGlobalHostilitySettings() {
        FileConfiguration globalConfig = plugin.getConfig();
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

        CowSettings.VisualEffectsSettings visualEffectsSettings = new CowSettings.VisualEffectsSettings(
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

        return new CowSettings.GlobalHostilitySettings(
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
                visualEffectsSettings
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

    private static int secondsToTicks(double seconds, boolean minimumOne) {
        long ticks = Math.round(seconds * 20.0D);
        if (ticks > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return Math.max(minimumOne ? 1 : 0, (int) ticks);
    }

    private static double clampedDouble(double value, double min, double max, double fallback) {
        if (Double.isNaN(value) || Double.isInfinite(value) || value < min) {
            return fallback;
        }
        return Math.min(value, max);
    }
}

