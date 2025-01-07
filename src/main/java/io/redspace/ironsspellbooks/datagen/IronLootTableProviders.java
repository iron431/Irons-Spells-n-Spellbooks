package io.redspace.ironsspellbooks.datagen;

import io.redspace.ironsspellbooks.registries.BlockRegistry;
import io.redspace.ironsspellbooks.registries.ItemRegistry;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.storage.loot.LootTable;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class IronLootTableProviders {
    static class Block extends BlockLootSubProvider {
        HashSet<net.minecraft.world.level.block.Block> knownBlocks = new HashSet<>();
        private static final Set<Item> EXPLOSION_RESISTANT = Stream.of(
                        BlockRegistry.MITHRIL_ORE.get(),
                        BlockRegistry.MITHRIL_ORE_DEEPSLATE.get()
                )
                .map(ItemLike::asItem)
                .collect(Collectors.toSet());

        public Block(HolderLookup.Provider pRegistries) {
            super(EXPLOSION_RESISTANT, FeatureFlags.REGISTRY.allFlags(), pRegistries);
        }

        @Override
        protected void generate() {
            //TODO: when adding table, make sure to add the block to known blocks (#add does it automatically)
            this.add(BlockRegistry.MITHRIL_ORE.get(), p_249875_ -> this.createOreDrop(p_249875_, ItemRegistry.RAW_MITHRIL.get()));
            this.add(BlockRegistry.MITHRIL_ORE_DEEPSLATE.get(), p_249875_ -> this.createOreDrop(p_249875_, ItemRegistry.RAW_MITHRIL.get()));
            this.add(BlockRegistry.PORTAL_FRAME.get(), p_249875_ -> this.createSingleItemTable(ItemRegistry.PORTAL_FRAME_ITEM.get()));
            this.add(BlockRegistry.WISEWOOD_BOOKSHELF.get(), p_249875_ -> this.createSingleItemTable(ItemRegistry.WISEWOOD_BOOKSHELF_BLOCK_ITEM.get()));
            this.add(BlockRegistry.WISEWOOD_CHISELLED_BOOKSHELF.get(), p_249875_ -> this.createSingleItemTable(ItemRegistry.WISEWOOD_CHISELED_BOOKSHELF_BLOCK_ITEM.get()));
            this.add(BlockRegistry.BRAZIER_FIRE.get(), p_249875_ -> this.createSingleItemTable(ItemRegistry.BRAZIER_ITEM.get()));
            this.add(BlockRegistry.BRAZIER_SOUL.get(), p_249875_ -> this.createSingleItemTable(ItemRegistry.SOUL_BRAZIER_ITEM.get()));
            this.add(BlockRegistry.NETHER_BRICK_PILLAR.get(), p_249875_ -> this.createSingleItemTable(ItemRegistry.NETHER_BRICK_PILLAR_BLOCK_ITEM.get()));
        }

        @Override
        protected void add(net.minecraft.world.level.block.Block pBlock, Function<net.minecraft.world.level.block.Block, LootTable.Builder> pFactory) {
            knownBlocks.add(pBlock);
            super.add(pBlock, pFactory);
        }

        @Override
        protected Iterable<net.minecraft.world.level.block.Block> getKnownBlocks() {
            return knownBlocks;
        }
    }
}
