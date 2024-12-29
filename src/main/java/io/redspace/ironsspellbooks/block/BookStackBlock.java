package io.redspace.ironsspellbooks.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

public class BookStackBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<BookStackBlock> CODEC = simpleCodec((p)->new BookStackBlock());

    @Override
    public MapCodec<BookStackBlock> codec() {
        return CODEC;
    }
    public static final VoxelShape SHAPE = Block.box(3, 0, 3, 13, 8, 13);

    public BookStackBlock() {
        super(Properties.of().mapColor(DyeColor.WHITE).noOcclusion().pushReaction(PushReaction.DESTROY).sound(
                new SoundType(1, 1, SoundEvents.BOOK_PUT, SoundEvents.WOOL_STEP, SoundEvents.BOOK_PUT, SoundEvents.WOOL_HIT, SoundEvents.BOOK_PAGE_TURN)
        ).strength(0.2F));
    }

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        return SHAPE;
    }

    public boolean canSurvive(BlockState pState, LevelReader pLevel, BlockPos pPos) {
        return canSupportCenter(pLevel, pPos.below(), Direction.UP);
    }

    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext pContext) {
        Direction direction = pContext.getHorizontalDirection();
        return this.defaultBlockState().setValue(FACING, direction.getOpposite());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

}
