package io.redspace.ironsspellbooks.item;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Optional;

public interface ILecternPlaceable {
    List<Component> getPages(ItemStack stack);

    default Optional<ResourceLocation> simpleTextureOverride(ItemStack stack){
        return Optional.empty();
    }
}
