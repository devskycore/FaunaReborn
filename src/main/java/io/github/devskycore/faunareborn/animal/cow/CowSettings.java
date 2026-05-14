package io.github.devskycore.faunareborn.animal.cow;

import io.github.devskycore.faunareborn.config.common.WorldFilter;
import io.github.devskycore.faunareborn.config.common.TargetingSettings;
import io.github.devskycore.faunareborn.config.entity.EntitySettings;
import io.github.devskycore.faunareborn.system.lod.LodSettings;

import java.util.Map;
import io.github.devskycore.faunareborn.system.environment.EnvironmentAggressionSettings;

public record CowSettings(
        boolean enabled,
        MilkProvocationSettings milkProvocation,
        ResourceProvocationSettings resourceProvocation,
        SocialAlertSettings socialAlert,
        GlobalHostilitySettings globalHostility,
        EnvironmentAggressionSettings environmentAggression,
        LodSettings lod
) implements EntitySettings {

    public record MilkProvocationSettings(
            boolean enabled,
            int aggressionDurationTicks,
            int forgetTargetAfterTicks,
            int milkingTriggerCooldownTicks,
            double detectionRange,
            double detectionRangeSq,
            boolean requireLineOfSight,
            int warningDurationTicks,
            double attackDamage,
            int attackCooldownTicks,
            double knockbackStrength,
            double speedMultiplier,
            int retargetGraceTicks,
            boolean playAggressiveSounds,
            boolean playWarningSound,
            boolean playStompSound,
            boolean chargeEnabled,
            int chargeMinIntervalTicks,
            int chargeMaxIntervalTicks,
            double chargeExtraPush
    ) {
    }

    public record GlobalHostilitySettings(
            double activationChance,
            boolean onlyNatural,
            boolean ignoreNamed,
            WorldFilter worldFilter,
            int maxActiveHostilePerChunk,
            int maxActiveHostilePerWorld,
            int maxProcessedPerTick,
            double peacefulDamageMultiplier,
            double easyDamageMultiplier,
            double normalDamageMultiplier,
            double hardDamageMultiplier,
            Map<String, Double> worldDamageMultipliers,
            boolean nightDamageEnabled,
            double nightDamageMultiplier,
            VisualEffectsSettings visualEffects,
            TargetingSettings targeting
    ) {
    }

    public record SocialAlertSettings(
            boolean enabled,
            boolean onDamage,
            boolean onNearbyDeath,
            boolean responderAdultsOnly,
            double radius,
            double radiusSq,
            int cooldownTicks,
            int joinCooldownTicks,
            int maxResponders
    ) {
    }

    public record ResourceProvocationSettings(
            boolean enabled,
            int leatherThreshold,
            int rawBeefThreshold,
            int boneThreshold,
            double detectionRadius,
            double detectionRadiusSq,
            int timeWindowTicks,
            int maxItemAgeTicks,
            boolean nightModifierEnabled,
            double nightThresholdMultiplier,
            boolean socialPropagationEnabled,
            int maxResponders,
            int triggerCooldownTicks,
            int aggressionDurationTicks
    ) {
    }

    public record VisualEffectsSettings(
            boolean glowEnabled,
            boolean particlesEnabled,
            int particlesIntervalTicks,
            double particlesIntensity,
            boolean soundEnabled,
            int soundIntervalTicks,
            double soundVolume
    ) {
    }
}

