package io.redspace.ironsspellbooks.entity.spells.fiery_dagger;

import io.redspace.ironsspellbooks.IronsSpellbooks;
import io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob.AbstractSpellCastingMob;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class FieryDaggerModel extends GeoModel<FieryDaggerEntity> {
    public static final ResourceLocation TEXTURE = new ResourceLocation(IronsSpellbooks.MODID, "textures/entity/fiery_dagger.png");
    public static final ResourceLocation MODEL = new ResourceLocation(IronsSpellbooks.MODID, "geo/fiery_dagger.geo.json");
    @Override
    public ResourceLocation getModelResource(FieryDaggerEntity animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(FieryDaggerEntity animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(FieryDaggerEntity animatable) {
        return AbstractSpellCastingMob.animationInstantCast;
    }
}
