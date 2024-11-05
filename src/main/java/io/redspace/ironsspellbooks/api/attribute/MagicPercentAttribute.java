package io.redspace.ironsspellbooks.api.attribute;

/**
 * Serves as marker class for Apothic Attributes mixin injection (on top of MagicRangedAttribute)
 */
public class MagicPercentAttribute extends MagicRangedAttribute {
    public MagicPercentAttribute(String pDescriptionId, double pDefaultValue, double pMin, double pMax) {
        super(pDescriptionId, pDefaultValue, pMin, pMax);
    }
}
