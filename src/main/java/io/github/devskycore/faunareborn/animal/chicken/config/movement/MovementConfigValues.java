package io.github.devskycore.faunareborn.animal.chicken.config.movement;

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


