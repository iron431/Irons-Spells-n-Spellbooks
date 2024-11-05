package io.redspace.ironsspellbooks.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.redspace.ironsspellbooks.item.ILecternPlaceable;
import io.redspace.ironsspellbooks.item.SpellBook;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.BookModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.LecternRenderer;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LecternRenderer.class)
public class LecternRendererMixin {
    @Shadow
    BookModel bookModel;

    @Inject(
            method = "render(Lnet/minecraft/world/level/block/entity/LecternBlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V",
            remap = false,
            at = @At(value = "HEAD"),
            cancellable = true)
    private void render(LecternBlockEntity pBlockEntity, float pPartialTick, PoseStack pPoseStack, MultiBufferSource pBufferSource, int pPackedLight, int pPackedOverlay, CallbackInfo ci) {
        BlockState blockstate = pBlockEntity.getBlockState();
        if (blockstate.getValue(LecternBlock.HAS_BOOK)) {
            var stack = pBlockEntity.getBook();
            if (stack.getItem() instanceof ILecternPlaceable lecternPlaceable) {
                pPoseStack.pushPose();
                pPoseStack.translate(0.5F, 1.0625F, 0.5F);
                float f = blockstate.getValue(LecternBlock.FACING).getClockWise().toYRot();
                pPoseStack.mulPose(Axis.YP.rotationDegrees(-f));
                pPoseStack.mulPose(Axis.ZP.rotationDegrees(67.5F));
                var textureOverride = lecternPlaceable.simpleTextureOverride(stack);
                if (textureOverride.isPresent()) {
                    pPoseStack.translate(0.0F, -0.125F, 0.0F);
                    this.bookModel.setupAnim(0.0F, 0.1F, 0.9F, 1.2F);
                    var vertexconsumer = pBufferSource.getBuffer(RenderType.entitySolid(textureOverride.get()));
                    this.bookModel.render(pPoseStack, vertexconsumer, pPackedLight, pPackedOverlay, -1);
                } else if (stack.getItem() instanceof SpellBook spellBook) {
                    pPoseStack.mulPose(Axis.XP.rotationDegrees(-90f));
                    pPoseStack.mulPose(Axis.ZP.rotationDegrees(90f));
                    pPoseStack.mulPose(Axis.YP.rotationDegrees(180f));
                    pPoseStack.translate(0.125F, -0.625F, 0.125F);
                    var itemRenderer = Minecraft.getInstance().getItemRenderer();
                    itemRenderer.renderStatic(stack, ItemDisplayContext.HEAD, pPackedLight, pPackedOverlay, pPoseStack, pBufferSource, null, 0);
                }
                pPoseStack.popPose();
                ci.cancel();
            }
        }
    }
}
