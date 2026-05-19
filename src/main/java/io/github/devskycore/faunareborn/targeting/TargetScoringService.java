package io.github.devskycore.faunareborn.targeting;

import io.github.devskycore.faunareborn.config.common.TargetingSettings;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;

import java.util.UUID;

public final class TargetScoringService {

    private final TargetingSettings.Scoring settings;

    public TargetScoringService(TargetingSettings.Scoring settings) {
        this.settings = settings;
    }

    public boolean enabled() {
        return settings.enabled();
    }

    public boolean requireMultipleCandidates() {
        return settings.requireMultipleCandidates();
    }

    public int retargetCooldownTicks() {
        return settings.retargetCooldownTicks();
    }

    public double score(Player candidate, UUID currentTargetId, int attackers, double distanceSq, boolean hasLineOfSight) {
        double distance = Math.sqrt(Math.max(0.0D, distanceSq));
        double maxHealth = 20.0D;
        AttributeInstance maxHealthAttribute = candidate.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealthAttribute != null) {
            maxHealth = Math.max(1.0D, maxHealthAttribute.getValue());
        }
        double healthRatio = Math.clamp(candidate.getHealth() / maxHealth, 0.0D, 1.0D);
        double healthComponent = (1.0D - healthRatio) * settings.healthWeight();
        double distanceComponent = (-distance) * settings.distanceWeight();

        double threatComponent = 0.0D;
        if (currentTargetId != null && currentTargetId.equals(candidate.getUniqueId())) {
            threatComponent += settings.currentThreatWeight();
        }
        threatComponent -= attackers * (settings.currentThreatWeight() * 0.15D);

        double losComponent = hasLineOfSight ? settings.lineOfSightBonus() : 0.0D;
        return healthComponent + distanceComponent + threatComponent + losComponent;
    }
}
