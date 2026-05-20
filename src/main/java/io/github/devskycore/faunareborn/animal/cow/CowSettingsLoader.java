package io.github.devskycore.faunareborn.animal.cow;

import io.github.devskycore.faunareborn.animal.common.GlobalHostilitySettingsData;
import io.github.devskycore.faunareborn.animal.common.GlobalHostilitySettingsReader;
import io.github.devskycore.faunareborn.animal.common.settings.SharedVisualEffectsSettings;
import io.github.devskycore.faunareborn.config.common.ConfigNumbers;
import io.github.devskycore.faunareborn.config.common.LodSettingsReader;
import io.github.devskycore.faunareborn.config.common.TargetingSettingsReader;
import io.github.devskycore.faunareborn.config.common.WorldFilterConfigReader;
import io.github.devskycore.faunareborn.config.entity.EntitySettingsLoader;
import io.github.devskycore.faunareborn.config.entity.EntityType;
import io.github.devskycore.faunareborn.core.FaunaRebornPlugin;
import io.github.devskycore.faunareborn.system.environment.EnvironmentAggressionSettings;
import io.github.devskycore.faunareborn.system.lod.LodSettings;
import org.bukkit.configuration.file.FileConfiguration;

public final class CowSettingsLoader implements EntitySettingsLoader<CowSettings> {

    private final ConfigNumbers numbers;
    private final LodSettingsReader lodSettingsReader;
    private final GlobalHostilitySettingsReader globalHostilitySettingsReader;

