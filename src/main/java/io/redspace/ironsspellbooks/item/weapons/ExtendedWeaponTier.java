package io.redspace.ironsspellbooks.item.weapons;

import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.registries.ItemRegistry;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;

import java.util.function.Supplier;

public class ExtendedWeaponTier implements Tier, IronsWeaponTier {
    public static ExtendedWeaponTier HELLRAZOR = new ExtendedWeaponTier(2031, 11, -2.8f, 25, BlockTags.INCORRECT_FOR_NETHERITE_TOOL, () -> Ingredient.of(Items.NETHERITE_SCRAP));
    public static ExtendedWeaponTier KEEPER_FLAMBERGE = new ExtendedWeaponTier(1000, 10, -2.7f, 4, BlockTags.INCORRECT_FOR_NETHERITE_TOOL, () -> Ingredient.of(Items.NETHERITE_SCRAP), new AttributeContainer(Attributes.ARMOR, 4, AttributeModifier.Operation.ADD_VALUE));
    public static ExtendedWeaponTier DREADSWORD = new ExtendedWeaponTier(1061, 6, -2.4f, 14, BlockTags.INCORRECT_FOR_DIAMOND_TOOL, () -> Ingredient.of(ItemRegistry.ARCANE_INGOT.get()));
    public static ExtendedWeaponTier MISERY = new ExtendedWeaponTier(1061, 7, -2.1f, 14, BlockTags.INCORRECT_FOR_DIAMOND_TOOL, () -> Ingredient.of(ItemRegistry.ARCANE_INGOT.get()));
    public static ExtendedWeaponTier METAL_MAGEHUNTER = new ExtendedWeaponTier(1561, 6, -2.4f, 12, BlockTags.INCORRECT_FOR_DIAMOND_TOOL, () -> Ingredient.of(ItemRegistry.ARCANE_INGOT.get()), new AttributeContainer(AttributeRegistry.SPELL_RESIST, .15, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
    public static ExtendedWeaponTier CRYSTAL_MAGEHUNTER = new ExtendedWeaponTier(1561, 6, -2.4f, 12, BlockTags.INCORRECT_FOR_DIAMOND_TOOL, () -> Ingredient.of(Items.DIAMOND), new AttributeContainer(AttributeRegistry.SPELL_RESIST, .15, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
    public static ExtendedWeaponTier SPELLBREAKER = new ExtendedWeaponTier(2031, 9, -2.2f, 12, BlockTags.INCORRECT_FOR_DIAMOND_TOOL, () -> Ingredient.of(Items.DIAMOND), new AttributeContainer(AttributeRegistry.COOLDOWN_REDUCTION, .15, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
    public static ExtendedWeaponTier TRUTHSEEKER = new ExtendedWeaponTier(2031, 11, -3f, 10, BlockTags.INCORRECT_FOR_NETHERITE_TOOL, () -> Ingredient.of(ItemRegistry.ARCANE_INGOT.get()));
    public static ExtendedWeaponTier CLAYMORE = new ExtendedWeaponTier(1000, 9, -2.7f, 8, BlockTags.INCORRECT_FOR_IRON_TOOL, () -> Ingredient.of(Items.IRON_INGOT));
    public static ExtendedWeaponTier AMETHYST_RAPIER = new ExtendedWeaponTier(2031, 7, -1.7f, 16, BlockTags.INCORRECT_FOR_DIAMOND_TOOL, () -> Ingredient.of(Items.AMETHYST_SHARD));

    int uses;
    float damage;
    float speed;
    int enchantmentValue;
    TagKey<Block> incorrectBlocksForDrops;
    Supplier<Ingredient> repairIngredient;
    AttributeContainer[] attributes;

    public ExtendedWeaponTier(int uses, float damage, float speed, int enchantmentValue, TagKey<Block> incorrectBlocksForDrops, Supplier<Ingredient> repairIngredient, AttributeContainer... attributes) {
        this.uses = uses;
        this.damage = damage;
        this.speed = speed;
        this.enchantmentValue = enchantmentValue;
        this.incorrectBlocksForDrops = incorrectBlocksForDrops;
        this.repairIngredient = repairIngredient;
        this.attributes = attributes;
    }

    @Override
    public int getUses() {
        return uses;
    }

    @Override
    public float getSpeed() {
        return speed;
    }

    @Override
    public float getAttackDamageBonus() {
        return damage;
    }

    @Override
    public TagKey<Block> getIncorrectBlocksForDrops() {
        return incorrectBlocksForDrops;
    }

    @Override
    public int getEnchantmentValue() {
        return enchantmentValue;
    }

    @Override
    public Ingredient getRepairIngredient() {
        return repairIngredient.get();
    }

    @Override
    public AttributeContainer[] getAdditionalAttributes() {
        return this.attributes;
    }
}
