package io.redspace.ironsspellbooks.entity.armor.priest;

import io.redspace.ironsspellbooks.entity.armor.GenericCustomArmorRenderer;
import io.redspace.ironsspellbooks.item.armor.PriestArmorItem;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.model.GeoModel;

public class PriestArmorRenderer extends GenericCustomArmorRenderer<PriestArmorItem> {
    public PriestArmorRenderer(GeoModel<PriestArmorItem> model) {
        super(model);
        asyncBones.add(
                new AsyncBone("altArmorHood", EquipmentSlot.HEAD, m -> m.head, Vec3.ZERO)
        );
    }
}
