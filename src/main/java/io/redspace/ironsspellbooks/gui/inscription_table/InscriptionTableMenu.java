package io.redspace.ironsspellbooks.gui.inscription_table;

import io.redspace.ironsspellbooks.api.events.InscribeSpellEvent;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.item.Scroll;
import io.redspace.ironsspellbooks.item.SpellBook;
import io.redspace.ironsspellbooks.registries.BlockRegistry;
import io.redspace.ironsspellbooks.registries.ComponentRegistry;
import io.redspace.ironsspellbooks.registries.ItemRegistry;
import io.redspace.ironsspellbooks.registries.MenuRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.NeoForge;


public class InscriptionTableMenu extends AbstractContainerMenu {
    //    public final InscriptionTableTile blockEntity;
    private final Player player;
    private final Level level;
    private final Slot spellBookSlot;
    private final Slot scrollSlot;
    private final Slot resultSlot;
    private int selectedSpellIndex = -1;
    private boolean fromCurioSlot = false;

    protected final ResultContainer resultContainer = new ResultContainer();
    protected final Container scrollContainer = new SimpleContainer(1) {
        /**
         * For block entities, ensures the chunk containing the block entity is saved to disk later - the game won't think
         * it hasn't changed and skip it.
         */
        public void setChanged() {
            super.setChanged();
            InscriptionTableMenu.this.slotsChanged(this);
        }
    };
    protected final Container spellbookContainer = new SimpleContainer(1) {
        /**
         * For block entities, ensures the chunk containing the block entity is saved to disk later - the game won't think
         * it hasn't changed and skip it.
         */
        public void setChanged() {
            super.setChanged();
            InscriptionTableMenu.this.slotsChanged(this);
        }

        @Override
        public boolean canPlaceItem(int pSlot, ItemStack pStack) {
            return super.canPlaceItem(pSlot, pStack);
        }
    };

    public InscriptionTableMenu(int containerId, Inventory inv, FriendlyByteBuf extraData) {
        this(containerId, inv, ContainerLevelAccess.NULL/* inv.player.level().getBlockEntity(extraData.readBlockPos())*/);
    }

    protected final ContainerLevelAccess access;


    public InscriptionTableMenu(int containerId, Inventory inv, ContainerLevelAccess access/* BlockEntity entity*/) {
        super(MenuRegistry.INSCRIPTION_TABLE_MENU.get(), containerId);
        this.access = access;
        //exists on server and render
        checkContainerSize(inv, 3);
        this.level = inv.player.level();
        this.player = inv.player;

        addPlayerInventory(inv);
        addPlayerHotbar(inv);

        spellBookSlot = new Slot(spellbookContainer, 0, 17, 21) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof SpellBook;
            }

            @Override
            public void set(ItemStack pStack) {
                super.set(pStack);
                if (fromCurioSlot && player != null) {
                    Utils.setPlayerSpellbookStack(player, pStack);
                }
            }

            @Override
            public void onTake(Player pPlayer, ItemStack pStack) {
                InscriptionTableMenu.this.setSelectedSpell(-1);
                super.onTake(pPlayer, pStack);
            }
        };
        scrollSlot = new Slot(scrollContainer, 0, 17, 53) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(ItemRegistry.SCROLL.get());
            }

        };
        resultSlot = new Slot(resultContainer, 2, 208, 136) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }

            @Override
            public void onTake(Player player, ItemStack stack) {
                //Ironsspellbooks.logger.debug("InscriptionTableMenu.take spell!");
                var spellBookStack = spellBookSlot.getItem();
                var spellList = ISpellContainer.get(spellBookStack).mutableCopy();
                spellList.removeSpellAtIndex(selectedSpellIndex);
                spellBookStack.set(ComponentRegistry.SPELL_CONTAINER, spellList.toImmutable());
                super.onTake(player, spellBookStack);
            }
        };

        this.addSlot(spellBookSlot);
        this.addSlot(scrollSlot);
        this.addSlot(resultSlot);
