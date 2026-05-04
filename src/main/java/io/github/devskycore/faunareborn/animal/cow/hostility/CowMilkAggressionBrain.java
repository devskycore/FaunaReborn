package io.github.devskycore.faunareborn.animal.cow.hostility;

import java.util.UUID;

final class CowMilkAggressionBrain {
    UUID targetUuid;
    long aggressionUntilTick;
    long forgetTargetAtTick;
    long lastAttackTick;
    long nextSocialPropagationAllowedTick;
    long nextChargeTick;
    long warningUntilTick;
    CowMilkAggressionState state;

    CowMilkAggressionBrain() {
        this.lastAttackTick = Long.MIN_VALUE;
        this.state = CowMilkAggressionState.WARNING;
    }
}
