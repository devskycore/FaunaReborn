package io.github.devskycore.faunareborn.combat.deathmessage;

import io.github.devskycore.faunareborn.lang.LanguageManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;

public final class HostilityDeathMessageListener implements Listener {

    private static final NamedTextColor PLAYER_COLOR = NamedTextColor.YELLOW;
    private static final NamedTextColor MESSAGE_COLOR = NamedTextColor.GRAY;
    private static final NamedTextColor EMPHASIS_COLOR = NamedTextColor.RED;
    private static final TextColor MOB_GRADIENT_START = TextColor.color(0xFF5555);
    private static final TextColor MOB_GRADIENT_END = TextColor.color(0xAA0000);
    private final LanguageManager language;

    public HostilityDeathMessageListener(LanguageManager language) {
        this.language = language;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getPlayer();
        HostileSpecies species = resolveHostileSpecies(player);
        if (species == null) {
            HostilityContextTracker.clear(player.getUniqueId());
            return;
        }

        HostilityContextTracker.ContextEntry context = HostilityContextTracker.find(player.getUniqueId());
        HostilityCause cause = context != null && context.species() == species
                ? context.cause()
                : HostilityCause.DIRECT_ASSAULT;

        event.deathMessage(buildMessage(player.getName(), species, cause));
        HostilityContextTracker.clear(player.getUniqueId());
    }

    private HostileSpecies resolveHostileSpecies(Player player) {
        if (!(player.getLastDamageCause() instanceof EntityDamageByEntityEvent damageEvent)) {
            return null;
        }
        EntityType type = damageEvent.getDamager().getType();
        return HostileSpecies.from(type);
    }

    private Component buildMessage(String playerName, HostileSpecies species, HostilityCause cause) {
        return Component.text(playerName, PLAYER_COLOR)
                .append(Component.text(t("death.common.died", " died "), MESSAGE_COLOR))
                .append(detailComponent(species, cause));
    }

    private Component detailComponent(HostileSpecies species, HostilityCause cause) {
        Component hostileName = hostileName(species);
        Component hostilePlural = hostilePlural(species);
        return switch (cause) {
            case ROD_PROVOCATION -> base(t("death.cause.rod.prefix", "after "))
                    .append(emphasis(t("death.cause.rod.hook-action", "hooking")))
                    .append(base(t("death.cause.rod.middle", " the wrong ")))
                    .append(hostileName)
                    .append(base(t("death.common.dot", ".")));
            case MILKING_PROVOCATION -> base(t("death.cause.milking.prefix", "after "))
                    .append(emphasis(t("death.cause.milking.action", "milking")))
                    .append(base(t("death.cause.milking.middle", " the wrong ")))
                    .append(hostileName)
                    .append(base(t("death.common.dot", ".")));
            case TERRITORIAL_PICKUP -> base(t("death.cause.territorial.prefix", "after "))
                    .append(emphasis(t("death.cause.territorial.action", "trespassing")))
                    .append(base(t("death.cause.territorial.middle", " into ")))
                    .append(hostilePlural)
                    .append(base(t("death.cause.territorial.suffix", " territory.")));
            case COOKING_FURNACE -> base(t("death.cause.cooking.prefix", "by "))
                    .append(emphasis(t("death.cause.cooking.action", "angering")))
                    .append(base(" "))
                    .append(hostilePlural)
                    .append(base(t("death.cause.cooking.furnace-suffix", " with a furnace.")));
            case COOKING_SMOKER -> base(t("death.cause.cooking.prefix", "by "))
                    .append(emphasis(t("death.cause.cooking.action", "angering")))
                    .append(base(" "))
                    .append(hostilePlural)
                    .append(base(t("death.cause.cooking.smoker-suffix", " with a smoker.")));
            case COOKING_CAMPFIRE -> base(t("death.cause.cooking.prefix", "by "))
                    .append(emphasis(t("death.cause.cooking.action", "angering")))
                    .append(base(" "))
                    .append(hostilePlural)
                    .append(base(t("death.cause.cooking.campfire-suffix", " with a campfire.")));
            case HERD_RETALIATION_DAMAGE -> base(t("death.cause.herd-damage.prefix", "after "))
                    .append(emphasis(t("death.cause.herd-damage.action", "striking")))
                    .append(base(t("death.cause.herd-damage.middle", " the wrong herd of ")))
                    .append(hostilePlural)
                    .append(base(t("death.common.dot", ".")));
            case HERD_RETALIATION_NEARBY_KILL -> base(t("death.cause.herd-nearby.prefix", "while trying to escape "))
                    .append(emphasis(t("death.cause.herd-nearby.action", "enraged")))
                    .append(base(" "))
                    .append(hostilePlural)
                    .append(base(t("death.common.dot", ".")));
            case BABY_PROTECTION -> base(t("death.cause.baby.prefix", "after "))
                    .append(emphasis(t("death.cause.baby.action", "threatening")))
                    .append(base(t("death.cause.baby.middle", " a baby ")))
                    .append(hostileName)
                    .append(base(t("death.common.dot", ".")));
            case DIRECT_ASSAULT -> directAssaultText(species, hostileName);
        };
    }

