package io.redspace.ironsspellbooks.item.armor;

import io.redspace.ironsspellbooks.api.spells.IPresetSpellContainer;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.entity.armor.GenericCustomArmorRenderer;
import io.redspace.ironsspellbooks.entity.armor.TarnishedCrownModel;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;


import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

public class TarnishedCrownArmorItem extends ExtendedArmorItem implements IPresetSpellContainer {
    public TarnishedCrownArmorItem(ArmorItem.Type slot, Properties settings) {
        super(ExtendedArmorMaterials.TARNISHED, slot, settings);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public GeoArmorRenderer<?> supplyRenderer() {
        return new GenericCustomArmorRenderer<>(new TarnishedCrownModel());
    }

    @Override
    public void initializeSpellContainer(ItemStack itemStack) {
        if (itemStack == null) {
            return;
        }

        if (!ISpellContainer.isSpellContainer(itemStack)) {
            var spellContainer = ISpellContainer.create(1, true, true);
            spellContainer.save(itemStack);
        }
    }
}
