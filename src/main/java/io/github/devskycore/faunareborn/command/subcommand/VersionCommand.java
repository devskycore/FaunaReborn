package io.github.devskycore.faunareborn.command.subcommand;

import io.github.devskycore.faunareborn.command.permission.PermissionConstants;
import io.github.devskycore.faunareborn.core.FaunaRebornPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;

import java.util.List;

public final class VersionCommand implements FaunaSubcommand {

    private static final CommandInfo INFO = new CommandInfo(
            "version",
            "/fauna version",
            "Show plugin and runtime version details.",
            PermissionConstants.COMMAND_VERSION,
            false
    );

    private final FaunaRebornPlugin plugin;

    public VersionCommand(FaunaRebornPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public CommandInfo info() {
        return INFO;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        List<String> authors = plugin.getDescription().getAuthors();
        String authorText = authors.isEmpty() ? "Unknown" : String.join(", ", authors);

        sender.sendMessage(Component.text("FaunaReborn Version", NamedTextColor.GREEN));
        sender.sendMessage(Component.text(" - Name: ", NamedTextColor.DARK_GRAY).append(Component.text(plugin.getDescription().getName(), NamedTextColor.WHITE)));
        sender.sendMessage(Component.text(" - Version: ", NamedTextColor.DARK_GRAY).append(Component.text(plugin.getDescription().getVersion(), NamedTextColor.WHITE)));
        sender.sendMessage(Component.text(" - Authors: ", NamedTextColor.DARK_GRAY).append(Component.text(authorText, NamedTextColor.WHITE)));
        sender.sendMessage(Component.text(" - Minecraft: ", NamedTextColor.DARK_GRAY).append(Component.text(plugin.getServer().getMinecraftVersion(), NamedTextColor.WHITE)));
        sender.sendMessage(Component.text(" - Server: ", NamedTextColor.DARK_GRAY).append(Component.text(plugin.getServer().getName() + " " + plugin.getServer().getVersion(), NamedTextColor.WHITE)));
        sender.sendMessage(Component.text(" - Status: ", NamedTextColor.DARK_GRAY).append(Component.text("Running", NamedTextColor.GREEN)));
    }
}
