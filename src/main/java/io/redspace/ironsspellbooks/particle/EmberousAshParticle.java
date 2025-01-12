package io.redspace.ironsspellbooks.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public class EmberousAshParticle extends TextureSheetParticle {
    public EmberousAshParticle(ClientLevel level, double xCoord, double yCoord, double zCoord, double xd, double yd, double zd) {
        super(level, xCoord, yCoord, zCoord, xd, yd, zd);
        this.scale(this.random.nextFloat() * .65f + .4f);
        this.lifetime = 40 + (int) (Math.random() * 45);
        this.gravity = 0;
        this.friction = 1;
        this.quadSize = .0625f;
        //1, .6f, 0.3
        this.rCol = 1 * (.9f + this.random.nextFloat() * 0.1f);
        this.gCol = .6f * (.9f + this.random.nextFloat() * 0.1f);
        this.bCol = .3f * (.9f + this.random.nextFloat() * 0.1f);
        this.xd = xd;
        this.yd = yd;
        this.zd = zd;
        this.seed = (this.random.nextFloat() - .5f) * 2 * 5;
        this.speed = (float) new Vec3(xd, yd, zd).length();
        if (speed > 4) {
            this.xd = 0.15;
            this.yd = 0;
            this.zd = 0;
        }
    }

    final float seed;
    final float speed;

    private float f(float x) {
        return Mth.sin(seed * Mth.sin(x) + x);
    }

    private float function(float x) {
        return 0.2f *
                0.25f * (f(2 * x) +
                1f * f(.25f * x) +
                2f * f(.125f * x));
    }

    @Override
    public float getQuadSize(float pScaleFactor) {
        return Mth.lerp((age + pScaleFactor) / (float) lifetime, super.getQuadSize(pScaleFactor), 0);
    }

    @Override
    public void tick() {
        super.tick();

        float f = Math.abs(seed) < .2 ? 1 : seed;
        this.xd = 0.3 * seed * (.05f * Mth.sin((this.age + 700 * seed) * 0.2f / f) + function(this.age * 0.2f + 700 * seed));
        this.yd = 0.3 * seed * (.05f * Mth.sin((this.age + 500 * seed) * 0.2f / f) + function(this.age * 0.2f + 500 * seed));
        this.zd = 0.3 * seed * (.05f * Mth.cos((this.age + 100 * seed) * 0.2f / f) + function(this.age * 0.2f + 100 * seed));
        if (speed > 4) {
            xd = Math.abs(xd) * 3;
        }
        if (random.nextFloat() < 0.5) {
            this.level.addParticle(ParticleTypes.SMOKE, this.x, this.y, this.z, this.xd, this.yd, this.zd);
        }
        if (new Vec3(x - xo, y - yo, z - zo).lengthSqr() < 0.001) {
            this.remove();
        }
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
    }

    @OnlyIn(Dist.CLIENT)
    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet spriteSet) {
            this.sprites = spriteSet;
        }

        public Particle createParticle(SimpleParticleType particleType, ClientLevel level,
                                       double x, double y, double z,
                                       double dx, double dy, double dz) {
            var p = new EmberousAshParticle(level, x, y, z, dx, dy, dz);
            p.pickSprite(this.sprites);
            return p;
        }
    }

    @Override
    public int getLightColor(float p_107564_) {
        return LightTexture.FULL_BRIGHT;
    }
}
