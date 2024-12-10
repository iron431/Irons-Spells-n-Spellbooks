package io.redspace.ironsspellbooks.item.armor;

import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.entity.armor.BootsOfSpeedArmorModel;
import io.redspace.ironsspellbooks.entity.armor.GenericCustomArmorRenderer;
import io.redspace.ironsspellbooks.item.weapons.AttributeContainer;
import io.redspace.ironsspellbooks.registries.ArmorMaterialRegistry;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

public class BootsOfSpeedArmorItem extends ImbuableChestplateArmorItem {
    public BootsOfSpeedArmorItem(Type type, Properties settings) {
        super(ArmorMaterialRegistry.BOOTS_OF_SPEED, type, settings,
                new AttributeContainer(AttributeRegistry.MAX_MANA, 150, AttributeModifier.Operation.ADD_VALUE),
                new AttributeContainer(AttributeRegistry.CASTING_MOVESPEED, 0.60, AttributeModifier.Operation.ADD_MULTIPLIED_BASE),
                new AttributeContainer(AttributeRegistry.SPELL_POWER, 0.10, AttributeModifier.Operation.ADD_MULTIPLIED_BASE),
                new AttributeContainer(Attributes.MOVEMENT_SPEED, 0.20, AttributeModifier.Operation.ADD_MULTIPLIED_BASE)
        );
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public GeoArmorRenderer<?> supplyRenderer() {
        return new GenericCustomArmorRenderer<>(new BootsOfSpeedArmorModel());
    }

}
