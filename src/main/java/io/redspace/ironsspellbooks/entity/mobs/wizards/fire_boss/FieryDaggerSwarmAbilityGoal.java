package io.redspace.ironsspellbooks.entity.mobs.wizards.fire_boss;

import io.redspace.ironsspellbooks.api.entity.IMagicEntity;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.entity.mobs.IAnimatedAttacker;
import io.redspace.ironsspellbooks.entity.spells.fiery_dagger.FieryDaggerEntity;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

public class FieryDaggerSwarmAbilityGoal<T extends Mob & IMagicEntity & IAnimatedAttacker> extends AnimatedActionGoal<T> {
    public static final int ANIM_DURATION = (int) (1.25 * 20);
    public static final int ACTION_TIMESTAMP = (int) (.88 * 20);

    public FieryDaggerSwarmAbilityGoal(T mob) {
        super(mob);
    }

    @Override
    protected boolean canStartAction() {
        return mob.getTarget() != null;
    }

    @Override
    protected int getActionTimestamp() {
        return ACTION_TIMESTAMP;
    }

    @Override
    protected int getActionDuration() {
        return ANIM_DURATION;
    }

    @Override
    protected int getCooldown() {
        return 20 * 4;
    }

    @Override
    protected String getAnimationId() {
        return "summon_fiery_daggers";
    }

    @Override
    protected void doAction() {
        var target = mob.getTarget();
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
}
