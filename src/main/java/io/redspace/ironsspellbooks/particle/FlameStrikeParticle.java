package io.redspace.ironsspellbooks.particle;

import com.mojang.blaze3d.vertex.VertexConsumer;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.util.ParticleHelper;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

public class FlameStrikeParticle extends TextureSheetParticle {
    private final SpriteSet sprites;
    private final Vec3 forward;
    private final boolean mirror, vertical;
    private final Vector3f[] localVertices;

    FlameStrikeParticle(ClientLevel pLevel, double pX, double pY, double pZ, SpriteSet spriteSet, double xd, double yd, double zd, FlameStrikeParticleOptions options) {
        super(pLevel, pX, pY, pZ, 0, 0, 0);

        this.xd = xd;
        this.yd = yd;
        this.zd = zd;

        this.lifetime = 4;
        this.gravity = 0;
        sprites = spriteSet;

        this.quadSize = options.scale * 3.25f;
        this.forward = new Vec3(options.xf, options.yf, options.zf).normalize();
        this.mirror = options.mirror;
        this.vertical = options.vertical;
        this.localVertices = calculateVertices();

        this.friction = 1;
    }

    private Vec3 vec3Copy(Vector3f vector3f) {
        return new Vec3(vector3f.x, vector3f.y, vector3f.z);
    }

    @Override
    public void tick() {
        if (this.age == 0) {
            createEmberTrail();
        }
        if (this.age++ > this.lifetime) {
            this.remove();
        } else {
            this.setSpriteFromAge(sprites);
        }
    }

    private void createEmberTrail() {
        int particleCount = (int) (9 * this.quadSize);
        for (int i = 1; i < particleCount - 1; i++) {
            float t = i / (float) particleCount;
            float u = 1 - t;
            //bezier curve interpolation
            Vec3 localPos =
                    vec3Copy(localVertices[1]).scale(u * u * u).add(
                            vec3Copy(localVertices[2]).scale(3 * u * u * t).add(
                                    vec3Copy(localVertices[3]).scale(3 * u * t * t).add(
                                            vec3Copy(localVertices[0]).scale(t * t * t)
                                    )
                            )
                    ).scale(this.quadSize * .75f).add(Utils.getRandomVec3(0.3));
            level.addParticle(ParticleHelper.EMBERS, x + localPos.x, y + localPos.y, z + localPos.z, 0, 0, 0);
        }
    }

    private Vector3f[] calculateVertices() {
        boolean vertical = this.vertical;
        Vec3 forward = this.forward;
        Vec3 up = new Vec3(0, 1, 0);
        if (forward.dot(up) > .999) {
            up = new Vec3(1, 0, 0);
        }
        Vec3 right = forward.cross(up);

        // apply gram schmidt orthonormalization
        up = up.subtract(proj(forward, up)).normalize();
        right = right.subtract(proj(forward, right)).subtract(proj(up, right)).normalize();
        Vec3 primary, secondary;
        if (!vertical) {
            primary = forward;
            secondary = right;
        } else {
            primary = forward;
            secondary = up;
        }
        Vector3f[] vertices = new Vector3f[]{new Vector3f(-1.0F, -1.0F, 0.0F), new Vector3f(-1.0F, 1.0F, 0.0F), new Vector3f(1.0F, 1.0F, 0.0F), new Vector3f(1.0F, -1.0F, 0.0F)};
        for (int i = 0; i < 4; i++) {
            float x = (float) (primary.x * vertices[i].x + secondary.x * vertices[i].y);
            float y = (float) (primary.y * vertices[i].x + secondary.y * vertices[i].y);
            float z = (float) (primary.z * vertices[i].x + secondary.z * vertices[i].y);
            vertices[i] = new Vector3f(x, y, z);
        }
        return vertices;
    }

    /**
     * projection_u(v)
     */
    public Vec3 proj(Vec3 u, Vec3 v) {
        return u.scale(v.dot(u) / u.lengthSqr());
    }

    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTick) {
        boolean mirrored = !this.mirror; // based on animation, we actually want the default to be mirrored
        Vec3 vec3 = camera.getPosition();
        float f = (float) (Mth.lerp(partialTick, this.xo, this.x) - vec3.x());
        float f1 = (float) (Mth.lerp(partialTick, this.yo, this.y) - vec3.y());
        float f2 = (float) (Mth.lerp(partialTick, this.zo, this.z) - vec3.z());
        Vector3f[] vertices = new Vector3f[4];
        for (int i = 0; i < 4; i++) {
            var localVertex = localVertices[i];
            vertices[i] = new Vector3f(localVertex.x, localVertex.y, localVertex.z);
            vertices[i].mul(this.getQuadSize(partialTick));
            vertices[i].add(f, f1, f2);
        }
        int j = this.getLightColor(partialTick);
        this.makeCornerVertex(buffer, vertices[0], this.getU1(), mirrored ? this.getV0() : this.getV1(), j);
        this.makeCornerVertex(buffer, vertices[1], this.getU1(), mirrored ? this.getV1() : this.getV0(), j);
        this.makeCornerVertex(buffer, vertices[2], this.getU0(), mirrored ? this.getV1() : this.getV0(), j);
        this.makeCornerVertex(buffer, vertices[3], this.getU0(), mirrored ? this.getV0() : this.getV1(), j);
        //backface
        this.makeCornerVertex(buffer, vertices[3], this.getU0(), mirrored ? this.getV0() : this.getV1(), j);
        this.makeCornerVertex(buffer, vertices[2], this.getU0(), mirrored ? this.getV1() : this.getV0(), j);
        this.makeCornerVertex(buffer, vertices[1], this.getU1(), mirrored ? this.getV1() : this.getV0(), j);
        this.makeCornerVertex(buffer, vertices[0], this.getU1(), mirrored ? this.getV0() : this.getV1(), j);

    }

    private void makeCornerVertex(VertexConsumer pConsumer, Vector3f pVec3f, float p_233996_, float p_233997_, int p_233998_) {
        pConsumer.addVertex(pVec3f.x(), pVec3f.y(), pVec3f.z()).setUv(p_233996_, p_233997_).setColor(this.rCol, this.gCol, this.bCol, this.alpha).setLight(p_233998_);
    }

    @NotNull
    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
    }

    @Override
    protected int getLightColor(float pPartialTick) {
        return LightTexture.FULL_BRIGHT;
    }

    @OnlyIn(Dist.CLIENT)
    public static class Provider implements ParticleProvider<FlameStrikeParticleOptions> {
        private final SpriteSet sprite;

        public Provider(SpriteSet pSprite) {
            this.sprite = pSprite;
        }

        public Particle createParticle(@NotNull FlameStrikeParticleOptions options, @NotNull ClientLevel pLevel, double pX, double pY, double pZ, double pXSpeed, double pYSpeed, double pZSpeed) {
            FlameStrikeParticle shriekparticle = new FlameStrikeParticle(pLevel, pX, pY, pZ, sprite, pXSpeed, pYSpeed, pZSpeed, options);
            shriekparticle.setSpriteFromAge(this.sprite);
            shriekparticle.setAlpha(1.0F);
            return shriekparticle;
        }
    }

}
