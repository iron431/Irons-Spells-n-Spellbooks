package io.redspace.ironsspellbooks.entity.spells.sunbeam;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import io.redspace.ironsspellbooks.entity.mobs.AntiMagicSusceptible;
import io.redspace.ironsspellbooks.api.entity.spells.AoeEntity;
import io.redspace.ironsspellbooks.registries.EntityRegistry;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;

import java.util.Optional;

public class Sunbeam extends AoeEntity implements AntiMagicSusceptible {

    public Sunbeam(EntityType<? extends Projectile> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        this.setRadius((float) (this.getBoundingBox().getXsize() * .5f));
        this.setNoGravity(true);
    }

    public Sunbeam(Level level) {
        this(EntityRegistry.SUNBEAM.get(), level);
    }


    @Override
    public void tick() {

        if (tickCount == 4) {
            checkHits();
            if (!level.isClientSide)
                MagicManager.spawnParticles(level, ParticleTypes.FIREWORK, getX(), getY(), getZ(), 9, getRadius() * .7f, .2f, getRadius() * .7f, 1, true);
        }

        if (this.tickCount > 6) {
            discard();
        }
    }


    @Override
    public void applyEffect(LivingEntity target) {
//        DamageSources.applyDamage(target, getDamage(), SpellRegistry.SUNBEAM_SPELL.get().getDamageSource(this, getOwner()));
    }

    @Override
    public float getParticleCount() {
        return 0f;
    }

    @Override
    public void refreshDimensions() {
        return;
    }

    @Override
    public void ambientParticles() {
        return;
    }

    @Override
    public Optional<ParticleOptions> getParticle() {
        return Optional.empty();
    }

    @Override
    public void onAntiMagic(MagicData magicData) {
        discard();
    }
}
