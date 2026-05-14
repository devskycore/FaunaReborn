package io.github.devskycore.faunareborn.gui;

import io.github.devskycore.faunareborn.command.FaunaReloadService;
import io.github.devskycore.faunareborn.command.message.CommandMessages;
import io.github.devskycore.faunareborn.command.permission.PermissionService;
import io.github.devskycore.faunareborn.core.FaunaRebornPlugin;
import io.github.devskycore.faunareborn.lang.LanguageManager;
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
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public final class FaunaMainGui implements Listener {
    private static final int SIZE = 54;
    private static final int LANGUAGE_SLOT = 45;
    private static final int RELOAD_SLOT = 49;
    private static final int CLOSE_SLOT = 53;
    private static final int[] TOGGLE_SLOTS = {20, 22, 24, 33};
    private static final int LANGUAGE_GUI_SIZE = 27;
    private static final int LANGUAGE_ENGLISH_SLOT = 11;
    private static final int LANGUAGE_PORTUGUESE_SLOT = 13;
    private static final int LANGUAGE_SPANISH_SLOT = 15;
    private static final int LANGUAGE_BACK_SLOT = 22;

    private final FaunaRebornPlugin plugin;
    private final PluginGuiConfigService configService;
    private final FaunaReloadService reloadService;
    private final SchedulerAdapter scheduler;
    private final PermissionService permissionService;
    private final LanguageManager language;
    private final CommandMessages commandMessages;

    public FaunaMainGui(
            FaunaRebornPlugin plugin,
            PluginGuiConfigService configService,
            FaunaReloadService reloadService
    ) {
        this.plugin = plugin;
        this.configService = configService;
        this.reloadService = reloadService;
        this.scheduler = SchedulerAdapters.create(plugin);
        this.permissionService = new PermissionService();
        this.language = plugin.languageManager();
        this.commandMessages = new CommandMessages(language);
    }

    public void open(Player player) {
        if (!permissionService.canUseGui(player)) {
            commandMessages.sendNoPermission(player);
            return;
        }
        player.openInventory(buildInventory());
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, SoundCategory.MASTER, 0.5f, 1.25f);
    }

    public void openLanguageSelector(Player player) {
        if (!permissionService.canUseGui(player)) {
            commandMessages.sendNoPermission(player);
            return;
        }
        player.openInventory(buildLanguageInventory());
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, SoundCategory.MASTER, 0.55f, 1.15f);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        GuiViewType viewType = resolveViewType(event.getView().getTopInventory());
        if (viewType == GuiViewType.MAIN) {
            handleMainGuiClick(event);
            return;
        }
        if (viewType == GuiViewType.LANGUAGE) {
            handleLanguageGuiClick(event);
        }
    }

    private void handleMainGuiClick(InventoryClickEvent event) {
        event.setCancelled(true);
        if (event.getClickedInventory() == null || event.getClickedInventory() != event.getView().getTopInventory()) {
            return;
        }

        HumanEntity clicker = event.getWhoClicked();
        if (!permissionService.canUseGui(clicker)) {
            commandMessages.sendNoPermission(clicker);
            clicker.closeInventory();
            return;
        }
        int slot = event.getRawSlot();
        if (slot == LANGUAGE_SLOT) {
            clicker.openInventory(buildLanguageInventory());
            if (clicker instanceof Player player) {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, SoundCategory.MASTER, 0.55f, 1.15f);
            }
            return;
        }

        if (slot == CLOSE_SLOT) {
            if (clicker instanceof Player player) {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, SoundCategory.MASTER, 0.6f, 0.9f);
            }
            clicker.closeInventory();
            return;
        }
        if (slot == RELOAD_SLOT) {
            if (!permissionService.canUseReload(clicker)) {
                commandMessages.sendNoPermission(clicker);
                return;
            }
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
                    language.text("gui.feedback.failed-update-state", "Failed to update module state for ") + toggle.label() + ".",
                    NamedTextColor.RED
            ));
            return;
        }

        ModuleManager moduleManager = plugin.moduleManager();
        if (moduleManager != null && !moduleManager.setModuleEnabled(toggle.moduleId(), targetState)) {
            clicker.sendMessage(Component.text(
                    language.text("gui.feedback.failed-runtime-update", "Runtime update failed for ") + toggle.label() + language.text("gui.feedback.failed-runtime-update-suffix", ". Use reload."),
                    NamedTextColor.RED
            ));
            return;
        }

        Component toggleMessage = Component.text(toggle.label(), NamedTextColor.YELLOW)
                .append(Component.text(language.text("gui.feedback.toggle-set-to", " set to "), NamedTextColor.GRAY))
                .append(Component.text(
                        targetState
                                ? language.text("commands.common.state.enabled", "enabled")
                                : language.text("commands.common.state.disabled", "disabled"),
                        targetState ? NamedTextColor.GREEN : NamedTextColor.RED
                ))
                .append(Component.text(language.text("gui.feedback.dot", "."), NamedTextColor.GRAY))
                .decoration(TextDecoration.ITALIC, false);
        clicker.sendMessage(toggleMessage);
        if (clicker instanceof Player player) {
            Sound stateSound = targetState ? Sound.BLOCK_NOTE_BLOCK_CHIME : Sound.BLOCK_AMETHYST_BLOCK_HIT;
            float statePitch = targetState ? 1.6f : 0.8f;
            player.playSound(player.getLocation(), stateSound, SoundCategory.MASTER, 0.6f, statePitch);
        }
        Inventory top = event.getView().getTopInventory();
        top.setItem(slot, createAnimatedStateItem(toggle, targetState));
        scheduler.runLater(() -> clicker.openInventory(buildInventory()), 1L);
    }

    private void handleLanguageGuiClick(InventoryClickEvent event) {
        event.setCancelled(true);
        if (event.getClickedInventory() == null || event.getClickedInventory() != event.getView().getTopInventory()) {
            return;
        }

        HumanEntity clicker = event.getWhoClicked();
        if (!permissionService.canUseGui(clicker)) {
            commandMessages.sendNoPermission(clicker);
            clicker.closeInventory();
            return;
        }

        int slot = event.getRawSlot();
        if (slot == LANGUAGE_BACK_SLOT) {
            clicker.openInventory(buildInventory());
            if (clicker instanceof Player player) {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, SoundCategory.MASTER, 0.6f, 0.9f);
            }
            return;
        }
        if (slot == LANGUAGE_ENGLISH_SLOT) {
            applyLanguageSelection(clicker, "en");
            return;
        }
        if (slot == LANGUAGE_PORTUGUESE_SLOT) {
            applyLanguageSelection(clicker, "pt");
            return;
        }
        if (slot == LANGUAGE_SPANISH_SLOT) {
            applyLanguageSelection(clicker, "es");
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (resolveViewType(event.getView().getTopInventory()) == GuiViewType.NONE) {
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
        Inventory inventory = plugin.getServer().createInventory(new GuiHolder(GuiViewType.MAIN), SIZE, title());
        fillBackground(inventory);

        List<EntityModuleToggle> toggles = configService.moduleToggles();
        for (int i = 0; i < toggles.size() && i < TOGGLE_SLOTS.length; i++) {
            EntityModuleToggle toggle = toggles.get(i);
            boolean enabled = configService.isEnabled(toggle);
            inventory.setItem(TOGGLE_SLOTS[i], createToggleItem(toggle, enabled));
        }
        inventory.setItem(LANGUAGE_SLOT, createLanguageItem());
        inventory.setItem(RELOAD_SLOT, createReloadItem());
        inventory.setItem(CLOSE_SLOT, createCloseItem());
        return inventory;
    }

    private Inventory buildLanguageInventory() {
        Inventory inventory = plugin.getServer().createInventory(new GuiHolder(GuiViewType.LANGUAGE), LANGUAGE_GUI_SIZE, languageTitle());
        ItemStack frame = createPane(Component.text(" ", NamedTextColor.DARK_GRAY));
        for (int i = 0; i < LANGUAGE_GUI_SIZE; i++) {
            inventory.setItem(i, frame);
        }
        inventory.setItem(LANGUAGE_ENGLISH_SLOT, createLanguageOptionItem("ENGLISH", "en", Material.WHITE_BANNER));
        inventory.setItem(LANGUAGE_PORTUGUESE_SLOT, createLanguageOptionItem("PORTUGUÊS", "pt", Material.GREEN_BANNER));
        inventory.setItem(LANGUAGE_SPANISH_SLOT, createLanguageOptionItem("ESPAÑOL", "es", Material.RED_BANNER));
        inventory.setItem(LANGUAGE_BACK_SLOT, createLanguageBackItem());
        return inventory;
    }

    private Component title() {
        return Component.text(language.text("gui.title.main", "FaunaReborn"), NamedTextColor.BLACK)
                .decorate(TextDecoration.BOLD)
                .append(Component.text(" | ", NamedTextColor.GRAY).decoration(TextDecoration.BOLD, false))
                .append(Component.text(language.text("gui.title.panel", "Control Panel"), NamedTextColor.DARK_GRAY).decoration(TextDecoration.BOLD, false))
                .decoration(TextDecoration.ITALIC, false);
    }

    private Component languageTitle() {
        return Component.text(language.text("gui.language.title-main", "Language"), NamedTextColor.BLACK)
                .decorate(TextDecoration.BOLD)
                .append(Component.text(" | ", NamedTextColor.GRAY).decoration(TextDecoration.BOLD, false))
                .append(Component.text(language.text("gui.language.title-tail", "Select"), NamedTextColor.DARK_GRAY).decoration(TextDecoration.BOLD, false))
                .decoration(TextDecoration.ITALIC, false);
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
        String entityName = entityDisplayName(toggle);
        meta.displayName(
                Component.text(entityName + " ", NamedTextColor.YELLOW)
                        .decorate(TextDecoration.BOLD)
                        .append(Component.text(language.text("gui.common.settings", "Settings"), NamedTextColor.GRAY).decoration(TextDecoration.BOLD, false))
                        .decoration(TextDecoration.ITALIC, false)
        );
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("| ", NamedTextColor.WHITE).append(Component.text(language.text("gui.common.status", "Status") + ": ", NamedTextColor.GRAY)).append(
                Component.text(
                        enabled ? language.text("gui.common.enabled", "ENABLED") : language.text("gui.common.disabled", "DISABLED"),
                        enabled ? NamedTextColor.GREEN : NamedTextColor.RED
                )
                        .decorate(TextDecoration.BOLD)
        ));
        lore.add(Component.empty());
        lore.add(Component.text("(!) ", NamedTextColor.YELLOW).append(
                Component.text(language.text("gui.toggle.click-to-toggle", "Click to toggle hostility"), NamedTextColor.GRAY)
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
                Component.text(language.text("gui.reload.title-main", "RELOAD") + " ", NamedTextColor.LIGHT_PURPLE)
                        .decorate(TextDecoration.BOLD)
                        .append(Component.text(language.text("gui.reload.title-tail", "Files"), NamedTextColor.GRAY).decoration(TextDecoration.BOLD, false))
                        .decoration(TextDecoration.ITALIC, false)
        );
        List<Component> reloadLore = new ArrayList<>(List.of(
                Component.text("| ", NamedTextColor.WHITE).append(Component.text(language.text("gui.reload.applies-manual", "Applies manual changes"), NamedTextColor.GRAY)),
                Component.empty(),
                Component.text("(!) ", NamedTextColor.YELLOW).append(
                        Component.text(language.text("gui.reload.click-to-reload", "Click to reload files"), NamedTextColor.GRAY)
                )
        ));
        reloadLore.replaceAll(line -> line.decoration(TextDecoration.ITALIC, false));
        meta.lore(reloadLore);
        stack.setItemMeta(meta);
        return stack;
    }

    private ItemStack createLanguageItem() {
        ItemStack stack = new ItemStack(Material.NAME_TAG);
        ItemMeta meta = stack.getItemMeta();
        meta.displayName(
                Component.text(language.text("gui.language.button-main", "LANGUAGE") + " ", NamedTextColor.GOLD)
                        .decorate(TextDecoration.BOLD)
                        .append(Component.text(language.text("gui.language.button-tail", "Menu"), NamedTextColor.GRAY).decoration(TextDecoration.BOLD, false))
                        .decoration(TextDecoration.ITALIC, false)
        );
        List<Component> lore = new ArrayList<>(List.of(
                Component.text("| ", NamedTextColor.WHITE).append(Component.text(language.text("gui.language.current-prefix", "Current") + ": " + activeLanguageCode(), NamedTextColor.GRAY)),
                Component.empty(),
                Component.text("(!) ", NamedTextColor.YELLOW).append(Component.text(language.text("gui.language.click-to-open", "Click to change language"), NamedTextColor.GRAY))
        ));
        lore.replaceAll(line -> line.decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);
        stack.setItemMeta(meta);
        return stack;
    }

    private ItemStack createLanguageOptionItem(String label, String fileName, Material material) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        boolean selected = activeLanguageCode().equalsIgnoreCase(fileName);
        meta.displayName(
                Component.text(label, selected ? NamedTextColor.GREEN : NamedTextColor.YELLOW)
                        .decorate(TextDecoration.BOLD)
                        .decoration(TextDecoration.ITALIC, false)
        );
        List<Component> lore = new ArrayList<>(List.of(
                Component.text("| ", NamedTextColor.WHITE)
                        .append(Component.text(language.text("gui.language.status-label", "Status") + ": ", NamedTextColor.GRAY))
                        .append(Component.text(
                                selected
                                        ? language.text("gui.language.selected", "SELECTED")
                                        : language.text("gui.language.available", "AVAILABLE"),
                                selected ? NamedTextColor.GREEN : NamedTextColor.GRAY
                        ).decorate(TextDecoration.BOLD)),
                Component.empty(),
                Component.text("(!) ", NamedTextColor.YELLOW).append(Component.text(language.text("gui.language.click-to-select", "Click to select"), NamedTextColor.GRAY))
        ));
        lore.replaceAll(line -> line.decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);
        stack.setItemMeta(meta);
        return stack;
    }

    private ItemStack createLanguageBackItem() {
        ItemStack stack = new ItemStack(Material.ARROW);
        ItemMeta meta = stack.getItemMeta();
        meta.displayName(
                Component.text(language.text("gui.language.back-main", "BACK") + " ", NamedTextColor.RED)
                        .decorate(TextDecoration.BOLD)
                        .append(Component.text(language.text("gui.language.back-tail", "Main Menu"), NamedTextColor.GRAY).decoration(TextDecoration.BOLD, false))
                        .decoration(TextDecoration.ITALIC, false)
        );
        List<Component> lore = new ArrayList<>(List.of(
                Component.text("| ", NamedTextColor.WHITE).append(Component.text(language.text("gui.language.back-description", "Return to the control panel"), NamedTextColor.GRAY)),
                Component.empty(),
                Component.text("(!) ", NamedTextColor.YELLOW).append(Component.text(language.text("gui.language.back-click", "Click to go back"), NamedTextColor.GRAY))
        ));
        lore.replaceAll(line -> line.decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);
        stack.setItemMeta(meta);
        return stack;
    }

    private ItemStack createCloseItem() {
        ItemStack stack = new ItemStack(Material.BARRIER);
        ItemMeta meta = stack.getItemMeta();
        meta.displayName(
                Component.text(language.text("gui.close.title-main", "CLOSE") + " ", NamedTextColor.RED)
                        .decorate(TextDecoration.BOLD)
                        .append(Component.text(language.text("gui.close.title-tail", "Menu"), NamedTextColor.GRAY).decoration(TextDecoration.BOLD, false))
                        .decoration(TextDecoration.ITALIC, false)
        );
        List<Component> closeLore = new ArrayList<>(List.of(
                Component.text("| ", NamedTextColor.WHITE).append(Component.text(language.text("gui.close.exit-panel", "Exit this control panel"), NamedTextColor.GRAY)),
                Component.empty(),
                Component.text("(!) ", NamedTextColor.YELLOW).append(
                        Component.text(language.text("gui.close.click-to-close", "Click to close menu"), NamedTextColor.GRAY)
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
        String entityName = entityDisplayName(toggle);
        meta.displayName(
                Component.text(entityName + " ", NamedTextColor.YELLOW)
                        .decorate(TextDecoration.BOLD)
                        .append(Component.text(language.text("gui.common.settings", "Settings"), NamedTextColor.GRAY).decoration(TextDecoration.BOLD, false))
                        .decoration(TextDecoration.ITALIC, false)
        );
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("| ", NamedTextColor.WHITE).append(Component.text(language.text("gui.common.status", "Status") + ": ", NamedTextColor.GRAY)).append(
                Component.text(enabled ? language.text("gui.common.enabled", "ENABLED") : language.text("gui.common.disabled", "DISABLED"), enabled ? NamedTextColor.GREEN : NamedTextColor.RED)
                        .decorate(TextDecoration.BOLD)
        ).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("| ", NamedTextColor.WHITE).append(Component.text(language.text("gui.toggle.applying-update", "Applying runtime update..."), NamedTextColor.AQUA)).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        lore.add(Component.text("(!) ", NamedTextColor.YELLOW).append(
                Component.text(language.text("gui.toggle.done", "Done"), NamedTextColor.GRAY)
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
                Component.text(language.text("gui.reload.title-main", "RELOAD") + " ", NamedTextColor.LIGHT_PURPLE)
                        .decorate(TextDecoration.BOLD)
                        .append(Component.text(language.text("gui.reload.title-tail", "Files"), NamedTextColor.GRAY).decoration(TextDecoration.BOLD, false))
                        .decoration(TextDecoration.ITALIC, false)
        );
        List<Component> lore = new ArrayList<>(List.of(
                Component.text("| ", NamedTextColor.WHITE).append(Component.text(language.text("gui.reload.syncing-files", "Syncing latest file changes..."), NamedTextColor.AQUA)),
                Component.empty(),
                Component.text("(!) ", NamedTextColor.YELLOW).append(Component.text(language.text("gui.reload.please-wait-tick", "Please wait 1 tick"), NamedTextColor.GRAY))
        ));
        lore.replaceAll(line -> line.decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);
        stack.setItemMeta(meta);
        return stack;
    }

    private void applyLanguageSelection(HumanEntity clicker, String languageCode) {
        if (activeLanguageCode().equalsIgnoreCase(languageCode)) {
            clicker.sendMessage(Component.text(
                    plugin.languageManager().text("gui.language.already-selected", "The plugin language is already set to {file}.", java.util.Map.of("file", languageCode)),
                    NamedTextColor.YELLOW
            ).decoration(TextDecoration.ITALIC, false));
            if (clicker instanceof Player player) {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, SoundCategory.MASTER, 0.5f, 0.95f);
            }
            clicker.openInventory(buildLanguageInventory());
            return;
        }
        plugin.languageManager().switchLanguage(languageCode);
        clicker.sendMessage(Component.text(
                plugin.languageManager().text("gui.language.changed", "Language changed to {file}.", java.util.Map.of("file", languageCode)),
                NamedTextColor.GREEN
        ).decoration(TextDecoration.ITALIC, false));
        if (clicker instanceof Player player) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, SoundCategory.MASTER, 0.6f, 1.4f);
        }
        clicker.openInventory(buildLanguageInventory());
    }

    private String activeLanguageCode() {
        return plugin.languageManager().currentLanguageCode();
    }

    private GuiViewType resolveViewType(Inventory inventory) {
        if (inventory == null) {
            return GuiViewType.NONE;
        }
        InventoryHolder holder = inventory.getHolder();
        if (holder instanceof GuiHolder guiHolder) {
            return guiHolder.viewType();
        }
        return GuiViewType.NONE;
    }

    private String entityDisplayName(EntityModuleToggle toggle) {
        return language.text(
                "gui.entities." + toggle.entityType().id(),
                toggle.entityType().id().toUpperCase()
        );
    }

    private enum GuiViewType {
        MAIN,
        LANGUAGE,
        NONE
    }

    private record GuiHolder(GuiViewType viewType) implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }

}
