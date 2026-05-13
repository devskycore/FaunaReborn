package io.github.devskycore.faunareborn.command.message;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.CommandSender;

public final class CommandMessages {

    private CommandMessages() {
    }

    public static void sendNoPermission(CommandSender sender) {
        sender.sendMessage(prefix().append(Component.text("You do not have permission to use this command.", NamedTextColor.RED)));
    }

    public static void sendPlayerOnly(CommandSender sender) {
        sender.sendMessage(prefix().append(Component.text("This command can only be executed by a player.", NamedTextColor.RED)));
    }

    public static void sendUnknownCommand(CommandSender sender) {
        sender.sendMessage(prefix().append(Component.text("Unknown subcommand. Use /fauna help.", NamedTextColor.RED)));
    }

    public static Component prefix() {
        return Component.text("[", NamedTextColor.DARK_GRAY)
                .append(Component.text("FaunaReborn", NamedTextColor.GREEN).decorate(TextDecoration.BOLD))
                .append(Component.text("] ", NamedTextColor.DARK_GRAY));
    }
}
