package io.redspace.ironsspellbooks.entity.mobs.wizards.fire_boss;

import io.redspace.ironsspellbooks.IronsSpellbooks;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.entity.mobs.IAnimatedAttacker;
import io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob.AbstractSpellCastingMob;
import io.redspace.ironsspellbooks.entity.mobs.dead_king_boss.DeadKingBoss;
import io.redspace.ironsspellbooks.entity.mobs.goals.AttackAnimationData;
import io.redspace.ironsspellbooks.entity.mobs.goals.PatrolNearLocationGoal;
import io.redspace.ironsspellbooks.entity.mobs.wizards.GenericAnimatedWarlockAttackGoal;
import io.redspace.ironsspellbooks.network.SyncAnimationPacket;
import io.redspace.ironsspellbooks.registries.ItemRegistry;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.LookControl;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.*;

import javax.annotation.Nullable;
import java.util.List;

public class FireBossEntity extends AbstractSpellCastingMob implements Enemy, IAnimatedAttacker {

    public FireBossEntity(EntityType<? extends AbstractSpellCastingMob> pEntityType, Level pLevel) {
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

    private final ServerBossEvent bossEvent = (ServerBossEvent) (new ServerBossEvent(this.getDisplayName(), BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.PROGRESS)).setDarkenScreen(true);

    public void startSeenByPlayer(ServerPlayer pPlayer) {
        super.startSeenByPlayer(pPlayer);
        this.bossEvent.addPlayer(pPlayer);
//        PacketDistributor.sendToPlayer(pPlayer, new EntityEventPacket<DeadKingBoss>(this, START_MUSIC));
    }

    public void stopSeenByPlayer(ServerPlayer pPlayer) {
        super.stopSeenByPlayer(pPlayer);
        this.bossEvent.removePlayer(pPlayer);
//        PacketDistributor.sendToPlayer(pPlayer, new EntityEventPacket<DeadKingBoss>(this, STOP_MUSIC));
    }

    GenericAnimatedWarlockAttackGoal<?> attackGoal;

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.attackGoal = (GenericAnimatedWarlockAttackGoal<?>) new GenericAnimatedWarlockAttackGoal<>(this, 1.25f, 50, 75)
                .setMoveset(List.of(
                        AttackAnimationData.builder("scythe_backpedal")
                                .length(40)
                                .attacks(
                                        new AttackAnimationData.AttackKeyframe(20, new Vec3(0, .3, -2))
                                )
                                .build(),
                        AttackAnimationData.builder("scythe_low_rightward_sweep")
                                .length(40)
                                .area(0.25f)
                                .attacks(
                                        new AttackAnimationData.AttackKeyframe(20, new Vec3(0, .1, 0.8))
                                )
                                .build(),
                        AttackAnimationData.builder("scythe_sideslash_downslash")
                                .length(54)
                                .attacks(18, 32)
                                .build(),
                        AttackAnimationData.builder("scythe_jump_combo")
                                .length(45)
                                .cancellable()
                                .attacks(
                                        new AttackAnimationData.AttackKeyframe(20, new Vec3(0, 1, 0), new Vec3(0, 1.15, .1)),
                                        new AttackAnimationData.AttackKeyframe(35, new Vec3(0, 0, -.2), new Vec3(0, 0, 0.5))
                                )
                                .build(),
                        AttackAnimationData.builder("scythe_downslash_pull")
                                .length(60)
                                .cancellable()
                                .attacks(
                                        new AttackAnimationData.AttackKeyframe(22, new Vec3(0, 0, .5f), new Vec3(0, -.2, 0)),
                                        new AttackAnimationData.AttackKeyframe(38, new Vec3(0, .2, -0.8), new Vec3(0, .3, -1.8))
                                )
                                .build(),
                        AttackAnimationData.builder("scythe_horizontal_slash_spin")
                                .length(45)
                                .area(0.25f)
                                .attacks(
                                        new AttackAnimationData.AttackKeyframe(16, new Vec3(0, 0, -0.5), new Vec3(0, .1, -1)),
                                        new AttackAnimationData.AttackKeyframe(36, new Vec3(0, 0, 1), new Vec3(0, .3, 1))
                                )
                                .build()

                ))
                .setComboChance(.4f)
                .setMeleeAttackInverval(0, 30)
                .setMeleeBias(1f, 1f)
                .setSpells(
                        List.of(SpellRegistry.FIRE_ARROW_SPELL.get(), SpellRegistry.SCORCH_SPELL.get()),
                        List.of(),
                        List.of(),
                        List.of()
                );
        this.goalSelector.addGoal(3, attackGoal);

        this.goalSelector.addGoal(4, new PatrolNearLocationGoal(this, 30, .75f));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        //this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        //this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Pig.class, true));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, DeadKingBoss.class, true));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void tick() {
        super.tick();
        this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
    }

