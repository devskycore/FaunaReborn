package io.github.devskycore.faunareborn.animal.cow.hostility;
import io.github.devskycore.faunareborn.animal.common.settings.CommonGlobalHostilitySettings;
import io.github.devskycore.faunareborn.animal.common.settings.CommonSocialAlertSettings;

import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent;
import io.github.devskycore.faunareborn.animal.common.hostility.AbstractCookProvocationListenerSupport;
import io.github.devskycore.faunareborn.animal.common.hostility.HostilityEventSupport;
import io.github.devskycore.faunareborn.animal.cow.CowSettings;
import io.github.devskycore.faunareborn.combat.deathmessage.HostilityCause;
import io.github.devskycore.faunareborn.core.FaunaRebornPlugin;
import io.github.devskycore.faunareborn.system.scheduler.SchedulerAdapter;
import io.github.devskycore.faunareborn.system.scheduler.SchedulerAdapters;
import org.bukkit.Difficulty;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Cow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

final class CowMilkInteractionListener extends AbstractCookProvocationListenerSupport implements Listener {

    private static final byte TRUE_BYTE = 1;
    private static final Material RAW_MEAT = Material.BEEF;
    private static final Material COOKED_MEAT = Material.COOKED_BEEF;

    private final CowSettings.MilkProvocationSettings settings;
    private final CommonSocialAlertSettings socialAlertSettings;
    private final CowSettings.ResourceProvocationSettings resourceProvocationSettings;
    private final CommonGlobalHostilitySettings global;
    private final CowMilkAggressionController aggressionController;
    private final CowTerritorialPickupService territorialPickupService;
    private final NamespacedKey nonNaturalCowKey;
    private final SchedulerAdapter scheduler;

