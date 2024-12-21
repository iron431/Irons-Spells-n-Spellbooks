package io.redspace.ironsspellbooks.entity.spells;

import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.registries.EntityRegistry;
import io.redspace.ironsspellbooks.registries.SoundRegistry;
import io.redspace.ironsspellbooks.util.ParticleHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

public class FireEruptionAoe extends AoeEntity {

    public FireEruptionAoe(EntityType<? extends Projectile> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        this.reapplicationDelay = 25;
        this.setCircular();
    }

    public FireEruptionAoe(Level level, float radius) {
        this(EntityRegistry.FIRE_ERUPTION_AOE.get(), level);
        this.setRadius(radius);
    }

    @Override
    public void applyEffect(LivingEntity target) {
        //todo: real damage source
        var damageSource = SpellRegistry.RAISE_HELL_SPELL.get().getDamageSource(this.getOwner() == null ? this : this.getOwner());
        DamageSources.ignoreNextKnockback(target);
        if (target.hurt(damageSource, getDamage())) {
            target.igniteForSeconds(5);
            target.setDeltaMovement(target.getDeltaMovement().add(0, .65, 0));
            target.hurtMarked = true;
        }
    }

    @Override
    public float getParticleCount() {
        return 0f;
    }

    @Override
    public void ambientParticles() {

    }

    int waveAnim = -1; // anim step in units of blocks

    @Override
    public void tick() {
        var radius = this.getRadius();
        var level = this.level;
        if (waveAnim++ < radius) {
            if (!level.isClientSide) {
                if (waveAnim % 2 == 0) {
                    float volume = (waveAnim + 8) / 16f;
                    this.playSound(SoundRegistry.EARTHQUAKE_IMPACT.get(), volume, Utils.random.nextIntBetweenInclusive(90, 110) * .01f);
                }
                var circumferenceMin = waveAnim * 2 * 3.14f;
                var circumferenceMax = (waveAnim + 1) * 2 * 3.14f;
                int minBlocks = Mth.clamp((int) circumferenceMin, 0, 60);
                int maxBlocks = Mth.clamp((int) circumferenceMax, 0, 60);
                float anglePerBlockMin = 360f / minBlocks;
                float anglePerBlockMax = 360f / maxBlocks;
                //block trail
                for (int i = 0; i < minBlocks; i++) {
                    Vec3 vec3 = new Vec3(
                            waveAnim * Mth.cos(anglePerBlockMin * i),
                            0,
                            waveAnim * Mth.sin(anglePerBlockMin * i)
                    );
                    BlockPos blockPos = BlockPos.containing(Utils.moveToRelativeGroundLevel(level, position().add(vec3), 8)).below();
                    Utils.createTremorBlock(level, blockPos, .1f + random.nextFloat() * .2f);
                }
                //fire trail
                for (int i = 0; i < maxBlocks; i++) {
                    Vec3 vec3 = new Vec3(
                            (waveAnim + 1) * Mth.cos(anglePerBlockMax * i),
                            0,
                            (waveAnim + 1) * Mth.sin(anglePerBlockMax * i)
                    );
                    BlockPos blockPos = BlockPos.containing(Utils.moveToRelativeGroundLevel(level, position().add(vec3), 8)).above();
                    Utils.createTremorBlockWithState(level, Blocks.FIRE.defaultBlockState(), blockPos, .1f + random.nextFloat() * .2f);
                }
                List<LivingEntity> targets = this.level.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(this.getInflation().x, this.getInflation().y, this.getInflation().z));
                var r1Sqr = waveAnim * waveAnim;
                var r2Sqr = (waveAnim + 1) * (waveAnim + 1);
                for (LivingEntity target : targets) {
                    var distanceSqr = target.distanceToSqr(this);
                    if (canHitEntity(target) && distanceSqr >= r1Sqr && distanceSqr <= r2Sqr) {
                        if (canHitTargetForGroundContext(target)) {
                            applyEffect(target);
                        }
                    }
                }
            } else {
                // fire particles
                int particles = (int) ((waveAnim + 1) * 2 * 3.14f * 2.5f);
                float anglePerParticle = Mth.TWO_PI / particles;
                for (int i = 0; i < particles; i++) {
                    Vec3 trig = new Vec3(
                            Mth.cos(anglePerParticle * i),
                            0,
                            Mth.sin(anglePerParticle * i)
                    );
                    float r = Mth.lerp(Utils.random.nextFloat(), waveAnim, waveAnim + 1);
                    Vec3 pos = trig.scale(r).add(Utils.getRandomVec3(0.4)).add(this.position()).add(0, 0.5, 0);
                    Vec3 motion = trig.add(Utils.getRandomVec3(0.5)).scale(0.1);
                    level.addParticle(ParticleHelper.FIRE, pos.x, pos.y, pos.z, motion.x, motion.y, motion.z);
                }
            }
        } else {
            this.discard();
        }

    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    protected boolean canHitTargetForGroundContext(LivingEntity target) {
        return Utils.raycastForBlock(target.level, target.position(), target.position().add(0, -1, 0), ClipContext.Fluid.NONE).getType() != HitResult.Type.MISS;
    }

    @Override
    protected Vec3 getInflation() {
        return new Vec3(0, 5, 0);
    }

    @Override
    public EntityDimensions getDimensions(Pose pPose) {
        return EntityDimensions.scalable(this.getRadius() * 2.0F, 3F);
    }

    @Override
    public Optional<ParticleOptions> getParticle() {
        return Optional.empty();
    }

}
