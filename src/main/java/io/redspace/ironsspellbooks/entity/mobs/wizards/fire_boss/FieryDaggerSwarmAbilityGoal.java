package io.redspace.ironsspellbooks.entity.mobs.wizards.fire_boss;

import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.entity.spells.fiery_dagger.FieryDaggerEntity;
import io.redspace.ironsspellbooks.registries.SoundRegistry;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

public class FieryDaggerSwarmAbilityGoal extends AnimatedActionGoal<FireBossEntity> {
    public static final int ANIM_DURATION = (int) (1.25 * 20);
    public static final int ACTION_TIMESTAMP = (int) (.88 * 20);

    public FieryDaggerSwarmAbilityGoal(FireBossEntity mob) {
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
        return 20 * 3 + Utils.random.nextInt(41);
    }

    @Override
    protected String getAnimationId() {
        return "summon_fiery_daggers";
    }

    @Override
    public void tick() {
        if (mob.getTarget() != null) {
            //this is why we have brains huh
            mob.attackGoal.setTarget(mob.getTarget());
            mob.attackGoal.doMovement(mob.distanceToSqr(mob.getTarget()));
        }
        super.tick();
    }

    @Override
    protected void doAction() {
        var target = mob.getTarget();
        if (target != null) {
            mob.playSound(SoundRegistry.FIRE_CAST.get(), 2f, Utils.random.nextIntBetweenInclusive(80, 110) * .01f);

            Vec3 pos = mob.position();
            int count = 7;
            int delay = Utils.random.nextIntBetweenInclusive(30, 70);
            float yAngle = -Utils.getAngle(target.getX(), target.getZ(), mob.getX(), mob.getZ()) + Mth.HALF_PI;
            for (int i = 0; i < count; i++) {
                Vec3 offset = new Vec3(1.5 * mob.getScale(), 0, 0)
                        .zRot(Mth.lerp(i / (count - 1f), 0, -Mth.PI))
                        .yRot(yAngle)
                        .add(0, mob.getEyeHeight(), 0);
                FieryDaggerEntity dagger = new FieryDaggerEntity(mob.level);
                dagger.setOwner(mob);
                dagger.ownerTrack = offset;
                dagger.setTarget(mob.getTarget());
                dagger.setPos(pos.add(offset.yRot(mob.getYRot())));
                dagger.delay = delay + i * 2;
                dagger.setDamage((float) (mob.getAttributeValue(Attributes.ATTACK_DAMAGE) * .75));
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
