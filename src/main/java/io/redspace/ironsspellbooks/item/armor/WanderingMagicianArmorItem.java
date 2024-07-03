package io.redspace.ironsspellbooks.item.armor;

import io.redspace.ironsspellbooks.entity.armor.GenericCustomArmorRenderer;
import io.redspace.ironsspellbooks.entity.armor.WanderingMagicianModel;
import net.minecraft.world.item.ArmorItem;


import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

public class WanderingMagicianArmorItem extends ExtendedArmorItem {
    public WanderingMagicianArmorItem(ArmorItem.Type slot, Properties settings) {
        super(ExtendedArmorMaterials.WANDERING_MAGICIAN, slot, settings);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public GeoArmorRenderer<?> supplyRenderer() {
        return new GenericCustomArmorRenderer<>(new WanderingMagicianModel());
    }
}