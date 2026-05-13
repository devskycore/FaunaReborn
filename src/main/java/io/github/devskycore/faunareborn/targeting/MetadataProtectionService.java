package io.github.devskycore.faunareborn.targeting;

import org.bukkit.entity.Player;
import org.bukkit.metadata.MetadataValue;

import java.util.Locale;
import java.util.Set;

final class MetadataProtectionService implements PlayerProtectionService {

    private static final Set<String> GOD_MODE_KEYS = Set.of(
            "god",
            "godmode",
            "essentials.god",
            "essentials.godmode"
    );

    @Override
    public boolean isProtected(Player player) {
        if (player == null) {
            return false;
        }
        if (player.isInvulnerable()) {
            return true;
        }
        for (String key : GOD_MODE_KEYS) {
            if (hasTruthyMetadata(player, key)) {
                return true;
            }
        }
        for (String tag : player.getScoreboardTags()) {
            String normalized = tag.toLowerCase(Locale.ROOT);
            if (normalized.contains("god")) {
                return true;
            }
        }
        return false;
    }

    static boolean hasTruthyMetadata(Player player, String key) {
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
