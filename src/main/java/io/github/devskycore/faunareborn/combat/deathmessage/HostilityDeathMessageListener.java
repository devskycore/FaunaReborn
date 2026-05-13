package io.github.devskycore.faunareborn.combat.deathmessage;

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
                .append(Component.text(" died ", MESSAGE_COLOR))
                .append(detailComponent(species, cause));
    }

    private Component detailComponent(HostileSpecies species, HostilityCause cause) {
        Component hostileName = hostileName(species);
        Component hostilePlural = hostilePlural(species);
        return switch (cause) {
            case ROD_PROVOCATION -> base("after ")
                    .append(emphasis("hooking"))
                    .append(base(" the wrong "))
                    .append(hostileName)
                    .append(base("."));
            case MILKING_PROVOCATION -> base("after ")
                    .append(emphasis("milking"))
                    .append(base(" the wrong "))
                    .append(hostileName)
                    .append(base("."));
            case TERRITORIAL_PICKUP -> base("after ")
                    .append(emphasis("trespassing"))
                    .append(base(" into "))
                    .append(hostilePlural)
                    .append(base(" territory."));
            case COOKING_FURNACE -> base("by ")
                    .append(emphasis("angering"))
                    .append(base(" "))
                    .append(hostilePlural)
                    .append(base(" with a furnace."));
            case COOKING_SMOKER -> base("by ")
                    .append(emphasis("angering"))
                    .append(base(" "))
                    .append(hostilePlural)
                    .append(base(" with a smoker."));
            case COOKING_CAMPFIRE -> base("by ")
                    .append(emphasis("angering"))
                    .append(base(" "))
                    .append(hostilePlural)
                    .append(base(" with a campfire."));
            case HERD_RETALIATION_DAMAGE -> base("after ")
                    .append(emphasis("striking"))
                    .append(base(" the wrong herd of "))
                    .append(hostilePlural)
                    .append(base("."));
            case HERD_RETALIATION_NEARBY_KILL -> base("while trying to escape ")
                    .append(emphasis("enraged"))
                    .append(base(" "))
                    .append(hostilePlural)
                    .append(base("."));
            case BABY_PROTECTION -> base("after ")
                    .append(emphasis("threatening"))
                    .append(base(" a baby "))
                    .append(hostileName)
                    .append(base("."));
            case DIRECT_ASSAULT -> directAssaultText(species, hostileName);
        };
    }

    private Component directAssaultText(HostileSpecies species, Component hostileName) {
        return switch (species) {
            case PIG -> base("after being ")
                    .append(emphasis("mauled"))
                    .append(base(" by a hostile "))
                    .append(hostileName)
                    .append(base("."));
            case COW -> base("after being ")
                    .append(emphasis("trampled"))
                    .append(base(" by an enraged "))
                    .append(hostileName)
                    .append(base("."));
            case CHICKEN -> base("after being ")
                    .append(emphasis("pecked apart"))
                    .append(base(" by a hostile "))
                    .append(hostileName)
                    .append(base("."));
        };
    }

    private Component hostileName(HostileSpecies species) {
        return switch (species) {
            case PIG -> gradientUppercase("PIG");
            case COW -> gradientUppercase("COW");
            case CHICKEN -> gradientUppercase("CHICKEN");
        };
    }

    private Component hostilePlural(HostileSpecies species) {
        return switch (species) {
            case PIG -> gradientUppercase("PIGS");
            case COW -> gradientUppercase("COWS");
            case CHICKEN -> gradientUppercase("CHICKENS");
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
}
