package io.github.devskycore.faunareborn.config.movement;

public record MovementConfigValues(
        double movementSpeedMultiplier,
        double movementDistanceBoostStartDistance,
        double movementDistanceBoostExtraSpeedPerBlock,
        double movementDistanceBoostMaxMultiplier,
        boolean movementTerrainJumpEnabled,
        double movementTerrainJumpVerticalBoost,
        int movementTerrainJumpCooldownTicks,
        double movementTerrainJumpTriggerHeightDelta
) {
}

