package io.redspace.ironsspellbooks.entity.mobs.wizards.fire_boss;


import io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob.AbstractSpellCastingMobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public class FireBossRenderer extends AbstractSpellCastingMobRenderer {

    public FireBossRenderer(EntityRendererProvider.Context context) {
        super(context, new FireBossModel());
        addRenderLayer(new FireBossSoulLayer(this));
    }

}
