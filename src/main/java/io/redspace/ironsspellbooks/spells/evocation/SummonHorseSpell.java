package io.redspace.ironsspellbooks.spells.evocation;

import io.redspace.ironsspellbooks.IronsSpellbooks;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.events.SpellSummonEvent;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.config.ServerConfigs;
import io.redspace.ironsspellbooks.entity.mobs.SummonedHorse;
import io.redspace.ironsspellbooks.registries.MobEffectRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;

import java.util.Optional;

@AutoSpellConfig
public class SummonHorseSpell extends AbstractSpell {
    private final ResourceLocation spellId = new ResourceLocation(IronsSpellbooks.MODID, "summon_horse");

    public SummonHorseSpell() {
        this.manaCostPerLevel = 2;
        this.baseSpellPower = 2;
        this.spellPowerPerLevel = 1;
        this.castTime = 20;
        this.baseManaCost = 50;
    }

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.COMMON)
            .setSchoolResource(SchoolRegistry.EVOCATION_RESOURCE)
            .setMaxLevel(5)
            .setCooldownSeconds(20)
            .build();

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
        return Optional.of(SoundEvents.ILLUSIONER_PREPARE_MIRROR);
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.of(SoundEvents.ILLUSIONER_MIRROR_MOVE);
    }

    @Override
    public void onCast(Level world, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        int summonTime = 20 * 60 * 10;
        Vec3 spawn = entity.position();
        Vec3 forward = entity.getForward().normalize().scale(1.5f);
        spawn.add(forward.x, 0.15f, forward.z);

        //Teleport pre-existing or create new horse
        var horses = world.getEntitiesOfClass(SummonedHorse.class, entity.getBoundingBox().inflate(100), (summonedHorse) -> summonedHorse.getSummoner() == entity && !summonedHorse.isDeadOrDying());
        SummonedHorse horse = horses.size() > 0 ? horses.get(0) : new SummonedHorse(world, entity);

        horse.setPos(spawn);
        horse.removeEffectNoUpdate(MobEffectRegistry.SUMMON_HORSE_TIMER);
        horse.forceAddEffect(new MobEffectInstance(MobEffectRegistry.SUMMON_HORSE_TIMER, summonTime, 0, false, false, false), null);
        setAttributes(horse, getSpellPower(spellLevel, entity));
        var event = NeoForge.EVENT_BUS.post(new SpellSummonEvent<SummonedHorse>(entity, horse, this.spellId, spellLevel));
        world.addFreshEntity(event.getCreature());
        entity.addEffect(new MobEffectInstance(MobEffectRegistry.SUMMON_HORSE_TIMER, summonTime, 0, false, false, true));

        super.onCast(world, spellLevel, entity, castSource, playerMagicData);
    }

    private void setAttributes(AbstractHorse horse, float power) {
        int maxPower = baseSpellPower + (ServerConfigs.getSpellConfig(this).maxLevel() - 1) * spellPowerPerLevel;
        float quality = power / (float) maxPower;

        float minSpeed = .2f;
        float maxSpeed = .45f;

        float minJump = .6f;
        float maxJump = 1f;

        float minHealth = 10;
        float maxHealth = 40;

        horse.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(Mth.lerp(quality, minSpeed, maxSpeed));
        horse.getAttribute(Attributes.JUMP_STRENGTH).setBaseValue(Mth.lerp(quality, minJump, maxJump));
        horse.getAttribute(Attributes.MAX_HEALTH).setBaseValue(Mth.lerp(quality, minHealth, maxHealth));
        if (!horse.isDeadOrDying())
            horse.setHealth(horse.getMaxHealth());
    }
}
