package io.redspace.ironsspellbooks.entity.mobs.wizards.fire_boss;

import io.redspace.ironsspellbooks.IronsSpellbooks;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob.AbstractSpellCastingMob;
import io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob.AbstractSpellCastingMobModel;
import io.redspace.ironsspellbooks.registries.ItemRegistry;
import io.redspace.ironsspellbooks.registries.ParticleRegistry;
import io.redspace.ironsspellbooks.util.DefaultBipedBoneIdents;
import io.redspace.ironsspellbooks.util.ParticleHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.PartNames;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.WalkAnimationState;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector2f;
import org.joml.Vector3d;
import org.joml.Vector3f;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;

public class FireBossModel extends AbstractSpellCastingMobModel {
    public static final ResourceLocation TEXTURE = new ResourceLocation(IronsSpellbooks.MODID, "textures/entity/tyros.png");
    public static final ResourceLocation TEXTURE_SOUL_MODE = new ResourceLocation(IronsSpellbooks.MODID, "textures/entity/tyros_soul_mode.png");
    public static final ResourceLocation MODEL = new ResourceLocation(IronsSpellbooks.MODID, "geo/tyros.geo.json");
    private static final float tilt = 15 * Mth.DEG_TO_RAD;
    private static final Vector3f forward = new Vector3f(0, 0, Mth.sin(tilt) * -12);

    @Override
    public ResourceLocation getTextureResource(AbstractSpellCastingMob object) {
        if (object instanceof FireBossEntity fireBossEntity && fireBossEntity.isSoulMode()) {
            return TEXTURE_SOUL_MODE;
        }
        return TEXTURE;
    }

    @Override
    public ResourceLocation getModelResource(AbstractSpellCastingMob object) {
        return MODEL;
    }

    //fixme: this really doesnt work when multiple entities exist
    int lastTick;

    @Override
    public void setCustomAnimations(AbstractSpellCastingMob entity, long instanceId, AnimationState<AbstractSpellCastingMob> animationState) {
        if (Minecraft.getInstance().isPaused()) {
            return;
        }
        if (entity instanceof FireBossEntity fireBossEntity) {
            handleParticles(fireBossEntity);
            float partialTick = animationState.getPartialTick();
            Vector2f limbSwing = getLimbSwing(entity, entity.walkAnimation, partialTick);
            if (entity.isAnimating()) {
                fireBossEntity.isAnimatingDampener = Mth.lerp(.3f * partialTick, fireBossEntity.isAnimatingDampener, 0);
            } else {
                fireBossEntity.isAnimatingDampener = Mth.lerp(.1f * partialTick, fireBossEntity.isAnimatingDampener, 1);
            }
            if (entity.getMainHandItem().is(ItemRegistry.HELLRAZOR)) {
                GeoBone rightArm = this.getAnimationProcessor().getBone(PartNames.RIGHT_ARM);
                GeoBone rightHand = this.getAnimationProcessor().getBone(DefaultBipedBoneIdents.RIGHT_HAND_BONE_IDENT);
                Vector3f armPose = new Vector3f(-30, -30, 10);
                armPose.mul(Mth.DEG_TO_RAD * fireBossEntity.isAnimatingDampener);
                transformStack.pushRotation(rightArm, armPose);

                Vector3f scythePos = new Vector3f(-5, 0, -48);
                scythePos.mul(Mth.DEG_TO_RAD * fireBossEntity.isAnimatingDampener);
                transformStack.pushRotation(rightHand, scythePos);

                if (!entity.isAnimating()) {
                    float walkDampener = (Mth.cos(limbSwing.y() * 0.6662F + (float) Math.PI) * 2.0F * limbSwing.x() * 0.5F) * -.75f;
                    transformStack.pushRotation(rightArm, walkDampener, 0, 0);
                }
            }
        }


        super.setCustomAnimations(entity, instanceId, animationState);
    }

    public void handleParticles(FireBossEntity entity) {
        GeoBone particleEmitter = this.getAnimationProcessor().getBone("particle_emitter");
        GeoBone body = this.getAnimationProcessor().getBone("body");
        if (entity.isSpawning()) {
            body.setTrackingMatrices(true);
            if (lastTick != entity.tickCount) {
                int particles = (int) (10 * Mth.clamp((entity.spawnTimer - 20f) / FireBossEntity.SPAWN_ANIM_TIME, 0, 1));
                lastTick = entity.tickCount;
                Vector3d pos = body.getWorldPosition();
                for (int i = 0; i < particles; i++) {
                    Vec3 random = Utils.getRandomVec3(0.5);
                    entity.level.addParticle(ParticleRegistry.EMBEROUS_ASH_PARTICLE.get(), pos.x + random.x, pos.y + 1 + random.y * 2, pos.z + random.z, 200, 0, 0);
                }
            }
        } else {
            body.setTrackingMatrices(false);
        }
        if (entity.isSoulMode()) {
            particleEmitter.setTrackingMatrices(true);
            if (lastTick != entity.tickCount) {
                lastTick = entity.tickCount;
                Vec3 entityMotion = entity.getDeltaMovement().add(0, entity.getGravity(), 0);
                Vector3d headPos = particleEmitter.getWorldPosition().add(entityMotion.x * 3, entityMotion.y * 3, entityMotion.z * 3).add(0, 0.2 * entity.getScale(), 0);
                for (int i = 0; i < 3; i++) {
                    Vec3 random = Utils.getRandomVec3(0.25);
                    entity.level.addParticle(ParticleHelper.FIRE, headPos.x + random.x, headPos.y + random.y, headPos.z + random.z, entityMotion.x * .5, entityMotion.y * .5, entityMotion.z * .5);
                }
            }
        } else {
            particleEmitter.setTrackingMatrices(false);
        }
    }

    @Override
    protected Vector2f getLimbSwing(AbstractSpellCastingMob entity, WalkAnimationState walkAnimationState, float partialTick) {
        Vector2f swing = super.getLimbSwing(entity, walkAnimationState, partialTick);
        swing.mul(0.6f, 1f);
        return swing;
    }
}