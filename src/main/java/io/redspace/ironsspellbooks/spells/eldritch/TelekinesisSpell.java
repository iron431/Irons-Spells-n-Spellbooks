package io.redspace.ironsspellbooks.spells.eldritch;

import io.redspace.ironsspellbooks.IronsSpellbooks;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.capabilities.magic.TargetEntityCastData;
import io.redspace.ironsspellbooks.capabilities.magic.TelekinesisData;
import io.redspace.ironsspellbooks.registries.MobEffectRegistry;
import io.redspace.ironsspellbooks.registries.SoundRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

@AutoSpellConfig
public class TelekinesisSpell extends AbstractEldritchSpell {
    private final ResourceLocation spellId = new ResourceLocation(IronsSpellbooks.MODID, "telekinesis");

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(Component.translatable("ui.irons_spellbooks.distance", Utils.stringTruncation(getRange(spellLevel, caster), 1)));
    }

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.LEGENDARY)
            .setSchoolResource(SchoolRegistry.ELDRITCH_RESOURCE)
            .setMaxLevel(5)
            .setCooldownSeconds(35)
            .build();

    public TelekinesisSpell() {
        this.manaCostPerLevel = 0;
        this.baseSpellPower = 8;
        this.spellPowerPerLevel = 4;
        this.castTime = 140;
        this.baseManaCost = 25;
    }

    @Override
    public int getCastTime(int spellLevel) {
        return castTime + 20 * (spellLevel - 1);
    }

    @Override
    public CastType getCastType() {
        return CastType.CONTINUOUS;
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
        return Optional.of(SoundRegistry.TELEKINESIS_CAST.get());
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.of(SoundRegistry.TELEKINESIS_LOOP.get());
    }

    @Override
    public boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData) {
        if (Utils.preCastTargetHelper(level, entity, playerMagicData, this, getRange(spellLevel, entity), .15f)) {
            var target = ((TargetEntityCastData) playerMagicData.getAdditionalCastData()).getTarget((ServerLevel) level);
            if (target == null) {
                return false;
            }
            playerMagicData.setAdditionalCastData(new TelekinesisData(entity.distanceTo(target), target, 6));
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    @Override
    public void onServerCastTick(Level level, int spellLevel, LivingEntity entity, @Nullable MagicData playerMagicData) {
        super.onServerCastTick(level, spellLevel, entity, playerMagicData);
        if (playerMagicData != null && (playerMagicData.getCastDurationRemaining()) % 2 == 0) {
            handleTelekinesis((ServerLevel) level, entity, playerMagicData, .6f);
        }
    }

    private void handleTelekinesis(ServerLevel world, LivingEntity entity, MagicData playerMagicData, float strength) {
        if (playerMagicData.getAdditionalCastData() instanceof TelekinesisData targetData) {
            var targetEntity = targetData.getTarget(world);
            if (targetEntity != null) {
                if ((targetEntity.isRemoved() || targetEntity.isDeadOrDying()) && entity instanceof ServerPlayer serverPlayer) {
                    Utils.serverSideCancelCast(serverPlayer);
                    return;
                }
                float resistance = Mth.clamp(1 - (float) targetEntity.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), .2f, 1f);
                float lockedDistance = targetData.getDistance();
                float actualDistance = entity.distanceTo(targetEntity);
                float distance = Mth.lerp(actualDistance > lockedDistance ? .25f : .1f, lockedDistance, actualDistance);
                targetData.setDistance(distance);
                Vec3 force = (entity.getForward().normalize().scale(targetData.getDistance()).add(entity.position()).subtract(targetEntity.position())).scale(resistance * strength);
                Vec3 travel = new Vec3(targetEntity.getX() - targetEntity.xOld, targetEntity.getY() - targetEntity.yOld, targetEntity.getZ() - targetEntity.zOld);
                if (force.y > 0) {
                    targetEntity.resetFallDistance();
                }
                if ((playerMagicData.getCastDurationRemaining()) % 10 == 0) {
                    int airborne = (int) (travel.x * travel.x + travel.z * travel.z) / 2;
                    targetEntity.addEffect(new MobEffectInstance(MobEffectRegistry.AIRBORNE.get(), 31, airborne));
                    targetEntity.addEffect(new MobEffectInstance(MobEffectRegistry.ANTIGRAVITY.get(), 11, 0));
                }
                var deltaMovement = targetEntity.getDeltaMovement();
                var newMotion = force.subtract(deltaMovement).scale(0.25).add(deltaMovement);
                var delta = newMotion.subtract(deltaMovement);
                var clampedMotion = new Vec3(
                        Utils.signedMin(delta.x, force.x * 4),
                        Utils.signedMin(delta.y, force.y * 4),
                        Utils.signedMin(delta.z, force.z * 4)
                );
                targetEntity.setDeltaMovement(deltaMovement.add(clampedMotion));
                targetEntity.hurtMarked = true;
            }
        }
    }


    @Override
    public Vector3f getTargetingColor() {
        //color similar to the spell icon
        return new Vector3f(1f, .24f, .95f);
    }

    private int getRange(int spellLevel, LivingEntity caster) {
        return 12 + (spellLevel - 1) * 2;
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        return SpellAnimations.SELF_CAST_ANIMATION;
    }
}
