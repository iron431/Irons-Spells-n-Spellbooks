package io.redspace.ironsspellbooks.entity.spells.fiery_dagger;

import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.entity.mobs.wizards.fire_boss.FireBossEntity;
import io.redspace.ironsspellbooks.entity.spells.AbstractMagicProjectile;
import io.redspace.ironsspellbooks.entity.spells.magma_ball.FireField;
import io.redspace.ironsspellbooks.particle.BlastwaveParticleOptions;
import io.redspace.ironsspellbooks.registries.EntityRegistry;
import io.redspace.ironsspellbooks.registries.SoundRegistry;
import io.redspace.ironsspellbooks.util.NBT;
import io.redspace.ironsspellbooks.util.ParticleHelper;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.Optional;
import java.util.UUID;

public class FieryDaggerEntity extends AbstractMagicProjectile implements IEntityWithComplexSpawn, GeoAnimatable {


    public int delay;
    public @Nullable Vec3 ownerTrack = null;
    private @Nullable UUID targetEntity = null;
    private @Nullable Entity cachedTarget = null;
    /**
     * client-synced tick count
     */
    int age;
    /**
     * flag for the special behavior when we are formed as a circle of daggers in the ground
     */
    boolean isGrounded;

    public FieryDaggerEntity(EntityType<? extends Projectile> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        setNoGravity(true);
    }

    public FieryDaggerEntity(Level level) {
        this(EntityRegistry.FIERY_DAGGER_PROJECTILE.get(), level);
    }

    public void setTarget(Entity target) {
        this.cachedTarget = target;
        this.targetEntity = target.getUUID();
    }

    public boolean isTrackingOwner() {
        return ownerTrack != null;
    }

    public boolean hasTarget() {
        return targetEntity != null;
    }

    public boolean isSpawnDagger() {
        //repurpose explosion radius as summon radius; if present, we summon on impact
        return explosionRadius > 0;
    }

    private void createFireField() {
        FireField fireField = new FireField(this.level);
        fireField.setOwner(level.getNearestEntity(FireBossEntity.class, TargetingConditions.forNonCombat().ignoreLineOfSight().ignoreInvisibilityTesting(), null, getX(), getY(), getZ(), this.getBoundingBox().inflate(32)));
        fireField.setPos(this.position());
        fireField.setRadius(this.explosionRadius + 1);
        fireField.setCircular();
        fireField.setDamage(this.getDamage());
        fireField.setDuration(20 * 15);
        fireField.setDelay(this.delay + 15);
        level.addFreshEntity(fireField);
    }

    @Override
    protected void onHitEntity(EntityHitResult entityHitResult) {
        super.onHitEntity(entityHitResult);
        //TODO: damage source
        DamageSources.applyDamage(entityHitResult.getEntity(), getDamage(), SpellRegistry.BLOOD_NEEDLES_SPELL.get().getDamageSource(this, getOwner()));
        entityHitResult.getEntity().invulnerableTime = 0;
    }

    @Override
    protected void onHit(HitResult hitresult) {
        super.onHit(hitresult);
        if (isSpawnDagger() && level instanceof ServerLevel) {
            createDaggerZone(Utils.moveToRelativeGroundLevel(level, hitresult.getLocation(), 3));
        }
        discard();
    }

    public void createDaggerZone(Vec3 center) {
        MagicManager.spawnParticles(level, new BlastwaveParticleOptions(new Vector3f(1, .6f, 0.3f), explosionRadius + 1), center.x, center.y + .15, center.z, 1, 0, 0, 0, 0, false);
        playSound(SoundRegistry.FIRE_CAST.get(), 2f, Utils.random.nextIntBetweenInclusive(80, 110) * .01f);

        float spawnRadius = this.explosionRadius;
        float density = 1f;
        int rings = (int) (spawnRadius * density);
        float ringSpacing = 1 / density;
        for (int i = 1; i < rings; i++) {
            float ringRadius = ringSpacing * i;
            int daggerCount = (int) (ringRadius * Mth.TWO_PI);
            float angle = 360f / daggerCount * Mth.DEG_TO_RAD;
            for (int j = 0; j < daggerCount; j++) {
                Vec3 jitter = Utils.getRandomVec3(ringSpacing * .4f);
                Vec3 pos = Utils.moveToRelativeGroundLevel(level, center.add(ringRadius * Mth.sin(angle * j), 0, ringRadius * Mth.cos(angle * j)).add(jitter), 8);
                FieryDaggerEntity dagger = new FieryDaggerEntity(level);
                dagger.setOwner(this.getOwner());
                dagger.setDamage(this.getDamage());
                dagger.delay = this.delay + Utils.random.nextInt(20);
                dagger.setDeltaMovement(0, getSpeed(), 0);
                dagger.deltaMovementOld = dagger.getDeltaMovement();
                dagger.moveTo(pos);
                dagger.isGrounded = true;
                level.addFreshEntity(dagger);
            }
        }
        createFireField();
    }

