package io.github.devskycore.faunareborn.config.movement;

import io.github.devskycore.faunareborn.config.core.ConfigNumbers;
import io.github.devskycore.faunareborn.config.core.PluginConfigDefaults;
import io.github.devskycore.faunareborn.core.FaunaRebornPlugin;
import org.bukkit.configuration.file.FileConfiguration;

public final class MovementConfigReader {

    private final FaunaRebornPlugin plugin;
    private final ConfigNumbers numbers;

    public MovementConfigReader(FaunaRebornPlugin plugin) {
        this.plugin = plugin;
        this.numbers = new ConfigNumbers(plugin);
    }

    public MovementConfigValues read(FileConfiguration config) {
        double movementSpeedMultiplier = sanitizeMovementSpeedMultiplier(
                config.getDouble("chicken-hostility.movement-speed-multiplier", PluginConfigDefaults.MOVEMENT_SPEED_MULTIPLIER)
        );
        double startDistance = numbers.finiteAndMin(
                config.getDouble(
                        "chicken-hostility.movement.distance-boost.start-distance",
                        PluginConfigDefaults.MOVEMENT_DISTANCE_BOOST_START_DISTANCE
                ),
                0.0D,
                PluginConfigDefaults.MOVEMENT_DISTANCE_BOOST_START_DISTANCE,
                "Invalid chicken-hostility.movement.distance-boost.start-distance in config.yml. Falling back to 5.0"
        );
        double extraSpeedPerBlock = numbers.finiteAndMin(
                config.getDouble(
                        "chicken-hostility.movement.distance-boost.extra-speed-per-block",
                        PluginConfigDefaults.MOVEMENT_DISTANCE_BOOST_EXTRA_SPEED_PER_BLOCK
                ),
                0.0D,
                PluginConfigDefaults.MOVEMENT_DISTANCE_BOOST_EXTRA_SPEED_PER_BLOCK,
                "Invalid chicken-hostility.movement.distance-boost.extra-speed-per-block in config.yml. Falling back to 0.08"
        );
        double maxBoostMultiplier = numbers.finiteAndMin(
                config.getDouble(
                        "chicken-hostility.movement.distance-boost.max-multiplier",
                        PluginConfigDefaults.MOVEMENT_DISTANCE_BOOST_MAX_MULTIPLIER
                ),
                1.0D,
                PluginConfigDefaults.MOVEMENT_DISTANCE_BOOST_MAX_MULTIPLIER,
                "Invalid chicken-hostility.movement.distance-boost.max-multiplier in config.yml. Falling back to 1.35"
        );
        boolean terrainJumpEnabled = config.getBoolean(
                "chicken-hostility.movement.terrain-jump.enabled",
                PluginConfigDefaults.MOVEMENT_TERRAIN_JUMP_ENABLED
        );
        double terrainJumpVerticalBoost = numbers.finiteAndPositive(
                config.getDouble(
                        "chicken-hostility.movement.terrain-jump.vertical-boost",
                        PluginConfigDefaults.MOVEMENT_TERRAIN_JUMP_VERTICAL_BOOST
                ),
                PluginConfigDefaults.MOVEMENT_TERRAIN_JUMP_VERTICAL_BOOST,
                "Invalid chicken-hostility.movement.terrain-jump.vertical-boost in config.yml. Falling back to 0.42"
        );
        int terrainJumpCooldownTicks = numbers.minInt(
                config.getInt(
                        "chicken-hostility.movement.terrain-jump.cooldown-ticks",
                        PluginConfigDefaults.MOVEMENT_TERRAIN_JUMP_COOLDOWN_TICKS
                ),
                1,
                PluginConfigDefaults.MOVEMENT_TERRAIN_JUMP_COOLDOWN_TICKS,
                "Invalid chicken-hostility.movement.terrain-jump.cooldown-ticks in config.yml. Falling back to 8"
        );
        double terrainJumpTriggerHeightDelta = numbers.finiteAndMin(
                config.getDouble(
                        "chicken-hostility.movement.terrain-jump.trigger-height-delta",
                        PluginConfigDefaults.MOVEMENT_TERRAIN_JUMP_TRIGGER_HEIGHT_DELTA
                ),
                0.0D,
                PluginConfigDefaults.MOVEMENT_TERRAIN_JUMP_TRIGGER_HEIGHT_DELTA,
                "Invalid chicken-hostility.movement.terrain-jump.trigger-height-delta in config.yml. Falling back to 0.6"
        );

        return new MovementConfigValues(
                movementSpeedMultiplier,
                startDistance,
                extraSpeedPerBlock,
                maxBoostMultiplier,
                terrainJumpEnabled,
                terrainJumpVerticalBoost,
                terrainJumpCooldownTicks,
                terrainJumpTriggerHeightDelta
        );
    }

    private double sanitizeMovementSpeedMultiplier(double configuredMultiplier) {
        if (Double.isNaN(configuredMultiplier) || Double.isInfinite(configuredMultiplier) || configuredMultiplier <= 0.0D) {
            plugin.getLogger().warning("Invalid chicken-hostility.movement-speed-multiplier in config.yml. Falling back to 1.0");
            return PluginConfigDefaults.MOVEMENT_SPEED_MULTIPLIER;
        }

        if (configuredMultiplier < PluginConfigDefaults.MIN_MOVEMENT_SPEED_MULTIPLIER) {
            plugin.getLogger().warning("chicken-hostility.movement-speed-multiplier is too low. Clamped to 0.1");
            return PluginConfigDefaults.MIN_MOVEMENT_SPEED_MULTIPLIER;
        }

        if (configuredMultiplier > PluginConfigDefaults.MAX_MOVEMENT_SPEED_MULTIPLIER) {
            plugin.getLogger().warning("chicken-hostility.movement-speed-multiplier is too high. Clamped to 4.0");
            return PluginConfigDefaults.MAX_MOVEMENT_SPEED_MULTIPLIER;
        }

        return configuredMultiplier;
    }
}

