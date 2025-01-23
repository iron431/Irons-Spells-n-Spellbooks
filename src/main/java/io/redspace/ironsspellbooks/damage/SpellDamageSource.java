package io.redspace.ironsspellbooks.damage;

import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SpellDamageSource extends DamageSource {
    AbstractSpell spell;
    float lifesteal;
    int freezeTicks;
    int fireTime;
    int iFrames = -1;

    protected SpellDamageSource(@NotNull Entity directEntity, @NotNull Entity causingEntity, @Nullable Vec3 damageSourcePosition, AbstractSpell spell) {
        super(getHolderFromResource(directEntity, spell.getSchoolType().getDamageType()), directEntity, causingEntity, damageSourcePosition);
        this.spell = spell;
    }

    @Override
    public @NotNull Component getLocalizedDeathMessage(@NotNull LivingEntity pLivingEntity) {
        String s = "death.attack." + spell.getDeathMessageId();
        Component component = this.causingEntity == null ? this.directEntity.getDisplayName() : this.causingEntity.getDisplayName();
        return Component.translatable(s, pLivingEntity.getDisplayName(), component);
    }

    //TODO: need a better way to get the registry access without going through the level each time
    private static Holder<DamageType> getHolderFromResource(Entity entity, ResourceKey<DamageType> damageTypeResourceKey) {
        var option = entity.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolder(damageTypeResourceKey);
        if (option.isPresent()) {
            return option.get();
        } else {
            return entity.level().damageSources().genericKill().typeHolder();
        }
    }

    public static SpellDamageSource source(@NotNull Entity entity, @NotNull AbstractSpell spell) {
        return source(entity, entity, spell);
    }

    public static SpellDamageSource source(@NotNull Entity directEntity, @NotNull Entity causingEntity, @NotNull AbstractSpell spell) {
        return new SpellDamageSource(directEntity, causingEntity, null, spell);
    }

    public SpellDamageSource setLifestealPercent(float lifesteal) {
        this.lifesteal = lifesteal;
        return this;
    }

    public SpellDamageSource setFireTicks(int fireTicks) {
        this.fireTime = fireTicks;
        return this;
    }

    public SpellDamageSource setFreezeTicks(int freezeTicks) {
        this.freezeTicks = freezeTicks;
        return this;
    }

    public SpellDamageSource setIFrames(int iFrames) {
        this.iFrames = iFrames;
        return this;
    }

    public DamageSource get() {
        return this;
    }

    public AbstractSpell spell() {
        return this.spell;
    }

    public float getLifestealPercent() {
        return this.lifesteal;
    }

    public int getFireTime() {
        return this.fireTime;
    }

    public int getFreezeTicks() {
        return this.freezeTicks;
    }

    public int getIFrames() {
        return this.iFrames;
    }

    public boolean hasPostHitEffects() {
        return getLifestealPercent() > 0 || getFireTime() > 0 || getFreezeTicks() > 0;
    }
}
