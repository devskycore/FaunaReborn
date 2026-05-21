package io.github.devskycore.faunareborn.animal.cow.hostility;
import io.github.devskycore.faunareborn.animal.common.settings.CommonGlobalHostilitySettings;

import io.github.devskycore.faunareborn.animal.cow.CowSettings;
import io.github.devskycore.faunareborn.config.common.WorldFilter;
import io.github.devskycore.faunareborn.targeting.TargetEligibilityService;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.bukkit.Difficulty;
import org.bukkit.entity.Cow;
import org.bukkit.entity.Player;

import java.util.concurrent.ThreadLocalRandom;

final class CowActivationPolicy {

    private final double activationChance;
    private final boolean onlyNaturalCows;
    private final boolean ignoreNamed;
    private final double activationProximitySq;
    private final TargetEligibilityService targetEligibilityService;
    private final WorldFilter worldFilter;
    private final Int2ObjectOpenHashMap<ActivationState> activationStates = new Int2ObjectOpenHashMap<>();

    CowActivationPolicy(
            CowSettings.MilkProvocationSettings settings,
            CommonGlobalHostilitySettings global,
            TargetEligibilityService targetEligibilityService
    ) {
        this.activationChance = global.activationChance();
        this.onlyNaturalCows = global.onlyNatural();
        this.ignoreNamed = global.ignoreNamed();
        this.worldFilter = global.worldFilter();
        double activationProximity = Math.max(8.0D, settings.detectionRange());
        this.activationProximitySq = activationProximity * activationProximity;
        this.targetEligibilityService = targetEligibilityService;
    }

    void clear() {
        activationStates.clear();
    }

    void forget(int cowId) {
        activationStates.remove(cowId);
    }

    boolean isActivationBlocked(Cow cow, Player aggressor, boolean naturalCow) {
        if (cow == null || aggressor == null) {
            return true;
        }
        if (!cow.isAdult() || !cow.isValid() || cow.isDead()) {
            return true;
        }
        if (targetEligibilityService.isIneligible(cow, aggressor, worldFilter, -1L)) {
            return true;
        }
        if (cow.getWorld() != aggressor.getWorld()) {
            return true;
        }
        if (cow.getWorld().getDifficulty() == Difficulty.PEACEFUL) {
            return true;
        }
        if (ignoreNamed && cow.customName() != null) {
            return true;
        }
        if (onlyNaturalCows && !naturalCow) {
            return true;
        }
        if (distanceSq(cow, aggressor) > activationProximitySq) {
            return true;
        }

        ActivationState state = activationStates.get(cow.getEntityId());
        if (state == null) {
            state = new ActivationState();
            activationStates.put(cow.getEntityId(), state);
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

    private static double distanceSq(Cow cow, Player player) {
        double dx = cow.getX() - player.getX();
        double dy = cow.getY() - player.getY();
        double dz = cow.getZ() - player.getZ();
        return dx * dx + dy * dy + dz * dz;
    }

    private static final class ActivationState {
        private boolean chanceRolled;
        private boolean chancePassed;
    }
}
