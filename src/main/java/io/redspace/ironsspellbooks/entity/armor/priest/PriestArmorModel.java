package io.redspace.ironsspellbooks.entity.armor.priest;

import io.redspace.ironsspellbooks.IronsSpellbooks;
import io.redspace.ironsspellbooks.item.armor.PriestArmorItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedItemGeoModel;

public class PriestArmorModel extends DefaultedItemGeoModel<PriestArmorItem> {

    public PriestArmorModel() {
        super(new ResourceLocation(IronsSpellbooks.MODID, "armor/priest"));
    }

    @Override
    public ResourceLocation getModelResource(PriestArmorItem object) {
        return new ResourceLocation(IronsSpellbooks.MODID, "geo/priest_armor.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(PriestArmorItem object) {
        return new ResourceLocation(IronsSpellbooks.MODID, "textures/models/armor/priest.png");
    }

    @Override
    public ResourceLocation getAnimationResource(PriestArmorItem animatable) {
        return new ResourceLocation(IronsSpellbooks.MODID, "animations/wizard_armor_animation.json");
    }
//    public static String listOfBonesToString(List<IBone> list){
//        String s = "";
//        for (IBone o:list)
//            s += o.getName()+", ";
//        return s;
//    }
}