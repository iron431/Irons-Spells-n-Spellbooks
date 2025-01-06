package io.redspace.ironsspellbooks.player;

import io.redspace.ironsspellbooks.IronsSpellbooks;
import io.redspace.ironsspellbooks.api.entity.IMagicEntity;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.capabilities.magic.ClientSpellTargetingData;
import io.redspace.ironsspellbooks.capabilities.magic.PlayerCooldowns;
import io.redspace.ironsspellbooks.capabilities.magic.PlayerRecasts;
import io.redspace.ironsspellbooks.capabilities.magic.SyncedSpellData;
import io.redspace.ironsspellbooks.util.Log;
import io.redspace.ironsspellbooks.util.MinecraftInstanceHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.UUID;

public class ClientMagicData {

    /**
     * Current Player's Synced Data
     */
    private static final MagicData playerMagicData = new MagicData();

    /**
     * Other Player's Synced Data
     */
    private static final HashMap<Integer, SyncedSpellData> playerSyncedDataLookup = new HashMap<>();
    private static final SyncedSpellData emptySyncedData = new SyncedSpellData(-999);

    /**
     * Spell Selections
     */
    static SpellSelectionManager spellSelectionManager;

    public static SpellSelectionManager getSpellSelectionManager() {
        if (spellSelectionManager == null) {
            var player = MinecraftInstanceHelper.getPlayer();
            if (player != null) {
                spellSelectionManager = new SpellSelectionManager(player);
            }

        }

        return spellSelectionManager;
    }

    public static void updateSpellSelectionManager(@NotNull ServerPlayer player) {
        spellSelectionManager = new SpellSelectionManager(player);
    }

    public static void updateSpellSelectionManager() {
        var player = Minecraft.getInstance().player;
        if (player != null) {
            spellSelectionManager = new SpellSelectionManager(Minecraft.getInstance().player);
        }
    }

    /**
     * Local Targeting data
     */
    private static ClientSpellTargetingData spellTargetingData;

    public static void setTargetingData(ClientSpellTargetingData spellTargetingData) {
        ClientMagicData.spellTargetingData = spellTargetingData;
    }

    public static ClientSpellTargetingData getTargetingData() {
        if (spellTargetingData == null)
            setTargetingData(new ClientSpellTargetingData());
        return spellTargetingData;
    }

    public static void resetTargetingData() {
        spellTargetingData = null;
    }

    public static PlayerCooldowns getCooldowns() {
        return playerMagicData.getPlayerCooldowns();
    }

    public static PlayerRecasts getRecasts() {
        return playerMagicData.getPlayerRecasts();
    }

    public static void setRecasts(PlayerRecasts playerRecasts) {
        playerMagicData.setPlayerRecasts(playerRecasts);
    }

    public static float getCooldownPercent(AbstractSpell spell) {
        return playerMagicData.getPlayerCooldowns().getCooldownPercent(spell);
    }

    public static int getPlayerMana() {
        return (int) playerMagicData.getMana();
    }

    public static void setMana(int playerMana) {
        ClientMagicData.playerMagicData.setMana(playerMana);
    }

    public static CastType getCastType() {
        return ClientMagicData.playerMagicData.getCastType();
    }

    public static String getCastingSpellId() {
        return playerMagicData.getCastingSpellId();
    }

    public static int getCastingSpellLevel() {
        return playerMagicData.getCastingSpellLevel();
    }

    public static int getCastDurationRemaining() {
        return playerMagicData.getCastDurationRemaining();
    }

    public static int getCastDuration() {
        return playerMagicData.getCastDuration();
    }

    public static boolean isCasting() {
        return playerMagicData.isCasting();
    }

    public static void handleCastDuration() {
        playerMagicData.handleCastDuration();
    }

    public static float getCastCompletionPercent() {
        return playerMagicData.getCastCompletionPercent();
    }

    public static void setClientCastState(String spellId, int spellLevel, int castDuration, CastSource castSource, String castingEquipmentSlot) {
        playerMagicData.initiateCast(SpellRegistry.getSpell(spellId), spellLevel, castDuration, castSource, castingEquipmentSlot);
    }

    public static void resetClientCastState(UUID playerUUID) {
        //Ironsspellbooks.logger.debug("resetClientCastState.1: instanceUUID:{}, playerUUID:{}", Minecraft.getInstance().player.getUUID(), playerUUID);

        if (Minecraft.getInstance().player.getUUID().equals(playerUUID)) {
            //Ironsspellbooks.logger.debug("resetClientCastState.1.1");
            playerMagicData.resetCastingState();
            resetTargetingData();
        }

        if (Minecraft.getInstance().player != null && Minecraft.getInstance().player.isUsingItem() && Minecraft.getInstance().player.getUUID().equals(playerUUID)) {
            //Ironsspellbooks.logger.debug("resetClientCastState.2: instanceUUID:{}, playerUUID:{}", Minecraft.getInstance().player.getUUID(), playerUUID);
            Minecraft.getInstance().player.stopUsingItem();
        }
    }

    public static SyncedSpellData getSyncedSpellData(LivingEntity livingEntity) {
        if (livingEntity instanceof Player) {
            return playerSyncedDataLookup.getOrDefault(livingEntity.getId(), emptySyncedData);
        }
        if (livingEntity instanceof IMagicEntity abstractSpellCastingMob) {
            return abstractSpellCastingMob.getMagicData().getSyncedData();
        }
        return new SyncedSpellData(null);

    }

    public static void handlePlayerSyncedData(SyncedSpellData playerSyncedData) {
        if (Log.SPELL_SELECTION) {
            IronsSpellbooks.LOGGER.debug("ClientMagicData.handlePlayerSyncedData {}", playerSyncedData.getSpellSelection());
        }
        playerSyncedDataLookup.put(playerSyncedData.getServerPlayerId(), playerSyncedData);
    }

    public static void handleAbstractCastingMobSyncedData(int entityId, SyncedSpellData syncedSpellData) {
        var level = Minecraft.getInstance().level;

        if (Log.SPELL_DEBUG) {
            IronsSpellbooks.LOGGER.debug("handleAbstractCastingMobSyncedData {}, {}, {}", level, entityId, syncedSpellData);
        }

        if (level == null) {
            return;
        }

        var entity = level.getEntity(entityId);
        if (entity instanceof IMagicEntity abstractSpellCastingMob) {
            abstractSpellCastingMob.setSyncedSpellData(syncedSpellData);
        }
    }
}