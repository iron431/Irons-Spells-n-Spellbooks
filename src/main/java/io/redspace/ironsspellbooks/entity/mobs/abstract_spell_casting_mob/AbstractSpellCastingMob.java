package io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob;

import com.google.common.collect.Maps;
import io.redspace.ironsspellbooks.IronsSpellbooks;
import io.redspace.ironsspellbooks.api.entity.IMagicEntity;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.capabilities.magic.SyncedSpellData;
import io.redspace.ironsspellbooks.spells.ender.TeleportSpell;
import io.redspace.ironsspellbooks.spells.fire.BurningDashSpell;
import io.redspace.ironsspellbooks.util.Log;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.LookControl;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.Nullable;
import java.util.HashMap;

public abstract class AbstractSpellCastingMob extends PathfinderMob implements GeoEntity, IMagicEntity {
    public static final ResourceLocation modelResource = new ResourceLocation(IronsSpellbooks.MODID, "geo/abstract_casting_mob.geo.json");
    public static final ResourceLocation textureResource = new ResourceLocation(IronsSpellbooks.MODID, "textures/entity/abstract_casting_mob/abstract_casting_mob.png");
    public static final ResourceLocation animationInstantCast = new ResourceLocation(IronsSpellbooks.MODID, "animations/casting_animations.json");
    //private static final EntityDataAccessor<SyncedSpellData> DATA_SPELL = SynchedEntityData.defineId(AbstractSpellCastingMob.class, SyncedSpellData.SYNCED_SPELL_DATA);
    private static final EntityDataAccessor<Boolean> DATA_CANCEL_CAST = SynchedEntityData.defineId(AbstractSpellCastingMob.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_DRINKING_POTION = SynchedEntityData.defineId(AbstractSpellCastingMob.class, EntityDataSerializers.BOOLEAN);
    private final MagicData playerMagicData = new MagicData(true);
    private static final AttributeModifier SPEED_MODIFIER_DRINKING = new AttributeModifier(IronsSpellbooks.id("potion_slowdown"), -0.15D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);

    private @Nullable SpellData castingSpell;
    private final HashMap<String, AbstractSpell> spells = Maps.newHashMap();
    private int drinkTime;
    public boolean hasUsedSingleAttack;
    private boolean recreateSpell;

    protected AbstractSpellCastingMob(EntityType<? extends PathfinderMob> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        playerMagicData.setSyncedData(new SyncedSpellData(this));
        this.lookControl = createLookControl();
    }

    public boolean getHasUsedSingleAttack() {
        return hasUsedSingleAttack;
    }

    @Override
    public void setHasUsedSingleAttack(boolean hasUsedSingleAttack) {
        this.hasUsedSingleAttack = hasUsedSingleAttack;
    }

    //FIXME: 1.21: is #getPassengerRidingPosition the new name for this method?
    //@Override
    //public double getMyRidingOffset() {
    //    return -0.5;
    //}

    @Override
    public Vec3 getPassengerRidingPosition(Entity pEntity) {
        return super.getPassengerRidingPosition(pEntity);
    }

    @Override
    public void rideTick() {
        super.rideTick();
        if (this.getVehicle() instanceof PathfinderMob pathfindermob) {
            pathfindermob.yBodyRot = this.yBodyRot;
        }
    }

    protected LookControl createLookControl() {
        return new LookControl(this) {
            @Override
            protected boolean resetXRotOnTick() {
                return getTarget() == null;
            }
        };
    }

    public MagicData getMagicData() {
        return playerMagicData;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder pBuilder) {
        super.defineSynchedData(pBuilder);
        //pBuilder.define(DATA_SPELL, new SyncedSpellData(-1));
        pBuilder.define(DATA_CANCEL_CAST, false);
        pBuilder.define(DATA_DRINKING_POTION, false);
    }

    public boolean isDrinkingPotion() {
        return entityData.get(DATA_DRINKING_POTION);
    }

    protected void setDrinkingPotion(boolean drinkingPotion) {
        this.entityData.set(DATA_DRINKING_POTION, drinkingPotion);
    }

    @Override
    public boolean canBeLeashed() {
        return false;
    }

    public void startDrinkingPotion() {
        if (!level.isClientSide) {
            setDrinkingPotion(true);
            drinkTime = 35;
            AttributeInstance attributeinstance = this.getAttribute(Attributes.MOVEMENT_SPEED);
            attributeinstance.removeModifier(SPEED_MODIFIER_DRINKING);
            attributeinstance.addTransientModifier(SPEED_MODIFIER_DRINKING);
        }
    }

    private void finishDrinkingPotion() {
        setDrinkingPotion(false);
        this.heal(Math.min(Math.max(10, getMaxHealth() / 10), getMaxHealth() / 4));
        this.getAttribute(Attributes.MOVEMENT_SPEED).removeModifier(SPEED_MODIFIER_DRINKING);
        if (!this.isSilent()) {
            this.level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.WITCH_DRINK, this.getSoundSource(), 1.0F, 0.8F + this.random.nextFloat() * 0.4F);
        }
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> pKey) {
        super.onSyncedDataUpdated(pKey);

        if (!level.isClientSide) {
            return;
        }

        if (pKey.id() == DATA_CANCEL_CAST.id()) {
            if (Log.SPELL_DEBUG) {
                IronsSpellbooks.LOGGER.debug("ASCM.onSyncedDataUpdated.1 this.isCasting:{}, playerMagicData.isCasting:{} isClient:{}", isCasting(), playerMagicData == null ? "null" : playerMagicData.isCasting(), this.level.isClientSide());
            }
            cancelCast();
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (level.isClientSide) {
            this.noCulling = this.isAnimating();
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag pCompound) {
        super.addAdditionalSaveData(pCompound);
        playerMagicData.getSyncedData().saveNBTData(pCompound, level.registryAccess());
        pCompound.putBoolean("usedSpecial", hasUsedSingleAttack);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);
        var syncedSpellData = new SyncedSpellData(this);
        syncedSpellData.loadNBTData(pCompound, level.registryAccess());
        if (syncedSpellData.isCasting()) {
            this.recreateSpell = true;
        }
        playerMagicData.setSyncedData(syncedSpellData);
        hasUsedSingleAttack = pCompound.getBoolean("usedSpecial");
    }

    public void cancelCast() {
        if (isCasting()) {
            if (level.isClientSide) {
                cancelCastAnimation = true;
            } else {
                //Need to ensure we pass a different value if we want the data to sync
                entityData.set(DATA_CANCEL_CAST, !entityData.get(DATA_CANCEL_CAST));
            }

            castComplete();
        }

    }

    public void castComplete() {
        if (!level.isClientSide) {
            if (castingSpell != null) {
                castingSpell.getSpell().onServerCastComplete(level, castingSpell.getLevel(), this, playerMagicData, false);
            }
        } else {
            playerMagicData.resetCastingState();
        }

        castingSpell = null;
    }

    public void setSyncedSpellData(SyncedSpellData syncedSpellData) {
        if (!level.isClientSide) {
            return;
        }

        var isCasting = playerMagicData.isCasting();
        playerMagicData.setSyncedData(syncedSpellData);
        castingSpell = playerMagicData.getCastingSpell();

        if (Log.SPELL_DEBUG) {
            IronsSpellbooks.LOGGER.debug("ASCM.setSyncedSpellData playerMagicData:{}, priorIsCastingState:{}, spell:{}", playerMagicData, isCasting, castingSpell);
        }

        if (castingSpell == null) {
            return;
        }

        if (!playerMagicData.isCasting() && isCasting) {
            castComplete();
        } else if (playerMagicData.isCasting() && !isCasting)/* if (syncedSpellData.getCastingSpellType().getCastType() == CastType.CONTINUOUS)*/ {
            var spell = playerMagicData.getCastingSpell().getSpell();

            initiateCastSpell(spell, playerMagicData.getCastingSpellLevel());

            if (castingSpell.getSpell().getCastType() == CastType.INSTANT) {
                instantCastSpellType = castingSpell.getSpell();
                castingSpell.getSpell().onClientPreCast(level, castingSpell.getLevel(), this, InteractionHand.MAIN_HAND, playerMagicData);
                castComplete();
            }
        }
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (recreateSpell) {
            recreateSpell = false;
            var syncedSpellData = playerMagicData.getSyncedData();
            var spell = SpellRegistry.getSpell(syncedSpellData.getCastingSpellId());
            this.initiateCastSpell(spell, syncedSpellData.getCastingSpellLevel());
            //setSyncedSpellData(syncedSpellData);
        }

        if (isDrinkingPotion()) {
            if (drinkTime-- <= 0) {
                finishDrinkingPotion();
            } else if (drinkTime % 4 == 0) {
                if (!this.isSilent()) {
                    this.level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.GENERIC_DRINK, this.getSoundSource(), 1.0F, Utils.random.nextFloat() * 0.1F + 0.9F);
                }
            }
        }

        if (castingSpell == null) {
            return;
        }

        playerMagicData.handleCastDuration();

        if (playerMagicData.isCasting()) {
            castingSpell.getSpell().onServerCastTick(level, castingSpell.getLevel(), this, playerMagicData);
        }

        if (Log.SPELL_DEBUG) {
            IronsSpellbooks.LOGGER.debug("ASCM.customServerAiStep.1");
        }

        this.forceLookAtTarget(getTarget());

        if (playerMagicData.getCastDurationRemaining() <= 0) {
            if (Log.SPELL_DEBUG) {
                IronsSpellbooks.LOGGER.debug("ASCM.customServerAiStep.2");
            }

            if (castingSpell.getSpell().getCastType() == CastType.LONG || castingSpell.getSpell().getCastType() == CastType.INSTANT) {
                if (Log.SPELL_DEBUG) {
                    IronsSpellbooks.LOGGER.debug("ASCM.customServerAiStep.3");
                }
                castingSpell.getSpell().onCast(level, castingSpell.getLevel(), this, CastSource.MOB, playerMagicData);
            }
            castComplete();
        } else if (castingSpell.getSpell().getCastType() == CastType.CONTINUOUS) {
            if ((playerMagicData.getCastDurationRemaining() + 1) % 10 == 0) {
                castingSpell.getSpell().onCast(level, castingSpell.getLevel(), this, CastSource.MOB, playerMagicData);
            }
        }
    }

