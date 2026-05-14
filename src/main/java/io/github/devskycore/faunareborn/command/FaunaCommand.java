package io.github.devskycore.faunareborn.command;

import io.github.devskycore.faunareborn.command.message.CommandMessages;
import io.github.devskycore.faunareborn.command.permission.PermissionService;
import io.github.devskycore.faunareborn.command.subcommand.AboutCommand;
import io.github.devskycore.faunareborn.command.subcommand.CommandRegistry;
import io.github.devskycore.faunareborn.command.subcommand.EntitiesCommand;
import io.github.devskycore.faunareborn.command.subcommand.GuiCommand;
import io.github.devskycore.faunareborn.command.subcommand.HelpCommand;
import io.github.devskycore.faunareborn.command.subcommand.LangCommand;
import io.github.devskycore.faunareborn.command.subcommand.ReloadCommand;
import io.github.devskycore.faunareborn.command.subcommand.VersionCommand;
import io.github.devskycore.faunareborn.core.FaunaRebornPlugin;
import io.github.devskycore.faunareborn.gui.FaunaMainGui;
import io.github.devskycore.faunareborn.gui.PluginGuiConfigService;
import io.github.devskycore.faunareborn.lang.LanguageManager;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.command.CommandSender;
import org.jspecify.annotations.NonNull;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public final class FaunaCommand implements BasicCommand {

    private final CommandRegistry registry;

    public FaunaCommand(FaunaRebornPlugin plugin, FaunaMainGui mainGui, PluginGuiConfigService guiConfigService, LanguageManager language) {
        PermissionService permissionService = new PermissionService();
        CommandMessages commandMessages = new CommandMessages(language);

        HelpCommand helpCommand = new HelpCommand(permissionService, commandMessages, language);
        CommandRegistry builtRegistry = new CommandRegistry(
                CommandRegistry.defaults(
                        helpCommand,
                        new VersionCommand(plugin, language),
                        new AboutCommand(language),
                        new EntitiesCommand(guiConfigService, language),
                        new ReloadCommand(plugin.reloadService()),
                        new GuiCommand(mainGui, commandMessages),
                        new LangCommand(language, mainGui)
                ),
                permissionService,
                commandMessages
        );
        helpCommand.bindRegistry(builtRegistry);
        this.registry = builtRegistry;
    }

    @Override
    public void execute(@NonNull CommandSourceStack source, String[] args) {
        final CommandSender sender = source.getSender();
        registry.execute(sender, args);
    }

    @Override
    public @NonNull Collection<String> suggest(@NonNull CommandSourceStack source, String[] args) {
        final CommandSender sender = source.getSender();
        List<String> suggestions = registry.suggest(sender, args);
        return suggestions.isEmpty() ? Collections.emptyList() : List.copyOf(suggestions);
    }

    @Override
    public @NonNull String permission() {
        return "";
    }
}
