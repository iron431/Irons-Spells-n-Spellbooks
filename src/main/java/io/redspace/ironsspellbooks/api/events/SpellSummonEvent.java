package io.redspace.ironsspellbooks.api.events;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.event.entity.living.LivingEvent;

public class SpellSummonEvent<K extends LivingEntity> extends LivingEvent {
    private LivingEntity caster = null;
    private K creature = null;
    private final ResourceLocation spellId;
    private int spellLevel = 0;
    public SpellSummonEvent(LivingEntity caster, K creature, ResourceLocation spellId, int spellLevel) {
        super(caster);
        this.caster = caster;
        this.creature= creature;
        this.spellId = spellId;
        this.spellLevel = spellLevel;
    }

    public K getCreature() {
        return creature;
    }

    public void setCreature(K creature) {
        this.creature = creature;
    }

    public LivingEntity getCaster() {
        return caster;
    }

    public ResourceLocation getSpellId() {
        return spellId;
    }

    public int getSpellLevel() {
        return spellLevel;
    }
}