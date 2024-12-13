package io.redspace.ironsspellbooks.entity.spells.fire_arrow;

import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.config.ServerConfigs;
import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.entity.spells.AbstractMagicProjectile;
import io.redspace.ironsspellbooks.network.particles.FieryExplosionParticlesPacket;
import io.redspace.ironsspellbooks.registries.EntityRegistry;
import io.redspace.ironsspellbooks.util.ParticleHelper;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Optional;
import java.util.UUID;

public class FireArrowProjectile extends AbstractMagicProjectile {
    public FireArrowProjectile(EntityType<? extends Projectile> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        super.setNoGravity(true); // avoid lcoal gravity flag by using super
    }

    public FireArrowProjectile(Level pLevel, LivingEntity pShooter) {
        this(EntityRegistry.FIRE_ARROW_PROJECTILE.get(), pLevel);
        this.setOwner(pShooter);
    }

    boolean suspendGravity; // if true, we always have no gravity

    @Override
    public void setNoGravity(boolean pNoGravity) {
        // if some takes away our gravity, also take away our gravity mechanic
        suspendGravity = pNoGravity;
        super.setNoGravity(pNoGravity);
    }

    @Override
    public void trailParticles() {
        Vec3 vec3 = getDeltaMovement();
        double d0 = this.getX() - vec3.x;
        double d1 = this.getY() - vec3.y;
        double d2 = this.getZ() - vec3.z;
        var count = Mth.clamp((int) (vec3.lengthSqr() * 4), 1, 4);
        for (int i = 0; i < count; i++) {
            Vec3 random = Utils.getRandomVec3(1).add(vec3.normalize()).scale(0.25);
            var f = i / ((float) count);
            var x = Mth.lerp(f, d0, this.getX() + vec3.x);
            var y = Mth.lerp(f, d1, this.getY() + vec3.y) - .4;
            var z = Mth.lerp(f, d2, this.getZ() + vec3.z);
            this.level.addParticle(ParticleHelper.FIRE_EMITTER, true,x - random.x, y + 0.5D - random.y, z - random.z, random.x * .5f, random.y * .5f, random.z * .5f);
        }
    }

    @Override
    public void tick() {
        if (this.tickCount == 10 && !suspendGravity) {
            this.setNoGravity(false);
        }
        super.tick();
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
        return Optional.of(SoundEvents.GENERIC_EXPLODE);
    }

    @Override
    protected void onHit(HitResult hitResult) {
        if (!this.level.isClientSide) {
            float directDamage = this.damage;
            float explosionDamage = directDamage * .5f;
            UUID ignore = null;
            if (hitResult instanceof EntityHitResult entityHitResult) {
                var directHit = entityHitResult.getEntity();
                DamageSources.applyDamage(directHit, directDamage, SpellRegistry.FIRE_ARROW_SPELL.get().getDamageSource(this, getOwner()));
                ignore = directHit.getUUID();
            }

            float explosionRadius = getExplosionRadius();
            var explosionRadiusSqr = explosionRadius * explosionRadius;
            var entities = level.getEntities(this, this.getBoundingBox().inflate(explosionRadius));
            Vec3 losPoint = Utils.raycastForBlock(level, this.position(), this.position().add(0, 2, 0), ClipContext.Fluid.NONE).getLocation();
            for (Entity entity : entities) {
                double distanceSqr = entity.distanceToSqr(hitResult.getLocation());
                if (ignore != entity.getUUID() && distanceSqr < explosionRadiusSqr && canHitEntity(entity) && Utils.hasLineOfSight(level, losPoint, entity.getBoundingBox().getCenter(), true)) {
                    double p = (1 - distanceSqr / explosionRadiusSqr);
                    float damage = (float) (explosionDamage * p);
                    DamageSources.applyDamage(entity, damage, SpellRegistry.FIRE_ARROW_SPELL.get().getDamageSource(this, getOwner()));
                }
            }
            if (ServerConfigs.SPELL_GREIFING.get()) {
                Explosion explosion = new Explosion(
                        level,
                        null,
                        null,
                        null,
                        this.getX(), this.getY(), this.getZ(),
                        this.getExplosionRadius() / 2,
                        true,
                        Explosion.BlockInteraction.DESTROY,
                        ParticleTypes.EXPLOSION,
                        ParticleTypes.EXPLOSION_EMITTER,
                        SoundEvents.GENERIC_EXPLODE);
                if (!NeoForge.EVENT_BUS.post(new ExplosionEvent.Start(level, explosion)).isCanceled()) {
                    explosion.explode();
                    explosion.finalizeExplosion(false);
                }
            }
            PacketDistributor.sendToPlayersTrackingEntity(this, new FieryExplosionParticlesPacket(hitResult.getLocation().subtract(getDeltaMovement().scale(0.25)), getExplosionRadius() * .7f));
            playSound(SoundEvents.GENERIC_EXPLODE.value(), 4.0F, (1.0F + (this.level.random.nextFloat() - this.level.random.nextFloat()) * 0.2F) * 0.7F);
            this.discard();
        }
    }
}
