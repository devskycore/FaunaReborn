package io.github.devskycore.faunareborn.command.subcommand;

import io.github.devskycore.faunareborn.command.permission.PermissionConstants;
import io.github.devskycore.faunareborn.core.FaunaRebornPlugin;
import io.github.devskycore.faunareborn.lang.LanguageManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.CommandSender;

import java.util.List;

public final class VersionCommand implements FaunaSubcommand {

    private static final CommandInfo INFO = new CommandInfo(
            "version",
            "/fauna version",
            "Show plugin and runtime version details.",
            PermissionConstants.COMMAND_VERSION,
            false,
            List.of("ver", "v")
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
        List<String> authors = plugin.getPluginMeta().getAuthors();
        String authorText = authors.isEmpty() ? language.text("commands.version.unknown-author", "Unknown") : String.join(", ", authors);
        String pluginName = language.text("commands.common.prefix", "FaunaReborn");
        String title = language.text("commands.version.header-label", "VERSION");

        sender.sendMessage(Component.text(pluginName, NamedTextColor.WHITE)
                .decorate(TextDecoration.BOLD)
                .append(Component.text(" ", NamedTextColor.GRAY).decoration(TextDecoration.BOLD, false))
                .append(Component.text("\u00B7", NamedTextColor.DARK_GRAY).decoration(TextDecoration.BOLD, false))
                .append(Component.text(" ", NamedTextColor.GRAY).decoration(TextDecoration.BOLD, false))
                .append(Component.text(title, NamedTextColor.GREEN))
                .decoration(TextDecoration.ITALIC, false));
        sender.sendMessage(Component.empty());
        sender.sendMessage(versionLine(language.text("commands.version.name-label", "Name"), plugin.getPluginMeta().getName(), NamedTextColor.WHITE));
        sender.sendMessage(versionLine(language.text("commands.version.version-label", "Version"), plugin.getPluginMeta().getVersion(), NamedTextColor.WHITE));
        sender.sendMessage(versionLine(language.text("commands.version.authors-label", "Authors"), authorText, NamedTextColor.WHITE));
        sender.sendMessage(versionLine(language.text("commands.version.minecraft-label", "Minecraft"), plugin.getServer().getMinecraftVersion(), NamedTextColor.WHITE));
        sender.sendMessage(versionLine(language.text("commands.version.server-label", "Server"), plugin.getServer().getName() + " " + plugin.getServer().getVersion(), NamedTextColor.WHITE));
        sender.sendMessage(versionLine(
                language.text("commands.version.status-label", "Status"),
                language.text("commands.version.status-running", "Running"),
                NamedTextColor.GREEN
        ));
        sender.sendMessage(Component.empty());
    }

    private Component versionLine(String label, String value, NamedTextColor valueColor) {
        return Component.text("\u27A4 ", NamedTextColor.DARK_AQUA)
                .append(Component.text(label, NamedTextColor.AQUA))
                .append(Component.text(" - ", NamedTextColor.DARK_GRAY))
                .append(Component.text(value, valueColor));
    }
}
