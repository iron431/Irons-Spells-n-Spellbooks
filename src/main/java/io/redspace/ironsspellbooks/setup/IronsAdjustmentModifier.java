package io.redspace.ironsspellbooks.setup;

import dev.kosmx.playerAnim.api.TransformType;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.api.layered.modifier.AbstractModifier;
import dev.kosmx.playerAnim.api.layered.modifier.AdjustmentModifier;
import dev.kosmx.playerAnim.core.util.Vec3f;

import java.util.Optional;
import java.util.function.BiFunction;

/**
 * Implementation of {@link AdjustmentModifier} but uses a BiFunction to include the additional parameter of partial tick when making adjustments
 */
public class IronsAdjustmentModifier extends AbstractModifier {
    /**
     * Keep reference to the instance so that we can invoke a fadeout when we stop an animation
     */
    public static IronsAdjustmentModifier INSTANCE;
    public boolean enabled = true;

    protected BiFunction<String, Float, Optional<AdjustmentModifier.PartModifier>> transformFunction;

    public IronsAdjustmentModifier(BiFunction<String, Float, Optional<AdjustmentModifier.PartModifier>> transformFunction) {
        this.transformFunction = transformFunction;
    }

    protected float getFadeIn(float delta) {
        float fadeIn = 1;
        IAnimation animation = this.getAnim();
        if (animation instanceof KeyframeAnimationPlayer) {
            KeyframeAnimationPlayer player = (KeyframeAnimationPlayer) anim;
            float currentTick = player.getTick() + delta;
            fadeIn = currentTick / (float) player.getData().beginTick;
            fadeIn = Math.min(fadeIn, 1F);
        }
        return fadeIn;
    }

    @Override
    public void tick() {
        super.tick();

        if (remainingFadeout > 0) {
            remainingFadeout -= 1;
            if (remainingFadeout <= 0) {
                instructedFadeout = 0;
            }
        }
    }

    protected int instructedFadeout = 0;
    private int remainingFadeout = 0;

    public void fadeOut(int fadeOut) {
        if (instructedFadeout == 0) {
            instructedFadeout = fadeOut;
            remainingFadeout = fadeOut + 1;
        }
    }

    public void resetFadeOut(){
        instructedFadeout = 0;
        remainingFadeout = 0;
    }

    protected float getFadeOut(float delta) {
        float fadeOut = 1;
        if (remainingFadeout > 0 && instructedFadeout > 0) {
            float current = Math.max(remainingFadeout - delta, 0);
            fadeOut = current / ((float) instructedFadeout);
            fadeOut = Math.min(fadeOut, 1F);
            return fadeOut;
        }
        IAnimation animation = this.getAnim();
        if (animation instanceof KeyframeAnimationPlayer) {
            KeyframeAnimationPlayer player = (KeyframeAnimationPlayer) anim;

            float currentTick = player.getTick() + delta;
            float position = (-1F) * (currentTick - player.getData().stopTick);
            float length = player.getData().stopTick - player.getData().endTick;
            if (length > 0) {
                fadeOut = position / length;
                fadeOut = Math.min(fadeOut, 1F);
            }
        }
        return fadeOut;
    }

    @Override
    public Vec3f get3DTransform(String modelName, TransformType type, float partialTick, Vec3f value0) {
        if (!enabled) {
            return super.get3DTransform(modelName, type, partialTick, value0);
        }

        Optional<AdjustmentModifier.PartModifier> partModifier = transformFunction.apply(modelName, partialTick);

        Vec3f modifiedVector = value0;
        float fade = getFadeIn(partialTick) * getFadeOut(partialTick);
        if (partModifier.isPresent()) {
            modifiedVector = super.get3DTransform(modelName, type, partialTick, modifiedVector);
            return transformVector(modifiedVector, type, partModifier.get(), fade);
        } else {
            return super.get3DTransform(modelName, type, partialTick, value0);
        }
    }

    protected Vec3f transformVector(Vec3f vector, TransformType type, AdjustmentModifier.PartModifier partModifier, float fade) {
        switch (type) {
            case POSITION:
                return vector.add(partModifier.offset().scale(fade));
            case ROTATION:
                return vector.add(partModifier.rotation().scale(fade));
            case SCALE:
                return vector.add(partModifier.scale().scale(fade));
            case BEND:
                break;
        }
        return vector;
    }
}