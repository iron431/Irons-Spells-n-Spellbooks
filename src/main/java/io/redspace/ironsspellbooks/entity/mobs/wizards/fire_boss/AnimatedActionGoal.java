package io.redspace.ironsspellbooks.entity.mobs.wizards.fire_boss;

import io.redspace.ironsspellbooks.api.entity.IMagicEntity;
import io.redspace.ironsspellbooks.entity.mobs.IAnimatedAttacker;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

public abstract class AnimatedActionGoal<T extends Mob & IMagicEntity & IAnimatedAttacker> extends Goal {
    int abilityTimer;
    int delay;
    boolean isUsing;
    final T mob;

    public AnimatedActionGoal(T mob) {
        this.setFlags(EnumSet.of(Flag.TARGET));
        this.mob = mob;
        this.delay = getCooldown();
    }

    @Override
    public final boolean canUse() {
        return canStartAction() && delay-- <= 0;
    }


    @Override
    public boolean canContinueToUse() {
        return isUsing;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    protected abstract boolean canStartAction();

    protected abstract int getActionTimestamp();

    protected abstract int getActionDuration();

    protected abstract int getCooldown();

    protected abstract String getAnimationId();

    protected abstract void doAction();

    @Override
    public void tick() {
        abilityTimer++;
        var target = mob.getTarget();
        if (target != null) {
            mob.getLookControl().setLookAt(target);
        }
        if (abilityTimer == getActionTimestamp()) {
            doAction();
        }
        if (abilityTimer >= getActionDuration()) {
            isUsing = false;
        }
    }


    @Override
    public void start() {
        isUsing = true;
        abilityTimer = 0;
        delay = getCooldown();
        mob.serverTriggerAnimation(getAnimationId());
    }
}
