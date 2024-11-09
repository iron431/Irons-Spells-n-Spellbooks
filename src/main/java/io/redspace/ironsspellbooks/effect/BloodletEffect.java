package io.redspace.ironsspellbooks.effect;

import io.redspace.ironsspellbooks.api.events.SpellOnCastEvent;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.damage.ISSDamageTypes;
import io.redspace.ironsspellbooks.registries.MobEffectRegistry;
import io.redspace.ironsspellbooks.util.ParticleHelper;
import net.minecraft.world.effect.MobEffectCategory;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;


@EventBusSubscriber
public class BloodletEffect extends MagicMobEffect {
    public BloodletEffect(MobEffectCategory pCategory, int pColor) {
        super(pCategory, pColor);
    }

    @SubscribeEvent
    public static void onSpellcast(SpellOnCastEvent event) {
        var entity = event.getEntity();
        if (!entity.level.isClientSide) {
            if (!event.getSpellId().equals(SpellRegistry.BLOODLET_SPELL.get().getSpellId()) && entity.hasEffect(MobEffectRegistry.BLOODLET)) {
                float effectiveDamage = 2 * DamageSources.getResist(entity, SpellRegistry.BLOODLET_SPELL.get().getSchoolType());
                if (entity.getHealth() > effectiveDamage) {
                    DamageSources.ignoreNextKnockback(entity);
                    entity.hurt(entity.level.damageSources().source(ISSDamageTypes.BLOODLET), effectiveDamage);
                    entity.invulnerableTime=0;
                    MagicManager.spawnParticles(entity.level, ParticleHelper.BLOOD, entity.getX(), entity.getY() + entity.getBbHeight() * .5f, entity.getZ(), 30, entity.getBbWidth() * .15f, entity.getBbHeight() * .15f, entity.getBbWidth() * .25f, .06, false);
                }
            }
        }
    }
}
