package io.redspace.ironsspellbooks.mixin;

import io.redspace.ironsspellbooks.api.attribute.MagicPercentAttribute;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(MagicPercentAttribute.class)
public class Compat$apothic_attributes$AttributeHandler {/*implements IFormattableAttribute {

    public MutableComponent toValueComponent(AttributeModifier.Operation op, double value, TooltipFlag flag) {
        return Component.translatable("apothic_attributes.value.percent", FORMAT.format(value * 100.0));
    }*/
}