    @Override
    public void calculateEntityAnimation(boolean pIncludeHeight) {
        super.calculateEntityAnimation(false);
    }

    @Override
    protected void updateWalkAnimation(float f) {
        //reduce walk animation swing if we are floating or meleeing
        super.updateWalkAnimation(f * ((!this.onGround() || attackGoal.isMeleeing()) ? .3f : .9f));
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor pLevel, DifficultyInstance pDifficulty, MobSpawnType pReason, @Nullable SpawnGroupData pSpawnData) {
        RandomSource randomsource = Utils.random;
        this.populateDefaultEquipmentSlots(randomsource, pDifficulty);
        return super.finalizeSpawn(pLevel, pDifficulty, pReason, pSpawnData);
    }

    @Override
    protected void populateDefaultEquipmentSlots(RandomSource pRandom, DifficultyInstance pDifficulty) {
        this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ItemRegistry.HELLRAZOR.get()));
        this.setDropChance(EquipmentSlot.MAINHAND, 0);
    }

    @Override
    public boolean causeFallDamage(float pFallDistance, float pMultiplier, DamageSource pSource) {
        return false;
    }

    public static AttributeSupplier.Builder prepareAttributes() {
        return LivingEntity.createLivingAttributes()
                .add(Attributes.ATTACK_DAMAGE, 10.0)
                .add(AttributeRegistry.SPELL_POWER, 1.15)
                .add(Attributes.ARMOR, 15)
                .add(AttributeRegistry.SPELL_RESIST, 1)
                .add(Attributes.MAX_HEALTH, 400.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.8)
                .add(Attributes.ATTACK_KNOCKBACK, .6)
                .add(Attributes.FOLLOW_RANGE, 32.0)
                .add(Attributes.SCALE, 1.4)
                .add(Attributes.GRAVITY, 0.03)
                .add(Attributes.ENTITY_INTERACTION_RANGE, 3.5)
                .add(Attributes.MOVEMENT_SPEED, .195);
    }

    RawAnimation animationToPlay = null;
    private final AnimationController<FireBossEntity> meleeController = new AnimationController<>(this, "melee_animations", 0, this::predicate);

    @Override
    public void playAnimation(String animationId) {
        try {
            animationToPlay = RawAnimation.begin().thenPlay(animationId);
        } catch (Exception ignored) {
            IronsSpellbooks.LOGGER.error("Entity {} Failed to play animation: {}", this, animationId);
        }
    }

    private PlayState predicate(AnimationState<FireBossEntity> animationEvent) {
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
    public boolean hurt(DamageSource pSource, float pAmount) {
        /*
        can parry:
        - serverside
        - we aren't in melee attack anim or spell cast
        - the damage source is caused by an entity (ie not fall damage)
        - the damage is caused within our rough field of vision (117 degrees)
        - the damage is not /kill
         */
        boolean canParry = !level.isClientSide &&
                !attackGoal.isActing() &&
                pSource.getEntity() != null &&
                pSource.getSourcePosition() != null && pSource.getSourcePosition().subtract(this.position()).normalize().dot(this.getForward()) >= 0.35
                && !pSource.is(DamageTypeTags.BYPASSES_INVULNERABILITY);
        if (canParry && this.random.nextFloat() < 0.5) {
            PacketDistributor.sendToPlayersTrackingEntity(this, new SyncAnimationPacket<>("instant_self", this));
            this.playSound(SoundEvents.SHIELD_BLOCK);
            return false;
        }
        return super.hurt(pSource, pAmount);
    }
}
