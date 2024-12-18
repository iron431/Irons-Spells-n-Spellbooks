package io.redspace.ironsspellbooks.entity.mobs.wizards.cultist;

import io.redspace.ironsspellbooks.IronsSpellbooks;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.entity.mobs.IAnimatedAttacker;
import io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob.AbstractSpellCastingMob;
import io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob.NeutralWizard;
import io.redspace.ironsspellbooks.entity.mobs.goals.melee.AttackAnimationData;
import io.redspace.ironsspellbooks.entity.mobs.goals.PatrolNearLocationGoal;
import io.redspace.ironsspellbooks.entity.mobs.goals.SpellBarrageGoal;
import io.redspace.ironsspellbooks.entity.mobs.goals.WizardRecoverGoal;
import io.redspace.ironsspellbooks.entity.mobs.wizards.GenericAnimatedWarlockAttackGoal;
import io.redspace.ironsspellbooks.entity.mobs.wizards.fire_boss.NotIdioticNavigation;
import io.redspace.ironsspellbooks.registries.ItemRegistry;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.LookControl;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.ResetUniversalAngerTargetGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.*;

import javax.annotation.Nullable;
import java.util.List;

public class CultistEntity extends NeutralWizard implements Enemy, IAnimatedAttacker {

    public CultistEntity(EntityType<? extends AbstractSpellCastingMob> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        xpReward = 25;
        this.lookControl = createLookControl();
        this.moveControl = createMoveControl();
    }

    protected LookControl createLookControl() {
        return new LookControl(this) {
            //This allows us to more rapidly turn towards our target. Helps to make sure his targets are aligned with his swing animations
            @Override
            protected float rotateTowards(float pFrom, float pTo, float pMaxDelta) {
                return super.rotateTowards(pFrom, pTo, pMaxDelta * 2.5f);
            }

            @Override
            protected boolean resetXRotOnTick() {
                return getTarget() == null;
            }
        };
    }

    protected MoveControl createMoveControl() {
        return new MoveControl(this) {
            //This fixes a bug where a mob tries to path into the block it's already standing, and spins around trying to look "forward"
            //We nullify our rotation calculation if we are close to block we are trying to get to
            @Override
            protected float rotlerp(float pSourceAngle, float pTargetAngle, float pMaximumChange) {
                double d0 = this.wantedX - this.mob.getX();
                double d1 = this.wantedZ - this.mob.getZ();
                if (d0 * d0 + d1 * d1 < .5f) {
                    return pSourceAngle;
                } else {
                    return super.rotlerp(pSourceAngle, pTargetAngle, pMaximumChange * .25f);
                }
            }
        };
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new SpellBarrageGoal(this, SpellRegistry.DEVOUR_SPELL.get(), 3, 6, 100, 250, 1));
        this.goalSelector.addGoal(3, new GenericAnimatedWarlockAttackGoal<>(this, 1.25f, 50, 75)
                .setMoveset(List.of(
                        new AttackAnimationData(9, "simple_sword_upward_swipe", 5),
                        new AttackAnimationData(8, "simple_sword_lunge_stab", 6),
                        new AttackAnimationData(10, "simple_sword_stab_alternate", 8),
                        new AttackAnimationData(10, "simple_sword_horizontal_cross_swipe", 8)
                ))
                .setComboChance(.4f)
                .setMeleeAttackInverval(10, 30)
                .setMeleeMovespeedModifier(1.5f)
                .setSpells(
                        List.of(SpellRegistry.BLOOD_NEEDLES_SPELL.get(), SpellRegistry.BLOOD_NEEDLES_SPELL.get(), SpellRegistry.WITHER_SKULL_SPELL.get(), SpellRegistry.BLOOD_SLASH_SPELL.get()),
                        List.of(SpellRegistry.RAY_OF_SIPHONING_SPELL.get()),
                        List.of(/*SpellRegistry.BLOOD_STEP_SPELL.get()*/),
                        List.of()
                ).setDrinksPotions()

        );
        this.goalSelector.addGoal(4, new PatrolNearLocationGoal(this, 30, .75f));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(10, new WizardRecoverGoal(this));
        //this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        //this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false, this::isHostileTowards));
        this.targetSelector.addGoal(5, new ResetUniversalAngerTargetGoal<>(this, false));
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor pLevel, DifficultyInstance pDifficulty, MobSpawnType pReason, @Nullable SpawnGroupData pSpawnData) {
        RandomSource randomsource = Utils.random;
        this.populateDefaultEquipmentSlots(randomsource, pDifficulty);
        return super.finalizeSpawn(pLevel, pDifficulty, pReason, pSpawnData);
    }

    @Override
    protected void populateDefaultEquipmentSlots(RandomSource pRandom, DifficultyInstance pDifficulty) {
        this.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ItemRegistry.CULTIST_HELMET.get()));
        this.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ItemRegistry.CULTIST_CHESTPLATE.get()));
        this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ItemRegistry.MISERY.get()));
        this.setDropChance(EquipmentSlot.HEAD, 0);
        this.setDropChance(EquipmentSlot.CHEST, 0);
        this.setDropChance(EquipmentSlot.MAINHAND, 0);
    }

    public static AttributeSupplier.Builder prepareAttributes() {
        return LivingEntity.createLivingAttributes()
                .add(Attributes.ATTACK_DAMAGE, 3.0)
                .add(Attributes.ATTACK_KNOCKBACK, 0.0)
                .add(Attributes.MAX_HEALTH, 60.0)
                .add(Attributes.FOLLOW_RANGE, 24.0)
                .add(Attributes.ENTITY_INTERACTION_RANGE, 3)
                .add(Attributes.MOVEMENT_SPEED, .25);
    }

    @Override
    public boolean shouldSheathSword() {
        return true;
    }

    RawAnimation animationToPlay = null;
    private final AnimationController<CultistEntity> meleeController = new AnimationController<>(this, "keeper_animations", 0, this::predicate);

    @Override
    public void playAnimation(String animationId) {
        try {
            animationToPlay = RawAnimation.begin().thenPlay(animationId);
        } catch (Exception ignored) {
            IronsSpellbooks.LOGGER.error("Entity {} Failed to play animation: {}", this, animationId);
        }
    }

    private PlayState predicate(AnimationState<CultistEntity> animationEvent) {
        var controller = animationEvent.getController();

        if (this.animationToPlay != null) {
            controller.forceAnimationReset();
            controller.setAnimation(animationToPlay);
            animationToPlay = null;
        }
        return PlayState.CONTINUE;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {
        controllerRegistrar.add(meleeController);
        super.registerControllers(controllerRegistrar);
    }

    @Override
    public boolean isAnimating() {
        return meleeController.getAnimationState() != AnimationController.State.STOPPED || super.isAnimating();
    }

    @Override
    public boolean guardsBlocks() {
        return false;
    }

    @Override
    public boolean isHostileTowards(LivingEntity pTarget) {
        return super.isHostileTowards(pTarget) || pTarget.getAttributeValue(AttributeRegistry.BLOOD_SPELL_POWER) < 1.15;
    }

    @Override
    protected PathNavigation createNavigation(Level pLevel) {
        return new NotIdioticNavigation(this, pLevel);
    }
}
