package io.github.devskycore.faunareborn.command.subcommand;

import io.github.devskycore.faunareborn.command.permission.PermissionConstants;
import io.github.devskycore.faunareborn.command.permission.PermissionService;
import io.github.devskycore.faunareborn.command.message.CommandMessages;
import io.github.devskycore.faunareborn.lang.LanguageManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class HelpCommand implements FaunaSubcommand {

    private static final CommandInfo INFO = new CommandInfo(
            "help",
            "/fauna help [page|admin|permissions|query]",
            "Show help pages, admin commands, or command search results.",
            PermissionConstants.COMMAND_HELP,
            false,
            List.of("h", "?")
    );
    private static final int PAGE_SIZE = 4;
    private static final String ADMIN_MODE = "admin";
    private static final String PERMISSIONS_MODE = "permissions";
    private static final String NAVIGATION_TOKEN = "__nav";
    private static final Set<String> SAFE_RUN_COMMANDS = Set.of("about", "entities", "version");
    private static final Map<String, Integer> HELP_ORDER = Map.of(
            "reload", 0,
            "gui", 1,
            "about", 2,
            "version", 3,
            "entities", 4,
            "help", 5,
            "lang", 6
    );

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
        HelpRequest request = parseRequest(args);
        if (!request.valid()) {
            sender.sendMessage(commandMessages.prefix()
                    .append(Component.text(language.text("commands.help.usage", "Usage: /fauna help [page|admin|permissions|query]"), NamedTextColor.RED)));
            return;
        }
        if (request.adminOnly() && !includeAdmin) {
            commandMessages.sendNoPermission(sender);
            return;
        }
        playNavigationFeedbackIfNeeded(sender, request);

        List<FaunaSubcommand> visible = filterVisibleCommands(sender, includeAdmin, request);
        if (visible.isEmpty()) {
            sender.sendMessage(commandMessages.prefix()
                    .append(Component.text(language.text("commands.help.none-available", "No commands are available to you."), NamedTextColor.RED)));
            return;
        }

        int requestedPage = request.paginated() ? request.page() : 1;

        int totalPages = request.paginated() ? Math.max(1, (int) Math.ceil((double) visible.size() / PAGE_SIZE)) : 1;
        if (request.paginated() && requestedPage > totalPages) {
            sender.sendMessage(commandMessages.prefix()
                    .append(Component.text(
                            language.text("commands.help.page-out-of-range", "Page out of range. Available pages: 1-{totalPages}.")
                                    .replace("{totalPages}", String.valueOf(totalPages)),
                            NamedTextColor.RED
                    )));
            sender.sendMessage(commandMessages.prefix()
                    .append(Component.text(language.textAny("Use ", "commands.help.out-of-range-use"), NamedTextColor.GRAY))
                    .append(Component.text(request.commandForPage(1), NamedTextColor.AQUA))
                    .append(Component.text(language.textAny(" or ", "commands.help.out-of-range-or"), NamedTextColor.GRAY))
                    .append(Component.text(request.commandForPage(totalPages), NamedTextColor.AQUA)));
            return;
        }

        int from = request.paginated() ? (requestedPage - 1) * PAGE_SIZE : 0;
        int to = request.paginated() ? Math.min(from + PAGE_SIZE, visible.size()) : visible.size();
        List<FaunaSubcommand> pageItems = visible.subList(from, to);

        sender.sendMessage(helpHeader(requestedPage, totalPages, request));
        sender.sendMessage(Component.empty());
        for (FaunaSubcommand subcommand : pageItems) {
            String commandName = subcommand.info().name();
            String usage = language.text("commands.meta." + commandName + ".usage", subcommand.info().usage());
            String description = language.text("commands.meta." + commandName + ".description", subcommand.info().description());
            String suggestedCommand = clickableCommandFromUsage(usage);
            boolean runOnClick = isRunOnClickCommand(commandName);
            String hoverText = language.textAny(
                    runOnClick ? "Click to run: {command}" : "Click to suggest: {command}",
                    runOnClick ? "commands.help.click-to-run" : "commands.help.click-to-suggest",
                    "commands.help.click-to-use"
            ).replace("{command}", suggestedCommand);
            Component line = Component.text("\u27A4 ", NamedTextColor.DARK_AQUA)
                    .append(formatUsage(usage))
                    .append(Component.text(" - ", NamedTextColor.DARK_GRAY))
                    .append(Component.text(description, NamedTextColor.GRAY))
                    .clickEvent(runOnClick ? ClickEvent.runCommand(suggestedCommand) : ClickEvent.suggestCommand(suggestedCommand))
                    .hoverEvent(HoverEvent.showText(coloredHoverText(hoverText, runOnClick)));
            if (request.permissionsMode()) {
                line = line.append(Component.text("  [", NamedTextColor.DARK_GRAY))
                        .append(Component.text(subcommand.info().permission(), NamedTextColor.YELLOW))
                        .append(Component.text("]", NamedTextColor.DARK_GRAY));
            }
            sender.sendMessage(line);
        }
        sender.sendMessage(Component.empty());
        if (request.paginated()) {
            sender.sendMessage(navigationRow(sender, requestedPage, totalPages, request));
        }
    }

    @Override
    public List<String> suggest(CommandSender sender, String[] args) {
        if (args.length == 1) {
            if (registry == null) {
                return List.of("1", ADMIN_MODE, PERMISSIONS_MODE);
            }
            boolean includeAdmin = permissions.canViewAdminHelp(sender);
            String token = args[0].toLowerCase(Locale.ROOT);
            List<String> suggestions = new ArrayList<>();
            suggestions.addAll(pageSuggestions(registry.visibleCommands(sender, includeAdmin).size(), token));
            if (ADMIN_MODE.startsWith(token) && includeAdmin) {
                suggestions.add(ADMIN_MODE);
            }
            if (PERMISSIONS_MODE.startsWith(token)) {
                suggestions.add(PERMISSIONS_MODE);
            }
            for (FaunaSubcommand subcommand : registry.visibleCommands(sender, includeAdmin)) {
                String name = subcommand.info().name();
                if (name.startsWith(token)) {
                    suggestions.add(name);
                }
            }
            suggestions.sort(Comparator.naturalOrder());
            return suggestions;
        }
        if (args.length == 2 && registry != null) {
            String mode = args[0].toLowerCase(Locale.ROOT);
            String token = args[1].toLowerCase(Locale.ROOT);
            boolean includeAdmin = permissions.canViewAdminHelp(sender);
            if (ADMIN_MODE.equals(mode) || PERMISSIONS_MODE.equals(mode)) {
                List<FaunaSubcommand> commands = registry.visibleCommands(sender, includeAdmin);
                if (ADMIN_MODE.equals(mode)) {
                    commands = commands.stream().filter(c -> c.info().administrative()).toList();
                }
                return pageSuggestions(commands.size(), token);
            }
            HelpRequest request = parseRequest(args);
            if (request.valid() && request.paginated()) {
                List<FaunaSubcommand> filtered = filterVisibleCommands(sender, includeAdmin, request);
                return pageSuggestions(filtered.size(), token);
            }
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

    private List<String> pageSuggestions(int totalCommands, String token) {
        int totalPages = Math.max(1, (int) Math.ceil((double) totalCommands / PAGE_SIZE));
        List<String> pages = new ArrayList<>();
        for (int i = 1; i <= totalPages; i++) {
            String page = String.valueOf(i);
            if (page.startsWith(token)) {
                pages.add(page);
            }
        }
        return pages;
    }

    private List<FaunaSubcommand> filterVisibleCommands(CommandSender sender, boolean includeAdmin, HelpRequest request) {
        List<FaunaSubcommand> visible = registry.visibleCommands(sender, includeAdmin);
        if (request.adminOnly()) {
            visible = visible.stream().filter(command -> command.info().administrative()).toList();
        }
        if (request.query() != null) {
            String query = request.query().toLowerCase(Locale.ROOT);
            List<FaunaSubcommand> exactMatches = visible.stream()
                    .filter(command -> command.info().name().equalsIgnoreCase(query))
                    .toList();
            if (!exactMatches.isEmpty()) {
                return orderForHelpDisplay(exactMatches);
            }
            visible = visible.stream()
                    .filter(command -> command.info().name().toLowerCase(Locale.ROOT).contains(query))
                    .toList();
        }
        return orderForHelpDisplay(visible);
    }

    private HelpRequest parseRequest(String[] args) {
        if (args.length == 0) {
            return new HelpRequest(1, false, false, null, true, false);
        }
        if (args.length > 3) {
            return invalidRequest();
        }

        boolean fromNavigation = hasNavigationToken(args);
        int effectiveLength = fromNavigation ? args.length - 1 : args.length;
        if (effectiveLength <= 0) {
            return invalidRequest();
        }

        String first = args[0].toLowerCase(Locale.ROOT);
        if (ADMIN_MODE.equals(first)) {
            int page = effectiveLength > 1 ? parsePage(args[1]) : 1;
            return page < 1 ? invalidRequest() : new HelpRequest(page, true, false, null, true, fromNavigation);
        }
        if (PERMISSIONS_MODE.equals(first)) {
            int page = effectiveLength > 1 ? parsePage(args[1]) : 1;
            return page < 1 ? invalidRequest() : new HelpRequest(page, false, true, null, true, fromNavigation);
        }

        int page = parsePage(first);
        if (page >= 1) {
            if (effectiveLength > 1) {
                return invalidRequest();
            }
            return new HelpRequest(page, false, false, null, true, fromNavigation);
        }

        if (effectiveLength > 1) {
            return invalidRequest();
        }
        return new HelpRequest(1, false, false, first, true, fromNavigation);
    }

    private HelpRequest invalidRequest() {
        return new HelpRequest(-1, false, false, null, false, false);
    }

    private Component helpHeader(int currentPage, int totalPages, HelpRequest request) {
        String pluginName = language.text("commands.common.prefix", "FaunaReborn");
        if (!request.paginated()) {
            String searchHeaderTemplate = language.textAny("Command: {query}", "commands.help.header-search");
            String searchHeader = normalizeSearchHeaderTemplate(searchHeaderTemplate)
                    .replace("{query}", request.query());
            return Component.text(pluginName, NamedTextColor.WHITE)
                    .decorate(TextDecoration.BOLD)
                    .append(Component.text(" ", NamedTextColor.GRAY).decoration(TextDecoration.BOLD, false))
                    .append(Component.text("\u00B7", NamedTextColor.DARK_GRAY).decoration(TextDecoration.BOLD, false))
                    .append(Component.text(" ", NamedTextColor.GRAY).decoration(TextDecoration.BOLD, false))
                    .append(Component.text(searchHeader, NamedTextColor.GREEN))
                    .decoration(TextDecoration.ITALIC, false);
        }
        String title = language.textAny("Commands", "commands.help.header-title-default", "commands.help.header-default", "commands.help.header");
        if (request.adminOnly()) {
            title = language.textAny("Commands: Admin", "commands.help.header-title-admin", "commands.help.header-admin");
        } else if (request.permissionsMode()) {
            title = language.textAny("Commands: Permissions", "commands.help.header-title-permissions", "commands.help.header-permissions");
        }
        title = normalizePageHeaderTemplate(title)
                .replace("{page}", String.valueOf(currentPage))
                .replace("{totalPages}", String.valueOf(totalPages))
                .trim();
        return Component.text(pluginName, NamedTextColor.WHITE)
                .decorate(TextDecoration.BOLD)
                .append(Component.text(" ", NamedTextColor.GRAY).decoration(TextDecoration.BOLD, false))
                .append(Component.text("\u00B7", NamedTextColor.DARK_GRAY).decoration(TextDecoration.BOLD, false))
                .append(Component.text(" ", NamedTextColor.GRAY).decoration(TextDecoration.BOLD, false))
                .append(Component.text(title, NamedTextColor.GREEN))
                .append(Component.text(" ", NamedTextColor.GRAY).decoration(TextDecoration.BOLD, false))
                .append(Component.text("(", NamedTextColor.GRAY).decoration(TextDecoration.BOLD, false))
                .append(Component.text(String.valueOf(currentPage), NamedTextColor.GREEN).decoration(TextDecoration.BOLD, false))
                .append(Component.text("/", NamedTextColor.GRAY).decoration(TextDecoration.BOLD, false))
                .append(Component.text(String.valueOf(totalPages), NamedTextColor.WHITE).decoration(TextDecoration.BOLD, false))
                .append(Component.text(")", NamedTextColor.GRAY).decoration(TextDecoration.BOLD, false))
                .decoration(TextDecoration.ITALIC, false);
    }

    private Component formatUsage(String usage) {
        Component result = Component.empty();
        StringBuilder token = new StringBuilder();
        for (char c : usage.toCharArray()) {
            if (c == ' ') {
                if (!token.isEmpty()) {
                    result = result.append(usageToken(token.toString()));
                    token.setLength(0);
                }
                result = result.append(Component.text(" ", NamedTextColor.GRAY));
                continue;
            }
            token.append(c);
        }
        if (!token.isEmpty()) {
            result = result.append(usageToken(token.toString()));
        }
        return result;
    }

    private Component usageToken(String token) {
        if (token.startsWith("[") && token.endsWith("]")) {
            return Component.text(token, NamedTextColor.WHITE);
        }
        if (token.startsWith("<") && token.endsWith(">")) {
            return Component.text(token, NamedTextColor.YELLOW);
        }
        return Component.text(token, NamedTextColor.AQUA);
    }

    private String clickableCommandFromUsage(String usage) {
        String[] tokens = usage.trim().split("\\s+");
        List<String> baseTokens = new ArrayList<>();
        for (String token : tokens) {
            if (token.startsWith("[") || token.startsWith("<")) {
                break;
            }
            baseTokens.add(token);
        }
        if (baseTokens.isEmpty()) {
            return usage.trim();
        }
        return String.join(" ", baseTokens);
    }

    private Component navigationRow(CommandSender sender, int currentPage, int totalPages, HelpRequest request) {
        if (!(sender instanceof Player)) {
            return consoleNavigationRow(currentPage, totalPages, request);
        }
        String prevLabel = language.textAny("\u2190 PREV", "commands.help.nav-prev-label");
        String nextLabel = language.textAny("NEXT \u2192", "commands.help.nav-next-label");
        Component row = Component.empty();
        if (currentPage > 1) {
            int prev = currentPage - 1;
            row = row.append(Component.text(prevLabel, NamedTextColor.YELLOW)
                    .decorate(TextDecoration.BOLD)
                    .clickEvent(ClickEvent.runCommand(request.commandForPage(prev, true)))
                    .hoverEvent(HoverEvent.showText(Component.text(
                            language.text("commands.help.nav-go-to-page", "Go to page {page}")
                                    .replace("{page}", String.valueOf(prev)),
                            NamedTextColor.GRAY
                    ))));
        } else {
            row = row.append(Component.text(prevLabel, NamedTextColor.DARK_GRAY).decorate(TextDecoration.BOLD));
        }
        row = row.append(Component.text("     |     ", NamedTextColor.GRAY));
        if (currentPage < totalPages) {
            int next = currentPage + 1;
            row = row.append(Component.text(nextLabel, NamedTextColor.YELLOW)
                    .decorate(TextDecoration.BOLD)
                    .clickEvent(ClickEvent.runCommand(request.commandForPage(next, true)))
                    .hoverEvent(HoverEvent.showText(Component.text(
                            language.text("commands.help.nav-go-to-page", "Go to page {page}")
                                    .replace("{page}", String.valueOf(next)),
                            NamedTextColor.GRAY
                    ))));
        } else {
            row = row.append(Component.text(nextLabel, NamedTextColor.DARK_GRAY).decorate(TextDecoration.BOLD));
        }
        return row;
    }

    private Component consoleNavigationRow(int currentPage, int totalPages, HelpRequest request) {
        Component row = Component.empty();
        boolean hasPrev = currentPage > 1;
        boolean hasNext = currentPage < totalPages;
        String prevLabel = language.textAny("PREV", "commands.help.nav-prev-label");
        String nextLabel = language.textAny("NEXT", "commands.help.nav-next-label");

        if (hasPrev) {
            row = row.append(Component.text(prevLabel + ": ", NamedTextColor.GRAY))
                    .append(Component.text(request.commandForPage(currentPage - 1), NamedTextColor.AQUA));
        }
        if (hasPrev && hasNext) {
            row = row.append(Component.text(" | ", NamedTextColor.GRAY));
        }
        if (hasNext) {
            row = row.append(Component.text(nextLabel + ": ", NamedTextColor.GRAY))
                    .append(Component.text(request.commandForPage(currentPage + 1), NamedTextColor.AQUA));
        }
        return row;
    }

    private boolean isRunOnClickCommand(String commandName) {
        return SAFE_RUN_COMMANDS.contains(commandName.toLowerCase(Locale.ROOT));
    }

    private Component coloredHoverText(String hoverText, boolean runOnClick) {
        int separatorIndex = hoverText.indexOf(':');
        if (separatorIndex <= 0) {
            return Component.text(hoverText, NamedTextColor.GRAY);
        }
        NamedTextColor actionColor = runOnClick ? NamedTextColor.GREEN : NamedTextColor.YELLOW;
        String actionLabel = hoverText.substring(0, separatorIndex);
        String remainder = hoverText.substring(separatorIndex);
        return Component.text(actionLabel, actionColor)
                .append(Component.text(remainder, NamedTextColor.GRAY));
    }

    private List<FaunaSubcommand> orderForHelpDisplay(List<FaunaSubcommand> commands) {
        return commands.stream()
                .sorted(Comparator
                        .comparingInt((FaunaSubcommand command) -> HELP_ORDER.getOrDefault(command.info().name(), Integer.MAX_VALUE))
                        .thenComparing(command -> command.info().name()))
                .toList();
    }

    private String normalizeSearchHeaderTemplate(String template) {
        String normalized = template.replace(" ({page}/{totalPages})", "");
        normalized = normalized.replace("({page}/{totalPages})", "");
        return normalized.trim();
    }

    private String normalizePageHeaderTemplate(String template) {
        int open = template.lastIndexOf('(');
        int close = template.lastIndexOf(')');
        if (open >= 0 && close > open) {
            String inside = template.substring(open + 1, close);
            if (inside.contains("{page}") && inside.contains("{totalPages}")) {
                return template.substring(0, open).trim();
            }
        }
        return template;
    }

    private boolean hasNavigationToken(String[] args) {
        if (args.length == 0) {
            return false;
        }
        return NAVIGATION_TOKEN.equalsIgnoreCase(args[args.length - 1]);
    }

    private void playNavigationFeedbackIfNeeded(CommandSender sender, HelpRequest request) {
        if (!request.fromNavigation() || !(sender instanceof Player player)) {
            return;
        }
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.55f, 1.15f);
    }

    private record HelpRequest(int page, boolean adminOnly, boolean permissionsMode, String query, boolean valid, boolean fromNavigation) {
        boolean paginated() {
            return query == null;
        }

        String commandForPage(int targetPage) {
            return commandForPage(targetPage, false);
        }

        String commandForPage(int targetPage, boolean navigationClick) {
            String suffix = navigationClick ? " " + NAVIGATION_TOKEN : "";
            if (adminOnly) {
                return "/fauna help " + ADMIN_MODE + " " + targetPage + suffix;
            }
            if (permissionsMode) {
                return "/fauna help " + PERMISSIONS_MODE + " " + targetPage + suffix;
            }
            if (query != null) {
                return "/fauna help " + query + " " + targetPage + suffix;
            }
            return "/fauna help " + targetPage + suffix;
        }
    }
}
