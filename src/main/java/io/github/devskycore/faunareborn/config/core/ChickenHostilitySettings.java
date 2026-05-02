package io.github.devskycore.faunareborn.config.core;

import io.github.devskycore.faunareborn.config.world.WorldFilter;
import java.util.Map;

public record ChickenHostilitySettings(
        boolean enabled,
        double attackDamage,
        int globalTargetCooldownTicks,
        int maxSimultaneousAttackersPerPlayer,
        int maxActiveHostileChickensPerChunk,
        int maxActiveHostileChickensPerWorld,
        int maxProcessedChickensPerTick,
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
        int socialAlertMaxResponders,
        boolean visualGlowEnabled,
        boolean visualParticlesEnabled,
        int visualParticlesIntervalTicks,
        double visualParticlesVolume,
        boolean visualSoundEnabled,
        int visualSoundIntervalTicks,
        double visualSoundVolume,
        ActivationConfig activation,
        double detectionRadius,
        double attackRange,
        double movementSpeedMultiplier,
        double movementDistanceBoostStartDistance,
        double movementDistanceBoostExtraSpeedPerBlock,
        double movementDistanceBoostMaxMultiplier,
        boolean movementTerrainJumpEnabled,
        double movementTerrainJumpVerticalBoost,
        int movementTerrainJumpCooldownTicks,
        double movementTerrainJumpTriggerHeightDelta,
        WorldFilter worldFilter,
        double peacefulDamageMultiplier,
        double easyDamageMultiplier,
        double normalDamageMultiplier,
        double hardDamageMultiplier,
        Map<String, Double> worldDamageMultipliers,
        boolean nightDamageEnabled,
        double nightDamageMultiplier
) {
}