    public CowSettingsLoader(FaunaRebornPlugin plugin) {
        this.numbers = new ConfigNumbers(plugin);
        WorldFilterConfigReader worldFilterConfigReader = new WorldFilterConfigReader(plugin);
        TargetingSettingsReader targetingSettingsReader = new TargetingSettingsReader(plugin);
        this.lodSettingsReader = new LodSettingsReader(plugin);
        this.globalHostilitySettingsReader = new GlobalHostilitySettingsReader(
                plugin,
                numbers,
                worldFilterConfigReader,
                targetingSettingsReader
        );
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
    public CowSettings load(FileConfiguration globalConfig, FileConfiguration entityConfig) {
        boolean moduleEnabled = entityConfig.getBoolean("cow.enabled", true);
        boolean provocationEnabled = entityConfig.getBoolean("cow-milk-provocation.enabled", true);

        double aggressionDurationSeconds = clampedDouble(
                entityConfig.getDouble("cow-milk-provocation.aggression-duration-seconds", 6.0D),
                1.0D,
                120.0D,
                6.0D
        );
        double forgetTargetAfterSeconds = clampedDouble(
                entityConfig.getDouble("cow-milk-provocation.targeting.forget-target-after-seconds", 8.0D),
                1.0D,
                180.0D,
                8.0D
        );
        double detectionRange = clampedDouble(
                entityConfig.getDouble("cow-milk-provocation.detection-range", 12.0D),
                2.0D,
                64.0D,
                12.0D
        );
        int milkingTriggerCooldownTicks = secondsToTicks(clampedDouble(
                entityConfig.getDouble("cow-milk-provocation.milking-trigger-cooldown-seconds", 2.0D),
                0.0D,
                60.0D,
                2.0D
        ), false);
        int warningDurationTicks = secondsToTicks(clampedDouble(
                entityConfig.getDouble("cow-milk-provocation.warning.duration-seconds", 0.25D),
                0.0D,
                5.0D,
                0.25D
        ), false);

        double attackDamage = clampedDouble(
                entityConfig.getDouble("cow-milk-provocation.attack.damage", 2.0D),
                0.1D,
                20.0D,
                2.0D
        );
        double attackCooldownSeconds = clampedDouble(
                entityConfig.getDouble("cow-milk-provocation.attack.cooldown", 1.2D),
                0.2D,
                10.0D,
                1.2D
        );
        double knockbackStrength = clampedDouble(
                entityConfig.getDouble("cow-milk-provocation.attack.knockback-strength", 1.1D),
                0.0D,
                4.0D,
                1.1D
        );

        double speedMultiplier = clampedDouble(
                entityConfig.getDouble("cow-milk-provocation.movement.speed-multiplier", 1.25D),
                0.5D,
                2.5D,
                1.25D
        );

        boolean requireLineOfSight = entityConfig.getBoolean("cow-milk-provocation.targeting.require-line-of-sight", true);
        int retargetGraceTicks = secondsToTicks(clampedDouble(
                entityConfig.getDouble("cow-milk-provocation.targeting.retarget-grace-seconds", 3.0D),
                0.0D,
                60.0D,
                3.0D
        ), false);
        boolean playAggressiveSounds = entityConfig.getBoolean("cow-milk-provocation.sounds.aggressive-enabled", true);
        boolean playWarningSound = entityConfig.getBoolean("cow-milk-provocation.sounds.warning-enabled", true);
        boolean playStompSound = entityConfig.getBoolean("cow-milk-provocation.sounds.stomp-enabled", true);
        boolean chargeEnabled = entityConfig.getBoolean("cow-milk-provocation.charge.enabled", true);
        int chargeMinIntervalTicks = secondsToTicks(clampedDouble(
                entityConfig.getDouble("cow-milk-provocation.charge.min-interval-seconds", 0.8D),
                0.05D,
                10.0D,
                0.8D
        ), true);
        int chargeMaxIntervalTicks = secondsToTicks(clampedDouble(
                entityConfig.getDouble("cow-milk-provocation.charge.max-interval-seconds", 2.5D),
                0.1D,
                15.0D,
                2.5D
        ), true);
        if (chargeMaxIntervalTicks < chargeMinIntervalTicks) {
            chargeMaxIntervalTicks = chargeMinIntervalTicks;
        }
        double chargeExtraPush = clampedDouble(
                entityConfig.getDouble("cow-milk-provocation.charge.extra-push", 0.17D),
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
                entityConfig.getDouble("cow-resource-provocation.detection-radius", 8.0D),
                2.0D,
                64.0D,
                8.0D
        );
        CowSettings.ResourceProvocationSettings resourceProvocation = new CowSettings.ResourceProvocationSettings(
                entityConfig.getBoolean("cow-resource-provocation.enabled", true),
                numbers.intRange(
                        entityConfig.getInt("cow-resource-provocation.thresholds.leather", 8),
                        1,
                        512,
                        8,
                        "Invalid cow-resource-provocation.thresholds.leather in cow.yml. Falling back to 8",
                        "cow-resource-provocation.thresholds.leather is too high. Clamped to 512"
                ),
                numbers.intRange(
                        entityConfig.getInt("cow-resource-provocation.thresholds.raw-beef", 6),
                        1,
                        512,
                        6,
                        "Invalid cow-resource-provocation.thresholds.raw-beef in cow.yml. Falling back to 6",
                        "cow-resource-provocation.thresholds.raw-beef is too high. Clamped to 512"
                ),
                numbers.intRange(
                        entityConfig.getInt("cow-resource-provocation.thresholds.bone", 4),
                        1,
                        512,
                        4,
                        "Invalid cow-resource-provocation.thresholds.bone in cow.yml. Falling back to 4",
                        "cow-resource-provocation.thresholds.bone is too high. Clamped to 512"
                ),
                resourceDetectionRadius,
                resourceDetectionRadius * resourceDetectionRadius,
                secondsToTicks(clampedDouble(
                        entityConfig.getDouble("cow-resource-provocation.time-window-seconds", 12.0D),
                        0.2D,
                        300.0D,
                        12.0D
                ), true),
                numbers.intRange(
                        entityConfig.getInt("cow-resource-provocation.max-item-age-ticks", 2400),
                        0,
                        12000,
                        2400,
                        "Invalid cow-resource-provocation.max-item-age-ticks in cow.yml. Falling back to 2400",
                        "cow-resource-provocation.max-item-age-ticks is too high. Clamped to 12000"
                ),
                entityConfig.getBoolean("cow-resource-provocation.night-modifier.enabled", true),
                clampedDouble(
                        entityConfig.getDouble("cow-resource-provocation.night-modifier.threshold-multiplier", 0.75D),
                        0.1D,
                        5.0D,
                        0.75D
                ),
                entityConfig.getBoolean("cow-resource-provocation.social-propagation-enabled", true),
                numbers.intRange(
                        entityConfig.getInt("cow-resource-provocation.max-responders", 3),
                        1,
                        32,
                        3,
                        "Invalid cow-resource-provocation.max-responders in cow.yml. Falling back to 3",
                        "cow-resource-provocation.max-responders is too high. Clamped to 32"
                ),
                secondsToTicks(clampedDouble(
                        entityConfig.getDouble("cow-resource-provocation.trigger-cooldown-seconds", 2.5D),
                        0.0D,
                        60.0D,
                        2.5D
                ), false),
                secondsToTicks(clampedDouble(
                        entityConfig.getDouble("cow-resource-provocation.aggression-duration-seconds", 10.0D),
                        1.0D,
                        180.0D,
                        10.0D
                ), true)
        );
        CowSettings.SocialAlertSettings socialAlert = new CowSettings.SocialAlertSettings(
                entityConfig.getBoolean("cow-hostility.social-alert.enabled", true),
                entityConfig.getBoolean("cow-hostility.social-alert.triggers.by-damage-to-cow", true),
                entityConfig.getBoolean("cow-hostility.social-alert.triggers.by-nearby-cow-death", true),
                entityConfig.getBoolean("cow-hostility.social-alert.responders.adults-only", true),
                clampedDouble(
                        entityConfig.getDouble("cow-hostility.social-alert.radius", 10.0D),
                        2.0D,
                        32.0D,
                        10.0D
                ),
                secondsToTicks(clampedDouble(
                        entityConfig.getDouble("cow-hostility.social-alert.cooldown-seconds", 1.0D),
                        0.0D,
                        60.0D,
                        1.0D
                ), false),
                secondsToTicks(clampedDouble(
                        entityConfig.getDouble("cow-hostility.social-alert.join-cooldown-seconds", 2.0D),
                        0.0D,
                        60.0D,
                        2.0D
                ), false),
                numbers.intRange(
                        entityConfig.getInt("cow-hostility.social-alert.max-responders", 4),
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
                socialAlert.cooldownTicks(),
                socialAlert.joinCooldownTicks(),
                socialAlert.maxResponders()
        );
        LodSettings lodSettings = lodSettingsReader.read(globalConfig, "lod");
        return new CowSettings(moduleEnabled, milkProvocation, resourceProvocation, socialAlert, loadGlobalHostilitySettings(globalConfig),
                EnvironmentAggressionSettings.fromConfig(entityConfig, ""), lodSettings);
    }

    private CowSettings.GlobalHostilitySettings loadGlobalHostilitySettings(FileConfiguration globalConfig) {
        GlobalHostilitySettingsData data = globalHostilitySettingsReader.read(globalConfig);
        SharedVisualEffectsSettings visualEffectsSettings = visualEffectsSettings(data);

        return new CowSettings.GlobalHostilitySettings(
                data.activationChance(),
                data.onlyNatural(),
                data.ignoreNamed(),
                data.worldFilter(),
                data.maxActivePerChunk(),
                data.maxActivePerWorld(),
                data.maxProcessedPerTick(),
                data.peacefulDamageMultiplier(),
                data.easyDamageMultiplier(),
                data.normalDamageMultiplier(),
                data.hardDamageMultiplier(),
                data.worldDamageMultipliers(),
                data.nightDamageEnabled(),
                data.nightDamageMultiplier(),
                visualEffectsSettings,
                data.targeting()
        );
    }

    private static SharedVisualEffectsSettings visualEffectsSettings(GlobalHostilitySettingsData data) {
        GlobalHostilitySettingsData.VisualEffectsSettingsData visualData = data.visualEffectsSettings();
        return new SharedVisualEffectsSettings(
                visualData.glowEnabled(),
                visualData.particlesEnabled(),
                visualData.particlesIntervalTicks(),
                visualData.particlesIntensity(),
                visualData.soundEnabled(),
                visualData.soundIntervalTicks(),
                visualData.soundVolume()
        );
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
