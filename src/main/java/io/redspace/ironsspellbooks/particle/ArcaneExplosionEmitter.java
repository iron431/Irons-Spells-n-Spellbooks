package io.redspace.ironsspellbooks.particle;

import io.redspace.ironsspellbooks.api.util.Utils;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.NoRenderParticle;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ArcaneExplosionEmitter extends NoRenderParticle {
    protected ArcaneExplosionEmitter(ClientLevel pLevel, double pX, double pY, double pZ) {
        super(pLevel, pX, pY, pZ, 0.0, 0.0, 0.0);
        this.lifetime = 3;
    }

    @Override
    public void tick() {
        if (age == 0) {
            this.level.addParticle(new BlastwaveParticleOptions(180f/255f, 220f/255f, 1f, 5), this.x, this.y, this.z, 0, 0.0, 0.0);
        }
        for (int i = 0; i < 100; i++) {
            Vec3 motion = Utils.getRandomVec3(5);
            this.level.addParticle(ParticleTypes.ENCHANTED_HIT, this.x, this.y, this.z, motion.x, motion.y, motion.z);
        }


        this.age++;
        if (this.age == this.lifetime) {
            this.remove();
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class Provider implements ParticleProvider<SimpleParticleType> {
        public Particle createParticle(
                SimpleParticleType pType,
                ClientLevel pLevel,
                double pX,
                double pY,
                double pZ,
                double pXSpeed,
                double pYSpeed,
                double pZSpeed
        ) {
            return new ArcaneExplosionEmitter(pLevel, pX, pY, pZ);
        }
    }
}
