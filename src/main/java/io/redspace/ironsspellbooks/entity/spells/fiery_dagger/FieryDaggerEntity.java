package io.redspace.ironsspellbooks.entity.spells.fiery_dagger;

import io.redspace.ironsspellbooks.entity.spells.AbstractMagicProjectile;
import io.redspace.ironsspellbooks.registries.EntityRegistry;
import io.redspace.ironsspellbooks.util.NBT;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.Optional;
import java.util.UUID;

public class FieryDaggerEntity extends AbstractMagicProjectile implements IEntityWithComplexSpawn, GeoAnimatable {
    @Override
    public void writeSpawnData(RegistryFriendlyByteBuf buffer) {
        buffer.writeInt(this.delay);
        var tracking = ownerTrack != null;
        buffer.writeBoolean(tracking);
        if (tracking) {
            buffer.writeDouble(ownerTrack.x);
            buffer.writeDouble(ownerTrack.y);
            buffer.writeDouble(ownerTrack.z);
        }
        var target = cachedTarget != null;
        buffer.writeBoolean(target);
        if (target) {
            buffer.writeInt(cachedTarget.getId());
        }
    }

    @Override
    public void readSpawnData(RegistryFriendlyByteBuf buffer) {
        this.delay = buffer.readInt();
        if (buffer.readBoolean()) {
            this.ownerTrack = new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
        }
        if (buffer.readBoolean()) {
            this.cachedTarget = this.level.getEntity(buffer.readInt());
            if (this.cachedTarget != null) {
                this.targetEntity = cachedTarget.getUUID();
            }
        }
    }

    public int delay;
    public @Nullable Vec3 ownerTrack = null;
    private @Nullable UUID targetEntity = null;
    private @Nullable Entity cachedTarget = null;

    public void setTarget(Entity target) {
        this.cachedTarget = target;
        this.targetEntity = target.getUUID();
    }

    public FieryDaggerEntity(EntityType<? extends Projectile> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        setNoGravity(true);
    }

    public FieryDaggerEntity(Level level) {
        this(EntityRegistry.FIERY_DAGGER_PROJECTILE.get(), level);
    }

    public Entity getTargetEntity() {
        if (cachedTarget != null && cachedTarget.isAlive()) {
            return cachedTarget;
        } else if (targetEntity != null && level instanceof ServerLevel serverLevel) {
            this.cachedTarget = serverLevel.getEntity(targetEntity);
            if (cachedTarget == null) {
                this.targetEntity = null;
            }
            return cachedTarget;
        } else {
            return null;
        }
    }

    /**
     * client-synced tick count
     */
    int age;

    @Override
    public void tick() {
        if (age++ < delay) {
            var owner = getOwner();
            float strength = .5f;

            if (owner != null && isTrackingOwner()) {
                //use client delta motion instead of jittery server information
                Vec3 ownerMotion = owner.position().subtract(owner.xOld, owner.yOld, owner.zOld);
                setPos(this.position().add(ownerMotion));
            }
            var target = getTargetEntity();
            if (target != null) {
                var targetPos = target.getBoundingBox().getCenter();
                Vec3 targetMotion = targetPos.subtract(this.position()).normalize().scale(this.getSpeed());
                Vec3 currentMotion = getDeltaMovement();
                deltaMovementOld = currentMotion;
                this.setDeltaMovement(currentMotion.add(targetMotion.subtract(currentMotion).scale(strength)));
                if (tickCount == 1) {
                    deltaMovementOld = getDeltaMovement();
                }
            }
        } else {
            super.tick();
        }
    }

    @Override
    public void trailParticles() {

    }

    @Override
    public void impactParticles(double x, double y, double z) {

    }

    @Override
    public float getSpeed() {
        return 2;
    }

    @Override
    public Optional<Holder<SoundEvent>> getImpactSound() {
        return Optional.empty();
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("delay", delay);
        if (ownerTrack != null) {
            tag.put("ownerTrack", NBT.writeVec3Pos(ownerTrack));
        }
        if (targetEntity != null) {
            tag.putUUID("target", targetEntity);
        }
        tag.putInt("Age", age);
    }

    public boolean isTrackingOwner() {
        return ownerTrack != null;
    }

    public boolean hasTarget() {
        return targetEntity != null;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.delay = tag.getInt("delay");
        if (tag.hasUUID("ownerTrack")) {
            this.ownerTrack = NBT.readVec3(tag.getCompound("ownerTrack"));
        }
        if (tag.hasUUID("target")) {
            this.targetEntity = tag.getUUID("target");
        }
        this.age = tag.getInt("Age");
    }

    /*
    Geckolib
     */
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {

    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    @Override
    public double getTick(Object object) {
        return tickCount;
    }
}
