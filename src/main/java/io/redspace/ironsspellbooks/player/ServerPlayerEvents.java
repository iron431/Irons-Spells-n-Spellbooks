package io.redspace.ironsspellbooks.player;

import io.redspace.ironsspellbooks.IronsSpellbooks;
import io.redspace.ironsspellbooks.api.entity.IMagicEntity;
import io.redspace.ironsspellbooks.api.events.SpellTeleportEvent;
import io.redspace.ironsspellbooks.api.item.UpgradeData;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.api.util.CameraShakeManager;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.block.BloodCauldronBlock;
import io.redspace.ironsspellbooks.capabilities.magic.RecastResult;
import io.redspace.ironsspellbooks.capabilities.magic.SyncedSpellData;
import io.redspace.ironsspellbooks.compat.tetra.TetraProxy;
import io.redspace.ironsspellbooks.config.ServerConfigs;
import io.redspace.ironsspellbooks.data.IronsDataStorage;
import io.redspace.ironsspellbooks.datagen.DamageTypeTagGenerator;
import io.redspace.ironsspellbooks.effect.AbyssalShroudEffect;
import io.redspace.ironsspellbooks.effect.EvasionEffect;
import io.redspace.ironsspellbooks.effect.IMobEffectEndCallback;
import io.redspace.ironsspellbooks.effect.SummonTimer;
import io.redspace.ironsspellbooks.entity.mobs.IMagicSummon;
import io.redspace.ironsspellbooks.entity.spells.root.PreventDismount;
import io.redspace.ironsspellbooks.item.CastingItem;
import io.redspace.ironsspellbooks.item.Scroll;
import io.redspace.ironsspellbooks.network.EquipmentChangedPacket;
import io.redspace.ironsspellbooks.network.SyncManaPacket;
import io.redspace.ironsspellbooks.registries.BlockRegistry;
import io.redspace.ironsspellbooks.registries.ComponentRegistry;
import io.redspace.ironsspellbooks.registries.ItemRegistry;
import io.redspace.ironsspellbooks.registries.MobEffectRegistry;
import io.redspace.ironsspellbooks.util.MinecraftInstanceHelper;
import io.redspace.ironsspellbooks.util.ModTags;
import io.redspace.ironsspellbooks.util.UpgradeUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.util.StringUtil;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AnvilUpdateEvent;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;
import net.neoforged.neoforge.event.entity.EntityMountEvent;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.entity.living.*;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.event.CurioAttributeModifierEvent;
import top.theillusivec4.curios.api.event.CurioChangeEvent;

@EventBusSubscriber
public class ServerPlayerEvents {

    //    @SubscribeEvent
//    public static void onPlayerAttack(AttackEntityEvent event) {
//        TODO: this only gets called when the player successfully hits something. we want it to cancel if they even try.
//              granted, the input even should be cancelled already, but better combat skips that due to custom weapon handling.
//        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
//            if (PlayerMagicData.getPlayerMagicData(serverPlayer).isCasting()) {
//                Utils.serverSideCancelCast(serverPlayer);
//            }
//        }
//    }


