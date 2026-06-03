package io.github.devskycore.faunareborn.command.subcommand;

import io.github.devskycore.faunareborn.command.message.CommandMessages;
import io.github.devskycore.faunareborn.command.permission.PermissionService;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class CommandRegistry {
    private static final int MAX_SUGGESTIONS = 3;

    private final Map<String, FaunaSubcommand> subcommands;
    private final Map<String, FaunaSubcommand> aliases;
    private final PermissionService permissions;
    private final CommandMessages commandMessages;
    private final SuggestionCache fuzzySuggestionCache = new SuggestionCache(256);

    public CommandRegistry(List<FaunaSubcommand> commandList, PermissionService permissions, CommandMessages commandMessages) {
        this.permissions = permissions;
        this.commandMessages = commandMessages;
        Map<String, FaunaSubcommand> byName = new LinkedHashMap<>();
        Map<String, FaunaSubcommand> byAlias = new LinkedHashMap<>();
        for (FaunaSubcommand subcommand : commandList) {
            String canonicalName = subcommand.info().name().toLowerCase(Locale.ROOT);
            byName.put(canonicalName, subcommand);
            for (String rawAlias : subcommand.info().aliases()) {
                String alias = rawAlias.toLowerCase(Locale.ROOT);
                if (alias.equals(canonicalName) || byName.containsKey(alias) || byAlias.containsKey(alias)) {
                    throw new IllegalArgumentException("Duplicated command alias: " + rawAlias);
                }
                byAlias.put(alias, subcommand);
            }
        }
        this.subcommands = Map.copyOf(byName);
        this.aliases = Map.copyOf(byAlias);
    }

    public void execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            FaunaSubcommand help = subcommands.get("help");
            if (help != null) {
                if (help.cannotAccess(sender, permissions)) {
                    commandMessages.sendNoPermission(sender);
                    return;
                }
                help.execute(sender, new String[0]);
            }
            return;
        }

        String key = args[0].toLowerCase(Locale.ROOT);
        FaunaSubcommand subcommand = resolveSubcommand(key);
        if (subcommand == null) {
            commandMessages.sendUnknownCommand(sender, key, suggestClosest(sender, key));
            return;
        }

        if (subcommand.cannotAccess(sender, permissions)) {
            commandMessages.sendNoPermission(sender);
            return;
        }

        String[] tail = new String[Math.max(0, args.length - 1)];
        if (tail.length > 0) {
            System.arraycopy(args, 1, tail, 0, tail.length);
        }
        subcommand.execute(sender, tail);
    }

    private List<String> suggestClosest(CommandSender sender, String token) {
        List<String> cachedCandidates = fuzzySuggestionCache.getOrCompute(token, this::computeClosestNames);
        List<String> filtered = new ArrayList<>();
        for (String candidate : cachedCandidates) {
            FaunaSubcommand subcommand = resolveSubcommand(candidate);
            if (subcommand == null || subcommand.cannotAccess(sender, permissions)) {
                continue;
            }
            filtered.add(candidate);
            if (filtered.size() == MAX_SUGGESTIONS) {
                break;
            }
        }
        return filtered;
    }

    private List<String> computeClosestNames(String token) {
        List<ScoredSuggestion> scored = new ArrayList<>();
        for (FaunaSubcommand subcommand : subcommands.values()) {
            String canonicalName = subcommand.info().name().toLowerCase(Locale.ROOT);
            Set<String> tokens = new LinkedHashSet<>();
            tokens.add(canonicalName);
            for (String alias : subcommand.info().aliases()) {
                tokens.add(alias.toLowerCase(Locale.ROOT));
            }

            double bestScore = Double.POSITIVE_INFINITY;
            for (String candidate : tokens) {
                int distance = levenshtein(token, candidate);
                double normalizedDistance = candidate.isEmpty() ? 1.0 : (double) distance / (double) candidate.length();
                boolean startsWith = candidate.startsWith(token) || token.startsWith(candidate);
                boolean contains = candidate.contains(token) || token.contains(candidate);
                if (!startsWith && !contains && distance > 2 && normalizedDistance > 0.45D) {
                    continue;
                }
                double score = startsWith ? 0.0D : (contains ? 0.1D : normalizedDistance);
                if (score < bestScore) {
                    bestScore = score;
                }
            }
            if (bestScore != Double.POSITIVE_INFINITY) {
                scored.add(new ScoredSuggestion(canonicalName, bestScore));
            }
        }

        scored.sort(Comparator.comparingDouble(ScoredSuggestion::score).thenComparing(ScoredSuggestion::command));
        List<String> results = new ArrayList<>();
        for (ScoredSuggestion entry : scored) {
            results.add(entry.command());
            if (results.size() == MAX_SUGGESTIONS) {
                break;
            }
        }
        return results;
    }

    private static int levenshtein(String a, String b) {
        if (a.equals(b)) {
            return 0;
        }
        if (a.isEmpty()) {
            return b.length();
        }
        if (b.isEmpty()) {
            return a.length();
        }

        int[] prev = new int[b.length() + 1];
        int[] curr = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) {
            prev[j] = j;
        }

        for (int i = 1; i <= a.length(); i++) {
            curr[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                curr[j] = Math.min(
                        Math.min(curr[j - 1] + 1, prev[j] + 1),
                        prev[j - 1] + cost
                );
            }
            int[] tmp = prev;
            prev = curr;
            curr = tmp;
        }
        return prev[b.length()];
    }

    private record ScoredSuggestion(String command, double score) {
    }

    private static final class SuggestionCache {
        private final int maxEntries;
        private final Map<String, List<String>> cache;

        private SuggestionCache(int maxEntries) {
            this.maxEntries = maxEntries;
            this.cache = new LinkedHashMap<>(maxEntries, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, List<String>> eldest) {
                    return size() > SuggestionCache.this.maxEntries;
                }
            };
        }

        private synchronized List<String> getOrCompute(String token, java.util.function.Function<String, List<String>> computer) {
            String key = token.toLowerCase(Locale.ROOT);
            List<String> value = cache.get(key);
            if (value != null) {
                return value;
            }
            List<String> computed = List.copyOf(computer.apply(key));
            cache.put(key, computed);
            return computed;
        }
    }

    public List<String> suggest(CommandSender sender, String[] args) {
        if (args.length == 1) {
            String token = args[0].toLowerCase(Locale.ROOT);
            List<String> suggestions = new ArrayList<>();
            for (FaunaSubcommand subcommand : subcommands.values()) {
                if (!subcommand.info().name().startsWith(token)) {
                    continue;
                }
                if (subcommand.cannotAccess(sender, permissions)) {
                    continue;
                }
                suggestions.add(subcommand.info().name());
            }
            for (Map.Entry<String, FaunaSubcommand> entry : aliases.entrySet()) {
                String alias = entry.getKey();
                FaunaSubcommand subcommand = entry.getValue();
                if (!alias.startsWith(token)) {
                    continue;
                }
                if (subcommand.cannotAccess(sender, permissions)) {
                    continue;
                }
                suggestions.add(alias);
            }
            return suggestions;
        }

        if (args.length > 1) {
            FaunaSubcommand subcommand = resolveSubcommand(args[0].toLowerCase(Locale.ROOT));
            if (subcommand == null || subcommand.cannotAccess(sender, permissions)) {
                return List.of();
            }
            String[] tail = new String[args.length - 1];
            System.arraycopy(args, 1, tail, 0, tail.length);
            return subcommand.suggest(sender, tail);
        }

        return List.of();
    }

    private FaunaSubcommand resolveSubcommand(String key) {
        FaunaSubcommand byName = subcommands.get(key);
        if (byName != null) {
            return byName;
        }
        return aliases.get(key);
    }

    public List<FaunaSubcommand> visibleCommands(CommandSender sender, boolean includeAdminCommands) {
        List<FaunaSubcommand> visible = new ArrayList<>();
        for (FaunaSubcommand subcommand : subcommands.values()) {
            if (subcommand.info().administrative() && !includeAdminCommands) {
                continue;
            }
            if (subcommand.cannotAccess(sender, permissions)) {
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
