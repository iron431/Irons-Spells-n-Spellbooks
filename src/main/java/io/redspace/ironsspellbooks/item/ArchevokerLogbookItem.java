package io.redspace.ironsspellbooks.item;

import io.redspace.ironsspellbooks.IronsSpellbooks;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.WrittenBookContent;

import java.util.List;

public class ArchevokerLogbookItem extends ReadableLoreItem {
    public static WrittenBookContent TRANSLATED_CONTENTS = new WrittenBookContent(Filterable.passThrough(""), "Archevoker", 0, List.of(
            Filterable.passThrough(Component.translatable("item.irons_spellbooks.archevoker_log.header").append("2:\n\n").append(Component.translatable("item.irons_spellbooks.archevoker_log.entry_1.1"))),
            Filterable.passThrough(Component.translatable("item.irons_spellbooks.archevoker_log.entry_1.2")),
            Filterable.passThrough(Component.translatable("item.irons_spellbooks.archevoker_log.header").append("14:\n\n").append(Component.translatable("item.irons_spellbooks.archevoker_log.entry_2.1"))),
            Filterable.passThrough(Component.translatable("item.irons_spellbooks.archevoker_log.entry_2.2")),
            Filterable.passThrough(Component.translatable("item.irons_spellbooks.archevoker_log.header").append("31:\n\n").append(Component.translatable("item.irons_spellbooks.archevoker_log.entry_3.1"))),
            Filterable.passThrough(Component.translatable("item.irons_spellbooks.archevoker_log.entry_3.2")),
            Filterable.passThrough(Component.translatable("item.irons_spellbooks.archevoker_log.header").append("73:\n\n").append(Component.translatable("item.irons_spellbooks.archevoker_log.entry_4.1"))),
            Filterable.passThrough(Component.translatable("item.irons_spellbooks.archevoker_log.entry_4.2"))
    ), true);
    public static WrittenBookContent UNTRANSLATED_CONTENTS = new WrittenBookContent(Filterable.passThrough(""), "Archevoker", 0, List.of(
            Filterable.passThrough(Component.translatable("item.irons_spellbooks.archevoker_log.header").withStyle(Style.EMPTY.withFont(ResourceLocation.withDefaultNamespace("illageralt"))).append(Component.literal("2:\n\n").withStyle(Style.EMPTY.withFont(ResourceLocation.withDefaultNamespace("default")))).append(Component.translatable("item.irons_spellbooks.archevoker_log.entry_1.1").withStyle(Style.EMPTY.withFont(ResourceLocation.withDefaultNamespace("illageralt"))))),
            Filterable.passThrough(Component.translatable("item.irons_spellbooks.archevoker_log.entry_1.2").withStyle(Style.EMPTY.withFont(ResourceLocation.withDefaultNamespace("illageralt")))),
            Filterable.passThrough(Component.translatable("item.irons_spellbooks.archevoker_log.header").withStyle(Style.EMPTY.withFont(ResourceLocation.withDefaultNamespace("illageralt"))).append(Component.literal("14:\n\n").withStyle(Style.EMPTY.withFont(ResourceLocation.withDefaultNamespace("default")))).append(Component.translatable("item.irons_spellbooks.archevoker_log.entry_2.1").withStyle(Style.EMPTY.withFont(ResourceLocation.withDefaultNamespace("illageralt"))))),
            Filterable.passThrough(Component.translatable("item.irons_spellbooks.archevoker_log.entry_2.2").withStyle(Style.EMPTY.withFont(ResourceLocation.withDefaultNamespace("illageralt")))),
            Filterable.passThrough(Component.translatable("item.irons_spellbooks.archevoker_log.header").withStyle(Style.EMPTY.withFont(ResourceLocation.withDefaultNamespace("illageralt"))).append(Component.literal("31:\n\n").withStyle(Style.EMPTY.withFont(ResourceLocation.withDefaultNamespace("default")))).append(Component.translatable("item.irons_spellbooks.archevoker_log.entry_3.1").withStyle(Style.EMPTY.withFont(ResourceLocation.withDefaultNamespace("illageralt"))))),
            Filterable.passThrough(Component.translatable("item.irons_spellbooks.archevoker_log.entry_3.2").withStyle(Style.EMPTY.withFont(ResourceLocation.withDefaultNamespace("illageralt")))),
            Filterable.passThrough(Component.translatable("item.irons_spellbooks.archevoker_log.header").withStyle(Style.EMPTY.withFont(ResourceLocation.withDefaultNamespace("illageralt"))).append(Component.literal("73:\n\n").withStyle(Style.EMPTY.withFont(ResourceLocation.withDefaultNamespace("default")))).append(Component.translatable("item.irons_spellbooks.archevoker_log.entry_4.1").withStyle(Style.EMPTY.withFont(ResourceLocation.withDefaultNamespace("illageralt"))))),
            Filterable.passThrough(Component.translatable("item.irons_spellbooks.archevoker_log.entry_4.2").withStyle(Style.EMPTY.withFont(ResourceLocation.withDefaultNamespace("illageralt"))))
            ), true);

    private final boolean translated;
    public ArchevokerLogbookItem(boolean translated, Properties pProperties) {
        super(IronsSpellbooks.id("textures/entity/lectern/archevoker_logbook.png"), pProperties);
        this.translated = translated;
    }

    @Override
    public void appendHoverText(ItemStack pStack, TooltipContext pContext, List<Component> pTooltipComponents, TooltipFlag pTooltipFlag) {
        super.appendHoverText(pStack, pContext, pTooltipComponents, pTooltipFlag);
        if (translated) {
            pTooltipComponents.add(Component.translatable("tooltip.irons_spellbooks.translated").withStyle(ChatFormatting.YELLOW));
        } else {
            pTooltipComponents.add(Component.translatable("tooltip.irons_spellbooks.untranslated").withStyle(ChatFormatting.RED));
        }
    }
}

