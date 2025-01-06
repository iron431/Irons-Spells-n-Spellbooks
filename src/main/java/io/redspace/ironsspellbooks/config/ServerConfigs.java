package io.redspace.ironsspellbooks.config;

import io.redspace.ironsspellbooks.IronsSpellbooks;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class ServerConfigs {

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec SPEC;
    public static final SpellConfigParameters DEFAULT_CONFIG = new SpellConfigParameters(null, () -> true, SchoolRegistry.EVOCATION_RESOURCE::toString, () -> 10, () -> SpellRarity.COMMON, () -> 1d, () -> 1d, () -> 10d, () -> true);
    public static final ModConfigSpec.ConfigValue<Boolean> SWORDS_CONSUME_MANA;
    public static final ModConfigSpec.ConfigValue<Double> SWORDS_CD_MULTIPLIER;
    public static final ModConfigSpec.ConfigValue<Boolean> CAN_ATTACK_OWN_SUMMONS;
    public static final ModConfigSpec.ConfigValue<Integer> MAX_UPGRADES;
    public static final ModConfigSpec.ConfigValue<Double> MANA_SPAWN_PERCENT;
    public static final ModConfigSpec.ConfigValue<Double> SCROLL_RECYCLE_CHANCE;
    public static final ModConfigSpec.ConfigValue<Boolean> SCROLL_MERGING;
    private static final ModConfigSpec.ConfigValue<List<? extends String>> UPGRADE_WHITELIST;
    private static final ModConfigSpec.ConfigValue<List<? extends String>> UPGRADE_BLACKLIST;
    private static final ModConfigSpec.ConfigValue<List<? extends String>> IMBUE_WHITELIST;
    private static final ModConfigSpec.ConfigValue<List<? extends String>> IMBUE_BLACKLIST;
    public static final ModConfigSpec.ConfigValue<Integer> PRIEST_TOWER_SPAWNRATE;
    public static final ModConfigSpec.ConfigValue<Boolean> ALLOW_CAULDRON_BREWING;
    public static final ModConfigSpec.ConfigValue<Boolean> FURLED_MAPS_SKIP_CHUNKS;
    public static final ModConfigSpec.ConfigValue<Boolean> APPLY_ALL_MULTIHAND_ATTRIBUTES;
    public static final ModConfigSpec.ConfigValue<Boolean> BETTER_CREEPER_THUNDERHIT;
    public static final ModConfigSpec.ConfigValue<Boolean> SPELL_GREIFING;
    public static final ModConfigSpec.ConfigValue<Boolean> ADDITIONAL_WANDERING_TRADER_TRADES;
    public static final ModConfigSpec.ConfigValue<Boolean> DISABLE_ADVENTURE_MODE_CASTING;
    public static final ModConfigSpec.ConfigValue<Boolean> HOGLIN_OFFSPRING_PROTECTION;
    public static final ModConfigSpec.ConfigValue<Double> MANA_REGEN_MULTIPLIER;

    //public static final ModConfigSpec.ConfigValue<String[]> UPGRADE_BLACKLIST;

    public static final ModConfigSpec.ConfigValue<List<? extends Double>> RARITY_CONFIG;
    public static final Set<Item> UPGRADE_WHITELIST_ITEMS = new HashSet<>();
    public static final Set<Item> UPGRADE_BLACKLIST_ITEMS = new HashSet<>();
    public static final Set<Item> IMBUE_WHITELIST_ITEMS = new HashSet<>();
    public static final Set<Item> IMBUE_BLACKLIST_ITEMS = new HashSet<>();

    //https://forge.gemwire.uk/wiki/Configs

    private static final Map<String, SpellConfigParameters> SPELL_CONFIGS = new HashMap<>();

    static {
        BUILDER.comment("Other Configuration");
        BUILDER.push("Misc");

        RARITY_CONFIG = BUILDER.worldRestart()
                .comment(String.format("rarityConfig array values must sum to 1: [%s, %s, %s, %s, %s]. Default: [.3d, .25d, .2d, .15d, .1d]", SpellRarity.COMMON, SpellRarity.UNCOMMON, SpellRarity.RARE, SpellRarity.EPIC, SpellRarity.LEGENDARY))
                .defineList("rarityConfig", List.of(.3d, .25d, .2d, .15d, .1d), x -> true);

        BUILDER.comment("Whether or not imbued weapons require mana to be casted. Default: true");
        SWORDS_CONSUME_MANA = BUILDER.worldRestart().define("swordsConsumeMana", true);
        BUILDER.comment("The multiplier on the cooldown of imbued weapons. Default: 0.5 (50% of default cooldown)");
        SWORDS_CD_MULTIPLIER = BUILDER.worldRestart().define("swordsCooldownMultiplier", .5);
        BUILDER.comment("Whether or not players can harm their own magic summons. Default: false");
        CAN_ATTACK_OWN_SUMMONS = BUILDER.worldRestart().define("canAttackOwnSummons", false);
        BUILDER.comment("The maximum amount of times an applicable piece of equipment can be upgraded in the arcane anvil. Default: 3");
        MAX_UPGRADES = BUILDER.worldRestart().define("maxUpgrades", 3);
        BUILDER.comment("From 0-1, the percent of max mana a player respawns with. Default: 0.0");
        MANA_SPAWN_PERCENT = BUILDER.worldRestart().define("manaSpawnPercent", 0.0);
        BUILDER.comment("From 0-1, the percent chance for scrolls to be successfully recycled. Default: 0.5 (50%)");
        SCROLL_RECYCLE_CHANCE = BUILDER.worldRestart().define("scrollRecycleChance", 0.5);
        BUILDER.comment("Whether or not potions should be allowed to be brewed in the alchemist cauldron)");
        ALLOW_CAULDRON_BREWING = BUILDER.worldRestart().define("allowCauldronBrewing", true);
        BUILDER.comment("Whether or not Furled Map items should skip chunks when searching for structures (only find new structures). Can impact performance while searching. Default: true");
        FURLED_MAPS_SKIP_CHUNKS = BUILDER.worldRestart().define("furledMapSkipsExistingChunks", true);
        BUILDER.comment("Whether or not casting items should apply all attributes while in the offhand, or just magic related ones. Default: true");
        APPLY_ALL_MULTIHAND_ATTRIBUTES = BUILDER.worldRestart().define("applyAllMultihandAttributes", true);
        BUILDER.comment("Whether or not creepers should be healed and become fire immune when struck by lightning. Default: true");
        BETTER_CREEPER_THUNDERHIT = BUILDER.worldRestart().define("betterCreeperThunderHit", true);
        BUILDER.comment("Whether or not spells such as Fireball or Fire Breath should destroy terrain or create fire. Default: false");
        SPELL_GREIFING = BUILDER.worldRestart().define("spellGriefing", false);
        BUILDER.comment("Whether or not the wandering trader can have magic related trades, such as ink or scrolls. Default: true");
        ADDITIONAL_WANDERING_TRADER_TRADES = BUILDER.worldRestart().define("additionalWanderingTraderTrades", true);
        BUILDER.comment("Whether casting spells should be disabled in adventure mode. Default: false");
        DISABLE_ADVENTURE_MODE_CASTING = BUILDER.worldRestart().define("disableAdventureModeCasting", false);
        BUILDER.comment("Whether hoglins have the ability to pass overworld zombification immunity to their offspring. Default: true");
        HOGLIN_OFFSPRING_PROTECTION = BUILDER.worldRestart().define("hoglinOffspringProtection", true);
        BUILDER.comment("Global multiplier to all players' mana regeneration. Default: 1.0");
        MANA_REGEN_MULTIPLIER = BUILDER.worldRestart().define("manaRegenMultiplier", 1.0);
        BUILDER.comment("Whether merging scrolls with ink to upgrade them in the Arcane Anvil is enabled.");
        SCROLL_MERGING = BUILDER.define("scrollMerging", true);
        BUILDER.pop();

        BUILDER.push("Upgrade Overrides");
        BUILDER.comment("Use these lists to change what items can interact with the Arcane Anvil's upgrade system. This can also be done via datapack.");
        BUILDER.comment("Upgrade Whitelist. Use an item's id to allow it to be upgraded, ex: \"minecraft:iron_sword\"");
        UPGRADE_WHITELIST = BUILDER.defineListAllowEmpty("upgradeWhitelist", ArrayList::new, (string) -> true);
        BUILDER.comment("Upgrade Blacklist. Use an item's id to prevent it from being upgraded, ex: \"minecraft:iron_sword\"");
        UPGRADE_BLACKLIST = BUILDER.defineListAllowEmpty("upgradeBlacklist", ArrayList::new, (string) -> true);
        BUILDER.pop();

        BUILDER.push("Imbue Overrides");
        BUILDER.comment("Use these lists to change what items can interact with the Arcane Anvil's imbue system.");
        BUILDER.comment("/!\\ Unsupported item types are not guaranteed to work out of the box.");
        BUILDER.comment("Imbue Whitelist. Use an item's id to allow it to be imbued, ex: \"minecraft:iron_sword\"");
        IMBUE_WHITELIST = BUILDER.defineListAllowEmpty("imbueWhitelist", ArrayList::new, (string) -> true);
        BUILDER.comment("Imbue Blacklist. Use an item's id to prevent it from being imbued, ex: \"minecraft:iron_sword\"");
        IMBUE_BLACKLIST = BUILDER.defineListAllowEmpty("imbueBlacklist", ArrayList::new, (string) -> true);
        BUILDER.pop();

        BUILDER.push("Worldgen");
        BUILDER.comment("The weight of the priest house spawning in a village. Default: 4");
        PRIEST_TOWER_SPAWNRATE = BUILDER.define("priestHouseWeight", 4);
        BUILDER.pop();
        //IronsSpellbooks.LOGGER.debug("CFG: static");
        BUILDER.comment("Individual Spell Configuration");
        BUILDER.push("Spells");

        SpellDiscovery.getSpellsForConfig()
                .stream()
                .collect(Collectors.groupingBy(x -> x.getDefaultConfig().schoolResource))
                .forEach((school, spells) -> {
                    BUILDER.comment(school.toString());
                    spells.forEach(ServerConfigs::createSpellConfig);
                });

        BUILDER.pop();

        SPEC = BUILDER.build();
    }

    public static SpellConfigParameters getSpellConfig(AbstractSpell abstractSpell) {
        //IronsSpellbooks.LOGGER.debug("CFG: getSpellConfig {} {}", spellType, SPELL_CONFIGS.containsKey(spellType));
        return SPELL_CONFIGS.getOrDefault(abstractSpell.getSpellId(), DEFAULT_CONFIG);
    }

    public static Map<String, SpellConfigParameters> getSpellConfigs() {
        return SPELL_CONFIGS;
    }

    public static void onConfigReload() {
        IronsSpellbooks.LOGGER.debug("ServerConfigs load item blacklists:");
        cacheItemList(UPGRADE_WHITELIST.get(), UPGRADE_WHITELIST_ITEMS);
        cacheItemList(UPGRADE_BLACKLIST.get(), UPGRADE_BLACKLIST_ITEMS);
        cacheItemList(IMBUE_WHITELIST.get(), IMBUE_WHITELIST_ITEMS);
        cacheItemList(IMBUE_BLACKLIST.get(), IMBUE_BLACKLIST_ITEMS);
    }

    private static void cacheItemList(List<? extends String> ids, Set<Item> output) {
        output.clear();
        for (String name : ids) {
            try {
                if (name.startsWith("#")) {
                    var tag = new TagKey<Item>(Registries.ITEM, ResourceLocation.parse(name.substring(1)));
                    output.addAll(BuiltInRegistries.ITEM.stream().filter(item -> item.builtInRegistryHolder().is(tag)).toList());
                } else {
                    var item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(name));
                    if (item != null) {
                        output.add(item);
                    } else {
                        IronsSpellbooks.LOGGER.warn("Unable to add item to config, no such item id: {}", name);
                    }
                }
            } catch (Exception e) {
                IronsSpellbooks.LOGGER.warn("Unable to validate item config: {}", e.getMessage());
            }
        }
    }

    private static void createSpellConfig(AbstractSpell spell) {
        DefaultConfig config = spell.getDefaultConfig();
        //IronsSpellbooks.LOGGER.debug("CFG: createSpellConfig");
        BUILDER.push(spell.getSpellId());

        SPELL_CONFIGS.put(spell.getSpellId(), new SpellConfigParameters(
                config,
                BUILDER.define("Enabled", config.enabled),
                BUILDER.define("School", config.schoolResource.toString()),
                BUILDER.define("MaxLevel", config.maxLevel),
                BUILDER.defineEnum("MinRarity", config.minRarity),
                BUILDER.define("ManaCostMultiplier", 1d),
                BUILDER.define("SpellPowerMultiplier", 1d),
                BUILDER.define("CooldownInSeconds", config.cooldownInSeconds),
                BUILDER.define("AllowCrafting", config.allowCrafting)
        ));

        BUILDER.pop();
    }

    private static String createSpellConfigTitle(String str) {
        var words = str.split("[_| ]");
        for (int i = 0; i < words.length; i++) {
            words[i] = words[i].substring(0, 1).toUpperCase() + words[i].substring(1);
        }
        return Arrays.stream(words).sequential().collect(Collectors.joining("-"));
    }

    public static class SpellConfigParameters {
        //why did i do all this manually why isnt it a record :D
        final Supplier<Boolean> ENABLED;
        final Supplier<String> SCHOOL;
        final Supplier<SchoolType> ACTUAL_SCHOOL;
        final Supplier<Integer> MAX_LEVEL;
        final Supplier<SpellRarity> MIN_RARITY;
        final Supplier<Double> M_MULT;
        final Supplier<Double> P_MULT;
        final Supplier<Double> CS;
        final Supplier<Boolean> ALLOW_CRAFTING;

        SpellConfigParameters(
                DefaultConfig defaultConfig,
                Supplier<Boolean> ENABLED,
                Supplier<String> SCHOOL,
                Supplier<Integer> MAX_LEVEL,
                Supplier<SpellRarity> MIN_RARITY,
                Supplier<Double> M_MULT,
                Supplier<Double> P_MULT,
                Supplier<Double> CS,
                Supplier<Boolean> ALLOW_CRAFTING) {
            this.ENABLED = ENABLED;
            this.SCHOOL = SCHOOL;
            this.MAX_LEVEL = MAX_LEVEL;
            this.MIN_RARITY = MIN_RARITY;
            this.M_MULT = M_MULT;
            this.P_MULT = P_MULT;
            this.CS = CS;
            this.ALLOW_CRAFTING = ALLOW_CRAFTING;
            this.ACTUAL_SCHOOL = () -> {
                var school = SchoolRegistry.getSchool(ResourceLocation.parse(SCHOOL.get()));
                if (school != null) {
                    return school;
                }
                IronsSpellbooks.LOGGER.warn("Bad school config entry: {}. Reverting to default ({}).", SCHOOL.get(), defaultConfig.schoolResource);
                return SchoolRegistry.getSchool(defaultConfig.schoolResource);
            };
        }

        public boolean enabled() {
            return ENABLED.get();
        }

        public int maxLevel() {
            return MAX_LEVEL.get();
        }

        public SpellRarity minRarity() {
            return MIN_RARITY.get();
        }

        public double powerMultiplier() {
            return P_MULT.get();
        }

        public double manaMultiplier() {
            return M_MULT.get();
        }

        public int cooldownInTicks() {
            return (int) (CS.get() * 20);
        }

        public boolean allowCrafting() {
            return ALLOW_CRAFTING.get();
        }

        public SchoolType school() {
            return ACTUAL_SCHOOL.get();
        }
    }

}
