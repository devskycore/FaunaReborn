package io.github.devskycore.faunareborn.animal.pig;

import io.github.devskycore.faunareborn.animal.common.settings.BaseResourceProvocationSettings;
import io.github.devskycore.faunareborn.animal.common.settings.BaseGlobalHostilitySettings;
import io.github.devskycore.faunareborn.animal.common.settings.BaseProvocationSettings;
import io.github.devskycore.faunareborn.animal.common.settings.BaseSocialAlertSettings;
import io.github.devskycore.faunareborn.animal.common.settings.SharedVisualEffectsSettings;
import io.github.devskycore.faunareborn.config.common.WorldFilter;
import io.github.devskycore.faunareborn.config.common.TargetingSettings;
import io.github.devskycore.faunareborn.config.entity.EntitySettings;
import io.github.devskycore.faunareborn.system.lod.LodSettings;

import java.util.Map;
import io.github.devskycore.faunareborn.system.environment.EnvironmentAggressionSettings;

public record PigSettings(
        boolean enabled,
        RodProvocationSettings rodProvocation,
        ResourceProvocationSettings resourceProvocation,
        SocialAlertSettings socialAlert,
        GlobalHostilitySettings globalHostility,
        EnvironmentAggressionSettings environmentAggression,
        LodSettings lod
) implements EntitySettings {

    public static final class RodProvocationSettings extends BaseProvocationSettings {
        public RodProvocationSettings(
                boolean enabled,
                int aggressionDurationTicks,
                int forgetTargetAfterTicks,
                int rodTriggerCooldownTicks,
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
            super(
                    enabled,
                    aggressionDurationTicks,
                    forgetTargetAfterTicks,
                    rodTriggerCooldownTicks,
                    detectionRange,
                    detectionRangeSq,
                    requireLineOfSight,
                    warningDurationTicks,
                    attackDamage,
                    attackCooldownTicks,
                    knockbackStrength,
                    speedMultiplier,
                    retargetGraceTicks,
                    playAggressiveSounds,
                    playWarningSound,
                    playStompSound,
                    chargeEnabled,
                    chargeMinIntervalTicks,
                    chargeMaxIntervalTicks,
                    chargeExtraPush
            );
        }

        public int rodTriggerCooldownTicks() {
            return triggerCooldownTicks();
        }
    }

    public static final class GlobalHostilitySettings extends BaseGlobalHostilitySettings {
        public GlobalHostilitySettings(
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
                SharedVisualEffectsSettings visualEffects,
                TargetingSettings targeting
        ) {
            super(
                    activationChance,
                    onlyNatural,
                    ignoreNamed,
                    worldFilter,
                    maxActiveHostilePerChunk,
                    maxActiveHostilePerWorld,
                    maxProcessedPerTick,
                    peacefulDamageMultiplier,
                    easyDamageMultiplier,
                    normalDamageMultiplier,
                    hardDamageMultiplier,
                    worldDamageMultipliers,
                    nightDamageEnabled,
                    nightDamageMultiplier,
                    visualEffects,
                    targeting
            );
        }
    }

    public static final class SocialAlertSettings extends BaseSocialAlertSettings {
        public SocialAlertSettings(
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
            super(
                    enabled,
                    onDamage,
                    onNearbyDeath,
                    responderAdultsOnly,
                    radius,
                    radiusSq,
                    cooldownTicks,
                    joinCooldownTicks,
                    maxResponders
            );
        }
    }

    public static final class ResourceProvocationSettings extends BaseResourceProvocationSettings {
        public ResourceProvocationSettings(
                boolean enabled,
                int carrotThreshold,
                int appleThreshold,
                int rawPorkchopThreshold,
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
            super(
                    enabled,
                    carrotThreshold,
                    appleThreshold,
                    rawPorkchopThreshold,
                    detectionRadius,
                    detectionRadiusSq,
                    timeWindowTicks,
                    maxItemAgeTicks,
                    nightModifierEnabled,
                    nightThresholdMultiplier,
                    socialPropagationEnabled,
                    maxResponders,
                    triggerCooldownTicks,
                    aggressionDurationTicks
            );
        }

        public int carrotThreshold() {
            return thresholdOne();
        }

        public int appleThreshold() {
            return thresholdTwo();
        }

        public int rawPorkchopThreshold() {
            return thresholdThree();
        }
    }
}


