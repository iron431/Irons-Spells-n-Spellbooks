package io.redspace.ironsspellbooks.gui.arcane_anvil;

import io.redspace.ironsspellbooks.api.item.UpgradeData;
import io.redspace.ironsspellbooks.api.item.curios.AffinityData;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.config.ServerConfigs;
import io.redspace.ironsspellbooks.item.*;
import io.redspace.ironsspellbooks.item.armor.UpgradeType;
import io.redspace.ironsspellbooks.item.curios.AffinityRing;
import io.redspace.ironsspellbooks.registries.BlockRegistry;
import io.redspace.ironsspellbooks.registries.ComponentRegistry;
import io.redspace.ironsspellbooks.registries.ItemRegistry;
import io.redspace.ironsspellbooks.registries.MenuRegistry;
import io.redspace.ironsspellbooks.util.UpgradeUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.ItemCombinerMenu;
import net.minecraft.world.inventory.ItemCombinerMenuSlotDefinition;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

public class ArcaneAnvilMenu extends ItemCombinerMenu {
    public ArcaneAnvilMenu(int pContainerId, Inventory inventory, ContainerLevelAccess containerLevelAccess) {
        super(MenuRegistry.ARCANE_ANVIL_MENU.get(), pContainerId, inventory, containerLevelAccess);
    }

    private final List<ItemStack> additionalDrops = new ArrayList<>();

    public ArcaneAnvilMenu(int pContainerId, Inventory inventory, FriendlyByteBuf extraData) {
        this(pContainerId, inventory, ContainerLevelAccess.NULL);
    }

    @Override
    protected boolean mayPickup(Player pPlayer, boolean pHasStack) {
        return true;
    }

