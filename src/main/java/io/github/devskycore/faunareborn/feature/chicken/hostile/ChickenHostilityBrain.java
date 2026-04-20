package io.github.devskycore.faunareborn.feature.chicken.hostile;

import java.util.UUID;

final class ChickenHostilityBrain {

    ChickenHostilityState state;
    long stateStartedTick;

    UUID targetUuid;
    long nextTargetSearchTick;

    long lastAttackTick;

    boolean eligible;
    long nextEligibilityRefreshTick;
    long nextProcessTick;

    double lastTargetDistSq2D = Double.NaN;
    int noProgressTicks;

    ChickenHostilityBrain(long currentTick) {
        this.state = ChickenHostilityState.IDLE;
        this.stateStartedTick = currentTick;
        this.lastAttackTick = Long.MIN_VALUE;
        this.nextProcessTick = 0L;
    }
}
