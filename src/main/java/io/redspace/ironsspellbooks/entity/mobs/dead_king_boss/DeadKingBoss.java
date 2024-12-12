package io.redspace.ironsspellbooks.entity.mobs.dead_king_boss;

import io.redspace.ironsspellbooks.IronsSpellbooks;
import io.redspace.ironsspellbooks.api.entity.IMagicEntity;
import io.redspace.ironsspellbooks.api.network.IClientEventEntity;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import io.redspace.ironsspellbooks.entity.mobs.IAnimatedAttacker;
import io.redspace.ironsspellbooks.entity.mobs.IMagicSummon;
import io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob.AbstractSpellCastingMob;
import io.redspace.ironsspellbooks.entity.mobs.goals.AttackAnimationData;
import io.redspace.ironsspellbooks.entity.mobs.goals.PatrolNearLocationGoal;
import io.redspace.ironsspellbooks.entity.mobs.goals.SpellBarrageGoal;
import io.redspace.ironsspellbooks.network.EntityEventPacket;
import io.redspace.ironsspellbooks.registries.EntityRegistry;
import io.redspace.ironsspellbooks.registries.ItemRegistry;
import io.redspace.ironsspellbooks.registries.SoundRegistry;
import io.redspace.ironsspellbooks.util.ParticleHelper;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
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
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.AbstractIllager;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.*;

import javax.annotation.Nullable;
import java.util.List;

public class DeadKingBoss extends AbstractSpellCastingMob implements Enemy, IAnimatedAttacker, IClientEventEntity {
    public static final byte STOP_MUSIC = 0;
    public static final byte START_MUSIC = 1;

    @Override
    public void handleClientEvent(byte eventId) {
        switch (eventId) {
            case STOP_MUSIC -> DeadKingMusicManager.stop(this);
            case START_MUSIC -> DeadKingMusicManager.createOrResumeInstance(this);
        }
    }

    public DeadKingBoss(Level pLevel) {
        this(EntityRegistry.DEAD_KING.get(), pLevel);
        setPersistenceRequired();
    }

    public enum Phases {
        FirstPhase(0),
        Transitioning(1),
        FinalPhase(2);
        final int value;

        Phases(int value) {
            this.value = value;
        }
    }

    public enum AttackType {
        DOUBLE_SWING(51, "dead_king_double_swing", 16, 36),
        SLAM(48, "dead_king_slam", 30);

        AttackType(int lengthInTicks, String animationId, int... attackTimestamps) {
            this.data = new AttackAnimationData(lengthInTicks, animationId, attackTimestamps);
        }

        public final AttackAnimationData data;
    }

    private final ServerBossEvent bossEvent = (ServerBossEvent) (new ServerBossEvent(this.getDisplayName(), BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.PROGRESS)).setDarkenScreen(true).setCreateWorldFog(true);
    private final static EntityDataAccessor<Integer> PHASE = SynchedEntityData.defineId(DeadKingBoss.class, EntityDataSerializers.INT);
    private int transitionAnimationTime = 139; // Animation Length in ticks
    private boolean isCloseToGround;
    public boolean isMeleeing;

    public DeadKingBoss(EntityType<? extends AbstractSpellCastingMob> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        setPersistenceRequired();
        xpReward = 60;
        this.lookControl = createLookControl();
        this.moveControl = createMoveControl();
    }

    private DeadKingAnimatedWarlockAttackGoal getCombatGoal() {
        return (DeadKingAnimatedWarlockAttackGoal) new DeadKingAnimatedWarlockAttackGoal(this, 1f, 55, 85).setSpellQuality(.3f, .5f).setSpells(
                List.of(
                        SpellRegistry.RAY_OF_SIPHONING_SPELL.get(),
                        SpellRegistry.BLOOD_SLASH_SPELL.get(), SpellRegistry.BLOOD_SLASH_SPELL.get(),
                        SpellRegistry.WITHER_SKULL_SPELL.get(), SpellRegistry.WITHER_SKULL_SPELL.get(), SpellRegistry.WITHER_SKULL_SPELL.get(),
                        SpellRegistry.FANG_STRIKE_SPELL.get(), SpellRegistry.FANG_STRIKE_SPELL.get(),
                        SpellRegistry.POISON_ARROW_SPELL.get(), SpellRegistry.POISON_ARROW_SPELL.get(),
                        SpellRegistry.BLIGHT_SPELL.get(),
                        SpellRegistry.ACID_ORB_SPELL.get()
                ),
                List.of(SpellRegistry.FANG_WARD_SPELL.get(), SpellRegistry.BLOOD_STEP_SPELL.get()),
                List.of(/*SpellType.BLOOD_STEP_SPELL*/),
                List.of()
        ).setMeleeBias(0.8f, 0.8f).setAllowFleeing(false);
    }

