package io.redspace.ironsspellbooks.entity.mobs.wizards.cursed_armor_stand;

import io.redspace.ironsspellbooks.IronsSpellbooks;
import io.redspace.ironsspellbooks.entity.mobs.IAnimatedAttacker;
import io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob.AbstractSpellCastingMob;
import io.redspace.ironsspellbooks.entity.mobs.goals.melee.AttackAnimationData;
import io.redspace.ironsspellbooks.entity.mobs.wizards.GenericAnimatedWarlockAttackGoal;
import io.redspace.ironsspellbooks.entity.mobs.wizards.fire_boss.NotIdioticNavigation;
import io.redspace.ironsspellbooks.registries.ItemRegistry;
import io.redspace.ironsspellbooks.util.NBT;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.util.TimeUtil;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.LookControl;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.ResetUniversalAngerTargetGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.*;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class CursedArmorStandEntity extends AbstractSpellCastingMob implements IAnimatedAttacker, NeutralMob {
    public enum Pose {
        /**
         * vanilla armor stand pose
         */
        DEFAULT,
        /**
         * kneeling
         */
        KNEELING,
        /**
         * arm and head raised
         */
        HEROIC,
        /**
         * standing straight w/ sword in ground
         */
        STOIC
    }

    public static final int JIGGLE_TIME = 15;

    private static final EntityDataAccessor<Boolean> DATA_FROZEN = SynchedEntityData.defineId(CursedArmorStandEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<String> DATA_POSE = SynchedEntityData.defineId(CursedArmorStandEntity.class, EntityDataSerializers.STRING);

    @Nullable
    Vec3 spawn = null;
    float originalYRot = 0;
    /**
     * client tick timers for animating interactions
     */
    int bootJiggle, legJiggle, chestJiggle, helmetJiggle;

    /**
     * anger metric when player interacts with it. too much jostling causes it to aggro
     */
    int interactionAnger;

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder pBuilder) {
        super.defineSynchedData(pBuilder);
        pBuilder.define(DATA_FROZEN, true);
        pBuilder.define(DATA_POSE, "DEFAULT");
    }

    public boolean isArmorStandFrozen() {
        return this.entityData.get(DATA_FROZEN);
    }

    public Pose getArmorstandPose() {
        return Pose.valueOf(this.entityData.get(DATA_POSE));
    }

    public void setArmorstandPose(Pose pose) {
        this.entityData.set(DATA_POSE, pose.name());
    }

    public void setArmorStandFrozen(boolean frozen) {
        this.entityData.set(DATA_FROZEN, frozen);
        if (frozen) {
            this.setYHeadRot(originalYRot);
            this.setYBodyRot(originalYRot);
            this.setYRot(originalYRot);
        }
    }

    @Override
    public InteractionResult interactAt(Player pPlayer, Vec3 pVector, InteractionHand pHand) {
        if (isArmorStandFrozen()) {
            if (pPlayer.level.isClientSide) {
                handleInteraction(pVector, slot -> {
                    switch (slot) {
                        case HEAD -> helmetJiggle = JIGGLE_TIME;
                        case CHEST -> chestJiggle = JIGGLE_TIME;
                        case LEGS -> legJiggle = JIGGLE_TIME;
                        case FEET -> bootJiggle = JIGGLE_TIME;
                    }
                });
            } else {
                AtomicReference<SoundEvent> sound = new AtomicReference<>(SoundEvents.ARMOR_STAND_HIT);

                handleInteraction(pVector, slot -> {
                    if (hasItemInSlot(slot) && getItemBySlot(slot).getItem() instanceof ArmorItem armorItem) {
                        sound.set(armorItem.getMaterial().value().equipSound().value());
                    }
                    if (pPlayer.isCreative() && pPlayer.isCrouching()) {
                        ItemStack equipped = getItemBySlot(slot);
                        ItemStack playerHeld = pPlayer.getItemInHand(pHand);
                        if ((equipped.isEmpty() || equipped.getItem() instanceof ArmorItem) && (playerHeld.isEmpty() || (playerHeld.getItem() instanceof ArmorItem armorItem && armorItem.getEquipmentSlot().equals(slot)))) {
                            pPlayer.setItemInHand(pHand, equipped);
                            this.setItemSlot(slot, playerHeld);
                        }
                    }
                });
                playSound(sound.get());
                if (canAttack(pPlayer) && interactionAnger++ >= 2) {
                    this.setTarget(pPlayer);
                }
            }
            return InteractionResult.SUCCESS;
        }
        return super.interactAt(pPlayer, pVector, pHand);
    }

    private void handleInteraction(Vec3 interactionVector, Consumer<EquipmentSlot> onInteract) {
        double d0 = interactionVector.y / (double) (this.getScale() * this.getAgeScale());
        if (d0 >= 0.1 && d0 < 0.1 + 0.45) {
            onInteract.accept(EquipmentSlot.FEET);
        } else if (d0 >= 0.9 + 0.0 && d0 < 0.9 + 0.7) {
            onInteract.accept(EquipmentSlot.CHEST);
        } else if (d0 >= 0.4 && d0 < 0.4 + 0.8) {
            onInteract.accept(EquipmentSlot.LEGS);
        } else if (d0 >= 1.6) {
            onInteract.accept(EquipmentSlot.HEAD);
        }
    }

    @Override
    public boolean shouldBeExtraAnimated() {
        return !isArmorStandFrozen();
    }

    public CursedArmorStandEntity(EntityType<? extends AbstractSpellCastingMob> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        xpReward = 0;
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
    public void tick() {
        super.tick();
        if (!level.isClientSide) {
//            if (isArmorStandFrozen()) {
//                MagicManager.spawnParticles(level, ParticleHelper.SNOW_DUST, getX(), getY() + 2, getZ(), 1, 0, 0, 0, 0, true);
//            } else {
//                MagicManager.spawnParticles(level, ParticleHelper.EMBERS, getX(), getY() + 2, getZ(), 1, 0, 0, 0, 0, true);
//            }
//            if (spawn != null) {
//                MagicManager.spawnParticles(level, ParticleHelper.ELECTRICITY, spawn.x, spawn.y + 0.5, spawn.z, 1, 0, 0, 0, 0, true);
//            }
        } else {
            if (helmetJiggle > 0) {
                helmetJiggle--;
            }
            if (chestJiggle > 0) {
                chestJiggle--;
            }
            if (legJiggle > 0) {
                legJiggle--;
            }
            if (bootJiggle > 0) {
                bootJiggle--;
            }
        }
    }


    @Override
    public void addAdditionalSaveData(CompoundTag pCompound) {
        super.addAdditionalSaveData(pCompound);
        pCompound.putFloat("originalYRot", originalYRot);
        pCompound.putBoolean("armorStandFrozen", isArmorStandFrozen());
        if (spawn != null) {
            pCompound.put("spawnPos", NBT.writeVec3Pos(spawn));
        }
        pCompound.putString("armorStandPose", getArmorstandPose().name());
        this.addPersistentAngerSaveData(pCompound);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);
        this.originalYRot = pCompound.getFloat("originalYRot");
        if (pCompound.contains("spawnPos", 10)) {
            this.spawn = NBT.readVec3(pCompound.getCompound("spawnPos"));
        }
        this.setArmorStandFrozen(pCompound.getBoolean("armorStandFrozen"));
        String pose = pCompound.getString("armorStandPose");
        try {
            this.setArmorstandPose(Pose.valueOf(pose));
        } catch (Exception ignored) {
            IronsSpellbooks.LOGGER.warn("Attempting to load invalid pose: {}", pose);
            this.setArmorstandPose(Pose.DEFAULT);
        }
        this.readPersistentAngerSaveData(this.level, pCompound);
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (setSpawnOnFirstTick) {
            setSpawnOnFirstTick = false;
            this.spawn = this.position();
        }
        if (this.level instanceof ServerLevel serverLevel) {
            updatePersistentAnger(serverLevel, true);
        }
        if (interactionAnger > 0 && tickCount % 30 == 0) {
            interactionAnger--;
        }
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(3, new GenericAnimatedWarlockAttackGoal<>(this, 1.25f, 50, 75)
                .setMoveset(List.of(
                        new AttackAnimationData(10, "simple_sword_horizontal_cross_swipe", 8),
                        new AttackAnimationData(20, "simple_sword_downstrike", 16)
                ))
                .setComboChance(.4f)
                .setMeleeAttackInverval(10, 30)
                .setMeleeMovespeedModifier(1.5f)
                .setMeleeBias(1f, 1f)
                .setSpells(
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of()
                )

        );
        this.goalSelector.addGoal(5, new ArmorStandReturnToHomeGoal(this, 1));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false, this::isAngryAt));
        this.targetSelector.addGoal(5, new ResetUniversalAngerTargetGoal<>(this, false));
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void pushEntities() {
        return;
    }

    @Override
    protected boolean isImmobile() {
        return super.isImmobile() || isArmorStandFrozen();
    }

    @Override
    public void setTarget(@org.jetbrains.annotations.Nullable LivingEntity pTarget) {
        if (pTarget != null) {
            setArmorStandFrozen(false);
        }
        super.setTarget(pTarget);
    }

    @Override
    public boolean hurt(DamageSource pSource, float pAmount) {
        setArmorStandFrozen(false);
        return super.hurt(pSource, pAmount);
    }

    boolean setSpawnOnFirstTick;

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor pLevel, DifficultyInstance pDifficulty, MobSpawnType pReason, @Nullable SpawnGroupData pSpawnData) {
        this.setSpawnOnFirstTick = true;
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

    RawAnimation animationToPlay = null;
    private final AnimationController<CursedArmorStandEntity> meleeController = new AnimationController<>(this, "keeper_animations", 0, this::predicate);

    @Override
    public void playAnimation(String animationId) {
        try {
            animationToPlay = RawAnimation.begin().thenPlay(animationId);
        } catch (Exception ignored) {
            IronsSpellbooks.LOGGER.error("Entity {} Failed to play animation: {}", this, animationId);
        }
    }

    private PlayState predicate(AnimationState<CursedArmorStandEntity> animationEvent) {
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
    protected PathNavigation createNavigation(Level pLevel) {
        return new NotIdioticNavigation(this, pLevel);
    }


    /*
    Neutral mob impl
     */
    private static final UniformInt PERSISTENT_ANGER_TIME = TimeUtil.rangeOfSeconds(20, 39);
    private int remainingPersistentAngerTime;
    @Nullable
    private UUID persistentAngerTarget;

    @Override
    public int getRemainingPersistentAngerTime() {
        return remainingPersistentAngerTime;
    }

    @Override
    public void setRemainingPersistentAngerTime(int pRemainingPersistentAngerTime) {
        remainingPersistentAngerTime = pRemainingPersistentAngerTime;
    }

    @org.jetbrains.annotations.Nullable
    @Override
    public UUID getPersistentAngerTarget() {
        return persistentAngerTarget;
    }

    @Override
    public void setPersistentAngerTarget(@org.jetbrains.annotations.Nullable UUID pPersistentAngerTarget) {
        persistentAngerTarget = pPersistentAngerTarget;
    }

    @Override
    public void startPersistentAngerTimer() {
        this.setRemainingPersistentAngerTime(PERSISTENT_ANGER_TIME.sample(this.random));
    }
}
