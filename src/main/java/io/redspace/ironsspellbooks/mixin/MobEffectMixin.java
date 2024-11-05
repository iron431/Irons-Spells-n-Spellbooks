package io.redspace.ironsspellbooks.mixin;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.PoisonMobEffect;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PoisonMobEffect.class)
public class MobEffectMixin {
    @Inject(method = "applyEffectTick", at = @At(value = "HEAD"))
    private void markPoisoned(LivingEntity pLivingEntity, int pAmplifier, CallbackInfoReturnable<Boolean> cir) {
        if (pLivingEntity instanceof ServerPlayer serverPlayer) {
            MagicData.getPlayerMagicData(serverPlayer).markPoisoned();
        }
    }
}