    @SubscribeEvent
    public static void onUseItem(PlayerInteractEvent.RightClickItem event) {
        var player = event.getEntity();
        if (player.level.isClientSide) {
            MinecraftInstanceHelper.ifPlayerPresent(localPlayer -> {
                if (ClientMagicData.isCasting() && player.getUUID().equals(localPlayer.getUUID())) {
                    event.setCanceled(true);
                }
            });
        } else {
            var magicData = MagicData.getPlayerMagicData(player);
            if (magicData.isCasting() && event.getItemStack() != magicData.getPlayerCastingItem()) {
                event.setCanceled(true);
            }
        }
        if (event.isCanceled()) {
            return;
        }

        var level = player.level;
        var hand = event.getHand();
        ItemStack itemStack = player.getItemInHand(hand);
        if (itemStack.has(ComponentRegistry.CASTING_IMPLEMENT)) {
            SpellSelectionManager spellSelectionManager = new SpellSelectionManager(player);
            SpellSelectionManager.SelectionOption selectionOption = spellSelectionManager.getSelection();
            if (selectionOption == null || selectionOption.spellData.equals(SpellData.EMPTY)) {
                //IronsSpellbooks.LOGGER.debug("CastingItem.Use.1 {} {}", level.isClientSide, hand);
                return;
            }
            SpellData spellData = selectionOption.spellData;
            int spellLevel = spellData.getSpell().getLevelFor(spellData.getLevel(), player);
            if (level.isClientSide()) {
                if (ClientMagicData.isCasting()) {
                    //IronsSpellbooks.LOGGER.debug("CastingItem.Use.2 {} {}", level.isClientSide, hand);
                    event.setCancellationResult(InteractionResult.CONSUME);
                } else if (ClientMagicData.getPlayerMana() < spellData.getSpell().getManaCost(spellLevel)
                        || ClientMagicData.getCooldowns().isOnCooldown(spellData.getSpell())
                        || !ClientMagicData.getSyncedSpellData(player).isSpellLearned(spellData.getSpell())) {
                    //IronsSpellbooks.LOGGER.debug("CastingItem.Use.3 {} {}", level.isClientSide, hand);
                    return;
                } else {
                    //IronsSpellbooks.LOGGER.debug("CastingItem.Use.4 {} {}", level.isClientSide, hand);
                    event.setCancellationResult(InteractionResult.CONSUME);
                }
            }

            var castingSlot = hand.ordinal() == 0 ? SpellSelectionManager.MAINHAND : SpellSelectionManager.OFFHAND;

            if (spellData.getSpell().attemptInitiateCast(itemStack, spellLevel, level, player, selectionOption.getCastSource(), true, castingSlot)) {
                event.setCancellationResult(InteractionResult.CONSUME);
            } else {
                //IronsSpellbooks.LOGGER.debug("CastingItem.Use.6 {} {}", level.isClientSide, hand);
                event.setCancellationResult(InteractionResult.FAIL);
            }
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onPlayerDropItem(ItemTossEvent event) {
        var itemStack = event.getEntity().getItem();
        if (itemStack.getItem() instanceof Scroll) {
            var magicData = MagicData.getPlayerMagicData(event.getPlayer());
            if (magicData.isCasting() && magicData.getCastSource() == CastSource.SCROLL) {
                if (magicData.getCastType() == CastType.CONTINUOUS) {
                    itemStack.shrink(1);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onLevelLoaded(LevelEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel serverLevel && serverLevel.dimension() == Level.OVERWORLD) {
            IronsDataStorage.init(serverLevel.getDataStorage());
        }
    }

    @SubscribeEvent
    public static void onServerStoppedEvent(ServerStoppedEvent event) {
        IronsSpellbooks.MCS = null;
        IronsSpellbooks.OVERWORLD = null;
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        IronsSpellbooks.MCS = event.getServer();
        IronsSpellbooks.OVERWORLD = IronsSpellbooks.MCS.overworld();
    }

    //TODO: 1.21: clean out world upgrader
//    @SubscribeEvent
//    public static void onServerAboutToStart(ServerAboutToStartEvent event) {
//        DataFixerStorage.init(event.getServer().storageSource);
//
//        if (ServerConfigs.RUN_WORLD_UPGRADER.get()) {
//            var server = event.getServer();
//            new IronsWorldUpgrader(server.storageSource, server.registries()).runUpgrade();
//        }
//    }

    @SubscribeEvent
    public static void onLivingEquipmentChangeEvent(LivingEquipmentChangeEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            var playerMagicData = MagicData.getPlayerMagicData(serverPlayer);

            if (playerMagicData.isCasting() && (event.getFrom().getItem() instanceof CastingItem || event.getTo().getItem() instanceof CastingItem)) {
                Utils.serverSideCancelCast(serverPlayer);
                PacketDistributor.sendToPlayer(serverPlayer, new EquipmentChangedPacket());
                return;
            }

            var isFromSpellContainer = ISpellContainer.isSpellContainer(event.getFrom());
            if (isFromSpellContainer && ISpellContainer.get(event.getFrom()).getIndexForSpell(playerMagicData.getCastingSpell().getSpell()) >= 0) {
                IronsSpellbooks.LOGGER.debug("onLivingEquipmentChangeEvent from:\n{}\n{}", event.getFrom().toString(), Integer.toHexString(event.getFrom().hashCode()));
                IronsSpellbooks.LOGGER.debug("onLivingEquipmentChangeEvent to:\n{}\n{}", event.getTo().toString(), Integer.toHexString(event.getTo().hashCode()));
                if (playerMagicData.isCasting()) {
                    Utils.serverSideCancelCast(serverPlayer);
                }
                PacketDistributor.sendToPlayer(serverPlayer, new EquipmentChangedPacket());
            } else if (isFromSpellContainer || ISpellContainer.isSpellContainer(event.getTo())) {
                PacketDistributor.sendToPlayer(serverPlayer, new EquipmentChangedPacket());
            }
        }
    }

    @SubscribeEvent
    public static void onCurioChangeEvent(CurioChangeEvent event) {
        var entity = event.getEntity();
        if (entity instanceof ServerPlayer serverPlayer && (ISpellContainer.isSpellContainer(event.getFrom()) || ISpellContainer.isSpellContainer(event.getTo()))) {
            PacketDistributor.sendToPlayer(serverPlayer, new EquipmentChangedPacket());
        }
    }

    @SubscribeEvent
    public static void onPlayerLogOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            Utils.serverSideCancelCast(serverPlayer);
        }
    }

    @SubscribeEvent
    public static void onPlayerOpenContainer(PlayerContainerEvent.Open event) {
        if (event.getEntity().level.isClientSide) {
            return;
        }
        //Ironsspellbooks.logger.debug("onPlayerOpenContainer {} {}", event.getEntity().getName().getString(), event.getContainer().getType());

        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            var playerMagicData = MagicData.getPlayerMagicData(serverPlayer);
            if (playerMagicData.isCasting()) {
                Utils.serverSideCancelCast(serverPlayer);
            }
        }
    }

    @SubscribeEvent
    public static void handleUpgradeModifiers(ItemAttributeModifierEvent event) {
        UpgradeData upgradeData = UpgradeData.getUpgradeData(event.getItemStack());
        if (upgradeData != UpgradeData.NONE) {
            try {
                var equipmentSlot = EquipmentSlot.byName(upgradeData.getUpgradedSlot());
                var groupSlot = EquipmentSlotGroup.bySlot(equipmentSlot);
                UpgradeUtils.handleAttributeEvent(event.getModifiers(), upgradeData, (atr, mod) -> event.addModifier(atr, mod, groupSlot), (atr, mod) -> event.removeModifier(atr, mod.id()), upgradeData.getUpgradedSlot());
            } catch (IllegalArgumentException e) {
                return;
            }
        }
    }

    @SubscribeEvent
    public static void handleCurioUpgradeModifiers(CurioAttributeModifierEvent event) {
        UpgradeData upgradeData = UpgradeData.getUpgradeData(event.getItemStack());
        if (upgradeData != UpgradeData.NONE && upgradeData.getUpgradedSlot().equals(event.getSlotContext().identifier())) {
//        IronsSpellbooks.LOGGER.debug("handleCurioUpgradeModifiers slot: {} uuid: {}",event.getSlotContext().getIdentifier(), event.getUuid());
            var list = event.getModifiers().entries().stream().map(entry -> new ItemAttributeModifiers.Entry(entry.getKey(), entry.getValue(), EquipmentSlotGroup.ANY)).toList();
            UpgradeUtils.handleAttributeEvent(list, upgradeData, event::addModifier, event::removeModifier, event.getSlotContext().identifier());
        }
    }

    @SubscribeEvent
    public static void onExperienceDroppedEvent(LivingExperienceDropEvent event) {
        var player = event.getAttackingPlayer();
        if (player != null) {
            int ringCount = CuriosApi.getCuriosInventory(player).map(inventory -> inventory.findCurios(ItemRegistry.EMERALD_STONEPLATE_RING.get()).size()).orElse(0);
            for (int i = 0; i < ringCount; i++) {
                event.setDroppedExperience((int) (event.getDroppedExperience() * 1.25));
            }
        }
    }

    @SubscribeEvent
    public static void onStartTracking(final PlayerEvent.StartTracking event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer && event.getTarget() instanceof ServerPlayer targetPlayer) {
            MagicData.getPlayerMagicData(serverPlayer).getSyncedData().syncToPlayer(targetPlayer);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            var playerMagicData = MagicData.getPlayerMagicData(serverPlayer);
            playerMagicData.getPlayerCooldowns().syncToPlayer(serverPlayer);
            playerMagicData.getPlayerRecasts().syncAllToPlayer();
            playerMagicData.getSyncedData().syncToPlayer(serverPlayer);
            PacketDistributor.sendToPlayer(serverPlayer, new SyncManaPacket(playerMagicData));
            CameraShakeManager.doSync(serverPlayer);
        }
    }