    public void initiateCastSpell(AbstractSpell spell, int spellLevel) {
        if (Log.SPELL_DEBUG) {
            IronsSpellbooks.LOGGER.debug("ASCM.initiateCastSpell: spellType:{} spellLevel:{}, isClient:{}", spell.getSpellId(), spellLevel, level.isClientSide);
        }

        if (spell == SpellRegistry.none()) {
            castingSpell = null;
            return;
        }

        if (level.isClientSide) {
            cancelCastAnimation = false;
        }

        castingSpell = new SpellData(spell, spellLevel);

        if (getTarget() != null) {
            forceLookAtTarget(getTarget());
        }

        if (!level.isClientSide && !castingSpell.getSpell().checkPreCastConditions(level, spellLevel, this, playerMagicData)) {
            if (Log.SPELL_DEBUG) {
                IronsSpellbooks.LOGGER.debug("ASCM.precastfailed: spellType:{} spellLevel:{}, isClient:{}", spell.getSpellId(), spellLevel, level.isClientSide);
            }

            castingSpell = null;
            return;
        }

        if (spell == SpellRegistry.TELEPORT_SPELL.get() || spell == SpellRegistry.FROST_STEP_SPELL.get()) {
            setTeleportLocationBehindTarget(10);
        } else if (spell == SpellRegistry.BLOOD_STEP_SPELL.get()) {
            setTeleportLocationBehindTarget(3);
        } else if (spell == SpellRegistry.BURNING_DASH_SPELL.get()) {
            setBurningDashDirectionData();
        }

        playerMagicData.initiateCast(castingSpell.getSpell(), castingSpell.getLevel(), castingSpell.getSpell().getEffectiveCastTime(castingSpell.getLevel(), this), CastSource.MOB, SpellSelectionManager.MAINHAND);

        if (!level.isClientSide) {
            castingSpell.getSpell().onServerPreCast(level, castingSpell.getLevel(), this, playerMagicData);
        }
    }

