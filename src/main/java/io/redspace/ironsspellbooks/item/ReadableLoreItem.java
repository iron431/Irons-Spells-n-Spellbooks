package io.redspace.ironsspellbooks.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundOpenBookPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.WrittenBookItem;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;

public class ReadableLoreItem extends Item implements ILecternPlaceable {
    private final ResourceLocation lecternLocation;

    public ReadableLoreItem(ResourceLocation lecternLocation, Properties pProperties) {
        super(pProperties);
        this.lecternLocation = lecternLocation;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level pLevel, Player pPlayer, InteractionHand pHand) {
        var itemstack = pPlayer.getItemInHand(pHand);
        if (pPlayer instanceof ServerPlayer serverPlayer) {
            if (WrittenBookItem.resolveBookComponents(itemstack, serverPlayer.createCommandSourceStack(), serverPlayer)) {
                // not sure why vanilla broadcasts book resolution changes, but i will too
                serverPlayer.containerMenu.broadcastChanges();
            }

            serverPlayer.connection.send(new ClientboundOpenBookPacket(pHand));
        }
        return InteractionResultHolder.sidedSuccess(itemstack, pLevel.isClientSide());
    }

    @Override
    public List<Component> getPages(ItemStack stack) {
        WrittenBookContent writtenbookcontent = stack.get(DataComponents.WRITTEN_BOOK_CONTENT);
        if (writtenbookcontent != null) {
            return writtenbookcontent.getPages(false);
        }
        return List.of();
    }

    @Override
    public Optional<ResourceLocation> simpleTextureOverride(ItemStack stack) {
        return Optional.of(lecternLocation);
    }
}
