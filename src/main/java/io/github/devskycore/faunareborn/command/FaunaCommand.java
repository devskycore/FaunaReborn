package io.github.devskycore.faunareborn.command;

import io.github.devskycore.faunareborn.core.FaunaRebornPlugin;
import io.github.devskycore.faunareborn.gui.FaunaMainGui;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.jspecify.annotations.NonNull;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class FaunaCommand implements BasicCommand {

    private static final String SUBCOMMAND_RELOAD = "reload";
    private static final String SUBCOMMAND_GUI = "gui";
    private static final String RELOAD_PERMISSION = "faunareborn.command.reload";
    private static final String GUI_PERMISSION = "faunareborn.command.gui";

    private final FaunaReloadService reloadService;
    private final FaunaMainGui mainGui;

    public FaunaCommand(FaunaRebornPlugin plugin, FaunaMainGui mainGui) {
        this.reloadService = new FaunaReloadService(plugin);
        this.mainGui = mainGui;
    }

    @Override
    public void execute(@NonNull CommandSourceStack source, String[] args) {
        final CommandSender sender = source.getSender();

        if (args.length != 1) {
            sender.sendMessage("Usage: /fauna <reload|gui>");
            return;
        }

        String subcommand = args[0].toLowerCase(Locale.ROOT);
        if (SUBCOMMAND_RELOAD.equals(subcommand)) {
            executeReload(sender);
            return;
        }

        if (SUBCOMMAND_GUI.equals(subcommand)) {
            executeGui(sender);
            return;
        }

        sender.sendMessage("Usage: /fauna <reload|gui>");
    }

    @Override
    public @NonNull Collection<String> suggest(@NonNull CommandSourceStack source, String[] args) {
        final CommandSender sender = source.getSender();
        if (args.length != 1) {
            return Collections.emptyList();
        }
        String token = args[0].toLowerCase(Locale.ROOT);
        List<String> options = new java.util.ArrayList<>(2);
        if (sender.hasPermission(RELOAD_PERMISSION) && SUBCOMMAND_RELOAD.startsWith(token)) {
            options.add(SUBCOMMAND_RELOAD);
        }
        if (sender.hasPermission(GUI_PERMISSION) && SUBCOMMAND_GUI.startsWith(token)) {
            options.add(SUBCOMMAND_GUI);
        }
        return options.isEmpty() ? Collections.emptyList() : List.copyOf(options);
    }

    @Override
    public @NonNull String permission() {
        return "";
    }

    private void executeReload(CommandSender sender) {
        if (!sender.hasPermission(RELOAD_PERMISSION)) {
            sender.sendMessage("You do not have permission to use this command.");
            return;
        }
        reloadService.reload(sender);
    }

    private void executeGui(CommandSender sender) {
        if (!sender.hasPermission(GUI_PERMISSION)) {
            sender.sendMessage("You do not have permission to use this command.");
            return;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can open the FaunaReborn GUI.");
            return;
        }
        mainGui.open(player);
    }
}

