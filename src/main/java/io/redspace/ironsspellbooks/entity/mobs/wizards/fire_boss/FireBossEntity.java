package io.redspace.ironsspellbooks.entity.mobs.wizards.fire_boss;

import io.redspace.ironsspellbooks.IronsSpellbooks;
import io.redspace.ironsspellbooks.api.network.IClientEventEntity;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.util.CameraShakeData;
import io.redspace.ironsspellbooks.api.util.CameraShakeManager;
import io.redspace.ironsspellbooks.api.util.FogManager;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import io.redspace.ironsspellbooks.entity.mobs.IAnimatedAttacker;
import io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob.AbstractSpellCastingMob;
import io.redspace.ironsspellbooks.entity.mobs.dead_king_boss.DeadKingBoss;
import io.redspace.ironsspellbooks.entity.mobs.goals.PatrolNearLocationGoal;
import io.redspace.ironsspellbooks.entity.mobs.goals.melee.AttackAnimationData;
import io.redspace.ironsspellbooks.entity.mobs.goals.melee.AttackKeyframe;
import io.redspace.ironsspellbooks.entity.mobs.keeper.KeeperEntity;
import io.redspace.ironsspellbooks.entity.spells.FireEruptionAoe;
import io.redspace.ironsspellbooks.network.EntityEventPacket;
import io.redspace.ironsspellbooks.particle.BlastwaveParticleOptions;
import io.redspace.ironsspellbooks.registries.ItemRegistry;
import io.redspace.ironsspellbooks.registries.ParticleRegistry;
import io.redspace.ironsspellbooks.registries.SoundRegistry;
import io.redspace.ironsspellbooks.util.ModTags;
import io.redspace.ironsspellbooks.util.ParticleHelper;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.LookControl;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;
import net.neoforged.neoforge.network.PacketDistributor;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.*;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

public class FireBossEntity extends AbstractSpellCastingMob implements Enemy, IAnimatedAttacker, IEntityWithComplexSpawn, IClientEventEntity {
    public static final byte STOP_FOG = 0;
    public static final byte START_FOG = 1;

    @Override
    public void handleClientEvent(byte eventId) {
        switch (eventId) {
            case STOP_FOG -> FogManager.stopEvent(this.uuid);
            case START_FOG -> FogManager.createEvent(this.uuid, new FogManager.FogEvent(Optional.empty(), true));
        }
    }

    @Override
    public void writeSpawnData(RegistryFriendlyByteBuf buffer) {
        buffer.writeInt(this.spawnTimer);
    }

    @Override
    public void readSpawnData(RegistryFriendlyByteBuf additionalData) {
        this.spawnTimer = additionalData.readInt();
        if (spawnTimer > 0) {
            playAnimation("fire_boss_spawn");
        }
        float y = this.getYRot();
        this.yBodyRot = y;
        this.yBodyRotO = y;
        this.yHeadRot = y;
        this.yHeadRotO = y;
        this.yRotO = y;
    }