    @Override
    protected void registerGoals() {
        setFirstPhaseGoals();
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, IronGolem.class, true));
        this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, Villager.class, true));
        this.targetSelector.addGoal(5, new NearestAttackableTargetGoal<>(this, AbstractIllager.class, true));
    }

    protected void setFirstPhaseGoals() {
        this.goalSelector.getAvailableGoals().forEach(WrappedGoal::stop);
        this.goalSelector.removeAllGoals((x) -> true);
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new DeadKingBarrageGoal(this, SpellRegistry.WITHER_SKULL_SPELL.get(), 3, 4, 70, 140, 3));
        this.goalSelector.addGoal(2, new DeadKingBarrageGoal(this, SpellRegistry.RAISE_DEAD_SPELL.get(), 3, 5, 600, 900, 1));
        this.goalSelector.addGoal(3, new DeadKingBarrageGoal(this, SpellRegistry.BLOOD_STEP_SPELL.get(), 1, 1, 100, 180, 1));
        this.goalSelector.addGoal(4, getCombatGoal().setSingleUseSpell(SpellRegistry.RAISE_DEAD_SPELL.get(), 10, 50, 8, 8));
        this.goalSelector.addGoal(5, new PatrolNearLocationGoal(this, 32, 0.9f));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
    }

    protected void setFinalPhaseGoals() {
        this.goalSelector.getAvailableGoals().forEach(WrappedGoal::stop);
        this.goalSelector.removeAllGoals((x) -> true);
        this.goalSelector.addGoal(1, new DeadKingBarrageGoal(this, SpellRegistry.WITHER_SKULL_SPELL.get(), 5, 5, 60, 140, 4));
        this.goalSelector.addGoal(2, new DeadKingBarrageGoal(this, SpellRegistry.SUMMON_VEX_SPELL.get(), 3, 5, 400, 600, 1));
        this.goalSelector.addGoal(3, new DeadKingBarrageGoal(this, SpellRegistry.BLOOD_STEP_SPELL.get(), 1, 1, 100, 180, 1));
        this.goalSelector.addGoal(4, getCombatGoal().setIsFlying().setSingleUseSpell(SpellRegistry.BLAZE_STORM_SPELL.get(), 10, 30, 10, 10));
        this.goalSelector.addGoal(5, new PatrolNearLocationGoal(this, 32, 0.9f));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.hasUsedSingleAttack = false;
    }

    protected SoundEvent getAmbientSound() {
        return SoundEvents.SKELETON_AMBIENT;
    }

    protected SoundEvent getHurtSound(DamageSource pDamageSource) {
        return SoundRegistry.DEAD_KING_HURT.get();
    }

    protected SoundEvent getDeathSound() {
        return SoundRegistry.DEAD_KING_DEATH.get();
    }

    @Override
    public void handleEntityEvent(byte pId) {
        if (pId == 3) {
            //play death sound event
        } else {
            super.handleEntityEvent(pId);
        }
    }

    @Override
    public float getVoicePitch() {
        return 1f;
    }

    @Override
    public boolean isPushable() {
        return !isPhaseTransitioning();
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor pLevel, DifficultyInstance pDifficulty, MobSpawnType pReason, @Nullable SpawnGroupData pSpawnData) {
        RandomSource randomsource = Utils.random;
        this.populateDefaultEquipmentSlots(randomsource, pDifficulty);
        return pSpawnData;
    }

    @Override
    protected void populateDefaultEquipmentSlots(RandomSource pRandom, DifficultyInstance pDifficulty) {
        this.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(ItemRegistry.BLOOD_STAFF.get()));
        this.setDropChance(EquipmentSlot.OFFHAND, 0f);
    }

    @Override
    public boolean isAlliedTo(Entity pEntity) {
        return super.isAlliedTo(pEntity) || (pEntity instanceof IMagicSummon summon && summon.getSummoner() == this);
    }

    //Instead of being undead (smite is ridiculous)
    @Override
    public boolean isInvertedHealAndHarm() {
        return true;
    }

    @Override
    public void tick() {
        if (isPhase(Phases.FinalPhase)) {
            setNoGravity(true);
            if (tickCount % 10 == 0) {
                isCloseToGround = Utils.raycastForBlock(level, position(), position().subtract(0, 2.5, 0), ClipContext.Fluid.ANY).getType() == HitResult.Type.BLOCK;
            }
            Vec3 woosh = new Vec3(
                    Mth.sin((tickCount * 5) * Mth.DEG_TO_RAD),
                    (Mth.cos((tickCount * 3 + 986741) * Mth.DEG_TO_RAD) + (isCloseToGround ? .05 : -.185)) * .5f,
                    Mth.sin((tickCount * 1 + 465) * Mth.DEG_TO_RAD)
            );
            if (this.getTarget() == null) {
                woosh = woosh.scale(.25f);
            }
            this.setDeltaMovement(getDeltaMovement().add(woosh.scale(.0085f)));
        }
        super.tick();
        if (level.isClientSide) {
            if (isPhase(Phases.FinalPhase)) {
                if (!this.isInvisible()) {
                    float radius = .35f;
                    for (int i = 0; i < 5; i++) {
                        Vec3 random = position().add(new Vec3(
                                (this.random.nextFloat() * 2 - 1) * radius,
                                1 + (this.random.nextFloat() * 2 - 1) * radius,
                                (this.random.nextFloat() * 2 - 1) * radius
                        ));
                        level.addParticle(ParticleTypes.SMOKE, random.x, random.y, random.z, 0, -.1, 0);
                    }
                }
            }
        } else {
            //irons_spellbooks.LOGGER.debug("DeadKingBoss.tick | Phase: {} | isTransitioning: {} | TransitionTime: {}", getPhase(), isPhaseTransitioning(), transitionAnimationTime);
            float halfHealth = this.getMaxHealth() / 2;
            if (isPhase(Phases.FirstPhase)) {
                this.bossEvent.setProgress((this.getHealth() - halfHealth) / (this.getMaxHealth() - halfHealth));
                if (this.getHealth() <= halfHealth) {
                    setPhase(Phases.Transitioning);
                    var player = level.getNearestPlayer(this, 16);
                    if (player != null) {
                        lookAt(player, 360, 360);
                    }
                    if (!isDeadOrDying()) {
                        setHealth(halfHealth);
                    }
                    playSound(SoundRegistry.DEAD_KING_FAKE_DEATH.get());
                    //Overriding isInvulnerable just doesn't seem to work
                    setInvulnerable(true);
                }
            } else if (isPhase(Phases.Transitioning)) {
                if (--transitionAnimationTime <= 0) {
                    setPhase(Phases.FinalPhase);
                    MagicManager.spawnParticles(level, ParticleHelper.FIRE, position().x, position().y + 2.5, position().z, 80, .2, .2, .2, .25, true);
                    setFinalPhaseGoals();
                    setNoGravity(true);
                    playSound(SoundRegistry.DEAD_KING_EXPLODE.get());
                    level.getEntities(this, this.getBoundingBox().inflate(5), (entity) -> entity instanceof LivingEntity && entity.isPickable() && entity.distanceToSqr(position()) < 5 * 5).forEach(super::doHurtTarget);
                    setInvulnerable(false);
                }
            } else if (isPhase(Phases.FinalPhase)) {
                this.bossEvent.setProgress(this.getHealth() / (this.getMaxHealth() - halfHealth));
            }
        }
    }

    /**
     * immune to fall damage
     */
    @Override
    public boolean causeFallDamage(float pFallDistance, float pMultiplier, DamageSource pSource) {
        return false;
    }

    public boolean isPhase(Phases phase) {
        return phase.value == getPhase();
    }

    @Override
    public boolean hurt(DamageSource pSource, float pAmount) {
        if (pSource == level.damageSources().lava()) {
            return false;
        }
        return super.hurt(pSource, pAmount);
    }

    @Override
    protected boolean isImmobile() {
        return isPhase(Phases.Transitioning) || super.isImmobile();
    }

    public boolean isPhaseTransitioning() {
        return isPhase(Phases.Transitioning);
    }

    public void startSeenByPlayer(ServerPlayer pPlayer) {
        super.startSeenByPlayer(pPlayer);
        this.bossEvent.addPlayer(pPlayer);
        PacketDistributor.sendToPlayer(pPlayer, new EntityEventPacket<DeadKingBoss>(this, START_MUSIC));
    }

    public void stopSeenByPlayer(ServerPlayer pPlayer) {
        super.stopSeenByPlayer(pPlayer);
        this.bossEvent.removePlayer(pPlayer);
        PacketDistributor.sendToPlayer(pPlayer, new EntityEventPacket<DeadKingBoss>(this, STOP_MUSIC));
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
                .add(Attributes.ENTITY_INTERACTION_RANGE, 4)
                .add(Attributes.FOLLOW_RANGE, 32.0)
                .add(Attributes.MOVEMENT_SPEED, .155);
    }

    @Override
    public void setCustomName(@Nullable Component pName) {
        super.setCustomName(pName);
        this.bossEvent.setName(this.getDisplayName());
    }

    private void setPhase(int phase) {
        this.entityData.set(PHASE, phase);
    }

    private void setPhase(Phases phase) {
        this.setPhase(phase.value);
    }

    public int getPhase() {
        return this.entityData.get(PHASE);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag pCompound) {
        super.addAdditionalSaveData(pCompound);
        pCompound.putInt("phase", getPhase());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);
        if (this.hasCustomName()) {
            this.bossEvent.setName(this.getDisplayName());
        }
        setPhase(pCompound.getInt("phase"));
        if (isPhase(Phases.FinalPhase))
            setFinalPhaseGoals();

    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder pBuilder) {
        super.defineSynchedData(pBuilder);
        pBuilder.define(PHASE, 0);
    }

    private final RawAnimation phase_transition_animation = RawAnimation.begin().thenPlay("dead_king_die");
    private final RawAnimation melee = RawAnimation.begin().thenPlay("dead_king_melee");
    private final RawAnimation slam = RawAnimation.begin().thenPlay("dead_king_slam");

    private final AnimationController<DeadKingBoss> transitionController = new AnimationController<>(this, "dead_king_transition", 0, this::transitionPredicate);
    private final AnimationController<DeadKingBoss> meleeController = new AnimationController<>(this, "dead_king_animations", 0, this::meleePredicate);
    RawAnimation animationToPlay = null;

    @Override
    public void playAnimation(String animationId) {
        try {
            var attackType = AttackType.valueOf(animationId);
            animationToPlay = RawAnimation.begin().thenPlay(attackType.data.animationId);
        } catch (Exception ignored) {
            IronsSpellbooks.LOGGER.error("Entity {} Failed to play animation: {}", this, animationId);
        }
    }

    private PlayState meleePredicate(AnimationState<DeadKingBoss> animationEvent) {
        var controller = animationEvent.getController();
        if (this.animationToPlay != null) {
            controller.forceAnimationReset();
            controller.setAnimation(animationToPlay);
            animationToPlay = null;
        }
        return transitionController.getAnimationState() == AnimationController.State.STOPPED ? PlayState.CONTINUE : PlayState.STOP;
    }

    private PlayState transitionPredicate(AnimationState animationEvent) {
        var controller = animationEvent.getController();
        if (isPhaseTransitioning()) {
            controller.setAnimation(phase_transition_animation);
            return PlayState.CONTINUE;
        }
        return PlayState.STOP;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {
        controllerRegistrar.add(transitionController);
        controllerRegistrar.add(meleeController);
        super.registerControllers(controllerRegistrar);
    }

    @Override
    public boolean shouldAlwaysAnimateHead() {
        return !isPhaseTransitioning();
    }

    @Override
    public boolean bobBodyWhileWalking() {
        return this.isPhase(Phases.FirstPhase);
    }

    @Override
    public boolean isAnimating() {
        return transitionController.getAnimationState() != AnimationController.State.STOPPED || meleeController.getAnimationState() != AnimationController.State.STOPPED || super.isAnimating();
    }

    @Override
    public boolean doHurtTarget(Entity pEntity) {
        level.playSound(null, getX(), getY(), getZ(), SoundRegistry.DEAD_KING_HIT.get(), SoundSource.HOSTILE, 1, 1);
        return super.doHurtTarget(pEntity);
    }

    @Override
    public boolean shouldAlwaysAnimateLegs() {
        return this.isPhase(Phases.FirstPhase);
    }

    private class DeadKingBarrageGoal extends SpellBarrageGoal {
        public DeadKingBarrageGoal(IMagicEntity abstractSpellCastingMob, AbstractSpell spell, int minLevel, int maxLevel, int pAttackIntervalMin, int pAttackIntervalMax, int projectileCount) {
            super(abstractSpellCastingMob, spell, minLevel, maxLevel, pAttackIntervalMin, pAttackIntervalMax, projectileCount);
        }

        @Override
        public boolean canUse() {
            return !isMeleeing && super.canUse();
        }
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
                return !isCasting();
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

}
