package io.github.devskycore.faunareborn.animal.cow;

import io.github.devskycore.faunareborn.config.common.WorldFilter;
import io.github.devskycore.faunareborn.config.entity.EntitySettings;

import java.util.Map;

public record CowSettings(
        boolean enabled,
        MilkProvocationSettings milkProvocation,
        GlobalHostilitySettings globalHostility
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
            VisualEffectsSettings visualEffects
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
