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
    private final float xrot;
    private final float yrot;

    FlameStrikeParticle(ClientLevel pLevel, double pX, double pY, double pZ, SpriteSet spriteSet, double xd, double yd, double zd, FlameStrikeParticleOptions options) {
        super(pLevel, pX, pY, pZ, 0, 0, 0);

        this.xd = xd;
        this.yd = yd;
        this.zd = zd;

        this.lifetime = 8;
        this.gravity = 0;
        sprites = spriteSet;

        this.xrot = options.xrot * Mth.DEG_TO_RAD;
        this.yrot = options.yrot * Mth.DEG_TO_RAD;
        this.quadSize = options.scale * 3.25f;

        this.friction = 1;
    }

    @Override
    public void tick() {
        this.setSpriteFromAge(sprites);
        if (this.age++ >= this.lifetime) {
            this.remove();
        }
    }

    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialticks) {
        boolean mirrored = false;
//        this.renderRotatedParticle(buffer, camera, partialticks, !mirrored, (p_234005_) -> {
//            p_234005_.mul(Axis.XP.rotation(-DEGREES_90));
//            p_234005_.mul(Axis.YP.rotation(yrot));
//            p_234005_.mul(Axis.XP.rotation(xrot));
//
//        });
//        this.renderRotatedParticle(buffer, camera, partialticks, mirrored, (p_234000_) -> {
//            p_234000_.mul(Axis.XP.rotation(DEGREES_90));
//            p_234000_.mul(Axis.YP.rotation(yrot));
//            p_234000_.mul(Axis.XP.rotation(-xrot));
//
//        });
        Quaternionf quaternionf = new Quaternionf();
        quaternionf.rotationZYX(yrot, 0, -xrot);
        this.renderRotatedQuad(buffer, camera, quaternionf, partialticks);
        quaternionf.rotationZYX(yrot, -Mth.PI, xrot);
        this.renderRotatedQuad(buffer, camera, quaternionf, partialticks);
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
        pConsumer.addVertex(pVec3f.x(), pVec3f.y() + .08f, pVec3f.z()).setUv(p_233996_, p_233997_).setColor(this.rCol, this.gCol, this.bCol, this.alpha).setLight(p_233998_);
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
