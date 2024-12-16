package io.redspace.ironsspellbooks.particle;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import io.redspace.ironsspellbooks.registries.ParticleRegistry;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.NotNull;

public class FlameStrikeParticleOptions implements ParticleOptions {
    public final float scale;
    public final float xrot;
    public final float yrot;

    public FlameStrikeParticleOptions(float xrot, float yrot, float scale) {
        this.scale = scale;
        this.xrot = xrot;
        this.yrot = yrot;
    }

    public float getScale() {
        return this.scale;
    }

    public static StreamCodec<? super ByteBuf, FlameStrikeParticleOptions> STREAM_CODEC = StreamCodec.of(
            (buf, option) -> {
                buf.writeFloat(option.xrot);
                buf.writeFloat(option.yrot);
                buf.writeFloat(option.scale);
            },
            (buf) -> {
                return new FlameStrikeParticleOptions(buf.readFloat(), buf.readFloat(), buf.readFloat());
            }
    );

    //For command only?
    public static MapCodec<FlameStrikeParticleOptions> MAP_CODEC = RecordCodecBuilder.mapCodec(object ->
            object.group(
                    Codec.FLOAT.fieldOf("xrot").forGetter(p -> ((FlameStrikeParticleOptions) p).xrot),
                    Codec.FLOAT.fieldOf("yrot").forGetter(p -> ((FlameStrikeParticleOptions) p).yrot),
                    Codec.FLOAT.fieldOf("scale").forGetter(p -> ((FlameStrikeParticleOptions) p).scale)
            ).apply(object, FlameStrikeParticleOptions::new
            ));

    public @NotNull ParticleType<FlameStrikeParticleOptions> getType() {
        return ParticleRegistry.FLAME_STRIKE_PARTICLE.get();
    }
}