    public void notifyDangerousProjectile(Projectile projectile) {
    }

    public boolean isCasting() {
        return playerMagicData.isCasting();
    }

    public boolean setTeleportLocationBehindTarget(int distance) {
        var target = getTarget();
        boolean valid = false;
        if (target != null) {
            var rotation = target.getLookAngle().normalize().scale(-distance);
            var pos = target.position();
            var teleportPos = rotation.add(pos);

            for (int i = 0; i < 24; i++) {
                Vec3 randomness = Utils.getRandomVec3(.15f * i).multiply(1, 0, 1);
                teleportPos = Utils.moveToRelativeGroundLevel(level, target.position().subtract(new Vec3(0, 0, distance / (float) (i / 7 + 1)).yRot(-(target.getYRot() + i * 45) * Mth.DEG_TO_RAD)).add(randomness), 5);
                teleportPos = new Vec3(teleportPos.x, teleportPos.y + .1f, teleportPos.z);
                var reposBB = this.getBoundingBox().move(teleportPos.subtract(this.position()));
                //IronsSpellbooks.LOGGER.debug("setTeleportLocationBehindTarget attempt to teleport to {}:", reposBB.getCenter());
                if (!level.collidesWithSuffocatingBlock(this, reposBB.inflate(-.05f))) {
                    //IronsSpellbooks.LOGGER.debug("\n\n\n~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n\n\nsetTeleportLocationBehindTarget: {} {} {} empty. teleporting\n\n\n~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n\n\n", reposBB.minX, reposBB.minY, reposBB.minZ);
                    valid = true;
                    break;
                }
                //IronsSpellbooks.LOGGER.debug("fail");

            }
            if (valid) {
                if (Log.SPELL_DEBUG) {
                    //IronsSpellbooks.LOGGER.debug("ASCM.setTeleportLocationBehindTarget: valid, pos:{}, isClient:{}", teleportPos, level.isClientSide());
                }
                playerMagicData.setAdditionalCastData(new TeleportSpell.TeleportData(teleportPos));
            } else {
                if (Log.SPELL_DEBUG) {
                    //IronsSpellbooks.LOGGER.debug("ASCM.setTeleportLocationBehindTarget: invalid, pos:{}, isClient:{}", teleportPos, level.isClientSide());
                }
                playerMagicData.setAdditionalCastData(new TeleportSpell.TeleportData(this.position()));

            }
        } else {
            if (Log.SPELL_DEBUG) {
                //IronsSpellbooks.LOGGER.debug("ASCM.setTeleportLocationBehindTarget: no target, isClient:{}", level.isClientSide());
            }
            playerMagicData.setAdditionalCastData(new TeleportSpell.TeleportData(this.position()));
        }
        return valid;
    }

