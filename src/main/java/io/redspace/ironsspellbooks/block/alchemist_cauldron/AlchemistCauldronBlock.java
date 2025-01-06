package io.redspace.ironsspellbooks.block.alchemist_cauldron;

import com.mojang.serialization.MapCodec;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.damage.ISSDamageTypes;
import io.redspace.ironsspellbooks.registries.BlockRegistry;
import io.redspace.ironsspellbooks.registries.ItemRegistry;
import io.redspace.ironsspellbooks.util.ParticleHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AlchemistCauldronBlock extends BaseEntityBlock {
    public AlchemistCauldronBlock() {
        super(Properties.ofFullCopy(Blocks.CAULDRON).lightLevel((blockState) -> 3));
    }

    //    private static final VoxelShape INSIDE = box(2, 4, 2, 14, 16, 14);
//    private static final VoxelShape BODY = box(0, 2, 0, 16, 16, 16);
//    private static final VoxelShape RIM_NEGATIVE = box(0, 12, 0, 16, 14, 16);
//    private static final VoxelShape RIM_INNER = box(1, 12, 1, 15, 14, 15);
//    private static final VoxelShape LOGS = Shapes.or(box(0, 0, 4, 16, 2, 6), box(0, 0, 10, 16, 2, 12), box(4, 0, 0, 6, 2, 16), box(10, 0, 0, 12, 2, 16));
//    private static final VoxelShape DETAILED_BODY = Shapes.join(Shapes.or(Shapes.join(BODY, RIM_NEGATIVE, BooleanOp.ONLY_FIRST), RIM_INNER), INSIDE, BooleanOp.ONLY_FIRST);
//    private static final VoxelShape SHAPE = Shapes.or(LOGS, DETAILED_BODY);
    //magic shape. see comment above for legible shape
    private static final VoxelShape SHAPE = Shapes.or(Shapes.or(box(0, 0, 4, 16, 2, 6), box(0, 0, 10, 16, 2, 12), box(4, 0, 0, 6, 2, 16), box(10, 0, 0, 12, 2, 16)), Shapes.join(Shapes.or(Shapes.join(box(0, 2, 0, 16, 16, 16), box(0, 12, 0, 16, 14, 16), BooleanOp.ONLY_FIRST), box(1, 12, 1, 15, 14, 15)), box(2, 4, 2, 14, 16, 14), BooleanOp.ONLY_FIRST));


    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level pLevel, BlockState pState, BlockEntityType<T> pBlockEntityType) {
        return createTicker(pLevel, pBlockEntityType, BlockRegistry.ALCHEMIST_CAULDRON_TILE.get());
    }

    @javax.annotation.Nullable
    protected static <T extends BlockEntity> BlockEntityTicker<T> createTicker(Level pLevel, BlockEntityType<T> pServerType, BlockEntityType<? extends AlchemistCauldronTile> pClientType) {
        return pLevel.isClientSide ? null : createTickerHelper(pServerType, pClientType, AlchemistCauldronTile::serverTick);
    }

    public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        return SHAPE;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult blockHitResult) {
        if (level.getBlockEntity(pos) instanceof AlchemistCauldronTile tile) {
            return tile.handleUse(level.getBlockState(pos), level, pos, player, hand);
        }
        return super.useItemOn(stack, state, level, pos, player, hand, blockHitResult);
    }

    @Override
    public void entityInside(BlockState blockState, Level level, BlockPos pos, Entity entity) {
        if (entity.tickCount % 20 == 0) {
            if (level.getBlockEntity(pos) instanceof AlchemistCauldronTile cauldronTile) {
                    if (entity instanceof LivingEntity livingEntity && livingEntity.hurt(DamageSources.get(level, ISSDamageTypes.CAULDRON), 2)) {
                        MagicManager.spawnParticles(level, ParticleHelper.BLOOD, entity.getX(), entity.getY() + entity.getBbHeight() / 2, entity.getZ(), 20, .05, .05, .05, .1, false);
                        if (cauldronTile.addToOutput(new ItemStack(ItemRegistry.BLOOD_VIAL.get()))) {
                            cauldronTile.setChanged();
                        }
                    }
            }
        }
        super.entityInside(blockState, level, pos, entity);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AlchemistCauldronTile(pos, state);
    }

    @Override
    public void onRemove(BlockState pState, Level pLevel, BlockPos pPos, BlockState pNewState, boolean pIsMoving) {
        if (pState.getBlock() != pNewState.getBlock()) {
            BlockEntity blockEntity = pLevel.getBlockEntity(pPos);
            if (blockEntity instanceof AlchemistCauldronTile cauldronTile) {
                cauldronTile.drops();
            }
        }
        super.onRemove(pState, pLevel, pPos, pNewState, pIsMoving);
    }

    @Override
    @SuppressWarnings("deprecation")
    public @NotNull RenderShape getRenderShape(@NotNull BlockState blockState) {
        return RenderShape.MODEL;
    }

    public static final MapCodec<AlchemistCauldronBlock> CODEC = simpleCodec((t) -> new AlchemistCauldronBlock());

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }
}
