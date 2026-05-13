package io.github.devskycore.faunareborn.animal.pig.hostility;

import io.github.devskycore.faunareborn.combat.deathmessage.HostilityCause;

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
    long stateStartedTick;
    HostilityCause hostilityCause;
    PigAggressionState state;

    PigAggressionBrain() {
        this.lastAttackTick = Long.MIN_VALUE;
        this.lastAttackWallTimeMs = Long.MIN_VALUE;
        this.lastLineOfSightCheckTick = Long.MIN_VALUE;
        this.lastLineOfSightResult = true;
        this.lastMovementBaseValue = Double.NaN;
        this.socialAlertBlockedUntilTick = Long.MIN_VALUE;
        this.stateStartedTick = 0L;
        this.hostilityCause = HostilityCause.DIRECT_ASSAULT;
        this.state = PigAggressionState.IDLE;
    }
}


