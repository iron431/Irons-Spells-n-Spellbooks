package io.redspace.ironsspellbooks.entity.mobs.goals;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;

import javax.annotation.Nullable;

public interface HomeOwner {

    @Nullable
    BlockPos getHome();

    void setHome(BlockPos homePos);

    //fixme: self reference... (remove)
    default void serializeHome(HomeOwner self, CompoundTag tag) {
        if (self.getHome() != null)
            tag.putIntArray("HomePos", new int[]{getHome().getX(), getHome().getY(), getHome().getZ()});
    }

    default void deserializeHome(HomeOwner self, CompoundTag tag) {
        if (tag.contains("HomePos")) {
            var home = tag.getIntArray("HomePos");
            self.setHome(new BlockPos(home[0], home[1], home[2]));
        }
    }
}
