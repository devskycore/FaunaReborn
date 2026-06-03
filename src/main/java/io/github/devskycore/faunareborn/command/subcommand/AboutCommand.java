package io.github.devskycore.faunareborn.command.subcommand;

import io.github.devskycore.faunareborn.command.permission.PermissionConstants;
import io.github.devskycore.faunareborn.lang.LanguageManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.CommandSender;

import java.util.List;

public final class AboutCommand implements FaunaSubcommand {

    private static final CommandInfo INFO = new CommandInfo(
            "about",
            "/fauna about",
            "Learn what FaunaReborn does.",
            PermissionConstants.COMMAND_ABOUT,
            false,
            List.of("info")
    );
    private final LanguageManager language;

    public AboutCommand(LanguageManager language) {
        this.language = language;
    }

    @Override
    public CommandInfo info() {
        return INFO;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        String pluginName = language.text("commands.common.prefix", "FaunaReborn");
        String title = language.text("commands.about.header-label", "ABOUT");

        sender.sendMessage(Component.text(pluginName, NamedTextColor.WHITE)
                .decorate(TextDecoration.BOLD)
                .append(Component.text(" ", NamedTextColor.GRAY).decoration(TextDecoration.BOLD, false))
                .append(Component.text("\u00B7", NamedTextColor.DARK_GRAY).decoration(TextDecoration.BOLD, false))
                .append(Component.text(" ", NamedTextColor.GRAY).decoration(TextDecoration.BOLD, false))
                .append(Component.text(title, NamedTextColor.GREEN))
                .decoration(TextDecoration.ITALIC, false));
        sender.sendMessage(Component.empty());
        sender.sendMessage(aboutLine(
                language.text("commands.about.tagline-label", "Tagline"),
                language.text("commands.about.tagline", "Nature fights back."),
                NamedTextColor.GOLD
        ));
        sender.sendMessage(aboutLine(
                language.text("commands.about.summary-label", "Summary"),
                language.text("commands.about.line1", "Turns passive animals into intelligent, configurable threats."),
                NamedTextColor.GRAY
        ));
        sender.sendMessage(aboutLine(
                language.text("commands.about.details-label", "Details"),
                language.text("commands.about.line2", "Includes per-entity settings, runtime controls, optimized processing, and advanced behavior systems."),
                NamedTextColor.GRAY
        ));
        sender.sendMessage(Component.empty());
    }

    private Component aboutLine(String label, String text, NamedTextColor textColor) {
        return Component.text("\u27A4 ", NamedTextColor.DARK_AQUA)
                .append(Component.text(label, NamedTextColor.AQUA))
                .append(Component.text(" - ", NamedTextColor.DARK_GRAY))
                .append(Component.text(text, textColor));
    }
}
