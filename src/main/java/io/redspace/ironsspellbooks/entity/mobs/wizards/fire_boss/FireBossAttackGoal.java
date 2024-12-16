package io.redspace.ironsspellbooks.entity.mobs.wizards.fire_boss;

import io.redspace.ironsspellbooks.entity.mobs.wizards.GenericAnimatedWarlockAttackGoal;
import io.redspace.ironsspellbooks.entity.spells.flame_strike.FlameStrike;
import net.minecraft.world.phys.Vec3;

public class FireBossAttackGoal extends GenericAnimatedWarlockAttackGoal<FireBossEntity> {
    public FireBossAttackGoal(FireBossEntity abstractSpellCastingMob, double pSpeedModifier, int minAttackInterval, int maxAttackInterval) {
        super(abstractSpellCastingMob, pSpeedModifier, minAttackInterval, maxAttackInterval);
    }

    @Override
    protected void onHitFrame(float meleeRange) {
        super.onHitFrame(meleeRange);
        boolean mirrored = false;
        FlameStrike flameStrike = new FlameStrike(mob.level, mirrored);
        Vec3 hitLocation = mob.getEyePosition().add(mob.getForward().scale(meleeRange*.5f));
        flameStrike.moveTo(hitLocation);
        flameStrike.setYRot(mob.getYRot());
        flameStrike.setXRot(mob.getXRot());
        mob.level.addFreshEntity(flameStrike);
    }
}
