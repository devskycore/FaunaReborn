package io.github.devskycore.faunareborn.animal.cow.hostility;

import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent;
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
import org.bukkit.block.Block;
import org.bukkit.entity.AnimalTamer;
import org.bukkit.entity.Cow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.entity.Tameable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockCookEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.FurnaceExtractEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.projectiles.ProjectileSource;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

final class CowMilkInteractionListener implements Listener {

    private static final byte TRUE_BYTE = 1;
    private static final long COOK_VALIDATION_WINDOW_TICKS = 20L * 180L;
    private static final Material RAW_MEAT = Material.BEEF;
    private static final Material COOKED_MEAT = Material.COOKED_BEEF;

    private final CowSettings.MilkProvocationSettings settings;
    private final CowSettings.SocialAlertSettings socialAlertSettings;
    private final CowSettings.ResourceProvocationSettings resourceProvocationSettings;
    private final CowSettings.GlobalHostilitySettings global;
    private final CowMilkAggressionController aggressionController;
    private final CowTerritorialPickupService territorialPickupService;
    private final NamespacedKey nonNaturalCowKey;
    private final SchedulerAdapter scheduler;
    private final Map<UUID, PendingCookIntent> pendingCookIntentByPlayer = new HashMap<>();
    private final Map<String, CookedBatchReady> cookedBatchByCooker = new HashMap<>();