    public void setBurningDashDirectionData() {
        playerMagicData.setAdditionalCastData(new BurningDashSpell.BurningDashDirectionOverrideCastData());
    }

    private void forceLookAtTarget(LivingEntity target) {
        if (target != null) {
            double d0 = target.getX() - this.getX();
            double d2 = target.getZ() - this.getZ();
            double d1 = target.getEyeY() - this.getEyeY();

            double d3 = Math.sqrt(d0 * d0 + d2 * d2);
            float f = (float) (Mth.atan2(d2, d0) * (double) (180F / (float) Math.PI)) - 90.0F;
            float f1 = (float) (-(Mth.atan2(d1, d3) * (double) (180F / (float) Math.PI)));
            this.setXRot(f1 % 360);
            this.setYRot(f % 360);
        }
    }

    /**
     * GeckoLib Animations
     **/
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private AbstractSpell lastCastSpellType = SpellRegistry.none();
    private AbstractSpell instantCastSpellType = SpellRegistry.none();
    private boolean cancelCastAnimation = false;
    private boolean animatingLegs = false;
    private final AnimationController animationControllerOtherCast = new AnimationController(this, "other_casting", 0, this::otherCastingPredicate);
    private final AnimationController animationControllerInstantCast = new AnimationController(this, "instant_casting", 0, this::instantCastingPredicate);
    private final AnimationController animationControllerLongCast = new AnimationController(this, "long_casting", 0, this::longCastingPredicate);

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    public void triggerAnim(@org.jetbrains.annotations.Nullable String controllerName, String animName) {
        GeoEntity.super.triggerAnim(controllerName, animName);
    }