    private static final EntityDataAccessor<Boolean> DATA_SOUL_MODE = SynchedEntityData.defineId(FireBossEntity.class, EntityDataSerializers.BOOLEAN);
    private static final AttributeModifier SOUL_SPEED_MODIFIER = new AttributeModifier(IronsSpellbooks.id("soul_mode"), 0.05, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    private static final AttributeModifier SOUL_SCALE_MODIFIER = new AttributeModifier(IronsSpellbooks.id("soul_mode"), 0.15, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);

    @Override
    public void kill() {
        if (this.isDeadOrDying() || this.isSpawning()) {
            discard();
        } else {
            super.kill();
        }
    }

    @Override
    public void push(Entity pEntity) {
        if (!isSpawning()) {
            super.push(pEntity);
        }
    }

    /**
     * Client flag for whether code animations should pause over current animation
     */
    private boolean canAnimateOver;

    /**
     * Client side model control value
     */
    public float isAnimatingDampener;

    public FireBossEntity(EntityType<? extends AbstractSpellCastingMob> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        xpReward = 25;
        this.lookControl = createLookControl();
        this.moveControl = createMoveControl();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder pBuilder) {
        super.defineSynchedData(pBuilder);
        pBuilder.define(DATA_SOUL_MODE, false);
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
        return new FireBossMoveControl(this) {
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


    public void startSeenByPlayer(ServerPlayer pPlayer) {
        super.startSeenByPlayer(pPlayer);
        this.bossEvent.addPlayer(pPlayer);
        PacketDistributor.sendToPlayer(pPlayer, new EntityEventPacket<DeadKingBoss>(this, START_FOG));
    }

    public void stopSeenByPlayer(ServerPlayer pPlayer) {
        super.stopSeenByPlayer(pPlayer);
        this.bossEvent.removePlayer(pPlayer);
        PacketDistributor.sendToPlayer(pPlayer, new EntityEventPacket<DeadKingBoss>(this, STOP_FOG));
    }

    FireBossAttackGoal attackGoal;

    @Override
    public FireBossMoveControl getMoveControl() {
        return (FireBossMoveControl) super.getMoveControl();
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.attackGoal = (FireBossAttackGoal) new FireBossAttackGoal(this, 0.8f, 50, 75)
                .setMoveset(List.of(
                        AttackAnimationData.builder("scythe_backpedal")
                                .length(40)
                                .attacks(
                                        new FireBossAttackKeyframe(20, new Vec3(0, .3, -2), new FireBossAttackKeyframe.SwingData(false, true))
                                )
                                .build(),
                        AttackAnimationData.builder("scythe_low_rightward_sweep")
                                .length(40)
                                .area(0.25f)
                                .attacks(
                                        new FireBossAttackKeyframe(20, new Vec3(0, .1, 0.8), new FireBossAttackKeyframe.SwingData(false, false))
                                )
                                .build(),
                        AttackAnimationData.builder("scythe_sideslash_downslash")
                                .length(54)
                                .attacks(
                                        new FireBossAttackKeyframe(18, new Vec3(0, 0, .45), new FireBossAttackKeyframe.SwingData(false, true)),
                                        new FireBossAttackKeyframe(32, new Vec3(0, 0, .45), new FireBossAttackKeyframe.SwingData(true, true)))
                                .build(),
                        AttackAnimationData.builder("scythe_jump_combo")
                                .length(45)
                                .cancellable()
                                .rangeMultiplier(2f)
                                .attacks(
                                        new FireBossAttackKeyframe(20, new Vec3(0, 1, 0), new Vec3(0, 1.15, .1), new FireBossAttackKeyframe.SwingData(true, false)),
                                        new FireBossAttackKeyframe(35, new Vec3(0, 0, -.2), new Vec3(0, 0, 0.5), new FireBossAttackKeyframe.SwingData(false, false))
                                )
                                .build(),
                        AttackAnimationData.builder("scythe_downslash_pull")
                                .length(60)
                                .cancellable()
                                .attacks(
                                        new FireBossAttackKeyframe(22, new Vec3(0, 0, .5f), new Vec3(0, -.2, 0), new FireBossAttackKeyframe.SwingData(true, true)),
                                        new AttackKeyframe(38, new Vec3(0, .2, -0.8), new Vec3(0, .3, -1.8))
                                )
                                .build(),
                        AttackAnimationData.builder("scythe_horizontal_slash_spin")
                                .length(53)
                                .area(0.25f)
                                .rangeMultiplier(3f)
                                .attacks(
                                        new FireBossAttackKeyframe(16, new Vec3(0, 0.1, 0.75), new Vec3(0, .1, 0.8), new FireBossAttackKeyframe.SwingData(false, true)),
                                        new FireBossAttackKeyframe(36, new Vec3(0, 0.1, 1.25), new Vec3(0, .3, 0.8), new FireBossAttackKeyframe.SwingData(false, false))
                                )
                                .build()

                ))
                .setComboChance(1f)
                .setMeleeAttackInverval(20, 20)
                .setMeleeBias(1f, 1f)
                .setSpells(
                        List.of(SpellRegistry.FIRE_ARROW_SPELL.get(), SpellRegistry.FIRE_ARROW_SPELL.get(), SpellRegistry.SCORCH_SPELL.get()),
                        List.of(),
                        List.of(),
                        List.of()
                );
        //this.goalSelector.addGoal(2, new FieryDaggerSwarmAbilityGoal(this));
        //this.goalSelector.addGoal(2, new FieryDaggerZoneAbilityGoal(this));
        //this.goalSelector.addGoal(2, new SpellBarrageGoal(this, SpellRegistry.RAISE_HELL_SPELL.get(), 5, 5, 80, 240, 1));
        this.goalSelector.addGoal(3, attackGoal);

        this.goalSelector.addGoal(4, new PatrolNearLocationGoal(this, 30, .75f));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Pig.class, true));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, DeadKingBoss.class, true));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    private final ServerBossEvent bossEvent = (ServerBossEvent) (new ServerBossEvent(this.getDisplayName(), BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.PROGRESS)).setCreateWorldFog(true);
    int stanceBreakCounter;
    int stanceBreakTimer;
    static final int STANCE_BREAK_ANIM_TIME = (int) (9 * 20);
    static final int ERUPTION_BEGIN_ANIM_TIME = (int) (6.5 * 20);
    static final int STANCE_BREAK_COUNT = 2;
    int spawnTimer;
    static final int SPAWN_ANIM_TIME = (int) (8.75 * 20);

