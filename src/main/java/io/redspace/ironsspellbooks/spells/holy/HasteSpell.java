package io.redspace.ironsspellbooks.spells.holy;

import io.redspace.ironsspellbooks.IronsSpellbooks;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.capabilities.magic.TargetEntityCastData;
import io.redspace.ironsspellbooks.entity.spells.target_area.TargetedAreaEntity;
import io.redspace.ironsspellbooks.registries.MobEffectRegistry;
import io.redspace.ironsspellbooks.registries.SoundRegistry;
import io.redspace.ironsspellbooks.spells.TargetedTargetAreaCastData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

@AutoSpellConfig
public class HasteSpell extends AbstractSpell {
    private final ResourceLocation spellId = new ResourceLocation(IronsSpellbooks.MODID, "haste");
    private static final int MAX_TARGETS = 5;

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.hastened", Utils.stringTruncation((1 + getAmplifier(spellLevel, caster)) * .1f * 100, 1)),
                Component.translatable("ui.irons_spellbooks.effect_length", Utils.timeFromTicks(getDuration(spellLevel, caster), 1)),
                Component.translatable("ui.irons_spellbooks.max_victims", MAX_TARGETS)
        );
    }

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.EPIC)
            .setSchoolResource(SchoolRegistry.HOLY_RESOURCE)
            .setMaxLevel(4)
            .setCooldownSeconds(80)
            .build();

    public HasteSpell() {
        this.manaCostPerLevel = 10;
        this.baseSpellPower = 30;
        this.spellPowerPerLevel = 5;
        this.castTime = 30;
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
        return Optional.of(SoundRegistry.CLOUD_OF_REGEN_LOOP.get());
    }

    @Override
    public boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData) {
        //If we did not target a creature, target ourself
        if (!Utils.preCastTargetHelper(level, entity, playerMagicData, this, 32, .35f, false)) {
            playerMagicData.setAdditionalCastData(new TargetEntityCastData(entity));
            if (entity instanceof ServerPlayer serverPlayer) {
                serverPlayer.connection.send(new ClientboundSetActionBarTextPacket(Component.translatable("ui.irons_spellbooks.spell_target_success_self", this.getDisplayName(serverPlayer)).withStyle(ChatFormatting.GREEN)));
            }
        }
        float radius = 3f;
        var target = ((TargetEntityCastData) playerMagicData.getAdditionalCastData()).getTarget((ServerLevel) level);
        var area = TargetedAreaEntity.createTargetAreaEntity(level, target.position(), radius, Utils.packRGB(this.getTargetingColor()), target);
        playerMagicData.setAdditionalCastData(new TargetedTargetAreaCastData(target, area));
        return true;
    }

    @Override
    public void onCast(Level world, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (playerMagicData.getAdditionalCastData() instanceof TargetedTargetAreaCastData targetData) {
            var targetEntity = targetData.getTarget((ServerLevel) world);
            if (targetEntity != null) {
                float radius = 3;
                AtomicInteger targets = new AtomicInteger(0);
                targetEntity.level.getEntitiesOfClass(LivingEntity.class, targetEntity.getBoundingBox().inflate(radius)).forEach((victim) -> {
                    if (targets.get() < MAX_TARGETS && victim.distanceToSqr(targetEntity) < radius * radius && Utils.shouldHealEntity(entity, victim)) {
                        victim.addEffect(new MobEffectInstance(MobEffectRegistry.HASTENED.get(), getDuration(spellLevel, entity), getAmplifier(spellLevel, entity)));
                        targets.incrementAndGet();
                    }
                });
            }
        }
        super.onCast(world, spellLevel, entity, castSource, playerMagicData);
    }

    public int getAmplifier(int spellLevel, LivingEntity caster) {
        return spellLevel - 1;
    }

    public int getDuration(int spellLevel, LivingEntity caster) {
        return (int) (getSpellPower(spellLevel, caster) * 20);
    }
}
