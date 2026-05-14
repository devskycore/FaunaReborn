package io.github.devskycore.faunareborn.command.subcommand;

import io.github.devskycore.faunareborn.command.permission.PermissionConstants;
import io.github.devskycore.faunareborn.gui.EntityModuleToggle;
import io.github.devskycore.faunareborn.gui.PluginGuiConfigService;
import io.github.devskycore.faunareborn.lang.LanguageManager;
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
        sender.sendMessage(Component.text(language.text("commands.entities.header", "Supported Entities"), NamedTextColor.GREEN));
        for (EntityModuleToggle toggle : toggles) {
            boolean enabled = configService.isEnabled(toggle);
            String status = enabled
                    ? language.text("commands.common.state.enabled-title", "Enabled")
                    : language.text("commands.common.state.disabled-title", "Disabled");
            sender.sendMessage(Component.text(" - ", NamedTextColor.DARK_GRAY)
                    .append(Component.text(toggle.label(), NamedTextColor.WHITE))
                    .append(Component.text(": ", NamedTextColor.DARK_GRAY))
                    .append(Component.text(status, enabled ? NamedTextColor.GREEN : NamedTextColor.RED)));
        }
    }
}
