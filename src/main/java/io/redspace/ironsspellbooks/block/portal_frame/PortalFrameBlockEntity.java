package io.redspace.ironsspellbooks.block.portal_frame;

import io.redspace.ironsspellbooks.IronsSpellbooks;
import io.redspace.ironsspellbooks.capabilities.magic.PortalManager;
import io.redspace.ironsspellbooks.entity.spells.portal.PortalData;
import io.redspace.ironsspellbooks.registries.BlockRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.RelativeMovement;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

public class PortalFrameBlockEntity extends BlockEntity {
    private PortalId portalId;
    @Nullable
    //private PortalData portalData;
    boolean clientIsConnected;

    public PortalFrameBlockEntity(BlockPos pWorldPosition, BlockState pBlockState) {
        this(BlockRegistry.PORTAL_FRAME_BLOCK_ENTITY.get(), pWorldPosition, pBlockState);
    }

    public PortalFrameBlockEntity(BlockEntityType<?> pType, BlockPos pPos, BlockState pBlockState) {
        super(pType, pPos, pBlockState);
        if (isPrimary(pBlockState)) {
            this.portalId = new PortalId(Optional.of(UUID.randomUUID()));
        } else {
            this.portalId = new PortalId(Optional.empty());
        }
    }

    public static boolean isPrimary(BlockState blockState) {
        return blockState.getValue(PortalFrameBlock.HALF).equals(DoubleBlockHalf.LOWER);
    }


    private void ifNeighborPresent(Consumer<PortalFrameBlockEntity> consumer) {
        if (level != null) {
            var e = level.getBlockEntity(this.getBlockPos().relative(this.getBlockState().getValue(PortalFrameBlock.HALF).getDirectionToOther()));
            if (e instanceof PortalFrameBlockEntity portalFrameBlockEntity) {
                consumer.accept(portalFrameBlockEntity);
            }
        }
    }

    public boolean isPortalConnected() {
        return getPortalData() != null;
    }

    public void breakPortalConnection() {
        var portalData = this.getPortalData();
        if (portalData != null) {
            PortalManager.INSTANCE.removePortalData(portalData.portalEntityId1);
            PortalManager.INSTANCE.removePortalData(portalData.portalEntityId2);
            var server = this.level == null ? null : this.level.getServer();
            if (server != null) {
                boolean primary = this.getUUID().equals(portalData.portalEntityId1);
                var otherPos = primary ? portalData.globalPos2 : portalData.globalPos1;
                var dimension = server.getLevel(otherPos.dimension());
                var otherBlockPos = BlockPos.containing(otherPos.pos());
                if (dimension != null && dimension.isLoaded(otherBlockPos)) {
                    if (dimension.getBlockEntity(otherBlockPos) instanceof PortalFrameBlockEntity portalFrame) {
                        portalFrame.setChanged();
                    }
                }
            }
            this.setChanged();
        }
    }

    private @Nullable PortalData getPortalData() {
        return PortalManager.INSTANCE.getPortalData(this.portalId.uuid(this));
    }

    public Vec3 getPortalLocation() {
        if (isPrimary(this.getBlockState())) {
            return this.getBlockPos().getBottomCenter();
        } else {
            return this.getBlockPos().getBottomCenter().subtract(0, 1, 0);
        }
    }

    public void teleport(Entity entity) {
        if (entity.level instanceof ServerLevel serverLevel) {
            var uuid = this.getUUID();
            PortalManager.INSTANCE.processDelayCooldown(uuid, entity.getUUID(), 1);
            IronsSpellbooks.LOGGER.debug("PortalFrame.teleport: {}: {}", this.getUUID(), PortalManager.INSTANCE.getPortalData(uuid));
            if (PortalManager.INSTANCE.canUsePortal(uuid, entity)) {
                var portalData = PortalManager.INSTANCE.getPortalData(uuid);
                PortalManager.INSTANCE.addPortalCooldown(entity, uuid);
                //PortalManager.INSTANCE.addPortalCooldown(entity, portalData.portalEntityId2);
                portalData.getConnectedPortalPos(uuid).ifPresent(portalPos -> {
                    Vec3 destination = portalPos.pos();
                    serverLevel.playSound(null, this.getBlockPos(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.BLOCKS, 1f, 1f);
                    if (serverLevel.dimension().equals(portalPos.dimension())) {
                        entity.teleportTo(serverLevel, destination.x, destination.y, destination.z, RelativeMovement.ROTATION, portalPos.rotation(), entity.getXRot());
                    } else {
                        var server = serverLevel.getServer();
                        var dim = server.getLevel(portalPos.dimension());
                        if (dim != null) {
                            entity.changeDimension(new DimensionTransition(dim, destination, Vec3.ZERO, entity.getYRot(), entity.getXRot(), DimensionTransition.DO_NOTHING));
                        }
                    }
                    serverLevel.playSound(null, destination.x, destination.y, destination.z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.BLOCKS, 1f, 1f);
                });
            }
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider pRegistries) {
        super.loadAdditional(tag, pRegistries);
        if (tag.contains("uuid")) {
            var uuid = tag.getUUID("uuid");
            this.portalId = new PortalId(Optional.of(uuid));
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider pRegistries) {
        super.saveAdditional(tag, pRegistries);
        if (isPrimary(this.getBlockState())) {
            var uuid = getUUID();
            if (uuid != null) {
                tag.putUUID("uuid", uuid);
            }
        }
    }

    public UUID getUUID() {
        return this.portalId.uuid(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider pRegistries) {
        var tag = super.getUpdateTag(pRegistries);
        tag.putBoolean("connected", this.isPortalConnected());
        return tag;
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
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider lookupProvider) {
        super.handleUpdateTag(tag, lookupProvider);
        this.clientIsConnected = tag.getBoolean("connected");
    }

    @Override
    public void setChanged() {
        super.setChanged();
        if (isPrimary(this.getBlockState())) {
            if (level != null) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
            }
        } else {
            ifNeighborPresent(PortalFrameBlockEntity::setChanged);
        }
    }

    private boolean active;
    private int activeCooldown;

    public static void serverTick(Level level, BlockPos pos, BlockState blockState, PortalFrameBlockEntity portalFrameBlockEntity) {
        if (level.getGameTime() % 5 == 0) {
//            IronsSpellbooks.LOGGER.debug("portalFrame server tick: {}:\n{}", portalFrameBlockEntity.getUUID(), PortalManager.INSTANCE.cooldownLookup.get(portalFrameBlockEntity.getUUID()));
            PortalManager.INSTANCE.processCooldownTick(portalFrameBlockEntity.getUUID(), -5);
        }
        if (portalFrameBlockEntity.active) {
            portalFrameBlockEntity.active = --portalFrameBlockEntity.activeCooldown > 0;
            portalFrameBlockEntity.level.getEntities(null, blockState.getShape(level, pos).bounds().move(pos)).forEach(entity -> portalFrameBlockEntity.teleport(entity));
        }
    }

    public void setActive() {
        this.active = true;
        activeCooldown = 10;
    }

    record PortalId(Optional<UUID> _uuid) {
        UUID uuid(PortalFrameBlockEntity portalFrameBlockEntity) {
            return _uuid.orElse(
                    portalFrameBlockEntity.level.isLoaded(portalFrameBlockEntity.getBlockPos().below()) && portalFrameBlockEntity.level.getBlockEntity(portalFrameBlockEntity.getBlockPos().below()) instanceof PortalFrameBlockEntity be ? be.getUUID() : null);
        }
    }
}
