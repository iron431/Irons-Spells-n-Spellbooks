package io.redspace.ironsspellbooks.block;

import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.util.ParticleHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;
import org.jetbrains.annotations.Nullable;

import static net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT;
import static net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED;

public class BrazierBlock extends Block implements SimpleWaterloggedBlock {
    public static final BooleanProperty HANGING = BooleanProperty.create("hanging");

    private final boolean soul;

    public BrazierBlock(boolean soul) {
        super(Properties.ofFullCopy(Blocks.CHAIN).lightLevel((blockState) -> blockState.getValue(LIT) ? 15 : 0));
        this.registerDefaultState(this.defaultBlockState().setValue(WATERLOGGED, false).setValue(LIT, true).setValue(HANGING, false));
        this.soul = soul;
    }

    public BrazierBlock() {
        this(false);
    }

    public static final VoxelShape COLLISION_SHAPE = Block.box(1, 0, 1, 15, 10, 15);
    public static final VoxelShape RENDER_SHAPE = Block.box(1, 0, 1, 15, 16, 15);

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        return RENDER_SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        return COLLISION_SHAPE;
    }

    @Override
    protected void entityInside(BlockState pState, Level pLevel, BlockPos blockpos, Entity entity) {
        if (pState.getValue(LIT) && entity instanceof LivingEntity) {
            float margin = 0.0625f; // one pixel
            var bb = entity.getBoundingBox();
            // model is not full cube in horizontal dimensions. make sure they are inside fire before dishing damage
            if (bb.maxX > blockpos.getX() + margin &&
                    bb.maxZ > blockpos.getZ() + margin &&
                    bb.minX < blockpos.getX() + 1 - margin &&
                    bb.minZ < blockpos.getZ() + 1 - margin
            ) {
                entity.hurt(pLevel.damageSources().campfire(), 1);
            }
        }

        super.entityInside(pState, pLevel, blockpos, entity);
    }

    @Override
    protected BlockState updateShape(BlockState pState, Direction pDirection, BlockState pNeighborState, LevelAccessor pLevel, BlockPos pPos, BlockPos pNeighborPos) {
        var newState = super.updateShape(pState, pDirection, pNeighborState, pLevel, pPos, pNeighborPos);
        if (pState.getValue(WATERLOGGED)) {
            pLevel.scheduleTick(pPos, Fluids.WATER, Fluids.WATER.getTickDelay(pLevel));
        }
        if (pDirection == Direction.UP) {
            boolean chain = pNeighborState.is(Blocks.CHAIN) && pNeighborState.getValue(RotatedPillarBlock.AXIS).isVertical();
            if (chain != pState.getValue(HANGING)) {
                newState = newState.setValue(HANGING, chain);
            }
        }
        return newState;
    }

    @Override
    public @Nullable BlockState getToolModifiedState(BlockState state, UseOnContext context, ItemAbility itemAbility, boolean simulate) {
        if (ItemAbilities.SHOVEL_DOUSE == itemAbility) {
            if (state.getBlock() instanceof BrazierBlock && state.getValue(LIT)) {
                if (!simulate) {
                    context.getLevel().playSound(null, context.getClickedPos(), SoundEvents.GENERIC_EXTINGUISH_FIRE, SoundSource.BLOCKS, 1.0F, 1.0F);
                }
                return state.setValue(LIT, false);
            }
        } else if (ItemAbilities.FIRESTARTER_LIGHT == itemAbility) {
            if (state.getBlock() instanceof BrazierBlock && !state.getValue(LIT) && !state.getValue(WATERLOGGED)) {
                return state.setValue(LIT, true);
            }
        }
        return super.getToolModifiedState(state, context, itemAbility, simulate);
    }

    @Override
    public boolean placeLiquid(LevelAccessor pLevel, BlockPos pPos, BlockState pState, FluidState pFluidState) {
        if (!pState.getValue(BlockStateProperties.WATERLOGGED) && pFluidState.getType() == Fluids.WATER) {
            boolean flag = pState.getValue(LIT);
            if (flag) {
                if (!pLevel.isClientSide()) {
                    pLevel.playSound(null, pPos, SoundEvents.GENERIC_EXTINGUISH_FIRE, SoundSource.BLOCKS, 1.0F, 1.0F);
                }
            }

            pLevel.setBlock(pPos, pState.setValue(WATERLOGGED, true).setValue(LIT, false), 3);
            pLevel.scheduleTick(pPos, pFluidState.getType(), pFluidState.getType().getTickDelay(pLevel));
            return true;
        } else {
            return false;
        }
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext pContext) {
        boolean water = pContext.getLevel().getFluidState(pContext.getClickedPos()).getType() == Fluids.WATER;
        boolean hanging = pContext.getLevel().getBlockState(pContext.getClickedPos().above()).is(Blocks.CHAIN);
        return this.defaultBlockState().setValue(WATERLOGGED, water).setValue(LIT, !water).setValue(HANGING, hanging);
    }

    @Override
    protected FluidState getFluidState(BlockState pState) {
        return pState.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(pState);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        pBuilder.add(WATERLOGGED, LIT, HANGING);
    }

    @Override
    public void animateTick(BlockState pState, Level pLevel, BlockPos pPos, RandomSource pRandom) {
        if (pState.getValue(LIT) && (!soul || pRandom.nextFloat() < 0.085f)) {
            float scale = 0.25f;
            double d0 = pPos.getX() + 0.5D;
            double d1 = pPos.getY() + 0.7 + scale;
            double d2 = pPos.getZ() + 0.5D;
            double d3 = Utils.getRandomScaled(scale);
            double d4 = Utils.getRandomScaled(scale);
            double d6 = Utils.getRandomScaled(scale);
            double d7 = pRandom.nextDouble() * 0.17;

            pLevel.addParticle(soul ? ParticleTypes.SOUL : ParticleHelper.EMBERS, d0 + d3, d1 + d4, d2 + d6, 0.0D, d7, 0.0D);
        }
    }
}