    public void triggerSpawnAnim() {
        this.spawnTimer = SPAWN_ANIM_TIME;
    }

    public void triggerStanceBreak() {
        stanceBreakCounter++;
        stanceBreakTimer = STANCE_BREAK_ANIM_TIME;
        this.castComplete();
        this.attackGoal.stopMeleeAction();
        this.serverTriggerAnimation("fire_boss_break_stance");
        this.playSound(SoundRegistry.BOSS_STANCE_BREAK.get(), 3, 1);
        Vec3 vec3 = this.getBoundingBox().getCenter();
        MagicManager.spawnParticles(level, ParticleRegistry.EMBEROUS_ASH_PARTICLE.get(), vec3.x, vec3.y, vec3.z, 25, 0.2, 0.2, 0.2, 0.12, false);
    }

    protected SoundEvent getHurtSound(DamageSource pDamageSource) {
        return SoundRegistry.FIRE_BOSS_HURT.get();
    }

    public boolean isStanceBroken() {
        return stanceBreakTimer > 0;
    }

    public boolean isSpawning() {
        return spawnTimer > 0;
    }

    @Override
    protected boolean isImmobile() {
        return super.isImmobile() || isStanceBroken() || isSpawning();
    }

    @Override
    public boolean isInvulnerableTo(DamageSource pSource) {
        return isSpawning() || super.isInvulnerableTo(pSource);
    }

