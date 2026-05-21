package io.github.devskycore.faunareborn.targeting;

import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.Set;

final class MetadataPlayerStateHook implements PlayerStateHook {

    private static final Set<String> GOD_MODE_KEYS = Set.of(
            "god",
            "godmode",
            "god_mode",
            "essentials.god",
            "essentials.godmode",
            "essentialsx.god",
            "cmi.god",
            "cmi.godmode"
    );

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

    @Override
    public String name() {
        return "metadata";
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public boolean isProtected(Player player) {
        if (player == null) {
            return false;
        }
        if (player.isInvulnerable()) {
            return true;
        }
        for (String key : GOD_MODE_KEYS) {
            if (PlayerDataFlagLookup.hasTruthyMetadata(player, key)
                    || PlayerDataFlagLookup.hasTruthyPersistentFlag(player, key)) {
                return true;
            }
        }
        for (String tag : player.getScoreboardTags()) {
            if (tag.toLowerCase(Locale.ROOT).contains("god")) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isHidden(Player player) {
        if (player == null) {
            return true;
        }
        if (player.isInvisible()) {
            return true;
        }
        for (String key : VANISH_KEYS) {
            if (PlayerDataFlagLookup.hasTruthyMetadata(player, key)
                    || PlayerDataFlagLookup.hasTruthyPersistentFlag(player, key)) {
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
}
