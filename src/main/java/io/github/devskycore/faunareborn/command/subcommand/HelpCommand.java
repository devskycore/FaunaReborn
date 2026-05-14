package io.github.devskycore.faunareborn.command.subcommand;

import io.github.devskycore.faunareborn.command.permission.PermissionConstants;
import io.github.devskycore.faunareborn.command.permission.PermissionService;
import io.github.devskycore.faunareborn.command.message.CommandMessages;
import io.github.devskycore.faunareborn.lang.LanguageManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;

public final class HelpCommand implements FaunaSubcommand {

    private static final CommandInfo INFO = new CommandInfo(
            "help",
            "/fauna help [page]",
            "Show available commands.",
            PermissionConstants.COMMAND_HELP,
            false
    );
    private static final int PAGE_SIZE = 4;

    private final PermissionService permissions;
    private final CommandMessages commandMessages;
    private final LanguageManager language;
    private CommandRegistry registry;

    public HelpCommand(PermissionService permissions, CommandMessages commandMessages, LanguageManager language) {
        this.permissions = permissions;
        this.commandMessages = commandMessages;
        this.language = language;
    }

    public void bindRegistry(CommandRegistry registry) {
        this.registry = registry;
    }

    @Override
    public CommandInfo info() {
        return INFO;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (registry == null) {
            return;
        }

        boolean includeAdmin = permissions.canViewAdminHelp(sender);
        List<FaunaSubcommand> visible = registry.visibleCommands(sender, includeAdmin);
        if (visible.isEmpty()) {
            sender.sendMessage(commandMessages.prefix()
                    .append(Component.text(language.text("commands.help.none-available", "No commands are available to you."), NamedTextColor.RED)));
            return;
        }

        int requestedPage = 1;
        if (args.length > 0) {
            requestedPage = parsePage(args[0]);
            if (requestedPage < 1) {
                sender.sendMessage(commandMessages.prefix()
                        .append(Component.text(language.text("commands.help.usage", "Usage: /fauna help [page]"), NamedTextColor.RED)));
                return;
            }
        }

        int totalPages = Math.max(1, (int) Math.ceil((double) visible.size() / PAGE_SIZE));
        if (requestedPage > totalPages) {
            sender.sendMessage(commandMessages.prefix()
                    .append(Component.text(
                            language.text("commands.help.page-out-of-range", "Page out of range. Available pages: 1-{totalPages}.")
                                    .replace("{totalPages}", String.valueOf(totalPages)),
                            NamedTextColor.RED
                    )));
            return;
        }

        int from = (requestedPage - 1) * PAGE_SIZE;
        int to = Math.min(from + PAGE_SIZE, visible.size());
        List<FaunaSubcommand> pageItems = visible.subList(from, to);

        sender.sendMessage(commandMessages.prefix()
                .append(Component.text(
                        language.text("commands.help.header", "Commands ({page}/{totalPages})")
                                .replace("{page}", String.valueOf(requestedPage))
                                .replace("{totalPages}", String.valueOf(totalPages)),
                        NamedTextColor.GREEN
                )));
        for (FaunaSubcommand subcommand : pageItems) {
            String commandName = subcommand.info().name();
            String usage = language.text("commands.meta." + commandName + ".usage", subcommand.info().usage());
            String description = language.text("commands.meta." + commandName + ".description", subcommand.info().description());
            sender.sendMessage(Component.text(" - ", NamedTextColor.DARK_GRAY)
                    .append(Component.text(usage, NamedTextColor.AQUA))
                    .append(Component.text("  " + description, NamedTextColor.GRAY)));
        }
        if (requestedPage < totalPages) {
            sender.sendMessage(commandMessages.prefix()
                    .append(Component.text(
                            language.text("commands.help.next-page-tip", "Tip: /fauna help {nextPage}")
                                    .replace("{nextPage}", String.valueOf(requestedPage + 1)),
                            NamedTextColor.GRAY
                    )));
        }
    }

    @Override
    public List<String> suggest(CommandSender sender, String[] args) {
        if (args.length == 1) {
            if (registry == null) {
                return List.of("1");
            }
            boolean includeAdmin = permissions.canViewAdminHelp(sender);
            int totalCommands = registry.visibleCommands(sender, includeAdmin).size();
            int totalPages = Math.max(1, (int) Math.ceil((double) totalCommands / PAGE_SIZE));
            List<String> pages = new ArrayList<>();
            for (int i = 1; i <= totalPages; i++) {
                pages.add(String.valueOf(i));
            }
            return pages;
        }
        return List.of();
    }

    private int parsePage(String raw) {
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }
}