    @Override
    protected void onTake(Player p_150601_, ItemStack p_150602_) {
        inputSlots.getItem(0).shrink(1);
        inputSlots.getItem(1).shrink(1);

        this.access.execute((level, pos) -> {
            level.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, .8f, 1.1f);
            level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.BLOCKS, 1f, 1f);
            additionalDrops.forEach(stack -> {
                if (!stack.isEmpty()) {
                    level.addFreshEntity(new ItemEntity(level, pos.getX() + .5, pos.getY() + 1, pos.getZ() + .5, stack));
                }
            });
            additionalDrops.clear();

        });
        createResult();
    }

    @Override
    protected boolean isValidBlock(BlockState pState) {
        return pState.is(BlockRegistry.ARCANE_ANVIL_BLOCK.get());
    }

    @Override
    public void createResult() {
        ItemStack result = ItemStack.EMPTY;
        this.additionalDrops.clear();
        /*
        Actions that can be taken in arcane anvil:
        - Upgrade scroll (scroll + scroll)
        - Imbue Weapon (weapon + scroll)
        - Upgrade item (item + upgrade orb)
         */
        ItemStack baseItemStack = inputSlots.getItem(0);
        ItemStack modifierItemStack = inputSlots.getItem(1);
        if (!baseItemStack.isEmpty() && !modifierItemStack.isEmpty()) {
            //Scroll Merging
            if (ServerConfigs.SCROLL_MERGING.get() && baseItemStack.getItem() instanceof Scroll && modifierItemStack.getItem() instanceof InkItem inkItem/*Scroll*/) {
                var spell1 = ISpellContainer.get(baseItemStack).getSpellAtIndex(0);
                if (spell1.getLevel() < spell1.getSpell().getMaxLevel()) {
                    var baseRarity = spell1.getRarity();
                    var nextRarity = spell1.getSpell().getRarity(spell1.getLevel() + 1);
                    if (nextRarity.equals(inkItem.getRarity())) {
                        result = baseItemStack.copy();
                        result.setCount(1);
                        ISpellContainer.createScrollContainer(spell1.getSpell(), spell1.getLevel() + 1, result);
                    }
                }
                //var spell2 = ISpellContainer.get(modifierItemStack).getSpellAtIndex(0);

                //if (spell1.equals(spell2)) {
                //    if (spell1.getLevel() < ServerConfigs.getSpellConfig(spell1.getSpell()).maxLevel()) {
                //        result = new ItemStack(ItemRegistry.SCROLL.get());
                //        ISpellContainer.createScrollContainer(spell1.getSpell(), spell1.getLevel() + 1, result);
                //    }
                //}
            }
            //Unique Weapon Improving
            else if (baseItemStack.getItem() instanceof UniqueItem && modifierItemStack.getItem() instanceof Scroll scroll) {
                var scrollSlot = ISpellContainer.get(modifierItemStack).getSpellAtIndex(0);
                if (ISpellContainer.isSpellContainer(baseItemStack)) {
                    var spellContainer = ISpellContainer.get(baseItemStack);
                    var matchIndex = spellContainer.getIndexForSpell(scrollSlot.getSpell());
                    if (matchIndex >= 0) {
                        var spellData = spellContainer.getSpellAtIndex(matchIndex);
                        if (spellData.getLevel() < scrollSlot.getLevel() && spellData.isLocked()) {
                            result = baseItemStack.copy();
                            var newContainer = spellContainer.mutableCopy();
                            newContainer.removeSpellAtIndex(matchIndex);
                            newContainer.addSpellAtIndex(scrollSlot.getSpell(), scrollSlot.getLevel(), matchIndex, true);
                            newContainer.setImproved(true);
                            result.set(ComponentRegistry.SPELL_CONTAINER, newContainer.toImmutable());
                        }
                    }
                }
            }
            //Weapon Imbuement
            else if (Utils.canImbue(baseItemStack) && modifierItemStack.getItem() instanceof Scroll scroll) {
                result = baseItemStack.copy();
                var spellContainer = ISpellContainer.getOrCreate(result).mutableCopy();

                var scrollSlot = ISpellContainer.get(modifierItemStack).getSpellAtIndex(0);
                var nextSlotIndex = spellContainer.getNextAvailableIndex();

                if (nextSlotIndex == -1) {
                    nextSlotIndex = 0;
                }
                //override slot
                spellContainer.removeSpellAtIndex(nextSlotIndex);
                spellContainer.addSpellAtIndex(scrollSlot.getSpell(), scrollSlot.getLevel(), nextSlotIndex, false);
                result.set(ComponentRegistry.SPELL_CONTAINER, spellContainer.toImmutable());
            }
            //Upgrade System
            else if (Utils.canBeUpgraded(baseItemStack) && UpgradeData.getUpgradeData(baseItemStack).getTotalUpgrades() < ServerConfigs.MAX_UPGRADES.get() && modifierItemStack.getItem() instanceof UpgradeOrbItem upgradeOrb) {
                result = baseItemStack.copy();
                String slot = UpgradeUtils.getRelevantEquipmentSlot(result);
                UpgradeData.getUpgradeData(result).addUpgrade(result, upgradeOrb.getUpgradeType(), slot);
                //IronsSpellbooks.LOGGER.debug("ArcaneAnvilMenu: upgrade system test: total upgrades on {}: {}", result.getDisplayName().getString(), UpgradeUtils.getUpgradeCount(result));
            }
            //Shriving Stone
            else if (modifierItemStack.is(ItemRegistry.SHRIVING_STONE.get())) {
                result = Utils.handleShriving(baseItemStack);
                UpgradeData upgradeData = UpgradeData.getUpgradeData(baseItemStack);
                upgradeData.getUpgrades().forEach((upgrade, count) -> additionalDrops.add(upgradeOrbFromType(upgrade, count)));
            }
            //Spell Slot upgrades
            else if (modifierItemStack.getItem() instanceof SpellSlotUpgradeItem spellSlotUpgradeItem) {
                if (baseItemStack.getItem() instanceof SpellBook) {
                    ISpellContainer spellBookContainer = ISpellContainer.get(baseItemStack);
                    int max = spellSlotUpgradeItem.maxSlots();
                    if (spellBookContainer.getMaxSpellCount() < max) {
                        result = baseItemStack.copy();
                        var upgradedContainer = ISpellContainer.get(result).mutableCopy();
                        upgradedContainer.setMaxSpellCount(upgradedContainer.getMaxSpellCount() + 1);
                        result.set(ComponentRegistry.SPELL_CONTAINER, upgradedContainer.toImmutable());
                    }
                }
            }
            //Attune Affinity Ring
            else if (baseItemStack.getItem() instanceof AffinityRing affinityRing && modifierItemStack.getItem() instanceof Scroll scroll) {
                result = baseItemStack.copy();
                var scrollSlot = ISpellContainer.get(modifierItemStack).getSpellAtIndex(0);
                AffinityData originalData = baseItemStack.getOrDefault(ComponentRegistry.AFFINITY_COMPONENT, AffinityData.NONE);
                AffinityData newData = new AffinityData(scrollSlot.getSpell().getSpellId(), originalData.bonus());
                result.set(ComponentRegistry.AFFINITY_COMPONENT, newData);
            }
        }

        resultSlots.setItem(0, result);
    }

    private ItemStack upgradeOrbFromType(UpgradeType type, int count) {
        return type.getContainerItem().map(item -> {
            var stack = new ItemStack(item);
            stack.setCount(count);
            return stack;
        }).orElse(ItemStack.EMPTY);
    }

    @Override
    protected ItemCombinerMenuSlotDefinition createInputSlotDefinitions() {
        //copied from anvil for 1.19.4
        return ItemCombinerMenuSlotDefinition.create().withSlot(0, 27, 47, (p_266635_) -> {
            return true;
        }).withSlot(1, 76, 47, (p_266634_) -> {
            return true;
        }).withResultSlot(2, 134, 47).build();
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack pStack, Slot pSlot) {
        return pSlot.container != this.resultSlots && super.canTakeItemForPickAll(pStack, pSlot);
    }
}
