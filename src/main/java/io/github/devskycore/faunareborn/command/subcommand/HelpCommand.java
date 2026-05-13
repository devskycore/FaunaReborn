package io.github.devskycore.faunareborn.command.subcommand;

import io.github.devskycore.faunareborn.command.permission.PermissionConstants;
import io.github.devskycore.faunareborn.command.permission.PermissionService;
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
    private CommandRegistry registry;

    public HelpCommand(PermissionService permissions) {
        this.permissions = permissions;
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
            sender.sendMessage(io.github.devskycore.faunareborn.command.message.CommandMessages.prefix()
                    .append(net.kyori.adventure.text.Component.text("No commands are available to you.", net.kyori.adventure.text.format.NamedTextColor.RED)));
            return;
        }

        int requestedPage = 1;
        if (args.length > 0) {
            requestedPage = parsePage(args[0]);
            if (requestedPage < 1) {
                sender.sendMessage(io.github.devskycore.faunareborn.command.message.CommandMessages.prefix()
                        .append(net.kyori.adventure.text.Component.text("Usage: /fauna help [page]", net.kyori.adventure.text.format.NamedTextColor.RED)));
                return;
            }
        }

        int totalPages = Math.max(1, (int) Math.ceil((double) visible.size() / PAGE_SIZE));
        if (requestedPage > totalPages) {
            sender.sendMessage(io.github.devskycore.faunareborn.command.message.CommandMessages.prefix()
                    .append(net.kyori.adventure.text.Component.text("Page out of range. Available pages: 1-" + totalPages + ".", net.kyori.adventure.text.format.NamedTextColor.RED)));
            return;
        }

        int from = (requestedPage - 1) * PAGE_SIZE;
        int to = Math.min(from + PAGE_SIZE, visible.size());
        List<FaunaSubcommand> pageItems = visible.subList(from, to);

        sender.sendMessage(io.github.devskycore.faunareborn.command.message.CommandMessages.prefix()
                .append(net.kyori.adventure.text.Component.text("Commands (" + requestedPage + "/" + totalPages + ")", net.kyori.adventure.text.format.NamedTextColor.GREEN)));
        for (FaunaSubcommand subcommand : pageItems) {
            sender.sendMessage(net.kyori.adventure.text.Component.text(" - ", net.kyori.adventure.text.format.NamedTextColor.DARK_GRAY)
                    .append(net.kyori.adventure.text.Component.text(subcommand.info().usage(), net.kyori.adventure.text.format.NamedTextColor.AQUA))
                    .append(net.kyori.adventure.text.Component.text("  " + subcommand.info().description(), net.kyori.adventure.text.format.NamedTextColor.GRAY)));
        }
        if (requestedPage < totalPages) {
            sender.sendMessage(io.github.devskycore.faunareborn.command.message.CommandMessages.prefix()
                    .append(net.kyori.adventure.text.Component.text("Tip: /fauna help " + (requestedPage + 1), net.kyori.adventure.text.format.NamedTextColor.GRAY)));
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
