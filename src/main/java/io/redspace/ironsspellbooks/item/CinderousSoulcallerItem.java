package io.redspace.ironsspellbooks.item;

import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import io.redspace.ironsspellbooks.entity.mobs.wizards.fire_boss.FireBossEntity;
import io.redspace.ironsspellbooks.particle.BlastwaveParticleOptions;
import io.redspace.ironsspellbooks.registries.EntityRegistry;
import io.redspace.ironsspellbooks.registries.ItemRegistry;
import io.redspace.ironsspellbooks.registries.PoiTypeRegistry;
import io.redspace.ironsspellbooks.registries.SoundRegistry;
import io.redspace.ironsspellbooks.util.ParticleHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Objects;

public class CinderousSoulcallerItem extends Item {

    public CinderousSoulcallerItem(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (level instanceof ServerLevel serverlevel && player instanceof ServerPlayer serverPlayer) {
            player.getCooldowns().addCooldown(ItemRegistry.CINDEROUS_SOULCALLER.get(), 80);
            ItemStack itemStack = player.getItemInHand(hand);

            BlockPos playerBlockPos = player.blockPosition();
            PoiManager poimanager = serverlevel.getPoiManager();
            // arena radius is 23. slightly shorter distance means player must approach center/keystone
            var keystone = poimanager.findClosest(poi -> Objects.equals(poi.getKey(), PoiTypeRegistry.FIRE_BOSS_KEYSTONE.getKey()), playerBlockPos, 18, PoiManager.Occupancy.ANY);
            if (keystone.isPresent()) {
                BlockPos keystonePos = keystone.get();
                AABB exclusiveRange = AABB.ofSize(keystonePos.getCenter(), 80, 80, 80);
                if (level.getEntitiesOfClass(FireBossEntity.class, exclusiveRange).isEmpty()) {
                    //success, consume and summon
                    if (!player.getAbilities().instabuild) {
                        Vec3 particlePos = player.getEyePosition().add(player.getForward().scale(0.6)).subtract(0, 0.3, 0);
                        MagicManager.spawnParticles(serverlevel, new ItemParticleOption(ParticleTypes.ITEM, itemStack), particlePos.x, particlePos.y, particlePos.z, 9, .15, .15, .15, 0.08, false);
                        itemStack.shrink(1);
                        player.setItemInHand(hand, itemStack);
                    }
                    Vec3 center = keystonePos.getCenter().add(0, 0.6, 0);
                    float yRot = Utils.getAngle(center.x, center.z, player.getX(), player.getZ()) * Mth.RAD_TO_DEG;
                    FireBossEntity fireBoss = EntityRegistry.FIRE_BOSS.get().create(serverlevel);
                    fireBoss.moveTo(center);
                    fireBoss.setYRot(yRot + 90);
                    fireBoss.triggerSpawnAnim();
                    fireBoss.finalizeSpawn(serverlevel, level.getCurrentDifficultyAt(player.blockPosition()), MobSpawnType.MOB_SUMMONED, null);
                    level.addFreshEntity(fireBoss);
                    tollEffects(serverlevel, player.position(), true);
                } else {
                    //failure, boss already exists
                    serverPlayer.connection.send(new ClientboundSetActionBarTextPacket(Component.translatable("ui.irons_spellbooks.cinderous_soulcaller.failure.already_exists").withStyle(ChatFormatting.GOLD)));
                    tollEffects(serverlevel, player.position(), false);
                }
            } else {
                //failure, no soul present
                serverPlayer.connection.send(new ClientboundSetActionBarTextPacket(Component.translatable("ui.irons_spellbooks.cinderous_soulcaller.failure.no_soul").withStyle(ChatFormatting.GOLD)));
                tollEffects(serverlevel, player.position(), false);
            }


        }
        return super.use(level, player, hand);
    }

    public void tollEffects(ServerLevel serverLevel, Vec3 usePosition, boolean success) {
        serverLevel.playSound(null, usePosition.x, usePosition.y, usePosition.z, success ? SoundRegistry.SOULCALLER_TOLL_SUCCESS : SoundRegistry.SOULCALLER_TOLL_FAILURE, SoundSource.PLAYERS, 6, 1f);
        MagicManager.spawnParticles(serverLevel, new BlastwaveParticleOptions(1, .6f, 0.3f, 16), usePosition.x, usePosition.y, usePosition.z, 0, 0, 0, 0, 0, false);
    }
}
