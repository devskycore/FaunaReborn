package io.github.devskycore.faunareborn.animal.chicken.hostility;

import io.github.devskycore.faunareborn.animal.chicken.config.ActivationConfig;
import io.github.devskycore.faunareborn.config.common.WorldFilter;
import io.github.devskycore.faunareborn.core.FaunaRebornPlugin;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.bukkit.Difficulty;
import org.bukkit.GameMode;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Chicken;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

final class ActivationPolicy {

    private static final byte TRUE_BYTE = 1;

    private final double activationChance;
    private final boolean onlyNaturalChickens;
    private final boolean ignoreNamed;
    private final WorldFilter worldFilter;
    private final NamespacedKey nonNaturalChickenKey;
    private final Int2ObjectOpenHashMap<ActivationState> activationStates = new Int2ObjectOpenHashMap<>();

    ActivationPolicy(FaunaRebornPlugin plugin, ActivationConfig activation, WorldFilter worldFilter) {
        this.activationChance = activation.chance();
        this.onlyNaturalChickens = activation.onlyNaturalChickens();
        this.ignoreNamed = activation.ignoreNamed();
        this.worldFilter = worldFilter;
        this.nonNaturalChickenKey = new NamespacedKey(plugin, "non_natural_chicken");
    }

    void clear() {
        activationStates.clear();
    }

    void forget(int chickenId) {
        activationStates.remove(chickenId);
    }

    void track(Chicken chicken, CreatureSpawnEvent.SpawnReason spawnReason, boolean replaceActivationState) {
        int chickenId = chicken.getEntityId();
        if (replaceActivationState) {
            activationStates.put(chickenId, new ActivationState());
            return;
        }
        activationStates.putIfAbsent(chickenId, new ActivationState());
    }

    void markNonNaturalChicken(Chicken chicken, CreatureSpawnEvent.SpawnReason spawnReason) {
        if (isNaturalSpawnReason(spawnReason)) {
            return;
        }
        chicken.getPersistentDataContainer().set(nonNaturalChickenKey, PersistentDataType.BYTE, TRUE_BYTE);
    }

    boolean isWorldDisallowed(World world) {
        return !worldFilter.isWorldAllowed(world.getName());
    }

    boolean isPeacefulWorld(World world) {
        return world.getDifficulty() == Difficulty.PEACEFUL;
    }

    boolean isInvalidTarget(Chicken chicken, Player player) {
        if (player == null) return true;
        if (!player.isOnline() || player.isDead()) return true;
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) return true;
        if (isWorldDisallowed(chicken.getWorld())) return true;
        if (isPeacefulWorld(chicken.getWorld())) return true;
        return chicken.getWorld() != player.getWorld();
    }

    boolean isActivationBlocked(Chicken chicken, List<Entity> nearbyEntities) {
        if (!isPlayerNearby(chicken, nearbyEntities)) {
            return true;
        }
        return isActivationBlockedForEntity(chicken);
    }

    boolean isActivationBlockedForAggressor(Chicken chicken, Player aggressor) {
        if (aggressor == null || !aggressor.isOnline() || aggressor.isDead()) {
            return true;
        }
        if (aggressor.getWorld() != chicken.getWorld()) {
            return true;
        }
        if (HostilityDistances.distanceSq(chicken, aggressor) > ChickenHostilityConstants.PLAYER_PROXIMITY_RADIUS_SQ) {
            return true;
        }
        return isActivationBlockedForEntity(chicken);
    }

    private boolean isActivationBlockedForEntity(Chicken chicken) {
        if (ignoreNamed && chicken.customName() != null) {
            return true;
        }

        ActivationState activationState = activationStates.get(chicken.getEntityId());
        if (activationState == null) {
            activationState = new ActivationState();
            activationStates.put(chicken.getEntityId(), activationState);
        }

        if (onlyNaturalChickens && !isNaturalChicken(chicken)) {
            return true;
        }

        if (!activationState.chanceRolled) {
            activationState.chanceRolled = true;
            activationState.chancePassed = rollActivationChance();
        }

        return !activationState.chancePassed;
    }

    private boolean isPlayerNearby(Entity entity, List<Entity> nearbyEntities) {
        for (Entity nearbyEntity : nearbyEntities) {
            if (!(nearbyEntity instanceof Player player)) continue;
            if (!player.isOnline() || player.isDead()) continue;
            if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) continue;
            if (isPeacefulWorld(entity.getWorld())) continue;
            if (player.getWorld() != entity.getWorld()) continue;
            if (HostilityDistances.distanceSq(entity, player) <= ChickenHostilityConstants.PLAYER_PROXIMITY_RADIUS_SQ) return true;
        }
        return false;
    }

    private boolean isNaturalChicken(Chicken chicken) {
        CreatureSpawnEvent.SpawnReason entitySpawnReason = chicken.getEntitySpawnReason();
        return isNaturalSpawnReason(entitySpawnReason);
    }

    private boolean isNaturalSpawnReason(CreatureSpawnEvent.SpawnReason spawnReason) {
        if (spawnReason == null) {
            return true;
        }

        return switch (spawnReason) {
            case SPAWNER, SPAWNER_EGG, DISPENSE_EGG, EGG, BREEDING -> false;
            default -> true;
        };
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

    private static final class ActivationState {
        private boolean chanceRolled;
        private boolean chancePassed;

        private ActivationState() {
            this.chanceRolled = false;
            this.chancePassed = false;
        }
    }
}
