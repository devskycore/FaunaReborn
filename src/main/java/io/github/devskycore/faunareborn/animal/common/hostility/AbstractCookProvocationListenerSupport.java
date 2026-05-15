package io.github.devskycore.faunareborn.animal.common.hostility;

import io.github.devskycore.faunareborn.combat.deathmessage.HostilityCause;
import org.bukkit.Difficulty;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockCookEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.FurnaceExtractEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;

public abstract class AbstractCookProvocationListenerSupport {

    private static final long COOK_VALIDATION_WINDOW_TICKS = 20L * 180L;

    private final Map<UUID, PendingCookIntent> pendingCookIntentByPlayer = new ConcurrentHashMap<>();
    private final Map<String, CookedBatchReady> cookedBatchByCooker = new ConcurrentHashMap<>();

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    protected final void onEntityPickupItem(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (!isResourceProvocationEnabled() || player.isDead() || !player.isOnline()) {
            return;
        }
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) {
            return;
        }
        if (isWorldDisallowed(player.getWorld().getName())) {
            return;
        }
        if (player.getWorld().getDifficulty() == Difficulty.PEACEFUL) {
            return;
        }

        Material material = event.getItem().getItemStack().getType();
        if (material == cookedMeat()) {
            HostilityCause cookingCause = consumeCookValidation(player, event.getItem().getLocation(), cookedMeat());
            if (cookingCause == null) {
                return;
            }
            triggerCookedMeatAggression(player, event.getItem().getLocation(), cookingCause);
            return;
        }

