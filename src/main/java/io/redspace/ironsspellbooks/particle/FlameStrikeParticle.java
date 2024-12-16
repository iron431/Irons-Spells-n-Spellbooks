package io.redspace.ironsspellbooks.particle;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.Util;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.function.Consumer;

public class FlameStrikeParticle extends TextureSheetParticle {
    private static final Vector3f ROTATION_VECTOR = Util.make(new Vector3f(0.5F, 0.5F, 0.5F), Vector3f::normalize);
    private static final Vector3f TRANSFORM_VECTOR = new Vector3f(-1.0F, -1.0F, 0.0F);
    private static final float DEGREES_90 = Mth.PI / 2f;
    private final SpriteSet sprites;
    private final Vec3 normal;

    FlameStrikeParticle(ClientLevel pLevel, double pX, double pY, double pZ, SpriteSet spriteSet, double xd, double yd, double zd, FlameStrikeParticleOptions options) {
        super(pLevel, pX, pY, pZ, 0, 0, 0);

        this.xd = xd;
        this.yd = yd;
        this.zd = zd;

        this.lifetime = 8;
        this.gravity = 0;
        sprites = spriteSet;

        this.quadSize = options.scale * 3.25f;
        this.normal = new Vec3(options.xn, options.yn, options.zn).normalize();

        this.friction = 1;
    }

    @Override
    public void tick() {
        this.setSpriteFromAge(sprites);
        if (this.age++ >= this.lifetime) {
            this.remove();
        }
    }

