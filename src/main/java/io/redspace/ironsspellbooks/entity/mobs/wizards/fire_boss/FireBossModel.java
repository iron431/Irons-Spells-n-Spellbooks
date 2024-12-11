package io.redspace.ironsspellbooks.entity.mobs.wizards.fire_boss;

import io.redspace.ironsspellbooks.IronsSpellbooks;
import io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob.AbstractSpellCastingMob;
import io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob.AbstractSpellCastingMobModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.PartNames;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.WalkAnimationState;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;

public class FireBossModel extends AbstractSpellCastingMobModel {
    public static final ResourceLocation TEXTURE = new ResourceLocation(IronsSpellbooks.MODID, "textures/entity/tyros.png");
    public static final ResourceLocation MODEL = new ResourceLocation(IronsSpellbooks.MODID, "geo/tyros.geo.json");
    private static final float tilt = 15 * Mth.DEG_TO_RAD;
    private static final Vector3f forward = new Vector3f(0, 0, Mth.sin(tilt) * -12);
    private static final Vector3f armPose = new Vector3f(44, 34, 40);
    @Override
    public ResourceLocation getTextureResource(AbstractSpellCastingMob object) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getModelResource(AbstractSpellCastingMob object) {
        return MODEL;
    }

    int animLerp;
    @Override
    public void setCustomAnimations(AbstractSpellCastingMob entity, long instanceId, AnimationState<AbstractSpellCastingMob> animationState) {
        super.setCustomAnimations(entity, instanceId, animationState);

        if (Minecraft.getInstance().isPaused() || !entity.shouldBeExtraAnimated())
            return;

        float partialTick = animationState.getPartialTick();
        /*
                This overrides all other animation
         */
        GeoBone head = this.getAnimationProcessor().getBone(PartNames.HEAD);
        GeoBone body = this.getAnimationProcessor().getBone(PartNames.BODY);
        GeoBone torso = this.getAnimationProcessor().getBone("torso");
        GeoBone rightArm = this.getAnimationProcessor().getBone(PartNames.RIGHT_ARM);
        GeoBone leftArm = this.getAnimationProcessor().getBone(PartNames.LEFT_ARM);
        GeoBone rightLeg = this.getAnimationProcessor().getBone(PartNames.RIGHT_LEG);
        GeoBone leftLeg = this.getAnimationProcessor().getBone(PartNames.LEFT_LEG);

        /*
            Head Controls
         */
        //Make the head look forward, whatever forward is (influenced externally, such as a lootAt target)

        if (!entity.isAnimating() || entity.shouldAlwaysAnimateHead()) {
            transformStack.pushRotation(head,
                    Mth.lerp(partialTick, -entity.xRotO, -entity.getXRot()) * Mth.DEG_TO_RAD,
                    Mth.lerp(partialTick,
                            Mth.wrapDegrees(-entity.yHeadRotO + entity.yBodyRotO) * Mth.DEG_TO_RAD,
                            Mth.wrapDegrees(-entity.yHeadRot + entity.yBodyRot) * Mth.DEG_TO_RAD
                    ),
                    0);
        }
        /*
            Crazy Vanilla Magic Calculations (LivingEntityRenderer:116 & HumanoidModel#setupAnim
         */
        WalkAnimationState walkAnimationState = entity.walkAnimation;
        float pLimbSwingAmount = 0.0F;
        float pLimbSwing = 0.0F;
        if (entity.isAlive()) {
            pLimbSwingAmount = walkAnimationState.speed(partialTick);
            pLimbSwing = walkAnimationState.position(partialTick);
            if (entity.isBaby()) {
                pLimbSwing *= 3.0F;
            }

            if (pLimbSwingAmount > 1.0F) {
                pLimbSwingAmount = 1.0F;
            }
        }
        float f = 1.0F;
        if (entity.getFallFlyingTicks() > 4) {
            f = (float) entity.getDeltaMovement().lengthSqr();
            f /= 0.2F;
            f *= f * f;
        }

        if (f < 1.0F) {
            f = 1.0F;
        }
        /*
            Leg Controls
         */
        if (entity.isPassenger() && entity.getVehicle().shouldRiderSit()) {
            //If we are riding something, pose ourselves sitting
            transformStack.pushRotation(rightLeg,
                    1.4137167F,
                    -(float) Math.PI / 10F,
                    -0.07853982F
            );
            transformStack.pushRotation(leftLeg,
                    1.4137167F,
                    (float) Math.PI / 10F,
                    0.07853982F
            );
        } else if (!entity.isAnimating() || entity.shouldAlwaysAnimateLegs()) {
            float strength = .75f;
            Vec3 facing = entity.getForward().multiply(1, 0, 1).normalize();
            Vec3 momentum = entity.getDeltaMovement().multiply(1, 0, 1).normalize();
            Vec3 facingOrth = new Vec3(-facing.z, 0, facing.x);
            float directionForward = (float) facing.dot(momentum);
            float directionSide = (float) facingOrth.dot(momentum) * .35f; //scale side to side movement so they dont rip off thier own legs
            float rightLateral = -Mth.sin(pLimbSwing * 0.6662F) * 4 * pLimbSwingAmount;
            float leftLateral = -Mth.sin(pLimbSwing * 0.6662F - Mth.PI) * 4 * pLimbSwingAmount;
            transformStack.pushPosition(rightLeg, rightLateral * directionSide, Mth.cos(pLimbSwing * 0.6662F) * 4 * strength * pLimbSwingAmount, rightLateral * directionForward);
            transformStack.pushRotation(rightLeg, Mth.cos(pLimbSwing * 0.6662F) * 1.4F * pLimbSwingAmount * strength, 0, 0);

            transformStack.pushPosition(leftLeg, leftLateral * directionSide, Mth.cos(pLimbSwing * 0.6662F - Mth.PI) * 4 * strength * pLimbSwingAmount, leftLateral * directionForward);
            transformStack.pushRotation(leftLeg, Mth.cos(pLimbSwing * 0.6662F + (float) Math.PI) * 1.4F * pLimbSwingAmount * strength, 0, 0);

            if (entity.bobBodyWhileWalking()) {
                transformStack.pushPosition(body, 0, Mth.abs(Mth.cos((pLimbSwing * 1.2662F - Mth.PI * .5f) * .5f)) * 2 * strength * pLimbSwingAmount, 0);
            }
        }
        /*
            Arm Controls
         */
        if (!entity.isAnimating()) {
            transformStack.pushRotationWithBase(rightArm, Mth.cos(pLimbSwing * 0.6662F + (float) Math.PI) * 2.0F * pLimbSwingAmount * 0.5F / f, 0, 0);
            transformStack.pushRotationWithBase(leftArm, Mth.cos(pLimbSwing * 0.6662F) * 2.0F * pLimbSwingAmount * 0.5F / f, 0, 0);
        } else if (entity.shouldPointArmsWhileCasting() && entity.isCasting()) {
            transformStack.pushRotation(leftArm, leftArm.getRotX() -entity.getXRot() * Mth.DEG_TO_RAD, leftArm.getRotY(), leftArm.getRotZ());
            transformStack.pushRotation(rightArm, rightArm.getRotX() -entity.getXRot() * Mth.DEG_TO_RAD, rightArm.getRotY(), rightArm.getRotZ());
        }
        transformStack.popStack();
    }
}