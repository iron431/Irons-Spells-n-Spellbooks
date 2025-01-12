package io.redspace.ironsspellbooks.entity.mobs.wizards.cursed_armor_stand;

import io.redspace.ironsspellbooks.IronsSpellbooks;
import io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob.AbstractSpellCastingMob;
import io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob.AbstractSpellCastingMobModel;
import io.redspace.ironsspellbooks.util.DefaultBipedBoneIdents;
import net.minecraft.client.model.geom.PartNames;
import net.minecraft.client.renderer.entity.ArmorStandRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;

import static io.redspace.ironsspellbooks.entity.mobs.wizards.cursed_armor_stand.CursedArmorStandEntity.JIGGLE_TIME;

public class CursedArmorStandModel extends AbstractSpellCastingMobModel {
    public static final ResourceLocation TEXTURE = new ResourceLocation(IronsSpellbooks.MODID, "textures/entity/cultist.png");
    public static final ResourceLocation MODEL = new ResourceLocation(IronsSpellbooks.MODID, "geo/armor_stand.geo.json");

    @Override
    public ResourceLocation getTextureResource(AbstractSpellCastingMob object) {
        return ArmorStandRenderer.DEFAULT_SKIN_LOCATION;
    }

    public static double[] rightArmPos = new double[]{110, -25, 180};

