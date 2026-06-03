package io.github.devskycore.faunareborn.command.message;

import io.github.devskycore.faunareborn.lang.LanguageManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.CommandSender;

import java.util.List;

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

    public void sendUnknownCommand(CommandSender sender, String typedSubcommand, List<String> suggestions) {
        sender.sendMessage(blockLine(
                language.text("commands.common.unknown-subcommand", "Unknown subcommand. Use /fauna help."),
                NamedTextColor.RED
        ));

        if (suggestions.isEmpty()) {
            String helpCommand = "/fauna help";
            sender.sendMessage(blockLine(Component.text(
                    language.text("commands.common.try-help", "Try: "),
                    NamedTextColor.GRAY
            ).append(Component.text(helpCommand, NamedTextColor.AQUA)
                    .clickEvent(ClickEvent.suggestCommand(helpCommand))
                    .hoverEvent(HoverEvent.showText(suggestHoverText(helpCommand))))));
            sender.sendMessage(Component.empty());
            return;
        }

        if (suggestions.size() == 1) {
            String suggestedCommand = "/fauna " + suggestions.get(0);
            sender.sendMessage(blockLine(
                    language.text(
                            "commands.common.did-you-mean",
                            "Did you mean /fauna {command}?"
                    ).replace("{command}", suggestions.get(0)),
                    NamedTextColor.YELLOW
            ).clickEvent(ClickEvent.suggestCommand(suggestedCommand))
                    .hoverEvent(HoverEvent.showText(suggestHoverText(suggestedCommand)))
            );
            sender.sendMessage(Component.empty());
            return;
        }

        sender.sendMessage(blockLine(Component.text(
                language.text(
                        "commands.common.possible-subcommands",
                        "Possible subcommands: {commands}"
                ).replace("{commands}", ""),
                NamedTextColor.YELLOW
        )));
        Component line = Component.text("\u27A4 ", NamedTextColor.DARK_AQUA);
        for (int i = 0; i < suggestions.size(); i++) {
            String suggestion = suggestions.get(i);
            String suggestedCommand = "/fauna " + suggestion;
            if (i > 0) {
                line = line.append(Component.text(", ", NamedTextColor.GRAY));
            }
            line = line.append(Component.text(suggestion, NamedTextColor.AQUA)
                    .clickEvent(ClickEvent.suggestCommand(suggestedCommand))
                    .hoverEvent(HoverEvent.showText(suggestHoverText(suggestedCommand))));
        }
        sender.sendMessage(line);
        sender.sendMessage(Component.empty());
    }

    public Component prefix() {
        return Component.text("[", NamedTextColor.DARK_GRAY)
                .append(Component.text(
                        language.text("commands.common.prefix", "FaunaReborn"),
                        NamedTextColor.GREEN
                ).decorate(TextDecoration.BOLD))
                .append(Component.text("] ", NamedTextColor.DARK_GRAY));
    }

    private Component blockLine(String text, NamedTextColor color) {
        return blockLine(Component.text(text, color));
    }

    private Component blockLine(Component content) {
        return Component.text("\u27A4 ", NamedTextColor.DARK_AQUA)
                .append(content);
    }

    private Component suggestHoverText(String suggestedCommand) {
        String hoverText = language.textAny(
                "Suggest: {command}",
                "commands.help.click-to-suggest",
                "commands.help.click-to-use"
        ).replace("{command}", suggestedCommand);
        int separatorIndex = hoverText.indexOf(':');
        if (separatorIndex <= 0) {
            return Component.text(hoverText, NamedTextColor.GRAY);
        }
        String actionLabel = hoverText.substring(0, separatorIndex);
        String remainder = hoverText.substring(separatorIndex);
        return Component.text(actionLabel, NamedTextColor.YELLOW)
                .append(Component.text(remainder, NamedTextColor.GRAY));
    }
}
