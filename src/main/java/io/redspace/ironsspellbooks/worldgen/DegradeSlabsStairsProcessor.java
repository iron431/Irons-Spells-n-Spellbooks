package io.redspace.ironsspellbooks.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.redspace.ironsspellbooks.registries.StructureProcessorRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import javax.annotation.Nullable;
import java.util.Optional;

public class DegradeSlabsStairsProcessor extends StructureProcessor {
    public static final MapCodec<DegradeSlabsStairsProcessor> CODEC =
            RecordCodecBuilder.mapCodec(builder -> builder.group(
                    Codec.DOUBLE.fieldOf("chance_stairs").forGetter(obj -> (double) obj.chanceStairs),
                    Codec.DOUBLE.fieldOf("chance_slabs").forGetter(obj -> (double) obj.chanceSlabs)
            ).apply(builder, (a, b) -> new DegradeSlabsStairsProcessor(a.floatValue(), b.floatValue())));

    private final float chanceStairs;
    private final float chanceSlabs;

    public DegradeSlabsStairsProcessor(float stairs, float slabs) {
        chanceStairs = stairs;
        chanceSlabs = slabs;
    }

    @Nullable
    @Override
    public StructureTemplate.StructureBlockInfo processBlock(
            LevelReader pLevel,
            BlockPos pOffset,
            BlockPos pPos,
            StructureTemplate.StructureBlockInfo pBlockInfo,
            StructureTemplate.StructureBlockInfo pRelativeBlockInfo,
            StructurePlaceSettings pSettings
    ) {
        RandomSource randomsource = pSettings.getRandom(pRelativeBlockInfo.pos());
        BlockState blockstate = pRelativeBlockInfo.state();
        BlockPos blockpos = pRelativeBlockInfo.pos();
        BlockState blockstate1 = null;
        if (blockstate.is(BlockTags.STAIRS)) {
            blockstate1 = this.maybeReplaceStairs(randomsource, pRelativeBlockInfo.state());
        } else if (blockstate.is(BlockTags.SLABS)) {
            blockstate1 = this.maybeReplaceSlab(randomsource);
        }

        return blockstate1 != null ? new StructureTemplate.StructureBlockInfo(blockpos, blockstate1, pRelativeBlockInfo.nbt()) : pRelativeBlockInfo;
    }

    @Nullable
    private BlockState maybeReplaceStairs(RandomSource pRandom, BlockState pState) {
        Half half = pState.getValue(StairBlock.HALF);
        if (pRandom.nextFloat() >= chanceStairs) {
            return null;
        } else {
            var block = tryGetSlab(pState.getBlock());
            if (block.isPresent()) {
                return block.get().defaultBlockState().setValue(SlabBlock.TYPE, half == Half.TOP ? SlabType.TOP : SlabType.BOTTOM);
            } else {
                return null;
            }
        }
    }

    private Optional<Block> tryGetSlab(Block original) {
        try {
            var stringKey = BuiltInRegistries.BLOCK.getKey(original).toString().replace("_stairs", "_slab");
            var block = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(stringKey));
            if (block.equals(Blocks.AIR)) {
                return Optional.empty();
            } else {
                return Optional.of(block);
            }
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    @Nullable
    private BlockState maybeReplaceSlab(RandomSource pRandom) {
        return pRandom.nextFloat() >= this.chanceSlabs ? Blocks.AIR.defaultBlockState() : null;
    }

    @Override
    protected StructureProcessorType<?> getType() {
        return StructureProcessorRegistry.DEGRADE_SLABS_STAIRS.get();
    }
}