        if (isNonTerritorialMaterial(material)) {
            return;
        }
        if (maxItemAgeTicks() > 0 && event.getItem().getTicksLived() > maxItemAgeTicks()) {
            return;
        }
        int pickedUpAmount = event.getItem().getItemStack().getAmount() - event.getRemaining();
        if (pickedUpAmount <= 0) {
            return;
        }
        double radius = detectionRadius();
        var nearby = event.getItem().getNearbyEntities(radius, radius, radius);
        if (!hasTerritorialWitness(player, nearby)) {
            return;
        }
        long currentTick = currentTick();
        recordPickup(player, material, pickedUpAmount, nearby, currentTick);
        if (currentTick % 100L == 0L) {
            cleanupCounters(currentTick);
            cleanupCookValidationState(currentTick);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    protected final void onBlockCook(BlockCookEvent event) {
        if (!isResourceProvocationEnabled()) {
            return;
        }
        Material cookerType = event.getBlock().getType();
        if (!isSupportedCooker(cookerType)) {
            return;
        }
        if (event.getSource().getType() != rawMeat()) {
            return;
        }
        if (isWorldDisallowed(event.getBlock().getWorld().getName())) {
            return;
        }
        if (event.getBlock().getWorld().getDifficulty() == Difficulty.PEACEFUL) {
            return;
        }
        long now = currentTick();
        cookedBatchByCooker.put(
                blockKey(event.getBlock().getLocation()),
                new CookedBatchReady(cookedMeat(), now + COOK_VALIDATION_WINDOW_TICKS, resolveCookingCause(cookerType))
        );
        cleanupCookValidationState(now);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    protected final void onFurnaceExtract(FurnaceExtractEvent event) {
        if (!isResourceProvocationEnabled() || event.getItemType() != cookedMeat()) {
            return;
        }
        Block block = event.getBlock();
        if (!isSupportedCooker(block.getType())) {
            return;
        }
        if (isWorldDisallowed(block.getWorld().getName()) || block.getWorld().getDifficulty() == Difficulty.PEACEFUL) {
            return;
        }
        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) {
            return;
        }
        Location outputLocation = block.getLocation().add(0.5D, 0.5D, 0.5D);
        HostilityCause cause = consumeCookValidation(player, outputLocation, cookedMeat(), resolveCookingCause(block.getType()));
        if (cause == null) {
            return;
        }
        triggerCookedMeatAggression(player, outputLocation, cause);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    protected final void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (!isResourceProvocationEnabled()) {
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
        boolean placedRawOnInputSlot = event.getRawSlot() == 0 && cursor != null && cursor.getType() == rawMeat();
        boolean shiftMovedRaw = event.isShiftClick()
                && event.getClickedInventory() != null
                && event.getClickedInventory().getType() == InventoryType.PLAYER
                && current != null
                && current.getType() == rawMeat();
        if (placedRawOnInputSlot || shiftMovedRaw) {
            rememberCookIntent(player, holder.getBlock().getLocation(), resolveCookingCause(holder.getBlock().getType()));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    protected final void onPlayerInteract(PlayerInteractEvent event) {
        if (!isResourceProvocationEnabled()) {
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
        if (hand == null || hand.getType() != rawMeat()) {
            return;
        }
        rememberCookIntent(event.getPlayer(), event.getClickedBlock().getLocation(), HostilityCause.COOKING_CAMPFIRE);
    }

    protected final void clearCookSupportState() {
        pendingCookIntentByPlayer.clear();
        cookedBatchByCooker.clear();
    }

    protected final void removeCookIntent(UUID playerId) {
        pendingCookIntentByPlayer.remove(playerId);
    }

    protected final void cleanupCookValidationState(long currentTick) {
        pendingCookIntentByPlayer.entrySet().removeIf(entry -> currentTick > entry.getValue().expiresAtTick());
        cookedBatchByCooker.entrySet().removeIf(entry -> currentTick > entry.getValue().expiresAtTick());
    }

    private void rememberCookIntent(Player player, Location cookerLocation, HostilityCause cookingCause) {
        long now = currentTick();
        pendingCookIntentByPlayer.put(
                player.getUniqueId(),
                new PendingCookIntent(blockKey(cookerLocation), now + COOK_VALIDATION_WINDOW_TICKS, cookingCause)
        );
        cleanupCookValidationState(now);
    }

    private HostilityCause consumeCookValidation(Player player, Location outputLocation, Material cookedType) {
        return consumeCookValidation(player, outputLocation, cookedType, null);
    }

    private HostilityCause consumeCookValidation(Player player, Location outputLocation, Material cookedType, HostilityCause fallbackCause) {
        long now = currentTick();
        PendingCookIntent intent = pendingCookIntentByPlayer.get(player.getUniqueId());
        String cookerKey = resolveCookerKey(outputLocation);
        if (cookerKey == null) {
            return null;
        }
        CookedBatchReady ready = cookedBatchByCooker.get(cookerKey);
        if (ready == null || now > ready.expiresAtTick() || ready.material() != cookedType) {
            return null;
        }
        if (intent != null && now <= intent.expiresAtTick() && intent.cookerKey().equals(cookerKey)) {
            pendingCookIntentByPlayer.remove(player.getUniqueId());
            cookedBatchByCooker.remove(cookerKey);
            return intent.cookingCause();
        }
        cookedBatchByCooker.remove(cookerKey);
        return ready.cookingCause() == null ? fallbackCause : ready.cookingCause();
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

    protected abstract boolean isResourceProvocationEnabled();

    protected abstract boolean isWorldDisallowed(String worldName);

    protected abstract boolean isNonTerritorialMaterial(Material material);

    protected abstract int maxItemAgeTicks();

    protected abstract double detectionRadius();

    protected abstract boolean hasTerritorialWitness(Player player, List<Entity> nearby);

    protected abstract void recordPickup(Player player, Material material, int pickedUpAmount, List<Entity> nearby, long currentTick);

    protected abstract void cleanupCounters(long currentTick);

    protected abstract long currentTick();

    protected abstract Material rawMeat();

    protected abstract Material cookedMeat();

    protected abstract boolean isSupportedCooker(Material material);

    protected HostilityCause resolveCookingCause(Material cookerType) {
        return HostilityEventSupport.resolveCookingCause(cookerType);
    }

    protected abstract void triggerCookedMeatAggression(Player player, Location origin, HostilityCause cookingCause);

    private record PendingCookIntent(String cookerKey, long expiresAtTick, HostilityCause cookingCause) {}

    private record CookedBatchReady(Material material, long expiresAtTick, HostilityCause cookingCause) {}
}
