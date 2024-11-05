package io.redspace.ironsspellbooks.spells.ender;

import io.redspace.ironsspellbooks.IronsSpellbooks;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.entity.IMagicEntity;
import io.redspace.ironsspellbooks.api.events.CounterSpellEvent;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import io.redspace.ironsspellbooks.capabilities.magic.RecastResult;
import io.redspace.ironsspellbooks.effect.MagicMobEffect;
import io.redspace.ironsspellbooks.entity.mobs.AntiMagicSusceptible;
import io.redspace.ironsspellbooks.entity.mobs.IMagicSummon;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;

@AutoSpellConfig
public class CounterspellSpell extends AbstractSpell {
    private final ResourceLocation spellId = new ResourceLocation(IronsSpellbooks.MODID, "counterspell");

    public CounterspellSpell() {
        this.manaCostPerLevel = 1;
        this.baseSpellPower = 1;
        this.spellPowerPerLevel = 1;
        this.castTime = 0;
        this.baseManaCost = 50;
    }

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.RARE)
            .setSchoolResource(SchoolRegistry.ENDER_RESOURCE)
            .setMaxLevel(1)
            .setCooldownSeconds(10)
            .build();

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
    public void onCast(Level world, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        Vec3 start = entity.getEyePosition();
        Vec3 end = start.add(entity.getForward().normalize().scale(80));
        HitResult hitResult = Utils.raycastForEntity(entity.level, entity, start, end, true, 0.35f, Utils::validAntiMagicTarget);
        Vec3 forward = entity.getForward().normalize();
        if (hitResult instanceof EntityHitResult entityHitResult) {
            var hitEntity = entityHitResult.getEntity();
            double distance = entity.distanceTo(hitEntity);
            for (float i = 1; i < distance; i += .5f) {
                Vec3 pos = entity.getEyePosition().add(forward.scale(i));
                MagicManager.spawnParticles(world, ParticleTypes.ENCHANT, pos.x, pos.y, pos.z, 1, 0, 0, 0, 0, false);
            }

            //if (entityHitResult.getEntity() instanceof AntiMagicSusceptible antiMagicSusceptible && !(antiMagicSusceptible instanceof MagicSummon summon && summon.getSummoner() == entity)) {
            if (!(NeoForge.EVENT_BUS.post(new CounterSpellEvent(entity, hitEntity)).isCanceled())) {
                if (hitEntity instanceof AntiMagicSusceptible antiMagicSusceptible) {
                    if (antiMagicSusceptible instanceof IMagicSummon summon) {
                        if (summon.getSummoner() == entity) {
                            if (summon instanceof Mob mob && mob.getTarget() == null) {
                                antiMagicSusceptible.onAntiMagic(playerMagicData);
                            }
                        } else {
                            antiMagicSusceptible.onAntiMagic(playerMagicData);
                        }


                    } else {
                        antiMagicSusceptible.onAntiMagic(playerMagicData);
                    }
                } else if (hitEntity instanceof ServerPlayer serverPlayer) {
                    Utils.serverSideCancelCast(serverPlayer, true);
                    MagicData.getPlayerMagicData(serverPlayer).getPlayerRecasts().removeAll(RecastResult.COUNTERSPELL);
                } else if (hitEntity instanceof IMagicEntity abstractSpellCastingMob) {
                    abstractSpellCastingMob.cancelCast();
                }
                if (hitEntity instanceof LivingEntity livingEntity) {
                    //toList to avoid concurrent modification
                    for (Holder<MobEffect> mobEffect : livingEntity.getActiveEffectsMap().keySet().stream().toList()) {
                        if (mobEffect.value() instanceof MagicMobEffect) {
                            livingEntity.removeEffect(mobEffect);
                        }
                    }
                }
            } else {
                for (float i = 1; i < 40; i += .5f) {
                    Vec3 pos = entity.getEyePosition().add(forward.scale(i));
                    MagicManager.spawnParticles(world, ParticleTypes.ENCHANT, pos.x, pos.y, pos.z, 1, 0, 0, 0, 0, false);
                    if (!world.getBlockState(BlockPos.containing(pos)).isAir()) {
                        break;
                    }
                }
            }
        }
        super.onCast(world, spellLevel, entity, castSource, playerMagicData);
    }
}