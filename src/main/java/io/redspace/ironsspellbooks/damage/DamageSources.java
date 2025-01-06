package io.redspace.ironsspellbooks.damage;

import io.redspace.ironsspellbooks.api.entity.NoKnockbackProjectile;
import io.redspace.ironsspellbooks.api.events.SpellDamageEvent;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.entity.mobs.IMagicSummon;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;

import java.util.HashMap;
import java.util.UUID;

//https://github.com/cleannrooster/Spellblade-1.19.2/search?q=MobEffect
//https://github.com/LittleEzra/Augment-1.19.2/blob/334dc95462a3e6b25e6f73d3d909d012d63be109/src/main/java/com/littleezra/augment/item/enchantment/RecoilCurseEnchantment.java
//DamageSource
//StatusEffect
//MobEffect: https://forge.gemwire.uk/wiki/Mob_Effects/1.18

@EventBusSubscriber
public class DamageSources {
    public static DamageSource get(Level level, ResourceKey<DamageType> damageType) {
        return level.damageSources().source(damageType);
    }

    public static Holder<DamageType> getHolderFromResource(Entity entity, ResourceKey<DamageType> damageTypeResourceKey) {
        var option = entity.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolder(damageTypeResourceKey);
        if (option.isPresent()) {
            return option.get();
        } else {
            return entity.level().damageSources().genericKill().typeHolder();
        }
    }

    public static boolean applyDamage(Entity target, float baseAmount, DamageSource damageSource) {
        if (target instanceof LivingEntity livingTarget && damageSource instanceof SpellDamageSource spellDamageSource) {
            var e = new SpellDamageEvent(livingTarget, baseAmount, spellDamageSource);
            if (NeoForge.EVENT_BUS.post(e).isCanceled()) {
                return false;
            }
            baseAmount = e.getAmount();
            float adjustedDamage = baseAmount * getResist(livingTarget, spellDamageSource.spell.getSchoolType());
            IMagicSummon fromSummon = damageSource.getDirectEntity() instanceof IMagicSummon summon ? summon : damageSource.getEntity() instanceof IMagicSummon summon ? summon : null;
            if (fromSummon != null) {
                if (fromSummon.getSummoner() != null) {
                    adjustedDamage *= (float) fromSummon.getSummoner().getAttributeValue(AttributeRegistry.SUMMON_DAMAGE);
                }
            } else if (damageSource.getDirectEntity() instanceof NoKnockbackProjectile) {
                ignoreNextKnockback(livingTarget);
            }
            if (damageSource.getEntity() instanceof LivingEntity livingAttacker) {
                if (isFriendlyFireBetween(livingAttacker, livingTarget)) {
                    return false;
                }
                livingAttacker.setLastHurtMob(target);
            }
            var flag = livingTarget.hurt(damageSource, adjustedDamage);
            if (fromSummon instanceof LivingEntity livingSummon) {
                livingTarget.setLastHurtByMob(livingSummon);
            }
            return flag;
        } else {
            return target.hurt(damageSource, baseAmount);
        }
    }

    private static final HashMap<UUID, Integer> knockbackImmunes = new HashMap<>();

    public static void ignoreNextKnockback(LivingEntity livingEntity) {
        if (livingEntity.getServer() != null) {
            var tickCount = livingEntity.getServer().getTickCount();
            //help manage memory
            knockbackImmunes.entrySet().removeIf(entry -> tickCount - entry.getValue() >= 10);
            //enter entity
            knockbackImmunes.put(livingEntity.getUUID(), tickCount);
        }
    }

    @SubscribeEvent
    public static void cancelKnockback(LivingKnockBackEvent event) {
        //IronsSpellbooks.LOGGER.debug("DamageSources.cancelKnockback {}", event.getEntity().getName().getString());
        var entity = event.getEntity();
        if (entity.getServer() != null && knockbackImmunes.containsKey(event.getEntity().getUUID())) {
            if (entity.getServer().getTickCount() - knockbackImmunes.get(entity.getUUID()) <= 1) {
                event.setCanceled(true);
            }
            knockbackImmunes.remove(entity.getUUID());
        }
    }

    @SubscribeEvent
    public static void postHitEffects(LivingDamageEvent.Post event) {
        if (event.getSource() instanceof SpellDamageSource spellDamageSource && spellDamageSource.hasPostHitEffects()) {
            float actualDamage = event.getNewDamage();
            var target = event.getEntity();
            var attacker = event.getSource().getEntity();
            if (attacker instanceof LivingEntity livingAttacker) {
                if (spellDamageSource.getLifestealPercent() > 0) {
                    livingAttacker.heal(spellDamageSource.getLifestealPercent() * actualDamage);
                }
            }
            if (spellDamageSource.getFreezeTicks() > 0 && target.canFreeze()) {
                //Freeze ticks count down by 2, so we * 2 so the spell damages source can be dumb
                target.setTicksFrozen(target.getTicksFrozen() + spellDamageSource.getFreezeTicks() * 2);
            }
            if (spellDamageSource.getFireTime() > 0 && target instanceof LivingEntity) {
                target.igniteForTicks(spellDamageSource.getFireTime());
            }
        }
    }

    public static boolean isFriendlyFireBetween(Entity attacker, Entity target) {
        if (attacker == null || target == null)
            return false;
        if (attacker.isPassengerOfSameVehicle(target)) {
            return true;
        }
        var team = attacker.getTeam();
        if (team != null) {
            return team.isAlliedTo(target.getTeam()) && !team.isAllowFriendlyFire();
        }
        //We already manually checked for teams, so this will only return true for any overrides (such as summons)
        return attacker.isAlliedTo(target);
    }

    /**
     * Returns the resistance multiplier of the entity. (If they are resistant, the value is < 1)
     */
    public static float getResist(LivingEntity entity, SchoolType damageSchool) {
        var baseResist = entity.getAttributeValue(AttributeRegistry.SPELL_RESIST);
        if (damageSchool == null)
            return 2 - (float) Utils.softCapFormula(baseResist);
        else
            return 2 - (float) Utils.softCapFormula(damageSchool.getResistanceFor(entity) * baseResist);
    }
}
