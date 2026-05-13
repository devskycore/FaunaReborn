package io.github.devskycore.faunareborn.animal.pig.hostility;

import io.github.devskycore.faunareborn.animal.pig.PigSettings;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.bukkit.Difficulty;
import org.bukkit.GameMode;
import org.bukkit.entity.Pig;
import org.bukkit.entity.Player;

import java.util.concurrent.ThreadLocalRandom;

final class PigActivationPolicy {

    private final double activationChance;
    private final boolean onlyNaturalPigs;
    private final boolean ignoreNamed;
    private final double activationProximitySq;
    private final Int2ObjectOpenHashMap<ActivationState> activationStates = new Int2ObjectOpenHashMap<>();

    PigActivationPolicy(
            PigSettings.RodProvocationSettings settings,
            PigSettings.GlobalHostilitySettings global
    ) {
        this.activationChance = global.activationChance();
        this.onlyNaturalPigs = global.onlyNatural();
        this.ignoreNamed = global.ignoreNamed();
        double activationProximity = Math.max(8.0D, settings.detectionRange());
        this.activationProximitySq = activationProximity * activationProximity;
    }

    void clear() {
        activationStates.clear();
    }

    void forget(int pigId) {
        activationStates.remove(pigId);
    }

    boolean isActivationBlocked(Pig pig, Player aggressor, boolean naturalPig) {
        if (pig == null || aggressor == null) {
            return true;
        }
        if (!pig.isAdult() || !pig.isValid() || pig.isDead()) {
            return true;
        }
        if (!aggressor.isOnline() || aggressor.isDead()) {
            return true;
        }
        GameMode mode = aggressor.getGameMode();
        if (mode == GameMode.CREATIVE || mode == GameMode.SPECTATOR) {
            return true;
        }
        if (pig.getWorld() != aggressor.getWorld()) {
            return true;
        }
        if (pig.getWorld().getDifficulty() == Difficulty.PEACEFUL) {
            return true;
        }
        if (ignoreNamed && pig.customName() != null) {
            return true;
        }
        if (onlyNaturalPigs && !naturalPig) {
            return true;
        }
        if (distanceSq(pig, aggressor) > activationProximitySq) {
            return true;
        }

        ActivationState state = activationStates.get(pig.getEntityId());
        if (state == null) {
            state = new ActivationState();
            activationStates.put(pig.getEntityId(), state);
        }
        if (!state.chanceRolled) {
            state.chanceRolled = true;
            state.chancePassed = rollActivationChance();
        }
        return !state.chancePassed;
    }

    private boolean rollActivationChance() {
        if (activationChance <= 0.0D) {
            return false;
        }
        if (activationChance >= 1.0D) {
            return true;
        }
        return ThreadLocalRandom.current().nextDouble() < activationChance;
    }

    private static double distanceSq(Pig pig, Player player) {
        double dx = pig.getX() - player.getX();
        double dy = pig.getY() - player.getY();
        double dz = pig.getZ() - player.getZ();
        return dx * dx + dy * dy + dz * dz;
    }

    private static final class ActivationState {
        private boolean chanceRolled;
        private boolean chancePassed;
    }
}

