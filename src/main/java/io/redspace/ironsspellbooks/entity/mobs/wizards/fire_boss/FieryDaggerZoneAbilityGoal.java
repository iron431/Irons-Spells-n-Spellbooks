package io.redspace.ironsspellbooks.entity.mobs.wizards.fire_boss;

import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.entity.spells.fiery_dagger.FieryDaggerEntity;
import io.redspace.ironsspellbooks.registries.SoundRegistry;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

public class FieryDaggerZoneAbilityGoal extends AnimatedActionGoal<FireBossEntity> {
    public FieryDaggerZoneAbilityGoal(FireBossEntity mob) {
        super(mob);
    }

    @Override
    protected boolean canStartAction() {
        return mob.onGround() && mob.getTarget() != null && mob.distanceToSqr(mob.getTarget()) > 6 * 6;
    }

    @Override
    protected int getActionTimestamp() {
        return 1;
    }

    @Override
    protected int getActionDuration() {
        return 5;
    }

    @Override
    protected int getCooldown() {
        return 20 * 4 + Utils.random.nextInt(41);
    }

    @Override
    protected String getAnimationId() {
        return "instant_slash";
    }

    @Override
    public void tick() {
        if (mob.getTarget() != null) {
            mob.attackGoal.setTarget(mob.getTarget());
            mob.attackGoal.doMovement(mob.distanceToSqr(mob.getTarget()));
        }
        super.tick();
    }

    @Override
    protected void doAction() {
        var target = mob.getTarget();
        if (target != null) {
            mob.playSound(SoundRegistry.FIERY_DAGGER_THROW.get(), 2f, Utils.random.nextIntBetweenInclusive(80, 110) * .01f);

            Vec3 start = mob.getEyePosition();
            Vec3 targetPos = target.position();
            Vec3 deltaAim = targetPos.subtract(start);
            // throw 3 daggers at 30 degree angles, centered around our target's postion
            for (int i = 0; i < 3; i++) {
                Vec3 aim = start.add(deltaAim.yRot(Mth.PI / 6 * (i - 1)));
                int delay = Utils.random.nextIntBetweenInclusive(20, 50);

                FieryDaggerEntity dagger = new FieryDaggerEntity(mob.level);
                dagger.setOwner(mob);
                dagger.setPos(start);
                dagger.delay = delay;
                dagger.setDamage((float) (mob.getAttributeValue(Attributes.ATTACK_DAMAGE) * .25));
                dagger.setExplosionRadius(4 + Utils.random.nextFloat() * 2);
                dagger.setNoGravity(false);

                Vec3 horizontal = aim.subtract(start).multiply(1, 0, 1);
                double horizontalSpeed = 1 * Mth.cos(Mth.PI * .25f);
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

    @Override
    public void stop() {
        super.stop();
        mob.attackGoal.setTarget(null);
    }
}
