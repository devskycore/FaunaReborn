package io.github.devskycore.faunareborn.command.message;

import io.github.devskycore.faunareborn.lang.LanguageManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.CommandSender;

public final class CommandMessages {

    private final LanguageManager language;

    public CommandMessages(LanguageManager language) {
        this.language = language;
    }

    public void sendNoPermission(CommandSender sender) {
        sender.sendMessage(prefix().append(Component.text(
                language.text("commands.common.no-permission", "You do not have permission to use this command."),
                NamedTextColor.RED
        )));
    }

    public void sendPlayerOnly(CommandSender sender) {
        sender.sendMessage(prefix().append(Component.text(
                language.text("commands.common.player-only", "This command can only be executed by a player."),
                NamedTextColor.RED
        )));
    }

    public void sendUnknownCommand(CommandSender sender) {
        sender.sendMessage(prefix().append(Component.text(
                language.text("commands.common.unknown-subcommand", "Unknown subcommand. Use /fauna help."),
                NamedTextColor.RED
        )));
    }

    public Component prefix() {
        return Component.text("[", NamedTextColor.DARK_GRAY)
                .append(Component.text(
                        language.text("commands.common.prefix", "FaunaReborn"),
                        NamedTextColor.GREEN
                ).decorate(TextDecoration.BOLD))
                .append(Component.text("] ", NamedTextColor.DARK_GRAY));
    }
}