//        this.blockEntity.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY).ifPresent(handler -> {
//            this.addSlot(new SlotItemHandler(handler, 0, 17, 21));
//            this.addSlot(new SlotItemHandler(handler, 1, 17, 53));
//            this.addSlot(new ScrollExtractionSlot(handler, 2, 208, 136));
//        });

        var spellbookStack = Utils.getPlayerSpellbookStack(inv.player);
        if (spellbookStack != null) {
            fromCurioSlot = true;
            spellBookSlot.set(spellbookStack);
        }
    }


    public Slot getSpellBookSlot() {
        return spellBookSlot;
    }


    public Slot getScrollSlot() {
        return scrollSlot;
    }

    public Slot getResultSlot() {
        return resultSlot;
    }

    @Override
    public void slotsChanged(Container pContainer) {
        super.slotsChanged(pContainer);
        setupResultSlot();
    }

    public void setSelectedSpell(int index) {
        selectedSpellIndex = index;
        setupResultSlot();
    }

    public void doInscription(int selectedIndex) {
        // This method is called by the inscription packet
        ItemStack spellBookItemStack = getSpellBookSlot().getItem();
        ItemStack scrollItemStack = getScrollSlot().getItem();

        if (spellBookItemStack.getItem() instanceof SpellBook && scrollItemStack.getItem() instanceof Scroll) {
            var bookContainer = ISpellContainer.get(spellBookItemStack);
            var scrollContainer = ISpellContainer.get(scrollItemStack);
            var scrollSlot = scrollContainer.getSpellAtIndex(0);
            var mutableBookContainer = bookContainer.mutableCopy();
            if (mutableBookContainer.addSpellAtIndex(scrollSlot.getSpell(), scrollSlot.getLevel(), selectedIndex, false)) {
                getScrollSlot().remove(1);
                spellBookItemStack.set(ComponentRegistry.SPELL_CONTAINER, mutableBookContainer.toImmutable());
            }
        }
    }

    @Override
    public boolean clickMenuButton(Player pPlayer, int pId) {
        //Called whenever the client clicks on a button. The ID passed in is the spell slot index or -1. If it is positive, it is to select that slot. If it is negative, it is to inscribe
        if (pId < 0) {
            var scrollStack = getScrollSlot().getItem();
            if (selectedSpellIndex >= 0 && scrollStack.getItem() instanceof Scroll scroll) {
                SpellData spellData = ISpellContainer.get(scrollStack).getSpellAtIndex(0);
                if (NeoForge.EVENT_BUS.post(new InscribeSpellEvent(pPlayer, spellData)).isCanceled())
                    return false;
                doInscription(selectedSpellIndex);
            }
        } else {
            setSelectedSpell(pId);
        }
        return true;
    }

    private void setupResultSlot() {
        //Ironsspellbooks.logger.debug("InscriptionTableMenu.setupResultSlot");
        //Ironsspellbooks.logger.debug("InscriptionTableMenu.selected spell index: {}", selectedSpellIndex);

        ItemStack resultStack = ItemStack.EMPTY;
        ItemStack spellBookStack = spellBookSlot.getItem();

        if (spellBookStack.getItem() instanceof SpellBook) {
            var spellList = ISpellContainer.get(spellBookStack);
            if (selectedSpellIndex >= 0) {
                var spellData = spellList.getSpellAtIndex(selectedSpellIndex);

                if (spellData != SpellData.EMPTY && spellData.canRemove()) {
                    resultStack = new ItemStack(ItemRegistry.SCROLL.get());
                    resultStack.setCount(1);
                    ISpellContainer.createScrollContainer(spellData.getSpell(), spellData.getLevel(), resultStack);
                }
            }
        }

        if (!ItemStack.matches(resultStack, this.resultSlot.getItem())) {
            this.resultSlot.set(resultStack);
        }
    }

    // CREDIT GOES TO: diesieben07 | https://github.com/diesieben07/SevenCommons
    // must assign a slot number to each of the slots used by the GUI.
    // For this container, we can see both the tile inventory's slots as well as the player inventory slots and the hotbar.
    // Each time we add a Slot to the container, it automatically increases the slotIndex, which means
    //  0 - 8 = hotbar slots (which will map to the InventoryPlayer slot numbers 0 - 8)
    //  9 - 35 = player inventory slots (which map to the InventoryPlayer slot numbers 9 - 35)
    //  36 - 44 = TileInventory slots, which map to our TileEntity slot numbers 0 - 8)
    private static final int HOTBAR_SLOT_COUNT = 9;
    private static final int PLAYER_INVENTORY_ROW_COUNT = 3;
    private static final int PLAYER_INVENTORY_COLUMN_COUNT = 9;
    private static final int PLAYER_INVENTORY_SLOT_COUNT = PLAYER_INVENTORY_COLUMN_COUNT * PLAYER_INVENTORY_ROW_COUNT;
    private static final int VANILLA_SLOT_COUNT = HOTBAR_SLOT_COUNT + PLAYER_INVENTORY_SLOT_COUNT;
    private static final int VANILLA_FIRST_SLOT_INDEX = 0;
    private static final int TE_INVENTORY_FIRST_SLOT_INDEX = VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT;

    // THIS YOU HAVE TO DEFINE!
    private static final int TE_INVENTORY_SLOT_COUNT = 3;  // must be the number of slots you have!

    @Override
    public ItemStack quickMoveStack(Player playerIn, int index) {
        Slot sourceSlot = slots.get(index);
        if (sourceSlot == null || !sourceSlot.hasItem()) return ItemStack.EMPTY;  //EMPTY_ITEM
        ItemStack sourceStack = sourceSlot.getItem();
        ItemStack copyOfSourceStack = sourceStack.copy();

        // Check if the slot clicked is one of the vanilla container slots
        if (index < VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT) {
            // This is a vanilla container slot so merge the stack into the tile inventory
            if (!moveItemStackTo(sourceStack, TE_INVENTORY_FIRST_SLOT_INDEX, TE_INVENTORY_FIRST_SLOT_INDEX + TE_INVENTORY_SLOT_COUNT, false)) {
                return ItemStack.EMPTY;  // EMPTY_ITEM
            }
        } else if (index < TE_INVENTORY_FIRST_SLOT_INDEX + TE_INVENTORY_SLOT_COUNT) {
            // This is a TE slot so merge the stack into the players inventory
            if (!moveItemStackTo(sourceStack, VANILLA_FIRST_SLOT_INDEX, VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT, false)) {
                return ItemStack.EMPTY;
            }
        } else {
            return ItemStack.EMPTY;
        }
        // If stack size == 0 (the entire stack was moved) set slot contents to null
        if (sourceStack.getCount() == 0) {
            sourceSlot.set(ItemStack.EMPTY);
        } else {
            sourceSlot.setChanged();
        }
        sourceSlot.onTake(playerIn, sourceStack);
        return copyOfSourceStack;
    }

    @Override
    public boolean stillValid(Player pPlayer) {
//        return stillValid(ContainerLevelAccess.create(level, blockEntity.getBlockPos()),
//                pPlayer, INSCRIPTION_TABLE_BLOCK.get());
        return this.access.evaluate((level, blockPos) -> {
            return !level.getBlockState(blockPos).is(BlockRegistry.INSCRIPTION_TABLE_BLOCK.get()) ? false : pPlayer.distanceToSqr((double) blockPos.getX() + 0.5D, (double) blockPos.getY() + 0.5D, (double) blockPos.getZ() + 0.5D) <= 64.0D;
        }, true);
    }

    private void addPlayerInventory(Inventory playerInventory) {
        for (int i = 0; i < 3; ++i) {
            for (int l = 0; l < 9; ++l) {
                this.addSlot(new Slot(playerInventory, l + i * 9 + 9, 8 + l * 18, 84 + i * 18));
            }
        }
    }

    private void addPlayerHotbar(Inventory playerInventory) {
        for (int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(playerInventory, i, 8 + i * 18, 142));
        }
    }

    @Override
    public void removed(Player pPlayer) {
        if (pPlayer instanceof ServerPlayer) {
            super.removed(pPlayer);
            this.access.execute((p_39796_, p_39797_) -> {
                this.clearContainer(pPlayer, this.scrollContainer);
                if (fromCurioSlot) {
                    Utils.setPlayerSpellbookStack(pPlayer, spellBookSlot.remove(1));
                } else {
                    this.clearContainer(pPlayer, this.spellbookContainer);
                }
            });
        }
    }
}
