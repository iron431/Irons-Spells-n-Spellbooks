package io.redspace.ironsspellbooks.entity.mobs.wizards.fire_boss;


import io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob.AbstractSpellCastingMob;
import io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob.AbstractSpellCastingMobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;

public class FireBossRenderer extends AbstractSpellCastingMobRenderer {

    public FireBossRenderer(EntityRendererProvider.Context context) {
        super(context, new FireBossModel());
        addRenderLayer(new FireBossSoulLayer(this));
    }

    @Override
    public int getPackedOverlay(AbstractSpellCastingMob animatable, float u, float partialTick) {
        //disable red flashing during soul mode
        if (!(animatable instanceof FireBossEntity fbe) || !fbe.isSoulMode()) {
            return super.getPackedOverlay(animatable, u, partialTick);
        } else {
            return OverlayTexture.NO_OVERLAY;
        }
    }
}
