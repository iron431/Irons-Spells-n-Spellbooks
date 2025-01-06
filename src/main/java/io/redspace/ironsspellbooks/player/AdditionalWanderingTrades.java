package io.redspace.ironsspellbooks.player;

import io.redspace.ironsspellbooks.IronsSpellbooks;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.config.ServerConfigs;
import io.redspace.ironsspellbooks.item.InkItem;
import io.redspace.ironsspellbooks.item.Scroll;
import io.redspace.ironsspellbooks.loot.SpellFilter;
import io.redspace.ironsspellbooks.registries.ItemRegistry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.village.WandererTradesEvent;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;

@EventBusSubscriber
public class AdditionalWanderingTrades {

    //By default, wandering traders spawn with 5 random generic trades, and 1 random rare trade (6 trades total)
    public static final int INK_SALE_PRICE_PER_RARITY = 8;
    public static final int INK_BUY_PRICE_PER_RARITY = 5;

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void addWanderingTrades(WandererTradesEvent event) {
        if (ServerConfigs.SPEC.isLoaded() && !ServerConfigs.ADDITIONAL_WANDERING_TRADER_TRADES.get()) {
            return;
        }
        List<VillagerTrades.ItemListing> additionalGenericTrades = List.of(
                new RandomScrollTrade(new SpellFilter()),
                new RandomScrollTrade(new SpellFilter()),
                new RandomScrollTrade(new SpellFilter()),
                //Buy back ink
                new InkBuyTrade((InkItem) ItemRegistry.INK_COMMON.get()),
                new InkBuyTrade((InkItem) ItemRegistry.INK_UNCOMMON.get()),
                new InkBuyTrade((InkItem) ItemRegistry.INK_RARE.get()),
                new InkBuyTrade((InkItem) ItemRegistry.INK_EPIC.get()),
                new InkBuyTrade((InkItem) ItemRegistry.INK_LEGENDARY.get()),
                //Sell ink
                new InkSellTrade((InkItem) ItemRegistry.INK_COMMON.get()),
                new InkSellTrade((InkItem) ItemRegistry.INK_UNCOMMON.get()),
                new InkSellTrade((InkItem) ItemRegistry.INK_RARE.get()),
                new InkSellTrade((InkItem) ItemRegistry.INK_EPIC.get()),
                new InkSellTrade((InkItem) ItemRegistry.INK_LEGENDARY.get()),
                new RandomCurioTrade()
        );
        List<VillagerTrades.ItemListing> additionalRareTrades = List.of(
                SimpleTrade.of((trader, random) -> new MerchantOffer(
                        new ItemCost(Items.EMERALD, 64 - random.nextIntBetweenInclusive(1, 8)),
                        Optional.of(new ItemCost(Items.ECHO_SHARD, random.nextIntBetweenInclusive(1, 3))),
                        new ItemStack(ItemRegistry.LOST_KNOWLEDGE_FRAGMENT.get()),
                        8,
                        0,
                        .05f
                )),
                SimpleTrade.of((trader, random) -> new MerchantOffer(
                        new ItemCost(Items.EMERALD, 64),
                        Optional.of(new ItemCost(Items.EMERALD, random.nextIntBetweenInclusive(48, 64))),
                        new ItemStack(ItemRegistry.HITHER_THITHER_WAND.get()),
                        1,
                        0,
                        .05f
                )),
                //Add them multiple times to increase their likelihood of dropping.
                new RandomCurioTrade(),
                new RandomCurioTrade(),
                new RandomCurioTrade(),
                new ScrollPouchTrade(),
                new ScrollPouchTrade()
        );
        //rare but trades can be null, such as if all spells are disabled and he wants a scroll trade
        event.getGenericTrades().addAll(additionalGenericTrades.stream().filter(Objects::nonNull).toList());
        event.getRareTrades().addAll(additionalRareTrades.stream().filter(Objects::nonNull).toList());
    }

    public static class SimpleTrade implements VillagerTrades.ItemListing {
        final BiFunction<Entity, RandomSource, MerchantOffer> getOffer;

        protected SimpleTrade(BiFunction<Entity, RandomSource, MerchantOffer> getOffer) {
            this.getOffer = getOffer;
        }

        public static SimpleTrade of(BiFunction<Entity, RandomSource, MerchantOffer> getOffer) {
            return new SimpleTrade(getOffer);
        }

        public MerchantOffer getOffer(Entity pTrader, RandomSource pRandom) {
            return getOffer.apply(pTrader, pRandom);
        }
    }

    public static class SimpleBuy extends SimpleTrade {
        public SimpleBuy(int tradeCount, ItemCost buy, int minEmeralds, int maxEmeralds) {
            super((trader, random) -> {
                return new MerchantOffer(
                        buy,
                        new ItemStack(Items.EMERALD, random.nextIntBetweenInclusive(minEmeralds, maxEmeralds)),
                        tradeCount,
                        0,
                        .05f
                );
            });
        }
    }

