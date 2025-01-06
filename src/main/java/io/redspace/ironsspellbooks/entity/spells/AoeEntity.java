package io.redspace.ironsspellbooks.entity.spells;

import io.redspace.ironsspellbooks.api.entity.NoKnockbackProjectile;
import io.redspace.ironsspellbooks.api.util.Utils;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidType;

import java.util.List;
import java.util.Optional;

public abstract class AoeEntity extends Projectile implements NoKnockbackProjectile {
    private static final EntityDataAccessor<Float> DATA_RADIUS = SynchedEntityData.defineId(AoeEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> DATA_CIRCULAR = SynchedEntityData.defineId(AoeEntity.class, EntityDataSerializers.BOOLEAN);

    protected float damage;
    protected int duration = 600;
    protected int reapplicationDelay = 10;
    protected int durationOnUse;
    protected float radiusOnUse;
    protected float radiusPerTick;
    protected int effectDuration;

    public AoeEntity(EntityType<? extends Projectile> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        this.noPhysics = true;
        this.blocksBuilding = false;
    }

    protected float particleYOffset() {
        return 0f;
    }

    public void setDamage(float damage) {
        this.damage = damage;
    }

    public float getDamage() {
        return damage;
    }

    public void setEffectDuration(int effectDuration) {
        this.effectDuration = effectDuration;
    }

    public int getEffectDuration() {
        return effectDuration;
    }

    @Override
    public void tick() {
        super.tick();
        if (tickCount > duration) {
            //IronsSpellbooks.LOGGER.debug("AOEProjectile.discarding ({}/{})", tickCount, duration);
            discard();
            return;
        }
        if (!level.isClientSide) {
            if (tickCount % reapplicationDelay == 1) {
                checkHits();
            }
            if (tickCount % 5 == 0)
                this.setRadius(getRadius() + radiusPerTick);
        } else {
            ambientParticles();
        }
        setPos(position().add(getDeltaMovement()));
    }

    protected void checkHits() {
        if (level.isClientSide)
            return;
        List<LivingEntity> targets = this.level.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(this.getInflation().x, this.getInflation().y, this.getInflation().z));
        boolean hit = false;
        var radiusSqr = getRadius();
        radiusSqr *= radiusSqr;
        for (LivingEntity target : targets) {
            if (canHitEntity(target) && (!isCircular() || target.distanceToSqr(this) < radiusSqr)) {
                if (canHitTargetForGroundContext(target)) {
                    applyEffect(target);
                    hit = true;
                }
            }
        }
        if (hit) {
            this.setRadius(getRadius() + radiusOnUse);
            this.duration += durationOnUse;
            onPostHit();
        }
    }

    protected Vec3 getInflation() {
        return Vec3.ZERO;
    }

    /**
     * Logic to fix the area effect cloud issue of not hitting mobs unless they're on the exact same Y. can be overridden for Aoe's with weird-shaped hitboxes.
     */
    protected boolean canHitTargetForGroundContext(LivingEntity target) {
        return target.onGround() || target.getY() - getY() < .5;
    }

    @Override
    protected boolean canHitEntity(Entity pTarget) {
        return (getOwner() != null && pTarget != getOwner() && !getOwner().isAlliedTo(pTarget)) && super.canHitEntity(pTarget);
    }

    /**
     * Server Side, called if any entities were hit
     */
    public void onPostHit() {

    }

    public abstract void applyEffect(LivingEntity target);

    public void ambientParticles() {
        if (!level.isClientSide)
            return;

        getParticle().ifPresent((particle) -> {
            float f = getParticleCount();
            f = Mth.clamp(f * getRadius(), f / 4, f * 10);
            for (int i = 0; i < f; i++) {
                if (f - i < 1 && random.nextFloat() > f - i)
                    return;
                var r = getRadius();
                Vec3 pos;
                if (isCircular()) {
                    var distance = r * (1 - this.random.nextFloat() * this.random.nextFloat());
                    var theta = this.random.nextFloat() * 6.282f; // two pi :nerd:
                    pos = new Vec3(
                            distance * Mth.cos(theta),
                            .2f,
                            distance * Mth.sin(theta)
                    );
                } else {
                    pos = new Vec3(
                            Utils.getRandomScaled(r * .85f),
                            .2f,
                            Utils.getRandomScaled(r * .85f)
                    );
                }
                Vec3 motion = new Vec3(
                        Utils.getRandomScaled(.03f),
                        this.random.nextDouble() * .01f,
                        Utils.getRandomScaled(.03f)
                ).scale(this.getParticleSpeedModifier());
                Vec3 vec3 = new Vec3(getX() + pos.x, getY() + pos.y + 1, getZ() + pos.z);
                vec3 = Utils.moveToRelativeGroundLevel(level, vec3, 1, 2);
                level.addParticle(particle, vec3.x, vec3.y + particleYOffset(), vec3.z, motion.x, motion.y, motion.z);
            }
        });

    }

