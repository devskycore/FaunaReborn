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
            "essentials.vanish",
            "premiumvanish",
            "supervanish"
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
            if (tag.toLowerCase(Locale.ROOT).contains("vanish")) {
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