    private Component directAssaultText(HostileSpecies species, Component hostileName) {
        return switch (species) {
            case PIG -> base(t("death.direct.pig.prefix", "after being "))
                    .append(emphasis(t("death.direct.pig.action", "mauled")))
                    .append(base(t("death.direct.pig.middle", " by a hostile ")))
                    .append(hostileName)
                    .append(base(t("death.common.dot", ".")));
            case COW -> base(t("death.direct.cow.prefix", "after being "))
                    .append(emphasis(t("death.direct.cow.action", "trampled")))
                    .append(base(t("death.direct.cow.middle", " by an enraged ")))
                    .append(hostileName)
                    .append(base(t("death.common.dot", ".")));
            case CHICKEN -> base(t("death.direct.chicken.prefix", "after being "))
                    .append(emphasis(t("death.direct.chicken.action", "pecked apart")))
                    .append(base(t("death.direct.chicken.middle", " by a hostile ")))
                    .append(hostileName)
                    .append(base(t("death.common.dot", ".")));
        };
    }

    private Component hostileName(HostileSpecies species) {
        return switch (species) {
            case PIG -> gradientUppercase(t("death.species.pig.singular", "PIG"));
            case COW -> gradientUppercase(t("death.species.cow.singular", "COW"));
            case CHICKEN -> gradientUppercase(t("death.species.chicken.singular", "CHICKEN"));
        };
    }

    private Component hostilePlural(HostileSpecies species) {
        return switch (species) {
            case PIG -> gradientUppercase(t("death.species.pig.plural", "PIGS"));
            case COW -> gradientUppercase(t("death.species.cow.plural", "COWS"));
            case CHICKEN -> gradientUppercase(t("death.species.chicken.plural", "CHICKENS"));
        };
    }

    private Component gradientUppercase(String text) {
        if (text.length() == 1) {
            return Component.text(text, MOB_GRADIENT_START, TextDecoration.BOLD);
        }

        Component gradient = Component.empty();
        for (int i = 0; i < text.length(); i++) {
            float ratio = (float) i / (text.length() - 1);
            int red = lerp(MOB_GRADIENT_START.red(), MOB_GRADIENT_END.red(), ratio);
            int green = lerp(MOB_GRADIENT_START.green(), MOB_GRADIENT_END.green(), ratio);
            int blue = lerp(MOB_GRADIENT_START.blue(), MOB_GRADIENT_END.blue(), ratio);
            gradient = gradient.append(Component.text(text.charAt(i), TextColor.color(red, green, blue), TextDecoration.BOLD));
        }
        return gradient;
    }

    private Component base(String text) {
        return Component.text(text, MESSAGE_COLOR);
    }

    private Component emphasis(String text) {
        return Component.text(text, EMPHASIS_COLOR);
    }

    private int lerp(int start, int end, float ratio) {
        return Math.round(start + ((end - start) * ratio));
    }

    private String t(String path, String fallback) {
        return language.text(path, fallback);
    }
}
