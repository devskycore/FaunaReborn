package io.github.devskycore.faunareborn.targeting;

import org.bukkit.entity.Player;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.potion.PotionEffectType;

import java.util.Locale;
import java.util.Set;

final class PlayerVisibilityService {

    private static final Set<String> VANISH_KEYS = Set.of(
            "vanished",
            "isVanished",
            "vanish",
            "isvanish",
            "essentials.vanish",
            "essentials:vanish",
            "essentialsx.vanish",
            "essentialsx:vanish",
            "cmivanish",
            "cmi.vanish",
            "staffmode",
            "supervanish:vanished",
            "sv.vanish",
            "premiumvanish",
            "premiumvanish:vanished",
            "supervanish",
            "staff.vanish",
            "staff.vanished"
    );

    boolean isHidden(Player player, boolean checkPotion, boolean checkVanish) {
        if (player == null) {
            return true;
        }
        if (checkPotion && player.hasPotionEffect(PotionEffectType.INVISIBILITY)) {
            return true;
        }
        if (!checkVanish) {
            return false;
        }
        if (player.isInvisible()) {
            return true;
        }
        for (String key : VANISH_KEYS) {
            if (hasTruthyMetadata(player, key)) {
                return true;
            }
        }
        for (String tag : player.getScoreboardTags()) {
            String normalized = tag.toLowerCase(Locale.ROOT);
            if (normalized.contains("vanish")
                    || normalized.contains("invisible")
                    || normalized.contains("hidden")
                    || normalized.equals("staff")
                    || normalized.equals("staffmode")) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasTruthyMetadata(Player player, String key) {
        if (!player.hasMetadata(key)) {
            return false;
        }
        for (MetadataValue value : player.getMetadata(key)) {
            try {
                if (value != null && value.asBoolean()) {
                    return true;
                }
            } catch (Exception ignored) {
            }
        }
        return false;
    }
}
