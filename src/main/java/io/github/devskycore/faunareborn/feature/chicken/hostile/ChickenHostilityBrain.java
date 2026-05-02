package io.github.devskycore.faunareborn.feature.chicken.hostile;

import java.util.UUID;

final class ChickenHostilityBrain {

    ChickenHostilityState state;
    long stateStartedTick;

    UUID targetUuid;
    long nextTargetSearchTick;
    long lastThreatRefreshTick;
    UUID ignoreTargetUuid;
    long ignoreTargetUntilTick;

    long lastAttackTick;
    long socialAlertBlockedUntilTick;
    boolean socialAlertOverrideEligibility;

    boolean eligible;
    long nextEligibilityRefreshTick;
    long nextProcessTick;
    long lastJumpTick;
    int noLineOfSightTicks;

    double lastTargetDistSq2D = Double.NaN;
    int noProgressTicks;

    ChickenHostilityBrain(long currentTick) {
        this.state = ChickenHostilityState.IDLE;
        this.stateStartedTick = currentTick;
        this.lastAttackTick = Long.MIN_VALUE;
        this.socialAlertBlockedUntilTick = Long.MIN_VALUE;
        this.socialAlertOverrideEligibility = false;
        this.lastThreatRefreshTick = currentTick;
        this.lastJumpTick = Long.MIN_VALUE;
        this.ignoreTargetUntilTick = Long.MIN_VALUE;
        this.nextProcessTick = 0L;
    }
}
