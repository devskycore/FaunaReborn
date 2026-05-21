package io.github.devskycore.faunareborn.animal.common.settings;

import io.github.devskycore.faunareborn.config.common.TargetingSettings;
import io.github.devskycore.faunareborn.config.common.WorldFilter;

import java.util.Map;

public class CommonGlobalHostilitySettings extends BaseGlobalHostilitySettings {
    public CommonGlobalHostilitySettings(
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
