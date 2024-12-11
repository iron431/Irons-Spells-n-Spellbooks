package io.redspace.ironsspellbooks.entity.mobs.wizards.fire_boss;

import io.redspace.ironsspellbooks.IronsSpellbooks;
import io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob.AbstractSpellCastingMob;
import io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob.AbstractSpellCastingMobModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Vector3f;
import software.bernie.geckolib.animation.AnimationState;

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

    float leglerp = 1f;
    float isAnimatingLerp = 1f;

    @Override
    public void setCustomAnimations(AbstractSpellCastingMob entity, long instanceId, AnimationState<AbstractSpellCastingMob> animationState) {
//        if (Minecraft.getInstance().isPaused()) {
//            return;
//        }
//        float partialTick = animationState.getPartialTick();
//        if (entity.isAnimating()) {
//            isAnimatingLerp = Mth.lerp(.2f * partialTick, isAnimatingLerp, 1);
//        } else {
//            isAnimatingLerp = Mth.lerp(.2f * partialTick, isAnimatingLerp, 0);
//        }
//        if (entity.getMainHandItem().is(ItemRegistry.HELLRAZOR)) {
//            GeoBone rightArm = this.getAnimationProcessor().getBone(PartNames.RIGHT_ARM);
//            Vector3f armPose = new Vector3f(-30, -30, 10);
//            armPose.mul(Mth.DEG_TO_RAD * (1 - isAnimatingLerp));
//            transformStack.pushRotation(rightArm, armPose);
//        }
        super.setCustomAnimations(entity, instanceId, animationState);
    }
    //    @Override
//    protected Vector2f getLimbSwing(AbstractSpellCastingMob entity, WalkAnimationState walkAnimationState, float partialTick) {
//        Vector2f swing = super.getLimbSwing(entity, walkAnimationState, partialTick);
//        if (!entity.onGround()) {
//            swing.mul(leglerp);
//            leglerp = Mth.lerp(.2f * partialTick, leglerp, 0.5f);
//        } else if (leglerp < 1) {
//            leglerp = Mth.lerp(.2f * partialTick, leglerp, 1.01f);
//        }
//        return swing;
//    }
}