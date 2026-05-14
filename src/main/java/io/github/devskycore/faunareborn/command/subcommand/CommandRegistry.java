package io.github.devskycore.faunareborn.command.subcommand;

import io.github.devskycore.faunareborn.command.message.CommandMessages;
import io.github.devskycore.faunareborn.command.permission.PermissionService;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class CommandRegistry {

    private final Map<String, FaunaSubcommand> subcommands;
    private final PermissionService permissions;
    private final CommandMessages commandMessages;

    public CommandRegistry(List<FaunaSubcommand> commandList, PermissionService permissions, CommandMessages commandMessages) {
        this.permissions = permissions;
        this.commandMessages = commandMessages;
        Map<String, FaunaSubcommand> byName = new LinkedHashMap<>();
        for (FaunaSubcommand subcommand : commandList) {
            byName.put(subcommand.info().name().toLowerCase(Locale.ROOT), subcommand);
        }
        this.subcommands = Map.copyOf(byName);
    }

    public void execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            FaunaSubcommand help = subcommands.get("help");
            if (help != null) {
                if (!help.canAccess(sender, permissions)) {
                    commandMessages.sendNoPermission(sender);
                    return;
                }
                help.execute(sender, new String[0]);
            }
            return;
        }

        String key = args[0].toLowerCase(Locale.ROOT);
        FaunaSubcommand subcommand = subcommands.get(key);
        if (subcommand == null) {
            commandMessages.sendUnknownCommand(sender);
            return;
        }

        if (!subcommand.canAccess(sender, permissions)) {
            commandMessages.sendNoPermission(sender);
            return;
        }

        String[] tail = new String[Math.max(0, args.length - 1)];
        if (tail.length > 0) {
            System.arraycopy(args, 1, tail, 0, tail.length);
        }
        subcommand.execute(sender, tail);
    }

    public List<String> suggest(CommandSender sender, String[] args) {
        if (args.length == 1) {
            String token = args[0].toLowerCase(Locale.ROOT);
            List<String> suggestions = new ArrayList<>();
            for (FaunaSubcommand subcommand : subcommands.values()) {
                if (!subcommand.info().name().startsWith(token)) {
                    continue;
                }
                if (!subcommand.canAccess(sender, permissions)) {
                    continue;
                }
                suggestions.add(subcommand.info().name());
            }
            return suggestions;
        }

        if (args.length > 1) {
            FaunaSubcommand subcommand = subcommands.get(args[0].toLowerCase(Locale.ROOT));
            if (subcommand == null || !subcommand.canAccess(sender, permissions)) {
                return List.of();
            }
            String[] tail = new String[args.length - 1];
            System.arraycopy(args, 1, tail, 0, tail.length);
            return subcommand.suggest(sender, tail);
        }

        return List.of();
    }

    public List<FaunaSubcommand> visibleCommands(CommandSender sender, boolean includeAdminCommands) {
        List<FaunaSubcommand> visible = new ArrayList<>();
        for (FaunaSubcommand subcommand : subcommands.values()) {
            if (subcommand.info().administrative() && !includeAdminCommands) {
                continue;
            }
            if (!subcommand.canAccess(sender, permissions)) {
                continue;
            }
            visible.add(subcommand);
        }
        return visible;
    }

    public static List<FaunaSubcommand> defaults(
            HelpCommand help,
            VersionCommand version,
            AboutCommand about,
            EntitiesCommand entities,
            ReloadCommand reload,
            GuiCommand gui,
            LangCommand lang
    ) {
        return List.of(help, version, about, entities, reload, gui, lang);
    }
}
