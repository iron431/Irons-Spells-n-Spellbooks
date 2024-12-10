package io.redspace.ironsspellbooks.entity.armor;

import io.redspace.ironsspellbooks.IronsSpellbooks;
import io.redspace.ironsspellbooks.registries.ComponentRegistry;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoArmorRenderer;
import software.bernie.geckolib.util.RenderUtil;

import java.util.ArrayList;
import java.util.function.Function;

public class GenericCustomArmorRenderer<T extends Item & GeoItem> extends GeoArmorRenderer<T> {
    public class AsyncBone {
        @Nullable
        private GeoBone actualBone;
        private final String boneName;
        private final EquipmentSlot itemSlot;
        private final Function<HumanoidModel<?>, ModelPart> partToFollow;
        private final Vec3 partOffset;

        // Primary constructor
        public AsyncBone(String boneName, EquipmentSlot itemSlot, Function<HumanoidModel<?>, ModelPart> partToFollow, Vec3 partOffset) {
            this.actualBone = null;
            this.boneName = boneName;
            this.itemSlot = itemSlot;
            this.partToFollow = partToFollow;
            this.partOffset = partOffset;
        }

        public void grabBone(GeoModel<?> model) {
            this.actualBone = model.getBone(boneName).orElse(null);
        }

        public void applyVisibility(EquipmentSlot currentSlot) {
            if (currentSlot == this.itemSlot) {
                setBoneVisible(this.actualBone, !boneName.startsWith("alt") || GenericCustomArmorRenderer.this.getCurrentStack().has(ComponentRegistry.CLOTHING_ALT));
            }
        }
    }

    protected final ArrayList<AsyncBone> asyncBones;

    @Override
    public ResourceLocation getTextureLocation(T animatable) {
        return super.getTextureLocation(animatable);
    }

    public GenericCustomArmorRenderer(GeoModel<T> model) {
        super(model);
        this.asyncBones = new ArrayList<>();
        asyncBones.add(
                new AsyncBone("armorLeggingTorsoLayer", EquipmentSlot.LEGS, m -> m.body, Vec3.ZERO)
        );
        asyncBones.add(
                new AsyncBone("armorTorsoExtensionRightLeg", EquipmentSlot.CHEST, m -> m.rightLeg, new Vec3(2, 12, 0))
        );
        asyncBones.add(
                new AsyncBone("armorTorsoExtensionLeftLeg", EquipmentSlot.CHEST, m -> m.leftLeg, new Vec3(-2, 12, 0))
        );
    }

    @Override
    protected void grabRelevantBones(BakedGeoModel bakedModel) {
        if (this.lastModel != bakedModel) {
            asyncBones.forEach(bone -> bone.grabBone(this.model));
        }
        super.grabRelevantBones(bakedModel);
    }


    @Override
    protected void applyBoneVisibilityBySlot(EquipmentSlot currentSlot) {
        super.applyBoneVisibilityBySlot(currentSlot);
        asyncBones.forEach(bone -> bone.applyVisibility(currentSlot));
    }

    @Override
    public void applyBoneVisibilityByPart(EquipmentSlot currentSlot, ModelPart currentPart, HumanoidModel<?> model) {
        super.applyBoneVisibilityByPart(currentSlot, currentPart, model);
        asyncBones.forEach(bone -> {
            if ((!bone.boneName.startsWith("alt") || this.currentStack.has(ComponentRegistry.CLOTHING_ALT)) && bone.itemSlot == currentSlot && currentPart == bone.partToFollow.apply(model)) {
                setBoneVisible(bone.actualBone, true);
            }
        });
    }

    @Override
    protected void applyBaseTransformations(HumanoidModel<?> baseModel) {
        super.applyBaseTransformations(baseModel);
        asyncBones.forEach(bone -> {
            if (bone.actualBone != null) {
                var bodyPart = bone.partToFollow.apply(baseModel);
                RenderUtil.matchModelPartRot(bodyPart, bone.actualBone);
                bone.actualBone.updatePosition((float) bone.partOffset.x + bodyPart.x, (float) bone.partOffset.y + -bodyPart.y, (float) bone.partOffset.z + bodyPart.z);
            }
        });
    }

    @Override
    public void setAllVisible(boolean pVisible) {
        super.setAllVisible(pVisible);
        asyncBones.forEach(bone -> setBoneVisible(bone.actualBone, pVisible));
    }
}