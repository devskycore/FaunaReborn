package io.github.devskycore.faunareborn.command.subcommand;

import io.github.devskycore.faunareborn.command.permission.PermissionConstants;
import io.github.devskycore.faunareborn.core.FaunaRebornPlugin;
import io.github.devskycore.faunareborn.lang.LanguageManager;
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
    private final LanguageManager language;

    public VersionCommand(FaunaRebornPlugin plugin, LanguageManager language) {
        this.plugin = plugin;
        this.language = language;
    }

    @Override
    public CommandInfo info() {
        return INFO;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        List<String> authors = plugin.getDescription().getAuthors();
        String authorText = authors.isEmpty() ? language.text("commands.version.unknown-author", "Unknown") : String.join(", ", authors);

        sender.sendMessage(Component.text(language.text("commands.version.header", "FaunaReborn Version"), NamedTextColor.GREEN));
        sender.sendMessage(Component.text(" - " + language.text("commands.version.name-label", "Name") + ": ", NamedTextColor.DARK_GRAY).append(Component.text(plugin.getDescription().getName(), NamedTextColor.WHITE)));
        sender.sendMessage(Component.text(" - " + language.text("commands.version.version-label", "Version") + ": ", NamedTextColor.DARK_GRAY).append(Component.text(plugin.getDescription().getVersion(), NamedTextColor.WHITE)));
        sender.sendMessage(Component.text(" - " + language.text("commands.version.authors-label", "Authors") + ": ", NamedTextColor.DARK_GRAY).append(Component.text(authorText, NamedTextColor.WHITE)));
        sender.sendMessage(Component.text(" - " + language.text("commands.version.minecraft-label", "Minecraft") + ": ", NamedTextColor.DARK_GRAY).append(Component.text(plugin.getServer().getMinecraftVersion(), NamedTextColor.WHITE)));
        sender.sendMessage(Component.text(" - " + language.text("commands.version.server-label", "Server") + ": ", NamedTextColor.DARK_GRAY).append(Component.text(plugin.getServer().getName() + " " + plugin.getServer().getVersion(), NamedTextColor.WHITE)));
        sender.sendMessage(Component.text(" - " + language.text("commands.version.status-label", "Status") + ": ", NamedTextColor.DARK_GRAY).append(Component.text(language.text("commands.version.status-running", "Running"), NamedTextColor.GREEN)));
    }
}
