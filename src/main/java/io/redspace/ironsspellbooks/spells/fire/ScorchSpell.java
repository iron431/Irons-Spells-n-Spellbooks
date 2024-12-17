package io.redspace.ironsspellbooks.spells.fire;


import io.redspace.ironsspellbooks.IronsSpellbooks;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.damage.SpellDamageSource;
import io.redspace.ironsspellbooks.entity.spells.magma_ball.FireField;
import io.redspace.ironsspellbooks.entity.spells.target_area.TargetedAreaEntity;
import io.redspace.ironsspellbooks.registries.SoundRegistry;
import io.redspace.ironsspellbooks.spells.TargetAreaCastData;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

@AutoSpellConfig
public class ScorchSpell extends AbstractSpell {
    private final ResourceLocation spellId = new ResourceLocation(IronsSpellbooks.MODID, "scorch");

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(getDamage(spellLevel, caster), 2)),
                Component.translatable("ui.irons_spellbooks.radius", Utils.stringTruncation(getRadius(caster), 1))
        );
    }

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.UNCOMMON)
            .setSchoolResource(SchoolRegistry.FIRE_RESOURCE)
            .setMaxLevel(10)
            .setCooldownSeconds(12)
            .build();

    public ScorchSpell() {
        this.manaCostPerLevel = 5;
        this.baseSpellPower = 8;
        this.spellPowerPerLevel = 1;
        this.castTime = 20;
        this.baseManaCost = 50;

    }

    @Override
    public CastType getCastType() {
        return CastType.LONG;
    }

    @Override
    public DefaultConfig getDefaultConfig() {
        return defaultConfig;
    }

    @Override
    public ResourceLocation getSpellResource() {
        return spellId;
    }

    @Override
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.of(SoundRegistry.SCORCH_PREPARE.get());
    }

    @Override
    public boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData) {
        float radius = getRadius(entity);
        var hitResult = Utils.raycastForEntity(level, entity, 32, true, .2f);
        Vec3 location = Utils.moveToRelativeGroundLevel(level, hitResult.getLocation(), 3, 6);
        var area = TargetedAreaEntity.createTargetAreaEntity(level, location, radius, Utils.packRGB(this.getTargetingColor()));
        playerMagicData.setAdditionalCastData(new TargetAreaCastData(location, area));
        return true;
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (playerMagicData.getAdditionalCastData() instanceof TargetAreaCastData castData) {
            Vec3 targetArea = castData.getCenter();
            MagicManager.spawnParticles(level, ParticleTypes.LAVA, targetArea.x, targetArea.y, targetArea.z, 25, 1, 1, 1, 1, true);
            MagicManager.spawnParticles(level, ParticleTypes.LAVA, targetArea.x, targetArea.y + 1, targetArea.z, 25, .25, 1.5, .25, 1, false);
            level.playSound(null, targetArea.x, targetArea.y, targetArea.z, SoundRegistry.FIERY_EXPLOSION.get(), SoundSource.PLAYERS, 2, Utils.random.nextIntBetweenInclusive(8, 12) * .1f);
            var radius = castData.getCastingEntity().getRadius();
            var radiusSqr = radius * radius;
            var damage = getDamage(spellLevel, entity);
            var source = getDamageSource(entity);
            level.getEntitiesOfClass(LivingEntity.class, new AABB(targetArea.subtract(radius, radius, radius), targetArea.add(radius, radius, radius)),
                            livingEntity -> livingEntity != entity &&
                                    horizontalDistanceSqr(livingEntity, targetArea) < radiusSqr &&
                                    livingEntity.isPickable() &&
                                    !DamageSources.isFriendlyFireBetween(livingEntity, entity) &&
                                    Utils.hasLineOfSight(level, targetArea.add(0, 1.5, 0), livingEntity.getBoundingBox().getCenter(), true))
                    .forEach(livingEntity -> {
                        DamageSources.applyDamage(livingEntity, damage, source);
                        DamageSources.ignoreNextKnockback(livingEntity);
                    });
            FireField fire = new FireField(level);
            fire.setOwner(entity);
            fire.setDuration(200);
            fire.setDamage(damage * .1f);
            fire.setRadius(radius);
            fire.setCircular();
            fire.moveTo(targetArea);
            level.addFreshEntity(fire);
        }

        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    private float horizontalDistanceSqr(LivingEntity livingEntity, Vec3 vec3) {
        var dx = livingEntity.getX() - vec3.x;
        var dz = livingEntity.getZ() - vec3.z;
        return (float) (dx * dx + dz * dz);
    }

    @Override
    public SpellDamageSource getDamageSource(@Nullable Entity projectile, Entity attacker) {
        return super.getDamageSource(projectile, attacker).setFireTicks(60);
    }

    private float getDamage(int spellLevel, LivingEntity caster) {
        return getSpellPower(spellLevel, caster);
    }

    private float getRadius(LivingEntity caster) {
        return 2.5f;
    }
//
//    @Override
//    public Vector3f getTargetingColor() {
//        return new Vector3f(.7f, .3f, .15f);
//    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        return SpellAnimations.CHARGE_RAISED_HAND;
    }

    @Override
    public AnimationHolder getCastFinishAnimation() {
        return SpellAnimations.ANIMATION_INSTANT_CAST;
    }
}
