package io.redspace.ironsspellbooks.player;

import io.redspace.ironsspellbooks.IronsSpellbooks;
import io.redspace.ironsspellbooks.api.entity.IMagicEntity;
import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.capabilities.magic.SyncedSpellData;
import io.redspace.ironsspellbooks.config.ServerConfigs;
import io.redspace.ironsspellbooks.effect.AbyssalShroudEffect;
import io.redspace.ironsspellbooks.effect.AscensionEffect;
import io.redspace.ironsspellbooks.effect.CustomDescriptionMobEffect;
import io.redspace.ironsspellbooks.effect.guiding_bolt.GuidingBoltManager;
import io.redspace.ironsspellbooks.entity.mobs.dead_king_boss.DeadKingMusicManager;
import io.redspace.ironsspellbooks.item.Scroll;
import io.redspace.ironsspellbooks.item.SpellBook;
import io.redspace.ironsspellbooks.network.casting.CancelCastPacket;
import io.redspace.ironsspellbooks.registries.ComponentRegistry;
import io.redspace.ironsspellbooks.registries.MobEffectRegistry;
import io.redspace.ironsspellbooks.render.SpellRenderingHelper;
import io.redspace.ironsspellbooks.spells.blood.RayOfSiphoningSpell;
import io.redspace.ironsspellbooks.spells.ender.RecallSpell;
import io.redspace.ironsspellbooks.spells.fire.BurningDashSpell;
import io.redspace.ironsspellbooks.util.MinecraftInstanceHelper;
import io.redspace.ironsspellbooks.util.ParticleHelper;
import io.redspace.ironsspellbooks.util.TooltipsUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

@EventBusSubscriber(Dist.CLIENT)
public class ClientPlayerEvents {

    @SubscribeEvent
    public static void onCalculatePlayerSpeed(MovementInputUpdateEvent event) {
        if (ClientMagicData.isCasting()) {
            float baseCastingSpeed = 0.2f;
            //due to the way attribute modifiers work, using 0.2 as the base for the attribute means you need +500% movespeed to reach 1.0x movespeed.
            //thus, we abstract the formula to make the values make sense to the player
            //it takes +80% Casting Movespeed to reach maximum speed (zero penalty)
            float castingSpeedModifier = (float) event.getEntity().getAttributeValue(AttributeRegistry.CASTING_MOVESPEED);
            float speed = baseCastingSpeed + castingSpeedModifier - 1;
            event.getInput().forwardImpulse *= speed;
            event.getInput().leftImpulse *= speed;
        }
    }

    @SubscribeEvent
    public static void onPlayerLogOut(ClientPlayerNetworkEvent.LoggingOut event) {
        IronsSpellbooks.LOGGER.debug("ClientPlayerNetworkEvent onPlayerLogOut");
        DeadKingMusicManager.hardStop();
        GuidingBoltManager.handleClientLogout();
        ClientMagicData.spellSelectionManager = null;
        if (event.getPlayer() != null) {
            ClientMagicData.resetClientCastState(event.getPlayer().getUUID());
        }
    }

