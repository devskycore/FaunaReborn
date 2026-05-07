package io.github.devskycore.faunareborn.animal.cow.hostility;

import java.util.UUID;

final class CowMilkAggressionBrain {
    UUID targetUuid;
    UUID ignoreTargetUuid;
    long ignoreTargetUntilTick;
    long aggressionUntilTick;
    long forgetTargetAtTick;
    long lastAttackTick;
    long nextChargeTick;
    long warningUntilTick;
    long socialAlertBlockedUntilTick;
    CowMilkAggressionState state;

    CowMilkAggressionBrain() {
        this.lastAttackTick = Long.MIN_VALUE;
        this.socialAlertBlockedUntilTick = Long.MIN_VALUE;
        this.state = CowMilkAggressionState.WARNING;
    }
}
