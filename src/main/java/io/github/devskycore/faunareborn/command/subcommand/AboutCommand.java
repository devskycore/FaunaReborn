package io.github.devskycore.faunareborn.command.subcommand;

import io.github.devskycore.faunareborn.command.permission.PermissionConstants;
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

    @Override
    public CommandInfo info() {
        return INFO;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        sender.sendMessage(Component.text("FaunaReborn", NamedTextColor.GREEN));
        sender.sendMessage(Component.text("Nature fights back.", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("Turns passive animals into intelligent, configurable threats.", NamedTextColor.GRAY));
        sender.sendMessage(Component.text("Includes per-entity settings, runtime controls, optimized processing, and advanced behavior systems.", NamedTextColor.GRAY));
    }
}
