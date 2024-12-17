package io.redspace.ironsspellbooks.entity.spells.magma_ball;

import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.damage.ISSDamageTypes;
import io.redspace.ironsspellbooks.entity.spells.AoeEntity;
import io.redspace.ironsspellbooks.registries.EntityRegistry;
import io.redspace.ironsspellbooks.util.ParticleHelper;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;

import java.util.Optional;

public class FireField extends AoeEntity {

    private DamageSource damageSource;

    public FireField(EntityType<? extends Projectile> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    public FireField(Level level) {
        this(EntityRegistry.FIRE_FIELD.get(), level);
    }

    @Override
    public void applyEffect(LivingEntity target) {
        if (damageSource == null) {
            damageSource = new DamageSource(DamageSources.getHolderFromResource(target, ISSDamageTypes.FIRE_FIELD), this, getOwner());
        }
        DamageSources.ignoreNextKnockback(target);
        target.hurt(damageSource, getDamage());
        target.setRemainingFireTicks(60);
    }

    @Override
    public float getParticleCount() {
        return 1.5f * getRadius();
    }

    @Override
    protected float particleYOffset() {
        return .25f;
    }

    @Override
    protected float getParticleSpeedModifier() {
        return 1.4f;
    }

    @Override
    public Optional<ParticleOptions> getParticle() {
        return Optional.of(ParticleHelper.FIRE);
    }
}
