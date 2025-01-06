package io.redspace.ironsspellbooks.spells.lightning;

import io.redspace.ironsspellbooks.IronsSpellbooks;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.capabilities.magic.ImpulseCastData;
import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.registries.MobEffectRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

@AutoSpellConfig
public class AscensionSpell extends AbstractSpell {
    private final ResourceLocation spellId = new ResourceLocation(IronsSpellbooks.MODID, "ascension");

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(getSpellPower(spellLevel, caster), 1)));
    }

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.RARE)
            .setSchoolResource(SchoolRegistry.LIGHTNING_RESOURCE)
            .setMaxLevel(10)
            .setCooldownSeconds(15)
            .build();

    public AscensionSpell() {
        this.manaCostPerLevel = 1;
        this.baseSpellPower = 5;
        this.spellPowerPerLevel = 1;
        this.castTime = 0;
        this.baseManaCost = 50;

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
    public ICastDataSerializable getEmptyCastData() {
        return new ImpulseCastData();
    }

    @Override
    public void onClientCast(Level level, int spellLevel, LivingEntity entity, ICastData castData) {
        if (castData instanceof ImpulseCastData data) {
            entity.hasImpulse = data.hasImpulse;
            double y = Math.max(entity.getDeltaMovement().y, data.y);
            entity.setDeltaMovement(data.x, y, data.z);
        }
        super.onClientCast(level, spellLevel, entity, castData);
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {

        entity.addEffect(new MobEffectInstance(MobEffectRegistry.ASCENSION, 80, 0, false, false, true));

        Vec3 vec = entity.position();
        for (int i = 0; i < 32; i++) {
            if (!level.getBlockState(BlockPos.containing(vec).below()).isAir())
                break;
            vec = vec.subtract(0, 1, 0);
        }
        Vec3 strikePos = vec;

        LightningBolt lightningBolt = EntityType.LIGHTNING_BOLT.create(level);
        lightningBolt.setVisualOnly(true);
        lightningBolt.setDamage(0);
        lightningBolt.setPos(strikePos);
        level.addFreshEntity(lightningBolt);

        //livingEntity.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 100));
        float radius = 5;
        level.getEntities(entity, entity.getBoundingBox().inflate(radius)).forEach(target -> {
            double distance = target.distanceToSqr(strikePos);
            if (distance < radius * radius) {
                float finalDamage = (float) (getDamage(spellLevel, entity) * (1 - distance / (radius * radius)));
                DamageSources.applyDamage(target, finalDamage, getDamageSource(lightningBolt, entity));
                if (target instanceof Creeper creeper) {
                    creeper.thunderHit((ServerLevel) level, lightningBolt);
                }
                if (target instanceof LivingEntity livingEntity) {
                    livingEntity.knockback(0.25f + finalDamage / 10f, entity.getX() - livingEntity.getX(), entity.getZ() - livingEntity.getZ());
                }
            }
        });

        Vec3 motion = entity.getLookAngle().multiply(1, 0, 1).normalize().add(0, 5, 0).scale(.125);
        playerMagicData.setAdditionalCastData(new ImpulseCastData((float) motion.x, (float) motion.y, (float) motion.z, true));
        entity.setDeltaMovement(entity.getDeltaMovement().add(motion));
        entity.hasImpulse = true;


        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    private int getDamage(int spellLevel, LivingEntity caster) {
        return (int) getSpellPower(spellLevel, caster);
    }
}
