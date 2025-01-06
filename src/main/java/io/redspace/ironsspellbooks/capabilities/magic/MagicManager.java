package io.redspace.ironsspellbooks.capabilities.magic;

import io.redspace.ironsspellbooks.IronsSpellbooks;
import io.redspace.ironsspellbooks.api.events.SpellPreCastEvent;
import io.redspace.ironsspellbooks.api.magic.IMagicManager;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastResult;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.config.ServerConfigs;
import io.redspace.ironsspellbooks.item.Scroll;
import io.redspace.ironsspellbooks.network.SyncManaPacket;
import io.redspace.ironsspellbooks.network.casting.OnCastStartedPacket;
import io.redspace.ironsspellbooks.network.casting.SyncCooldownPacket;
import io.redspace.ironsspellbooks.network.casting.UpdateCastingStatePacket;
import io.redspace.ironsspellbooks.spells.PlayerCastContext;
import io.redspace.ironsspellbooks.util.Log;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;

import static io.redspace.ironsspellbooks.api.registry.AttributeRegistry.*;

public class MagicManager implements IMagicManager {
    public static final int MANA_REGEN_TICKS = 10;
    public static final int CONTINUOUS_CAST_TICK_INTERVAL = 10;

    public boolean regenPlayerMana(ServerPlayer serverPlayer, MagicData playerMagicData) {
        int playerMaxMana = (int) serverPlayer.getAttributeValue(MAX_MANA);
        var mana = playerMagicData.getMana();
        if (mana != playerMaxMana) {
            float playerManaRegenMultiplier = (float) serverPlayer.getAttributeValue(MANA_REGEN);
            var increment = playerMaxMana * playerManaRegenMultiplier * .01f * ServerConfigs.MANA_REGEN_MULTIPLIER.get().floatValue();
            playerMagicData.setMana(Mth.clamp(playerMagicData.getMana() + increment, 0, playerMaxMana));
            return true;
        } else {
            return false;
        }
    }


    public void tick(Level level) {
        boolean doManaRegen = level.getServer().getTickCount() % MANA_REGEN_TICKS == 0;

        level.players().stream().toList().forEach(player -> {
            if (player instanceof ServerPlayer serverPlayer) {
                MagicData playerMagicData = MagicData.getMagicData(serverPlayer);
                playerMagicData.getPlayerCooldowns().tick(1);
                playerMagicData.getPlayerRecasts().tick(2);

                if (playerMagicData.isCasting()) {
                    playerMagicData.handleCastDuration();
                    var spell = SpellRegistry.getSpell(playerMagicData.getCastingSpellId());
                    if ((spell.getCastType() == CastType.LONG && !serverPlayer.isUsingItem()) || spell.getCastType() == CastType.INSTANT) {
                        if (playerMagicData.getCastDurationRemaining() <= 0) {
                            spell.castSpell(serverPlayer.level, playerMagicData.getCastingSpellLevel(), serverPlayer, playerMagicData.getCastSource(), true);
                            if (playerMagicData.getCastSource() == CastSource.SCROLL) {
                                Scroll.attemptRemoveScrollAfterCast(serverPlayer);
                            }
                            spell.onServerCastComplete(serverPlayer.level, playerMagicData.getCastingSpellLevel(), serverPlayer, playerMagicData, false);
                        }
                    } else if (spell.getCastType() == CastType.CONTINUOUS) {
                        if ((playerMagicData.getCastDurationRemaining() + 1) % CONTINUOUS_CAST_TICK_INTERVAL == 0) {
                            if (playerMagicData.getCastDurationRemaining() < CONTINUOUS_CAST_TICK_INTERVAL || (playerMagicData.getCastSource().consumesMana() && playerMagicData.getMana() - spell.getManaCost(playerMagicData.getCastingSpellLevel()) * 2 < 0)) {
                                spell.castSpell(serverPlayer.level, playerMagicData.getCastingSpellLevel(), serverPlayer, playerMagicData.getCastSource(), true);

                                if (playerMagicData.getCastSource() == CastSource.SCROLL) {
                                    Scroll.attemptRemoveScrollAfterCast(serverPlayer);
                                }

                                spell.onServerCastComplete(serverPlayer.level, playerMagicData.getCastingSpellLevel(), serverPlayer, playerMagicData, false);

                            } else {
                                spell.castSpell(serverPlayer.level, playerMagicData.getCastingSpellLevel(), serverPlayer, playerMagicData.getCastSource(), false);
                            }
                        }
                    }

                    if (playerMagicData.isCasting()) {
                        spell.onServerCastTick(serverPlayer.level, playerMagicData.getCastingSpellLevel(), serverPlayer, playerMagicData);
                    }
                }

                if (doManaRegen) {
                    if (regenPlayerMana(serverPlayer, playerMagicData)) {
                        PacketDistributor.sendToPlayer(serverPlayer, new SyncManaPacket(playerMagicData));
                    }
                }
            }
        });
    }

