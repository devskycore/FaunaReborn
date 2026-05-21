package io.github.devskycore.faunareborn.animal.pig;

import io.github.devskycore.faunareborn.animal.common.settings.BaseResourceProvocationSettings;
import io.github.devskycore.faunareborn.animal.common.settings.BaseProvocationSettings;
import io.github.devskycore.faunareborn.animal.common.settings.CommonGlobalHostilitySettings;
import io.github.devskycore.faunareborn.animal.common.settings.CommonSocialAlertSettings;
import io.github.devskycore.faunareborn.config.entity.EntitySettings;
import io.github.devskycore.faunareborn.system.lod.LodSettings;

import io.github.devskycore.faunareborn.system.environment.EnvironmentAggressionSettings;

public record PigSettings(
        boolean enabled,
        RodProvocationSettings rodProvocation,
        ResourceProvocationSettings resourceProvocation,
        CommonSocialAlertSettings socialAlert,
        CommonGlobalHostilitySettings globalHostility,
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


