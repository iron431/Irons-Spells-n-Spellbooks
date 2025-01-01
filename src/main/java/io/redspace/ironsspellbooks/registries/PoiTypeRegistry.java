package io.redspace.ironsspellbooks.registries;


import com.google.common.collect.ImmutableSet;
import io.redspace.ironsspellbooks.IronsSpellbooks;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Set;


public class PoiTypeRegistry {
    private static final DeferredRegister<PoiType> POIS = DeferredRegister.create(Registries.POINT_OF_INTEREST_TYPE, IronsSpellbooks.MODID);

    public static void register(IEventBus eventBus) {
        POIS.register(eventBus);
    }

    public static final DeferredHolder<PoiType, PoiType> FIRE_BOSS_KEYSTONE = POIS.register("fire_boss_keystone", ()->new PoiType(getBlockStates(BlockRegistry.CINDEROUS_SOUL_RUNE.get()),1,1));

    private static Set<BlockState> getBlockStates(Block pBlock) {
        return ImmutableSet.copyOf(pBlock.getStateDefinition().getPossibleStates());
    }
}
