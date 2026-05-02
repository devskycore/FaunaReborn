package io.github.devskycore.faunareborn.config.combat;

public record CombatConfigValues(
        double attackDamage,
        int maxSimultaneousAttackers,
        double detectionRadius,
        double attackRange
) {
}

