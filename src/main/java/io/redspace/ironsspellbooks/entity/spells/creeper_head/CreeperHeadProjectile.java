package io.redspace.ironsspellbooks.entity.spells.creeper_head;

import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.entity.spells.AbstractMagicProjectile;
import io.redspace.ironsspellbooks.registries.EntityRegistry;
import io.redspace.ironsspellbooks.spells.evocation.ChainCreeperSpell;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;


public class CreeperHeadProjectile extends AbstractMagicProjectile {
    public CreeperHeadProjectile(EntityType<? extends CreeperHeadProjectile> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        chainOnKill = false;
    }

    protected boolean chainOnKill;
    protected float speed;

    public CreeperHeadProjectile(LivingEntity shooter, Level level, float speed, float damage) {
        super(EntityRegistry.CREEPER_HEAD_PROJECTILE.get(), level);
        setOwner(shooter);
        this.speed = speed;
        this.damage = damage;
        this.explosionRadius = 3.5f;
        this.shoot(shooter.getLookAngle());
    }

    public CreeperHeadProjectile(LivingEntity shooter, Level level, Vec3 speed, float damage) {
        super(EntityRegistry.CREEPER_HEAD_PROJECTILE.get(), level);
        setOwner(shooter);
        this.damage = damage;
        this.explosionRadius = 3.5f;
        this.speed = (float) speed.length();
        this.shoot(speed);
    }

    public void setChainOnKill(boolean chain) {
        chainOnKill = chain;
    }

    @Override
    public void trailParticles() {
        var vec3 = this.getBoundingBox().getCenter();
        level.addParticle(ParticleTypes.SMOKE, vec3.x, vec3.y, vec3.z, 0, 0, 0);
    }

    @Override
    public void impactParticles(double x, double y, double z) {

    }

    @Override
    public float getSpeed() {
        return speed;
    }

    @Override
    public Optional<Holder<SoundEvent>> getImpactSound() {
        return Optional.empty();
    }

    @Override
    protected void onHit(HitResult hitResult) {
        if (!this.level().isClientSide) {
            var entities = level().getEntities(this, this.getBoundingBox().inflate(explosionRadius));
            for (Entity entity : entities) {
                double distance = entity.position().distanceTo(hitResult.getLocation());
                if (distance < explosionRadius) {
                    //Prevent duplicate chains
                    if (entity instanceof LivingEntity livingEntity && livingEntity.isDeadOrDying() && !canHitEntity(entity))
                        break;
                    float damage = (float) (this.damage * (1 - Math.pow(distance / (explosionRadius), 2)));
                    DamageSources.applyDamage(entity, damage, SpellRegistry.LOB_CREEPER_SPELL.get().getDamageSource(this, getOwner()));
                    if (chainOnKill && entity instanceof LivingEntity livingEntity && livingEntity.isDeadOrDying()) {
                        ChainCreeperSpell.summonCreeperRing(this.level(), this.getOwner() instanceof LivingEntity livingOwner ? livingOwner : null, livingEntity.getEyePosition(), this.damage * .85f, 3);
                    }
                }
            }

            this.level().explode(this, this.getX(), this.getY(), this.getZ(), 0.0F, false, Level.ExplosionInteraction.NONE);
            this.discard();
        }
    }

    @Override
    public void recreateFromPacket(ClientboundAddEntityPacket pPacket) {
        super.recreateFromPacket(pPacket);
        this.xRotO = this.getXRot();
        this.yRotO = this.getYRot();
    }
}
