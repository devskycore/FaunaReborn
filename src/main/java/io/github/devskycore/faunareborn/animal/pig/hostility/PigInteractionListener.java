package io.github.devskycore.faunareborn.animal.pig.hostility;

import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent;
import io.github.devskycore.faunareborn.animal.pig.PigSettings;
import io.github.devskycore.faunareborn.core.FaunaRebornPlugin;
import io.github.devskycore.faunareborn.system.scheduler.SchedulerAdapter;
import io.github.devskycore.faunareborn.system.scheduler.SchedulerAdapters;
import org.bukkit.Difficulty;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Pig;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.entity.Tameable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.entity.AnimalTamer;

final class PigInteractionListener implements Listener {

    private static final byte TRUE_BYTE = 1;

    private final PigSettings.RodProvocationSettings settings;
    private final PigSettings.SocialAlertSettings socialAlertSettings;
    private final PigSettings.ResourceProvocationSettings resourceProvocationSettings;
    private final PigSettings.GlobalHostilitySettings global;
    private final PigAggressionController aggressionController;
    private final PigTerritorialPickupService territorialPickupService;
    private final NamespacedKey nonNaturalPigKey;
    private final SchedulerAdapter scheduler;

    PigInteractionListener(
            FaunaRebornPlugin plugin,
            PigSettings.RodProvocationSettings settings,
            PigSettings.SocialAlertSettings socialAlertSettings,
            PigSettings.GlobalHostilitySettings global,
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

        Player player = event.getPlayer();
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
        if (!isNaturalSpawn(event.getSpawnReason())) {
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
                        this::isNaturalPig
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
        aggressionController.removeTarget(event.getPlayer().getUniqueId());
        territorialPickupService.removePlayer(event.getPlayer().getUniqueId());
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
        if (global.worldFilter().isWorldDisallowed(victimPig.getWorld().getName())) {
            return;
        }
        Player aggressor = resolveDamagingPlayer(event.getDamager());
        if (aggressor == null || aggressor.isDead() || !aggressor.isOnline()) {
            return;
        }
        aggressionController.provokeNearbyPigsFromSocialAlert(
                victimPig,
                aggressor,
                victimPig.getNearbyEntities(socialAlertSettings.radius(), socialAlertSettings.radius(), socialAlertSettings.radius()),
                socialAlertSettings,
                this::isNaturalPig
        );
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private void onEntityPickupItem(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (!resourceProvocationSettings.enabled() || player.isDead() || !player.isOnline()) {
            return;
        }
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) {
            return;
        }
        if (global.worldFilter().isWorldDisallowed(player.getWorld().getName())) {
            return;
        }
        if (player.getWorld().getDifficulty() == Difficulty.PEACEFUL) {
            return;
        }
        Material material = event.getItem().getItemStack().getType();
        if (territorialPickupService.isNonTerritorialMaterial(material)) {
            return;
        }
        if (resourceProvocationSettings.maxItemAgeTicks() > 0
                && event.getItem().getTicksLived() > resourceProvocationSettings.maxItemAgeTicks()) {
            return;
        }
        int pickedUpAmount = event.getItem().getItemStack().getAmount() - event.getRemaining();
        if (pickedUpAmount <= 0) {
            return;
        }
        double radius = resourceProvocationSettings.detectionRadius();
        var nearby = event.getItem().getNearbyEntities(radius, radius, radius);
        if (!territorialPickupService.hasTerritorialWitness(player, nearby)) {
            return;
        }
        long currentTick = aggressionController.currentTick();
        territorialPickupService.recordPickup(player, material, pickedUpAmount, nearby, currentTick);
        if (currentTick % 100L == 0L) {
            territorialPickupService.cleanupCounters(currentTick);
        }
    }

    private boolean isNaturalPig(Pig pig) {
        Byte marker = pig.getPersistentDataContainer().get(nonNaturalPigKey, PersistentDataType.BYTE);
        return marker == null || marker != TRUE_BYTE;
    }

    private boolean isNaturalSpawn(CreatureSpawnEvent.SpawnReason spawnReason) {
        return spawnReason == CreatureSpawnEvent.SpawnReason.NATURAL
                || spawnReason == CreatureSpawnEvent.SpawnReason.DEFAULT
                || spawnReason == CreatureSpawnEvent.SpawnReason.BREEDING
                || spawnReason == CreatureSpawnEvent.SpawnReason.COMMAND
                || "CHUNK_GEN".equals(spawnReason.name());
    }

    private Player resolveDamagingPlayer(Entity damager) {
        if (damager instanceof Player player) {
            return player;
        }
        if (damager instanceof Projectile projectile) {
            ProjectileSource shooter = projectile.getShooter();
            if (shooter instanceof Player player) {
                return player;
            }
        }
        if (damager instanceof Tameable tameable) {
            AnimalTamer owner = tameable.getOwner();
            if (owner instanceof Player player) {
                return player;
            }
        }
        if (damager instanceof TNTPrimed tnt) {
            Entity source = tnt.getSource();
            if (source instanceof Player player) {
                return player;
            }
        }
        return null;
    }

    void clearState() {
        territorialPickupService.clear();
    }
}



