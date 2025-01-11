package io.redspace.ironsspellbooks.item.curios;

import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.compat.Curios;
import io.redspace.ironsspellbooks.registries.ItemRegistry;
import io.redspace.ironsspellbooks.render.CinderousRarity;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

@EventBusSubscriber
public class BetrayerSignetRingItem extends PassiveAbilityCurio {
    public static final int COOLDOWN_IN_TICKS = 5 * 20;

    public BetrayerSignetRingItem() {
        super(new Properties().stacksTo(1).rarity(CinderousRarity.CINDEROUS_RARITY_PROXY.getValue()).fireResistant(), Curios.RING_SLOT);
        this.showHeader = false; // prevent generative header since we have attributes
    }

    @Override
    protected int getCooldownTicks() {
        return COOLDOWN_IN_TICKS;
    }

    @SubscribeEvent
    public static void handleAbility(LivingIncomingDamageEvent event) {
        var RING = ((BetrayerSignetRingItem) ItemRegistry.SIGNET_OF_THE_BETRAYER.get());
        if (event.getSource().getEntity() instanceof ServerPlayer attackingPlayer) {
            if (RING.isEquippedBy(attackingPlayer)) {
                var victim = event.getEntity();
                var victimMaxMana = victim.getAttributeValue(AttributeRegistry.MAX_MANA);
                var victimBaseMana = victim.getAttributeBaseValue(AttributeRegistry.MAX_MANA);
                if (victimMaxMana > victimBaseMana && RING.tryProcCooldown(attackingPlayer)) {
                    /*
                    If a victim's mana is above the base mana, they are considering a mage
                    Therefore, we do extra damage based on the following (informal) formula:
                        - Σ(10 - n)% for n steps of 100 mana above base
                        - ie, 10% for the first 100, 9% for the second 100, 8% for the third 100, etc
                     */
                    var manaAboveBase = victimMaxMana - victimBaseMana;
                    double conversionRatioPer100 = 0.10;
                    double totalExtraDamagePercent = 0;
                    while (manaAboveBase > 0 && conversionRatioPer100 > 0) {
                        var step = Math.clamp(manaAboveBase, 0, 100) * .01;
                        totalExtraDamagePercent += step * conversionRatioPer100;
                        manaAboveBase -= 100;
                        conversionRatioPer100 -= 0.01;
                    }
                    event.setAmount((float) (event.getAmount() * totalExtraDamagePercent));
                }
            }
        }
    }
}
