package io.redspace.ironsspellbooks.entity.mobs.wizards.fire_boss;

import io.redspace.ironsspellbooks.api.entity.IMagicEntity;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.entity.mobs.IAnimatedAttacker;
import io.redspace.ironsspellbooks.entity.spells.fiery_dagger.FieryDaggerEntity;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

public class FieryDaggerSwarmAbilityGoal<T extends Mob & IMagicEntity & IAnimatedAttacker> extends Goal {
    int abilityTimer;
    int delay;
    boolean isUsing;
    final T mob;

    public FieryDaggerSwarmAbilityGoal(T mob) {
        this.setFlags(EnumSet.of(Flag.TARGET));
        this.mob = mob;
    }

    @Override
    public boolean canUse() {
        return mob.onGround() && mob.getTarget() != null && mob.distanceToSqr(mob.getTarget()) > 3 * 3 && delay-- <= 0;
    }

    @Override
    public boolean canContinueToUse() {
        return isUsing;
    }

    public static final int ANIM_DURATION = (int) (1.25 * 20);
    public static final int ACTION_TIMESTAMP = (int) (.88 * 20);

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        abilityTimer++;
        var target = mob.getTarget();
        if (target != null) {
            mob.getLookControl().setLookAt(target);
        }
        if (abilityTimer == ACTION_TIMESTAMP) {
            if (target != null) {
                Vec3 pos = mob.position();
                int count = 7;
                int delay = Utils.random.nextIntBetweenInclusive(40, 80);
                for (int i = 0; i < count; i++) {
                    Vec3 offset = new Vec3(1.5 * mob.getScale(), 0, 0).zRot(Mth.lerp(i / (count - 1f), 0, -Mth.PI)).add(0, mob.getEyeHeight(), 0)
                            .yRot(-Mth.DEG_TO_RAD * mob.getYRot() - Mth.HALF_PI);
                    FieryDaggerEntity dagger = new FieryDaggerEntity(mob.level);
                    dagger.setOwner(mob);
                    dagger.ownerTrack = offset;
                    dagger.setTarget(mob.getTarget());
                    dagger.setPos(pos.add(offset.yRot(mob.getYRot())));
                    dagger.delay = delay + i * 2;
                    dagger.setDamage((float) (mob.getAttributeValue(Attributes.ATTACK_DAMAGE) * .25));
                    mob.level.addFreshEntity(dagger);
                }
            }
        }
        if (abilityTimer >= ANIM_DURATION) {
            isUsing = false;
        }
    }

    @Override
    public void start() {
        isUsing = true;
        abilityTimer = 0;
        //todo: remove debug cooldown
        delay = 20 * 4; //Utils.random.nextIntBetweenInclusive(12, 23) * 20;
        mob.serverTriggerAnimation("summon_fiery_daggers");
    }
}
