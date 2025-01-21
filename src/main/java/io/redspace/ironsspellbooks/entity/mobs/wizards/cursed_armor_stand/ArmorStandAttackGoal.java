package io.redspace.ironsspellbooks.entity.mobs.wizards.cursed_armor_stand;

import io.redspace.ironsspellbooks.entity.mobs.goals.melee.AttackAnimationData;
import io.redspace.ironsspellbooks.entity.mobs.wizards.GenericAnimatedWarlockAttackGoal;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;

public class ArmorStandAttackGoal extends GenericAnimatedWarlockAttackGoal<CursedArmorStandEntity> {
    public static final float PROTECTION_RANGE = 18;
    public static final float PROTECTION_RANGE_SQR = PROTECTION_RANGE * PROTECTION_RANGE;

    public ArmorStandAttackGoal(CursedArmorStandEntity abstractSpellCastingMob, double pSpeedModifier, int minAttackInterval, int maxAttackInterval) {
        super(abstractSpellCastingMob, pSpeedModifier, minAttackInterval, maxAttackInterval);
    }

    @Override
    protected AttackAnimationData getNextAttack(float distanceSquared) {
        if (moveList.isEmpty()) {
            return null;
        }
        //fixme: this is dumb and dangerous. need intelligent selection logic eventually
        if (mob.getMainHandItem().isEmpty()) {
            return moveList.get(1); // the second animation is the one that looks good with no item in hand
        }
        return super.getNextAttack(distanceSquared);
    }

    @Override
    public void playSwingSound() {
        if (mob.getMainHandItem().isEmpty()) {
            //emulate fist-swing sound
            mob.playSound(SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1, Mth.randomBetweenInclusive(mob.getRandom(), 9, 11) * .1f);
        } else {
            super.playSwingSound();
        }
    }

    @Override
    protected void doMovement(double distanceSquared) {
        if (mob.spawn != null) {
            var boundaryDistanceSqr = mob.spawn.distanceToSqr(target.position());
            if (boundaryDistanceSqr > PROTECTION_RANGE_SQR) {
                wantsToMelee = false;
                if (boundaryDistanceSqr > (PROTECTION_RANGE + 6) * (PROTECTION_RANGE + 6)) {
                    mob.stopBeingAngry();
                    mob.setLastHurtByMob(null);
                    mob.setLastHurtByPlayer(null);
                    mob.setPersistentAngerTarget(null);
                    stop();
                    return;
                }
            } else {
                wantsToMelee = true;
            }
        }
        super.doMovement(distanceSquared);
    }

    @Override
    public boolean canUse() {
        return super.canUse();
    }
}
