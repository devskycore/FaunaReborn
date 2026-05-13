package io.github.devskycore.faunareborn.system.environment;

public record EnvironmentAggressionModifiers(
        double aggressionMultiplier,
        double detectionRadiusMultiplier,
        double detectionRadiusBonus,
        double attackDamageMultiplier,
        double attackCooldownMultiplier,
        double movementSpeedMultiplier,
        double pickupRequirementMultiplier,
        double socialAlertMultiplier,
        double targetPersistenceMultiplier,
        boolean fearlessness
) {

    public static EnvironmentAggressionModifiers identity() {
        return new EnvironmentAggressionModifiers(1.0D, 1.0D, 0.0D, 1.0D, 1.0D, 1.0D, 1.0D, 1.0D, 1.0D, false);
    }

    public EnvironmentAggressionModifiers combine(EnvironmentAggressionModifiers other) {
        return new EnvironmentAggressionModifiers(
                aggressionMultiplier * other.aggressionMultiplier,
                detectionRadiusMultiplier * other.detectionRadiusMultiplier,
                detectionRadiusBonus + other.detectionRadiusBonus,
                attackDamageMultiplier * other.attackDamageMultiplier,
                attackCooldownMultiplier * other.attackCooldownMultiplier,
                movementSpeedMultiplier * other.movementSpeedMultiplier,
                pickupRequirementMultiplier * other.pickupRequirementMultiplier,
                socialAlertMultiplier * other.socialAlertMultiplier,
                targetPersistenceMultiplier * other.targetPersistenceMultiplier,
                fearlessness || other.fearlessness
        );
    }
}
