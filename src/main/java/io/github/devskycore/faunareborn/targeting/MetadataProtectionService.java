package io.github.devskycore.faunareborn.targeting;

import org.bukkit.entity.Player;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashSet;
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
            if (hasTruthyPersistentFlag(player, key)) {
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

    static boolean hasTruthyPersistentFlag(Player player, String key) {
        PersistentDataContainer container = player.getPersistentDataContainer();
        if (container.getKeys().isEmpty()) {
            return false;
        }
        for (String candidate : keyCandidates(key)) {
            NamespacedKey namespacedKey = NamespacedKey.fromString(candidate);
            if (namespacedKey == null) {
                continue;
            }
            if (isTruthy(container, namespacedKey)) {
                return true;
            }
        }
        return false;
    }

    private static Set<String> keyCandidates(String key) {
        String normalized = key.toLowerCase(Locale.ROOT).trim();
        Set<String> candidates = new HashSet<>();
        candidates.add(normalized);
        candidates.add("minecraft:" + normalized);
        int firstDot = normalized.indexOf('.');
        if (firstDot > 0 && firstDot < normalized.length() - 1) {
            candidates.add(normalized.substring(0, firstDot) + ":" + normalized.substring(firstDot + 1));
        }
        return candidates;
    }

    private static boolean isTruthy(PersistentDataContainer container, NamespacedKey key) {
        Byte byteValue = container.get(key, PersistentDataType.BYTE);
        if (byteValue != null) {
            return byteValue != 0;
        }
        Integer intValue = container.get(key, PersistentDataType.INTEGER);
        if (intValue != null) {
            return intValue != 0;
        }
        Long longValue = container.get(key, PersistentDataType.LONG);
        if (longValue != null) {
            return longValue != 0L;
        }
        String textValue = container.get(key, PersistentDataType.STRING);
        return textValue != null && !"false".equalsIgnoreCase(textValue) && !"0".equals(textValue);
    }
}
