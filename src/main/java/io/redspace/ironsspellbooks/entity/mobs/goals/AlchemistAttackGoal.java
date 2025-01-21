package io.redspace.ironsspellbooks.entity.mobs.goals;

import io.redspace.ironsspellbooks.api.entity.IMagicEntity;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.util.Utils;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class AlchemistAttackGoal extends WizardAttackGoal {

    protected float throwRangeSqr;
    protected float throwRange;
    protected float potionBias;

    public AlchemistAttackGoal(IMagicEntity abstractSpellCastingMob, double pSpeedModifier, int minAttackInterval, int maxAttackInterval, float throwRange, float potionBias) {
        super(abstractSpellCastingMob, pSpeedModifier, minAttackInterval, maxAttackInterval);
        this.throwRange = throwRange;
        this.throwRangeSqr = throwRange * throwRange;
        this.spellcastingRange = throwRange - 2;
        this.spellcastingRangeSqr = spellcastingRange * spellcastingRange;
        this.potionBias = potionBias;
    }

    @Override
    public AlchemistAttackGoal setSpells(List<AbstractSpell> attackSpells, List<AbstractSpell> defenseSpells, List<AbstractSpell> movementSpells, List<AbstractSpell> supportSpells) {
        return (AlchemistAttackGoal) super.setSpells(attackSpells, defenseSpells, movementSpells, supportSpells);
    }

    @Override
    public AlchemistAttackGoal setSpellQuality(float minSpellQuality, float maxSpellQuality) {
        return (AlchemistAttackGoal) super.setSpellQuality(minSpellQuality, maxSpellQuality);
    }

    @Override
    public AlchemistAttackGoal setSingleUseSpell(AbstractSpell spellType, int minDelay, int maxDelay, int minLevel, int maxLevel) {
        return (AlchemistAttackGoal) super.setSingleUseSpell(spellType, minDelay, maxDelay, minLevel, maxLevel);
    }

    @Override
    public AlchemistAttackGoal setIsFlying() {
        return (AlchemistAttackGoal) super.setIsFlying();
    }

    public static final List<Holder<MobEffect>> ATTACK_POTIONS = List.of(MobEffects.WEAKNESS, MobEffects.BLINDNESS, MobEffects.LEVITATION, MobEffects.MOVEMENT_SLOWDOWN, MobEffects.DIG_SLOWDOWN);

    @Override
    protected void doSpellAction() {
        //Instead of taking a normal casting action, there is a chance to attempt a potion action
        if (this.mob.distanceToSqr(this.target) < throwRangeSqr && this.mob.getRandom().nextFloat() < potionBias) {
            int attackWeight = getAttackWeight();
            int supportWeight = getSupportWeight();
            ItemStack potion = new ItemStack(Items.SPLASH_POTION);
            var targetedEntity = target;
            if (hasLineOfSight && mob.getRandom().nextFloat() * (attackWeight + supportWeight) > supportWeight) {
                // We want the potion amplifier to scale with the "difficulty" of our target, with a chunk of randomness. For vanilla players, this will stick to from I-II.
                int amplifier = (mob.getRandom().nextFloat() < 0.75f ? 0 : 1) + (target.getMaxHealth() > 30 ? (mob.getRandom().nextFloat() < 0.5f ? 0 : 1) : 0);
                var effect = target.isInvertedHealAndHarm() ? MobEffects.HEAL : MobEffects.HARM;
                if (mob.getRandom().nextFloat() < 0.45f) {
                    //Effect, instead of damage
                    for (int i = 0; i < ATTACK_POTIONS.size(); i++) {
                        int p = mob.getRandom().nextInt(ATTACK_POTIONS.size());
                        if (!target.hasEffect(ATTACK_POTIONS.get(p))) {
                            effect = ATTACK_POTIONS.get(p);
                            break;
                        }
                    }
                }
                //PotionUtils.setCustomEffects(potion, List.of(new MobEffectInstance(effect, effect.isInstantenous() ? 0 : 200, amplifier)));
                //Utils.setPotion(potion, Potions.WATER); //"Empty" potion skips rendering color. "water" checks for the effect and renders properly
                potion.set(DataComponents.POTION_CONTENTS, new PotionContents(Potions.WATER).withEffectAdded(new MobEffectInstance(effect, effect.value().isInstantenous() ? 0 : 200, amplifier)));
            } else {
                Utils.setPotion(potion, Potions.STRONG_HEALING);
                targetedEntity = this.mob;
            }
            ThrownPotion thrownpotion = new ThrownPotion(this.mob.level, this.mob);
            thrownpotion.setItem(potion);
            thrownpotion.setXRot(thrownpotion.getXRot() - -20.0F);
            Vec3 vec3 = targetedEntity.getDeltaMovement();
            double d0 = targetedEntity.getX() + vec3.x - this.mob.getX();
            double d1 = targetedEntity.getEyeY() - (double) 1.1F - this.mob.getEyeY();
            double d2 = targetedEntity.getZ() + vec3.z - this.mob.getZ();
            double d3 = Math.sqrt(d0 * d0 + d2 * d2);
            thrownpotion.shoot(d0, d1 + d3 * 0.2D, d2, (float) Mth.clampedLerp(0.5f, 1.25f, this.mob.distanceToSqr(targetedEntity) / throwRangeSqr), 8.0F);
            if (!this.mob.isSilent()) {
                this.mob.level.playSound(null, this.mob.getX(), this.mob.getY(), this.mob.getZ(), SoundEvents.WITCH_THROW, this.mob.getSoundSource(), 1.0F, 0.8F + this.mob.getRandom().nextFloat() * 0.4F);
            }
            this.mob.level.addFreshEntity(thrownpotion);
            this.mob.swing(InteractionHand.MAIN_HAND, true);
        } else {
            super.doSpellAction();
        }
    }
}
