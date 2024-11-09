package io.redspace.ironsspellbooks.entity.armor;

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

import java.util.List;
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
                setBoneVisible(this.actualBone, true);
            }
        }
    }

    //    public GeoBone leggingTorsoLayerBone = null;
//    public GeoBone torsoExtensionLeftLegBone = null;
//    public GeoBone torsoExtensionRightLegBone = null;
    private final List<AsyncBone> asyncBones;

    @Override
    public ResourceLocation getTextureLocation(T animatable) {
        return super.getTextureLocation(animatable);
    }

    public GenericCustomArmorRenderer(GeoModel<T> model) {
        super(model);
        this.asyncBones = List.of(
                new AsyncBone("armorLeggingTorsoLayer", EquipmentSlot.LEGS, m -> m.body, Vec3.ZERO),
                new AsyncBone("armorTorsoExtensionRightLeg", EquipmentSlot.CHEST, m -> m.rightLeg, new Vec3(2, 12, 0)),
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
//        if (currentSlot == EquipmentSlot.LEGS) {
//            setBoneVisible(this.leggingTorsoLayerBone, true);
//        } else if (currentSlot == EquipmentSlot.CHEST) {
//            setBoneVisible(this.torsoExtensionLeftLegBone, true);
//            setBoneVisible(this.torsoExtensionRightLegBone, true);
//        }
    }

    @Override
    public void applyBoneVisibilityByPart(EquipmentSlot currentSlot, ModelPart currentPart, HumanoidModel<?> model) {
        super.applyBoneVisibilityByPart(currentSlot, currentPart, model);
        asyncBones.forEach(bone -> {
            if (bone.itemSlot == currentSlot && currentPart == bone.partToFollow.apply(model)) {
                setBoneVisible(bone.actualBone, true);
            }
        });
//        if (currentPart == model.body && currentSlot == EquipmentSlot.LEGS) {
//            setBoneVisible(this.leggingTorsoLayerBone, true);
//        } else if (currentPart == model.leftLeg && currentSlot == EquipmentSlot.BODY) {
//            setBoneVisible(this.torsoExtensionLeftLegBone, true);
//        } else if (currentPart == model.rightLeg && currentSlot == EquipmentSlot.BODY) {
//            setBoneVisible(this.torsoExtensionRightLegBone, true);
//        }
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
//        if (this.leggingTorsoLayerBone != null) {
//            //IronsSpellbooks.LOGGER.debug("GenericCustomArmorRenderer: positioning leggingBone");
//            ModelPart bodyPart = baseModel.body;
//            RenderUtil.matchModelPartRot(bodyPart, this.leggingTorsoLayerBone);
//            this.leggingTorsoLayerBone.updatePosition(bodyPart.x, -bodyPart.y, bodyPart.z);
//        }
//        if (this.torsoExtensionRightLegBone != null) {
//            ModelPart bodyPart = baseModel.rightLeg;
//            RenderUtil.matchModelPartRot(bodyPart, this.torsoExtensionRightLegBone);
//            this.torsoExtensionRightLegBone.updatePosition(bodyPart.x, -bodyPart.y, bodyPart.z);
//        }
//        if (this.torsoExtensionLeftLegBone != null) {
//            ModelPart bodyPart = baseModel.leftLeg;
//            RenderUtil.matchModelPartRot(bodyPart, this.torsoExtensionLeftLegBone);
//            this.torsoExtensionLeftLegBone.updatePosition(bodyPart.x, -bodyPart.y, bodyPart.z);
//        }
    }

    @Override
    public void setAllVisible(boolean pVisible) {
        super.setAllVisible(pVisible);
        asyncBones.forEach(bone -> setBoneVisible(bone.actualBone, pVisible));
//        setBoneVisible(this.leggingTorsoLayerBone, pVisible);
//        setBoneVisible(this.torsoExtensionLeftLegBone, pVisible);
//        setBoneVisible(this.torsoExtensionRightLegBone, pVisible);
    }
}