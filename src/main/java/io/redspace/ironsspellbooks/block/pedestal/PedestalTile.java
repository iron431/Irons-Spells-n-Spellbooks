package io.redspace.ironsspellbooks.block.pedestal;

import io.redspace.ironsspellbooks.registries.BlockRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nonnull;

public class PedestalTile extends BlockEntity {
    private static final String NBT_HELD_ITEM = "heldItem";

    private ItemStack heldItem = ItemStack.EMPTY;

    public PedestalTile(BlockPos pWorldPosition, BlockState pBlockState) {
        super(BlockRegistry.PEDESTAL_TILE.get(), pWorldPosition, pBlockState);
    }

    public ItemStack getHeldItem() {
        return heldItem;
    }

    public void setHeldItem(ItemStack newItem) {
        heldItem = newItem;
        setChanged();
    }

    public void drops() {
        SimpleContainer simpleContainer = new SimpleContainer(heldItem);
        Containers.dropContents(this.level, this.worldPosition, simpleContainer);
    }

    @Override
    protected void loadAdditional(CompoundTag pTag, HolderLookup.Provider pRegistries) {
        super.loadAdditional(pTag, pRegistries);
        readNBT(pTag, pRegistries);
    }

    @Override
    protected void saveAdditional(@Nonnull CompoundTag tag, HolderLookup.Provider registryAccess) {
        writeNBT(tag, registryAccess);
    }

    @Override
    public void setChanged() {
        super.setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider pRegistries) {
        return saveWithoutMetadata(pRegistries);
    }

    private CompoundTag writeNBT(CompoundTag nbt, HolderLookup.Provider pRegistries) {
        if (!heldItem.isEmpty()) {
            nbt.put(NBT_HELD_ITEM, heldItem.save(pRegistries));
        } else {
            nbt.put(NBT_HELD_ITEM, new CompoundTag());
        }
        return nbt;
    }

    private CompoundTag readNBT(CompoundTag nbt, HolderLookup.Provider pRegistries) {
        if (nbt.contains(NBT_HELD_ITEM)) {
            heldItem = ItemStack.parseOptional(pRegistries, nbt.getCompound(NBT_HELD_ITEM));
        }
        return nbt;
    }
}