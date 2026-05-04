package io.github.devskycore.faunareborn.animal.chicken.config;

public record ItemPickupTerritorialityConfig(
        boolean enabled,
        int eggThreshold,
        int featherThreshold,
        int rawChickenThreshold,
        double detectionRadius,
        int timeWindowTicks,
        int aggressionDurationTicks,
        int maxItemAgeTicks,
        boolean nightModifierEnabled,
        double nightThresholdMultiplier,
        boolean socialPropagationEnabled
) {
}

