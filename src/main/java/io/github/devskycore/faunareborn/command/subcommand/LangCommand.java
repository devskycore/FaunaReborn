package io.github.devskycore.faunareborn.command.subcommand;

import io.github.devskycore.faunareborn.command.permission.PermissionConstants;
import io.github.devskycore.faunareborn.command.permission.PermissionService;
import io.github.devskycore.faunareborn.gui.FaunaMainGui;
import io.github.devskycore.faunareborn.lang.LanguageManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class LangCommand implements FaunaSubcommand {

    private static final CommandInfo INFO = new CommandInfo(
            "lang",
            "/fauna lang <language>",
            "Change active plugin language.",
            PermissionConstants.COMMAND_LANG,
            true
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
                    language.text("commands.lang.usage", "Usage: /fauna lang <language>"),
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
}
