package io.github.devskycore.faunareborn.command.subcommand;

import io.github.devskycore.faunareborn.command.permission.PermissionConstants;
import io.github.devskycore.faunareborn.command.permission.PermissionService;
import io.github.devskycore.faunareborn.gui.FaunaMainGui;
import io.github.devskycore.faunareborn.lang.LanguageManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class LangCommand implements FaunaSubcommand {

    private static final CommandInfo INFO = new CommandInfo(
            "lang",
            "/fauna lang [language]",
            "Open the language selector or change language.",
            PermissionConstants.COMMAND_LANG,
            true,
            List.of("language")
    );

    private final LanguageManager language;
    private final FaunaMainGui mainGui;

    public LangCommand(LanguageManager language, FaunaMainGui mainGui) {
        this.language = language;
        this.mainGui = mainGui;
    }

    @Override
    public CommandInfo info() {
        return INFO;
    }

    @Override
    public boolean cannotAccess(CommandSender sender, PermissionService permissions) {
        return !permissions.canUseLang(sender);
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 1 || args[0].isBlank()) {
            if (sender instanceof Player player) {
                mainGui.openLanguageSelector(player);
                return;
            }
            sender.sendMessage(Component.text(
                    language.text("commands.lang.usage", "Usage: /fauna lang [language]"),
                    NamedTextColor.RED
            ));
            sender.sendMessage(Component.text(
                    language.text(
                            "commands.lang.available",
                            "Available languages: {languages}",
                            Map.of("languages", String.join(", ", language.availableLanguageCodes()))
                    ),
                    NamedTextColor.GRAY
            ));
            return;
        }

        String requested = args[0].trim().toLowerCase(Locale.ROOT);
        boolean switched = language.switchLanguage(requested);
        if (!switched) {
            sender.sendMessage(Component.text(
                    language.text(
                            "commands.lang.invalid",
                            "Language '{language}' not found. Available: {languages}",
                            Map.of(
                                    "language", requested,
                                    "languages", String.join(", ", language.availableLanguageCodes())
                            )
                    ),
                    NamedTextColor.RED
            ));
            List<String> suggestions = suggestClosestLanguages(requested);
            if (suggestions.size() == 1) {
                String suggestion = suggestions.get(0);
                String suggestedCommand = "/fauna lang " + suggestion;
                sender.sendMessage(Component.text(
                                language.text(
                                        "commands.lang.did-you-mean",
                                        "Did you mean /fauna lang {language}?",
                                        Map.of("language", suggestion)
                                ),
                                NamedTextColor.YELLOW
                        )
                        .clickEvent(ClickEvent.suggestCommand(suggestedCommand))
                        .hoverEvent(HoverEvent.showText(Component.text(
                                language.textAny("Click to use: {command}", "commands.help.click-to-use")
                                        .replace("{command}", suggestedCommand),
                                NamedTextColor.GRAY
                        ))));
            } else if (!suggestions.isEmpty()) {
                Component line = Component.text(
                        language.text("commands.lang.possible-languages", "Possible languages: "),
                        NamedTextColor.YELLOW
                );
                for (int i = 0; i < suggestions.size(); i++) {
                    String suggestion = suggestions.get(i);
                    String suggestedCommand = "/fauna lang " + suggestion;
                    if (i > 0) {
                        line = line.append(Component.text(", ", NamedTextColor.YELLOW));
                    }
                    line = line.append(Component.text(suggestion, NamedTextColor.AQUA)
                            .clickEvent(ClickEvent.suggestCommand(suggestedCommand))
                            .hoverEvent(HoverEvent.showText(Component.text(
                                    language.textAny("Click to use: {command}", "commands.help.click-to-use")
                                            .replace("{command}", suggestedCommand),
                                    NamedTextColor.GRAY
                            ))));
                }
                sender.sendMessage(line);
            }
            return;
        }

        sender.sendMessage(Component.text(
                language.text(
                        "commands.lang.changed",
                        "Language changed to {language}.",
                        Map.of("language", language.currentLanguageCode())
                ),
                NamedTextColor.GREEN
        ));
    }

    @Override
    public List<String> suggest(CommandSender sender, String[] args) {
        if (args.length != 1) {
            return List.of();
        }

        String token = args[0].toLowerCase(Locale.ROOT);
        List<String> suggestions = new ArrayList<>();
        for (String langCode : language.availableLanguageCodes()) {
            if (langCode.startsWith(token)) {
                suggestions.add(langCode);
            }
        }
        return suggestions;
    }

    private List<String> suggestClosestLanguages(String token) {
        List<ScoredLanguage> scored = new ArrayList<>();
        for (String langCode : language.availableLanguageCodes()) {
            String code = langCode.toLowerCase(Locale.ROOT);
            int distance = levenshtein(token, code);
            double normalizedDistance = code.isEmpty() ? 1.0D : (double) distance / (double) code.length();
            boolean startsWith = code.startsWith(token) || token.startsWith(code);
            boolean contains = code.contains(token) || token.contains(code);
            if (!startsWith && !contains && distance > 2 && normalizedDistance > 0.45D) {
                continue;
            }
            double score = startsWith ? 0.0D : (contains ? 0.1D : normalizedDistance);
            scored.add(new ScoredLanguage(langCode, score));
        }

        scored.sort(Comparator.comparingDouble(ScoredLanguage::score).thenComparing(ScoredLanguage::code));
        List<String> results = new ArrayList<>();
        for (ScoredLanguage entry : scored) {
            results.add(entry.code());
            if (results.size() == 3) {
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

    private record ScoredLanguage(String code, double score) {
    }
}
