package io.redspace.ironsspellbooks.entity.spells.fireball;

import io.redspace.ironsspellbooks.IronsSpellbooks;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import io.redspace.ironsspellbooks.config.ServerConfigs;
import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.entity.spells.AbstractMagicProjectile;
import io.redspace.ironsspellbooks.registries.EntityRegistry;
import io.redspace.ironsspellbooks.util.ParticleHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.UUID;

public class SmallMagicFireball extends AbstractMagicProjectile implements IEntityWithComplexSpawn {
    public SmallMagicFireball(EntityType<? extends Projectile> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        this.setNoGravity(true);
    }

    public SmallMagicFireball(Level pLevel, LivingEntity pShooter) {
        this(EntityRegistry.SMALL_FIREBALL_PROJECTILE.get(), pLevel);
        this.setOwner(pShooter);
    }

    public void shoot(Vec3 rotation, float inaccuracy) {
        var speed = rotation.length();
        Vec3 offset = Utils.getRandomVec3(1).normalize().scale(inaccuracy);
        var motion = rotation.normalize().add(offset).normalize().scale(speed);
        super.shoot(motion);
    }

    @Nullable
    Entity cachedHomingTarget;
    @Nullable
    UUID homingTargetUUID;

    @Nullable
    public Entity getHomingTarget() {
        if (this.cachedHomingTarget != null && !this.cachedHomingTarget.isRemoved()) {
            return this.cachedHomingTarget;
        } else if (this.homingTargetUUID != null && this.level instanceof ServerLevel) {
            this.cachedHomingTarget = ((ServerLevel) this.level).getEntity(this.homingTargetUUID);
            return this.cachedHomingTarget;
        } else {
            return null;
        }
    }

    public void setHomingTarget(LivingEntity entity) {
        this.homingTargetUUID = entity.getUUID();
        this.cachedHomingTarget = entity;
    }

    @Override
    public void tick() {
        super.tick();
        var homingTarget = getHomingTarget();
        if (homingTarget != null) {
            if (!doHomingTowards(homingTarget)) {
                this.homingTargetUUID = null;
                this.cachedHomingTarget = null;
            }
        }
    }

    /**
     * @return if homing should continue
     */
    private boolean doHomingTowards(Entity entity) {
        if (entity.isRemoved()) {
            return false;
        }
        var motion = this.getDeltaMovement();
        var speed = this.getDeltaMovement().length();
        var delta = entity.getBoundingBox().getCenter().subtract(this.position()).add(entity.getDeltaMovement());
        float f = .08f;
        var newMotion = new Vec3(Mth.lerp(f, motion.x, delta.x), Mth.lerp(f, motion.y, delta.y), Mth.lerp(f, motion.z, delta.z)).normalize().scale(speed);
        this.setDeltaMovement(newMotion);
        // after a decent bit into our flight, if we are past our target, lose tracking
        return this.tickCount <= 10 || !(newMotion.dot(delta) < 0);
    }

    @Override
    public void trailParticles() {
        Vec3 vec3 = getDeltaMovement();
        double d0 = this.getX() - vec3.x;
        double d1 = this.getY() - vec3.y;
        double d2 = this.getZ() - vec3.z;
        var count = Mth.clamp((int) (vec3.lengthSqr() * 4), 1, 5);
        for (int i = 0; i < count; i++) {
            Vec3 random = Utils.getRandomVec3(.1);
            var f = i / ((float) count);
            var x = Mth.lerp(f, d0, this.getX());
            var y = Mth.lerp(f, d1, this.getY());
            var z = Mth.lerp(f, d2, this.getZ());
            this.level.addParticle(ParticleHelper.EMBERS, x - random.x, y + 0.5D - random.y, z - random.z, random.x * .5f, random.y * .5f, random.z * .5f);
        }
    }

    @Override
    public void impactParticles(double x, double y, double z) {
        MagicManager.spawnParticles(level, ParticleTypes.LAVA, x, y, z, 5, .1, .1, .1, .25, true);
    }

    @Override
    public float getSpeed() {
        return 1.85f;
    }

    @Override
    public Optional<Holder<SoundEvent>> getImpactSound() {
        return Optional.empty();
    }

    @Override
    protected void onHitEntity(EntityHitResult pResult) {
        if (!this.level.isClientSide) {
            var target = pResult.getEntity();
            var owner = getOwner();
            DamageSources.applyDamage(target, damage, SpellRegistry.BLAZE_STORM_SPELL.get().getDamageSource(this, owner));
            if (target.getUUID().equals(homingTargetUUID)) {
                target.invulnerableTime = 0;
            }
        }
    }

    protected void onHitBlock(BlockHitResult pResult) {
        super.onHitBlock(pResult);
        if (!this.level.isClientSide) {
            if (ServerConfigs.SPELL_GREIFING.get()) {
                BlockPos blockpos = pResult.getBlockPos().relative(pResult.getDirection());
                if (this.level.isEmptyBlock(blockpos)) {
                    this.level.setBlockAndUpdate(blockpos, BaseFireBlock.getState(this.level, blockpos));
                }
            }
        }
    }

    protected void onHit(HitResult pResult) {
        super.onHit(pResult);
        if (!this.level.isClientSide) {
            this.discard();
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (this.homingTargetUUID != null) {
            tag.putUUID("homingTarget", homingTargetUUID);
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("homingTarget", 11)) {
            this.homingTargetUUID = tag.getUUID("homingTarget");
        }
    }

    @Override
    public void writeSpawnData(RegistryFriendlyByteBuf buffer) {
        IronsSpellbooks.LOGGER.debug("Smallmagicfireball.writespawndata: {}", homingTargetUUID);
        var owner = getOwner();
        buffer.writeInt(owner == null ? 0 : owner.getId());
        var homingTarget = getHomingTarget();
        buffer.writeInt(homingTarget == null ? 0 : homingTarget.getId());

    }

    @Override
    public void readSpawnData(RegistryFriendlyByteBuf additionalData) {
        Entity owner = this.level.getEntity(additionalData.readInt());
        if (owner != null) {
            this.setOwner(owner);
        }
        Entity homingTarget = this.level.getEntity(additionalData.readInt());
        if (homingTarget != null) {
            this.cachedHomingTarget = homingTarget;
            this.homingTargetUUID = homingTarget.getUUID();
        }
        IronsSpellbooks.LOGGER.debug("Smallmagicfireball.readSpawnData: {}", homingTargetUUID);
    }


}
