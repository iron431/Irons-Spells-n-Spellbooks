package io.redspace.ironsspellbooks.item;

import io.redspace.ironsspellbooks.entity.mobs.wizards.fire_boss.FireBossEntity;
import io.redspace.ironsspellbooks.registries.EntityRegistry;
import io.redspace.ironsspellbooks.registries.ItemRegistry;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class CinderousSoulcallerItem extends Item {

    public CinderousSoulcallerItem(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (level instanceof ServerLevel serverlevel) {
            player.getCooldowns().addCooldown(ItemRegistry.CINDEROUS_SOULCALLER.get(), 50);
            ItemStack itemStack = player.getItemInHand(hand);
            FireBossEntity fireBoss = EntityRegistry.FIRE_BOSS.get().create(serverlevel);
            fireBoss.moveTo(player.position().add(0,0.1,0));
            fireBoss.triggerSpawnAnim();
            fireBoss.finalizeSpawn(serverlevel,level.getCurrentDifficultyAt(player.blockPosition()), MobSpawnType.MOB_SUMMONED, null);

            level.addFreshEntity(fireBoss);
        }
        return super.use(level, player, hand);
    }
}