    @Override
    public void tick() {
        super.tick();
        float maxHealth = this.getMaxHealth();
        float currentHealth = this.getHealth();
        this.bossEvent.setProgress(currentHealth / maxHealth);
        if (isSpawning()) {
            spawnTimer--;
            float z = Mth.lerp((float) spawnTimer / SPAWN_ANIM_TIME, 0, -60 / 16f);
            Vec3 position = this.position().add(new Vec3(0, 0, z).yRot(-this.getYRot() * Mth.DEG_TO_RAD));
            if (!level.isClientSide && spawnTimer == SPAWN_ANIM_TIME - 1) {
                MagicManager.spawnParticles(level, ParticleTypes.CAMPFIRE_COSY_SMOKE, position.x, position.y + 1.2, position.z, 165, 0.5, 1.2, 0.5, 0.01, true);
                MagicManager.spawnParticles(level, ParticleHelper.FOG_CAMPFIRE_SMOKE, position.x, position.y + 0.1, position.z, 6, 0.6, .1, 0.6, 0.05, true);
            }

            //step sounds
            if (spawnTimer == SPAWN_ANIM_TIME - 40 || spawnTimer == SPAWN_ANIM_TIME - 60 || spawnTimer == SPAWN_ANIM_TIME - 80 || spawnTimer == SPAWN_ANIM_TIME - 100 || spawnTimer == SPAWN_ANIM_TIME - 114 || spawnTimer == SPAWN_ANIM_TIME - 128) {
                level.playSound(null, position.x, position.y, position.z, SoundRegistry.KEEPER_STEP, this.getSoundSource(), 0.5f, 1f);
            }
            // responding bell toll
            if (spawnTimer == SPAWN_ANIM_TIME - 30) {
                level.playSound(null, position.x, position.y, position.z, SoundRegistry.SOULCALLER_TOLL_SUCCESS, SoundSource.PLAYERS, 5f, .75f);
                if (!level.isClientSide) {
                    MagicManager.spawnParticles(level, new BlastwaveParticleOptions(1, .6f, 0.3f, 8), position.x, position.y, position.z, 0, 0, 0, 0, 0, true);
                }
            }
            // summon scythe sound
            if (spawnTimer == SPAWN_ANIM_TIME - 132 + 17) {
                level.playSound(null, position.x, position.y, position.z, SoundRegistry.FIRE_BOSS_SUMMON_SCYTHE, this.getSoundSource(), 3f, 1f);
            }
            if (spawnTimer == 0 && !level.isClientSide) {
                spawnKnight(true);
                spawnKnight(false);
            }
        }
        if (!level.isClientSide) {
            if (isStanceBroken()) {
                stanceBreakTimer--;
                int tick = STANCE_BREAK_ANIM_TIME - stanceBreakTimer;
                if (stanceBreakCounter == 2) {
                    // we will enter soul mode
                    if (tick == 80) {
                        this.setSoulMode(true);
                        Vec3 vec3 = this.getBoundingBox().getCenter();
                        MagicManager.spawnParticles(level, ParticleHelper.FIRE, vec3.x, vec3.y, vec3.z, 120, 0.3, 0.3, 0.3, 0.3, true);
                        var speed = this.getAttribute(Attributes.MOVEMENT_SPEED);
                        speed.removeModifier(SOUL_SPEED_MODIFIER);
                        speed.addPermanentModifier(SOUL_SPEED_MODIFIER);
                        var scale = this.getAttribute(Attributes.SCALE);
                        scale.removeModifier(SOUL_SCALE_MODIFIER);
                        scale.addPermanentModifier(SOUL_SCALE_MODIFIER);
                        this.playSound(SoundRegistry.FIRE_BOSS_TRANSITION_SOUL.get(), 3, 1);
                    } else if (tick < 80) {
                        var f = Mth.lerp(tick / 80f, 0.2, 0.4);
                        Vec3 vec3 = this.getBoundingBox().getCenter();
                        MagicManager.spawnParticles(level, ParticleHelper.FIRE, vec3.x, vec3.y, vec3.z, 12 + (int) (f * 10), f, f, f, 0.02, true);
                    }
                }
                if (tick >= ERUPTION_BEGIN_ANIM_TIME) {
                    if (tick == ERUPTION_BEGIN_ANIM_TIME) {
                        createEruptionEntity(8, (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE));
                        playSound(SoundRegistry.FIRE_ERUPTION_SLAM.get(), 2, 1.2f);
                    } else if (tick == ERUPTION_BEGIN_ANIM_TIME + 25) {
                        createEruptionEntity(11, (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE) * 2);
                        playSound(SoundRegistry.FIRE_ERUPTION_SLAM.get(), 3, 1f);
                    } else if (tick == ERUPTION_BEGIN_ANIM_TIME + 50) {
                        createEruptionEntity(15, (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE) * 3);
                        playSound(SoundRegistry.FIRE_ERUPTION_SLAM.get(), 4, 0.9f);
                    }
                }
            }
            if (isSoulMode() && !dead) {
                soulParticles();
            }
        }
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        float maxHealth = this.getMaxHealth();
        float currentHealth = this.getHealth();
        float eruptionHealthStep = maxHealth / (STANCE_BREAK_COUNT + 1);
        if (currentHealth < maxHealth - eruptionHealthStep * (stanceBreakCounter + 1)) {
            triggerStanceBreak();
        }
        if (this.tickCount % 30 == 0 && this.getTarget() == null && this.tickCount - this.getLastHurtByMobTimestamp() > 200) {
            this.heal(5);
        }
        if (this.isAggressive() && this.tickCount % (12 * 20) == 0) {
            int knightCount = level.getEntitiesOfClass(KeeperEntity.class, this.getBoundingBox().inflate(32, 16, 32)).size();
            if (knightCount < 2) {
                spawnKnight(this.random.nextBoolean());
            }
        }
    }

