package io.redspace.ironsspellbooks.entity.mobs.wizards.fire_boss;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import io.redspace.ironsspellbooks.IronsSpellbooks;
import io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob.AbstractSpellCastingMob;
import net.minecraft.client.model.geom.PartNames;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;
import software.bernie.geckolib.util.RenderUtil;

@OnlyIn(Dist.CLIENT)
public class FireBossFlameLayer extends GeoRenderLayer<AbstractSpellCastingMob> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(IronsSpellbooks.MODID, "textures/entity/fire_boss/tyros_flame.png");

    public FireBossFlameLayer(GeoEntityRenderer entityRendererIn) {
        super(entityRendererIn);
    }

    @Override
    public void renderForBone(PoseStack poseStack, AbstractSpellCastingMob animatable, GeoBone bone, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
        if (bone.getName().equals(PartNames.HEAD) && animatable instanceof FireBossEntity fireBossEntity && fireBossEntity.isSoulMode()) {
            poseStack.pushPose();
            poseStack.mulPose(Axis.YP.rotationDegrees(45f));
            RenderUtil.translateToPivotPoint(poseStack, bone);
            poseStack.scale(1 / 2f, 1 / 2f, 1 / 2f);
            VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityCutout(TEXTURE));
            Matrix4f poseMatrix = poseStack.last().pose();

            int anim = (animatable.tickCount / ticksPerFrame) % frameCount;
            float uvMin = anim / (float) frameCount;
            float uvMax = (anim + 1) / (float) frameCount;
            float halfsqrt2 = 0.7071f;
            for (int i = 0; i < 4; i++) {
                poseStack.mulPose(Axis.YP.rotationDegrees(90f));
                consumer.addVertex(poseMatrix, 0, 0, -halfsqrt2).setColor(255, 255, 255, 255).setUv(0f, uvMax).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(0f, 1f, 0f);
                consumer.addVertex(poseMatrix, 0, 1, -halfsqrt2).setColor(255, 255, 255, 255).setUv(0f, uvMin).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(0f, 1f, 0f);
                consumer.addVertex(poseMatrix, 0, 1, halfsqrt2).setColor(255, 255, 255, 255).setUv(1f, uvMin).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(0f, 1f, 0f);
                consumer.addVertex(poseMatrix, 0, 0, halfsqrt2).setColor(255, 255, 255, 255).setUv(1f, uvMax).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(0f, 1f, 0f);
            }
            poseStack.popPose();
        }
    }

    static int frameCount = 8;
    static int ticksPerFrame = 1;
}