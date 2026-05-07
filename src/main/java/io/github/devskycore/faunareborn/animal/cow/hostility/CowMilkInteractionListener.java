package io.github.devskycore.faunareborn.animal.cow.hostility;

import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent;
import io.github.devskycore.faunareborn.animal.cow.CowSettings;
import io.github.devskycore.faunareborn.core.FaunaRebornPlugin;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Cow;
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
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.entity.AnimalTamer;

final class CowMilkInteractionListener implements Listener {

    private static final byte TRUE_BYTE = 1;

    private final FaunaRebornPlugin plugin;
    private final CowSettings.MilkProvocationSettings settings;
    private final CowSettings.SocialAlertSettings socialAlertSettings;
    private final CowSettings.GlobalHostilitySettings global;
    private final CowMilkAggressionController aggressionController;
    private final NamespacedKey nonNaturalCowKey;

    CowMilkInteractionListener(
            FaunaRebornPlugin plugin,
            CowSettings.MilkProvocationSettings settings,
            CowSettings.SocialAlertSettings socialAlertSettings,
            CowSettings.GlobalHostilitySettings global,
            CowMilkAggressionController aggressionController
    ) {
        this.plugin = plugin;
        this.settings = settings;
        this.socialAlertSettings = socialAlertSettings;
        this.global = global;
        this.aggressionController = aggressionController;
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

        Player player = event.getPlayer();
        PlayerInventory inventory = player.getInventory();
        if (inventory.getItemInMainHand().getType() != Material.BUCKET) {
            return;
        }

        int milkBefore = countMaterial(inventory, Material.MILK_BUCKET);
        int bucketBefore = countMaterial(inventory, Material.BUCKET);
        plugin.getServer().getScheduler().runTask(plugin, () -> {
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
        if (!isNaturalSpawn(event.getSpawnReason())) {
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
                        this::isNaturalCow
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
        aggressionController.removeTarget(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private void onCowDamaged(EntityDamageByEntityEvent event) {
        if (!socialAlertSettings.enabled() || !socialAlertSettings.onDamage()) {
            return;
        }
        if (!(event.getEntity() instanceof Cow victimCow)) {
            return;
        }
        if (global.worldFilter().isWorldDisallowed(victimCow.getWorld().getName())) {
            return;
        }
        Player aggressor = resolveDamagingPlayer(event.getDamager());
        if (aggressor == null || aggressor.isDead() || !aggressor.isOnline()) {
            return;
        }
        aggressionController.provokeNearbyCowsFromSocialAlert(
                victimCow,
                aggressor,
                victimCow.getNearbyEntities(socialAlertSettings.radius(), socialAlertSettings.radius(), socialAlertSettings.radius()),
                socialAlertSettings,
                this::isNaturalCow
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
}
