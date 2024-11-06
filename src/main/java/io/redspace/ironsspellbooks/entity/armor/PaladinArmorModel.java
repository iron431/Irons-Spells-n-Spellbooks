package io.redspace.ironsspellbooks.entity.armor;

import io.redspace.ironsspellbooks.IronsSpellbooks;
import io.redspace.ironsspellbooks.item.armor.PaladinArmorItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedItemGeoModel;

public class PaladinArmorModel extends DefaultedItemGeoModel<PaladinArmorItem> {

    public PaladinArmorModel() {
        super(new ResourceLocation(IronsSpellbooks.MODID, ""));
    }

    @Override
    public ResourceLocation getModelResource(PaladinArmorItem object) {
        return new ResourceLocation(IronsSpellbooks.MODID, "geo/paladin_chestplate.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(PaladinArmorItem object) {
        return new ResourceLocation(IronsSpellbooks.MODID, "textures/models/armor/paladin_chestplate.png");
    }

    @Override
    public ResourceLocation getAnimationResource(PaladinArmorItem animatable) {
        return new ResourceLocation(IronsSpellbooks.MODID, "animations/wizard_armor_animation.json");
    }
}