    @Override
    public void tick() {
        if (!isSpawnDagger() && age++ < delay) {
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
                // prevent first-tick flicker due to deltaMoveOld being "uninitialized" on our first tick
                if (tickCount == 1) {
                    deltaMovementOld = getDeltaMovement();
                }
            }
            if (age == delay) {
                if (isGrounded) {
                    if (Utils.random.nextFloat() < 0.25f) {
                        playSound(SoundRegistry.FIERY_DAGGER_THROW.get(), 0.75f, Utils.random.nextIntBetweenInclusive(90, 110) * .01f);
                    }
                } else {
                    playSound(SoundRegistry.FIERY_DAGGER_THROW.get(), 2f, Utils.random.nextIntBetweenInclusive(90, 110) * .01f);
                }
                // do an initial near-collision check since we cannot organically hit things if we originate inside their hitbox (such as if they are standing on a dagger)
                var hits = level.getEntities(this, this.getBoundingBox().inflate(0.4f), this::canHitEntity);
                EntityHitResult hitResult = hits.isEmpty() ? null : new EntityHitResult(hits.getFirst());
                if (hitResult != null) {
                    onHit(hitResult);
                }
            }
            if (level.isClientSide) {
                level.addParticle(ParticleHelper.EMBERS, getX(), getY() + getBbHeight() * .5f, getZ(), 0, 0, 0);
            }
        } else {
            super.tick();
        }
    }

    @Override
    protected boolean canHitEntity(Entity pTarget) {
        return !isSpawnDagger() && super.canHitEntity(pTarget);
    }

    @Override
    public void impactParticles(double x, double y, double z) {
        MagicManager.spawnParticles(level, ParticleTypes.LAVA, x, y, z, 5, .1, .1, .1, .25, true);
    }

    @Override
    public void trailParticles() {
        float yHeading = -((float) (Mth.atan2(getDeltaMovement().z, getDeltaMovement().x) * (double) (180F / (float) Math.PI)) + 90.0F);
        float radius = .25f;
        int steps = 2;
        var vec = getDeltaMovement();
        double x2 = getX();
        double x1 = x2 - vec.x;
        double y2 = getY();
        double y1 = y2 - vec.y;
        double z2 = getZ();
        double z1 = z2 - vec.z;
        for (int j = 0; j < steps; j++) {
            float offset = (1f / steps) * j;
            double radians = ((tickCount + offset) / 7.5f) * 360 * Mth.DEG_TO_RAD;
            Vec3 swirl = new Vec3(Math.cos(radians) * radius, Math.sin(radians) * radius, 0).yRot(yHeading * Mth.DEG_TO_RAD);
            double x = Mth.lerp(offset, x1, x2) + swirl.x;
            double y = Mth.lerp(offset, y1, y2) + swirl.y + getBbHeight() / 2;
            double z = Mth.lerp(offset, z1, z2) + swirl.z;
            Vec3 jitter = Vec3.ZERO;//Utils.getRandomVec3(.05f);
            level.addParticle(ParticleHelper.EMBERS, x, y, z, jitter.x, jitter.y, jitter.z);
        }
    }

    @Override
    public float getSpeed() {
        return 1.25f;
    }

    @Override
    public Optional<Holder<SoundEvent>> getImpactSound() {
        return isGrounded ? Optional.empty() : Optional.of(SoundRegistry.FIRE_IMPACT);
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
    Additional Spawn Info
     */
    @Override
    public void writeSpawnData(RegistryFriendlyByteBuf buffer) {
        buffer.writeInt(this.delay);
        buffer.writeFloat(this.explosionRadius);
        buffer.writeBoolean(this.isGrounded);
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
        this.explosionRadius = buffer.readFloat();
        this.isGrounded = buffer.readBoolean();
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
