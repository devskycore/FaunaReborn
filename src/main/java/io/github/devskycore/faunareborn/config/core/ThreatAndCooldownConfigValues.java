package io.github.devskycore.faunareborn.config.core;

public record ThreatAndCooldownConfigValues(
        int globalTargetCooldownTicks,
        int attackCooldownTicks,
        int threatTimeoutTicks,
        int retargetGraceTicks,
        int noLineOfSightResetTicks,
        boolean socialAlertEnabled,
        boolean socialAlertOnDamage,
        boolean socialAlertOnNearbyDeath,
        boolean socialAlertResponderAdultsOnly,
        double socialAlertRadius,
        int socialAlertCooldownTicks,
        int socialAlertJoinCooldownTicks,
        int socialAlertMaxResponders
) {
}