    @Override
    public void setCustomAnimations(AbstractSpellCastingMob entity, long instanceId, AnimationState<AbstractSpellCastingMob> animationState) {
        if (entity instanceof CursedArmorStandEntity cursedArmorStandEntity && cursedArmorStandEntity.isArmorStandFrozen()) {
            var pose = cursedArmorStandEntity.getArmorstandPose();
            GeoBone head = this.getAnimationProcessor().getBone(PartNames.HEAD);
            GeoBone body = this.getAnimationProcessor().getBone(PartNames.BODY);
            GeoBone torso = this.getAnimationProcessor().getBone("torso");
            GeoBone rightArm = this.getAnimationProcessor().getBone(PartNames.RIGHT_ARM);
            GeoBone rightHand = this.getAnimationProcessor().getBone(DefaultBipedBoneIdents.RIGHT_HAND_BONE_IDENT);
            GeoBone leftArm = this.getAnimationProcessor().getBone(PartNames.LEFT_ARM);
            GeoBone rightLeg = this.getAnimationProcessor().getBone(PartNames.RIGHT_LEG);
            GeoBone leftLeg = this.getAnimationProcessor().getBone(PartNames.LEFT_LEG);
            switch (pose) {
                case DEFAULT -> {
                    transformStack.pushRotation(leftArm, 10 * Mth.DEG_TO_RAD, 0, -10 * Mth.DEG_TO_RAD);
                    transformStack.pushRotation(rightArm, 15 * Mth.DEG_TO_RAD, 0, 10 * Mth.DEG_TO_RAD);
                    transformStack.pushRotation(leftLeg, -1 * Mth.DEG_TO_RAD, 0, -1 * Mth.DEG_TO_RAD);
                    transformStack.pushRotation(rightLeg, 1 * Mth.DEG_TO_RAD, 0, 1 * Mth.DEG_TO_RAD);
                }
                case KNEELING -> {
                    transformStack.pushPosition(rightLeg, 0, 2, -3);
                    transformStack.pushRotation(rightLeg, -52.5f * Mth.DEG_TO_RAD, 0, 0);
                    transformStack.pushPosition(leftLeg, 1 * Mth.DEG_TO_RAD, 6, -4);
                    transformStack.pushRotation(rightArm, 90 * Mth.DEG_TO_RAD, 45 * Mth.DEG_TO_RAD, 90 * Mth.DEG_TO_RAD);
                    transformStack.pushRotation(leftArm, 45 * Mth.DEG_TO_RAD, 0, 0);
                    transformStack.pushRotation(head, -45 * Mth.DEG_TO_RAD, 0, 0);
                    transformStack.pushRotation(torso, -10 * Mth.DEG_TO_RAD, 0, 0);
                    transformStack.pushPosition(body, 0, -6, 0);
                }
                case HEROIC -> {
                    transformStack.pushRotation(rightArm, 132.5f * Mth.DEG_TO_RAD, 0, 0);
                    transformStack.pushRotation(leftArm, -20f * Mth.DEG_TO_RAD, 0, 0);
                    transformStack.pushRotation(leftLeg, -12f * Mth.DEG_TO_RAD, 0, 0);
                    transformStack.pushPosition(rightLeg, 0, 0, -2);
                    transformStack.pushRotation(head, 25 * Mth.DEG_TO_RAD, 0, 0);
                }
                case STOIC -> {
                    transformStack.pushRotationDegrees(rightArm, 80, 35, 0);
                    transformStack.pushRotationDegrees(leftArm, 80, -35, 0);
                    transformStack.pushRotationDegrees(rightHand, 0, 90, 0);
                    transformStack.pushRotationDegrees(rightLeg, 0, -1, 0);
                    transformStack.pushRotationDegrees(leftLeg, 0, 1, 0);
                }
            }


            float partialTick = animationState.getPartialTick();
            if (cursedArmorStandEntity.helmetJiggle > 0) {
                float f = elastic(1f - ((cursedArmorStandEntity.helmetJiggle - partialTick) / JIGGLE_TIME));
                transformStack.pushRotation(this.getAnimationProcessor().getBone(DefaultBipedBoneIdents.HEAD_ARMOR_BONE_IDENT), 0, f, 0);
            }
            if (cursedArmorStandEntity.chestJiggle > 0) {
                float f = elastic(1 - (cursedArmorStandEntity.chestJiggle - partialTick) / JIGGLE_TIME);
                transformStack.pushRotation(this.getAnimationProcessor().getBone(DefaultBipedBoneIdents.BODY_ARMOR_BONE_IDENT), 0, f, 0);
                transformStack.pushRotation(this.getAnimationProcessor().getBone(DefaultBipedBoneIdents.RIGHT_ARM_ARMOR_BONE_IDENT), 0, f * .75f, 0);
                transformStack.pushRotation(this.getAnimationProcessor().getBone(DefaultBipedBoneIdents.LEFT_ARM_ARMOR_BONE_IDENT), 0, -f * .75f, 0);
            }
            if (cursedArmorStandEntity.legJiggle > 0) {
                float f = elastic(1 - (cursedArmorStandEntity.legJiggle - partialTick) / JIGGLE_TIME);
                transformStack.pushRotation(this.getAnimationProcessor().getBone(DefaultBipedBoneIdents.RIGHT_LEG_ARMOR_BONE_IDENT), 0, -f * .75f, 0);
                transformStack.pushRotation(this.getAnimationProcessor().getBone(DefaultBipedBoneIdents.LEFT_LEG_ARMOR_BONE_IDENT), 0, f * .75f, 0);
                transformStack.pushRotation(this.getAnimationProcessor().getBone(DefaultBipedBoneIdents.RIGHT_LEG_ARMOR_BONE_2_IDENT), 0, -f * .75f, 0);
                transformStack.pushRotation(this.getAnimationProcessor().getBone(DefaultBipedBoneIdents.LEFT_LEG_ARMOR_BONE_2_IDENT), 0, f * .75f, 0);
            }
            if (cursedArmorStandEntity.bootJiggle > 0) {
                float f = elastic(1 - (cursedArmorStandEntity.bootJiggle - partialTick) / JIGGLE_TIME);
                transformStack.pushRotation(this.getAnimationProcessor().getBone(DefaultBipedBoneIdents.RIGHT_FOOT_ARMOR_BONE_IDENT), 0, -f * .75f, 0);
                transformStack.pushRotation(this.getAnimationProcessor().getBone(DefaultBipedBoneIdents.LEFT_FOOT_ARMOR_BONE_IDENT), 0, f * .75f, 0);
                transformStack.pushRotation(this.getAnimationProcessor().getBone(DefaultBipedBoneIdents.RIGHT_FOOT_ARMOR_BONE_2_IDENT), 0, -f * .75f, 0);
                transformStack.pushRotation(this.getAnimationProcessor().getBone(DefaultBipedBoneIdents.LEFT_FOOT_ARMOR_BONE_2_IDENT), 0, f * .75f, 0);
            }
            transformStack.popStack();
        } else {
            super.setCustomAnimations(entity, instanceId, animationState);
        }
    }

    private float elastic(float f) {
        float x = (float) (Math.pow(2, -10 * f) * Mth.cos((10 * f - 0.75f) * 2 * .6f * Mth.PI / 3)) * .2f;
        if (Math.abs(x) < 0.001) {
            return 0;
        }
        return x;
    }

    @Override
    public ResourceLocation getModelResource(AbstractSpellCastingMob object) {
        return MODEL;
    }
}