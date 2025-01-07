package io.redspace.ironsspellbooks.util;

import io.redspace.ironsspellbooks.particle.FogParticleOptions;
import io.redspace.ironsspellbooks.particle.SparkParticleOptions;
import io.redspace.ironsspellbooks.registries.ParticleRegistry;
import net.minecraft.core.particles.ParticleOptions;
import org.joml.Vector3f;

public class ParticleHelper {
    //public static final ParticleOptions DRAGON_FIRE = ParticleRegistry.DRAGON_FIRE_PARTICLE.get();
    public static final ParticleOptions FIRE = ParticleRegistry.DRAGON_FIRE_PARTICLE.get();
    public static final ParticleOptions FIRE_EMITTER = ParticleRegistry.FIRE_PARTICLE.get();
    public static final ParticleOptions BLOOD = ParticleRegistry.BLOOD_PARTICLE.get();
    public static final ParticleOptions WISP = ParticleRegistry.WISP_PARTICLE.get();
    public static final ParticleOptions BLOOD_GROUND = ParticleRegistry.BLOOD_GROUND_PARTICLE.get();
    public static final ParticleOptions SNOWFLAKE = ParticleRegistry.SNOWFLAKE_PARTICLE.get();
    public static final ParticleOptions ELECTRICITY = ParticleRegistry.ELECTRICITY_PARTICLE.get();
    public static final ParticleOptions UNSTABLE_ENDER = ParticleRegistry.UNSTABLE_ENDER_PARTICLE.get();
    public static final ParticleOptions EMBERS = ParticleRegistry.EMBER_PARTICLE.get();
    public static final ParticleOptions SIPHON = ParticleRegistry.SIPHON_PARTICLE.get();
    public static final ParticleOptions ACID = ParticleRegistry.ACID_PARTICLE.get();
    public static final ParticleOptions ACID_BUBBLE = ParticleRegistry.ACID_BUBBLE_PARTICLE.get();
    public static final ParticleOptions FOG = new FogParticleOptions(new Vector3f(1, 1, 1), 1);
    public static final ParticleOptions VOID_TENTACLE_FOG = new FogParticleOptions(new Vector3f(0.0f, 0.075f, .13f), 2);
    public static final ParticleOptions ROOT_FOG = new FogParticleOptions(new Vector3f(61 / 255f, 40 / 255f, 18 / 255f), .4f);
    public static final ParticleOptions COMET_FOG = new FogParticleOptions(new Vector3f(.75f, .55f, 1f), 1.5f);
    public static final ParticleOptions FOG_THUNDER_LIGHT = new FogParticleOptions(new Vector3f(.5f, .5f, .5f), 1.5f);
    public static final ParticleOptions FOG_THUNDER_DARK = new FogParticleOptions(new Vector3f(.4f, .4f, .4f), 1.5f);
    public static final ParticleOptions POISON_CLOUD = new FogParticleOptions(new Vector3f(.08f, 0.64f, .16f), 1f);
    public static final ParticleOptions FOG_CAMPFIRE_SMOKE = new FogParticleOptions(new Vector3f(138 / 255f, 130 / 255f, 125 / 255f), 1f);
    public static final ParticleOptions ICY_FOG = new FogParticleOptions(new Vector3f(208 / 255f, 249 / 255f, 255 / 255f), 0.6f);
    public static final ParticleOptions SUNBEAM = new FogParticleOptions(new Vector3f(0.95f, 0.97f, 0.36f), 1f);
    public static final ParticleOptions FIREFLY = ParticleRegistry.FIREFLY_PARTICLE.get();
    public static final ParticleOptions PORTAL_FRAME = ParticleRegistry.PORTAL_FRAME_PARTICLE.get();
    public static final ParticleOptions FIERY_SPARKS = new SparkParticleOptions(new Vector3f(1, .6f, 0.3f));
    public static final ParticleOptions ELECTRIC_SPARKS = new SparkParticleOptions(new Vector3f(0.333f, 1f, 1f));
    public static final ParticleOptions SNOW_DUST = ParticleRegistry.SNOW_DUST.get();
    public static final ParticleOptions CLEANSE_PARTICLE = ParticleRegistry.CLEANSE_PARTICLE.get();

}
