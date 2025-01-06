package io.redspace.ironsspellbooks.block;

import io.redspace.ironsspellbooks.util.ParticleHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import static net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED;

public class FireflyJar extends Block implements SimpleWaterloggedBlock {
    public FireflyJar() {
        super(Properties.ofFullCopy(Blocks.GLASS).lightLevel((x) -> 8));
        this.registerDefaultState(this.defaultBlockState().setValue(WATERLOGGED, false));
    }

    public static final VoxelShape SHAPE = Shapes.or(Block.box(4, 0, 4, 12, 13, 12), Block.box(6, 13, 6, 10, 16, 10));

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        return SHAPE;
    }

    @Override
    protected BlockState updateShape(BlockState pState, Direction pDirection, BlockState pNeighborState, LevelAccessor pLevel, BlockPos pPos, BlockPos pNeighborPos) {
        if (pState.getValue(WATERLOGGED)) {
            pLevel.scheduleTick(pPos, Fluids.WATER, Fluids.WATER.getTickDelay(pLevel));
        }
        return super.updateShape(pState, pDirection, pNeighborState, pLevel, pPos, pNeighborPos);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext pContext) {
        return this.defaultBlockState().setValue(WATERLOGGED, pContext.getLevel().getFluidState(pContext.getClickedPos()).getType() == Fluids.WATER);
    }

    @Override
    protected FluidState getFluidState(BlockState pState) {
        return pState.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(pState);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        pBuilder.add(WATERLOGGED);
    }

    @Override
    public void animateTick(BlockState pState, Level pLevel, BlockPos pPos, RandomSource pRandom) {
        double d0 = pPos.getX() + 0.5D;
        double d1 = pPos.getY();
        double d2 = pPos.getZ() + 0.5D;
        double d3 = pRandom.nextDouble() * 0.6D - 0.3D;
        double d4 = pRandom.nextDouble() * 0.6D;
        double d6 = pRandom.nextDouble() * 0.6D - 0.3D;

        pLevel.addParticle(ParticleHelper.FIREFLY, d0 + d3, d1 + d4, d2 + d6, 0.0D, 0.0D, 0.0D);
        pLevel.addParticle(ParticleHelper.FIREFLY, d0 + d3 * 2, d1 + d4 * 2, d2 + d6 * 2, 0.0D, 0.0D, 0.0D);

    }
}
