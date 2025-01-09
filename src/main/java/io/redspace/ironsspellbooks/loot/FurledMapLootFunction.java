package io.redspace.ironsspellbooks.loot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.redspace.ironsspellbooks.item.FurledMapItem;
import io.redspace.ironsspellbooks.registries.LootRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

import java.util.List;
import java.util.Optional;

public class FurledMapLootFunction extends LootItemConditionalFunction {
    public Optional<String> getDimension() {
        return dimension;
    }

    public String getDestination() {
        return destination;
    }

    public String getTranslation() {
        return translation;
    }

    private final String destination, translation;
    private final Optional<String> dimension;


    public static final MapCodec<FurledMapLootFunction> CODEC = RecordCodecBuilder.mapCodec(builder -> commonFields(builder).and(
            builder.group(
                    Codec.STRING.fieldOf("destination").forGetter(FurledMapLootFunction::getDestination),
                    Codec.STRING.fieldOf("description_translation").forGetter(FurledMapLootFunction::getTranslation),
                    Codec.STRING.optionalFieldOf("dimension").forGetter(FurledMapLootFunction::getDimension)
            )
    ).apply(builder, FurledMapLootFunction::new));

    protected FurledMapLootFunction(List<LootItemCondition> lootConditions, String destination, String translation, Optional<String> dimension) {
        super(lootConditions);
        this.destination = destination;
        this.translation = translation;
        this.dimension = dimension;
    }

    @Override
    protected ItemStack run(ItemStack itemStack, LootContext lootContext) {
        if (itemStack.getItem() instanceof FurledMapItem) {
            //todo: ancient map support
            if (dimension.isPresent()) {
                return FurledMapItem.of(ResourceLocation.parse(destination), ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(dimension.get())), Component.translatable(translation));
            } else {
                return FurledMapItem.of(ResourceLocation.parse(destination), Component.translatable(translation));
            }
        }
        return itemStack;
    }

    @Override
    public LootItemFunctionType getType() {
        return LootRegistry.SET_FURLED_MAP_FUNCTION.get();
    }
}
