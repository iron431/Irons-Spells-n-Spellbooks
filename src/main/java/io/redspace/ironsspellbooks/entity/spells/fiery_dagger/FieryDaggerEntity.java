package io.redspace.ironsspellbooks.entity.spells.fiery_dagger;

import io.redspace.ironsspellbooks.entity.spells.AbstractMagicProjectile;
import io.redspace.ironsspellbooks.registries.EntityRegistry;
import io.redspace.ironsspellbooks.util.NBT;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

public class FieryDaggerEntity extends AbstractMagicProjectile implements IEntityWithComplexSpawn {
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
    public @Nullable UUID targetEntity = null;
    private @Nullable Entity cachedTarget = null;

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

    int age;
    @Override
    public void tick() {

        if (age++ < delay) {
//            if (hasTarget()) {
//                var target = getTargetEntity();
//                if (target != null) {
//                    var pos = target.getBoundingBox().getCenter();
//                    this.setDeltaMovement(pos.subtract(this.position()).normalize().scale(this.getSpeed()));
//                }
//            }
//            if (isTrackingOwner()) {
//                var owner = getOwner();
//                if (owner != null) {
//                    this.moveTo(owner.position().add(ownerTrack.yRot(owner.getYRot() * Mth.DEG_TO_RAD)));
//                } else {
//                    ownerTrack = null;
//                }
//            }
            this.xOld = getX();
            this.yOld = getY();
            this.zOld = getZ();
            var owner = getOwner();
            float strength = .5f;

            if (owner != null && isTrackingOwner()) {
                Vec3 currentPos = this.position();
                setPos(currentPos.add(owner.getDeltaMovement()));
            }
            var target = getTargetEntity();
            if (target != null) {
                var pos = target.getBoundingBox().getCenter();
                Vec3 targetMotion = pos.subtract(this.position()).normalize().scale(this.getSpeed());
                Vec3 currentMotion = getDeltaMovement();
                this.setDeltaMovement(currentMotion.add(targetMotion.subtract(currentMotion).scale(strength)));
                this.xRotO = getXRot();
                this.yRotO = getYRot();
                Vec3 motion = this.getDeltaMovement();
                float xRot = -((float) (Mth.atan2(motion.horizontalDistance(), motion.y) * (double) (180F / (float) Math.PI)) - 90.0F);
                float yRot = -((float) (Mth.atan2(motion.z, motion.x) * (double) (180F / (float) Math.PI)) + 90.0F);
                this.setXRot(Mth.wrapDegrees(xRot));
                this.setYRot(Mth.wrapDegrees(yRot));
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

    protected float getXRotD(Vec3 lookat) {
        double d0 = lookat.x - this.getX();
        double d1 = lookat.y - this.getY();
        double d2 = lookat.z - this.getZ();
        double d3 = Math.sqrt(d0 * d0 + d2 * d2);
        return !(Math.abs(d1) > 1.0E-5F) && !(Math.abs(d3) > 1.0E-5F) ? 0 : (float) (-(Mth.atan2(d1, d3) * 180.0F / (float) Math.PI));
    }

    protected float getYRotD(Vec3 lookat) {
        double d0 = lookat.x - this.getX();
        double d1 = lookat.z - this.getZ();
        return !(Math.abs(d1) > 1.0E-5F) && !(Math.abs(d0) > 1.0E-5F)
                ? 0
                : (float) (Mth.atan2(d1, d0) * 180.0F / (float) Math.PI) - 90.0F;
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
    }
}
