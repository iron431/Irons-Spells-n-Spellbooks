package io.redspace.ironsspellbooks.entity.spells;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.neoforged.neoforge.entity.PartEntity;


public class ShieldPart extends PartEntity<AbstractShieldEntity> {

    public final AbstractShieldEntity parentEntity;
    public final String name;
    private final EntityDimensions size;
    private final boolean hasCollision;

    public ShieldPart(AbstractShieldEntity shieldEntity, String name, float scaleX, float scaleY, boolean hasCollision) {
        super(shieldEntity);
        this.size = EntityDimensions.scalable(scaleX, scaleY);
        this.refreshDimensions();
        this.parentEntity = shieldEntity;
        this.name = name;
        this.hasCollision = hasCollision;
    }

    @Override
    public boolean canBeCollidedWith() {
        return hasCollision;
    }

    @Override
    public boolean isPickable() {
        return !this.isRemoved();
    }

    @Override
    public boolean hurt(DamageSource pSource, float pAmount) {
        if (!level.isClientSide) {
            if (!parentEntity.hurtThisTick) {
                parentEntity.takeDamage(pSource, pAmount, this.getBoundingBox().getCenter());
                parentEntity.hurtThisTick = true;
            }
        }
        return false;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder pBuilder) {
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compoundTag) {

    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compoundTag) {

    }

    @Override
    public boolean is(Entity entity) {
        return this == entity || this.parentEntity == entity;
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return this.size;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }
}