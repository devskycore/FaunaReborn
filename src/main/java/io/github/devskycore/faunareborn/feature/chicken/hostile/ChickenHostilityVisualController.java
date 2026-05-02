package io.github.devskycore.faunareborn.feature.chicken.hostile;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Chicken;

import java.util.concurrent.ThreadLocalRandom;

final class ChickenHostilityVisualController {

    private static final int MIN_INTERVAL_TICKS = 1;
    private static final int MAX_INTERVAL_TICKS = 1200;
    private static final double MIN_VISUAL_VOLUME = 0.0D;
    private static final double MAX_VISUAL_VOLUME = 5.0D;
    private static final double PARTICLE_Y_OFFSET = 0.45D;

    private final boolean glowEnabled;
    private final boolean particlesEnabled;
    private final int particlesIntervalTicks;
    private final double particlesVolume;
    private final boolean soundEnabled;
    private final int soundIntervalTicks;
    private final float soundVolume;
    private final Int2ObjectOpenHashMap<VisualState> activeVisuals = new Int2ObjectOpenHashMap<>();

    ChickenHostilityVisualController(
            boolean glowEnabled,
            boolean particlesEnabled,
            int particlesIntervalTicks,
            double particlesVolume,
            boolean soundEnabled,
            int soundIntervalTicks,
            double soundVolume
    ) {
        this.glowEnabled = glowEnabled;
        this.particlesEnabled = particlesEnabled;
        this.particlesIntervalTicks = Math.clamp(particlesIntervalTicks, MIN_INTERVAL_TICKS, MAX_INTERVAL_TICKS);
        this.particlesVolume = Math.clamp(particlesVolume, MIN_VISUAL_VOLUME, MAX_VISUAL_VOLUME);
        this.soundEnabled = soundEnabled;
        this.soundIntervalTicks = Math.clamp(soundIntervalTicks, MIN_INTERVAL_TICKS, MAX_INTERVAL_TICKS);
        this.soundVolume = (float) Math.clamp(soundVolume, MIN_VISUAL_VOLUME, MAX_VISUAL_VOLUME);
    }

    void sync(int chickenId, Chicken chicken, ChickenHostilityBrain brain, long currentTick) {
        if (brain != null && isAggressiveState(brain.state) && chicken.isValid() && !chicken.isDead()) {
            activate(chickenId, chicken, currentTick);
            return;
        }

        deactivate(chickenId, chicken);
    }

    void deactivate(int chickenId, Chicken chicken) {
        VisualState visualState = activeVisuals.remove(chickenId);
        if (visualState == null || chicken == null || !chicken.isValid()) {
            return;
        }

        chicken.setGlowing(visualState.originallyGlowing);
    }

    void clearAll(Int2ObjectOpenHashMap<Chicken> trackedChickens) {
        if (activeVisuals.isEmpty()) {
            return;
        }

        for (var iterator = activeVisuals.int2ObjectEntrySet().fastIterator(); iterator.hasNext(); ) {
            Int2ObjectMap.Entry<VisualState> entry = iterator.next();
            Chicken chicken = trackedChickens.get(entry.getIntKey());
            if (chicken != null && chicken.isValid()) {
                chicken.setGlowing(entry.getValue().originallyGlowing);
            }
        }
        activeVisuals.clear();
    }

    void tick(
            long currentTick,
            Int2ObjectOpenHashMap<Chicken> trackedChickens,
            Int2ObjectOpenHashMap<ChickenHostilityBrain> brains
    ) {
        if (activeVisuals.isEmpty()) {
            return;
        }

        for (var iterator = activeVisuals.int2ObjectEntrySet().fastIterator(); iterator.hasNext(); ) {
            Int2ObjectMap.Entry<VisualState> entry = iterator.next();
            int chickenId = entry.getIntKey();
            Chicken chicken = trackedChickens.get(chickenId);
            if (chicken == null) {
                iterator.remove();
                continue;
            }
            if (!chicken.isValid() || chicken.isDead()) {
                if (chicken.isValid()) {
                    chicken.setGlowing(entry.getValue().originallyGlowing);
                }
                iterator.remove();
                continue;
            }

            ChickenHostilityBrain brain = brains.get(chickenId);
            if (brain == null || !isAggressiveState(brain.state)) {
                chicken.setGlowing(entry.getValue().originallyGlowing);
                iterator.remove();
                continue;
            }

            VisualState visualState = entry.getValue();
            if (glowEnabled && !chicken.isGlowing()) {
                chicken.setGlowing(true);
            }

            if (particlesEnabled && currentTick >= visualState.nextParticleTick) {
                spawnSubtleParticles(chicken);
                visualState.nextParticleTick = currentTick + nextParticleIntervalTicks();
            }

            if (soundEnabled && currentTick >= visualState.nextSoundTick) {
                playSubtleSound(chicken);
                visualState.nextSoundTick = currentTick + nextSoundIntervalTicks();
            }
        }
    }

    private void activate(int chickenId, Chicken chicken, long currentTick) {
        VisualState visualState = activeVisuals.get(chickenId);
        if (visualState == null) {
            visualState = new VisualState(
                    chicken.isGlowing(),
                    currentTick + Math.floorMod(chickenId, particlesIntervalTicks),
                    currentTick + nextSoundIntervalTicks()
            );
            activeVisuals.put(chickenId, visualState);
        }

        if (glowEnabled && !chicken.isGlowing()) {
            chicken.setGlowing(true);
        }
    }

    private boolean isAggressiveState(ChickenHostilityState state) {
        return state == ChickenHostilityState.ALERT
                || state == ChickenHostilityState.CHASE
                || state == ChickenHostilityState.ATTACK;
    }

    private void spawnSubtleParticles(Chicken chicken) {
        chicken.getWorld().spawnParticle(
                selectParticle(),
                chicken.getX(),
                chicken.getY() + PARTICLE_Y_OFFSET,
                chicken.getZ(),
                1,
                0.28D,
                0.18D,
                0.28D,
                particlesVolume
        );
    }

    private Particle selectParticle() {
        int roll = ThreadLocalRandom.current().nextInt(100);
        if (roll < 50) return Particle.SMOKE;
        if (roll < 78) return Particle.ASH;
        if (roll < 92) return Particle.SOUL;
        return Particle.WITCH;
    }

    private void playSubtleSound(Chicken chicken) {
        chicken.getWorld().playSound(
                chicken,
                Sound.ENTITY_CHICKEN_AMBIENT,
                SoundCategory.HOSTILE,
                soundVolume,
                0.55F
        );
    }

    private int nextParticleIntervalTicks() {
        return particlesIntervalTicks;
    }

    private int nextSoundIntervalTicks() {
        return soundIntervalTicks;
    }

    private static final class VisualState {
        private final boolean originallyGlowing;
        private long nextParticleTick;
        private long nextSoundTick;

        private VisualState(boolean originallyGlowing, long nextParticleTick, long nextSoundTick) {
            this.originallyGlowing = originallyGlowing;
            this.nextParticleTick = nextParticleTick;
            this.nextSoundTick = nextSoundTick;
        }
    }
}
