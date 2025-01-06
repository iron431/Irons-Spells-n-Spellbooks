package io.redspace.ironsspellbooks.effect;

import io.redspace.ironsspellbooks.entity.mobs.IMagicSummon;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

public class SummonTimer extends MobEffect implements IMobEffectEndCallback {
    public SummonTimer(MobEffectCategory pCategory, int pColor) {
        super(pCategory, pColor);
    }

    @Override
    public void onEffectRemoved(LivingEntity pLivingEntity, int pAmplifier) {
        if (pLivingEntity instanceof IMagicSummon summon && !pLivingEntity.isDeadOrDying()&&!pLivingEntity.isRemoved()) {
            summon.onUnSummon();
        }
    }
}
