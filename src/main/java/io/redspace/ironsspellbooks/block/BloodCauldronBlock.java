package io.redspace.ironsspellbooks.block;

import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.damage.ISSDamageTypes;
import io.redspace.ironsspellbooks.registries.ItemRegistry;
import io.redspace.ironsspellbooks.util.ParticleHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.AbstractCauldronBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;

import java.util.HashMap;
import java.util.Map;

public class BloodCauldronBlock extends LayeredCauldronBlock {

    public BloodCauldronBlock() {
        super(Biome.Precipitation.NONE, getInteractionMap(), Properties.ofFullCopy(Blocks.CAULDRON));
    }

    @Override
    public void entityInside(BlockState blockState, Level level, BlockPos pos, Entity entity) {
        if (entity.tickCount % 20 == 0) {
            attemptCookEntity(blockState, level, pos, entity, () -> {
                level.setBlockAndUpdate(pos, blockState.cycle(LayeredCauldronBlock.LEVEL));
                level.gameEvent(null, GameEvent.FLUID_PLACE, pos);
            });
        }

        super.entityInside(blockState, level, pos, entity);
    }

    public static void attemptCookEntity(BlockState blockState, Level level, BlockPos pos, Entity entity, CookExecution execution) {
        if (!level.isClientSide) {
            if (CampfireBlock.isLitCampfire(level.getBlockState(pos.below()))) {
                if (level.getBlockState(pos).getBlock() instanceof AbstractCauldronBlock cauldron) {
                    if (entity instanceof LivingEntity livingEntity && livingEntity.getBoundingBox().intersects(cauldron.defaultBlockState().getInteractionShape(level, pos).bounds().move(pos))) {
                        if (livingEntity.hurt(DamageSources.get(level, ISSDamageTypes.CAULDRON), 2)) {
                            MagicManager.spawnParticles(level, ParticleHelper.BLOOD, entity.getX(), entity.getY() + entity.getBbHeight() / 2, entity.getZ(), 20, .05, .05, .05, .1, false);
                            if (Utils.random.nextDouble() <= .5 && !isCauldronFull(blockState)) {
                                execution.execute();
                            }
                        }

                    }
                }
            }
        }
    }

    private static boolean isCauldronFull(BlockState blockState) {
        if (!blockState.hasProperty(LEVEL))
            return false;
        else return blockState.getValue(LayeredCauldronBlock.LEVEL) == 3;
    }

    public static CauldronInteraction.InteractionMap getInteractionMap() {
        var map = CauldronInteraction.EMPTY.map();

        // Take Blood
        map.put(Items.GLASS_BOTTLE, (blockState, level, blockPos, player, hand, itemStack) -> {
            if (!level.isClientSide) {
                Item item = itemStack.getItem();
                player.setItemInHand(hand, ItemUtils.createFilledResult(itemStack, player, new ItemStack(ItemRegistry.BLOOD_VIAL.get())));
                player.awardStat(Stats.USE_CAULDRON);
                player.awardStat(Stats.ITEM_USED.get(item));
                LayeredCauldronBlock.lowerFillLevel(blockState, level, blockPos);
                level.playSound(null, blockPos, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
                level.gameEvent(null, GameEvent.FLUID_PICKUP, blockPos);
            }

            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        });

        return new CauldronInteraction.InteractionMap("blood_cauldron_interactions", map);
    }

    public interface CookExecution {
        void execute();
    }
}
