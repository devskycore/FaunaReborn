package io.github.devskycore.faunareborn.animal.pig.hostility;

import io.github.devskycore.faunareborn.animal.pig.PigSettings;
import io.github.devskycore.faunareborn.combat.deathmessage.HostilityCause;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Pig;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import io.github.devskycore.faunareborn.system.environment.WorldEnvironmentContextCache;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

final class PigTerritorialPickupService {

    private final PigSettings.ResourceProvocationSettings settings;
    private final PigSettings.SocialAlertSettings socialAlertSettings;
    private final PigAggressionController aggressionController;
    private final NaturalPigResolver naturalPigResolver;
    private final boolean requireLineOfSight;
    private final WorldEnvironmentContextCache environmentCache;
    private final Map<UUID, TerritorialPickupCounter> counters = new HashMap<>();

    PigTerritorialPickupService(
            PigSettings.ResourceProvocationSettings settings,
            PigSettings.SocialAlertSettings socialAlertSettings,
            PigAggressionController aggressionController,
            NaturalPigResolver naturalPigResolver,
            boolean requireLineOfSight,
            WorldEnvironmentContextCache environmentCache
    ) {
        this.settings = settings;
        this.socialAlertSettings = socialAlertSettings;
        this.aggressionController = aggressionController;
        this.naturalPigResolver = naturalPigResolver;
        this.requireLineOfSight = requireLineOfSight;
        this.environmentCache = environmentCache;
    }

    boolean isNonTerritorialMaterial(Material material) {
        return material != Material.CARROT && material != Material.APPLE && material != Material.PORKCHOP;
    }

    void recordPickup(Player player, Material material, int amount, List<Entity> nearbyEntities, long currentTick) {
        if (!settings.enabled() || amount <= 0 || isNonTerritorialMaterial(material) || player == null) {
            return;
        }
        if (!player.isOnline() || player.isDead()) {
            return;
        }

        int threshold = resolveThreshold(material, player.getWorld());
        TerritorialPickupCounter counter = counters.computeIfAbsent(player.getUniqueId(), ignored -> new TerritorialPickupCounter());
        int updatedAmount = counter.add(material, amount, currentTick, settings.timeWindowTicks());
        if (updatedAmount < threshold) {
            return;
        }
        counter.reset(material);
        triggerAggression(player, nearbyEntities);
    }

    boolean hasTerritorialWitness(Player player, List<Entity> nearbyEntities) {
        if (player == null || nearbyEntities == null || nearbyEntities.isEmpty()) {
            return false;
        }
        for (Entity entity : nearbyEntities) {
            if (entity instanceof Pig Pig && canPerceiveTerritorialPickup(Pig, player)) {
                return true;
            }
        }
        return false;
    }

    void cleanupCounters(long currentTick) {
        if (counters.isEmpty()) {
            return;
        }
        counters.entrySet().removeIf(entry -> entry.getValue().isExpired(currentTick));
    }

    void clear() {
        counters.clear();
    }

    void removePlayer(UUID playerId) {
        counters.remove(playerId);
    }

    private void triggerAggression(Player player, List<Entity> nearbyEntities) {
        if (nearbyEntities == null || nearbyEntities.isEmpty()) {
            return;
        }
        int recruited = 0;
        Pig firstRecruit = null;
        for (Entity entity : nearbyEntities) {
            if (!(entity instanceof Pig Pig) || !canPerceiveTerritorialPickup(Pig, player)) {
                continue;
            }
            if (!aggressionController.provokePigFromResources(
                    Pig,
                    player,
                    naturalPigResolver.isNaturalPig(Pig),
                    settings.triggerCooldownTicks(),
                    settings.aggressionDurationTicks()
            )) {
                continue;
            }
            if (firstRecruit == null) {
                firstRecruit = Pig;
            }
            recruited++;
            if (recruited >= settings.maxResponders()) {
                break;
            }
        }
        if (recruited > 0 && settings.socialPropagationEnabled()) {
            aggressionController.provokeNearbyPigsFromSocialAlert(
                    firstRecruit,
                    player,
                    firstRecruit.getNearbyEntities(socialAlertSettings.radius(), socialAlertSettings.radius(), socialAlertSettings.radius()),
                    socialAlertSettings,
                    naturalPigResolver::isNaturalPig,
                    HostilityCause.TERRITORIAL_PICKUP
            );
        }
    }

