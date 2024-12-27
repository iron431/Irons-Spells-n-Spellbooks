package io.redspace.ironsspellbooks.entity.mobs.wizards.fire_boss;

import io.redspace.ironsspellbooks.api.entity.IMagicEntity;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.entity.mobs.IAnimatedAttacker;
import io.redspace.ironsspellbooks.entity.spells.magma_ball.FireBomb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

public class MagmaThrowBossAbilityGoal<T extends Mob & IMagicEntity & IAnimatedAttacker> extends Goal {
    int abilityTimer;
    int delay;
    boolean isUsing;
    final T mob;

    public MagmaThrowBossAbilityGoal(T mob) {
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

    public static final int ANIM_DURATION = (int) (2.0 * 20);
    public static final int ACTION_TIMESTAMP = (int) (1.63 * 20);

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        abilityTimer++;
        mob.setSpeed(0);
        mob.setZza(0);
        mob.setXxa(0);
        var target = mob.getTarget();
        if (target != null) {
            mob.getLookControl().setLookAt(target);
        }
        if (abilityTimer == ACTION_TIMESTAMP) {
            //targeted orbs
            if (target != null) {
                var range = mob.getAttributeValue(Attributes.FOLLOW_RANGE);
                var targets = mob.level.getEntitiesOfClass(target.getClass(), mob.getBoundingBox().inflate(range, 8, range));
                for (LivingEntity entity : targets) {
                    if (Utils.hasLineOfSight(mob.level, mob, entity, false)) {
                        FireBomb fireBomb = fireBomb();
                        double horizontalSpeed = 0.5;
                        Vec3 horizontal = entity.position().subtract(mob.position()).multiply(1, 0, 1);
                        double distance = horizontal.length();
                        double ticks = distance / horizontalSpeed;
                        // y(t) = -1/2(g)(t^2) + v0*t
                        // => v0 = [y1 + 1/2(g)(t1^2)]/t1
                        double y1 = entity.getY() - mob.getY();
                        double g = fireBomb.getGravity();
                        double verticalSpeed = (y1 + 0.5 * g * ticks * ticks) / ticks;
                        Vec3 estMovement = entity.getDeltaMovement().multiply(1, 0, 1).scale(ticks);
                        Vec3 trajectory = horizontal.normalize().scale(horizontalSpeed).add(0, verticalSpeed, 0).add(estMovement);
                        fireBomb.setDeltaMovement(trajectory);
                        mob.level.addFreshEntity(fireBomb);
                    }
                }
            }
            //random orbs
            for (int i = 0; i < 4; i++) {
                FireBomb fireBomb = fireBomb();
                Vec3 motion = Utils.getRandomVec3(0.85).add(0, 3, 0).scale(0.3);
                fireBomb.setDeltaMovement(motion);

                mob.level.addFreshEntity(fireBomb);
            }
        }
        if (abilityTimer >= ANIM_DURATION) {
            isUsing = false;
        }
    }

    protected FireBomb fireBomb() {
        FireBomb fireBomb = new FireBomb(mob.level, mob);
        fireBomb.moveTo(mob.getEyePosition());
        fireBomb.setDamage(20);
        fireBomb.setAoeDamage(5);
        fireBomb.setExplosionRadius(4);
        return fireBomb;
    }

    @Override
    public void start() {
        isUsing = true;
        abilityTimer = 0;
        //todo: remove debug cooldown
        delay = 20 * 4; //Utils.random.nextIntBetweenInclusive(12, 23) * 20;
        mob.serverTriggerAnimation("magma_throw");
        mob.getNavigation().stop();
    }
}
