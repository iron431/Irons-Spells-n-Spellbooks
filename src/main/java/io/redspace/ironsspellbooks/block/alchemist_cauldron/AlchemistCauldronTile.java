package io.redspace.ironsspellbooks.block.alchemist_cauldron;

import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import io.redspace.ironsspellbooks.config.ServerConfigs;
import io.redspace.ironsspellbooks.item.InkItem;
import io.redspace.ironsspellbooks.item.Scroll;
import io.redspace.ironsspellbooks.item.consumables.SimpleElixir;
import io.redspace.ironsspellbooks.registries.BlockRegistry;
import io.redspace.ironsspellbooks.registries.ItemRegistry;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.Util;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class AlchemistCauldronTile extends BlockEntity implements WorldlyContainer {
    /*
    Cauldron Interaction Doctrine:
    - Placing in an input item should always replace a liquid item when brewed (or not brew at all)
        - Scroll: replace water for ink
        - Potion Reagent: replace previous potion for next potion
        - Elixir Crafting: consume previous liquid items, produce new liquid item
    - Emptying a liquid into the cauldron should always increase the level, and extracting a liquid item should decrease it (the level should be strictly tied to the amount of liquid item present)
     */
    public static int MAX_LEVELS = 4;
    public static final Object2ObjectOpenHashMap<Item, AlchemistCauldronInteraction> INTERACTIONS = AlchemistCauldronTile.newInteractionMap();
    public final NonNullList<ItemStack> inputItems = NonNullList.withSize(MAX_LEVELS, ItemStack.EMPTY);
    public final NonNullList<ItemStack> outputItems = NonNullList.withSize(MAX_LEVELS, ItemStack.EMPTY);
    private final int[] cooktimes = new int[MAX_LEVELS];

    public AlchemistCauldronTile(BlockPos pWorldPosition, BlockState pBlockState) {
        super(BlockRegistry.ALCHEMIST_CAULDRON_TILE.get(), pWorldPosition, pBlockState);
    }

    /************************************************************
     Logic
     ***********************************************************/
    public static void serverTick(Level level, BlockPos pos, BlockState blockState, AlchemistCauldronTile cauldronTile) {
        for (int i = 0; i < cauldronTile.inputItems.size(); i++) {
            ItemStack itemStack = cauldronTile.inputItems.get(i);
            if (itemStack.isEmpty() || !cauldronTile.isBoiling(blockState))
                cauldronTile.cooktimes[i] = 0;
            else {
                cauldronTile.cooktimes[i]++;
            }
            if (cauldronTile.cooktimes[i] > 100) {
                cauldronTile.tryMeltInput(itemStack);
                cauldronTile.cooktimes[i] = 0;
            }
        }
        var random = Utils.random;
        if (cauldronTile.isBoiling(blockState)) {
            float waterLevel = Mth.lerp(cauldronTile.getLiquidLevel() / (float) MAX_LEVELS, .25f, .9f);
            MagicManager.spawnParticles(level, ParticleTypes.BUBBLE_POP, pos.getX() + Mth.randomBetween(random, .2f, .8f), pos.getY() + waterLevel, pos.getZ() + Mth.randomBetween(random, .2f, .8f), 1, 0, 0, 0, 0, false);
        }
    }

    public ItemInteractionResult handleUse(BlockState blockState, Level level, BlockPos pos, Player player, InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);
        if (level.getBlockEntity(pos) instanceof AlchemistCauldronTile tile) {
            var cauldronInteractionResult = INTERACTIONS.get(itemStack.getItem()).interact(tile, blockState, level, pos, itemStack);
            if (cauldronInteractionResult != null) {
                player.setItemInHand(hand, ItemUtils.createFilledResult(itemStack, player, cauldronInteractionResult));
                this.setChanged();
                return ItemInteractionResult.sidedSuccess(level.isClientSide);
            } else if (isValidInput(itemStack)) {
                if (!level.isClientSide) {
                    for (int i = 0; i < inputItems.size(); i++) {
                        var stack = inputItems.get(i);
                        if (stack.isEmpty()) {
                            var input = player.getAbilities().instabuild ? itemStack.copy() : itemStack.split(1);
                            input.setCount(1);
                            inputItems.set(i, input);
                            player.setItemInHand(hand, itemStack);
                            this.setChanged();
                            break;
                        }
                    }
                }
                return ItemInteractionResult.sidedSuccess(level.isClientSide);
            } else if ((itemStack.isEmpty() || player.isCrouching()) && hand.equals(InteractionHand.MAIN_HAND)) {
                for (ItemStack item : inputItems) {
                    if (!item.isEmpty()) {
                        if (!level.isClientSide) {
                            var take = item.split(1);
                            if (player.getItemInHand(hand).isEmpty()) {
                                player.setItemInHand(hand, take);
                            } else {
                                if (!player.getInventory().add(take)) {
                                    player.drop(take, false);
                                }
                            }
                            this.setChanged();
                        }
                        return ItemInteractionResult.sidedSuccess(level.isClientSide);
                    }
                }
            }
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    protected boolean isBaseIngredientPresent(ItemStack stack) {
        return isBaseIngredientPresent(stack2 -> CauldronPlatformHelper.itemMatches(stack, stack2), 1);
    }

    protected boolean isBaseIngredientPresent(Predicate<ItemStack> baseIngredientPredicate, int minCount) {
        int count = 0;
        for (ItemStack stack : this.outputItems) {
            if (baseIngredientPredicate.test(stack)) {
                count += stack.getCount();
                if (count >= minCount) {
                    return true;
                }
            }
        }
        return false;
    }

    protected void convertOutput(Predicate<ItemStack> itemToReplace, ItemStack outputItem, int maxCount) {
        int count = 0;
        for (int i = outputItems.size() - 1; i >= 0; i--) {
            var stack = outputItems.get(i);
            if (itemToReplace.test(stack)) {
                outputItems.set(i, outputItem.copy());
                count++;
                if (count >= maxCount) {
                    return;
                }
            }
        }
    }

    public boolean addToOutput(ItemStack itemStack) {
        for (int i = 0; i < outputItems.size(); i++) {
            var stack = outputItems.get(i);
            if (stack.isEmpty()) {
                outputItems.set(i, itemStack);
                return true;
            }
        }
        return false;
    }

    public void tryMeltInput(ItemStack itemStack) {
        if (level == null || level.isClientSide)
            return;
        /** shouldMelt is whether the input should be consumed*/
        boolean shouldMelt = false;
        /** success is whether the process yields a result*/
        boolean success = true;
        if (itemStack.is(ItemRegistry.SCROLL.get()) && isBaseIngredientPresent(CauldronPlatformHelper.IS_WATER, 1)) {
            if (Utils.random.nextFloat() < ServerConfigs.SCROLL_RECYCLE_CHANCE.get()) {
                ItemStack result = new ItemStack(getInkFromScroll(itemStack));
                convertOutput(CauldronPlatformHelper.IS_WATER, result, 1);
            } else {
                success = false;
            }
            shouldMelt = true;
        }
        if (!shouldMelt && isBrewable(itemStack)) {
            for (int i = 0; i < outputItems.size(); i++) {
                ItemStack potentialPotionBase = outputItems.get(i);
                if (!potentialPotionBase.isEmpty()) {
                    ItemStack output = CauldronPlatformHelper.getNonDestructiveBrewingResult(potentialPotionBase, itemStack, level);
                    if (!output.isEmpty()) {
                        outputItems.set(i, output.copy());
                        shouldMelt = true;
                    }
                }
            }
        }
        if (!shouldMelt && AlchemistCauldronRecipeRegistry.isValidIngredient(itemStack)) {
            for (int i = 0; i < outputItems.size(); i++) {
                ItemStack potentialPotionBase = outputItems.get(i).copy();
                if (!potentialPotionBase.isEmpty()) {
                    var recipe = AlchemistCauldronRecipeRegistry.getRecipeForInputs(potentialPotionBase, itemStack);
                    if (recipe != null && isBaseIngredientPresent(stack -> CauldronPlatformHelper.itemMatches(stack, potentialPotionBase), recipe.getInput().getCount())) {
                        //This given potion base has a recipe with our reagent, and we have enough of it to successfully craft the result
                        ItemStack result = recipe.getResult();
                        int toConsume = recipe.getInput().getCount();
                        convertOutput((stack) -> CauldronPlatformHelper.itemMatches(stack, potentialPotionBase.copy()), ItemStack.EMPTY, toConsume);
                        int c = result.getCount();
                        for (int j = 0; j < c; j++) {
                            addToOutput(result.split(1));
                        }
                        shouldMelt = true;
                        break;
                    }
                }
            }
        }
        if (shouldMelt) {
            itemStack.shrink(1);
            setChanged();
            if (success) {
                level.playSound(null, this.getBlockPos(), SoundEvents.BREWING_STAND_BREW, SoundSource.MASTER, 1, 1);
                level.markAndNotifyBlock(this.getBlockPos(), this.level.getChunkAt(this.getBlockPos()), this.getBlockState(), this.getBlockState(), 1, 1);
            } else {
                level.playSound(null, this.getBlockPos(), SoundEvents.GENERIC_EXTINGUISH_FIRE, SoundSource.MASTER, 1, 1);
            }
            collapseContainer(outputItems);
        }
    }

    public void collapseContainer(NonNullList<ItemStack> container) {
        for (int i = 0; i < container.size(); i++) {
            if (container.get(i).isEmpty()) {
                for (int j = i + 1; j < container.size(); j++) {
                    var stack = container.get(j);
                    if (!stack.isEmpty()) {
                        container.set(i, stack);
                        container.set(j, ItemStack.EMPTY);
                        break;
                    }
                }
            }
        }
    }

    /************************************************************
     Cauldron Helpers
     ***********************************************************/
    public boolean isValidInput(ItemStack itemStack) {
        return itemStack.is(ItemRegistry.SCROLL.get()) || isBrewable(itemStack) || AlchemistCauldronRecipeRegistry.isValidIngredient(itemStack);
    }

    public boolean isBrewable(ItemStack itemStack) {
        return ServerConfigs.ALLOW_CAULDRON_BREWING.get() && this.level != null && CauldronPlatformHelper.isBrewingIngredient(itemStack, this.level);
    }

    public int getItemWaterColor(ItemStack itemStack) {
        if (this.getLevel() == null)
            return 0;
        if (itemStack.getItem() instanceof SimpleElixir simpleElixir)
            return simpleElixir.getMobEffect().getEffect().value().getColor();
        if (itemStack.is(ItemRegistry.INK_COMMON.get()))
            return 0x222222;
        if (itemStack.is(ItemRegistry.INK_UNCOMMON.get()))
            return 0x124300;
        if (itemStack.is(ItemRegistry.INK_RARE.get()))
            return 0x0f3844;
        if (itemStack.is(ItemRegistry.INK_EPIC.get()))
            return 0xa52ea0;
        if (itemStack.is(ItemRegistry.INK_LEGENDARY.get()))
            return 0xfcaf1c;
        if (itemStack.is(ItemRegistry.BLOOD_VIAL.get()))
            return 0x5b0716;
        var potion = itemStack.get(DataComponents.POTION_CONTENTS);
        if (potion != null && !potion.is(Potions.WATER))
            return potion.getColor();
        return BiomeColors.getAverageWaterColor(this.getLevel(), this.getBlockPos());
    }

    public int getAverageWaterColor() {
        float f = 0.0F;
        float f1 = 0.0F;
        float f2 = 0.0F;

        int i = 0;
        for (ItemStack itemStack : outputItems) {
            if (!itemStack.isEmpty()) {
                int k = getItemWaterColor(itemStack);
                f += (float) ((k >> 16 & 255)) / 255.0F;
                f1 += (float) ((k >> 8 & 255)) / 255.0F;
                f2 += (float) ((k >> 0 & 255)) / 255.0F;
                i++;
            }
        }

        f = f / (float) i * 255.0F;
        f1 = f1 / (float) i * 255.0F;
        f2 = f2 / (float) i * 255.0F;
        return (int) f << 16 | (int) f1 << 8 | (int) f2;
    }

    public static Item getInkFromScroll(ItemStack scrollStack) {
        if (scrollStack.getItem() instanceof Scroll scroll) {
            var spellContainer = ISpellContainer.get(scrollStack);
            var spellData = spellContainer.getSpellAtIndex(0);

            SpellRarity rarity = spellData.getSpell().getRarity(spellData.getLevel());
            return InkItem.getInkForRarity(rarity);
        } else
            return Items.AIR;
    }

    /************************************************************
     Tile Entity Handling
     ***********************************************************/
    @Override
    public void setChanged() {
        super.setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public boolean stillValid(Player pPlayer) {
        return false;
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registryAccess) {
        Utils.loadAllItems(tag, this.inputItems, "Items", registryAccess);
        Utils.loadAllItems(tag, this.outputItems, "Results", registryAccess);
        super.loadAdditional(tag, registryAccess);
    }

    @Override
    protected void saveAdditional(@Nonnull CompoundTag tag, HolderLookup.Provider registryAccess) {
        Utils.saveAllItems(tag, this.inputItems, "Items", registryAccess);
        Utils.saveAllItems(tag, this.outputItems, "Results", registryAccess);
        super.saveAdditional(tag, registryAccess);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        var packet = ClientboundBlockEntityDataPacket.create(this);
        //irons_spellbooks.LOGGER.debug("getUpdatePacket: packet.getTag:{}", packet.getTag());
        return packet;
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider lookupProvider) {
        handleUpdateTag(pkt.getTag(), lookupProvider);
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider pRegistries) {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag, pRegistries);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider lookupProvider) {
        this.inputItems.clear();
        this.outputItems.clear();
        if (tag != null) {
            loadAdditional(tag, lookupProvider);
        }
    }

    public void drops() {
        SimpleContainer simpleContainer = new SimpleContainer(inputItems.size());
        for (int i = 0; i < inputItems.size(); i++) {
            simpleContainer.setItem(i, inputItems.get(i));
        }
        if (level != null)
            Containers.dropContents(this.level, this.worldPosition, simpleContainer);
    }

    protected static ItemStack waterBottle() {
        var stack = new ItemStack(Items.POTION);
        stack.set(DataComponents.POTION_CONTENTS, new PotionContents(Potions.WATER));
        return stack;
    }

    /************************************************************
     Interaction Map
     ***********************************************************/
    static Object2ObjectOpenHashMap<Item, AlchemistCauldronInteraction> newInteractionMap() {
        var map = Util.make(new Object2ObjectOpenHashMap<Item, AlchemistCauldronInteraction>(), (o2o) -> {
            o2o.defaultReturnValue((tile, blockState, level, pos, itemstack) -> null);
        });

        map.put(Items.WATER_BUCKET, (tile, blockState, level, pos, itemstack) -> {
            if (tile.outputItems.stream().anyMatch(ItemStack::isEmpty)) {
                level.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
                for (int i = 0; i < tile.outputItems.size(); i++) {
                    if (tile.outputItems.get(i).isEmpty()) {
                        tile.outputItems.set(i, waterBottle());
                    }
                }
                return new ItemStack(Items.BUCKET);
            } else {
                return null;
            }
        });
        map.put(Items.BUCKET, (tile, blockState, level, pos, itemstack) -> {
            if (tile.outputItems.stream().allMatch(CauldronPlatformHelper.IS_WATER)) {
                tile.outputItems.clear();
                level.playSound(null, pos, SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
                return new ItemStack(Items.WATER_BUCKET);
            }
            return null;
        });
        map.put(Items.GLASS_BOTTLE, (tile, blockState, level, pos, itemstack) -> {
            for (int i = tile.outputItems.size() - 1; i >= 0; i--) {
                var stack = tile.outputItems.get(i);
                if (!stack.isEmpty()) {
                    level.playSound(null, pos, (CauldronPlatformHelper.IS_WATER.test(stack) ? SoundEvents.BOTTLE_FILL : SoundEvents.BOTTLE_FILL_DRAGONBREATH), SoundSource.BLOCKS, 1.0F, 1.0F);
                    return stack.split(1);
                }
            }
            return null;
        });

        createBottleEmptyInteraction(map, () -> Items.POTION);

        createBottleEmptyInteraction(map, ItemRegistry.INK_COMMON);
        createBottleEmptyInteraction(map, ItemRegistry.INK_UNCOMMON);
        createBottleEmptyInteraction(map, ItemRegistry.INK_RARE);
        createBottleEmptyInteraction(map, ItemRegistry.INK_EPIC);
        createBottleEmptyInteraction(map, ItemRegistry.INK_LEGENDARY);

        createBottleEmptyInteraction(map, ItemRegistry.BLOOD_VIAL);

        createBottleEmptyInteraction(map, ItemRegistry.OAKSKIN_ELIXIR);
        createBottleEmptyInteraction(map, ItemRegistry.GREATER_OAKSKIN_ELIXIR);

        createBottleEmptyInteraction(map, ItemRegistry.EVASION_ELIXIR);
        createBottleEmptyInteraction(map, ItemRegistry.GREATER_EVASION_ELIXIR);

        createBottleEmptyInteraction(map, ItemRegistry.INVISIBILITY_ELIXIR);
        createBottleEmptyInteraction(map, ItemRegistry.GREATER_INVISIBILITY_ELIXIR);

        createBottleEmptyInteraction(map, ItemRegistry.GREATER_HEALING_POTION);

        NeoForge.EVENT_BUS.post(new AlchemistCauldronBuildInteractionsEvent(map));

        return map;
    }

    protected static void createBottleEmptyInteraction(Object2ObjectOpenHashMap<Item, AlchemistCauldronInteraction> map, Supplier<Item> item) {
        map.put(item.get(), (tile, blockState, level, pos, itemstack) -> {
            for (int i = 0; i < tile.outputItems.size(); i++) {
                var stack = tile.outputItems.get(i);
                if (stack.isEmpty()) {
                    var input = itemstack.copy();
                    input.setCount(1);
                    tile.outputItems.set(i, input);
                    level.playSound(null, pos, SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
                    return new ItemStack(Items.GLASS_BOTTLE);
                }
            }
            return null;
        });
    }

    /************************************************************
     Wordly Container Implementation
     ***********************************************************/
    @Override
    public int[] getSlotsForFace(Direction pSide) {
        //any side has access to all four input items
        return new int[]{0, 1, 2, 3};
    }

    @Override
    public boolean canPlaceItemThroughFace(int pIndex, ItemStack pItemStack, @Nullable Direction pDirection) {
        //any side can insert a valid item into any empty slot
        return inputItems.get(pIndex).isEmpty() && isValidInput(pItemStack);
    }

    @Override
    public boolean canTakeItemThroughFace(int pIndex, ItemStack pStack, Direction pDirection) {
        //cannot automatically withdraw items
        return false;
    }

    @Override
    public void clearContent() {
        inputItems.clear();
        outputItems.clear();
    }

    @Override
    public int getContainerSize() {
        return MAX_LEVELS;
    }

    @Override
    public boolean isEmpty() {
        return inputItems.stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    public ItemStack getItem(int pSlot) {
        return pSlot >= 0 && pSlot <= inputItems.size() ? inputItems.get(pSlot) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int pSlot, int pAmount) {
        return pSlot >= 0 && pSlot <= inputItems.size() ? inputItems.remove(pSlot) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItemNoUpdate(int pSlot) {
        return pSlot >= 0 && pSlot <= inputItems.size() ? inputItems.remove(pSlot) : ItemStack.EMPTY;
    }

    @Override
    public void setItem(int pSlot, ItemStack pStack) {
        if (pSlot >= 0 && pSlot <= inputItems.size()) {
            inputItems.set(pSlot, pStack);
        }
    }

    public boolean isBoiling(BlockState blockState) {
        return AlchemistCauldronBlock.isLit(blockState) && getLiquidLevel() >= 1;
    }

    public int getLiquidLevel() {
        return this.outputItems.stream().filter(itemstack -> !itemstack.isEmpty()).toList().size();
    }
}