    public void spawnKnight(boolean left) {
        if (level instanceof ServerLevel serverLevel) {
            KeeperEntity knight = new KeeperEntity(level);
            float angle = (left ? -90 : 90) * Mth.DEG_TO_RAD;
            Vec3 offset = this.getForward().multiply(3, 0, 3).scale(this.getScale()).yRot(angle);
            Vec3 spawn = Utils.moveToRelativeGroundLevel(level, Utils.raycastForBlock(level, this.getEyePosition(), this.position().add(offset), ClipContext.Fluid.NONE).getLocation(), 4);
            knight.moveTo(spawn.add(0, 0.1, 0));
            knight.triggerRise();
            knight.setYRot(this.getYRot());
            knight.finalizeSpawn(serverLevel, level.getCurrentDifficultyAt(this.blockPosition()), MobSpawnType.MOB_SUMMONED, null);
            knight.summoned = true;
            level.addFreshEntity(knight);
            level.playSound(null, spawn.x, spawn.y, spawn.z, SoundRegistry.FIRE_BOSS_DEATH_FINAL.get(), this.getSoundSource(), 2, .9f);
        }
    }

    public void soulParticles() {
        Vec3 vec3 = this.getBoundingBox().getCenter();
        MagicManager.spawnParticles(level, ParticleHelper.FIRE, vec3.x, vec3.y, vec3.z, 2, 0.2, 0.6, 0.2, 0.01, true);
    }

    private void createEruptionEntity(float radius, float damage) {
        Vec3 forward = this.getForward().multiply(1, 0, 1).normalize().scale(3);
        Vec3 pos = Utils.moveToRelativeGroundLevel(level, this.position().add(forward).add(0, 1, 0), 4);
        FireEruptionAoe aoe = new FireEruptionAoe(level, radius);
        aoe.setOwner(this);
        aoe.setDamage(damage);
        aoe.moveTo(pos);
        level.addFreshEntity(aoe);
        CameraShakeManager.addCameraShake(new CameraShakeData(10 + (int) radius, pos, radius * 2 + 5));
    }

    SimpleContainer deathLoot = null;

    @Override
    protected void dropAllDeathLoot(ServerLevel pLevel, DamageSource pDamageSource) {
        // prevent drops from appearing before death animation, just store them
        this.dropEquipment();
        this.dropExperience(pDamageSource.getEntity());
        boolean playerDeath = this.lastHurtByPlayerTime > 0;
        this.dropCustomDeathLoot(pLevel, pDamageSource, playerDeath);
        ResourceKey<LootTable> resourcekey = this.getLootTable();
        LootTable loottable = this.level.getServer().reloadableRegistries().getLootTable(resourcekey);
        LootParams.Builder lootparams$builder = new LootParams.Builder(pLevel)
                .withParameter(LootContextParams.THIS_ENTITY, this)
                .withParameter(LootContextParams.ORIGIN, this.position())
                .withParameter(LootContextParams.DAMAGE_SOURCE, pDamageSource)
                .withOptionalParameter(LootContextParams.ATTACKING_ENTITY, pDamageSource.getEntity())
                .withOptionalParameter(LootContextParams.DIRECT_ATTACKING_ENTITY, pDamageSource.getDirectEntity());
        if (playerDeath && this.lastHurtByPlayer != null) {
            lootparams$builder = lootparams$builder.withParameter(LootContextParams.LAST_DAMAGE_PLAYER, this.lastHurtByPlayer)
                    .withLuck(this.lastHurtByPlayer.getLuck());
        }

        LootParams lootparams = lootparams$builder.create(LootContextParamSets.ENTITY);
        ObjectArrayList<ItemStack> objectarraylist = new ObjectArrayList<>();
        loottable.getRandomItems(lootparams, this.getLootTableSeed(), objectarraylist::add);
        this.deathLoot = new SimpleContainer(objectarraylist.size());
        objectarraylist.forEach(deathLoot::addItem);
    }

