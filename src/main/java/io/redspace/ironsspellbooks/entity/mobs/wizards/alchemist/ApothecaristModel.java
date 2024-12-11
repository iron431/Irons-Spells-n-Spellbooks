package io.redspace.ironsspellbooks.entity.mobs.wizards.alchemist;

import io.redspace.ironsspellbooks.IronsSpellbooks;
import io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob.AbstractSpellCastingMob;
import io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob.AbstractSpellCastingMobModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

public class ApothecaristModel extends AbstractSpellCastingMobModel {
    public static final ResourceLocation TEXTURE = new ResourceLocation(IronsSpellbooks.MODID, "textures/entity/apothecarist.png");
    public static final ResourceLocation MODEL = new ResourceLocation(IronsSpellbooks.MODID, "geo/piglin_casting_mob.geo.json");
    private static final float tilt = 10 * Mth.DEG_TO_RAD;
    private static final Vector3f forward = new Vector3f(0, 0, Mth.sin(tilt) * -12);

    @Override
    public ResourceLocation getModelResource(AbstractSpellCastingMob object) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(AbstractSpellCastingMob object) {
        return TEXTURE;
    }


//    @Override
//    public void setCustomAnimations(AbstractSpellCastingMob entity, long instanceId, AnimationState<AbstractSpellCastingMob> animationState) {
//        float partialTick = animationState.getPartialTick();
//        GeoBone leftEar = this.getAnimationProcessor().getBone("left_ear");
//        GeoBone rightEar = this.getAnimationProcessor().getBone("right_ear");
//        GeoBone head = this.getAnimationProcessor().getBone(PartNames.HEAD);
//        GeoBone body = this.getAnimationProcessor().getBone(PartNames.BODY);
//        GeoBone torso = this.getAnimationProcessor().getBone("torso");
//        GeoBone rightArm = this.getAnimationProcessor().getBone(PartNames.RIGHT_ARM);
//        GeoBone leftArm = this.getAnimationProcessor().getBone(PartNames.LEFT_ARM);
//        GeoBone rightLeg = this.getAnimationProcessor().getBone(PartNames.RIGHT_LEG);
//        GeoBone leftLeg = this.getAnimationProcessor().getBone(PartNames.LEFT_LEG);
//
//        //Limb Offsets
//        transformStack.pushPosition(head, forward);
//        transformStack.pushPosition(rightArm, forward);
//        transformStack.pushPosition(leftArm, forward);
//        transformStack.pushPosition(torso, forward);
//        transformStack.pushRotation(torso, -tilt, 0, 0);
//        transformStack.pushPosition(rightLeg, forward);
//        transformStack.pushPosition(leftLeg, new Vector3f(0, 0, 1));
//        //Potion throw animation
//        if (entity.swingTime > 0) {
//            float rot = Mth.lerp((entity.swingTime - partialTick) / 10f, 0, Mth.PI);
//            transformStack.pushRotation(rightArm, rot, 0, 0);
//        }
//        //Ear animations
//        if (leftEar != null && rightEar != null) {
//            WalkAnimationState walkAnimationState = entity.walkAnimation;
//            float pLimbSwingAmount = 0.0F;
//            float pLimbSwing = 0.0F;
//            if (entity.isAlive()) {
//                pLimbSwingAmount = walkAnimationState.speed(partialTick);
//                pLimbSwing = walkAnimationState.position(partialTick);
//                if (entity.isBaby()) {
//                    pLimbSwing *= 3.0F;
//                }
//
//                if (pLimbSwingAmount > 1.0F) {
//                    pLimbSwingAmount = 1.0F;
//                }
//            }
//            float f = 1.0F;
//            if (entity.getFallFlyingTicks() > 4) {
//                f = (float) entity.getDeltaMovement().lengthSqr();
//                f /= 0.2F;
//                f *= f * f;
//            }
//
//            if (f < 1.0F) {
//                f = 1.0F;
//            }
//            float r = Mth.cos(pLimbSwing * 0.6662F + (float) Math.PI) * 2.0F * pLimbSwingAmount * 0.5F / f;
//            r *= .3f;
//            r += Mth.PI * .08f;
//            transformStack.pushRotation(leftEar, 0, 0, -r);
//            transformStack.pushRotation(rightEar, 0, 0, r);
//
//        }
//        super.setCustomAnimations(entity, instanceId, animationState);
//    }
}