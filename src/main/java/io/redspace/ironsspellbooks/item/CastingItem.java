package io.redspace.ironsspellbooks.item;

import net.minecraft.util.Unit;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import static io.redspace.ironsspellbooks.registries.ComponentRegistry.CASTING_IMPLEMENT;
import static io.redspace.ironsspellbooks.registries.ComponentRegistry.MULTIHAND_WEAPON;

public class CastingItem extends Item {
    public CastingItem(Properties pProperties) {
        super(pProperties.component(CASTING_IMPLEMENT, Unit.INSTANCE).component(MULTIHAND_WEAPON, Unit.INSTANCE));
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged;
    }
}
