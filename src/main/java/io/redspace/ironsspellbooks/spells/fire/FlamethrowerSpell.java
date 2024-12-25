package io.redspace.ironsspellbooks.spells.fire;

import io.redspace.ironsspellbooks.IronsSpellbooks;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.damage.SpellDamageSource;
import io.redspace.ironsspellbooks.entity.spells.magma_ball.FireField;
import io.redspace.ironsspellbooks.network.particles.FlamethrowerParticlesPacket;
import io.redspace.ironsspellbooks.registries.SoundRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

@AutoSpellConfig
public class FlamethrowerSpell extends AbstractSpell {
    private final ResourceLocation spellId = new ResourceLocation(IronsSpellbooks.MODID, "flamethrower_wip_spell");
    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.COMMON)
            .setSchoolResource(SchoolRegistry.FIRE_RESOURCE)
            .setMaxLevel(10)
            .setCooldownSeconds(15)
            .build();

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(getTickDamage(spellLevel, caster), 2)),
                Component.translatable("ui.irons_spellbooks.distance", Utils.stringTruncation(getRange(spellLevel), 1)));
    }

    public FlamethrowerSpell() {
        this.manaCostPerLevel = 1;
        this.baseSpellPower = 4;
        this.spellPowerPerLevel = 1;
        this.castTime = 100;
        this.baseManaCost = 8;
    }

    @Override
    public CastType getCastType() {
        return CastType.CONTINUOUS;
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        return SpellAnimations.ANIMATION_CONTINUOUS_CAST_ONE_HANDED;
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
        return Optional.empty();
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.of(SoundRegistry.FIRE_BREATH_LOOP.get());
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        var hitResult = Utils.raycastForEntity(level, entity, getRange(0), true, .15f);
        if (hitResult.getType() == HitResult.Type.ENTITY) {
            Entity target = ((EntityHitResult) hitResult).getEntity();
            if (target instanceof LivingEntity) {
                DamageSources.applyDamage(target, getTickDamage(spellLevel, entity), getDamageSource(entity));
            }
        }
        Vec3 hitLocation = Utils.moveToRelativeGroundLevel(level, hitResult.getLocation(), 5);
        AABB fireZone = AABB.ofSize(hitLocation, 1.5, 2, 1.5);
        var fireFields = level.getEntitiesOfClass(FireField.class, fireZone,
                field -> field.getOwner() != null && field.getOwner().getUUID().equals(entity.getUUID()) &&
                        field.position().distanceToSqr(hitLocation) < 2.5 * 2.5
        );
        if (!fireFields.isEmpty()) {
            //expand
            float increase = 0.5f;
            var fireField = fireFields.getFirst();
            Vec3 delta = hitLocation.subtract(fireField.position());
            var distance = delta.length();
            fireField.setRadius(Math.min(fireField.getRadius() + increase, 4));
            fireField.setDuration(fireField.getDuration() + 40);
            fireField.setPos(fireField.position().add(delta.normalize().scale(Math.min(distance, increase * 3))));
        } else {
            //create
            FireField fire = new FireField(level);
            fire.setOwner(entity);
            fire.setDuration(150);
            fire.setDamage(1);
            fire.setRadius(2);
            fire.setCircular();
            fire.moveTo(hitLocation);
            level.addFreshEntity(fire);
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    @Override
    public SpellDamageSource getDamageSource(Entity projectile, Entity attacker) {
        return super.getDamageSource(projectile, attacker).setFireTicks(40);
    }

    @Override
    public void onServerCastTick(Level level, int spellLevel, LivingEntity entity, @Nullable MagicData playerMagicData) {
        super.onServerCastTick(level, spellLevel, entity, playerMagicData);
        Vec3 forward = entity.getForward();
        Vec3 start = entity.getEyePosition();
        var range = Utils.raycastForBlock(level, start, start.add(forward.scale(getRange(spellLevel))), ClipContext.Fluid.NONE).getLocation().distanceTo(start);
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(entity, new FlamethrowerParticlesPacket(entity.position().add(0, entity.getEyeHeight() * .8, 0).add(forward), forward.scale(range / 12f)));
    }

    public static float getRange(int level) {
        return 12;
    }

    private float getTickDamage(int spellLevel, LivingEntity caster) {
        return getSpellPower(spellLevel, caster) * .25f;
    }

}
