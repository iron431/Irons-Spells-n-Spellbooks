package io.redspace.ironsspellbooks.registries;


import io.redspace.ironsspellbooks.IronsSpellbooks;
import io.redspace.ironsspellbooks.block.ArmorPileBlock;
import io.redspace.ironsspellbooks.block.BloodCauldronBlock;
import io.redspace.ironsspellbooks.block.BrazierBlock;
import io.redspace.ironsspellbooks.block.FireflyJar;
import io.redspace.ironsspellbooks.block.alchemist_cauldron.AlchemistCauldronBlock;
import io.redspace.ironsspellbooks.block.alchemist_cauldron.AlchemistCauldronTile;
import io.redspace.ironsspellbooks.block.arcane_anvil.ArcaneAnvilBlock;
import io.redspace.ironsspellbooks.block.inscription_table.InscriptionTableBlock;
import io.redspace.ironsspellbooks.block.pedestal.PedestalBlock;
import io.redspace.ironsspellbooks.block.pedestal.PedestalTile;
import io.redspace.ironsspellbooks.block.portal_frame.PortalFrameBlock;
import io.redspace.ironsspellbooks.block.portal_frame.PortalFrameBlockEntity;
import io.redspace.ironsspellbooks.block.scroll_forge.ScrollForgeBlock;
import io.redspace.ironsspellbooks.block.scroll_forge.ScrollForgeTile;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Collection;


public class BlockRegistry {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, IronsSpellbooks.MODID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, IronsSpellbooks.MODID);

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
        BLOCK_ENTITIES.register(eventBus);
    }

    public static final DeferredHolder<Block, Block> INSCRIPTION_TABLE_BLOCK = BLOCKS.register("inscription_table", InscriptionTableBlock::new);
    public static final DeferredHolder<Block, Block> SCROLL_FORGE_BLOCK = BLOCKS.register("scroll_forge", ScrollForgeBlock::new);
    public static final DeferredHolder<Block, Block> PEDESTAL_BLOCK = BLOCKS.register("pedestal", PedestalBlock::new);
    public static final DeferredHolder<Block, Block> BLOOD_CAULDRON_BLOCK = BLOCKS.register("blood_cauldron", BloodCauldronBlock::new);
    public static final DeferredHolder<Block, Block> ARCANE_ANVIL_BLOCK = BLOCKS.register("arcane_anvil", ArcaneAnvilBlock::new);
    public static final DeferredHolder<Block, Block> ARMOR_PILE_BLOCK = BLOCKS.register("armor_pile", ArmorPileBlock::new);
    public static final DeferredHolder<Block, Block> ALCHEMIST_CAULDRON = BLOCKS.register("alchemist_cauldron", AlchemistCauldronBlock::new);
    public static final DeferredHolder<Block, Block> FIREFLY_JAR = BLOCKS.register("firefly_jar", FireflyJar::new);
    public static final DeferredHolder<Block, Block> PORTAL_FRAME = BLOCKS.register("portal_frame", PortalFrameBlock::new);
    public static final DeferredHolder<Block, Block> BRAZIER_FIRE = BLOCKS.register("brazier", () -> new BrazierBlock(false));
    public static final DeferredHolder<Block, Block> BRAZIER_SOUL = BLOCKS.register("brazier_soul", () -> new BrazierBlock(true));

    public static final DeferredHolder<Block, Block> MITHRIL_ORE = BLOCKS.register("mithril_ore", () -> new Block(BlockBehaviour.Properties.of().lightLevel(state -> 9).mapColor(DyeColor.GRAY).requiresCorrectToolForDrops().strength(20.0F, 1200.0F).sound(SoundType.ANCIENT_DEBRIS)));
    public static final DeferredHolder<Block, Block> MITHRIL_ORE_DEEPSLATE = BLOCKS.register("deepslate_mithril_ore", () -> new Block(BlockBehaviour.Properties.of().lightLevel(state -> 9).mapColor(DyeColor.GRAY).requiresCorrectToolForDrops().strength(20.0F, 1200.0F).sound(SoundType.ANCIENT_DEBRIS)));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ScrollForgeTile>> SCROLL_FORGE_TILE = BLOCK_ENTITIES.register("scroll_forge", () -> BlockEntityType.Builder.of(ScrollForgeTile::new, SCROLL_FORGE_BLOCK.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PedestalTile>> PEDESTAL_TILE = BLOCK_ENTITIES.register("pedestal", () -> BlockEntityType.Builder.of(PedestalTile::new, PEDESTAL_BLOCK.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AlchemistCauldronTile>> ALCHEMIST_CAULDRON_TILE = BLOCK_ENTITIES.register("alchemist_cauldron", () -> BlockEntityType.Builder.of(AlchemistCauldronTile::new, ALCHEMIST_CAULDRON.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PortalFrameBlockEntity>> PORTAL_FRAME_BLOCK_ENTITY = BLOCK_ENTITIES.register("portal_frame", () -> BlockEntityType.Builder.of(PortalFrameBlockEntity::new, PORTAL_FRAME.get()).build(null));

    public static Collection<DeferredHolder<Block, ? extends Block>> blocks() {
        return BLOCKS.getEntries();
    }
}
