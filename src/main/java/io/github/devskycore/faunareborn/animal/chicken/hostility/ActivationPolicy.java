package io.github.devskycore.faunareborn.animal.chicken.hostility;

import io.github.devskycore.faunareborn.animal.chicken.config.ActivationConfig;
import io.github.devskycore.faunareborn.config.common.WorldFilter;
import io.github.devskycore.faunareborn.core.FaunaRebornPlugin;
import io.github.devskycore.faunareborn.targeting.TargetEligibilityService;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.bukkit.Difficulty;
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
    private static final byte SPAWN_ORIGIN_NATURAL = 1;
    private static final byte SPAWN_ORIGIN_EGG = 2;
    private static final byte SPAWN_ORIGIN_BREEDING = 3;
    private static final byte SPAWN_ORIGIN_NON_NATURAL = 4;

    private final double activationChance;
    private final boolean onlyNaturalChickens;
    private final boolean ignoreNamed;
    private final WorldFilter worldFilter;
    private final NamespacedKey spawnOriginKey;
    // Legacy marker kept for backward compatibility with existing saved entities.
    private final NamespacedKey nonNaturalChickenKey;
    private final TargetEligibilityService targetEligibilityService;
    private final Int2ObjectOpenHashMap<ActivationState> activationStates = new Int2ObjectOpenHashMap<>();

    ActivationPolicy(
            FaunaRebornPlugin plugin,
            ActivationConfig activation,
            WorldFilter worldFilter,
            TargetEligibilityService targetEligibilityService
    ) {
        this.activationChance = activation.chance();
        this.onlyNaturalChickens = activation.onlyNaturalChickens();
        this.ignoreNamed = activation.ignoreNamed();
        this.worldFilter = worldFilter;
        this.spawnOriginKey = new NamespacedKey(plugin, "chicken_spawn_origin");
        this.nonNaturalChickenKey = new NamespacedKey(plugin, "non_natural_chicken");
        this.targetEligibilityService = targetEligibilityService;
    }

    void clear() {
        activationStates.clear();
    }

    void forget(int chickenId) {
        activationStates.remove(chickenId);
    }

    void track(int chickenId, boolean replaceActivationState) {
        if (replaceActivationState) {
            activationStates.put(chickenId, new ActivationState());
            return;
        }
        activationStates.putIfAbsent(chickenId, new ActivationState());
    }

    void recordChickenSpawnOrigin(Chicken chicken, CreatureSpawnEvent.SpawnReason spawnReason) {
        persistSpawnOrigin(chicken, originFromSpawnReason(spawnReason));
    }

    void ensureSpawnOriginPersisted(Chicken chicken) {
        Byte persistedOrigin = chicken.getPersistentDataContainer().get(spawnOriginKey, PersistentDataType.BYTE);
        if (isValidPersistedOrigin(persistedOrigin)) {
            if (persistedOrigin != SPAWN_ORIGIN_NON_NATURAL) {
                chicken.getPersistentDataContainer().remove(nonNaturalChickenKey);
            } else {
                chicken.getPersistentDataContainer().set(nonNaturalChickenKey, PersistentDataType.BYTE, TRUE_BYTE);
            }
            return;
        }

        Byte legacyNonNatural = chicken.getPersistentDataContainer().get(nonNaturalChickenKey, PersistentDataType.BYTE);
        if (legacyNonNatural != null && legacyNonNatural == TRUE_BYTE) {
            persistSpawnOrigin(chicken, SPAWN_ORIGIN_NON_NATURAL);
            return;
        }

        persistSpawnOrigin(chicken, originFromSpawnReason(chicken.getEntitySpawnReason()));
    }

    boolean isWorldDisallowed(World world) {
        return worldFilter.isWorldDisallowed(world.getName());
    }

    WorldFilter worldFilter() {
        return worldFilter;
    }

    boolean isPeacefulWorld(World world) {
        return world.getDifficulty() == Difficulty.PEACEFUL;
    }

    boolean isInvalidTarget(Chicken chicken, Player player) {
        return player == null
                || isPeacefulWorld(chicken.getWorld())
                || targetEligibilityService.isIneligible(chicken, player, worldFilter, -1L);
    }

    boolean isActivationBlocked(Chicken chicken, List<Entity> nearbyEntities) {
        if (!isPlayerNearby(chicken, nearbyEntities)) {
            return true;
        }
        return isActivationBlockedForEntity(chicken);
    }

    boolean isActivationBlockedForAggressor(Chicken chicken, Player aggressor) {
        if (aggressor == null || targetEligibilityService.isIneligible(chicken, aggressor, worldFilter, -1L)) {
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
            if (targetEligibilityService.isIneligible(player, worldFilter, -1L)) continue;
            if (isPeacefulWorld(entity.getWorld())) continue;
            if (player.getWorld() != entity.getWorld()) continue;
            if (HostilityDistances.distanceSq(entity, player) <= ChickenHostilityConstants.PLAYER_PROXIMITY_RADIUS_SQ) return true;
        }
        return false;
    }

    private boolean isNaturalChicken(Chicken chicken) {
        return resolvePersistedSpawnOrigin(chicken) != SPAWN_ORIGIN_NON_NATURAL;
    }

    private byte resolvePersistedSpawnOrigin(Chicken chicken) {
        Byte persistedOrigin = chicken.getPersistentDataContainer().get(spawnOriginKey, PersistentDataType.BYTE);
        if (isValidPersistedOrigin(persistedOrigin)) {
            return persistedOrigin;
        }

        Byte legacyNonNatural = chicken.getPersistentDataContainer().get(nonNaturalChickenKey, PersistentDataType.BYTE);
        if (legacyNonNatural != null && legacyNonNatural == TRUE_BYTE) {
            return SPAWN_ORIGIN_NON_NATURAL;
        }

        return originFromSpawnReason(chicken.getEntitySpawnReason());
    }

    private byte originFromSpawnReason(CreatureSpawnEvent.SpawnReason spawnReason) {
        if (spawnReason == null) {
            return SPAWN_ORIGIN_NATURAL;
        }
        if (spawnReason == CreatureSpawnEvent.SpawnReason.SPAWNER_EGG) {
            return SPAWN_ORIGIN_NON_NATURAL;
        }
        if (spawnReason.name().contains("SPAWNER")) {
            return SPAWN_ORIGIN_NON_NATURAL;
        }
        return switch (spawnReason) {
            case EGG, DISPENSE_EGG -> SPAWN_ORIGIN_EGG;
            case BREEDING, OCELOT_BABY -> SPAWN_ORIGIN_BREEDING;
            default -> SPAWN_ORIGIN_NATURAL;
        };
    }

    private void persistSpawnOrigin(Chicken chicken, byte origin) {
        chicken.getPersistentDataContainer().set(spawnOriginKey, PersistentDataType.BYTE, origin);
        if (origin == SPAWN_ORIGIN_NON_NATURAL) {
            chicken.getPersistentDataContainer().set(nonNaturalChickenKey, PersistentDataType.BYTE, TRUE_BYTE);
            return;
        }
        chicken.getPersistentDataContainer().remove(nonNaturalChickenKey);
    }

    private boolean isValidPersistedOrigin(Byte persistedOrigin) {
        return persistedOrigin != null
                && persistedOrigin >= SPAWN_ORIGIN_NATURAL
                && persistedOrigin <= SPAWN_ORIGIN_NON_NATURAL;
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
