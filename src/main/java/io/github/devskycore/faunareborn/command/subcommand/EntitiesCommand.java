package io.github.devskycore.faunareborn.command.subcommand;

import io.github.devskycore.faunareborn.command.permission.PermissionConstants;
import io.github.devskycore.faunareborn.gui.EntityModuleToggle;
import io.github.devskycore.faunareborn.gui.PluginGuiConfigService;
import io.github.devskycore.faunareborn.lang.LanguageManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.CommandSender;

import java.util.List;

public final class EntitiesCommand implements FaunaSubcommand {

    private static final CommandInfo INFO = new CommandInfo(
            "entities",
            "/fauna entities",
            "List supported entities and current state.",
            PermissionConstants.COMMAND_ENTITIES,
            false,
            List.of("ent", "entity")
    );

    private final PluginGuiConfigService configService;
    private final LanguageManager language;

    public EntitiesCommand(PluginGuiConfigService configService, LanguageManager language) {
        this.configService = configService;
        this.language = language;
    }

    @Override
    public CommandInfo info() {
        return INFO;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        List<EntityModuleToggle> toggles = configService.moduleToggles();
        String pluginName = language.text("commands.common.prefix", "FaunaReborn");
        String title = language.text("commands.entities.header", "Supported Entities");
        sender.sendMessage(Component.text(pluginName, NamedTextColor.WHITE)
                .decorate(TextDecoration.BOLD)
                .append(Component.text(" ", NamedTextColor.GRAY).decoration(TextDecoration.BOLD, false))
                .append(Component.text("\u00B7", NamedTextColor.DARK_GRAY).decoration(TextDecoration.BOLD, false))
                .append(Component.text(" ", NamedTextColor.GRAY).decoration(TextDecoration.BOLD, false))
                .append(Component.text(title, NamedTextColor.GREEN))
                .decoration(TextDecoration.ITALIC, false));
        sender.sendMessage(Component.empty());
        for (EntityModuleToggle toggle : toggles) {
            boolean enabled = configService.isEnabled(toggle);
            String status = enabled
                    ? language.text("commands.common.state.enabled-title", "Enabled")
                    : language.text("commands.common.state.disabled-title", "Disabled");
            sender.sendMessage(Component.text("\u27A4 ", NamedTextColor.DARK_AQUA)
                    .append(Component.text(toggle.label(), NamedTextColor.AQUA))
                    .append(Component.text(" - ", NamedTextColor.DARK_GRAY))
                    .append(Component.text(status, enabled ? NamedTextColor.GREEN : NamedTextColor.RED)));
        }
        sender.sendMessage(Component.empty());
    }
}
