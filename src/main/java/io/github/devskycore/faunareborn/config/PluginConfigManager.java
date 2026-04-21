package io.github.devskycore.faunareborn.config;

import io.github.devskycore.faunareborn.core.FaunaRebornPlugin;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Locale;

public final class PluginConfigManager {

    private static final double DEFAULT_ATTACK_DAMAGE = 2.0D;
    private static final int DEFAULT_MAX_SIMULTANEOUS_ATTACKERS = 3;
    private static final double DEFAULT_ATTACK_COOLDOWN_SECONDS = 0.9D;
    private static final double DEFAULT_ACTIVATION_CHANCE = 1.0D;
    private static final boolean DEFAULT_ONLY_NATURAL_CHICKENS = true;
    private static final boolean DEFAULT_IGNORE_NAMED = true;
    private static final double DEFAULT_DETECTION_RADIUS = 8.0D;
    private static final double DEFAULT_ATTACK_RANGE = 1.5D;
    private static final double DEFAULT_MOVEMENT_SPEED_MULTIPLIER = 1.0D;
    private static final double MIN_MOVEMENT_SPEED_MULTIPLIER = 0.1D;
    private static final double MAX_MOVEMENT_SPEED_MULTIPLIER = 4.0D;

    private final FaunaRebornPlugin plugin;

    public PluginConfigManager(FaunaRebornPlugin plugin) {
        this.plugin = plugin;
    }

    public PluginSettings load() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();

        FileConfiguration config = plugin.getConfig();

        boolean enabled = config.getBoolean("chicken-hostility.enabled", true);
        double attackDamage = sanitizeAttackDamage(
                config.getDouble("chicken-hostility.attack-damage", DEFAULT_ATTACK_DAMAGE)
        );
        int maxSimultaneousAttackers = sanitizeMaxSimultaneousAttackers(
                config.getInt(
                        "chicken-hostility.max-simultaneous-attackers-per-player",
                        DEFAULT_MAX_SIMULTANEOUS_ATTACKERS
                )
        );
        double attackCooldownSeconds = readAttackCooldownSeconds(config);
        int attackCooldownTicks = toTicks(attackCooldownSeconds);
        ActivationConfig activation = readActivationConfig(config);
        double detectionRadius = sanitizeDetectionRadius(
                config.getDouble("chicken-hostility.detection-radius", DEFAULT_DETECTION_RADIUS)
        );
        double attackRange = sanitizeAttackRange(
                config.getDouble("chicken-hostility.attack-range", DEFAULT_ATTACK_RANGE)
        );
        double movementSpeedMultiplier = readMovementSpeedMultiplier(config);
        WorldFilter worldFilter = readWorldFilter(config);

