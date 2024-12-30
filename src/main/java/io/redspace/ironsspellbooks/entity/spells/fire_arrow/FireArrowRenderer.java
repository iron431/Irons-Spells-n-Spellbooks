package io.redspace.ironsspellbooks.entity.spells.fire_arrow;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import io.redspace.ironsspellbooks.IronsSpellbooks;
import io.redspace.ironsspellbooks.render.RenderHelper;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.projectile.Projectile;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public class FireArrowRenderer extends EntityRenderer<Projectile> {
    private static final ResourceLocation TEXTURE = IronsSpellbooks.id("textures/entity/fire_arrow.png");

    public FireArrowRenderer(Context context) {
        super(context);
    }

    @Override
    public void render(Projectile entity, float yaw, float partialTicks, PoseStack poseStack, MultiBufferSource bufferSource, int light) {
        poseStack.pushPose();
//        Vec3 motion = entity.getDeltaMovement();
//        float xRot = -((float) (Mth.atan2(motion.horizontalDistance(), motion.y) * (double) (180F / (float) Math.PI)) - 90.0F);
//        float yRot = -((float) (Mth.atan2(motion.z, motion.x) * (double) (180F / (float) Math.PI)) + 90.0F);
        if(lastTick != entity.tickCount){
            tick = true;
            lastTick = entity.tickCount;
        }else{
            tick = false;
        }
        poseStack.mulPose(Axis.YP.rotationDegrees(Mth.lerp(partialTicks, entity.yRotO, entity.getYRot())));
        poseStack.mulPose(Axis.XP.rotationDegrees(Mth.lerp(partialTicks, entity.xRotO, entity.getXRot())));
        staticTick = this.tick;
        renderModel(poseStack, bufferSource);
        poseStack.popPose();

        super.render(entity, yaw, partialTicks, poseStack, bufferSource, light);
    }
    int lastTick;
    boolean tick;
    static boolean staticTick;
    public static void renderModel(PoseStack poseStack, MultiBufferSource bufferSource) {
        poseStack.scale(0.13f, 0.13f, 0.13f);

        //poseStack.mulPose(entityRenderDispatcher.cameraOrientation());
        //poseStack.mulPose(Vector3f.YP.rotationDegrees(180f));

        Pose pose = poseStack.last();
        Matrix4f poseMatrix = pose.pose();
        Matrix3f normalMatrix = pose.normal();

        VertexConsumer consumer = bufferSource.getBuffer(RenderHelper.CustomerRenderType.magic(getTextureLocation()));
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        poseStack.translate(-2, 0, 0);

        for (int j = 0; j < 4; ++j) {
            poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
            vertex(poseMatrix, normalMatrix, consumer, -8, -2, 0, 0.0F, 0.0F, 0, 1, 0, LightTexture.FULL_BRIGHT);
            vertex(poseMatrix, normalMatrix, consumer, 8, -2, 0, 0.5F, 0.0F, 0, 1, 0, LightTexture.FULL_BRIGHT);
            vertex(poseMatrix, normalMatrix, consumer, 8, 2, 0, 0.5F, 0.15625F, 0, 1, 0, LightTexture.FULL_BRIGHT);
            vertex(poseMatrix, normalMatrix, consumer, -8, 2, 0, 0.0F, 0.15625F, 0, 1, 0, LightTexture.FULL_BRIGHT);
        }
    }

    public static void vertex(Matrix4f pMatrix, Matrix3f pNormals, VertexConsumer pVertexBuilder, int pOffsetX, int pOffsetY, int pOffsetZ, float pTextureX, float pTextureY, int pNormalX, int p_113835_, int p_113836_, int pPackedLight) {
        pVertexBuilder.addVertex(pMatrix, (float) pOffsetX, (float) pOffsetY, (float) pOffsetZ).setColor(staticTick ? 0 :200, 200, 200, 255).setUv(pTextureX, pTextureY).setOverlay(OverlayTexture.NO_OVERLAY).setLight(pPackedLight).setNormal((float) pNormalX, (float) p_113836_, (float) p_113835_);
    }

    @Override
    public ResourceLocation getTextureLocation(Projectile entity) {
        return getTextureLocation();
    }

    public static ResourceLocation getTextureLocation() {
        return TEXTURE;
    }

}