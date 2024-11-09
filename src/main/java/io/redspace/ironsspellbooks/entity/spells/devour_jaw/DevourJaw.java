package io.redspace.ironsspellbooks.entity.spells.devour_jaw;

import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.entity.spells.AoeEntity;
import io.redspace.ironsspellbooks.registries.EntityRegistry;
import io.redspace.ironsspellbooks.registries.MobEffectRegistry;
import io.redspace.ironsspellbooks.registries.SoundRegistry;
import io.redspace.ironsspellbooks.util.ParticleHelper;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

public class DevourJaw extends AoeEntity {
    public DevourJaw(EntityType<? extends Projectile> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    LivingEntity target;

    public DevourJaw(Level level, LivingEntity owner, LivingEntity target) {
        this(EntityRegistry.DEVOUR_JAW.get(), level);
        setOwner(owner);
        this.target = target;
    }

    //dont need to serialize, dont need it only client either
    public int vigorLevel;

    @Override
    public void applyEffect(LivingEntity target) {
        if (target == this.target) {
            if (DamageSources.applyDamage(target, getDamage(), SpellRegistry.DEVOUR_SPELL.get().getDamageSource(this, getOwner())) && getOwner() instanceof LivingEntity livingOwner) {
                target.setDeltaMovement(target.getDeltaMovement().add(0, .5f, 0));
                target.hurtMarked = true;
                if (target.isDeadOrDying()) {
                    var oldVigor = livingOwner.getEffect(MobEffectRegistry.VIGOR);
                    var addition = 0;
                    if (oldVigor != null)
                        addition = oldVigor.getAmplifier() + 1;
                    livingOwner.addEffect(new MobEffectInstance(MobEffectRegistry.VIGOR, 20 * 60, Math.min(vigorLevel + addition, 9), false, false, true));
                    livingOwner.heal((vigorLevel + 1) * 2);
                }
            }
        }
    }

    public final int waitTime = 5;
    public final int warmupTime = waitTime + 8;
    public final int deathTime = warmupTime + 8;

    @Override
    public void tick() {
        if (tickCount < waitTime) {
            if (this.target != null)
                setPos(this.target.position());
        } else if (tickCount == waitTime) {
            this.playSound(SoundRegistry.DEVOUR_BITE.get(), 2, 1);
        } else if (tickCount == warmupTime) {
            if (level.isClientSide) {
                float y = this.getYRot();
                int countPerSide = 50;
                //These particles were not at all what I intended. But they're cooler. no clue how it works
                for (int i = -countPerSide; i < countPerSide; i++) {
                    Vec3 motion = new Vec3(0, Math.abs(countPerSide) - i, countPerSide * .5f).yRot(y).normalize().multiply(.4f, .8f, .4f);
                    motion = motion.scale(Utils.random.nextIntBetweenInclusive(6, 12) * .1f);
                    level.addParticle(ParticleHelper.BLOOD, getX(), getY() + .5f, getZ(), motion.x, motion.y, motion.z);
                }
            } else {
                checkHits();
            }
        } else if (tickCount > deathTime)
            discard();
    }

    @Override
    protected Vec3 getInflation() {
        return new Vec3(2, 2, 2);
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
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
    public float getParticleCount() {
        return 0;
    }

    @Override
    public Optional<ParticleOptions> getParticle() {
        return Optional.empty();
    }

    @Override
    public void recreateFromPacket(ClientboundAddEntityPacket pPacket) {
        super.recreateFromPacket(pPacket);
        this.xRotO = this.getXRot();
        this.yRotO = this.getYRot();
    }
}
