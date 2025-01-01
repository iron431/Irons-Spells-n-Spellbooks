package io.redspace.ironsspellbooks.entity.spells.small_magic_arrow;

import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.entity.spells.AbstractMagicProjectile;
import io.redspace.ironsspellbooks.registries.EntityRegistry;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

public class SmallMagicArrow extends AbstractMagicProjectile {
    private static final EntityDataAccessor<Boolean> IN_GROUND = SynchedEntityData.defineId(SmallMagicArrow.class, EntityDataSerializers.BOOLEAN);

    public SmallMagicArrow(EntityType<? extends Projectile> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    public SmallMagicArrow(Level levelIn, Entity shooter) {
        this(EntityRegistry.SMALL_MAGIC_ARROW.get(), levelIn);
        setOwner(shooter);
    }

    @Override
    public void shoot(Vec3 rotation) {
        this.setDeltaMovement(rotation);
    }

    public int shakeTime;
    protected boolean inGround;

    @Override
    public void tick() {
        if (this.shakeTime > 0) {
            --this.shakeTime;
        }
        if (!inGround) {
            super.tick();
        } else {
            deltaMovementOld = getDeltaMovement();
            if (tickCount > EXPIRE_TIME) {
                discard();
                return;
            }
            if (shouldFall()) {
                inGround = false;
                this.setDeltaMovement(getDeltaMovement().normalize().scale(0.05f));
            }
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder pBuilder) {
        super.defineSynchedData(pBuilder);
        pBuilder.define(IN_GROUND, false);
    }

    private boolean shouldFall() {
        return this.inGround && this.level.noCollision((new AABB(this.position(), this.position())).inflate(0.06D));
    }

    @Override
    protected void onHitBlock(BlockHitResult pResult) {
        super.onHitBlock(pResult);
        Vec3 vec3 = pResult.getLocation().subtract(this.getX(), this.getY(), this.getZ());
        this.setDeltaMovement(vec3);
        Vec3 vec31 = vec3.normalize().scale(0.05F);
        this.setPosRaw(this.getX() - vec31.x, this.getY() - vec31.y, this.getZ() - vec31.z);
        this.playSound(SoundEvents.ARROW_HIT, 1.0F, 1.2F / (this.random.nextFloat() * 0.2F + 0.9F));
        this.inGround = true;
        this.shakeTime = 7;
    }

    @Override
    protected void onHitEntity(EntityHitResult entityHitResult) {
        if (level.isClientSide)
            return;
        Entity entity = entityHitResult.getEntity();
        boolean hit = DamageSources.applyDamage(entity, getDamage(), SpellRegistry.ARROW_VOLLEY_SPELL.get().getDamageSource(this, getOwner()));
        //TODO: add evasion and stuff. Also do this for all other projectiles?
        boolean ignore = entity.getType() == EntityType.ENDERMAN;
        if (hit) {
            this.discard();
        } else {
            this.setDeltaMovement(this.getDeltaMovement().scale(-0.1D));
            this.setYRot(this.getYRot() + 180.0F);
            this.yRotO += 180.0F;
        }


    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("inGround", this.inGround);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.inGround = tag.getBoolean("inGround");
    }

    @Override
    public void trailParticles() {
    }

    @Override
    public void impactParticles(double x, double y, double z) {
    }

    @Override
    public float getSpeed() {
        return 2f;
    }

    @Override
    public Optional<Holder<SoundEvent>> getImpactSound() {
        return Optional.empty();
    }
}
