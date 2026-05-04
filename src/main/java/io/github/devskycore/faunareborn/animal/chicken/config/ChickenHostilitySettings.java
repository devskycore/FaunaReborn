package io.github.devskycore.faunareborn.animal.chicken.config;

import io.github.devskycore.faunareborn.config.entity.EntitySettings;
import io.github.devskycore.faunareborn.config.common.WorldFilter;
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
        ItemPickupTerritorialityConfig itemPickupTerritoriality
) implements EntitySettings {

    public ChickenHostilitySettings {
        combat = Objects.requireNonNull(combat, "combat");
        limits = Objects.requireNonNull(limits, "limits");
        socialAlert = Objects.requireNonNull(socialAlert, "socialAlert");
        visuals = Objects.requireNonNull(visuals, "visuals");
        movement = Objects.requireNonNull(movement, "movement");
        damageScaling = Objects.requireNonNull(damageScaling, "damageScaling");
        activation = Objects.requireNonNull(activation, "activation");
        worldFilter = Objects.requireNonNull(worldFilter, "worldFilter");
        itemPickupTerritoriality = Objects.requireNonNull(itemPickupTerritoriality, "itemPickupTerritoriality");
    }

    public ChickenHostilitySettings(
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
            double nightDamageMultiplier,
            ItemPickupTerritorialityConfig itemPickupTerritoriality
    ) {
        this(
                enabled,
                new Combat(
                        attackDamage,
                        globalTargetCooldownTicks,
                        maxSimultaneousAttackersPerPlayer,
                        attackCooldownTicks,
                        threatTimeoutTicks,
                        retargetGraceTicks,
                        noLineOfSightResetTicks,
                        detectionRadius,
                        attackRange
                ),
                new Limits(
                        maxActiveHostileChickensPerChunk,
                        maxActiveHostileChickensPerWorld,
                        maxProcessedChickensPerTick
                ),
                new SocialAlert(
                        socialAlertEnabled,
                        socialAlertOnDamage,
                        socialAlertOnNearbyDeath,
                        socialAlertResponderAdultsOnly,
                        socialAlertRadius,
                        socialAlertCooldownTicks,
                        socialAlertJoinCooldownTicks,
                        socialAlertMaxResponders
                ),
                new Visuals(
                        visualGlowEnabled,
                        visualParticlesEnabled,
                        visualParticlesIntervalTicks,
                        visualParticlesVolume,
                        visualSoundEnabled,
                        visualSoundIntervalTicks,
                        visualSoundVolume
                ),
                new Movement(
                        movementSpeedMultiplier,
                        movementDistanceBoostStartDistance,
                        movementDistanceBoostExtraSpeedPerBlock,
                        movementDistanceBoostMaxMultiplier,
                        movementTerrainJumpEnabled,
                        movementTerrainJumpVerticalBoost,
                        movementTerrainJumpCooldownTicks,
                        movementTerrainJumpTriggerHeightDelta
                ),
                new DamageScaling(
                        peacefulDamageMultiplier,
                        easyDamageMultiplier,
                        normalDamageMultiplier,
                        hardDamageMultiplier,
                        worldDamageMultipliers,
                        nightDamageEnabled,
                        nightDamageMultiplier
                ),
                activation,
                worldFilter,
                itemPickupTerritoriality
        );
    }

    public double attackDamage() {
        return combat.attackDamage();
    }

    public int globalTargetCooldownTicks() {
        return combat.globalTargetCooldownTicks();
    }

    public int maxSimultaneousAttackersPerPlayer() {
        return combat.maxSimultaneousAttackersPerPlayer();
    }

    public int maxActiveHostileChickensPerChunk() {
        return limits.maxActiveHostileChickensPerChunk();
    }

    public int maxActiveHostileChickensPerWorld() {
        return limits.maxActiveHostileChickensPerWorld();
    }

    public int maxProcessedChickensPerTick() {
        return limits.maxProcessedChickensPerTick();
    }

    public int attackCooldownTicks() {
        return combat.attackCooldownTicks();
    }

    public int threatTimeoutTicks() {
        return combat.threatTimeoutTicks();
    }

    public int retargetGraceTicks() {
        return combat.retargetGraceTicks();
    }

    public int noLineOfSightResetTicks() {
        return combat.noLineOfSightResetTicks();
    }

    public boolean socialAlertEnabled() {
        return socialAlert.enabled();
    }

    public boolean socialAlertOnDamage() {
        return socialAlert.onDamage();
    }

    public boolean socialAlertOnNearbyDeath() {
        return socialAlert.onNearbyDeath();
    }

    public boolean socialAlertResponderAdultsOnly() {
        return socialAlert.responderAdultsOnly();
    }

    public double socialAlertRadius() {
        return socialAlert.radius();
    }

    public int socialAlertCooldownTicks() {
        return socialAlert.cooldownTicks();
    }

    public int socialAlertJoinCooldownTicks() {
        return socialAlert.joinCooldownTicks();
    }

    public int socialAlertMaxResponders() {
        return socialAlert.maxResponders();
    }

    public boolean visualGlowEnabled() {
        return visuals.glowEnabled();
    }

    public boolean visualParticlesEnabled() {
        return visuals.particlesEnabled();
    }

    public int visualParticlesIntervalTicks() {
        return visuals.particlesIntervalTicks();
    }

    public double visualParticlesVolume() {
        return visuals.particlesVolume();
    }

    public boolean visualSoundEnabled() {
        return visuals.soundEnabled();
    }

    public int visualSoundIntervalTicks() {
        return visuals.soundIntervalTicks();
    }

    public double visualSoundVolume() {
        return visuals.soundVolume();
    }

    public double detectionRadius() {
        return combat.detectionRadius();
    }

    public double attackRange() {
        return combat.attackRange();
    }

    public double movementSpeedMultiplier() {
        return movement.speedMultiplier();
    }

    public double movementDistanceBoostStartDistance() {
        return movement.distanceBoostStartDistance();
    }

    public double movementDistanceBoostExtraSpeedPerBlock() {
        return movement.distanceBoostExtraSpeedPerBlock();
    }

    public double movementDistanceBoostMaxMultiplier() {
        return movement.distanceBoostMaxMultiplier();
    }

    public boolean movementTerrainJumpEnabled() {
        return movement.terrainJumpEnabled();
    }

    public double movementTerrainJumpVerticalBoost() {
        return movement.terrainJumpVerticalBoost();
    }

    public int movementTerrainJumpCooldownTicks() {
        return movement.terrainJumpCooldownTicks();
    }

    public double movementTerrainJumpTriggerHeightDelta() {
        return movement.terrainJumpTriggerHeightDelta();
    }

    public double peacefulDamageMultiplier() {
        return damageScaling.peacefulDamageMultiplier();
    }

    public double easyDamageMultiplier() {
        return damageScaling.easyDamageMultiplier();
    }

    public double normalDamageMultiplier() {
        return damageScaling.normalDamageMultiplier();
    }

    public double hardDamageMultiplier() {
        return damageScaling.hardDamageMultiplier();
    }

    public Map<String, Double> worldDamageMultipliers() {
        return damageScaling.worldDamageMultipliers();
    }

    public boolean nightDamageEnabled() {
        return damageScaling.nightDamageEnabled();
    }

    public double nightDamageMultiplier() {
        return damageScaling.nightDamageMultiplier();
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


