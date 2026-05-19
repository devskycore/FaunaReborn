package io.github.devskycore.faunareborn.system.environment;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

public record EnvironmentAggressionSettings(
        boolean enabled,
        boolean debug,
        int refreshIntervalTicks,
        Modifier rain,
        Modifier thunderstorm,
        Modifier fullMoon,
        Modifier nightRain,
        Modifier nightThunderstorm,
        Modifier nightFullMoon
) {
    private static final double MAX_DAMAGE_MULTIPLIER = 2.0D;
    private static final double MAX_DETECTION_MULTIPLIER = 3.0D;
    private static final double MIN_GENERAL_MULTIPLIER = 0.1D;
    private static final double MIN_ATTACK_COOLDOWN_MULTIPLIER = 0.4D;
    private static final double MIN_PICKUP_REQUIREMENT_MULTIPLIER = 0.25D;
    private static final double MAX_MOVEMENT_SPEED_MULTIPLIER = 2.0D;

    public static EnvironmentAggressionSettings fromConfig(FileConfiguration config, String rootPath) {
        String root = rootPath + ".environment-modifiers";
        boolean enabled = config.getBoolean(root + ".enabled", true);
        boolean debug = config.getBoolean(root + ".debug", false);
        int refreshIntervalSeconds = Math.max(1, config.getInt(root + ".cache.refresh-interval-seconds", 5));
        return new EnvironmentAggressionSettings(
                enabled,
                debug,
                Math.max(20, refreshIntervalSeconds * 20),
                readModifier(config.getConfigurationSection(root + ".rain")),
                readModifier(config.getConfigurationSection(root + ".thunderstorm")),
                readModifier(config.getConfigurationSection(root + ".full-moon")),
                readModifier(config.getConfigurationSection(root + ".combinations.night-rain")),
                readModifier(config.getConfigurationSection(root + ".combinations.night-thunderstorm")),
                readModifier(config.getConfigurationSection(root + ".combinations.night-full-moon"))
        );
    }

    public EnvironmentAggressionModifiers resolve(WorldEnvironmentContext context) {
        if (!enabled) {
            return EnvironmentAggressionModifiers.identity();
        }
        EnvironmentAggressionModifiers combined = EnvironmentAggressionModifiers.identity();
        if (context.raining()) {
            combined = combined.combine(rain.modifiers());
        }
        if (context.thundering()) {
            combined = combined.combine(thunderstorm.modifiers());
        }
        if (context.fullMoon()) {
            combined = combined.combine(fullMoon.modifiers());
        }
        if (context.night() && context.raining()) {
            combined = combined.combine(nightRain.modifiers());
        }
        if (context.night() && context.thundering()) {
            combined = combined.combine(nightThunderstorm.modifiers());
        }
        if (context.night() && context.fullMoon()) {
            combined = combined.combine(nightFullMoon.modifiers());
        }
        return combined;
    }

    private static Modifier readModifier(ConfigurationSection section) {
        if (section == null) {
            return Modifier.disabled();
        }
        boolean enabled = section.getBoolean("enabled", false);
        if (!enabled) {
            return Modifier.disabled();
        }
        return new Modifier(true, new EnvironmentAggressionModifiers(
                clampMin(section.getDouble("aggression-multiplier", 1.0D)),
                clampRange(section.getDouble("detection-radius-multiplier", 1.0D), MIN_GENERAL_MULTIPLIER, MAX_DETECTION_MULTIPLIER),
                clampRange(section.getDouble("detection-radius-bonus", 0.0D), 0.0D, 8.0D),
                clampRange(section.getDouble("attack-damage-multiplier", 1.0D), MIN_GENERAL_MULTIPLIER, MAX_DAMAGE_MULTIPLIER),
                clampRange(section.getDouble("attack-cooldown-multiplier", 1.0D), MIN_ATTACK_COOLDOWN_MULTIPLIER, 2.5D),
                clampRange(section.getDouble("movement-speed-multiplier", 1.0D), MIN_GENERAL_MULTIPLIER, MAX_MOVEMENT_SPEED_MULTIPLIER),
                clampRange(section.getDouble("pickup-requirement-multiplier", 1.0D), MIN_PICKUP_REQUIREMENT_MULTIPLIER, 4.0D),
                clampMin(section.getDouble("social-alert-multiplier", 1.0D)),
                clampMin(section.getDouble("target-persistence-multiplier", 1.0D)),
                section.getBoolean("fearlessness", false)
        ));
    }

    private static double clampRange(double value, double min, double max) {
        if (!Double.isFinite(value)) {
            return min;
        }
        return Math.clamp(value, min, max);
    }

    private static double clampMin(double value) {
        if (!Double.isFinite(value)) {
            return MIN_GENERAL_MULTIPLIER;
        }
        return Math.max(MIN_GENERAL_MULTIPLIER, value);
    }

    public record Modifier(boolean enabled, EnvironmentAggressionModifiers modifiers) {
        public static Modifier disabled() {
            return new Modifier(false, EnvironmentAggressionModifiers.identity());
        }
    }
}
