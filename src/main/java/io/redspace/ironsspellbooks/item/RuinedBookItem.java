package io.redspace.ironsspellbooks.item;

import io.redspace.ironsspellbooks.IronsSpellbooks;
import io.redspace.ironsspellbooks.registries.MobEffectRegistry;
import io.redspace.ironsspellbooks.util.MinecraftInstanceHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Optional;

public class RuinedBookItem extends Item implements ILecternPlaceable {
    public static Component PAGE = Component.literal(
            //fuck you cheater, *bytes your string*
            //oh, that was too easy? *encrypts bytes*
            //this is mainly a deterrent. be deterred.
            String.valueOf(new char[]{
                    876 / 4 / 3 / 1,
                    220 / 2 / 1,
                    64 / 2,
                    232 / 2,
                    130 / 1 * 4 / 5,
                    101 * 1 / 1,
                    24 * 4 / 3 * 1,
                    245 / 5 * 2,
                    202 * 1 / 1 / 2,
                    103 * 1 * 1,
                    175 * 3 / 1 / 5,
                    22 * 1 * 5 / 1,
                    1320 / 4 / 1 / 3,
                    21 * 5,
                    550 / 5 / 1,
                    103 * 2 / 2,
                    44 / 1,
                    8 * 1 / 1 * 4,
                    111 * 1,
                    55 * 5 / 5 * 2,
                    108 * 1 * 1,
                    121 * 3 / 3,
                    128 * 3 / 3 / 4,
                    8000 / 5 / 4 / 4,
                    776 / 2 / 4,
                    570 / 1 / 5,
                    214 / 2,
                    110 * 2 * 4 / 8,
                    101 * 1,
                    460 * 1 / 4,
                    23 * 5,
                    23 * 1 * 2 * 1,
                    30 / 3,
                    87 / 4 / 1 * 4 + 3,
                    104 / 1 / 1,
                    121 * 1 * 1,
                    32 * 2 / 1 / 2,
                    200 * 1 / 2 / 1,
                    210 * 1 / 2,
                    200 / 2 * 5 / 5,
                    96 / 3,
                    115 / 5 * 5,
                    208 * 1 / 2,
                    101 / 4 * 4 + 1,
                    8 * 4,
                    144 / 4 * 3,
                    101 * 1 * 1,
                    97 * 1 * 1,
                    118 * 1 * 1 * 1,
                    2424 / 3 / 2 / 4,
                    21 * 1 * 3,
                    1 * 1 * 5 * 2,
                    68 / 5 * 4 + 16,
                    388 * 1 / 4,
                    351 * 1 / 3,
                    412 / 4,
                    1040 / 5 / 2,
                    29 / 5 * 4 + 96,
                    303 * 1 / 3,
                    152 * 3 / 4,
                    32 / 1 / 4 * 4,
                    333 / 3 / 3 * 3,
                    102 * 1 / 1,
                    40 * 4 / 5 * 1,
                    17 * 4,
                    97 * 1 * 1 * 1,
                    570 / 1 / 5,
                    107 * 1 * 1 * 1,
                    110 / 5 * 1 * 5,
                    101 * 1,
                    115 * 1,
                    23 * 5 * 1,
                    11 * 4,
                    20 / 5 * 2 * 4,
                    33 * 2,
                    38 * 3 * 1,
                    525 / 5,
                    1100 / 5 / 2 * 1,
                    103 * 1 / 1,
                    101 * 1 * 1,
                    285 * 2 / 5 * 1,
                    16 * 4 / 2,
                    666 / 2 / 3,
                    34 * 3 * 1,
                    16 * 2,
                    19 * 4,
                    1260 / 3 / 4,
                    515 / 5,
                    52 / 2 * 4,
                    29 * 2 * 2,
                    23 * 1 / 1 * 2,
                    30 / 3,
                    158 * 1 / 2 * 1,
                    104 * 1 * 1,
                    8 * 2 * 2 / 1,
                    416 / 2 / 2 * 1,
                    1332 / 4 / 3,
                    476 / 4,
                    8 * 4,
                    145 * 4 / 5,
                    52 * 2 * 1,
                    101 * 1 * 1 * 1,
                    64 / 4 * 1 * 2,
                    86 * 1,
                    37 * 3 * 1,
                    210 / 4 * 2 + 1,
                    200 / 2,
                    48 / 3 / 1 * 2,
                    109 * 1 * 1 * 1,
                    234 / 2,
                    460 / 4 / 5 * 5,
                    58 / 2 * 4,
                    16 * 2,
                    52 * 2 / 1 / 1,
                    97 * 1 / 1 * 1,
                    116 / 1,
                    101 * 1 / 1,
                    16 / 2 * 4 / 1,
                    121 / 1 * 1 * 1,
                    666 / 3 / 2,
                    234 / 2 / 1,
                    132 / 3,
                    40 / 4,
                    13 * 5 * 1,
                    171 * 2 / 3,
                    97 * 1,
                    1160 / 2 / 5,
                    13 * 2 * 4,
                    484 / 4,
                    324 / 3 / 1,
                    46 * 1,
                    64 / 2,
                    65 / 4 * 4 + 1,
                    114 / 1 * 1,
                    97 * 1,
                    58 * 2,
                    832 * 1 / 2 / 4,
                    121 * 1 / 1,
                    135 / 5 * 4,
                    184 / 4,
                    16 * 2,
                    195 * 1 / 3,
                    228 * 1 / 2,
                    97 * 1 * 1,
                    2784 / 2 / 3 / 4,
                    312 / 1 / 3,
                    121 * 1,
                    27 / 1 * 4,
                    23 * 2
            })
    ).withStyle(Style.EMPTY.withFont(ResourceLocation.withDefaultNamespace("alt")));
    public static Component PAGE2 = PAGE.copy().withStyle(ChatFormatting.OBFUSCATED);
    public static Component DARKNESS = Component.literal("Darkness").withStyle(Style.EMPTY.withFont(ResourceLocation.withDefaultNamespace("alt")));
    public static Component DARKNESS2 = DARKNESS.copy().withStyle(ChatFormatting.OBFUSCATED);
    public static ResourceLocation LECTERN_LOCATION = IronsSpellbooks.id("textures/entity/lectern/ruined_book.png");

    public RuinedBookItem(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public List<Component> getPages(ItemStack stack) {
        var player = MinecraftInstanceHelper.getPlayer();
        if (player == null || !player.hasEffect(MobEffectRegistry.PLANAR_SIGHT)) {
            return List.of(PAGE2, DARKNESS2, DARKNESS2, DARKNESS2, DARKNESS2, DARKNESS2, DARKNESS2, DARKNESS2, DARKNESS2, DARKNESS2);
        } else {
            return List.of(PAGE, DARKNESS, DARKNESS, DARKNESS, DARKNESS, DARKNESS, DARKNESS, DARKNESS, DARKNESS, DARKNESS);
        }
    }

    @Override
    public Optional<ResourceLocation> simpleTextureOverride(ItemStack stack) {
        return Optional.of(LECTERN_LOCATION);
    }
}
