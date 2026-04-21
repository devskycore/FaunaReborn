package io.github.devskycore.faunareborn.config;

public record ChickenHostilitySettings(
        boolean enabled,
        double attackDamage,
        int maxSimultaneousAttackersPerPlayer,
        int attackCooldownTicks,
        ActivationConfig activation,
        double detectionRadius,
        double attackRange,
        double movementSpeedMultiplier,
        WorldFilter worldFilter
) {
}
