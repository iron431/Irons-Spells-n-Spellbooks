package io.redspace.ironsspellbooks.entity.mobs.wizards.fire_boss;

import io.redspace.ironsspellbooks.api.entity.IMagicEntity;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.entity.mobs.IAnimatedAttacker;
import io.redspace.ironsspellbooks.entity.spells.fiery_dagger.FieryDaggerEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

public class FieryDaggerZoneAbilityGoal<T extends Mob & IMagicEntity & IAnimatedAttacker> extends AnimatedActionGoal<T> {
    public FieryDaggerZoneAbilityGoal(T mob) {
        super(mob);
    }

    @Override
    protected boolean canStartAction() {
        return mob.onGround() && mob.getTarget() != null && mob.distanceToSqr(mob.getTarget()) > 4 * 4;
    }

    @Override
    protected int getActionTimestamp() {
        return 1;
    }

    @Override
    protected int getActionDuration() {
        return 20;
    }

    @Override
    protected int getCooldown() {
        return 20 * 6;
    }

    @Override
    protected String getAnimationId() {
        return "instant_projectile";
    }

    @Override
    protected void doAction() {
        var target = mob.getTarget();
        if (target != null) {
            Vec3 start = mob.getEyePosition();
            Vec3 aim = target.position();
            int delay = -Utils.random.nextIntBetweenInclusive(40, 80);

            FieryDaggerEntity dagger = new FieryDaggerEntity(mob.level);
            dagger.setOwner(mob);
            dagger.setPos(start);
            dagger.delay = delay;
            dagger.setDamage((float) (mob.getAttributeValue(Attributes.ATTACK_DAMAGE) * .25));
            dagger.setExplosionRadius(4 + Utils.random.nextFloat() * 2);
            dagger.setNoGravity(false);

            double horizontalSpeed = 1;
            Vec3 horizontal = aim.subtract(start).multiply(1, 0, 1);
            double distance = horizontal.length();
            double ticks = distance / horizontalSpeed;
            // y(t) = -1/2(g)(t^2) + v0*t
            // => v0 = [y1 + 1/2(g)(t1^2)]/t1
            double y1 = aim.y - start.y;
            double g = dagger.getGravity();
            double verticalSpeed = (y1 + 0.5 * g * ticks * ticks) / ticks;
            Vec3 trajectory = horizontal.normalize().scale(horizontalSpeed).add(0, verticalSpeed, 0);
            dagger.setDeltaMovement(trajectory);
            mob.level.addFreshEntity(dagger);
        }
    }
}
