package io.redspace.ironsspellbooks.entity.mobs.wizards.fire_boss;

import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import io.redspace.ironsspellbooks.entity.mobs.goals.melee.AttackKeyframe;
import io.redspace.ironsspellbooks.entity.mobs.wizards.GenericAnimatedWarlockAttackGoal;
import io.redspace.ironsspellbooks.particle.FlameStrikeParticleOptions;
import io.redspace.ironsspellbooks.registries.SoundRegistry;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public class FireBossAttackGoal extends GenericAnimatedWarlockAttackGoal<FireBossEntity> {
    public FireBossAttackGoal(FireBossEntity abstractSpellCastingMob, double pSpeedModifier, int minAttackInterval, int maxAttackInterval) {
        super(abstractSpellCastingMob, pSpeedModifier, minAttackInterval, maxAttackInterval);
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
    public void playSwingSound() {
        mob.playSound(SoundRegistry.HELLRAZOR_SWING.get(), 1, Mth.randomBetweenInclusive(mob.getRandom(), 9, 11) * .1f);
    }
}