    @SubscribeEvent
    public static void onPlayerOpenScreen(ScreenEvent.Opening event) {
        if (ClientMagicData.isCasting()) {
            PacketDistributor.sendToServer(new CancelCastPacket(SpellRegistry.getSpell(ClientMagicData.getCastingSpellId()).getCastType() == CastType.CONTINUOUS));
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Pre event) {
        if (event.getEntity() == Minecraft.getInstance().player) {
            var level = Minecraft.getInstance().level;

            ClientMagicData.getRecasts().tickRecasts();
            ClientMagicData.getCooldowns().tick(1);
            if (ClientMagicData.getCastDuration() > 0) {
                ClientMagicData.handleCastDuration();
            }

            if (level != null) {
                List<Entity> spellcasters = level.getEntities((Entity) null, event.getEntity().getBoundingBox().inflate(64), (mob) -> mob instanceof Player || mob instanceof IMagicEntity);
                spellcasters.forEach((entity) -> {
                    LivingEntity livingEntity = (LivingEntity) entity;
                    var spellData = ClientMagicData.getSyncedSpellData(livingEntity);
                    /*
                    Status Effect Visuals
                     */
                    if (spellData.hasEffect(SyncedSpellData.ABYSSAL_SHROUD)) {
                        AbyssalShroudEffect.ambientParticles(level, livingEntity);
                    }
                    if (spellData.hasEffect(SyncedSpellData.ASCENSION)) {
                        AscensionEffect.ambientParticles(level, livingEntity);
                    }
                    if (livingEntity.isAutoSpinAttack() && spellData.getSpinAttackType() == SpinAttackType.FIRE) {
                        BurningDashSpell.ambientParticles(level, livingEntity);
                    }
                    /*
                    Current Casting Spell Visuals
                     */
                    //TODO: what is this, shouldnt there be an onClientCastTick?
                    if (spellData.isCasting()) {
                        if (spellData.getCastingSpellId().equals(SpellRegistry.RAY_OF_SIPHONING_SPELL.get().getSpellId())) {
                            Vec3 impact = Utils.raycastForEntity(entity.level, entity, RayOfSiphoningSpell.getRange(0), true).getLocation().subtract(0, .25, 0);
                            for (int i = 0; i < 8; i++) {
                                Vec3 motion = new Vec3(
                                        Utils.getRandomScaled(.2f),
                                        Utils.getRandomScaled(.2f),
                                        Utils.getRandomScaled(.2f)
                                );
                                entity.level.addParticle(ParticleHelper.SIPHON, impact.x + motion.x, impact.y + motion.y, impact.z + motion.z, motion.x, motion.y, motion.z);
                            }
                        } else if (spellData.getCastingSpellId().equals(SpellRegistry.RECALL_SPELL.get().getSpellId())) {
                            RecallSpell.ambientParticles(livingEntity, spellData);
                        }
                    }
                });
            }

        }
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof LocalPlayer player) {
            ClientMagicData.spellSelectionManager = new SpellSelectionManager(player);
        }
    }

