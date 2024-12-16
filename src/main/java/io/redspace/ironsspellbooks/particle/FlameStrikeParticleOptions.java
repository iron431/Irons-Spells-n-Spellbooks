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
    public final float xn;
    public final float yn;
    public final float zn;

    public FlameStrikeParticleOptions(float xn, float yn, float zn, float scale) {
        this.scale = scale;
        this.xn = xn;
        this.yn = yn;
        this.zn = zn;
    }

    public static StreamCodec<? super ByteBuf, FlameStrikeParticleOptions> STREAM_CODEC = StreamCodec.of(
            (buf, option) -> {
                buf.writeFloat(option.xn);
                buf.writeFloat(option.yn);
                buf.writeFloat(option.zn);
                buf.writeFloat(option.scale);
            },
            (buf) -> new FlameStrikeParticleOptions(buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat())
    );

    public static MapCodec<FlameStrikeParticleOptions> MAP_CODEC = RecordCodecBuilder.mapCodec(object ->
            object.group(
                    Codec.FLOAT.fieldOf("xn").forGetter(p -> ((FlameStrikeParticleOptions) p).xn),
                    Codec.FLOAT.fieldOf("yn").forGetter(p -> ((FlameStrikeParticleOptions) p).yn),
                    Codec.FLOAT.fieldOf("zn").forGetter(p -> ((FlameStrikeParticleOptions) p).zn),
                    Codec.FLOAT.fieldOf("scale").forGetter(p -> ((FlameStrikeParticleOptions) p).scale)
            ).apply(object, FlameStrikeParticleOptions::new
            ));

    public @NotNull ParticleType<FlameStrikeParticleOptions> getType() {
        return ParticleRegistry.FLAME_STRIKE_PARTICLE.get();
    }
}
