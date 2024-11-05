package io.redspace.ironsspellbooks.api.registry;

import io.redspace.ironsspellbooks.IronsSpellbooks;
import io.redspace.ironsspellbooks.api.attribute.MagicPercentAttribute;
import io.redspace.ironsspellbooks.api.attribute.MagicRangedAttribute;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;


@EventBusSubscriber(modid = IronsSpellbooks.MODID, bus = EventBusSubscriber.Bus.MOD)
public class AttributeRegistry {

    private static final DeferredRegister<Attribute> ATTRIBUTES = DeferredRegister.create(Registries.ATTRIBUTE, IronsSpellbooks.MODID);

    public static void register(IEventBus eventBus) {
        ATTRIBUTES.register(eventBus);
    }

    public static final DeferredHolder<Attribute, Attribute> MAX_MANA = ATTRIBUTES.register("max_mana", () -> (new MagicRangedAttribute("attribute.irons_spellbooks.max_mana", 100.0D, 0.0D, 1000000.0D).setSyncable(true)));
    public static final DeferredHolder<Attribute, Attribute> MANA_REGEN = ATTRIBUTES.register("mana_regen", () -> (new MagicPercentAttribute("attribute.irons_spellbooks.mana_regen", 1.0D, 0.0D, 100.0D).setSyncable(true)));
    public static final DeferredHolder<Attribute, Attribute> COOLDOWN_REDUCTION = ATTRIBUTES.register("cooldown_reduction", () -> (new MagicPercentAttribute("attribute.irons_spellbooks.cooldown_reduction", 1.0D, -100.0D, 100.0D).setSyncable(true)));
    public static final DeferredHolder<Attribute, Attribute> SPELL_POWER = ATTRIBUTES.register("spell_power", () -> (new MagicPercentAttribute("attribute.irons_spellbooks.spell_power", 1.0D, -100, 100.0D).setSyncable(true)));
    public static final DeferredHolder<Attribute, Attribute> SPELL_RESIST = ATTRIBUTES.register("spell_resist", () -> (new MagicPercentAttribute("attribute.irons_spellbooks.spell_resist", 1.0D, -100, 100.0D).setSyncable(true)));
    public static final DeferredHolder<Attribute, Attribute> CAST_TIME_REDUCTION = ATTRIBUTES.register("cast_time_reduction", () -> (new MagicPercentAttribute("attribute.irons_spellbooks.cast_time_reduction", 1.0D, -100, 100.0D).setSyncable(true)));
    public static final DeferredHolder<Attribute, Attribute> SUMMON_DAMAGE = ATTRIBUTES.register("summon_damage", () -> (new MagicPercentAttribute("attribute.irons_spellbooks.summon_damage", 1.0D, -100, 100.0D).setSyncable(true)));
    public static final DeferredHolder<Attribute, Attribute> CASTING_MOVESPEED = ATTRIBUTES.register("casting_movespeed", () -> (new MagicPercentAttribute("attribute.irons_spellbooks.casting_movespeed", 1, 0, 100.0D).setSyncable(true)));

    public static final DeferredHolder<Attribute, Attribute> FIRE_MAGIC_RESIST = newResistanceAttribute("fire");
    public static final DeferredHolder<Attribute, Attribute> ICE_MAGIC_RESIST = newResistanceAttribute("ice");
    public static final DeferredHolder<Attribute, Attribute> LIGHTNING_MAGIC_RESIST = newResistanceAttribute("lightning");
    public static final DeferredHolder<Attribute, Attribute> HOLY_MAGIC_RESIST = newResistanceAttribute("holy");
    public static final DeferredHolder<Attribute, Attribute> ENDER_MAGIC_RESIST = newResistanceAttribute("ender");
    public static final DeferredHolder<Attribute, Attribute> BLOOD_MAGIC_RESIST = newResistanceAttribute("blood");
    public static final DeferredHolder<Attribute, Attribute> EVOCATION_MAGIC_RESIST = newResistanceAttribute("evocation");
    public static final DeferredHolder<Attribute, Attribute> NATURE_MAGIC_RESIST = newResistanceAttribute("nature");
    public static final DeferredHolder<Attribute, Attribute> ELDRITCH_MAGIC_RESIST = newResistanceAttribute("eldritch");

    public static final DeferredHolder<Attribute, Attribute> FIRE_SPELL_POWER = newPowerAttribute("fire");
    public static final DeferredHolder<Attribute, Attribute> ICE_SPELL_POWER = newPowerAttribute("ice");
    public static final DeferredHolder<Attribute, Attribute> LIGHTNING_SPELL_POWER = newPowerAttribute("lightning");
    public static final DeferredHolder<Attribute, Attribute> HOLY_SPELL_POWER = newPowerAttribute("holy");
    public static final DeferredHolder<Attribute, Attribute> ENDER_SPELL_POWER = newPowerAttribute("ender");
    public static final DeferredHolder<Attribute, Attribute> BLOOD_SPELL_POWER = newPowerAttribute("blood");
    public static final DeferredHolder<Attribute, Attribute> EVOCATION_SPELL_POWER = newPowerAttribute("evocation");
    public static final DeferredHolder<Attribute, Attribute> NATURE_SPELL_POWER = newPowerAttribute("nature");
    public static final DeferredHolder<Attribute, Attribute> ELDRITCH_SPELL_POWER = newPowerAttribute("eldritch");

    public static Holder<Attribute> holder(Supplier<Attribute> attributeSupplier) {
        return Holder.direct(attributeSupplier.get());
    }

    @SubscribeEvent
    public static void modifyEntityAttributes(EntityAttributeModificationEvent e) {
        e.getTypes().forEach(entity -> ATTRIBUTES.getEntries().forEach(attribute -> e.add(entity, attribute)));
    }

    private static DeferredHolder<Attribute, Attribute> newResistanceAttribute(String id) {
        return (DeferredHolder<Attribute, Attribute>) ATTRIBUTES.register(id + "_magic_resist", () -> (new MagicPercentAttribute("attribute.irons_spellbooks." + id + "_magic_resist", 1.0D, -100, 100).setSyncable(true)));
    }

    private static DeferredHolder<Attribute, Attribute> newPowerAttribute(String id) {
        return ATTRIBUTES.register(id + "_spell_power", () -> (new MagicPercentAttribute("attribute.irons_spellbooks." + id + "_spell_power", 1.0D, -100, 100).setSyncable(true)));
    }
}