    @SubscribeEvent
    public static void beforeLivingRender(RenderLivingEvent.Pre<? extends LivingEntity, ? extends EntityModel<? extends LivingEntity>> event) {
        var player = Minecraft.getInstance().player;
        if (player == null)
            return;

        var livingEntity = event.getEntity();
        if (livingEntity instanceof Player || livingEntity instanceof IMagicEntity) {

            var syncedData = ClientMagicData.getSyncedSpellData(livingEntity);
            if (syncedData.hasEffect(SyncedSpellData.TRUE_INVIS) && livingEntity.isInvisibleTo(player)) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void afterLivingRender(RenderLivingEvent.Post<? extends LivingEntity, ? extends EntityModel<? extends LivingEntity>> event) {
        var livingEntity = event.getEntity();
        if (livingEntity instanceof Player) {
            var syncedData = ClientMagicData.getSyncedSpellData(livingEntity);
            if (syncedData.isCasting()) {
                SpellRenderingHelper.renderSpellHelper(syncedData, livingEntity, event.getPoseStack(), event.getMultiBufferSource(), event.getPartialTick());
            }
        }
    }

    @SubscribeEvent
    public static void imbuedWeaponTooltips(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();

        if (stack.getItem() instanceof Scroll) return;

        /*
        Universal info to display:
        - Unique Info
        - Cast Time
        - Mana Cost
        - Cooldown Time
        Scrolls show:
        - Level w/ rarity
        - School
        Spellbooks and Imbued weapons show:
        - [*name* *lvl*]
         */
        MinecraftInstanceHelper.ifPlayerPresent((player1) -> {
            var player = (LocalPlayer) player1;
            var lines = event.getToolTip();
            boolean advanced = event.getFlags().isAdvanced();
            // Active Spell Tooltip
            if (stack.has(ComponentRegistry.CASTING_IMPLEMENT)) {
                handleCastingImplementTooltip(stack, player, lines, advanced);
            }
            // Imbued Spell Tooltip
            if (ISpellContainer.isSpellContainer(stack) && !(stack.getItem() instanceof SpellBook)) {
                handleImbuedSpellTooltip(stack, player, lines, advanced);
            }
            // "Can be Imbued" tooltip
            if (ISpellContainer.isSpellContainer(stack) && Utils.canImbue(stack)) {
                var spellContainer = ISpellContainer.get(stack);
//                if (spellContainer.getActiveSpellCount() < spellContainer.getMaxSpellCount()) {
//                    var component = Component.translatable("tooltip.irons_spellbooks.can_be_imbued", spellContainer.getActiveSpellCount(), spellContainer.getMaxSpellCount());
//                    component.setStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW));
//                    additionalLines.add(component);
//                }
                lines.add(1, Component.translatable("tooltip.irons_spellbooks.can_be_imbued_frame", Component.translatable("tooltip.irons_spellbooks.can_be_imbued_number", spellContainer.getActiveSpellCount(), spellContainer.getMaxSpellCount()).withStyle(ChatFormatting.YELLOW)).withStyle(ChatFormatting.GOLD));
            }
            if (stack.has(ComponentRegistry.MULTIHAND_WEAPON)) {
                Predicate<Holder<Attribute>> predicate = ServerConfigs.APPLY_ALL_MULTIHAND_ATTRIBUTES.get() ? Utils.NON_BASE_ATTRIBUTES : Utils.ONLY_MAGIC_ATTRIBUTES;
                int i = TooltipsUtils.indexOfComponent(lines, "item.modifiers.mainhand");
                if (i >= 0) {
                    int endIndex = 0;
                    List<Integer> linesToGrab = new ArrayList<>();
                    for (int j = i; j < lines.size(); j++) {
                        var contents = lines.get(j).getContents();
                        if (contents instanceof TranslatableContents translatableContents) {
                            //IronsSpellbooks.LOGGER.debug("FormatMultiTooltip translatableContents {}/{} :{}", j, lines.size(), translatableContents.getKey());
                            if (translatableContents.getKey().startsWith("attribute.modifier")) {
                                //IronsSpellbooks.LOGGER.debug("FormatMultiTooltip attribute line: {} | args: {}", lines.get(j).getString(), translatableContents.getArgs());
                                endIndex = j;
                                for (Object arg : translatableContents.getArgs()) {
                                    if (arg instanceof Component component && component.getContents() instanceof TranslatableContents translatableContents2) {
                                        //IronsSpellbooks.LOGGER.debug("attribute.modifier arg translatable key: {} ({})", translatableContents2.getKey(), getAttributeForDescriptionId(translatableContents2.getKey()));
                                        var atr = getAttributeForDescriptionId(translatableContents2.getKey());
                                        if (atr != null && predicate.test(BuiltInRegistries.ATTRIBUTE.wrapAsHolder(atr))) {
                                            linesToGrab.add(j);
                                        }
                                    }
                                }
                            } else if (i != j && translatableContents.getKey().startsWith("item.modifiers")) {
                                break;
                            }
                        } else {
                            //Based on the ItemStack tooltip code, the only attributes getting here should be the base UUID attributes
                            for (Component line : lines.get(j).getSiblings()) {
                                if (line.getContents() instanceof TranslatableContents translatableContents) {
                                    if (translatableContents.getKey().startsWith("attribute.modifier")) {
                                        endIndex = j;
                                    }
                                }
                            }
                        }
                    }
                    //IronsSpellbooks.LOGGER.debug("FormatMultiTooltip: lines to grab: {}", linesToGrab);
                    if (!linesToGrab.isEmpty()) {
                        //IronsSpellbooks.LOGGER.debug("FormatMultiTooltip: end index: {} ({})", endIndex, lines.get(endIndex));
                        lines.add(++endIndex, Component.empty());
                        lines.add(++endIndex, Component.translatable("tooltip.irons_spellbooks.modifiers.multihand").withStyle(lines.get(i).getStyle()));
                        for (Integer index : linesToGrab) {
                            lines.add(++endIndex, lines.get(index));
                        }
                        for (int j = linesToGrab.size() - 1; j >= 0; j--) {
                            lines.remove((int) linesToGrab.get(j));
                        }
                    }
                }
            }
        });
    }

    private static void handleImbuedSpellTooltip(ItemStack stack, LocalPlayer player, List<Component> lines, boolean advanced) {
        var spellContainer = ISpellContainer.get(stack);
        int i = advanced ? TooltipsUtils.indexOfAdvancedText(lines, stack) : lines.size();
        if (!spellContainer.isEmpty()) {
            var additionalLines = new ArrayList<Component>();

            spellContainer.getActiveSpells().forEach(spellSlot -> {
                var spellTooltip = TooltipsUtils.formatActiveSpellTooltip(stack, spellSlot.spellData(), CastSource.SWORD, player);
                //Indent the title because we'll have an additional header
                spellTooltip.set(1, Component.literal(" ").append(spellTooltip.get(1)));
                additionalLines.addAll(spellTooltip);
            });
            //Add header to sword tooltip
            additionalLines.add(1, Component.translatable("tooltip.irons_spellbooks.imbued_tooltip").withStyle(ChatFormatting.GRAY));
            lines.addAll(i < 0 ? lines.size() : i, additionalLines);
        }
    }

    private static void handleCastingImplementTooltip(ItemStack stack, LocalPlayer player, List<Component> lines, boolean advanced) {
        var spellSlot = ClientMagicData.getSpellSelectionManager().getSelection();
        if (spellSlot != null && spellSlot.spellData != SpellData.EMPTY) {
            var additionalLines = TooltipsUtils.formatActiveSpellTooltip(stack, spellSlot.spellData, spellSlot.getCastSource(), player);
            //Add header
            additionalLines.add(1, Component.translatable("tooltip.irons_spellbooks.casting_implement_tooltip").withStyle(ChatFormatting.GRAY));
            //Indent the title because we have an additional header
            additionalLines.set(2, Component.literal(" ").append(additionalLines.get(2)));
            //Keybind notification
            additionalLines.add(Component.literal(" ").append(Component.translatable("tooltip.irons_spellbooks.press_to_cast_active", Component.keybind("key.use")).withStyle(ChatFormatting.GOLD)));
            int i = advanced ? TooltipsUtils.indexOfAdvancedText(lines, stack) : lines.size();
            lines.addAll(i < 0 ? lines.size() : i, additionalLines);
        }
    }

    private static Attribute getAttributeForDescriptionId(String descriptionId) {
        return BuiltInRegistries.ATTRIBUTE.stream().filter(attribute -> attribute.getDescriptionId().equals(descriptionId)).findFirst().orElse(null);
    }

    @SubscribeEvent
    public static void customPotionTooltips(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        var potionData = stack.get(DataComponents.POTION_CONTENTS);
        if (potionData != null) {
            potionData.getAllEffects().forEach(mobEffectInstance -> {
                if (mobEffectInstance.getEffect().value() instanceof CustomDescriptionMobEffect customDescriptionMobEffect) {
                    CustomDescriptionMobEffect.handleCustomPotionTooltip(stack, event.getToolTip(), event.getFlags().isAdvanced(), mobEffectInstance, customDescriptionMobEffect);
                }
            });
        }
    }

    @SubscribeEvent
    public static void changeFogColor(ViewportEvent.ComputeFogColor event) {
        if (Minecraft.getInstance().player != null && Minecraft.getInstance().player.hasEffect(MobEffectRegistry.PLANAR_SIGHT)) {
            var color = MobEffectRegistry.PLANAR_SIGHT.get().getColor();
            float f = 0.0F;
            float f1 = 0.0F;
            float f2 = 0.0F;

            f += (float) ((color >> 16 & 255)) / 255.0F;
            f1 += (float) ((color >> 8 & 255)) / 255.0F;
            f2 += (float) ((color >> 0 & 255)) / 255.0F;
            event.setRed(f * .15f);
            event.setGreen(f1 * .15f);
            event.setBlue(f2 * .15f);
        }
    }
}