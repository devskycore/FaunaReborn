package io.github.devskycore.faunareborn.animal.cow.hostility;

import io.github.devskycore.faunareborn.combat.deathmessage.HostilityCause;

import java.util.UUID;

final class CowMilkAggressionBrain {
    UUID targetUuid;
    UUID ignoreTargetUuid;
    long ignoreTargetUntilTick;
    long aggressionUntilTick;
    long forgetTargetAtTick;
    long lastAttackTick;
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
    HostilityCause hostilityCause;
    CowMilkAggressionState state;

    CowMilkAggressionBrain() {
        this.lastAttackTick = Long.MIN_VALUE;
        this.lastLineOfSightCheckTick = Long.MIN_VALUE;
        this.lastLineOfSightResult = true;
        this.lastMovementBaseValue = Double.NaN;
        this.socialAlertBlockedUntilTick = Long.MIN_VALUE;
        this.hostilityCause = HostilityCause.DIRECT_ASSAULT;
        this.state = CowMilkAggressionState.WARNING;
    }
}
