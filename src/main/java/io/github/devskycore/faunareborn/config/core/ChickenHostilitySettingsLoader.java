package io.github.devskycore.faunareborn.config.core;

import io.github.devskycore.faunareborn.config.combat.CombatConfigReader;
import io.github.devskycore.faunareborn.config.combat.CombatConfigValues;
import io.github.devskycore.faunareborn.config.combat.DamageScalingConfigReader;
import io.github.devskycore.faunareborn.core.FaunaRebornPlugin;
import io.github.devskycore.faunareborn.config.movement.MovementConfigReader;
import io.github.devskycore.faunareborn.config.movement.MovementConfigValues;
import io.github.devskycore.faunareborn.config.night.NightBehaviorConfigReader;
import io.github.devskycore.faunareborn.config.night.NightBehaviorConfigValues;
import io.github.devskycore.faunareborn.config.world.WorldFilter;
import io.github.devskycore.faunareborn.config.world.WorldFilterConfigReader;
import org.bukkit.Difficulty;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Map;

public final class ChickenHostilitySettingsLoader {

    private final FaunaRebornPlugin plugin;
    private final CombatConfigReader combatReader;
    private final ThreatAndCooldownConfigReader threatAndCooldownReader;
    private final ProcessingLimitsConfigReader processingLimitsReader;
    private final ActivationConfigReader activationReader;
    private final MovementConfigReader movementReader;
    private final WorldFilterConfigReader worldFilterReader;
    private final DamageScalingConfigReader damageScalingReader;
    private final NightBehaviorConfigReader nightBehaviorReader;

    public ChickenHostilitySettingsLoader(FaunaRebornPlugin plugin) {
        this.plugin = plugin;
        this.combatReader = new CombatConfigReader(plugin);
        this.threatAndCooldownReader = new ThreatAndCooldownConfigReader(plugin);
        this.processingLimitsReader = new ProcessingLimitsConfigReader(plugin);
        this.activationReader = new ActivationConfigReader(plugin);
        this.movementReader = new MovementConfigReader(plugin);
        this.worldFilterReader = new WorldFilterConfigReader(plugin);
        this.damageScalingReader = new DamageScalingConfigReader(plugin);
        this.nightBehaviorReader = new NightBehaviorConfigReader(plugin);
    }

