package io.github.devskycore.faunareborn.gui;

import io.github.devskycore.faunareborn.command.FaunaReloadService;
import io.github.devskycore.faunareborn.core.FaunaRebornPlugin;
import io.github.devskycore.faunareborn.module.ModuleManager;
import io.github.devskycore.faunareborn.system.scheduler.SchedulerAdapter;
import io.github.devskycore.faunareborn.system.scheduler.SchedulerAdapters;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public final class FaunaMainGui implements Listener {

    private static final Component TITLE = Component.text("FaunaReborn", NamedTextColor.BLACK)
            .decorate(TextDecoration.BOLD)
            .append(Component.text(" | ", NamedTextColor.GRAY).decoration(TextDecoration.BOLD, false))
            .append(Component.text("Control Panel", NamedTextColor.DARK_GRAY).decoration(TextDecoration.BOLD, false))
            .decoration(TextDecoration.ITALIC, false);
    private static final int SIZE = 54;
    private static final int RELOAD_SLOT = 49;
    private static final int CLOSE_SLOT = 53;
    private static final int[] TOGGLE_SLOTS = {20, 22, 24, 33};

    private final FaunaRebornPlugin plugin;
    private final PluginGuiConfigService configService;
    private final FaunaReloadService reloadService;
    private final SchedulerAdapter scheduler;

    public FaunaMainGui(
            FaunaRebornPlugin plugin,
            PluginGuiConfigService configService,
            FaunaReloadService reloadService
    ) {
        this.plugin = plugin;
        this.configService = configService;
        this.reloadService = reloadService;
        this.scheduler = SchedulerAdapters.create(plugin);
    }

    public void open(Player player) {
        player.openInventory(buildInventory());
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, SoundCategory.MASTER, 0.5f, 1.25f);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!TITLE.equals(event.getView().title())) {
            return;
        }
        event.setCancelled(true);
        if (event.getClickedInventory() == null || event.getClickedInventory() != event.getView().getTopInventory()) {
            return;
        }

        HumanEntity clicker = event.getWhoClicked();
        int slot = event.getRawSlot();
        if (slot == CLOSE_SLOT) {
            if (clicker instanceof Player player) {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, SoundCategory.MASTER, 0.6f, 0.9f);
            }
            clicker.closeInventory();
            return;
        }
        if (slot == RELOAD_SLOT) {
            if (clicker instanceof Player player) {
                player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_RESONATE, SoundCategory.MASTER, 0.55f, 1.2f);
            }
            Inventory top = event.getView().getTopInventory();
            top.setItem(slot, createReloadingItem());
            scheduler.runLater(() -> {
                reloadService.reload(clicker);
                if (clicker instanceof Player player) {
                    player.openInventory(buildInventory());
                }
            }, 1L);
            return;
        }

        int toggleIndex = resolveToggleIndex(slot);
        List<EntityModuleToggle> toggles = configService.moduleToggles();
        if (toggleIndex < 0 || toggleIndex >= toggles.size()) {
            return;
        }

        EntityModuleToggle toggle = toggles.get(toggleIndex);
        boolean current = configService.isEnabled(toggle);
        boolean targetState = !current;

        if (!configService.setEnabled(toggle, targetState)) {
            clicker.sendMessage(Component.text(
                    "Failed to update module state for " + toggle.label() + ".",
                    NamedTextColor.RED
            ));
            return;
        }

        ModuleManager moduleManager = plugin.moduleManager();
        if (moduleManager != null && !moduleManager.setModuleEnabled(toggle.moduleId(), targetState)) {
            clicker.sendMessage(Component.text(
                    "Runtime update failed for " + toggle.label() + ". Use reload.",
                    NamedTextColor.RED
            ));
            return;
        }

        Component toggleMessage = Component.text(toggle.label(), NamedTextColor.YELLOW)
                .append(Component.text(" set to ", NamedTextColor.GRAY))
                .append(Component.text(targetState ? "enabled" : "disabled", targetState ? NamedTextColor.GREEN : NamedTextColor.RED))
                .append(Component.text(".", NamedTextColor.GRAY))
                .decoration(TextDecoration.ITALIC, false);
        clicker.sendMessage(toggleMessage);
        if (clicker instanceof Player player) {
            Sound stateSound = targetState ? Sound.BLOCK_NOTE_BLOCK_CHIME : Sound.BLOCK_AMETHYST_BLOCK_HIT;
            float statePitch = targetState ? 1.6f : 0.8f;
            player.playSound(player.getLocation(), stateSound, SoundCategory.MASTER, 0.6f, statePitch);
        }
        Inventory top = event.getView().getTopInventory();
        top.setItem(slot, createAnimatedStateItem(toggle, targetState));
        scheduler.runLater(() -> {
            clicker.openInventory(buildInventory());
        }, 1L);
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!TITLE.equals(event.getView().title())) {
            return;
        }
        for (int slot : event.getRawSlots()) {
            if (slot < event.getView().getTopInventory().getSize()) {
                event.setCancelled(true);
                return;
            }
        }
    }

    private Inventory buildInventory() {
        Inventory inventory = plugin.getServer().createInventory(null, SIZE, TITLE);
        fillBackground(inventory);

        List<EntityModuleToggle> toggles = configService.moduleToggles();
        for (int i = 0; i < toggles.size() && i < TOGGLE_SLOTS.length; i++) {
            EntityModuleToggle toggle = toggles.get(i);
            boolean enabled = configService.isEnabled(toggle);
            inventory.setItem(TOGGLE_SLOTS[i], createToggleItem(toggle, enabled));
        }
        inventory.setItem(RELOAD_SLOT, createReloadItem());
        inventory.setItem(CLOSE_SLOT, createCloseItem());
        return inventory;
    }

    private void fillBackground(Inventory inventory) {
        ItemStack frame = createPane(Component.text(" ", NamedTextColor.DARK_GRAY));
        for (int i = 0; i < SIZE; i++) {
            inventory.setItem(i, frame);
        }
    }

    private ItemStack createToggleItem(EntityModuleToggle toggle, boolean enabled) {
        ItemStack stack = new ItemStack(toggle.icon());
        ItemMeta meta = stack.getItemMeta();
        String entityName = toggle.entityType().id().toUpperCase();
        meta.displayName(
                Component.text(entityName + " ", NamedTextColor.YELLOW)
                        .decorate(TextDecoration.BOLD)
                        .append(Component.text("Settings", NamedTextColor.GRAY).decoration(TextDecoration.BOLD, false))
                        .decoration(TextDecoration.ITALIC, false)
        );
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("| ", NamedTextColor.WHITE).append(Component.text("Status: ", NamedTextColor.GRAY)).append(
                Component.text(enabled ? "ENABLED" : "DISABLED", enabled ? NamedTextColor.GREEN : NamedTextColor.RED)
                        .decorate(TextDecoration.BOLD)
        ));
        lore.add(Component.empty());
        lore.add(Component.text("(!) ", NamedTextColor.YELLOW).append(
                Component.text("Click to toggle hostility", NamedTextColor.GRAY)
        ));
        lore = lore.stream().map(line -> line.decoration(TextDecoration.ITALIC, false)).toList();
        meta.lore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        stack.setItemMeta(meta);
        return stack;
    }

    private ItemStack createReloadItem() {
        ItemStack stack = new ItemStack(Material.CLOCK);
        ItemMeta meta = stack.getItemMeta();
        meta.displayName(
                Component.text("RELOAD ", NamedTextColor.LIGHT_PURPLE)
                        .decorate(TextDecoration.BOLD)
                        .append(Component.text("Files", NamedTextColor.GRAY).decoration(TextDecoration.BOLD, false))
                        .decoration(TextDecoration.ITALIC, false)
        );
        List<Component> reloadLore = new ArrayList<>(List.of(
                Component.text("| ", NamedTextColor.WHITE).append(Component.text("Sync manual edits", NamedTextColor.GRAY)),
                Component.text("| ", NamedTextColor.WHITE).append(Component.text("from yml files", NamedTextColor.GRAY)),
                Component.empty(),
                Component.text("(!) ", NamedTextColor.YELLOW).append(
                        Component.text("Click to reload files", NamedTextColor.GRAY)
                )
        ));
        reloadLore.replaceAll(line -> line.decoration(TextDecoration.ITALIC, false));
        meta.lore(reloadLore);
        stack.setItemMeta(meta);
        return stack;
    }

    private ItemStack createCloseItem() {
        ItemStack stack = new ItemStack(Material.BARRIER);
        ItemMeta meta = stack.getItemMeta();
        meta.displayName(
                Component.text("CLOSE ", NamedTextColor.RED)
                        .decorate(TextDecoration.BOLD)
                        .append(Component.text("Menu", NamedTextColor.GRAY).decoration(TextDecoration.BOLD, false))
                        .decoration(TextDecoration.ITALIC, false)
        );
        List<Component> closeLore = new ArrayList<>(List.of(
                Component.text("(!) ", NamedTextColor.YELLOW).append(
                        Component.text("Click to close menu", NamedTextColor.GRAY)
                )
        ));
        closeLore.replaceAll(line -> line.decoration(TextDecoration.ITALIC, false));
        meta.lore(closeLore);
        stack.setItemMeta(meta);
        return stack;
    }

    private ItemStack createPane(Component name) {
        ItemStack stack = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = stack.getItemMeta();
        meta.displayName(name);
        stack.setItemMeta(meta);
        return stack;
    }

    private ItemStack createAnimatedStateItem(EntityModuleToggle toggle, boolean enabled) {
        ItemStack stack = new ItemStack(toggle.icon());
        ItemMeta meta = stack.getItemMeta();
        String entityName = toggle.entityType().id().toUpperCase();
        meta.displayName(
                Component.text(entityName + " ", NamedTextColor.YELLOW)
                        .decorate(TextDecoration.BOLD)
                        .append(Component.text("Settings", NamedTextColor.GRAY).decoration(TextDecoration.BOLD, false))
                        .decoration(TextDecoration.ITALIC, false)
        );
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("| ", NamedTextColor.WHITE).append(Component.text("Status: ", NamedTextColor.GRAY)).append(
                Component.text(enabled ? "ENABLED" : "DISABLED", enabled ? NamedTextColor.GREEN : NamedTextColor.RED)
                        .decorate(TextDecoration.BOLD)
        ).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("| ", NamedTextColor.WHITE).append(Component.text("Applying runtime update...", NamedTextColor.AQUA)).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        lore.add(Component.text("(!) ", NamedTextColor.YELLOW).append(
                Component.text("Done", NamedTextColor.GRAY)
        ).decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        stack.setItemMeta(meta);
        return stack;
    }

    private int resolveToggleIndex(int slot) {
        for (int i = 0; i < TOGGLE_SLOTS.length; i++) {
            if (TOGGLE_SLOTS[i] == slot) {
                return i;
            }
        }
        return -1;
    }

    private ItemStack createReloadingItem() {
        ItemStack stack = new ItemStack(Material.CLOCK);
        ItemMeta meta = stack.getItemMeta();
        meta.displayName(
                Component.text("RELOAD ", NamedTextColor.LIGHT_PURPLE)
                        .decorate(TextDecoration.BOLD)
                        .append(Component.text("Files", NamedTextColor.GRAY).decoration(TextDecoration.BOLD, false))
                        .decoration(TextDecoration.ITALIC, false)
        );
        List<Component> lore = new ArrayList<>(List.of(
                Component.text("| ", NamedTextColor.WHITE).append(Component.text("Syncing latest file changes...", NamedTextColor.AQUA)),
                Component.empty(),
                Component.text("(!) ", NamedTextColor.YELLOW).append(Component.text("Please wait 1 tick", NamedTextColor.GRAY))
        ));
        lore.replaceAll(line -> line.decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);
        stack.setItemMeta(meta);
        return stack;
    }
}
