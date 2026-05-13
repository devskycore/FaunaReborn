package io.github.devskycore.faunareborn.animal.chicken.config;

import io.github.devskycore.faunareborn.config.entity.EntitySettings;
import io.github.devskycore.faunareborn.config.common.WorldFilter;
import io.github.devskycore.faunareborn.system.environment.EnvironmentAggressionSettings;
import java.util.Map;
import java.util.Objects;

public record ChickenHostilitySettings(
        boolean enabled,
        Combat combat,
        Limits limits,
        SocialAlert socialAlert,
        Visuals visuals,
        Movement movement,
        DamageScaling damageScaling,
        ActivationConfig activation,
        WorldFilter worldFilter,
        ItemPickupTerritorialityConfig itemPickupTerritoriality,
        EnvironmentAggressionSettings environmentAggression
) implements EntitySettings {

    public ChickenHostilitySettings {
        Objects.requireNonNull(combat, "combat");
        Objects.requireNonNull(limits, "limits");
        Objects.requireNonNull(socialAlert, "socialAlert");
        Objects.requireNonNull(visuals, "visuals");
        Objects.requireNonNull(movement, "movement");
        Objects.requireNonNull(damageScaling, "damageScaling");
        Objects.requireNonNull(activation, "activation");
        Objects.requireNonNull(worldFilter, "worldFilter");
        Objects.requireNonNull(itemPickupTerritoriality, "itemPickupTerritoriality");
        Objects.requireNonNull(environmentAggression, "environmentAggression");
    }

    public record Combat(
            double attackDamage,
            int globalTargetCooldownTicks,
            int maxSimultaneousAttackersPerPlayer,
            int attackCooldownTicks,
            int threatTimeoutTicks,
            int retargetGraceTicks,
            int noLineOfSightResetTicks,
            double detectionRadius,
            double attackRange
    ) {
    }

    public record Limits(
            int maxActiveHostileChickensPerChunk,
            int maxActiveHostileChickensPerWorld,
            int maxProcessedChickensPerTick
    ) {
    }

    public record SocialAlert(
            boolean enabled,
            boolean onDamage,
            boolean onNearbyDeath,
            boolean responderAdultsOnly,
            double radius,
            int cooldownTicks,
            int joinCooldownTicks,
            int maxResponders
    ) {
    }

    public record Visuals(
            boolean glowEnabled,
            boolean particlesEnabled,
            int particlesIntervalTicks,
            double particlesVolume,
            boolean soundEnabled,
            int soundIntervalTicks,
            double soundVolume
    ) {
    }

    public record Movement(
            double speedMultiplier,
            double distanceBoostStartDistance,
            double distanceBoostExtraSpeedPerBlock,
            double distanceBoostMaxMultiplier,
            boolean terrainJumpEnabled,
            double terrainJumpVerticalBoost,
            int terrainJumpCooldownTicks,
            double terrainJumpTriggerHeightDelta
    ) {
    }

    public record DamageScaling(
            double peacefulDamageMultiplier,
            double easyDamageMultiplier,
            double normalDamageMultiplier,
            double hardDamageMultiplier,
            Map<String, Double> worldDamageMultipliers,
            boolean nightDamageEnabled,
            double nightDamageMultiplier
    ) {

        public DamageScaling {
            worldDamageMultipliers = worldDamageMultipliers == null ? Map.of() : Map.copyOf(worldDamageMultipliers);
        }
    }
}