    @Override
    public double getBoneResetTime() {
        return 5;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {
        controllerRegistrar.add(animationControllerOtherCast);
        controllerRegistrar.add(animationControllerInstantCast);
        controllerRegistrar.add(animationControllerLongCast);
        //controllerRegistrar.add(new AnimationController(this, "idle", 0, this::idlePredicate));
    }

    private PlayState instantCastingPredicate(AnimationState event) {
        if (cancelCastAnimation) {
            return PlayState.STOP;
        }

        var controller = event.getController();
        if (instantCastSpellType != SpellRegistry.none() && controller.getAnimationState() == AnimationController.State.STOPPED) {
            setStartAnimationFromSpell(controller, instantCastSpellType);
            instantCastSpellType = SpellRegistry.none();
        }
        return PlayState.CONTINUE;
    }

    private PlayState longCastingPredicate(AnimationState event) {
        var controller = event.getController();

        if (cancelCastAnimation || (controller.getAnimationState() == AnimationController.State.STOPPED && !(isCasting() && castingSpell != null && castingSpell.getSpell().getCastType() == CastType.LONG))) {
            return PlayState.STOP;
        }

        if (isCasting()) {
            if (controller.getAnimationState() == AnimationController.State.STOPPED) {
                setStartAnimationFromSpell(controller, castingSpell.getSpell());
            }
        } else if (lastCastSpellType.getCastType() == CastType.LONG) {
            setFinishAnimationFromSpell(controller, lastCastSpellType);
        }

        return PlayState.CONTINUE;
    }

    private PlayState otherCastingPredicate(AnimationState event) {
        if (cancelCastAnimation) {
            return PlayState.STOP;
        }

        var controller = event.getController();
        if (isCasting() && castingSpell != null && controller.getAnimationState() == AnimationController.State.STOPPED) {
            if (castingSpell.getSpell().getCastType() == CastType.CONTINUOUS) {
                setStartAnimationFromSpell(controller, castingSpell.getSpell());
            }
            return PlayState.CONTINUE;
        }

        if (isCasting()) {
            return PlayState.CONTINUE;
        } else {
            return PlayState.STOP;
        }
    }

    private void setStartAnimationFromSpell(AnimationController controller, AbstractSpell spell) {
        spell.getCastStartAnimation().getForMob().ifPresentOrElse(animationBuilder -> {
            if (Log.SPELL_DEBUG) {
                IronsSpellbooks.LOGGER.debug("ASCM.setStartAnimationFromSpell {}", animationBuilder);
            }
            controller.forceAnimationReset();
            controller.setAnimation(animationBuilder);
            lastCastSpellType = spell;
            cancelCastAnimation = false;
            animatingLegs = spell.getCastStartAnimation().animatesLegs;
        }, () -> {
            if (Log.SPELL_DEBUG) {
                IronsSpellbooks.LOGGER.debug("ASCM.setStartAnimationFromSpell cancelCastAnimation");
            }
            cancelCastAnimation = true;
        });
    }

    private void setFinishAnimationFromSpell(AnimationController controller, AbstractSpell spell) {
        if (spell.getCastFinishAnimation().isPass) {
            cancelCastAnimation = false;
            return;
        }
        spell.getCastFinishAnimation().getForMob().ifPresentOrElse(animationBuilder -> {
            if (Log.SPELL_DEBUG) {
                IronsSpellbooks.LOGGER.debug("ASCM.setFinishAnimationFromSpell {}", animationBuilder);
            }
            controller.forceAnimationReset();
            controller.setAnimation(animationBuilder);
            lastCastSpellType = SpellRegistry.none();
            cancelCastAnimation = false;
        }, () -> {
            if (Log.SPELL_DEBUG) {
                IronsSpellbooks.LOGGER.debug("ASCM.setFinishAnimationFromSpell cancelCastAnimation");
            }
            cancelCastAnimation = true;
        });
    }

    public boolean isAnimating() {
        return isCasting()
                || (animationControllerLongCast.getAnimationState() != AnimationController.State.STOPPED)
                || (animationControllerOtherCast.getAnimationState() != AnimationController.State.STOPPED)
                || (animationControllerInstantCast.getAnimationState() != AnimationController.State.STOPPED);
    }

    public boolean shouldBeExtraAnimated() {
        return true;
    }

    public boolean shouldAlwaysAnimateHead() {
        return true;
    }

    public boolean shouldAlwaysAnimateLegs() {
        return !animatingLegs;
    }

    public boolean shouldPointArmsWhileCasting() {
        return true;
    }

    public boolean bobBodyWhileWalking() {
        return true;
    }

    public boolean shouldSheathSword() {
        return false;
    }
}
