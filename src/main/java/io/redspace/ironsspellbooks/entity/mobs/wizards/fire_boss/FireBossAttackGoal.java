package io.redspace.ironsspellbooks.entity.mobs.wizards.fire_boss;

import io.redspace.ironsspellbooks.IronsSpellbooks;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import io.redspace.ironsspellbooks.entity.mobs.goals.melee.AttackKeyframe;
import io.redspace.ironsspellbooks.entity.mobs.wizards.GenericAnimatedWarlockAttackGoal;
import io.redspace.ironsspellbooks.particle.FlameStrikeParticleOptions;
import io.redspace.ironsspellbooks.registries.SoundRegistry;
import io.redspace.ironsspellbooks.util.ParticleHelper;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.phys.Vec3;

public class FireBossAttackGoal extends GenericAnimatedWarlockAttackGoal<FireBossEntity> {
    private static final AttributeModifier MODIFIER_FIRE_BALLER = new AttributeModifier(IronsSpellbooks.id("fireballer"), 0.50, AttributeModifier.Operation.ADD_VALUE);

    public FireBossAttackGoal(FireBossEntity abstractSpellCastingMob, double pSpeedModifier, int minAttackInterval, int maxAttackInterval) {
        super(abstractSpellCastingMob, pSpeedModifier, minAttackInterval, maxAttackInterval);
    }

    @Override
    protected void doMovement(double distanceSquared) {
        double speed = (spellCastingMob.isCasting() ? .75f : 1f) * movementSpeed();
        mob.lookAt(target, 30, 30);
        var meleeRange = meleeRange();
        float strafeMultiplier = getStrafeMultiplier();
        if (distanceSquared < spellcastingRangeSqr && seeTime >= 5) {
            // we are within normal ranges and have los. do local movements
            this.mob.getNavigation().stop();
            if (++strafeTime > 40) {
                if (mob.getRandom().nextDouble() < .08) {
                    strafingClockwise = !strafingClockwise;
                    strafeTime = 0;
                }
            }
            float strafeForward = meleeMoveSpeedModifier;
            if (distanceSquared > meleeRange * meleeRange * 3 * 3) {
                // we are really far: close distance
                strafeForward *= 2f;
            } else if (distanceSquared > meleeRange * meleeRange * .75 * .75) {
                // we are not too far, and not super close: advance forward
                strafeForward *= 1.3f;
            } else {
                // we are super close: back up
                strafeForward *= -1.15f;
            }
            int strafeDir = strafingClockwise ? 1 : -1;
            mob.getMoveControl().strafe(strafeForward * strafeMultiplier, (float) speed * strafeDir * strafeMultiplier);
        } else {
            // no los or we are completely out of range, path towards target
            if (mob.tickCount % 5 == 0) {
                this.mob.setXxa(0); //manually cancel strafe
                this.mob.getNavigation().moveTo(this.target, speedModifier);
            }
        }
    }

    public void setTarget(LivingEntity target) {
        this.target = target;
    }

    @Override
    protected void onHitFrame(AttackKeyframe attackKeyframe, float meleeRange) {
        super.onHitFrame(attackKeyframe, meleeRange);
        if (attackKeyframe instanceof FireBossAttackKeyframe fireKeyframe) {
            boolean mirrored = fireKeyframe.swingData.mirrored();
            boolean vertical = fireKeyframe.swingData.vertical();
            Vec3 forward = mob.getForward();
            float reach = 2 * mob.getScale();
            Vec3 hitLocation = mob.getBoundingBox().getCenter().add(mob.getForward().multiply(reach, 0.5, reach));
            MagicManager.spawnParticles(mob.level,
                    new FlameStrikeParticleOptions((float) forward.x, (float) forward.y, (float) forward.z, mirrored, vertical, mob.getScale()), hitLocation.x, hitLocation.y, hitLocation.z, 1, 0, 0, 0, 0, true);
        }
    }

    @Override
    public void stop() {
        super.stop();
        mob.getAttribute(AttributeRegistry.CAST_TIME_REDUCTION).removeModifier(MODIFIER_FIRE_BALLER);
    }

    int fireballcooldown;

    @Override
    protected void handleAttackLogic(double distanceSquared) {
        var meleeRange = meleeRange();
        if (fireballcooldown > 0) {
            // poor man's way to clean up the fireball attribute
            if (fireballcooldown == 20 * 10 - 20) {
                mob.getAttribute(AttributeRegistry.CAST_TIME_REDUCTION).removeModifier(MODIFIER_FIRE_BALLER);
            }
            fireballcooldown--;
        } else {
            // if we are very ranged (and preferably high in the sky) launch down a fireball
            if (!mob.onGround() && distanceSquared > meleeRange * meleeRange * 2 * 2) {
                if (!isActing()) {
                    // insta-cast that fireball
                    mob.getAttribute(AttributeRegistry.CAST_TIME_REDUCTION).addOrUpdateTransientModifier(MODIFIER_FIRE_BALLER);
                    mob.initiateCastSpell(SpellRegistry.FIREBALL_SPELL.get(), 5);
                    fireballcooldown = 20 * 10;
                    return;
                }
            }
        }
        if (meleeAnimTimer > 0 && currentAttack != null) {
            // in order to make more seamless animation -> action transitions, we cut our ai pause short if we have encountered our last frame
            // the animation still plays out, creating a smoother overall transition
            int shortcut = 5;
            if (meleeAnimTimer < shortcut) {
                if (currentAttack.attacks.keySet().intStream().noneMatch(i -> i > currentAttack.lengthInTicks - shortcut)) {
                    meleeAnimTimer = 0;
                }
            }
        }
        super.handleAttackLogic(distanceSquared);
    }


    @Override
    protected void doMeleeAction() {
        super.doMeleeAction();
        if (currentAttack != null) {
            float r = meleeRange();
            if (mob.distanceToSqr(target) > .75 * .75 * r * r) {
                //only proc charge if we are farther than 75% of melee range
                int i = currentAttack.attacks.keySet().intStream().sorted().findFirst().orElse(0);
                mob.getMoveControl().triggerCustomMovement(i + 5, f -> new Vec3(0, 0, 0.5 * (1 + currentAttack.rangeMultiplier)));
            }
        }
    }

    @Override
    protected double movementSpeed() {
        return this.meleeMoveSpeedModifier;
    }

    @Override
    public void playSwingSound() {
        mob.playSound(SoundRegistry.HELLRAZOR_SWING.get(), 1, Mth.randomBetweenInclusive(mob.getRandom(), 9, 11) * .1f);
    }
}
