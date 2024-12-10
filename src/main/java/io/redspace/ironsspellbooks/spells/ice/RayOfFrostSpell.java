package io.redspace.ironsspellbooks.spells.ice;

import io.redspace.ironsspellbooks.IronsSpellbooks;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.entity.spells.ray_of_frost.RayOfFrostVisualEntity;
import io.redspace.ironsspellbooks.registries.SoundRegistry;
import io.redspace.ironsspellbooks.util.ParticleHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.List;
import java.util.Optional;

@AutoSpellConfig
public class RayOfFrostSpell extends AbstractSpell {
    private final ResourceLocation spellId = new ResourceLocation(IronsSpellbooks.MODID, "ray_of_frost");
    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.COMMON)
            .setSchoolResource(SchoolRegistry.ICE_RESOURCE)
            .setMaxLevel(5)
            .setCooldownSeconds(15)
            .build();

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(getDamage(spellLevel, caster), 2)),
                Component.translatable("ui.irons_spellbooks.freeze_time", Utils.timeFromTicks(getFreezeTime(spellLevel, caster), 2)),
                Component.translatable("ui.irons_spellbooks.distance", Utils.stringTruncation(getRange(spellLevel, caster), 1))
        );
    }

    public RayOfFrostSpell() {
        this.manaCostPerLevel = 15;
        this.baseSpellPower = 6;
        this.spellPowerPerLevel = 1;
        this.castTime = 0;
        this.baseManaCost = 25;
    }

    @Override
    public CastType getCastType() {
        return CastType.INSTANT;
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
        return Optional.of(SoundRegistry.RAY_OF_FROST.get());
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        var hitResult = Utils.raycastForEntity(level, entity, getRange(spellLevel, entity), true, .15f);
        level.addFreshEntity(new RayOfFrostVisualEntity(level, entity.getEyePosition(), hitResult.getLocation(), entity));
        if (hitResult.getType() == HitResult.Type.ENTITY) {
            Entity target = ((EntityHitResult) hitResult).getEntity();
            //Set freeze time right here because it scales off of level and power
            DamageSources.applyDamage(target, getDamage(spellLevel, entity), getDamageSource(entity).setFreezeTicks(target.getTicksRequiredToFreeze() + getFreezeTime(spellLevel, entity)));
            MagicManager.spawnParticles(level, ParticleHelper.ICY_FOG, hitResult.getLocation().x, target.getY(), hitResult.getLocation().z, 4, 0, 0, 0, .3, true);
        } else if (hitResult.getType() == HitResult.Type.BLOCK) {
            MagicManager.spawnParticles(level, ParticleHelper.ICY_FOG, hitResult.getLocation().x, hitResult.getLocation().y, hitResult.getLocation().z, 4, 0, 0, 0, .3, true);
        }
        MagicManager.spawnParticles(level, ParticleHelper.SNOWFLAKE, hitResult.getLocation().x, hitResult.getLocation().y, hitResult.getLocation().z, 50, 0, 0, 0, .3, false);
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    public static float getRange(int level, LivingEntity caster) {
        return 30;
    }

    private float getDamage(int spellLevel, LivingEntity caster) {
        return 3 + getSpellPower(spellLevel, caster) * 1.5f;
    }

    private int getFreezeTime(int spellLevel, LivingEntity caster) {
        return (int) (getSpellPower(spellLevel, caster) * 15);
    }
}
