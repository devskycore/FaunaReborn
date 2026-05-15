package io.github.devskycore.faunareborn.animal.cow;

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

public record CowSettings(
        boolean enabled,
        MilkProvocationSettings milkProvocation,
        ResourceProvocationSettings resourceProvocation,
        SocialAlertSettings socialAlert,
        GlobalHostilitySettings globalHostility,
        EnvironmentAggressionSettings environmentAggression,
        LodSettings lod
) implements EntitySettings {

    public static final class MilkProvocationSettings extends BaseProvocationSettings {
        public MilkProvocationSettings(
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
            super(
                    enabled,
                    aggressionDurationTicks,
                    forgetTargetAfterTicks,
                    milkingTriggerCooldownTicks,
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

        public int milkingTriggerCooldownTicks() {
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
            super(
                    enabled,
                    leatherThreshold,
                    rawBeefThreshold,
                    boneThreshold,
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

        public int leatherThreshold() {
            return thresholdOne();
        }

        public int rawBeefThreshold() {
            return thresholdTwo();
        }

        public int boneThreshold() {
            return thresholdThree();
        }
    }
}

