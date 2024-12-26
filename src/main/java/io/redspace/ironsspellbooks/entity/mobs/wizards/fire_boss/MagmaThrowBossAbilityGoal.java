package io.redspace.ironsspellbooks.entity.mobs.wizards.fire_boss;

import io.redspace.ironsspellbooks.api.entity.IMagicEntity;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.entity.mobs.IAnimatedAttacker;
import io.redspace.ironsspellbooks.entity.spells.magma_ball.FireBomb;
import net.minecraft.util.Mth;
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

    public static final int ANIM_DURATION = (int) (1.67 * 20);
    public static final int ACTION_TIMESTAMP = (int) (1.38 * 20);

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
//                        double v0 = 0.9;
                        double v0 = Mth.lerp(Math.clamp((mob.distanceToSqr(entity) - 3) * .083, 0, 1), .3, 1.2);
                        Vec3 horizontal = entity.position().subtract(mob.position()).multiply(1, 0, 1);
                        Vec3 trajectory;
                        if (horizontal.lengthSqr() < 0.1 * 0.1) {
                            //super-close shot, just lob it up
                            trajectory = new Vec3(0, v0 * .5, 0);

                        } else {
                            double y1 = entity.getY() - mob.getY();
                            double gravity = fireBomb.getGravity();
                            // y(t) = -1/2(g)(t^2) + v0*t
                            // => 0 = -0.5g(t1^2) + v0(t1) - y1
                            // => t1 = positive solution of quadratic formula
                            double ticksInAir;
                            if (Math.abs(y1) < 0.25) {
                                // approx. c = 0 (=> x = -b/a)
                                ticksInAir = -v0 / (-0.5 * gravity);
                            } else {
                                //quadratic formula
                                var discriminant = v0 * v0 - (4 * -0.5 * gravity * -y1);
                                if (discriminant < 0) {
                                    //non-real solution
                                    continue;
                                }
                                ticksInAir = (-v0 + Math.sqrt(discriminant)) / (2 * gravity);
                            }
                            if (ticksInAir < 1) {
                                // nonsensical trajectory
                                continue;
                            }
                            var estMotion = entity.getDeltaMovement().multiply(1, 0, 1).scale(ticksInAir * 0.5);
                            trajectory = horizontal.add(estMotion).scale(1 / ticksInAir).add(0, v0, 0);
                        }
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
        delay = Utils.random.nextIntBetweenInclusive(12, 23) * 20;
        mob.serverTriggerAnimation("magma_throw");
        mob.getNavigation().stop();
    }
}