    @Override
    public void die(DamageSource pDamageSource) {
        super.die(pDamageSource);
        if (this.isDeadOrDying() && !this.level.isClientSide) {
            this.stanceBreakTimer = 0;
            this.castComplete();
            this.attackGoal.stop();
            this.serverTriggerAnimation("fire_boss_death");
            this.playSound(SoundRegistry.FIRE_BOSS_DEATH.get(), 5, 1);
            Vec3 vec3 = this.getBoundingBox().getCenter();
            MagicManager.spawnParticles(level, ParticleRegistry.EMBEROUS_ASH_PARTICLE.get(), vec3.x, vec3.y, vec3.z, 25, 0.2, 0.2, 0.2, 0.12, false);
            level.getEntitiesOfClass(KeeperEntity.class, this.getBoundingBox().inflate(32, 16, 32)).stream().filter(keeper -> keeper.summoned).forEach(LivingEntity::kill);
        }
    }

    @Override
    protected void tickDeath() {
        this.deathTime++;
        if (!level.isClientSide) {
            Vec3 vec3 = this.position();
            float scale = getScale();
            int particles = (int) Mth.lerp(Math.clamp((deathTime - 20) / 60f, 0, 1), 0, 5 * scale);
            float range = Mth.lerp(Math.clamp((deathTime - 20) / 80f, 0, 1), 0, 0.4f * scale);
            if (particles > 0) {
                MagicManager.spawnParticles(level, ParticleRegistry.EMBEROUS_ASH_PARTICLE.get(), vec3.x, vec3.y + 1, vec3.z, particles, range, range, range, 100, false);
            }
            if (this.deathTime >= 160 && !this.level().isClientSide() && !this.isRemoved()) {
                if (this.deathLoot != null) {
                    deathLoot.getItems().forEach(this::spawnAtLocation);
                }
                this.remove(Entity.RemovalReason.KILLED);
                MagicManager.spawnParticles(level, ParticleRegistry.EMBEROUS_ASH_PARTICLE.get(), vec3.x, vec3.y + 1, vec3.z, 50, 0.3, 0.3, 0.3, 0.2 * scale, true);
                this.playSound(SoundRegistry.FIRE_BOSS_DEATH_FINAL.get(), 4, .9f);
            }
        }
    }


    @Override
    public void calculateEntityAnimation(boolean pIncludeHeight) {
        super.calculateEntityAnimation(false);
    }

    @Override
    protected void updateWalkAnimation(float f) {
        //reduce walk animation swing if we are floating or meleeing
        super.updateWalkAnimation(f * ((!this.onGround() || this.isAnimating()) ? .5f : (this.isSoulMode() ? .7f : .9f)));
    }

    @Override
    public boolean bobBodyWhileWalking() {
        return !isAnimating();
    }