    public static class SimpleSell extends SimpleTrade {
        public SimpleSell(int tradeCount, ItemStack sell, int minEmeralds, int maxEmeralds) {
            super((trader, random) -> {
                return new MerchantOffer(
                        new ItemCost(Items.EMERALD, random.nextIntBetweenInclusive(minEmeralds, maxEmeralds)),
                        sell,
                        tradeCount,
                        0,
                        .05f
                );
            });
        }
    }

    public static class InkBuyTrade extends SimpleTrade {
        public InkBuyTrade(InkItem item) {
            super((trader, random) -> {
                //There is a 50% chance that the trader will give essence instead of emeralds. they give half as many essences as emeralds
                boolean emeralds = random.nextBoolean();
                return new MerchantOffer(
                        new ItemCost(item, 1),
                        new ItemStack(emeralds ? Items.EMERALD : ItemRegistry.ARCANE_ESSENCE.get(), INK_BUY_PRICE_PER_RARITY * item.getRarity().getValue() / (emeralds ? 1 : 2) + random.nextIntBetweenInclusive(2, 3)),
                        8,
                        1,
                        .05f
                );
            });
        }
    }

    public static class InkSellTrade extends SimpleTrade {
        public InkSellTrade(InkItem item) {
            super((trader, random) -> {
                return new MerchantOffer(
                        new ItemCost(Items.EMERALD, INK_SALE_PRICE_PER_RARITY * item.getRarity().getValue() + random.nextIntBetweenInclusive(2, 3)),
                        new ItemStack(item),
                        4,
                        1,
                        .05f
                );
            });
        }
    }

    public static class ExilirBuyTrade extends SimpleTrade {
        public ExilirBuyTrade(boolean onlyLesser, boolean onlyGreater) {
            super((trader, random) -> {
                List<Item> lesser = List.of(ItemRegistry.EVASION_ELIXIR.get(), ItemRegistry.OAKSKIN_ELIXIR.get(), ItemRegistry.INVISIBILITY_ELIXIR.get());
                List<Item> greater = List.of(ItemRegistry.GREATER_EVASION_ELIXIR.get(), ItemRegistry.GREATER_OAKSKIN_ELIXIR.get(), ItemRegistry.GREATER_INVISIBILITY_ELIXIR.get(), ItemRegistry.GREATER_HEALING_POTION.get());
                Item item;
                boolean isGreater;
                if (onlyLesser) {
                    isGreater = false;
                } else if (onlyGreater) {
                    isGreater = true;
                } else {
                    isGreater = random.nextBoolean();
                }
                item = isGreater ? greater.get(random.nextInt(greater.size())) : lesser.get(random.nextInt(lesser.size()));
                return new MerchantOffer(
                        new ItemCost(item),
                        new ItemStack(Items.EMERALD, 6 + random.nextIntBetweenInclusive(3, 6) * (isGreater ? 2 : 1)),
                        6,
                        1,
                        .05f
                );
            });
        }
    }

    public static class ExilirSellTrade extends SimpleTrade {
        public ExilirSellTrade(boolean onlyLesser, boolean onlyGreater) {
            super((trader, random) -> {
                List<Item> lesser = List.of(ItemRegistry.EVASION_ELIXIR.get(), ItemRegistry.OAKSKIN_ELIXIR.get(), ItemRegistry.INVISIBILITY_ELIXIR.get());
                List<Item> greater = List.of(ItemRegistry.GREATER_EVASION_ELIXIR.get(), ItemRegistry.GREATER_OAKSKIN_ELIXIR.get(), ItemRegistry.GREATER_INVISIBILITY_ELIXIR.get(), ItemRegistry.GREATER_HEALING_POTION.get());
                Item item;
                boolean isGreater;
                if (onlyLesser) {
                    isGreater = false;
                } else if (onlyGreater) {
                    isGreater = true;
                } else {
                    isGreater = random.nextBoolean();
                }
                item = isGreater ? greater.get(random.nextInt(greater.size())) : lesser.get(random.nextInt(lesser.size()));
                return new MerchantOffer(
                        new ItemCost(Items.EMERALD, 10 + random.nextIntBetweenInclusive(4, 8) * (isGreater ? 2 : 1)),
                        new ItemStack(item),
                        3,
                        1,
                        .05f
                );
            });
        }
    }

