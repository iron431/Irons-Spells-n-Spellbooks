package io.redspace.ironsspellbooks.registries;

import io.redspace.ironsspellbooks.IronsSpellbooks;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageScaling;
import net.minecraft.world.damagesource.DamageType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;

public class DamageTypeRegistry {
    public static final DeferredRegister<DamageType> DAMAGE_TYPE_REGISTRY = DeferredRegister.create(Registries.DAMAGE_TYPE, IronsSpellbooks.MODID);

    public static void register(IEventBus eventBus) {
        DAMAGE_TYPE_REGISTRY.register(eventBus);
    }

    public static ResourceKey<DamageType> register(String name) {
        return ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation(IronsSpellbooks.MODID, name));
    }

    //Spell School Related
    public static final ResourceKey<DamageType> FIRE_MAGIC = register("fire_magic");
    public static final ResourceKey<DamageType> ICE_MAGIC = register("ice_magic");
    public static final ResourceKey<DamageType> LIGHTNING_MAGIC = register("lightning_magic");
    public static final ResourceKey<DamageType> HOLY_MAGIC = register("holy_magic");
    public static final ResourceKey<DamageType> ENDER_MAGIC = register("ender_magic");
    public static final ResourceKey<DamageType> BLOOD_MAGIC = register("blood_magic");
    public static final ResourceKey<DamageType> EVOCATION_MAGIC = register("evocation_magic");
    public static final ResourceKey<DamageType> VOID_MAGIC = register("void_magic");
    public static final ResourceKey<DamageType> POISON_MAGIC = register("poison_magic");

    //Entity & Effect Related
    public static final ResourceKey<DamageType> CAULDRON = register("blood_cauldron");
    public static final ResourceKey<DamageType> HEARTSTOP = register("heartstop");
    public static final ResourceKey<DamageType> DRAGON_BREATH_POOL = register("dragon_breath_pool");
    public static final ResourceKey<DamageType> FIRE_FIELD = register("fire_field");
    public static final ResourceKey<DamageType> POISON_CLOUD = register("poison_cloud");

    public static void bootstrap(BootstapContext<DamageType> context) {
        context.register(FIRE_MAGIC, new DamageType(FIRE_MAGIC.location().getPath(), DamageScaling.WHEN_CAUSED_BY_LIVING_NON_PLAYER, 0f));
        context.register(ICE_MAGIC, new DamageType(ICE_MAGIC.location().getPath(), DamageScaling.WHEN_CAUSED_BY_LIVING_NON_PLAYER, 0f));
        context.register(LIGHTNING_MAGIC, new DamageType(LIGHTNING_MAGIC.location().getPath(), DamageScaling.WHEN_CAUSED_BY_LIVING_NON_PLAYER, 0f));
        context.register(HOLY_MAGIC, new DamageType(HOLY_MAGIC.location().getPath(), DamageScaling.WHEN_CAUSED_BY_LIVING_NON_PLAYER, 0f));
        context.register(ENDER_MAGIC, new DamageType(ENDER_MAGIC.location().getPath(), DamageScaling.WHEN_CAUSED_BY_LIVING_NON_PLAYER, 0f));
        context.register(BLOOD_MAGIC, new DamageType(BLOOD_MAGIC.location().getPath(), DamageScaling.WHEN_CAUSED_BY_LIVING_NON_PLAYER, 0f));
        context.register(EVOCATION_MAGIC, new DamageType(EVOCATION_MAGIC.location().getPath(), DamageScaling.WHEN_CAUSED_BY_LIVING_NON_PLAYER, 0f));
        context.register(VOID_MAGIC, new DamageType(VOID_MAGIC.location().getPath(), DamageScaling.WHEN_CAUSED_BY_LIVING_NON_PLAYER, 0f));
        context.register(POISON_MAGIC, new DamageType(POISON_MAGIC.location().getPath(), DamageScaling.WHEN_CAUSED_BY_LIVING_NON_PLAYER, 0f));

        context.register(CAULDRON, new DamageType(CAULDRON.location().getPath(), DamageScaling.WHEN_CAUSED_BY_LIVING_NON_PLAYER, 0f));
        context.register(HEARTSTOP, new DamageType(HEARTSTOP.location().getPath(), DamageScaling.WHEN_CAUSED_BY_LIVING_NON_PLAYER, 0f));
        context.register(DRAGON_BREATH_POOL, new DamageType(DRAGON_BREATH_POOL.location().getPath(), DamageScaling.WHEN_CAUSED_BY_LIVING_NON_PLAYER, 0f));
        context.register(FIRE_FIELD, new DamageType(FIRE_FIELD.location().getPath(), DamageScaling.WHEN_CAUSED_BY_LIVING_NON_PLAYER, 0f));
        context.register(POISON_CLOUD, new DamageType(POISON_CLOUD.location().getPath(), DamageScaling.WHEN_CAUSED_BY_LIVING_NON_PLAYER, 0f));
    }
}


