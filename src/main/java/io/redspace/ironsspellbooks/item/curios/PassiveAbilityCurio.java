package io.redspace.ironsspellbooks.item.curios;

import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.util.MinecraftInstanceHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static io.redspace.ironsspellbooks.api.registry.AttributeRegistry.COOLDOWN_REDUCTION;

public abstract class PassiveAbilityCurio extends SimpleDescriptiveCurio {
    public PassiveAbilityCurio(Properties properties, String slotIdentifier) {
        super(properties, slotIdentifier);
    }

    protected abstract int getCooldownTicks();

    /**
     * If the curio is off cooldown, return true and trigger cooldown. Otherwise, return false
     */
    public boolean tryProcCooldown(Player player) {
        if (player.getCooldowns().isOnCooldown(this)) {
            return false;
        } else {
            player.getCooldowns().addCooldown(this, getCooldownTicks(player));
            return true;
        }
    }

    public int getCooldownTicks(@Nullable LivingEntity livingEntity) {
        double playerCooldownModifier = livingEntity == null ? 1 : livingEntity.getAttributeValue(COOLDOWN_REDUCTION);
        return (int) (getCooldownTicks() * (2 - Utils.softCapFormula(playerCooldownModifier)));
    }

    @Override
    public List<Component> getDescriptionLines(ItemStack stack) {

        return List.of(
                Component.translatable(
                        "tooltip.irons_spellbooks.passive_ability",
                        Component.literal(Utils.timeFromTicks(getCooldownTicks(MinecraftInstanceHelper.getPlayer()), 1)).withStyle(ChatFormatting.AQUA)
                ).withStyle(ChatFormatting.GREEN),
                getDescription(stack)
        );
    }
}