    public ChickenHostilitySettings load(FileConfiguration config) {
        CombatConfigValues combat = combatReader.read(config);
        ThreatAndCooldownConfigValues threatAndCooldown = threatAndCooldownReader.read(config);
        ProcessingLimitsConfigValues processingLimits = processingLimitsReader.read(config);
        ActivationConfig activation = activationReader.read(config);
        MovementConfigValues movement = movementReader.read(config);
        WorldFilter worldFilter = worldFilterReader.readWorldFilter(config);
        NightBehaviorConfigValues nightBehavior = nightBehaviorReader.read(config);
        ConfigNumbers numbers = new ConfigNumbers(plugin);

        double peacefulDamageMultiplier = damageScalingReader.readDifficultyDamageMultiplier(config, Difficulty.PEACEFUL, 0.0D);
        double easyDamageMultiplier = damageScalingReader.readDifficultyDamageMultiplier(config, Difficulty.EASY, 1.0D);
        double normalDamageMultiplier = damageScalingReader.readDifficultyDamageMultiplier(config, Difficulty.NORMAL, 1.0D);
        double hardDamageMultiplier = damageScalingReader.readDifficultyDamageMultiplier(config, Difficulty.HARD, 1.2D);
        Map<String, Double> worldDamageMultipliers = damageScalingReader.readWorldDamageMultipliers(config);

        return new ChickenHostilitySettings(
                config.getBoolean("chicken-hostility.enabled", true),
                combat.attackDamage(),
                threatAndCooldown.globalTargetCooldownTicks(),
                combat.maxSimultaneousAttackers(),
                processingLimits.maxActiveHostileChickensPerChunk(),
                processingLimits.maxActiveHostileChickensPerWorld(),
                processingLimits.maxProcessedChickensPerTick(),
                threatAndCooldown.attackCooldownTicks(),
                threatAndCooldown.threatTimeoutTicks(),
                threatAndCooldown.retargetGraceTicks(),
                threatAndCooldown.noLineOfSightResetTicks(),
                threatAndCooldown.socialAlertEnabled(),
                threatAndCooldown.socialAlertOnDamage(),
                threatAndCooldown.socialAlertOnNearbyDeath(),
                threatAndCooldown.socialAlertResponderAdultsOnly(),
                threatAndCooldown.socialAlertRadius(),
                threatAndCooldown.socialAlertCooldownTicks(),
                threatAndCooldown.socialAlertJoinCooldownTicks(),
                threatAndCooldown.socialAlertMaxResponders(),
                config.getBoolean("chicken-hostility.visual-effects.glow.enabled", PluginConfigDefaults.VISUAL_GLOW_ENABLED),
                config.getBoolean("chicken-hostility.visual-effects.particles.enabled", PluginConfigDefaults.VISUAL_PARTICLES_ENABLED),
                numbers.intRange(
                        config.getInt(
                                "chicken-hostility.visual-effects.particles.interval-ticks",
                                PluginConfigDefaults.VISUAL_PARTICLES_INTERVAL_TICKS
                        ),
                        1,
                        PluginConfigDefaults.VISUAL_PARTICLES_INTERVAL_TICKS_LIMIT,
                        PluginConfigDefaults.VISUAL_PARTICLES_INTERVAL_TICKS,
                        "Invalid chicken-hostility.visual-effects.particles.interval-ticks in config.yml. Falling back to 8",
                        "chicken-hostility.visual-effects.particles.interval-ticks is too high. Clamped to 200"
                ),
                numbers.finiteRange(
                        config.getDouble(
                                "chicken-hostility.visual-effects.particles.intensity",
                                PluginConfigDefaults.VISUAL_PARTICLES_VOLUME
                        ),
                        0.0D,
                        PluginConfigDefaults.VISUAL_PARTICLES_VOLUME_LIMIT,
                        PluginConfigDefaults.VISUAL_PARTICLES_VOLUME,
                        "Invalid chicken-hostility.visual-effects.particles.intensity in config.yml. Falling back to 1.0",
                        "chicken-hostility.visual-effects.particles.intensity is too high. Clamped to 5.0"
                ),
                config.getBoolean("chicken-hostility.visual-effects.sound.enabled", PluginConfigDefaults.VISUAL_SOUND_ENABLED),
                numbers.intRange(
                        config.getInt(
                                "chicken-hostility.visual-effects.sound.interval-ticks",
                                PluginConfigDefaults.VISUAL_SOUND_INTERVAL_TICKS
                        ),
                        1,
                        PluginConfigDefaults.VISUAL_SOUND_INTERVAL_TICKS_LIMIT,
                        PluginConfigDefaults.VISUAL_SOUND_INTERVAL_TICKS,
                        "Invalid chicken-hostility.visual-effects.sound.interval-ticks in config.yml. Falling back to 160",
                        "chicken-hostility.visual-effects.sound.interval-ticks is too high. Clamped to 1200"
                ),
                numbers.finiteRange(
                        config.getDouble(
                                "chicken-hostility.visual-effects.sound.volume",
                                PluginConfigDefaults.VISUAL_SOUND_VOLUME
                        ),
                        0.0D,
                        PluginConfigDefaults.VISUAL_SOUND_VOLUME_LIMIT,
                        PluginConfigDefaults.VISUAL_SOUND_VOLUME,
                        "Invalid chicken-hostility.visual-effects.sound.volume in config.yml. Falling back to 0.18",
                        "chicken-hostility.visual-effects.sound.volume is too high. Clamped to 5.0"
                ),
                activation,
                combat.detectionRadius(),
                combat.attackRange(),
                movement.movementSpeedMultiplier(),
                movement.movementDistanceBoostStartDistance(),
                movement.movementDistanceBoostExtraSpeedPerBlock(),
                movement.movementDistanceBoostMaxMultiplier(),
                movement.movementTerrainJumpEnabled(),
                movement.movementTerrainJumpVerticalBoost(),
                movement.movementTerrainJumpCooldownTicks(),
                movement.movementTerrainJumpTriggerHeightDelta(),
                worldFilter,
                peacefulDamageMultiplier,
                easyDamageMultiplier,
                normalDamageMultiplier,
                hardDamageMultiplier,
                worldDamageMultipliers,
                nightBehavior.nightDamageEnabled(),
                nightBehavior.nightDamageMultiplier()
        );
    }

}

