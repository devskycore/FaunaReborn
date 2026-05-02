package io.github.devskycore.faunareborn.config.core;

import io.github.devskycore.faunareborn.core.FaunaRebornPlugin;
import org.bukkit.configuration.file.FileConfiguration;

public final class ThreatAndCooldownConfigReader {

    private final ConfigNumbers numbers;

    public ThreatAndCooldownConfigReader(FaunaRebornPlugin plugin) {
        this.numbers = new ConfigNumbers(plugin);
    }

    public ThreatAndCooldownConfigValues read(FileConfiguration config) {
        double globalTargetCooldownSeconds = numbers.finiteRange(
                config.getDouble("chicken-hostility.global-target-cooldown", PluginConfigDefaults.GLOBAL_TARGET_COOLDOWN_SECONDS),
                0.0D,
                PluginConfigDefaults.MAX_TIMER_SECONDS,
                PluginConfigDefaults.GLOBAL_TARGET_COOLDOWN_SECONDS,
                "Invalid chicken-hostility.global-target-cooldown in config.yml. Falling back to 1.5 seconds",
                "chicken-hostility.global-target-cooldown is too high. Clamped to 86400 seconds"
        );
        double attackCooldownSeconds = numbers.finiteRange(
                config.getDouble("chicken-hostility.attack-cooldown", PluginConfigDefaults.ATTACK_COOLDOWN_SECONDS),
                0.01D,
                PluginConfigDefaults.MAX_TIMER_SECONDS,
                PluginConfigDefaults.ATTACK_COOLDOWN_SECONDS,
                "Invalid chicken-hostility.attack-cooldown in config.yml. Falling back to 0.9 seconds",
                "chicken-hostility.attack-cooldown is too high. Clamped to 86400 seconds"
        );
        double threatTimeoutSeconds = numbers.finiteRange(
                config.getDouble("chicken-hostility.threat-timeout", PluginConfigDefaults.THREAT_TIMEOUT_SECONDS),
                0.0D,
                PluginConfigDefaults.MAX_TIMER_SECONDS,
                PluginConfigDefaults.THREAT_TIMEOUT_SECONDS,
                "Invalid chicken-hostility.threat-timeout in config.yml. Falling back to 8.0 seconds",
                "chicken-hostility.threat-timeout is too high. Clamped to 86400 seconds"
        );
        double retargetGraceSeconds = numbers.finiteRange(
                config.getDouble("chicken-hostility.retarget-grace", PluginConfigDefaults.RETARGET_GRACE_SECONDS),
                0.0D,
                PluginConfigDefaults.MAX_TIMER_SECONDS,
                PluginConfigDefaults.RETARGET_GRACE_SECONDS,
                "Invalid chicken-hostility.retarget-grace in config.yml. Falling back to 3.0 seconds",
                "chicken-hostility.retarget-grace is too high. Clamped to 86400 seconds"
        );
        double noLineOfSightResetSeconds = numbers.finiteRange(
                config.getDouble(
                        "chicken-hostility.no-line-of-sight-reset-seconds",
                        PluginConfigDefaults.NO_LINE_OF_SIGHT_RESET_TICKS / 20.0D
                ),
                0.01D,
                PluginConfigDefaults.MAX_TIMER_SECONDS,
                PluginConfigDefaults.NO_LINE_OF_SIGHT_RESET_TICKS / 20.0D,
                "Invalid chicken-hostility.no-line-of-sight-reset-seconds in config.yml. Falling back to 2.0 seconds",
                "chicken-hostility.no-line-of-sight-reset-seconds is too high. Clamped to 86400 seconds"
        );
        double socialAlertRadius = numbers.finiteRange(
                config.getDouble("chicken-hostility.social-alert.radius", PluginConfigDefaults.SOCIAL_ALERT_RADIUS),
                1.0D,
                PluginConfigDefaults.MAX_SOCIAL_ALERT_RADIUS,
                PluginConfigDefaults.SOCIAL_ALERT_RADIUS,
                "Invalid chicken-hostility.social-alert.radius in config.yml. Falling back to 10.0 blocks",
                "chicken-hostility.social-alert.radius is too high. Clamped to 32.0 blocks"
        );
        double socialAlertCooldownSeconds = numbers.finiteRange(
                config.getDouble("chicken-hostility.social-alert.cooldown", PluginConfigDefaults.SOCIAL_ALERT_COOLDOWN_SECONDS),
                0.0D,
                PluginConfigDefaults.MAX_TIMER_SECONDS,
                PluginConfigDefaults.SOCIAL_ALERT_COOLDOWN_SECONDS,
                "Invalid chicken-hostility.social-alert.cooldown in config.yml. Falling back to 1.0 seconds",
                "chicken-hostility.social-alert.cooldown is too high. Clamped to 86400 seconds"
        );
        double socialAlertJoinCooldownSeconds = numbers.finiteRange(
                config.getDouble("chicken-hostility.social-alert.join-cooldown", PluginConfigDefaults.SOCIAL_ALERT_JOIN_COOLDOWN_SECONDS),
                0.0D,
                PluginConfigDefaults.MAX_TIMER_SECONDS,
                PluginConfigDefaults.SOCIAL_ALERT_JOIN_COOLDOWN_SECONDS,
                "Invalid chicken-hostility.social-alert.join-cooldown in config.yml. Falling back to 2.0 seconds",
                "chicken-hostility.social-alert.join-cooldown is too high. Clamped to 86400 seconds"
        );
        int socialAlertMaxResponders = numbers.intRange(
                config.getInt(
                        "chicken-hostility.social-alert.max-responders",
                        PluginConfigDefaults.SOCIAL_ALERT_MAX_RESPONDERS
                ),
                1,
                PluginConfigDefaults.SOCIAL_ALERT_MAX_RESPONDERS_LIMIT,
                PluginConfigDefaults.SOCIAL_ALERT_MAX_RESPONDERS,
                "Invalid chicken-hostility.social-alert.max-responders in config.yml. Falling back to 4",
                "chicken-hostility.social-alert.max-responders is too high. Clamped to 32"
        );

        return new ThreatAndCooldownConfigValues(
                numbers.toNonNegativeTicks(globalTargetCooldownSeconds),
                numbers.toTicks(attackCooldownSeconds),
                numbers.toNonNegativeTicks(threatTimeoutSeconds),
                numbers.toNonNegativeTicks(retargetGraceSeconds),
                numbers.toTicks(noLineOfSightResetSeconds),
                config.getBoolean("chicken-hostility.social-alert.enabled", PluginConfigDefaults.SOCIAL_ALERT_ENABLED),
                config.getBoolean(
                        "chicken-hostility.social-alert.triggers.by-damage-to-chicken",
                        PluginConfigDefaults.SOCIAL_ALERT_ON_DAMAGE
                ),
                config.getBoolean(
                        "chicken-hostility.social-alert.triggers.by-nearby-chicken-death",
                        PluginConfigDefaults.SOCIAL_ALERT_ON_NEARBY_DEATH
                ),
                config.getBoolean(
                        "chicken-hostility.social-alert.responders.adults-only",
                        PluginConfigDefaults.SOCIAL_ALERT_RESPONDER_ADULTS_ONLY
                ),
                socialAlertRadius,
                numbers.toNonNegativeTicks(socialAlertCooldownSeconds),
                numbers.toNonNegativeTicks(socialAlertJoinCooldownSeconds),
                socialAlertMaxResponders
        );
    }
}

