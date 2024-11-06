package io.redspace.ironsspellbooks.item.armor;

import io.redspace.ironsspellbooks.entity.armor.PaladinArmorModel;
import io.redspace.ironsspellbooks.registries.ArmorMaterialRegistry;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

public class PaladinArmorItem extends ImbuableChestplateArmorItem {
    public PaladinArmorItem(Type type, Properties settings) {
        super(ArmorMaterialRegistry.PALADIN, type, settings, withManaAndSpellPowerAttribute(125, 0.05));
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public GeoArmorRenderer<?> supplyRenderer() {
        return new GeoArmorRenderer<>(new PaladinArmorModel());
    }

}
