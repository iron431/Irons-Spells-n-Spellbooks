package io.redspace.ironsspellbooks.entity.mobs.goals;

import io.redspace.ironsspellbooks.api.entity.IMagicEntity;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.List;

public class WarlockAttackGoal extends WizardAttackGoal {

    protected boolean wantsToMelee;
    protected int meleeTime;
    protected int meleeDecisionTime;
    protected float meleeBiasMin;
    protected float meleeBiasMax;
    protected float meleeMoveSpeedModifier;
    protected int meleeAttackIntervalMin;
    protected int meleeAttackIntervalMax;
    protected int meleeAttackDelay = -1;

    public WarlockAttackGoal(IMagicEntity abstractSpellCastingMob, double pSpeedModifier, int minAttackInterval, int maxAttackInterval) {
        super(abstractSpellCastingMob, pSpeedModifier, minAttackInterval, maxAttackInterval);
        this.meleeDecisionTime = mob.getRandom().nextIntBetweenInclusive(80, 200);
        this.meleeBiasMin = .25f;
        this.meleeBiasMax = .75f;
        this.allowFleeing = false;
        this.meleeMoveSpeedModifier = (float) pSpeedModifier;
        this.meleeAttackIntervalMin = minAttackInterval;
        this.meleeAttackIntervalMax = maxAttackInterval;
    }

    @Override
    public void tick() {
        super.tick();
        if (++meleeTime > meleeDecisionTime) {
            meleeTime = 0;
            wantsToMelee = mob.getRandom().nextFloat() <= meleeBias();
            meleeDecisionTime = mob.getRandom().nextIntBetweenInclusive(60, 120);
        }
    }

    public float meleeRange() {
        return (float) (mob.getAttributeValue(Attributes.ENTITY_INTERACTION_RANGE) * mob.getScale());
    }

    protected float meleeBias() {
        return Mth.clampedLerp(meleeBiasMin, meleeBiasMax, mob.getHealth() / mob.getMaxHealth());
    }

    @Override
    protected void doMovement(double distanceSquared) {
        if (!wantsToMelee) {
            super.doMovement(distanceSquared);
            return;
        }
        if (target.isDeadOrDying()) {
            this.mob.getNavigation().stop();
        } else {
            var meleeRange = meleeRange();
            mob.lookAt(target, 30, 30);
            float strafeForwards;
            float speed = (float) movementSpeed();
            if (distanceSquared > meleeRange * meleeRange) {
                mob.setXxa(0); // manually override strafe control before we set navigation
                if (mob.tickCount % 5 == 0) {
                    this.mob.getNavigation().moveTo(this.target, meleeMoveSpeedModifier);
                }
            } else {
                this.mob.getNavigation().stop();
                strafeForwards = .5f * meleeMoveSpeedModifier * (4 * distanceSquared > meleeRange * meleeRange ? 1.5f : -1);
                //we do a little strafing
                if (++strafeTime > 25) {
                    if (mob.getRandom().nextDouble() < .1) {
                        strafingClockwise = !strafingClockwise;
                        strafeTime = 0;
                    }
                }
                float strafeDir = strafingClockwise ? 1f : -1f;
                mob.getMoveControl().strafe(strafeForwards, speed * strafeDir);
            }
            //helps with head alignment? for some reason mobs just cannot align their head and body and target for their fucking life
            //update: that is due to pathfinding only working for 45 degree angles, meaning mobs must staircase their diagonal movement without manual intervention (see NotIdioticGroundNavigation)
            mob.getLookControl().setLookAt(target);
        }
    }

    @Override
    public void stop() {
        super.stop();
        meleeAttackDelay = -1;
    }

    @Override
    protected void handleAttackLogic(double distanceSquared) {
        var meleeRange = meleeRange();
        if (!wantsToMelee || distanceSquared > meleeRange * meleeRange || spellCastingMob.isCasting()) {
            super.handleAttackLogic(distanceSquared);
        } else if (--this.meleeAttackDelay <= 0) {
            this.mob.swing(InteractionHand.MAIN_HAND);
            doMeleeAction();
        }

    }

    protected void doMeleeAction() {
        double distanceSquared = this.mob.distanceToSqr(this.target.getX(), this.target.getY(), this.target.getZ());
        this.mob.doHurtTarget(target);
        resetMeleeAttackInterval(distanceSquared);
    }

    public WarlockAttackGoal setMeleeBias(float meleeBiasMin, float meleeBiasMax) {
        this.meleeBiasMin = meleeBiasMin;
        this.meleeBiasMax = meleeBiasMax;
        return this;
    }

    @Override
    public WarlockAttackGoal setSpells(List<AbstractSpell> attackSpells, List<AbstractSpell> defenseSpells, List<AbstractSpell> movementSpells, List<AbstractSpell> supportSpells) {
        return (WarlockAttackGoal) super.setSpells(attackSpells, defenseSpells, movementSpells, supportSpells);
    }

    @Override
    public WarlockAttackGoal setSpellQuality(float minSpellQuality, float maxSpellQuality) {
        return (WarlockAttackGoal) super.setSpellQuality(minSpellQuality, maxSpellQuality);
    }

    @Override
    public WarlockAttackGoal setSingleUseSpell(AbstractSpell spellType, int minDelay, int maxDelay, int minLevel, int maxLevel) {
        return (WarlockAttackGoal) super.setSingleUseSpell(spellType, minDelay, maxDelay, minLevel, maxLevel);
    }

    @Override
    public WarlockAttackGoal setIsFlying() {
        return (WarlockAttackGoal) super.setIsFlying();
    }

    public WarlockAttackGoal setMeleeMovespeedModifier(float meleeMovespeedModifier) {
        this.meleeMoveSpeedModifier = meleeMovespeedModifier;
        return this;
    }

    public WarlockAttackGoal setMeleeAttackInverval(int min, int max) {
        this.meleeAttackIntervalMax = max;
        this.meleeAttackIntervalMin = min;
        return this;
    }

    @Override
    protected double movementSpeed() {
        //fixme: move control already reads speed attribute, we should not be basing speed modifier based on it as well
        return wantsToMelee ? meleeMoveSpeedModifier * mob.getAttributeValue(Attributes.MOVEMENT_SPEED) * 2 : super.movementSpeed();
    }

    protected void resetMeleeAttackInterval(double distanceSquared) {
        float f = (float) Math.sqrt(distanceSquared) / this.spellcastingRange;
        this.meleeAttackDelay = Math.max(1, Mth.floor(f * (float) (this.meleeAttackIntervalMax - this.meleeAttackIntervalMin) + (float) this.meleeAttackIntervalMin));
    }
}
