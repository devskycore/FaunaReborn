package io.github.devskycore.faunareborn.command.subcommand;

import io.github.devskycore.faunareborn.command.permission.PermissionConstants;
import io.github.devskycore.faunareborn.gui.EntityModuleToggle;
import io.github.devskycore.faunareborn.gui.PluginGuiConfigService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;

import java.util.List;

public final class EntitiesCommand implements FaunaSubcommand {

    private static final CommandInfo INFO = new CommandInfo(
            "entities",
            "/fauna entities",
            "List supported entities and current state.",
            PermissionConstants.COMMAND_ENTITIES,
            false
    );

    private final PluginGuiConfigService configService;

    public EntitiesCommand(PluginGuiConfigService configService) {
        this.configService = configService;
    }

    @Override
    public CommandInfo info() {
        return INFO;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        List<EntityModuleToggle> toggles = configService.moduleToggles();
        sender.sendMessage(Component.text("Supported Entities", NamedTextColor.GREEN));
        for (EntityModuleToggle toggle : toggles) {
            boolean enabled = configService.isEnabled(toggle);
            sender.sendMessage(Component.text(" - ", NamedTextColor.DARK_GRAY)
                    .append(Component.text(toggle.label(), NamedTextColor.WHITE))
                    .append(Component.text(": ", NamedTextColor.DARK_GRAY))
                    .append(Component.text(enabled ? "Enabled" : "Disabled", enabled ? NamedTextColor.GREEN : NamedTextColor.RED)));
        }
    }
}