    protected float getParticleSpeedModifier() {
        return 1f;
    }

    @Override
    public boolean isPushedByFluid(FluidType type) {
        return false;
    }

    @Override
    public boolean isOnFire() {
        return false;
    }

    protected void defineSynchedData(SynchedEntityData.Builder pBuilder) {
        pBuilder.define(DATA_RADIUS, 2F);
        pBuilder.define(DATA_CIRCULAR, false);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> pKey) {
        if (DATA_RADIUS.equals(pKey)) {
            this.refreshDimensions();
            if (getRadius() < .1f) {
                this.discard();
            }
        }

        super.onSyncedDataUpdated(pKey);
    }

    public void setRadius(float pRadius) {
        if (!this.level.isClientSide) {
            this.getEntityData().set(DATA_RADIUS, Mth.clamp(pRadius, 0.0F, 32.0F));
        }
    }

    public void setDuration(int duration) {
        if (!this.level.isClientSide) {
            this.duration = duration;
        }
    }

    public void refreshDimensions() {
        double d0 = this.getX();
        double d1 = this.getY();
        double d2 = this.getZ();
        super.refreshDimensions();
        this.setPos(d0, d1, d2);
    }

    public float getRadius() {
        return this.getEntityData().get(DATA_RADIUS);
    }

    public Boolean isCircular() {
        return this.getEntityData().get(DATA_CIRCULAR);
    }

    public void setCircular() {
        this.getEntityData().set(DATA_CIRCULAR, true);
    }

    public abstract float getParticleCount();

    public abstract Optional<ParticleOptions> getParticle();

    @Override
    public EntityDimensions getDimensions(Pose pPose) {
        return EntityDimensions.scalable(this.getRadius() * 2.0F, 1.2F);
    }

    protected void addAdditionalSaveData(CompoundTag pCompound) {
        pCompound.putInt("Age", this.tickCount);
        pCompound.putInt("Duration", this.duration);
        pCompound.putInt("ReapplicationDelay", this.reapplicationDelay);
        pCompound.putInt("DurationOnUse", this.durationOnUse);
        pCompound.putFloat("RadiusOnUse", this.radiusOnUse);
        pCompound.putFloat("RadiusPerTick", this.radiusPerTick);
        pCompound.putFloat("Radius", this.getRadius());
        pCompound.putFloat("Damage", this.getDamage());
        pCompound.putBoolean("Circular", this.isCircular());
        pCompound.putInt("EffectDuration", this.effectDuration);
        super.addAdditionalSaveData(pCompound);

    }

    protected void readAdditionalSaveData(CompoundTag pCompound) {
        this.tickCount = pCompound.getInt("Age");
        if (pCompound.getInt("Duration") > 0)
            this.duration = pCompound.getInt("Duration");
        if (pCompound.getInt("ReapplicationDelay") > 0)
            this.reapplicationDelay = pCompound.getInt("ReapplicationDelay");
        if (pCompound.getInt("Radius") > 0)
            this.setRadius(pCompound.getFloat("Radius"));
        if (pCompound.getInt("DurationOnUse") > 0)
            this.durationOnUse = pCompound.getInt("DurationOnUse");
        if (pCompound.getInt("RadiusOnUse") > 0)
            this.radiusOnUse = pCompound.getFloat("RadiusOnUse");
        if (pCompound.getInt("RadiusPerTick") > 0)
            this.radiusPerTick = pCompound.getFloat("RadiusPerTick");
        if (pCompound.getInt("EffectDuration") > 0)
            this.effectDuration = pCompound.getInt("EffectDuration");
        this.setDamage(pCompound.getFloat("Damage"));
        if (pCompound.getBoolean("Circular"))
            setCircular();

        super.readAdditionalSaveData(pCompound);

    }

    @Override
    public boolean ignoreExplosion(Explosion explosion) {
        return true;
    }
}