    private boolean canPerceiveTerritorialPickup(Pig Pig, Player player) {
        if (Pig == null || player == null) {
            return false;
        }
        if (!Pig.isAdult() || !Pig.isValid() || Pig.isDead()) {
            return false;
        }
        if (Pig.getWorld() != player.getWorld()) {
            return false;
        }
        if (Math.abs(Pig.getY() - player.getY()) > 5.0D) {
            return false;
        }
        if (settings.detectionRadiusSq() < distanceSq(Pig, player)) {
            return false;
        }
        return !requireLineOfSight || Pig.hasLineOfSight(player);
    }

    private int resolveThreshold(Material material, World world) {
        int threshold = switch (material) {
            case CARROT -> settings.carrotThreshold();
            case APPLE -> settings.appleThreshold();
            case PORKCHOP -> settings.rawPorkchopThreshold();
            default -> Integer.MAX_VALUE;
        };
        if (!settings.nightModifierEnabled() || world == null || !isNight(world)) {
            return threshold;
        }
        return Math.max(1, (int) Math.ceil(threshold * settings.nightThresholdMultiplier()));
    }

    private static boolean isNight(World world) {
        long time = world.getTime();
        return time >= 13000L && time <= 23000L;
    }

    private static double distanceSq(Pig Pig, Player player) {
        double dx = Pig.getX() - player.getX();
        double dy = Pig.getY() - player.getY();
        double dz = Pig.getZ() - player.getZ();
        return dx * dx + dy * dy + dz * dz;
    }

    interface NaturalPigResolver {
        boolean isNaturalPig(Pig Pig);
    }

    private static final class TerritorialPickupCounter {
        private int carrotAmount;
        private long carrotExpiresTick;
        private int appleAmount;
        private long appleExpiresTick;
        private int rawPorkchopAmount;
        private long rawPorkchopExpiresTick;

        private int add(Material material, int amount, long currentTick, int windowTicks) {
            return switch (material) {
                case CARROT -> {
                    if (currentTick > carrotExpiresTick) {
                        carrotAmount = 0;
                    }
                    carrotAmount = boundedAdd(carrotAmount, amount);
                    carrotExpiresTick = currentTick + windowTicks;
                    yield carrotAmount;
                }
                case APPLE -> {
                    if (currentTick > appleExpiresTick) {
                        appleAmount = 0;
                    }
                    appleAmount = boundedAdd(appleAmount, amount);
                    appleExpiresTick = currentTick + windowTicks;
                    yield appleAmount;
                }
                case PORKCHOP -> {
                    if (currentTick > rawPorkchopExpiresTick) {
                        rawPorkchopAmount = 0;
                    }
                    rawPorkchopAmount = boundedAdd(rawPorkchopAmount, amount);
                    rawPorkchopExpiresTick = currentTick + windowTicks;
                    yield rawPorkchopAmount;
                }
                default -> 0;
            };
        }

        private void reset(Material material) {
            switch (material) {
                case CARROT -> {
                    carrotAmount = 0;
                    carrotExpiresTick = 0L;
                }
                case APPLE -> {
                    appleAmount = 0;
                    appleExpiresTick = 0L;
                }
                case PORKCHOP -> {
                    rawPorkchopAmount = 0;
                    rawPorkchopExpiresTick = 0L;
                }
                default -> {
                }
            }
        }

        private boolean isExpired(long currentTick) {
            return currentTick > carrotExpiresTick
                    && currentTick > appleExpiresTick
                    && currentTick > rawPorkchopExpiresTick;
        }

        private static int boundedAdd(int current, int amount) {
            long result = (long) current + amount;
            return result > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) result;
        }
    }
}