    @Override
    protected void playStepSound(BlockPos pPos, BlockState pState) {
        this.playSound(SoundRegistry.KEEPER_STEP.get(), .25f, .9f);
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor pLevel, DifficultyInstance pDifficulty, MobSpawnType pReason, @Nullable SpawnGroupData pSpawnData) {
        super.finalizeSpawn(pLevel, pDifficulty, pReason, pSpawnData);
        RandomSource randomsource = Utils.random;
        this.populateDefaultEquipmentSlots(randomsource, pDifficulty);
        this.setLeftHanded(false);
        return pSpawnData;
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
                .add(Attributes.ATTACK_DAMAGE, 8.0)
                .add(AttributeRegistry.SPELL_POWER, 1.15)
                .add(Attributes.ARMOR, 15)
                .add(AttributeRegistry.SPELL_RESIST, 1)
                .add(AttributeRegistry.FIRE_MAGIC_RESIST, 1.5)
                .add(Attributes.MAX_HEALTH, 800)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.8)
                .add(Attributes.ATTACK_KNOCKBACK, .6)
                .add(Attributes.FOLLOW_RANGE, 48.0)
                .add(Attributes.SCALE, 1.75)
                .add(Attributes.GRAVITY, 0.03)
                .add(Attributes.ENTITY_INTERACTION_RANGE, 3)
                .add(Attributes.STEP_HEIGHT, 1)
                .add(Attributes.MOVEMENT_SPEED, .21);
    }

    @Override
    public void knockback(double pStrength, double pX, double pZ) {
        if (isStanceBroken()) {
            return;
        }
        super.knockback(pStrength, pX, pZ);
    }

    @Override
    public boolean isPushable() {
        return super.isPushable() && !isImmobile();
    }

    RawAnimation animationToPlay = null;
    private final AnimationController<FireBossEntity> meleeController = new AnimationController<>(this, "melee_animations", 0, this::predicate);

    @Override
    public void playAnimation(String animationId) {
        animationToPlay = RawAnimation.begin().thenPlay(animationId);
        canAnimateOver = animationId.equals("fire_boss_spawn") || animationId.equals("summon_fiery_daggers");
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
        return (meleeController.getAnimationState() == AnimationController.State.RUNNING && !canAnimateOver) || super.isAnimating();
    }

    @Override
    public boolean hurt(DamageSource pSource, float pAmount) {
        if (level.isClientSide) {
            return false;
        }
        /*
        can parry:
        - serverside
        - in combat
        - we aren't in melee attack anim or spell cast
        - the damage source is caused by an entity (ie not fall damage)
        - the damage is caused within our rough field of vision (117 degrees)
        - the damage is not /kill
         */
        boolean canParry = false &&
                !level.isClientSide &&
                this.isAggressive() &&
                !isImmobile() &&
                !attackGoal.isActing() &&
                pSource.getEntity() != null &&
                pSource.getSourcePosition() != null && pSource.getSourcePosition().subtract(this.position()).normalize().dot(this.getForward()) >= 0.35
                && !pSource.is(DamageTypeTags.BYPASSES_INVULNERABILITY);
        if (canParry && this.random.nextFloat() < 0.5) {
            serverTriggerAnimation("instant_self");
            this.playSound(SoundEvents.SHIELD_BLOCK);
            return false;
        }
        if (isStanceBroken()) {
            pAmount *= 0.25f;
        }
        if (isSoulMode()) {
            pAmount *= 0.4f;
        }
        return super.hurt(pSource, pAmount);
    }

    public boolean isSoulMode() {
        return entityData.get(DATA_SOUL_MODE);
    }

    public void setSoulMode(boolean soulMode) {
        entityData.set(DATA_SOUL_MODE, soulMode);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag pCompound) {
        super.addAdditionalSaveData(pCompound);
        pCompound.putInt("stanceBreakCount", stanceBreakCounter);
        if (stanceBreakTimer > 0) {
            pCompound.putInt("stanceBreakTime", stanceBreakTimer);
        }
        pCompound.putBoolean("soulMode", isSoulMode());
        if (deathLoot != null) {
            pCompound.put("deathLootItems", deathLoot.createTag(this.registryAccess()));
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);
        this.stanceBreakCounter = pCompound.getInt("stanceBreakCount");
        if (this.hasCustomName()) {
            this.bossEvent.setName(this.getDisplayName());
        }
        int stanceTime = pCompound.getInt("stanceBreakTime");
        if (stanceTime > 0) {
            this.stanceBreakTimer = stanceTime;
            if (level.isClientSide) {
                //todo: sync anim to completed time
                this.animationToPlay = RawAnimation.begin().thenPlay("fire_boss_break_stance");
            }
        }

        this.setSoulMode(pCompound.getBoolean("soulMode"));
        if (pCompound.contains("deathLootItems", 9)) { // 9 for list tag
            var tag = pCompound.getList("deathLootItems", 10);
            this.deathLoot = new SimpleContainer(tag.size());
            this.deathLoot.fromTag(tag, this.registryAccess());
        }
    }

    @Override
    public boolean isAlliedTo(Entity pEntity) {
        return super.isAlliedTo(pEntity) || pEntity.getType().is(ModTags.INFERNAL_ALLIES);
    }

    @Override
    protected PathNavigation createNavigation(Level pLevel) {
        return new NotIdioticNavigation(this, pLevel);
    }
}