    /**
     * projection_u(v)
     */
    public Vec3 proj(Vec3 u, Vec3 v) {
        return u.scale(v.dot(u) / u.lengthSqr());
    }

    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTick) {
        boolean mirrored = false;
        Vec3 forward = this.normal;
        Vec3 up = new Vec3(0, 1, 0);
        if (forward.dot(up) > .999) {
            up = new Vec3(1, 0, 0);
        }
        Vec3 right = forward.cross(up);

        // apply gram schmidt orthonormalization
        up = up.subtract(proj(forward, up)).normalize();
        right = right.subtract(proj(forward, right)).subtract(proj(up, right)).normalize();


        Vec3 vec3 = camera.getPosition();
        float f = (float) (Mth.lerp(partialTick, this.xo, this.x) - vec3.x());
        float f1 = (float) (Mth.lerp(partialTick, this.yo, this.y) - vec3.y());
        float f2 = (float) (Mth.lerp(partialTick, this.zo, this.z) - vec3.z());
        Vector3f[] vertices = new Vector3f[]{new Vector3f(-1.0F, -1.0F, 0.0F), new Vector3f(-1.0F, 1.0F, 0.0F), new Vector3f(1.0F, 1.0F, 0.0F), new Vector3f(1.0F, -1.0F, 0.0F)};
        for (int i = 0; i < 4; i++) {
            float x = (float) (forward.x * vertices[i].x +right.x * vertices[i].y);
            float y = (float) (forward.y * vertices[i].x +right.y * vertices[i].y);
            float z = (float) (forward.z * vertices[i].x +right.z * vertices[i].y);
            vertices[i] = new Vector3f(x, y, z);
            vertices[i].mul(this.getQuadSize(partialTick));
            vertices[i].add(f, f1, f2);
        }
        int j = this.getLightColor(partialTick);
        this.makeCornerVertex(buffer, vertices[0], this.getU1(), false/*mirror*/ ? this.getV0() : this.getV1(), j);
        this.makeCornerVertex(buffer, vertices[1], this.getU1(), false/*mirror*/ ? this.getV1() : this.getV0(), j);
        this.makeCornerVertex(buffer, vertices[2], this.getU0(), false/*mirror*/ ? this.getV1() : this.getV0(), j);
        this.makeCornerVertex(buffer, vertices[3], this.getU0(), false/*mirror*/ ? this.getV0() : this.getV1(), j);
//Vec3 orthoXY = new Vec3(-forward.z, forward.y, forward.x);
//        if (mirrored) {
//            orthoXY = orthoXY.multiply(-1, 1, -1);
//        }
//
//        Vec3 vec3 = camera.getPosition();
//        float f = (float) (Mth.lerp(partialTick, this.xo, this.x) - vec3.x());
//        float f1 = (float) (Mth.lerp(partialTick, this.yo, this.y) - vec3.y());
//        float f2 = (float) (Mth.lerp(partialTick, this.zo, this.z) - vec3.z());
//        Vector3f[] vertices = new Vector3f[]{new Vector3f(-1.0F, -1.0F, 0.0F), new Vector3f(-1.0F, 1.0F, 0.0F), new Vector3f(1.0F, 1.0F, 0.0F), new Vector3f(1.0F, -1.0F, 0.0F)};
//        for (int i = 0; i < 4; i++) {
//            float x = (float) (orthoXY.x * vertices[i].x + forward.x * vertices[i].y);
//            float y = (float) (orthoXY.y * vertices[i].x + forward.y * vertices[i].y);
//            float z = (float) (orthoXY.z * vertices[i].x + forward.z * vertices[i].y);
//            vertices[i] = new Vector3f(x, y, z);
//            vertices[i].add(f, f1, f2);
//        }
//        int j = this.getLightColor(partialTick);
//        this.makeCornerVertex(buffer, vertices[0], this.getU1(), false/*mirror*/ ? this.getV0() : this.getV1(), j);
//        this.makeCornerVertex(buffer, vertices[1], this.getU1(), false/*mirror*/ ? this.getV1() : this.getV0(), j);
//        this.makeCornerVertex(buffer, vertices[2], this.getU0(), false/*mirror*/ ? this.getV1() : this.getV0(), j);
//        this.makeCornerVertex(buffer, vertices[3], this.getU0(), false/*mirror*/ ? this.getV0() : this.getV1(), j);
//        Quaternionf quaternionf = new Quaternionf();
//        quaternionf.rotationZYX(-Mth.PI, 0, 0);
//        for (int i = 0; i < 4; i++) {
//            float x = (float) (orthoXY.x * vertices[i].x + forward.x * vertices[i].y);
//            float y = (float) (orthoXY.y * vertices[i].x + forward.y * vertices[i].y);
//            float z = (float) (orthoXY.z * vertices[i].x + forward.z * vertices[i].y);
//            vertices[i] = new Vector3f(x, y, z);
//            vertices[i].rotate(quaternionf);
//            vertices[i].add(f, f1, f2);
//        }
//        this.makeCornerVertex(buffer, vertices[0], this.getU1(), false/*mirror*/ ? this.getV0() : this.getV1(), j);
//        this.makeCornerVertex(buffer, vertices[1], this.getU1(), false/*mirror*/ ? this.getV1() : this.getV0(), j);
//        this.makeCornerVertex(buffer, vertices[2], this.getU0(), false/*mirror*/ ? this.getV1() : this.getV0(), j);
//        this.makeCornerVertex(buffer, vertices[3], this.getU0(), false/*mirror*/ ? this.getV0() : this.getV1(), j);
    }

    private void renderRotatedParticle(VertexConsumer pConsumer, Camera camera, float partialTick, boolean mirror, Consumer<Quaternionf> pQuaternion) {
        Vec3 vec3 = camera.getPosition();
        float f = (float) (Mth.lerp(partialTick, this.xo, this.x) - vec3.x());
        float f1 = (float) (Mth.lerp(partialTick, this.yo, this.y) - vec3.y());
        float f2 = (float) (Mth.lerp(partialTick, this.zo, this.z) - vec3.z());
        Quaternionf quaternion = (new Quaternionf()).setAngleAxis(0.0F, ROTATION_VECTOR.x(), ROTATION_VECTOR.y(), ROTATION_VECTOR.z());

        pQuaternion.accept(quaternion);
        quaternion.transform(TRANSFORM_VECTOR);
        Vector3f[] avector3f = new Vector3f[]{new Vector3f(-1.0F, -1.0F, 0.0F), new Vector3f(-1.0F, 1.0F, 0.0F), new Vector3f(1.0F, 1.0F, 0.0F), new Vector3f(1.0F, -1.0F, 0.0F)};
        float f3 = this.getQuadSize(partialTick);

        for (int i = 0; i < 4; ++i) {
            Vector3f vector3f = avector3f[i];
            vector3f.rotate(quaternion);
            vector3f.mul(f3);
            vector3f.add(f, f1, f2);
        }

        int j = this.getLightColor(partialTick);
        this.makeCornerVertex(pConsumer, avector3f[0], this.getU1(), mirror ? this.getV0() : this.getV1(), j);
        this.makeCornerVertex(pConsumer, avector3f[1], this.getU1(), mirror ? this.getV1() : this.getV0(), j);
        this.makeCornerVertex(pConsumer, avector3f[2], this.getU0(), mirror ? this.getV1() : this.getV0(), j);
        this.makeCornerVertex(pConsumer, avector3f[3], this.getU0(), mirror ? this.getV0() : this.getV1(), j);
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