        return new PluginSettings(
                new ChickenHostilitySettings(
                        enabled,
                        attackDamage,
                        maxSimultaneousAttackers,
                        attackCooldownTicks,
                        activation,
                        detectionRadius,
                        attackRange,
                        movementSpeedMultiplier,
                        worldFilter
                )
        );
    }

    private double sanitizeAttackDamage(double configuredDamage) {
        if (Double.isNaN(configuredDamage) || Double.isInfinite(configuredDamage) || configuredDamage < 0.0D) {
            plugin.getLogger().warning("Invalid chicken-hostility.attack-damage in config.yml. Falling back to 2.0");
            return DEFAULT_ATTACK_DAMAGE;
        }
        return configuredDamage;
    }

    private int sanitizeMaxSimultaneousAttackers(int configuredMax) {
        if (configuredMax < 1) {
            plugin.getLogger().warning("Invalid chicken-hostility.max-simultaneous-attackers-per-player in config.yml. Falling back to 3");
            return DEFAULT_MAX_SIMULTANEOUS_ATTACKERS;
        }
        return configuredMax;
    }

    private double readAttackCooldownSeconds(FileConfiguration config) {
        return sanitizeAttackCooldownSeconds(
                config.getDouble("chicken-hostility.attack-cooldown", DEFAULT_ATTACK_COOLDOWN_SECONDS)
        );
    }

    private double sanitizeAttackCooldownSeconds(double configuredSeconds) {
        if (Double.isNaN(configuredSeconds) || Double.isInfinite(configuredSeconds) || configuredSeconds <= 0.0D) {
            plugin.getLogger().warning("Invalid chicken-hostility.attack-cooldown in config.yml. Falling back to 0.9 seconds");
            return DEFAULT_ATTACK_COOLDOWN_SECONDS;
        }
        return configuredSeconds;
    }

    private int toTicks(double seconds) {
        return Math.max(1, (int) Math.round(seconds * 20.0D));
    }

    private ActivationConfig readActivationConfig(FileConfiguration config) {
        double chance = sanitizeActivationChance(
                config.getDouble("chicken-hostility.activation.chance", DEFAULT_ACTIVATION_CHANCE)
        );
        boolean onlyNaturalChickens = config.getBoolean(
                "chicken-hostility.activation.only-natural-chickens",
                DEFAULT_ONLY_NATURAL_CHICKENS
        );
        boolean ignoreNamed = config.getBoolean(
                "chicken-hostility.activation.ignore-named",
                DEFAULT_IGNORE_NAMED
        );
        return new ActivationConfig(chance, onlyNaturalChickens, ignoreNamed);
    }

    private double sanitizeActivationChance(double configuredChance) {
        if (Double.isNaN(configuredChance) || Double.isInfinite(configuredChance)) {
            plugin.getLogger().warning("Invalid chicken-hostility.activation.chance in config.yml. Falling back to 1.0");
            return DEFAULT_ACTIVATION_CHANCE;
        }

        if (configuredChance < 0.0D) {
            plugin.getLogger().warning("chicken-hostility.activation.chance is too low. Clamped to 0.0");
            return 0.0D;
        }

        if (configuredChance > 1.0D) {
            plugin.getLogger().warning("chicken-hostility.activation.chance is too high. Clamped to 1.0");
            return 1.0D;
        }

        return configuredChance;
    }

    private double sanitizeDetectionRadius(double configuredRadius) {
        if (Double.isNaN(configuredRadius) || Double.isInfinite(configuredRadius) || configuredRadius <= 0.0D) {
            plugin.getLogger().warning("Invalid chicken-hostility.detection-radius in config.yml. Falling back to 8.0");
            return DEFAULT_DETECTION_RADIUS;
        }
        return configuredRadius;
    }

    private double sanitizeAttackRange(double configuredRange) {
        if (Double.isNaN(configuredRange) || Double.isInfinite(configuredRange) || configuredRange <= 0.0D) {
            plugin.getLogger().warning("Invalid chicken-hostility.attack-range in config.yml. Falling back to 1.5");
            return DEFAULT_ATTACK_RANGE;
        }
        return configuredRange;
    }

    private double readMovementSpeedMultiplier(FileConfiguration config) {
        return sanitizeMovementSpeedMultiplier(
                config.getDouble("chicken-hostility.movement-speed-multiplier", DEFAULT_MOVEMENT_SPEED_MULTIPLIER)
        );
    }

    private double sanitizeMovementSpeedMultiplier(double configuredMultiplier) {
        if (Double.isNaN(configuredMultiplier) || Double.isInfinite(configuredMultiplier) || configuredMultiplier <= 0.0D) {
            plugin.getLogger().warning("Invalid chicken-hostility.movement-speed-multiplier in config.yml. Falling back to 1.0");
            return DEFAULT_MOVEMENT_SPEED_MULTIPLIER;
        }

        if (configuredMultiplier < MIN_MOVEMENT_SPEED_MULTIPLIER) {
            plugin.getLogger().warning("chicken-hostility.movement-speed-multiplier is too low. Clamped to 0.1");
            return MIN_MOVEMENT_SPEED_MULTIPLIER;
        }

        if (configuredMultiplier > MAX_MOVEMENT_SPEED_MULTIPLIER) {
            plugin.getLogger().warning("chicken-hostility.movement-speed-multiplier is too high. Clamped to 4.0");
            return MAX_MOVEMENT_SPEED_MULTIPLIER;
        }

        return configuredMultiplier;
    }

    private WorldFilter readWorldFilter(FileConfiguration config) {
        final String filterRoot = "chicken-hostility.world-filter";

        if (config.isConfigurationSection(filterRoot)) {
            WorldFilterMode mode = parseWorldFilterMode(config.getString(filterRoot + ".mode"));
            return new WorldFilter(mode, WorldFilter.normalizeWorldNames(config.getStringList(filterRoot + ".worlds")));
        }

        boolean hasEnabledLegacy = config.contains("chicken-hostility.enabled-worlds");
        boolean hasDisabledLegacy = config.contains("chicken-hostility.disabled-worlds");

        if (hasEnabledLegacy) {
            if (hasDisabledLegacy) {
                plugin.getLogger().warning(
                        "Both chicken-hostility.enabled-worlds and chicken-hostility.disabled-worlds are defined. " +
                                "Migrating to world-filter.mode=WHITELIST and ignoring disabled-worlds."
                );
            }
            return new WorldFilter(
                    WorldFilterMode.WHITELIST,
                    WorldFilter.normalizeWorldNames(config.getStringList("chicken-hostility.enabled-worlds"))
            );
        }

        if (hasDisabledLegacy) {
            return new WorldFilter(
                    WorldFilterMode.BLACKLIST,
                    WorldFilter.normalizeWorldNames(config.getStringList("chicken-hostility.disabled-worlds"))
            );
        }

        return new WorldFilter(WorldFilterMode.ALL, java.util.Set.of());
    }

    private WorldFilterMode parseWorldFilterMode(String rawMode) {
        if (rawMode == null || rawMode.trim().isEmpty()) {
            return WorldFilterMode.ALL;
        }

        try {
            return WorldFilterMode.valueOf(rawMode.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            plugin.getLogger().warning(
                    "Invalid chicken-hostility.world-filter.mode in config.yml. Falling back to ALL."
            );
            return WorldFilterMode.ALL;
        }
    }
}
