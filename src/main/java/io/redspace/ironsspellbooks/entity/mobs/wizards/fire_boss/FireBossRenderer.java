package io.redspace.ironsspellbooks.entity.mobs.wizards.fire_boss;


import io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob.AbstractSpellCastingMob;
import io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob.AbstractSpellCastingMobRenderer;
import io.redspace.ironsspellbooks.render.RenderHelper;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.util.Color;

public class FireBossRenderer extends AbstractSpellCastingMobRenderer {

    public FireBossRenderer(EntityRendererProvider.Context context) {
        super(context, new FireBossModel());
        this.shadowRadius = 0.65f;
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

    int fadeTime = 80;

    @Override
    public Color getRenderColor(AbstractSpellCastingMob animatable, float partialTick, int packedLight) {
        Color color = super.getRenderColor(animatable, partialTick, packedLight);
        if (!animatable.isInvisible() && animatable.deathTime > 160 - fadeTime) {
            color = new Color(RenderHelper.colorf(1f, 1f, 1f, Mth.clamp((160 - animatable.deathTime) / (float) fadeTime, 0, 1f)));
        }

        return color;
    }

    @Override
    public RenderType getRenderType(AbstractSpellCastingMob animatable, ResourceLocation texture, @Nullable MultiBufferSource bufferSource, float partialTick) {
        if (animatable.deathTime > 160 - fadeTime) {
            return RenderType.entityTranslucent(texture);
        }
        return super.getRenderType(animatable, texture, bufferSource, partialTick);
    }

    @Override
    protected float getDeathMaxRotation(AbstractSpellCastingMob animatable) {
        return 0;
    }
}
