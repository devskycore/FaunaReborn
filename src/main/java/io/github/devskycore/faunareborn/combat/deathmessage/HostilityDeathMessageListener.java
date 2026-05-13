package io.github.devskycore.faunareborn.combat.deathmessage;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;

public final class HostilityDeathMessageListener implements Listener {

    private static final NamedTextColor PREFIX_COLOR = NamedTextColor.DARK_RED;
    private static final NamedTextColor PLAYER_COLOR = NamedTextColor.GOLD;
    private static final NamedTextColor DETAIL_COLOR = NamedTextColor.RED;
    private static final NamedTextColor SEPARATOR_COLOR = NamedTextColor.GRAY;

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
        return Component.text("[DEATH] ", PREFIX_COLOR)
                .append(Component.text(playerName, PLAYER_COLOR))
                .append(Component.text(" ", SEPARATOR_COLOR))
                .append(Component.text(detailText(species, cause), DETAIL_COLOR));
    }

    private String detailText(HostileSpecies species, HostilityCause cause) {
        String hostileName = hostileName(species);
        String hostileGroup = hostileGroup(species);
        return switch (cause) {
            case ROD_PROVOCATION -> "hooked the wrong " + hostileName + ".";
            case MILKING_PROVOCATION -> "milked the wrong " + hostileName + ".";
            case TERRITORIAL_PICKUP -> "raided " + hostileGroup + " territory.";
            case COOKING_FURNACE -> "was cooked by angry " + hostileGroup + " after using a furnace.";
            case COOKING_SMOKER -> "was smoked by furious " + hostileGroup + " after using a smoker.";
            case COOKING_CAMPFIRE -> "was charred by raging " + hostileGroup + " after using a campfire.";
            case HERD_RETALIATION_DAMAGE -> "angered the " + hostileGroup + " after a hit.";
            case HERD_RETALIATION_NEARBY_KILL -> "couldn't escape the enraged " + hostileGroup + ".";
            case BABY_PROTECTION -> "was swarmed for threatening a baby " + hostileName + ".";
            case DIRECT_ASSAULT -> directAssaultText(species, hostileName);
        };
    }

    private String directAssaultText(HostileSpecies species, String hostileName) {
        return switch (species) {
            case PIG -> "was mauled by a hostile " + hostileName + ".";
            case COW -> "was trampled by an enraged " + hostileName + ".";
            case CHICKEN -> "was pecked apart by a hostile " + hostileName + ".";
        };
    }

    private String hostileName(HostileSpecies species) {
        return switch (species) {
            case PIG -> "Pig";
            case COW -> "Cow";
            case CHICKEN -> "Chicken";
        };
    }

    private String hostileGroup(HostileSpecies species) {
        return switch (species) {
            case PIG -> "pigs";
            case COW -> "cattle";
            case CHICKEN -> "flock";
        };
    }
}
