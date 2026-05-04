package io.github.devskycore.faunareborn.animal.chicken.config.combat;

public record CombatConfigValues(
        double attackDamage,
        int maxSimultaneousAttackers,
        double detectionRadius,
        double attackRange
) {
}


