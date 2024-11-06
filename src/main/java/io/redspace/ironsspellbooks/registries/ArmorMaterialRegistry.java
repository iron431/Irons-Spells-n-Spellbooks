package io.redspace.ironsspellbooks.registries;

import io.redspace.ironsspellbooks.IronsSpellbooks;
import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.List;
import java.util.function.Supplier;

public class ArmorMaterialRegistry {
    private static final DeferredRegister<ArmorMaterial> ARMOR_MATERIALS = DeferredRegister.create(Registries.ARMOR_MATERIAL, IronsSpellbooks.MODID);

    public static void register(IEventBus eventBus) {
        ARMOR_MATERIALS.register(eventBus);
    }

    public static DeferredHolder<ArmorMaterial, ArmorMaterial> TARNISHED = register("tarnished",
            makeArmorMap(0, 0, 0, 0),
            15,
            SoundEvents.ARMOR_EQUIP_DIAMOND,
            () -> Ingredient.of(Tags.Items.INGOTS_IRON),
            0,
            0);

    public static DeferredHolder<ArmorMaterial, ArmorMaterial> WANDERING_MAGICIAN = register("wandering_magician",
            makeArmorMap(2, 6, 5, 2),
            15,
            SoundEvents.ARMOR_EQUIP_LEATHER,
            () -> Ingredient.of(Tags.Items.LEATHERS),
            0,
            0);

    public static DeferredHolder<ArmorMaterial, ArmorMaterial> PUMPKIN = register("pumpkin",
            schoolArmorMap(),
            15,
            SoundEvents.ARMOR_EQUIP_TURTLE,
            () -> Ingredient.of(Items.HAY_BLOCK),
            0,
            0);

    public static DeferredHolder<ArmorMaterial, ArmorMaterial> SCHOOL = register("school_armor",
            schoolArmorMap(),
            20,
            SoundEvents.ARMOR_EQUIP_LEATHER,
            () -> Ingredient.of(ItemRegistry.MAGIC_CLOTH.get()),
            0,
            0);

    public static DeferredHolder<ArmorMaterial, ArmorMaterial> NETHERITE_BATTLEMAGE = register("netherite_battlemage",
            schoolArmorMap(),
            20,
            SoundEvents.ARMOR_EQUIP_NETHERITE,
            () -> Ingredient.of(Tags.Items.INGOTS_NETHERITE),
            3,
            0);

    public static DeferredHolder<ArmorMaterial, ArmorMaterial> PALADIN = register("paladin",
            schoolArmorMap(),
            20,
            SoundEvents.ARMOR_EQUIP_NETHERITE,
            () -> Ingredient.of(Tags.Items.INGOTS_NETHERITE),
            3,
            0);

    public static DeferredHolder<ArmorMaterial, ArmorMaterial> DEV = register("dev",
            makeArmorMap(20, 20, 20, 20),
            20,
            SoundEvents.ARMOR_EQUIP_GOLD,
            () -> Ingredient.of(Tags.Items.INGOTS_GOLD),
            20,
            20);

    private static DeferredHolder<ArmorMaterial, ArmorMaterial> register(
            String name,
            EnumMap<ArmorItem.Type, Integer> defense,
            int enchantmentValue,
            Holder<SoundEvent> equipSound,
            Supplier<Ingredient> repairIngredient,
            float toughness,
            float knockbackResistance
    ) {
        List<ArmorMaterial.Layer> list = List.of(new ArmorMaterial.Layer(IronsSpellbooks.id(name)));
        return ARMOR_MATERIALS.register(name, ()-> new ArmorMaterial(defense, enchantmentValue, equipSound, repairIngredient, list, toughness, knockbackResistance));
    }

    static public EnumMap<ArmorItem.Type, Integer> makeArmorMap(int helmet, int chestplate, int leggings, int boots) {
        return Util.make(new EnumMap<>(ArmorItem.Type.class), (p_266655_) -> {
            p_266655_.put(ArmorItem.Type.BOOTS, boots);
            p_266655_.put(ArmorItem.Type.LEGGINGS, leggings);
            p_266655_.put(ArmorItem.Type.CHESTPLATE, chestplate);
            p_266655_.put(ArmorItem.Type.HELMET, helmet);
        });
    }

    static public EnumMap<ArmorItem.Type, Integer> schoolArmorMap() {
        return makeArmorMap(3, 8, 6, 3);
    }
}