    CowMilkInteractionListener(
            FaunaRebornPlugin plugin,
            CowSettings.MilkProvocationSettings settings,
            CommonSocialAlertSettings socialAlertSettings,
            CommonGlobalHostilitySettings global,
            CowMilkAggressionController aggressionController,
            CowSettings.ResourceProvocationSettings resourceProvocationSettings
    ) {
        this.settings = settings;
        this.socialAlertSettings = socialAlertSettings;
        this.resourceProvocationSettings = resourceProvocationSettings;
        this.global = global;
        this.aggressionController = aggressionController;
        this.scheduler = SchedulerAdapters.create(plugin);
        this.territorialPickupService = new CowTerritorialPickupService(
                resourceProvocationSettings,
                socialAlertSettings,
                aggressionController,
                this::isNaturalCow,
                settings.requireLineOfSight()
        );
        this.nonNaturalCowKey = new NamespacedKey(plugin, "non_natural_cow");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private void onPlayerMilkCow(PlayerInteractEntityEvent event) {
        if (!settings.enabled() || event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        if (!(event.getRightClicked() instanceof Cow cow) || !cow.isAdult()) {
            return;
        }
        if (global.worldFilter().isWorldDisallowed(cow.getWorld().getName())) {
            return;
        }
        if (cow.getWorld().getDifficulty() == Difficulty.PEACEFUL) {
            return;
        }

        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) {
            return;
        }
        PlayerInventory inventory = player.getInventory();
        if (inventory.getItemInMainHand().getType() != Material.BUCKET) {
            return;
        }

        int milkBefore = countMaterial(inventory, Material.MILK_BUCKET);
        int bucketBefore = countMaterial(inventory, Material.BUCKET);
        scheduler.runForEntity(cow, () -> {
            if (!player.isOnline() || player.isDead() || !cow.isValid() || cow.isDead()) {
                return;
            }
            if (!wasMilkingSuccessful(player, milkBefore, bucketBefore)) {
                return;
            }
            aggressionController.provokeCowFromMilking(cow, player, isNaturalCow(cow));
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private void onCreatureSpawn(CreatureSpawnEvent event) {
        if (!(event.getEntity() instanceof Cow cow)) {
            return;
        }
        if (HostilityEventSupport.isNonNaturalSpawn(event.getSpawnReason())) {
            cow.getPersistentDataContainer().set(nonNaturalCowKey, PersistentDataType.BYTE, TRUE_BYTE);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private void onCowDeath(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof Cow cow)) {
            return;
        }
        if (socialAlertSettings.enabled() && socialAlertSettings.onNearbyDeath() && !global.worldFilter().isWorldDisallowed(cow.getWorld().getName())) {
            Player killer = cow.getKiller();
            if (killer != null && killer.isOnline() && !killer.isDead()) {
                aggressionController.provokeNearbyCowsFromSocialAlert(
                        cow,
                        killer,
                        cow.getNearbyEntities(socialAlertSettings.radius(), socialAlertSettings.radius(), socialAlertSettings.radius()),
                        socialAlertSettings,
                        this::isNaturalCow,
                        HostilityCause.HERD_RETALIATION_NEARBY_KILL
                );
            }
        }
        aggressionController.removeCow(cow.getEntityId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private void onCowRemoved(EntityRemoveFromWorldEvent event) {
        if (event.getEntity() instanceof Cow cow) {
            aggressionController.removeCow(cow.getEntityId());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private void onPlayerQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        aggressionController.removeTarget(playerId);
        territorialPickupService.removePlayer(playerId);
        removeCookIntent(playerId);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private void onCowDamaged(EntityDamageByEntityEvent event) {
        if (event.getFinalDamage() <= 0.0D) {
            return;
        }
        if (!socialAlertSettings.enabled() || !socialAlertSettings.onDamage()) {
            return;
        }
        if (!(event.getEntity() instanceof Cow victimCow)) {
            return;
        }
        if (global.worldFilter().isWorldDisallowed(victimCow.getWorld().getName())
                || victimCow.getWorld().getDifficulty() == Difficulty.PEACEFUL) {
            return;
        }
        Player aggressor = HostilityEventSupport.resolveDamagingPlayer(event.getDamager());
        if (aggressor == null || aggressor.isDead() || !aggressor.isOnline()) {
            return;
        }
        aggressionController.provokeCowFromDamage(victimCow, aggressor, isNaturalCow(victimCow));
        aggressionController.provokeNearbyCowsFromSocialAlert(
                victimCow,
                aggressor,
                victimCow.getNearbyEntities(socialAlertSettings.radius(), socialAlertSettings.radius(), socialAlertSettings.radius()),
                socialAlertSettings,
                this::isNaturalCow,
                HostilityCause.HERD_RETALIATION_DAMAGE
        );
    }

    private boolean wasMilkingSuccessful(Player player, int milkBefore, int bucketBefore) {
        PlayerInventory inventory = player.getInventory();
        int milkAfter = countMaterial(inventory, Material.MILK_BUCKET);
        int bucketAfter = countMaterial(inventory, Material.BUCKET);
        Material handAfter = inventory.getItemInMainHand().getType();

        if (handAfter == Material.MILK_BUCKET) {
            return true;
        }
        return milkAfter > milkBefore && bucketAfter < bucketBefore;
    }

    private int countMaterial(PlayerInventory inventory, Material material) {
        int total = 0;
        for (var item : inventory.getContents()) {
            if (item == null || item.getType() != material) {
                continue;
            }
            total += item.getAmount();
        }
        return total;
    }

    private boolean isNaturalCow(Cow cow) {
        Byte marker = cow.getPersistentDataContainer().get(nonNaturalCowKey, PersistentDataType.BYTE);
        return marker == null || marker != TRUE_BYTE;
    }

    @Override
    protected boolean isUnsupportedCooker(Material material) {
        return material != Material.FURNACE
                && material != Material.SMOKER
                && material != Material.CAMPFIRE
                && material != Material.SOUL_CAMPFIRE;
    }

    @Override
    protected void triggerCookedMeatAggression(Player player, Location origin, HostilityCause cookingCause) {
        double radius = resourceProvocationSettings.detectionRadius();
        var nearby = origin.getWorld().getNearbyEntities(origin, radius, radius, radius);
        Cow firstRecruit = null;
        for (Entity entity : nearby) {
            if (!(entity instanceof Cow cow)) {
                continue;
            }
            if (!aggressionController.provokeCowFromResources(
                    cow,
                    player,
                    isNaturalCow(cow),
                    resourceProvocationSettings.triggerCooldownTicks(),
                    resourceProvocationSettings.aggressionDurationTicks(),
                    cookingCause
            )) {
                continue;
            }
            if (firstRecruit == null) {
                firstRecruit = cow;
            }
        }
        if (firstRecruit != null) {
            aggressionController.provokeNearbyCowsFromSocialAlert(
                    firstRecruit,
                    player,
                    new ArrayList<>(nearby),
                    socialAlertSettings,
                    this::isNaturalCow,
                    cookingCause
            );
        }
    }

    @Override
    protected HostilityCause resolveCookingCause(Material cookerType) {
        return switch (cookerType) {
            case FURNACE -> HostilityCause.COOKING_FURNACE;
            case SMOKER -> HostilityCause.COOKING_SMOKER;
            case CAMPFIRE, SOUL_CAMPFIRE -> HostilityCause.COOKING_CAMPFIRE;
            default -> HostilityCause.TERRITORIAL_PICKUP;
        };
    }

    @Override
    protected boolean isResourceProvocationDisabled() {
        return !resourceProvocationSettings.enabled();
    }

    @Override
    protected boolean isWorldDisallowed(String worldName) {
        return global.worldFilter().isWorldDisallowed(worldName);
    }

    @Override
    protected boolean isNonTerritorialMaterial(Material material) {
        return territorialPickupService.isNonTerritorialMaterial(material);
    }

    @Override
    protected int maxItemAgeTicks() {
        return resourceProvocationSettings.maxItemAgeTicks();
    }

    @Override
    protected double detectionRadius() {
        return resourceProvocationSettings.detectionRadius();
    }

    @Override
    protected boolean hasTerritorialWitness(Player player, List<Entity> nearby) {
        return territorialPickupService.hasTerritorialWitness(player, nearby);
    }

    @Override
    protected void recordPickup(Player player, Material material, int pickedUpAmount, List<Entity> nearby, long currentTick) {
        territorialPickupService.recordPickup(player, material, pickedUpAmount, nearby, currentTick);
    }

    @Override
    protected void cleanupCounters(long currentTick) {
        territorialPickupService.cleanupCounters(currentTick);
    }

    @Override
    protected long currentTick() {
        return aggressionController.currentTick();
    }

    @Override
    protected Material rawMeat() {
        return RAW_MEAT;
    }

    @Override
    protected Material cookedMeat() {
        return COOKED_MEAT;
    }

    void clearState() {
        territorialPickupService.clear();
        clearCookSupportState();
    }
}