    @SubscribeEvent
    public static void onLivingDeathEvent(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            Utils.serverSideCancelCast(serverPlayer);
            MagicData.getPlayerMagicData(serverPlayer).getPlayerRecasts().removeAll(RecastResult.DEATH);
            serverPlayer.getActiveEffects().forEach(mobEffectInstance -> {
                if (mobEffectInstance.getEffect().value() instanceof IMobEffectEndCallback callback) {
                    callback.onEffectRemoved(serverPlayer, mobEffectInstance.getAmplifier());
                }
            });
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onSpellTeleport(SpellTeleportEvent event) {
        if (event.getEntity() instanceof LivingEntity livingEntity) {
            if (ItemRegistry.TELEPORTATION_AMULET.get().isEquippedBy(livingEntity)) {
                livingEntity.addEffect(new MobEffectInstance(MobEffectRegistry.EVASION, 3 * 20, 0, false, false, true));
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerCloned(PlayerEvent.Clone event) {
        if (event.getEntity() instanceof ServerPlayer newServerPlayer && event.isWasDeath()) {
            event.getOriginal().getActiveEffects().forEach((effect -> {
                //IronsSpellbooks.LOGGER.debug("{}", effect.getEffect().getDisplayName().getString());
                if (effect.getEffect() instanceof SummonTimer) {
                    newServerPlayer.addEffect(effect, newServerPlayer);
                }
            }));

            IronsSpellbooks.LOGGER.debug("onPlayerCloned: copy data: client: {}", newServerPlayer.level.isClientSide);
            MagicData oldMagicData = MagicData.getPlayerMagicData(event.getOriginal());
            MagicData newMagicData = MagicData.getPlayerMagicData(newServerPlayer);
            newMagicData.setSyncedData(oldMagicData.getSyncedData().getPersistentData(newServerPlayer));
            oldMagicData.getPlayerCooldowns().getSpellCooldowns().forEach((spellId, cooldown) -> newMagicData.getPlayerCooldowns().getSpellCooldowns().put(spellId, cooldown));
            //newMagicData.getSyncedData().syncToPlayer(newServerPlayer);
        }
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            Utils.serverSideCancelCast(serverPlayer);
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {

            //Clear fire and frozen
            serverPlayer.clearFire();
            serverPlayer.setTicksFrozen(0);
            var data = serverPlayer.getEntityData().packDirty();
            if (data != null) {
                serverPlayer.connection.send(new ClientboundSetEntityDataPacket(serverPlayer.getId(), data));
            }

            //Cancel casting
            Utils.serverSideCancelCast(serverPlayer);

            //Sync effects
            serverPlayer.getActiveEffects().forEach((effect -> {
                if (effect.getEffect() instanceof SummonTimer) {
                    serverPlayer.server.getPlayerList().sendActivePlayerEffects(serverPlayer);
                }
            }));

            //Set respawn mana
            MagicData.getPlayerMagicData(serverPlayer).setMana((int) (serverPlayer.getAttributeValue(AttributeRegistry.MAX_MANA) * ServerConfigs.MANA_SPAWN_PERCENT.get()));
        }
    }

    @SubscribeEvent
    public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
        var livingEntity = event.getEntity();
        //irons_spellbooks.LOGGER.debug("onLivingAttack.1: {}", livingEntity);

        if ((livingEntity instanceof ServerPlayer) || (livingEntity instanceof IMagicEntity)) {
            if (ItemRegistry.FIREWARD_RING.get().isEquippedBy(livingEntity) && event.getSource().is(DamageTypeTags.IS_FIRE)) {
                event.getEntity().clearFire();
                event.setCanceled(true);
                return;
            }
            var playerMagicData = MagicData.getPlayerMagicData(livingEntity);
            if (playerMagicData.getSyncedData().hasEffect(SyncedSpellData.EVASION)) {
                if (EvasionEffect.doEffect(livingEntity, event.getSource())) {
                    event.setCanceled(true);
                    return;
                }
            } else if (playerMagicData.getSyncedData().hasEffect(SyncedSpellData.ABYSSAL_SHROUD)) {
                if (AbyssalShroudEffect.doEffect(livingEntity, event.getSource())) {
                    event.setCanceled(true);
                    return;
                }
            }

            if (livingEntity instanceof ServerPlayer serverPlayer) {
                if (playerMagicData.isCasting() &&
                        playerMagicData.getCastingSpell().getSpell().canBeInterrupted(serverPlayer) &&
                        playerMagicData.getCastDurationRemaining() > 0 &&
                        !event.getSource().is(DamageTypeTagGenerator.LONG_CAST_IGNORE) &&
                        !playerMagicData.popMarkedPoison()) {
                    Utils.serverSideCancelCast(serverPlayer);
                }
            }
        }
        if (ServerConfigs.BETTER_CREEPER_THUNDERHIT.get() && event.getSource().is(DamageTypeTags.IS_FIRE) && event.getEntity() instanceof Creeper creeper && creeper.isPowered()) {
            event.setCanceled(true);
            return;
        }
    }

    @SubscribeEvent
    public static void onBeforeDamageTaken(LivingDamageEvent.Pre event) {
        var livingEntity = event.getEntity();
        if (livingEntity instanceof IMagicEntity || livingEntity instanceof ServerPlayer) {
            var playerMagicData = MagicData.getPlayerMagicData(livingEntity);
            if (playerMagicData.getSyncedData().hasEffect(SyncedSpellData.HEARTSTOP)) {
                playerMagicData.getSyncedData().addHeartstopDamage(event.getOriginalDamage() * .5f);
                event.setNewDamage(0);
            }
        }
    }

    @SubscribeEvent
    public static void onLivingChangeTarget(LivingChangeTargetEvent event) {
        var newTarget = event.getNewAboutToBeSetTarget();
        var entity = event.getEntity();
        if (newTarget != null) {
            //Prevent Village allies (ie preists/iron golems) from aggroing eachother
            if (newTarget.getType().is(ModTags.VILLAGE_ALLIES) && entity.getType().is(ModTags.VILLAGE_ALLIES)) {
                event.setCanceled(true);
                return;
            }
            //Prevent mobs who auto-target hostile mobs from targeting "enemy" summons, unless they are actually fighting
            if (newTarget instanceof IMagicSummon summon && !(entity.equals(((Mob) newTarget).getTarget()))) {
                event.setCanceled(true);
                return;
            }
            if (newTarget.hasEffect(MobEffectRegistry.TRUE_INVISIBILITY)) {
                event.setCanceled(true);
                return;
            }
        }
    }

    @SubscribeEvent
    public static void onLivingDamagePost(LivingDamageEvent.Post event) {
        //TODO: tetra update
        TetraProxy.PROXY.handleLivingAttackEvent(event);
    }

    @SubscribeEvent
    public static void onEntityMountEvent(EntityMountEvent event) {
        if (event.getEntity().level.isClientSide) {
            return;
        }

        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            var playerMagicData = MagicData.getPlayerMagicData(serverPlayer);
            if (playerMagicData.isCasting()) {
                Utils.serverSideCancelCast(serverPlayer);
            }
        }
    }

    @SubscribeEvent
    public static void preventDismount(EntityMountEvent event) {
        if (!event.getEntity().level.isClientSide && event.getEntityBeingMounted() instanceof PreventDismount && event.isDismounting() && !event.getEntityBeingMounted().isRemoved()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onProjectileImpact(ProjectileImpactEvent event) {
        if (event.getRayTraceResult() instanceof EntityHitResult entityHitResult) {
            var victim = entityHitResult.getEntity();
            //IronsSpellbooks.LOGGER.debug("onProjectileImpact: {}", victim);
            if (victim instanceof IMagicEntity || victim instanceof Player) {
                //IronsSpellbooks.LOGGER.debug("onProjectileImpact: is a casting mob");
                var livingEntity = (LivingEntity) victim;
                SyncedSpellData syncedSpellData = livingEntity.level.isClientSide ? ClientMagicData.getSyncedSpellData(livingEntity) : MagicData.getPlayerMagicData(livingEntity).getSyncedData();
                if (syncedSpellData.hasEffect(SyncedSpellData.EVASION)) {
                    //IronsSpellbooks.LOGGER.debug("onProjectileImpact: evasion");
                    if (EvasionEffect.doEffect(livingEntity, victim.damageSources().indirectMagic(event.getProjectile(), event.getProjectile().getOwner()))) {
                        event.setCanceled(true);
                    }
                } else if (syncedSpellData.hasEffect(SyncedSpellData.ABYSSAL_SHROUD)) {
                    //IronsSpellbooks.LOGGER.debug("onProjectileImpact: abyssal shroud");
                    if (AbyssalShroudEffect.doEffect(livingEntity, victim.damageSources().indirectMagic(event.getProjectile(), event.getProjectile().getOwner()))) {
                        event.setCanceled(true);
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void useOnEntityEvent(PlayerInteractEvent.EntityInteractSpecific event) {
        if (event.getTarget() instanceof Creeper creeper) {
            var player = event.getEntity();
            var useItem = player.getItemInHand(event.getHand());
            if (useItem.is(Items.GLASS_BOTTLE) && creeper.isPowered()) {
                creeper.hurt(creeper.damageSources().generic(), 5);
                player.level.playSound((Player) null, player.getX(), player.getY(), player.getZ(), SoundEvents.BOTTLE_FILL_DRAGONBREATH, SoundSource.NEUTRAL, 1.0F, 1.0F);
                player.swing(event.getHand());
                event.setCancellationResult(InteractionResultHolder.consume(ItemUtils.createFilledResult(useItem, player, new ItemStack(ItemRegistry.LIGHTNING_BOTTLE.get()))).getResult());
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void handleResistanceAttributesOnSpawn(FinalizeSpawnEvent event) {
        var mob = event.getEntity();
        //Attributes should never be null because all living entities have these attributes
        if (mob.getType().is(EntityTypeTags.UNDEAD)) {
            //Undead take extra holy damage, and less blood (necromantic) damage
            setIfNonNull(mob, AttributeRegistry.HOLY_MAGIC_RESIST, 0.5);
            setIfNonNull(mob, AttributeRegistry.BLOOD_MAGIC_RESIST, 1.5);
        } else if (mob.getType().is(EntityTypeTags.SENSITIVE_TO_IMPALING)) {
            //Water mobs take extra lightning damage
            setIfNonNull(mob, AttributeRegistry.LIGHTNING_MAGIC_RESIST, 0.5);
        }
        if (mob.fireImmune()) {
            //Fire immune (blazes, pyromancer, etc) take 50% fire damage
            setIfNonNull(mob, AttributeRegistry.FIRE_MAGIC_RESIST, 1.5);
        }
        //TODO: replace this with "fire_elemental" entity tag for all fiery mobs (blaze, magma cubes, modded mobs)
        if (mob.getType() == EntityType.BLAZE) {
            setIfNonNull(mob, AttributeRegistry.ICE_MAGIC_RESIST, 0.5);
        }
    }

    private static void setIfNonNull(LivingEntity mob, Holder<Attribute> attribute, double value) {
        var instance = mob.getAttributes().getInstance(attribute);
        if (instance != null) {
            instance.setBaseValue(value);
        }
    }

    @SubscribeEvent
    public static void onLivingTick(EntityTickEvent.Pre event) {
        var entity = event.getEntity();
        var level = entity.level;
        if (!level.isClientSide) {
            if (entity.tickCount % 40 == 0) {
                BlockPos pos = entity.blockPosition();
                BlockState blockState = entity.level.getBlockState(pos);
                if (blockState.is(Blocks.CAULDRON)) {
                    BloodCauldronBlock.attemptCookEntity(blockState, entity.level, pos, entity, () -> {
                        level.setBlockAndUpdate(pos, BlockRegistry.BLOOD_CAULDRON_BLOCK.get().defaultBlockState());
                        level.gameEvent(null, GameEvent.BLOCK_CHANGE, pos);
                    });
                }
            }
        }
    }

    @SubscribeEvent
    public static void onAnvilRecipe(AnvilUpdateEvent event) {
        //IronsSpellbooks.LOGGER.debug("onAnvilRecipe");
        if (event.getRight().is(ItemRegistry.SHRIVING_STONE.get())) {
            //IronsSpellbooks.LOGGER.debug("shriving stone");
            var result = Utils.handleShriving(event.getLeft());
            if (!result.isEmpty()) {
                var itemName = event.getName();
                if (itemName != null && !StringUtil.isBlank(itemName)) {
                    if (!itemName.equals(result.getHoverName().getString())) {
                        result.set(DataComponents.CUSTOM_NAME, Component.literal(itemName));
                    }
                } else if (result.has(DataComponents.CUSTOM_NAME)) {
                    result.remove(DataComponents.CUSTOM_NAME);
                }
                event.setOutput(result);
                event.setCost(1);
                event.setMaterialCost(1);
            }
        }
    }

    @SubscribeEvent
    public static void changeDigSpeed(PlayerEvent.BreakSpeed event) {
        //This event is getting run on the server and the client, and because the client is aware of its own status effects, this works
        //(If it did not get run on the client, then breaking particles would not match)
        var player = event.getEntity();
        if (player.hasEffect(MobEffectRegistry.HASTENED)) {
            int i = 1 + player.getEffect(MobEffectRegistry.HASTENED).getAmplifier();
            event.setNewSpeed(event.getNewSpeed() * Utils.intPow(1.2f, i));
        }
        if (player.hasEffect(MobEffectRegistry.SLOWED)) {
            int i = 1 + player.getEffect(MobEffectRegistry.SLOWED).getAmplifier();
            event.setNewSpeed(event.getNewSpeed() * Utils.intPow(.8f, i));
        }
    }

    @SubscribeEvent
    public static void changeBreedOutcome(BabyEntitySpawnEvent event) {
        if (ServerConfigs.HOGLIN_OFFSPRING_PROTECTION.get()) {
            if (event.getChild() instanceof Hoglin baby && event.getParentA() instanceof Hoglin parent1 && event.getParentB() instanceof Hoglin parent2) {
                double i = (parent1.isImmuneToZombification() ? 0.5 : 0) + (parent2.isImmuneToZombification() ? 0.5 : 0);
                //produces: 0% if neither, 50% if 1, 100% if both
                if (Utils.random.nextFloat() < i) {
                    baby.setImmuneToZombification(true);
                }
            }
        }
    }
}
