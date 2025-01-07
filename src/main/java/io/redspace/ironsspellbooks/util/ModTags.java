package io.redspace.ironsspellbooks.util;

import io.redspace.ironsspellbooks.IronsSpellbooks;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.structure.Structure;

public class ModTags {
    public static final TagKey<Item> SCHOOL_FOCUS = ItemTags.create(new ResourceLocation(IronsSpellbooks.MODID, "school_focus"));
    public static final TagKey<Item> FIRE_FOCUS = ItemTags.create(new ResourceLocation(IronsSpellbooks.MODID, "fire_focus"));
    public static final TagKey<Item> ICE_FOCUS = ItemTags.create(new ResourceLocation(IronsSpellbooks.MODID, "ice_focus"));
    public static final TagKey<Item> LIGHTNING_FOCUS = ItemTags.create(new ResourceLocation(IronsSpellbooks.MODID, "lightning_focus"));
    public static final TagKey<Item> ENDER_FOCUS = ItemTags.create(new ResourceLocation(IronsSpellbooks.MODID, "ender_focus"));
    public static final TagKey<Item> HOLY_FOCUS = ItemTags.create(new ResourceLocation(IronsSpellbooks.MODID, "holy_focus"));
    public static final TagKey<Item> BLOOD_FOCUS = ItemTags.create(new ResourceLocation(IronsSpellbooks.MODID, "blood_focus"));
    public static final TagKey<Item> EVOCATION_FOCUS = ItemTags.create(new ResourceLocation(IronsSpellbooks.MODID, "evocation_focus"));
    public static final TagKey<Item> ELDRITCH_FOCUS = ItemTags.create(new ResourceLocation(IronsSpellbooks.MODID, "eldritch_focus"));
    public static final TagKey<Item> NATURE_FOCUS = ItemTags.create(new ResourceLocation(IronsSpellbooks.MODID, "nature_focus"));
    public static final TagKey<Item> INSCRIBED_RUNES = ItemTags.create(new ResourceLocation(IronsSpellbooks.MODID, "inscribed_rune"));
    public static final TagKey<Item> MITHRIL_INGOT = ItemTags.create(ResourceLocation.parse("c:ingots/mithril"));
    public static final TagKey<Block> SPECTRAL_HAMMER_MINEABLE = BlockTags.create(new ResourceLocation(IronsSpellbooks.MODID, "spectral_hammer_mineable"));
    public static final TagKey<Block> GUARDED_BY_WIZARDS = BlockTags.create(new ResourceLocation(IronsSpellbooks.MODID, "guarded_by_wizards"));

    public static final TagKey<MobEffect> CLEANSE_IMMUNE = TagKey.create(Registries.MOB_EFFECT, new ResourceLocation(IronsSpellbooks.MODID, "cleanse_immune"));
    public static final TagKey<Structure> WAYWARD_COMPASS_LOCATOR = TagKey.create(Registries.STRUCTURE, new ResourceLocation(IronsSpellbooks.MODID, "wayward_compass_locator"));

    public static final TagKey<EntityType<?>> ALWAYS_HEAL = TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation(IronsSpellbooks.MODID, "always_heal"));
    public static final TagKey<EntityType<?>> CANT_ROOT = TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation(IronsSpellbooks.MODID, "cant_root"));
    public static final TagKey<EntityType<?>> VILLAGE_ALLIES = TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation(IronsSpellbooks.MODID, "village_allies"));
    public static final TagKey<EntityType<?>> CANT_USE_PORTAL = TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation(IronsSpellbooks.MODID, "cant_use_portal"));
    public static final TagKey<EntityType<?>> INFERNAL_ALLIES = TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation(IronsSpellbooks.MODID, "infernal_allies"));

    private static TagKey<DamageType> create(String tag) {
        return TagKey.create(Registries.DAMAGE_TYPE, new ResourceLocation(IronsSpellbooks.MODID, tag));
    }
}
