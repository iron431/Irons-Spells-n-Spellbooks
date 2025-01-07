package io.redspace.ironsspellbooks.entity.mobs.wizards.cursed_armor_stand;


import io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob.AbstractSpellCastingMobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public class CursedArmorStandRenderer extends AbstractSpellCastingMobRenderer {
    public CursedArmorStandRenderer(EntityRendererProvider.Context context) {
        super(context, new CursedArmorStandModel());
    }
}
