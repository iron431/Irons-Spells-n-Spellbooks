package io.redspace.ironsspellbooks.registries;

import io.redspace.ironsspellbooks.IronsSpellbooks;
import io.redspace.ironsspellbooks.worldgen.DegradeSlabsStairsProcessor;
import io.redspace.ironsspellbooks.worldgen.WeatherCopperProcessor;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;


public class StructureProcessorRegistry {

    public static final DeferredRegister<StructureProcessorType<?>> PROCESSORS = DeferredRegister.create(Registries.STRUCTURE_PROCESSOR, IronsSpellbooks.MODID);

    public static void register(IEventBus eventBus) {
        PROCESSORS.register(eventBus);
    }

    //public static final Supplier<StructureProcessorType<RemoveWaterProcessor>> REMOVE_WATER = PROCESSORS.register("remove_water", () -> () -> RemoveWaterProcessor.CODEC);
    public static final Supplier<StructureProcessorType<WeatherCopperProcessor>> WEATHER_COPPER = PROCESSORS.register("weather_copper", () -> () -> WeatherCopperProcessor.CODEC);
    public static final Supplier<StructureProcessorType<DegradeSlabsStairsProcessor>> DEGRADE_SLABS_STAIRS = PROCESSORS.register("degrade_slabs_stairs", () -> () -> DegradeSlabsStairsProcessor.CODEC);
}