    public void addCooldown(ServerPlayer serverPlayer, AbstractSpell spell, CastSource castSource) {
        if (castSource == CastSource.SCROLL)
            return;
        int effectiveCooldown = getEffectiveSpellCooldown(spell, serverPlayer, castSource);

        MagicData.getMagicData(serverPlayer).getPlayerCooldowns().addCooldown(spell, effectiveCooldown);
        PacketDistributor.sendToPlayer(serverPlayer, new SyncCooldownPacket(spell.getSpellId(), effectiveCooldown));
    }

    public void clearCooldowns(ServerPlayer serverPlayer) {
        MagicData.getMagicData(serverPlayer).getPlayerCooldowns().clearCooldowns();
        MagicData.getMagicData(serverPlayer).getPlayerCooldowns().syncToPlayer(serverPlayer);
    }

    /**
     * returns true/false for success/failure to cast
     */
    public static boolean attemptInitiateCast(AbstractSpell abstractSpell, ItemStack stack, Level level, PlayerCastContext playerCastContext) {
        if (Log.SPELL_DEBUG) {
            IronsSpellbooks.LOGGER.debug("AbstractSpell.attemptInitiateCast isClient:{}, spell{}({})", level.isClientSide, abstractSpell.getSpellId(), playerCastContext.getSpellLevel());
        }

        if (level.isClientSide) {
            return false;
        }

        var serverPlayer = (ServerPlayer) playerCastContext.getPlayer();
        var magicData = playerCastContext.getMagicData();

        if (!magicData.isCasting()) {
            var spellId = abstractSpell.getSpellId();
            CastResult castResult = abstractSpell.canBeCastedBy(playerCastContext);
            if (castResult.message != null) {
                serverPlayer.connection.send(new ClientboundSetActionBarTextPacket(castResult.message));
            }

            if (!castResult.isSuccess()
                    || !abstractSpell.checkPreCastConditions(level, playerCastContext)
                    || NeoForge.EVENT_BUS.post(new SpellPreCastEvent(serverPlayer, spellId, playerCastContext.getSpellLevel(), abstractSpell.getSchoolType(), playerCastContext.getCastSource())).isCanceled()) {
                return false;
            }

            if (serverPlayer.isUsingItem()) {
                serverPlayer.stopUsingItem();
            }
            int effectiveCastTime = abstractSpell.getEffectiveCastTime(playerCastContext);

            magicData.initiateCast(abstractSpell, playerCastContext.getSpellLevel(), effectiveCastTime, playerCastContext.getCastSource(), playerCastContext.getCastingEquipmentSlot());
            magicData.setPlayerCastingItem(stack);

            abstractSpell.onServerPreCast(level, playerCastContext);

            PacketDistributor.sendToPlayer(serverPlayer, new UpdateCastingStatePacket(spellId, playerCastContext.getSpellLevel(), effectiveCastTime, playerCastContext.getCastSource(), playerCastContext.getCastingEquipmentSlot()));
            PacketDistributor.sendToPlayersTrackingEntityAndSelf(serverPlayer, new OnCastStartedPacket(serverPlayer.getUUID(), spellId, playerCastContext.getSpellLevel()));

            return true;
        } else {
            Utils.serverSideCancelCast(serverPlayer);
            return false;
        }
    }

    public static int getEffectiveSpellCooldown(AbstractSpell spell, Player player, CastSource castSource) {
        double playerCooldownModifier = player.getAttributeValue(COOLDOWN_REDUCTION);

        float itemCoolDownModifer = 1;
        if (castSource == CastSource.SWORD) {
            itemCoolDownModifer = ServerConfigs.SWORDS_CD_MULTIPLIER.get().floatValue();
        }
        return (int) (spell.getSpellCooldown() * (2 - Utils.softCapFormula(playerCooldownModifier)) * itemCoolDownModifer);
    }

    public static void spawnParticles(Level level, ParticleOptions particle, double x, double y, double z, int count, double deltaX, double deltaY, double deltaZ, double speed, boolean force) {
        level.getServer().getPlayerList().getPlayers().forEach(player -> ((ServerLevel) level).sendParticles(player, particle, force, x, y, z, count, deltaX, deltaY, deltaZ, speed));
    }
}
