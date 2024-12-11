package io.redspace.ironsspellbooks.entity.mobs.keeper;

import io.redspace.ironsspellbooks.IronsSpellbooks;
import io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob.AbstractSpellCastingMob;
import io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob.AbstractSpellCastingMobModel;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.object.GeoBone;

public class KeeperModel extends AbstractSpellCastingMobModel {
    public static final ResourceLocation TEXTURE = new ResourceLocation(IronsSpellbooks.MODID, "textures/entity/keeper/keeper.png");
    public static final ResourceLocation modelResource = new ResourceLocation(IronsSpellbooks.MODID, "geo/citadel_keeper.geo.json");

    @Override
    public ResourceLocation getTextureResource(AbstractSpellCastingMob object) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getModelResource(AbstractSpellCastingMob object) {
        return modelResource;
    }

//    @Override
//    public void setCustomAnimations(AbstractSpellCastingMob entity, long instanceId, AnimationState<AbstractSpellCastingMob> animationState) {
//        super.setCustomAnimations(entity, instanceId, animationState);
//        if (Minecraft.getInstance().isPaused())
//            return;
//
//        float partialTick = animationState.getPartialTick();
//
//        GeoBone rightLeg = this.getAnimationProcessor().getBone(PartNames.RIGHT_LEG);
//        GeoBone leftLeg = this.getAnimationProcessor().getBone(PartNames.LEFT_LEG);
//        GeoBone rightArm = this.getAnimationProcessor().getBone(PartNames.RIGHT_ARM);
//        GeoBone leftArm = this.getAnimationProcessor().getBone(PartNames.LEFT_ARM);
//        GeoBone body = this.getAnimationProcessor().getBone(PartNames.BODY);
//
//        boolean tick = lastTick != entity.tickCount;
//        lastTick = entity.tickCount;
//
//        WalkAnimationState walkAnimationState = entity.walkAnimation;
//        float pLimbSwingAmount = 0.0F;
//        float pLimbSwing = 0.0F;
//        if (entity.isAlive()) {
//            pLimbSwingAmount = walkAnimationState.speed(partialTick);
//            pLimbSwing = walkAnimationState.position(partialTick);
//            if (pLimbSwingAmount > 1.0F) {
//                pLimbSwingAmount = 1.0F;
//            }
//            if (entity.hurtTime > 0) {
//                pLimbSwingAmount *= .25f;
//            }
//        }
//        if (!(entity.isPassenger() && entity.getVehicle().shouldRiderSit())) {
//            float strength = .75f;
//            rightLeg.updatePosition(0, Mth.cos(pLimbSwing * 0.6662F) * 4 * strength * pLimbSwingAmount, -Mth.sin(pLimbSwing * 0.6662F) * 4 * pLimbSwingAmount);
//            leftLeg.updatePosition(0, Mth.cos(pLimbSwing * 0.6662F - Mth.PI) * 4 * strength * pLimbSwingAmount, -Mth.sin(pLimbSwing * 0.6662F - Mth.PI) * 4 * pLimbSwingAmount);
//            body.updatePosition(0, Mth.abs(Mth.cos((pLimbSwing * 1.2662F - Mth.PI * .5f) * .5f)) * 2 * strength * pLimbSwingAmount, 0);
//            if (tick) {
//                if (!entity.isAnimating() || entity.shouldAlwaysAnimateLegs()) {
//                    legTween = Mth.lerp(.9f, 0, 1);
//                } else {
//                    legTween = Mth.lerp(.9f, 1, 0);
//                }
//            }
//            rightLeg.setRotX(Mth.cos(pLimbSwing * 0.6662F) * 1.4F * pLimbSwingAmount * legTween * strength);
//            leftLeg.setRotX(Mth.cos(pLimbSwing * 0.6662F + (float) Math.PI) * 1.4F * pLimbSwingAmount * legTween * strength);
//        }
//    }

    private int lastTick;
    private float legTween = 1f;

    protected static void updatePosition(GeoBone bone, float x, float y, float z) {
        bone.setPosX(x);
        bone.setPosY(y);
        bone.setPosZ(z);
    }

    protected static void updateRotation(GeoBone bone, float x, float y, float z) {
        bone.setRotX(x);
        bone.setRotY(y);
        bone.setRotZ(z);
    }
}