package io.github.devskycore.faunareborn.animal.chicken.hostility;

import io.github.devskycore.faunareborn.animal.chicken.config.ChickenHostilitySettings;
import it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap;
import org.bukkit.entity.Chicken;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.projectiles.ProjectileSource;

import java.util.List;

final class SocialAlertService {

    private final boolean enabled;
    private final boolean onDamage;
    private final boolean onNearbyDeath;
    private final boolean responderAdultsOnly;
    private final double radius;
    private final int cooldownTicks;
    private final int joinCooldownTicks;
    private final int maxResponders;
    private final ChickenRecruitment recruitment;
    private final Int2LongOpenHashMap cooldownUntilByChickenId = new Int2LongOpenHashMap();

    SocialAlertService(ChickenHostilitySettings.SocialAlert settings, ChickenRecruitment recruitment) {
        this.enabled = settings.enabled();
        this.onDamage = settings.onDamage();
        this.onNearbyDeath = settings.onNearbyDeath();
        this.responderAdultsOnly = settings.responderAdultsOnly();
        this.radius = settings.radius();
        this.cooldownTicks = settings.cooldownTicks();
        this.joinCooldownTicks = settings.joinCooldownTicks();
        this.maxResponders = settings.maxResponders();
        this.recruitment = recruitment;
        this.cooldownUntilByChickenId.defaultReturnValue(Long.MIN_VALUE);
    }

    boolean enabled() {
        return enabled;
    }

    boolean onDamage() {
        return onDamage;
    }

    boolean onNearbyDeath() {
        return onNearbyDeath;
    }

    boolean responderAdultsOnly() {
        return responderAdultsOnly;
    }

    double radius() {
        return radius;
    }

    boolean isJoinBlocked(ChickenHostilityBrain brain, long currentTick) {
        return currentTick < brain.socialAlertBlockedUntilTick;
    }

    void applyJoinCooldown(ChickenHostilityBrain brain, long currentTick) {
        if (joinCooldownTicks > 0) {
            brain.socialAlertBlockedUntilTick = currentTick + joinCooldownTicks;
        }
    }

    void emit(int emitterChickenId, Player aggressor, List<Entity> nearbyEntities, int aggressionDurationTicks, long currentTick) {
        if (!enabled) {
            return;
        }
        if (aggressor == null || !aggressor.isOnline() || aggressor.isDead()) {
            return;
        }
        if (maxResponders <= 0) {
            return;
        }

        long cooldownUntil = cooldownUntilByChickenId.get(emitterChickenId);
        if (cooldownUntil != Long.MIN_VALUE && currentTick < cooldownUntil) {
            return;
        }

        int recruited = 0;
        for (Entity entity : nearbyEntities) {
            if (!(entity instanceof Chicken ally)) {
                continue;
            }
            if (ally.getEntityId() == emitterChickenId) {
                continue;
            }
            if (recruitment.tryRecruit(ally, aggressor, aggressionDurationTicks, true)) {
                recruited++;
            }
            if (recruited >= maxResponders) {
                break;
            }
        }

        if (recruited > 0 && cooldownTicks > 0) {
            cooldownUntilByChickenId.put(emitterChickenId, currentTick + cooldownTicks);
        }
    }

    void cleanupCooldowns(long currentTick) {
        if (cooldownUntilByChickenId.isEmpty()) {
            return;
        }
        cooldownUntilByChickenId.int2LongEntrySet().removeIf(entry -> currentTick >= entry.getLongValue());
    }

    void removeChicken(int chickenId) {
        cooldownUntilByChickenId.remove(chickenId);
    }

    void clear() {
        cooldownUntilByChickenId.clear();
    }

    Player resolveDamagingPlayer(Entity damager) {
        if (damager instanceof Player player) {
            return player;
        }
        if (damager instanceof Projectile projectile) {
            ProjectileSource shooter = projectile.getShooter();
            if (shooter instanceof Player player) {
                return player;
            }
        }
        return null;
    }

    interface ChickenRecruitment {
        boolean tryRecruit(Chicken chicken, Player aggressor, int aggressionDurationTicks, boolean applyJoinCooldown);
    }
}