    CowMilkInteractionListener(
            FaunaRebornPlugin plugin,
            CowSettings.MilkProvocationSettings settings,
            CowSettings.SocialAlertSettings socialAlertSettings,
            CowSettings.GlobalHostilitySettings global,
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

        Player player = event.getPlayer();
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
        pendingCookIntentByPlayer.remove(playerId);
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
                this::isNaturalCow,
                HostilityCause.HERD_RETALIATION_DAMAGE
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
        if (material == COOKED_MEAT) {
            HostilityCause cookingCause = consumeCookValidation(player, event.getItem().getLocation(), COOKED_MEAT);
            if (cookingCause == null) {
                return;
            }
            triggerCookedMeatAggression(player, event.getItem().getLocation(), cookingCause);
            return;
        }

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
            cleanupCookValidationState(currentTick);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private void onBlockCook(BlockCookEvent event) {
        if (!resourceProvocationSettings.enabled()) {
            return;
        }
        Material cookerType = event.getBlock().getType();
        if (!isSupportedCooker(cookerType)) {
            return;
        }
        if (event.getSource().getType() != RAW_MEAT) {
            return;
        }
        if (global.worldFilter().isWorldDisallowed(event.getBlock().getWorld().getName())) {
            return;
        }
        if (event.getBlock().getWorld().getDifficulty() == Difficulty.PEACEFUL) {
            return;
        }
        long currentTick = aggressionController.currentTick();
        cookedBatchByCooker.put(blockKey(event.getBlock().getLocation()), new CookedBatchReady(COOKED_MEAT, currentTick + COOK_VALIDATION_WINDOW_TICKS));
        cleanupCookValidationState(currentTick);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private void onFurnaceExtract(FurnaceExtractEvent event) {
        if (!resourceProvocationSettings.enabled() || event.getItemType() != COOKED_MEAT) {
            return;
        }
        Block block = event.getBlock();
        if (!isSupportedCooker(block.getType())) {
            return;
        }
        if (global.worldFilter().isWorldDisallowed(block.getWorld().getName()) || block.getWorld().getDifficulty() == Difficulty.PEACEFUL) {
            return;
        }
        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) {
            return;
        }
        Location outputLocation = block.getLocation().add(0.5D, 0.5D, 0.5D);
        HostilityCause cookingCause = consumeCookValidation(player, outputLocation, COOKED_MEAT);
        if (cookingCause == null) {
            return;
        }
        triggerCookedMeatAggression(player, outputLocation, cookingCause);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (!resourceProvocationSettings.enabled()) {
            return;
        }
        InventoryType topType = event.getView().getTopInventory().getType();
        if (topType != InventoryType.FURNACE && topType != InventoryType.SMOKER) {
            return;
        }
        if (!(event.getView().getTopInventory().getHolder() instanceof org.bukkit.inventory.BlockInventoryHolder holder)) {
            return;
        }
        ItemStack cursor = event.getCursor();
        ItemStack current = event.getCurrentItem();
        boolean placedRawOnInputSlot = event.getRawSlot() == 0 && cursor != null && cursor.getType() == RAW_MEAT;
        boolean shiftMovedRaw = event.isShiftClick()
                && event.getClickedInventory() != null
                && event.getClickedInventory().getType() == InventoryType.PLAYER
                && current != null
                && current.getType() == RAW_MEAT;
        if (placedRawOnInputSlot || shiftMovedRaw) {
            rememberCookIntent(player, holder.getBlock().getLocation(), resolveCookingCause(holder.getBlock().getType()));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private void onPlayerInteract(PlayerInteractEvent event) {
        if (!resourceProvocationSettings.enabled()) {
            return;
        }
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) {
            return;
        }
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Material blockType = event.getClickedBlock().getType();
        if (blockType != Material.CAMPFIRE && blockType != Material.SOUL_CAMPFIRE) {
            return;
        }
        ItemStack hand = event.getItem();
        if (hand == null || hand.getType() != RAW_MEAT) {
            return;
        }
        rememberCookIntent(event.getPlayer(), event.getClickedBlock().getLocation(), HostilityCause.COOKING_CAMPFIRE);
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

    private boolean isSupportedCooker(Material material) {
        return material == Material.FURNACE
                || material == Material.SMOKER
                || material == Material.CAMPFIRE
                || material == Material.SOUL_CAMPFIRE;
    }

    private void rememberCookIntent(Player player, Location cookerLocation, HostilityCause cookingCause) {
        long currentTick = aggressionController.currentTick();
        pendingCookIntentByPlayer.put(
                player.getUniqueId(),
                new PendingCookIntent(blockKey(cookerLocation), currentTick + COOK_VALIDATION_WINDOW_TICKS, cookingCause)
        );
        cleanupCookValidationState(currentTick);
    }

    private HostilityCause consumeCookValidation(Player player, Location outputLocation, Material cookedType) {
        long currentTick = aggressionController.currentTick();
        PendingCookIntent intent = pendingCookIntentByPlayer.get(player.getUniqueId());
        if (intent == null || currentTick > intent.expiresAtTick()) {
            return null;
        }
        String cookerKey = resolveCookerKey(outputLocation);
        if (cookerKey == null) {
            return null;
        }
        CookedBatchReady ready = cookedBatchByCooker.get(cookerKey);
        if (ready == null || currentTick > ready.expiresAtTick() || ready.material() != cookedType) {
            return null;
        }
        if (!intent.cookerKey().equals(cookerKey)) {
            return null;
        }
        pendingCookIntentByPlayer.remove(player.getUniqueId());
        cookedBatchByCooker.remove(cookerKey);
        return intent.cookingCause();
    }

    private void triggerCookedMeatAggression(Player player, Location origin, HostilityCause cookingCause) {
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
                    new java.util.ArrayList<>(nearby),
                    socialAlertSettings,
                    this::isNaturalCow,
                    cookingCause
            );
        }
    }

    private HostilityCause resolveCookingCause(Material cookerType) {
        return switch (cookerType) {
            case FURNACE -> HostilityCause.COOKING_FURNACE;
            case SMOKER -> HostilityCause.COOKING_SMOKER;
            case CAMPFIRE, SOUL_CAMPFIRE -> HostilityCause.COOKING_CAMPFIRE;
            default -> HostilityCause.TERRITORIAL_PICKUP;
        };
    }

    private String blockKey(Location location) {
        return location.getWorld().getUID() + ":" + location.getBlockX() + ":" + location.getBlockY() + ":" + location.getBlockZ();
    }

    private String resolveCookerKey(Location location) {
        String direct = blockKey(location);
        if (cookedBatchByCooker.containsKey(direct)) {
            return direct;
        }
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    String candidate = blockKey(location.clone().add(dx, dy, dz));
                    if (cookedBatchByCooker.containsKey(candidate)) {
                        return candidate;
                    }
                }
            }
        }
        return null;
    }

    private void cleanupCookValidationState(long currentTick) {
        pendingCookIntentByPlayer.entrySet().removeIf(entry -> currentTick > entry.getValue().expiresAtTick());
        cookedBatchByCooker.entrySet().removeIf(entry -> currentTick > entry.getValue().expiresAtTick());
    }

    void clearState() {
        territorialPickupService.clear();
        pendingCookIntentByPlayer.clear();
        cookedBatchByCooker.clear();
    }

    private record PendingCookIntent(String cookerKey, long expiresAtTick, HostilityCause cookingCause) {}

    private record CookedBatchReady(Material material, long expiresAtTick) {}
}
