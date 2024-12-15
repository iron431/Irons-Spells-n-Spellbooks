package io.redspace.ironsspellbooks.entity.mobs;

import io.redspace.ironsspellbooks.network.SyncAnimationPacket;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.PacketDistributor;

public interface IAnimatedAttacker {
    void playAnimation(String animationId);

    default <T extends Entity & IAnimatedAttacker> void serverTriggerAnimation(String animationId) {
        PacketDistributor.sendToPlayersTrackingEntity((T) this, new SyncAnimationPacket<>(animationId, (T) this));
    }
}