    public static class PotionSellTrade extends SimpleTrade {
        public PotionSellTrade(@Nullable Potion potion) {
            super((trader, random) -> {
                var potion1 = potion;
                if (potion1 == null) {
                    var potions = BuiltInRegistries.POTION.stream().filter(p -> !p.getEffects().isEmpty()).toList();
                    if (!potions.isEmpty()) {
                        potion1 = potions.get(random.nextInt(potions.size()));
                    }
                }
                if (potion1 == null) {
                    //fallback for registry failure
                    potion1 = Potions.AWKWARD.value();
                }
                int amplifier = 0;
                int duration = 0;
                var effects = potion1.getEffects();
                if (!effects.isEmpty()) {
                    var effect = effects.getFirst();
                    amplifier = effect.getAmplifier();
                    duration = effect.getDuration() / (20 * 60); //1 emerald per minute of effect
                }
                var potionStack = new ItemStack(Items.POTION);
                potionStack.set(DataComponents.POTION_CONTENTS, new PotionContents(BuiltInRegistries.POTION.wrapAsHolder(potion1)));
                return new MerchantOffer(
                        new ItemCost(Items.EMERALD, random.nextIntBetweenInclusive(12, 16) + random.nextIntBetweenInclusive(4, 6) * amplifier + duration),
                        potionStack,
                        3,
                        1,
                        .05f
                );
            });
        }
    }

    static class RandomCurioTrade extends SimpleTrade {

        private RandomCurioTrade() {
            super((trader, random) -> {
                if (!trader.level.isClientSide) {
                    LootTable loottable = trader.level.getServer().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE, IronsSpellbooks.id("magic_items/basic_curios")));
                    var context = new LootParams.Builder((ServerLevel) trader.level).create(LootContextParamSets.EMPTY);
                    var items = loottable.getRandomItems(context);
                    if (!items.isEmpty()) {
                        ItemStack forSale = items.get(0);
                        var cost = new ItemCost(Items.EMERALD, random.nextIntBetweenInclusive(14, 25));
                        return new MerchantOffer(cost, forSale, 1, 5, 0.5f);
                    }
                }
                return null;
            });
        }
    }

    static class ScrollPouchTrade extends SimpleTrade {
        private ScrollPouchTrade() {
            super((trader, random) -> {
                if (!trader.level.isClientSide) {
                    LootTable loottable = trader.level.getServer().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE, IronsSpellbooks.id("magic_items/scroll_pouch")));
                    var context = new LootParams.Builder((ServerLevel) trader.level).create(LootContextParamSets.EMPTY);
                    var items = loottable.getRandomItems(context);
                    if (!items.isEmpty()) {
                        int quality = 0;
                        for (ItemStack stack : items) {
                            if (stack.getItem() instanceof Scroll) {
                                quality += ISpellContainer.get(stack).getSpellAtIndex(0).getRarity().getValue() + 1;
                            }
                        }
                        ItemStack forSale = new ItemStack(Items.BUNDLE);
                        forSale.set(DataComponents.ITEM_NAME, Component.translatable("item.irons_spellbooks.scroll_pouch"));
                        forSale.set(DataComponents.BUNDLE_CONTENTS, new BundleContents(items));
                        ItemCost cost = new ItemCost(Items.EMERALD, quality * 4 + random.nextIntBetweenInclusive(8, 16));
                        return new MerchantOffer(cost, forSale, 1, 5, 0.5f);
                    }
                }
                return null;
            });
        }
    }

    public static class RandomScrollTrade implements VillagerTrades.ItemListing {
        protected final Optional<ItemCost> price2;
        protected final ItemStack forSale;
        protected final int maxTrades;
        protected final int xp;
        protected final float priceMult;
        protected final SpellFilter spellFilter;
        protected float minQuality, maxQuality;

        public RandomScrollTrade(SpellFilter spellFilter) {
            this.spellFilter = spellFilter;
            this.price2 = Optional.empty();
            this.forSale = new ItemStack(ItemRegistry.SCROLL.get());
            this.maxTrades = 1;
            this.xp = 5;
            this.priceMult = .05f;
            this.minQuality = 0f;
            this.maxQuality = 1f;
        }

        public RandomScrollTrade(SpellFilter filter, float minQuality, float maxQuality) {
            this(filter);
            this.minQuality = minQuality;
            this.maxQuality = maxQuality;
        }

        @Nullable
        @Override
        public MerchantOffer getOffer(Entity pTrader, RandomSource random) {
            AbstractSpell spell = spellFilter.getRandomSpell(random);
            if (spell == SpellRegistry.none()) {
                return null;
            }
            int level = random.nextIntBetweenInclusive(1 + (int) (spell.getMaxLevel() * minQuality), (int) ((spell.getMaxLevel() - 1) * maxQuality) + 1);
            ISpellContainer.createScrollContainer(spell, level, forSale);
            var price = new ItemCost(Items.EMERALD, spell.getRarity(level).getValue() * 5 + random.nextIntBetweenInclusive(4, 7) + level);
            return new MerchantOffer(price, price2, forSale, maxTrades, xp, priceMult);
        }
    }
}
