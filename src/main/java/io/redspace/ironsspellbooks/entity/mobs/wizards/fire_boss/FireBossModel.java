package io.redspace.ironsspellbooks.entity.mobs.wizards.fire_boss;

import io.redspace.ironsspellbooks.IronsSpellbooks;
import io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob.AbstractSpellCastingMob;
import io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob.AbstractSpellCastingMobModel;
import io.redspace.ironsspellbooks.registries.ItemRegistry;
import io.redspace.ironsspellbooks.util.DefaultBipedBoneIdents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.PartNames;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Vector2f;
import org.joml.Vector3f;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;

public class FireBossModel extends AbstractSpellCastingMobModel {
    public static final ResourceLocation TEXTURE = new ResourceLocation(IronsSpellbooks.MODID, "textures/entity/tyros.png");
    public static final ResourceLocation MODEL = new ResourceLocation(IronsSpellbooks.MODID, "geo/tyros.geo.json");
    private static final float tilt = 15 * Mth.DEG_TO_RAD;
    private static final Vector3f forward = new Vector3f(0, 0, Mth.sin(tilt) * -12);

    @Override
    public ResourceLocation getTextureResource(AbstractSpellCastingMob object) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getModelResource(AbstractSpellCastingMob object) {
        return MODEL;
    }

    float leglerp = 1f;
    float isAnimatingDampener;

    @Override
    public void setCustomAnimations(AbstractSpellCastingMob entity, long instanceId, AnimationState<AbstractSpellCastingMob> animationState) {
        if (Minecraft.getInstance().isPaused()) {
            return;
        }
        float partialTick = animationState.getPartialTick();
        Vector2f limbSwing = getLimbSwing(entity, entity.walkAnimation, partialTick);
        if (entity.isAnimating()) {
            isAnimatingDampener = Mth.lerp(.3f * partialTick, isAnimatingDampener, 0);
        } else {
            isAnimatingDampener = Mth.lerp(.1f * partialTick, isAnimatingDampener, 1);
        }
        if (entity.getMainHandItem().is(ItemRegistry.HELLRAZOR)) {
            GeoBone rightArm = this.getAnimationProcessor().getBone(PartNames.RIGHT_ARM);
            GeoBone rightHand = this.getAnimationProcessor().getBone(DefaultBipedBoneIdents.RIGHT_HAND_BONE_IDENT);
            Vector3f armPose = new Vector3f(-30, -30, 10);
            armPose.mul(Mth.DEG_TO_RAD * isAnimatingDampener);
            transformStack.pushRotation(rightArm, armPose);

            Vector3f scythePos = new Vector3f(-5, 0, -48);
            scythePos.mul(Mth.DEG_TO_RAD * isAnimatingDampener);
            transformStack.pushRotation(rightHand, scythePos);

            if (!entity.isAnimating()) {
                float walkDampener = (Mth.cos(limbSwing.y() * 0.6662F + (float) Math.PI) * 2.0F * limbSwing.x() * 0.5F) * -.75f;
                transformStack.pushRotation(rightArm, walkDampener, 0, 0);
            }
        }

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