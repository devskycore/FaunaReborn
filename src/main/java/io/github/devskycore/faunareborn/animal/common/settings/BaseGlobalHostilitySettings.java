package io.github.devskycore.faunareborn.animal.common.settings;

import io.github.devskycore.faunareborn.config.common.TargetingSettings;
import io.github.devskycore.faunareborn.config.common.WorldFilter;

import java.util.Map;

public abstract class BaseGlobalHostilitySettings {

    private final double activationChance;
    private final boolean onlyNatural;
    private final boolean ignoreNamed;
    private final WorldFilter worldFilter;
    private final int maxActiveHostilePerChunk;
    private final int maxActiveHostilePerWorld;
    private final int maxProcessedPerTick;
    private final double peacefulDamageMultiplier;
    private final double easyDamageMultiplier;
    private final double normalDamageMultiplier;
    private final double hardDamageMultiplier;
    private final Map<String, Double> worldDamageMultipliers;
    private final boolean nightDamageEnabled;
    private final double nightDamageMultiplier;
    private final SharedVisualEffectsSettings visualEffects;
    private final TargetingSettings targeting;

    protected BaseGlobalHostilitySettings(
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
        this.activationChance = activationChance;
        this.onlyNatural = onlyNatural;
        this.ignoreNamed = ignoreNamed;
        this.worldFilter = worldFilter;
        this.maxActiveHostilePerChunk = maxActiveHostilePerChunk;
        this.maxActiveHostilePerWorld = maxActiveHostilePerWorld;
        this.maxProcessedPerTick = maxProcessedPerTick;
        this.peacefulDamageMultiplier = peacefulDamageMultiplier;
        this.easyDamageMultiplier = easyDamageMultiplier;
        this.normalDamageMultiplier = normalDamageMultiplier;
        this.hardDamageMultiplier = hardDamageMultiplier;
        this.worldDamageMultipliers = worldDamageMultipliers;
        this.nightDamageEnabled = nightDamageEnabled;
        this.nightDamageMultiplier = nightDamageMultiplier;
        this.visualEffects = visualEffects;
        this.targeting = targeting;
    }

    public final double activationChance() {
        return activationChance;
    }

    public final boolean onlyNatural() {
        return onlyNatural;
    }

    public final boolean ignoreNamed() {
        return ignoreNamed;
    }

    public final WorldFilter worldFilter() {
        return worldFilter;
    }

    public final int maxActiveHostilePerChunk() {
        return maxActiveHostilePerChunk;
    }

    public final int maxActiveHostilePerWorld() {
        return maxActiveHostilePerWorld;
    }

    public final int maxProcessedPerTick() {
        return maxProcessedPerTick;
    }

    public final double peacefulDamageMultiplier() {
        return peacefulDamageMultiplier;
    }

    public final double easyDamageMultiplier() {
        return easyDamageMultiplier;
    }

    public final double normalDamageMultiplier() {
        return normalDamageMultiplier;
    }

    public final double hardDamageMultiplier() {
        return hardDamageMultiplier;
    }

    public final Map<String, Double> worldDamageMultipliers() {
        return worldDamageMultipliers;
    }

    public final boolean nightDamageEnabled() {
        return nightDamageEnabled;
    }

    public final double nightDamageMultiplier() {
        return nightDamageMultiplier;
    }

    public final SharedVisualEffectsSettings visualEffects() {
        return visualEffects;
    }

    public final TargetingSettings targeting() {
        return targeting;
    }
}
