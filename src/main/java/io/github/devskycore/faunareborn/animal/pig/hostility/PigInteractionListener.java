package io.github.devskycore.faunareborn.animal.pig.hostility;
import io.github.devskycore.faunareborn.animal.common.settings.CommonGlobalHostilitySettings;
import io.github.devskycore.faunareborn.animal.common.settings.CommonSocialAlertSettings;

import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent;
import io.github.devskycore.faunareborn.animal.common.hostility.AbstractCookProvocationListenerSupport;
import io.github.devskycore.faunareborn.animal.common.hostility.HostilityEventSupport;
import io.github.devskycore.faunareborn.animal.pig.PigSettings;
import io.github.devskycore.faunareborn.combat.deathmessage.HostilityCause;
import io.github.devskycore.faunareborn.core.FaunaRebornPlugin;
import io.github.devskycore.faunareborn.system.scheduler.SchedulerAdapter;
import io.github.devskycore.faunareborn.system.scheduler.SchedulerAdapters;
import org.bukkit.Difficulty;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Pig;
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
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

final class PigInteractionListener extends AbstractCookProvocationListenerSupport implements Listener {

    private static final byte TRUE_BYTE = 1;
    private static final Material RAW_MEAT = Material.PORKCHOP;
    private static final Material COOKED_MEAT = Material.COOKED_PORKCHOP;

    private final PigSettings.RodProvocationSettings settings;
    private final CommonSocialAlertSettings socialAlertSettings;
    private final PigSettings.ResourceProvocationSettings resourceProvocationSettings;
    private final CommonGlobalHostilitySettings global;
    private final PigAggressionController aggressionController;
    private final PigTerritorialPickupService territorialPickupService;
    private final NamespacedKey nonNaturalPigKey;
    private final SchedulerAdapter scheduler;

    PigInteractionListener(
            FaunaRebornPlugin plugin,
            PigSettings.RodProvocationSettings settings,
            CommonSocialAlertSettings socialAlertSettings,
            CommonGlobalHostilitySettings global,
            PigAggressionController aggressionController,
            PigSettings.ResourceProvocationSettings resourceProvocationSettings
    ) {
        this.settings = settings;
        this.socialAlertSettings = socialAlertSettings;
        this.resourceProvocationSettings = resourceProvocationSettings;
        this.global = global;
        this.aggressionController = aggressionController;
        this.scheduler = SchedulerAdapters.create(plugin);
        this.territorialPickupService = new PigTerritorialPickupService(
                resourceProvocationSettings,
                socialAlertSettings,
                aggressionController,
                this::isNaturalPig,
                settings.requireLineOfSight()
        );
        this.nonNaturalPigKey = new NamespacedKey(plugin, "non_natural_pig");
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private void onPlayerProvokePigWithCarrotOnAStick(PlayerInteractEntityEvent event) {
        if (!settings.enabled() || event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        if (!(event.getRightClicked() instanceof Pig pig) || !pig.isAdult()) {
            return;
        }
        if (global.worldFilter().isWorldDisallowed(pig.getWorld().getName())) {
            return;
        }
        if (pig.getWorld().getDifficulty() == Difficulty.PEACEFUL) {
            return;
        }

        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) {
            return;
        }
        if (player.getInventory().getItemInMainHand().getType() != Material.CARROT_ON_A_STICK) {
            return;
        }
        scheduler.runForEntity(pig, () -> {
            if (!player.isOnline() || player.isDead() || !pig.isValid() || pig.isDead()) {
                return;
            }
            aggressionController.provokePigFromRodProvocation(pig, player, isNaturalPig(pig));
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private void onCreatureSpawn(CreatureSpawnEvent event) {
        if (!(event.getEntity() instanceof Pig pig)) {
            return;
        }
        if (HostilityEventSupport.isNonNaturalSpawn(event.getSpawnReason())) {
            pig.getPersistentDataContainer().set(nonNaturalPigKey, PersistentDataType.BYTE, TRUE_BYTE);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private void onPigDeath(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof Pig pig)) {
            return;
        }
        if (socialAlertSettings.enabled() && socialAlertSettings.onNearbyDeath() && !global.worldFilter().isWorldDisallowed(pig.getWorld().getName())) {
            Player killer = pig.getKiller();
            if (killer != null && killer.isOnline() && !killer.isDead()) {
                aggressionController.provokeNearbyPigsFromSocialAlert(
                        pig,
                        killer,
                        pig.getNearbyEntities(socialAlertSettings.radius(), socialAlertSettings.radius(), socialAlertSettings.radius()),
                        socialAlertSettings,
                        this::isNaturalPig,
                        HostilityCause.HERD_RETALIATION_NEARBY_KILL
                );
            }
        }
        aggressionController.removePig(pig.getEntityId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private void onPigRemoved(EntityRemoveFromWorldEvent event) {
        if (event.getEntity() instanceof Pig pig) {
            aggressionController.removePig(pig.getEntityId());
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
    private void onPigDamaged(EntityDamageByEntityEvent event) {
        if (event.getFinalDamage() <= 0.0D) {
            return;
        }
        if (!socialAlertSettings.enabled() || !socialAlertSettings.onDamage()) {
            return;
        }
        if (!(event.getEntity() instanceof Pig victimPig)) {
            return;
        }
        if (global.worldFilter().isWorldDisallowed(victimPig.getWorld().getName())
                || victimPig.getWorld().getDifficulty() == Difficulty.PEACEFUL) {
            return;
        }
        Player aggressor = HostilityEventSupport.resolveDamagingPlayer(event.getDamager());
        if (aggressor == null || aggressor.isDead() || !aggressor.isOnline()) {
            return;
        }
        aggressionController.provokePigFromDamage(victimPig, aggressor, isNaturalPig(victimPig));
        aggressionController.provokeNearbyPigsFromSocialAlert(
                victimPig,
                aggressor,
                victimPig.getNearbyEntities(socialAlertSettings.radius(), socialAlertSettings.radius(), socialAlertSettings.radius()),
                socialAlertSettings,
                this::isNaturalPig,
                HostilityCause.HERD_RETALIATION_DAMAGE
        );
    }

    private boolean isNaturalPig(Pig pig) {
        Byte marker = pig.getPersistentDataContainer().get(nonNaturalPigKey, PersistentDataType.BYTE);
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
        Pig firstRecruit = null;
        for (Entity entity : nearby) {
            if (!(entity instanceof Pig pig)) {
                continue;
            }
            if (!aggressionController.provokePigFromResources(
                    pig,
                    player,
                    isNaturalPig(pig),
                    resourceProvocationSettings.triggerCooldownTicks(),
                    resourceProvocationSettings.aggressionDurationTicks(),
                    cookingCause
            )) {
                continue;
            }
            if (firstRecruit == null) {
                firstRecruit = pig;
            }
        }
        if (firstRecruit != null) {
            aggressionController.provokeNearbyPigsFromSocialAlert(
                    firstRecruit,
                    player,
                    new ArrayList<>(nearby),
                    socialAlertSettings,
                    this::isNaturalPig,
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



