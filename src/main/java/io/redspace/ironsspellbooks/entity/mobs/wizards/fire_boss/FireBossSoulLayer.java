package io.redspace.ironsspellbooks.entity.mobs.wizards.fire_boss;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.redspace.ironsspellbooks.IronsSpellbooks;
import io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob.AbstractSpellCastingMob;
import io.redspace.ironsspellbooks.render.RenderHelper;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

import java.util.List;

@OnlyIn(Dist.CLIENT)
public class FireBossSoulLayer extends GeoRenderLayer<AbstractSpellCastingMob> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(IronsSpellbooks.MODID, "textures/entity/keeper/keeper_ghost.png");

    public FireBossSoulLayer(GeoEntityRenderer entityRendererIn) {
        super(entityRendererIn);
    }

    @Override
    public void render(PoseStack poseStack, AbstractSpellCastingMob animatable, BakedGeoModel bakedModel, RenderType renderType2, MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {

        int i = animatable.hurtTime + animatable.deathTime;
        if (i <= 0) {
            return;
        }
        float alpha = animatable.isDeadOrDying() ? i / 120f : i / (float) animatable.hurtDuration * 2;

        if (!(animatable instanceof FireBossEntity fireBossEntity) || !fireBossEntity.isSoulMode()) {
            return;
        }
        float f = (float) animatable.tickCount + partialTick;
        var renderType = RenderType.energySwirl(TEXTURE, f * 0.02F % 1.0F, f * 0.01F % 1.0F);

        VertexConsumer vertexconsumer = bufferSource.getBuffer(renderType);
        poseStack.pushPose();
        var bones = bakedModel.topLevelBones();
        setArmorVisible(bones, false);
        this.getRenderer().actuallyRender(poseStack, animatable, bakedModel, renderType, bufferSource, vertexconsumer, true, partialTick,
                LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, RenderHelper.colorf(.15f * alpha, .02f * alpha, 0.0f * alpha));
        setArmorVisible(bones, true);

        poseStack.popPose();
    }

    private void setArmorVisible(List<GeoBone> bones, boolean visible) {
        for (GeoBone bone : bones) {
            if (bone != null) {
                if (bone.getName().startsWith("armor")) {
                    bone.setHidden(!visible);
                }
                setArmorVisible(bone.getChildBones(), visible);
            }
        }
    }
}