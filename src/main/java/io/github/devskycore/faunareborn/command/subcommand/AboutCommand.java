package io.github.devskycore.faunareborn.command.subcommand;

import io.github.devskycore.faunareborn.command.permission.PermissionConstants;
import io.github.devskycore.faunareborn.lang.LanguageManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;

public final class AboutCommand implements FaunaSubcommand {

    private static final CommandInfo INFO = new CommandInfo(
            "about",
            "/fauna about",
            "Learn what FaunaReborn does.",
            PermissionConstants.COMMAND_ABOUT,
            false
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
        sender.sendMessage(Component.text(language.text("commands.about.title", "FaunaReborn"), NamedTextColor.GREEN));
        sender.sendMessage(Component.text(language.text("commands.about.tagline", "Nature fights back."), NamedTextColor.GOLD));
        sender.sendMessage(Component.text(language.text("commands.about.line1", "Turns passive animals into intelligent, configurable threats."), NamedTextColor.GRAY));
        sender.sendMessage(Component.text(language.text("commands.about.line2", "Includes per-entity settings, runtime controls, optimized processing, and advanced behavior systems."), NamedTextColor.GRAY));
    }
}
