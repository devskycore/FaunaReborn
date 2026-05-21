package io.github.devskycore.faunareborn.animal.cow;

import io.github.devskycore.faunareborn.animal.common.settings.BaseResourceProvocationSettings;
import io.github.devskycore.faunareborn.animal.common.settings.BaseProvocationSettings;
import io.github.devskycore.faunareborn.animal.common.settings.CommonGlobalHostilitySettings;
import io.github.devskycore.faunareborn.animal.common.settings.CommonSocialAlertSettings;
import io.github.devskycore.faunareborn.config.entity.EntitySettings;
import io.github.devskycore.faunareborn.system.lod.LodSettings;

import io.github.devskycore.faunareborn.system.environment.EnvironmentAggressionSettings;

public record CowSettings(
        boolean enabled,
        MilkProvocationSettings milkProvocation,
        ResourceProvocationSettings resourceProvocation,
        CommonSocialAlertSettings socialAlert,
        CommonGlobalHostilitySettings globalHostility,
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

