package io.github.devskycore.faunareborn.animal.pig.hostility;

import java.util.UUID;

final class PigAggressionBrain {
    UUID targetUuid;
    UUID ignoreTargetUuid;
    long ignoreTargetUntilTick;
    long aggressionUntilTick;
    long forgetTargetAtTick;
    long lastAttackTick;
    long lastAttackWallTimeMs;
    long nextChargeTick;
    long nextTargetRefreshTick;
    long lastLineOfSightCheckTick;
    boolean lastLineOfSightResult;
    long nextMovementUpdateTick;
    double lastMovementBaseValue;
    boolean originalGlowCaptured;
    boolean originallyGlowing;
    long nextParticleTick;
    long warningUntilTick;
    long socialAlertBlockedUntilTick;
    PigAggressionState state;

    PigAggressionBrain() {
        this.lastAttackTick = Long.MIN_VALUE;
        this.lastAttackWallTimeMs = Long.MIN_VALUE;
        this.lastLineOfSightCheckTick = Long.MIN_VALUE;
        this.lastLineOfSightResult = true;
        this.lastMovementBaseValue = Double.NaN;
        this.socialAlertBlockedUntilTick = Long.MIN_VALUE;
        this.state = PigAggressionState.WARNING;
    }
}


