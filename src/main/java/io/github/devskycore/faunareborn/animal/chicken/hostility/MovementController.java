package io.github.devskycore.faunareborn.animal.chicken.hostility;

import io.github.devskycore.faunareborn.animal.chicken.config.ChickenHostilitySettings;
import org.bukkit.entity.Chicken;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

final class MovementController {

    private static final int NO_PROGRESS_RESET_TICKS = 30;
    private static final double MIN_PROGRESS_DELTA_SQ = 0.0001D;
    private static final double NO_PROGRESS_MIN_DISTANCE_SQ_2D = 9.0D;
    private static final double BASE_CHASE_SPEED_PER_TICK = 0.12D;
    private static final double MIN_CHASE_SPEED_PER_TICK = 0.02D;
    private static final double MAX_CHASE_SPEED_PER_TICK = 0.35D;
    private static final long LINE_OF_SIGHT_CACHE_TICKS = 4L;

    private final int noLineOfSightResetTicks;
    private final double chaseSpeedPerTick;
    private final double distanceBoostStartDistanceSq;
    private final double distanceBoostExtraSpeedPerBlock;
    private final double distanceBoostMaxMultiplier;
    private final boolean terrainJumpEnabled;
    private final double terrainJumpVerticalBoost;
    private final int terrainJumpCooldownTicks;
    private final double terrainJumpTriggerHeightDelta;
    private final Vector scratchVelocity = new Vector();

    MovementController(ChickenHostilitySettings.Combat combat, ChickenHostilitySettings.Movement movement) {
        this.noLineOfSightResetTicks = combat.noLineOfSightResetTicks();
        this.chaseSpeedPerTick = Math.clamp(
                BASE_CHASE_SPEED_PER_TICK * movement.speedMultiplier(),
                MIN_CHASE_SPEED_PER_TICK,
                MAX_CHASE_SPEED_PER_TICK
        );
        this.distanceBoostStartDistanceSq = movement.distanceBoostStartDistance() * movement.distanceBoostStartDistance();
        this.distanceBoostExtraSpeedPerBlock = movement.distanceBoostExtraSpeedPerBlock();
        this.distanceBoostMaxMultiplier = movement.distanceBoostMaxMultiplier();
        this.terrainJumpEnabled = movement.terrainJumpEnabled();
        this.terrainJumpVerticalBoost = movement.terrainJumpVerticalBoost();
        this.terrainJumpCooldownTicks = movement.terrainJumpCooldownTicks();
        this.terrainJumpTriggerHeightDelta = movement.terrainJumpTriggerHeightDelta();
    }

    void move(Chicken chicken, Player player, ChickenHostilityBrain brain, long currentTick) {
        double dx = player.getX() - chicken.getX();
        double dz = player.getZ() - chicken.getZ();
        double lenSq = dx * dx + dz * dz;

        if (lenSq < 0.0001D) return;
        faceTarget(chicken, dx, dz);

        double invLen = normalizedSpeed(lenSq, resolveEffectiveSpeedPerTick(lenSq));

        scratchVelocity.setX(dx * invLen);
        scratchVelocity.setY(chicken.getVelocity().getY());
        scratchVelocity.setZ(dz * invLen);
        maybeApplyTerrainJump(chicken, player, brain, scratchVelocity, currentTick);

        chicken.setVelocity(scratchVelocity);
    }

    boolean failsSimplePathing(Chicken chicken, Player target, ChickenHostilityBrain brain, boolean requireProgress) {
        double verticalGap = Math.abs(target.getY() - chicken.getY());
        if (verticalGap > ChickenHostilityConstants.MAX_VERTICAL_GAP) {
            resetProgressTracking(brain);
            return true;
        }

        if (!requireProgress) {
            resetProgressTracking(brain);
            return false;
        }

        double distSq2D = HostilityDistances.distanceSq2D(chicken, target);
        if (distSq2D < NO_PROGRESS_MIN_DISTANCE_SQ_2D) {
            resetProgressTracking(brain);
            return false;
        }

        if (!Double.isNaN(brain.lastTargetDistSq2D)
                && distSq2D > brain.lastTargetDistSq2D - MIN_PROGRESS_DELTA_SQ) {
            brain.noProgressTicks++;
        } else {
            brain.noProgressTicks = 0;
        }

        brain.lastTargetDistSq2D = distSq2D;
        return brain.noProgressTicks >= NO_PROGRESS_RESET_TICKS;
    }

    void resetProgressTracking(ChickenHostilityBrain brain) {
        brain.lastTargetDistSq2D = Double.NaN;
        brain.noProgressTicks = 0;
        brain.noLineOfSightTicks = 0;
    }

    boolean hasLostLineOfSight(Chicken chicken, Player target, ChickenHostilityBrain brain, long currentTick) {
        if (brain.lastLineOfSightCheckTick == Long.MIN_VALUE
                || currentTick - brain.lastLineOfSightCheckTick >= LINE_OF_SIGHT_CACHE_TICKS) {
            brain.lastLineOfSightResult = chicken.hasLineOfSight(target);
            brain.lastLineOfSightCheckTick = currentTick;
        }
        if (brain.lastLineOfSightResult) {
            brain.noLineOfSightTicks = 0;
            return false;
        }
        brain.noLineOfSightTicks++;
        return brain.noLineOfSightTicks >= noLineOfSightResetTicks;
    }

    private void faceTarget(Chicken chicken, double dx, double dz) {
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        chicken.setRotation(yaw, chicken.getPitch());
    }

    private static double normalizedSpeed(double lenSq, double speedPerTick) {
        return speedPerTick / Math.sqrt(lenSq);
    }

    private double resolveEffectiveSpeedPerTick(double lenSq) {
        if (lenSq <= distanceBoostStartDistanceSq) {
            return chaseSpeedPerTick;
        }

        double farDistanceMultiplier = Math.min(
                distanceBoostMaxMultiplier,
                1.0D + ((Math.sqrt(lenSq) - Math.sqrt(distanceBoostStartDistanceSq)) * distanceBoostExtraSpeedPerBlock)
        );
        return Math.min(chaseSpeedPerTick * farDistanceMultiplier, MAX_CHASE_SPEED_PER_TICK);
    }

    private void maybeApplyTerrainJump(
            Chicken chicken,
            Player target,
            ChickenHostilityBrain brain,
            Vector velocity,
            long currentTick
    ) {
        if (!terrainJumpEnabled) return;
        if (!chicken.isOnGround()) return;
        if (brain.lastJumpTick != Long.MIN_VALUE
                && currentTick - brain.lastJumpTick < terrainJumpCooldownTicks) return;

        double verticalDelta = target.getY() - chicken.getY();
        boolean chasingUphill = verticalDelta >= terrainJumpTriggerHeightDelta;
        boolean likelyStuck = brain.noProgressTicks >= Math.max(4, terrainJumpCooldownTicks / 2);

        if (!chasingUphill && !likelyStuck) return;

        velocity.setY(Math.max(velocity.getY(), terrainJumpVerticalBoost));
        brain.lastJumpTick = currentTick;
    }
}
