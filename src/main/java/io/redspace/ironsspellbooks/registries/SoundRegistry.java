package io.redspace.ironsspellbooks.registries;

import io.redspace.ironsspellbooks.IronsSpellbooks;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;


public class SoundRegistry {
    private static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(Registries.SOUND_EVENT, IronsSpellbooks.MODID);

    public static void register(IEventBus eventBus) {
        SOUND_EVENTS.register(eventBus);
    }

    public static DeferredHolder<SoundEvent, SoundEvent> FORCE_IMPACT = registerSoundEvent("force_impact");
    public static DeferredHolder<SoundEvent, SoundEvent> ICE_IMPACT = registerSoundEvent("ice_impact");

    public static DeferredHolder<SoundEvent, SoundEvent> MAGIC_SPELL_REVERSE_3 = registerSoundEvent("magic_spell_reverse_3");
    public static DeferredHolder<SoundEvent, SoundEvent> ARIAL_SUMMONING_5_CUSTOM_1 = registerSoundEvent("arial_summoning_5_custom_1");
    public static DeferredHolder<SoundEvent, SoundEvent> DARK_MAGIC_BUFF_03_CUSTOM_1 = registerSoundEvent("dark_magic_buff_03_custom_1");
    public static DeferredHolder<SoundEvent, SoundEvent> DARK_SPELL_02 = registerSoundEvent("dark_spell_02");
    public static DeferredHolder<SoundEvent, SoundEvent> LIGHTNING_WOOSH_01 = registerSoundEvent("lightning_woosh_01");

    public static DeferredHolder<SoundEvent, SoundEvent> HEARTSTOP_CAST = registerSoundEvent("heartstop_cast");
    public static DeferredHolder<SoundEvent, SoundEvent> LIGHTNING_LANCE_CAST = registerSoundEvent("lightning_lance_cast");
    public static DeferredHolder<SoundEvent, SoundEvent> MAGIC_ARROW_RELEASE = registerSoundEvent("magic_arrow_release");
    public static DeferredHolder<SoundEvent, SoundEvent> MAGIC_ARROW_CHARGE = registerSoundEvent("magic_arrow_charge");
    public static DeferredHolder<SoundEvent, SoundEvent> FROST_STEP = registerSoundEvent("frost_step");
    public static DeferredHolder<SoundEvent, SoundEvent> ABYSSAL_TELEPORT = registerSoundEvent("abyssal_teleport");
    public static DeferredHolder<SoundEvent, SoundEvent> ABYSSAL_SHROUD = registerSoundEvent("cast.abyssal_shroud");
    public static DeferredHolder<SoundEvent, SoundEvent> BLOOD_STEP = registerSoundEvent("cast.blood_step");
    public static DeferredHolder<SoundEvent, SoundEvent> FIRE_BREATH_LOOP = registerSoundEvent("loop.fire_breath");
    public static DeferredHolder<SoundEvent, SoundEvent> ELECTROCUTE_LOOP = registerSoundEvent("loop.electrocute");
    public static DeferredHolder<SoundEvent, SoundEvent> CONE_OF_COLD_LOOP = registerSoundEvent("loop.cone_of_cold");
    public static DeferredHolder<SoundEvent, SoundEvent> CLOUD_OF_REGEN_LOOP = registerSoundEvent("loop.cloud_of_regen");
    public static DeferredHolder<SoundEvent, SoundEvent> RAISE_DEAD_START = registerSoundEvent("cast.raise_dead.start");
    public static DeferredHolder<SoundEvent, SoundEvent> RAISE_DEAD_FINISH = registerSoundEvent("cast.raise_dead.finish");
    public static DeferredHolder<SoundEvent, SoundEvent> VOID_TENTACLES_START = registerSoundEvent("cast.void_tentacles.start");
    public static DeferredHolder<SoundEvent, SoundEvent> VOID_TENTACLES_FINISH = registerSoundEvent("cast.void_tentacles.finish");
    public static DeferredHolder<SoundEvent, SoundEvent> VOID_TENTACLES_LEAVE = registerSoundEvent("entity.void_tentacles.retreat");
    public static DeferredHolder<SoundEvent, SoundEvent> VOID_TENTACLES_AMBIENT = registerSoundEvent("entity.void_tentacles.ambient");
    public static DeferredHolder<SoundEvent, SoundEvent> ICE_BLOCK_CAST = registerSoundEvent("cast.ice_block");
    public static DeferredHolder<SoundEvent, SoundEvent> ICE_BLOCK_IMPACT = registerSoundEvent("entity.ice_block.impact");
    public static DeferredHolder<SoundEvent, SoundEvent> RAY_OF_SIPHONING = registerSoundEvent("loop.ray_of_siphoning");
    public static DeferredHolder<SoundEvent, SoundEvent> FIREBALL_START = registerSoundEvent("cast.fireball");
    public static DeferredHolder<SoundEvent, SoundEvent> ACID_ORB_CHARGE = registerSoundEvent("spell.acid_orb.charge");
    public static DeferredHolder<SoundEvent, SoundEvent> ACID_ORB_CAST = registerSoundEvent("spell.acid_orb.cast");
    public static DeferredHolder<SoundEvent, SoundEvent> ACID_ORB_IMPACT = registerSoundEvent("entity.acid_orb.impact");
    public static DeferredHolder<SoundEvent, SoundEvent> POISON_ARROW_CHARGE = registerSoundEvent("spell.poison_arrow.charge");
    public static DeferredHolder<SoundEvent, SoundEvent> POISON_ARROW_CAST = registerSoundEvent("spell.poison_arrow.cast");
    public static DeferredHolder<SoundEvent, SoundEvent> POISON_BREATH_LOOP = registerSoundEvent("spell.poison_breath.loop");
    public static DeferredHolder<SoundEvent, SoundEvent> ROOT_EMERGE = registerSoundEvent("entity.root.emerge");
    public static DeferredHolder<SoundEvent, SoundEvent> BLACK_HOLE_CHARGE = registerSoundEvent("spell.black_hole.charge");
    public static DeferredHolder<SoundEvent, SoundEvent> BLACK_HOLE_CAST = registerSoundEvent("spell.black_hole.cast");
    public static DeferredHolder<SoundEvent, SoundEvent> BLACK_HOLE_LOOP = registerSoundEvent("entity.black_hole.loop");
    public static DeferredHolder<SoundEvent, SoundEvent> POISON_SPLASH_BEGIN = registerSoundEvent("spell.poison_splash.begin");
    public static DeferredHolder<SoundEvent, SoundEvent> BLIGHT_BEGIN = registerSoundEvent("spell.blight.begin");
    public static DeferredHolder<SoundEvent, SoundEvent> SPIDER_ASPECT_CAST = registerSoundEvent("spell.spider_aspect.cast");
    public static DeferredHolder<SoundEvent, SoundEvent> BLOOD_NEEDLE_IMPACT = registerSoundEvent("entity.blood_needle.impact");
    public static DeferredHolder<SoundEvent, SoundEvent> FIRE_BOMB_CHARGE = registerSoundEvent("spell.fire_bomb.charge");
    public static DeferredHolder<SoundEvent, SoundEvent> FIRE_BOMB_CAST = registerSoundEvent("spell.fire_bomb.cast");
    public static DeferredHolder<SoundEvent, SoundEvent> GUST_CHARGE = registerSoundEvent("spell.gust.charge");
    public static DeferredHolder<SoundEvent, SoundEvent> GUST_CAST = registerSoundEvent("spell.gust.cast");
    public static DeferredHolder<SoundEvent, SoundEvent> GUIDING_BOLT_IMPACT = registerSoundEvent("entity.guiding_bolt.impact");
    public static DeferredHolder<SoundEvent, SoundEvent> GUIDING_BOLT_CAST = registerSoundEvent("spell.guiding_bolt.cast");
    public static DeferredHolder<SoundEvent, SoundEvent> CHAIN_LIGHTNING_CHAIN = registerSoundEvent("entity.chain_lightning.lightning_chain");
    public static DeferredHolder<SoundEvent, SoundEvent> DEVOUR_BITE = registerSoundEvent("entity.devour_jaw.bite");
    public static DeferredHolder<SoundEvent, SoundEvent> KEEPER_SWING = registerSoundEvent("entity.citadel_keeper.swing");
    public static DeferredHolder<SoundEvent, SoundEvent> KEEPER_STEP = registerSoundEvent("entity.citadel_keeper.step");
    public static DeferredHolder<SoundEvent, SoundEvent> KEEPER_DEATH = registerSoundEvent("entity.citadel_keeper.death");
    public static DeferredHolder<SoundEvent, SoundEvent> KEEPER_HURT = registerSoundEvent("entity.citadel_keeper.hurt");
    public static DeferredHolder<SoundEvent, SoundEvent> KEEPER_SWORD_IMPACT = registerSoundEvent("entity.citadel_keeper.sword_impact");
    public static DeferredHolder<SoundEvent, SoundEvent> KEEPER_IDLE = registerSoundEvent("entity.citadel_keeper.idle");
    public static DeferredHolder<SoundEvent, SoundEvent> OAKSKIN_CAST = registerSoundEvent("spell.oakskin.cast");
    public static DeferredHolder<SoundEvent, SoundEvent> EARTHQUAKE_LOOP = registerSoundEvent("entity.earthquake_aoe.loop");
    public static DeferredHolder<SoundEvent, SoundEvent> EARTHQUAKE_IMPACT = registerSoundEvent("entity.earthquake_aoe.impact");
    public static DeferredHolder<SoundEvent, SoundEvent> EARTHQUAKE_CAST = registerSoundEvent("spell.earthquake.cast");
    public static DeferredHolder<SoundEvent, SoundEvent> FIREFLY_SWARM_IDLE = registerSoundEvent("entity.firefly_swarm.idle");
    public static DeferredHolder<SoundEvent, SoundEvent> FIREFLY_SWARM_ATTACK = registerSoundEvent("entity.firefly_swarm.attack");
    public static DeferredHolder<SoundEvent, SoundEvent> FIREFLY_SPELL_PREPARE = registerSoundEvent("spell.firefly_swarm.prepare");
    public static DeferredHolder<SoundEvent, SoundEvent> RAY_OF_FROST = registerSoundEvent("spell.ray_of_frost.cast");
    public static DeferredHolder<SoundEvent, SoundEvent> SONIC_BOOM = registerSoundEvent("spell.sonic_boom.cast");
    public static DeferredHolder<SoundEvent, SoundEvent> DIVINE_SMITE_WINDUP = registerSoundEvent("spell.divine_smite.windup");
    public static DeferredHolder<SoundEvent, SoundEvent> DIVINE_SMITE_CAST = registerSoundEvent("spell.divine_smite.cast");
    public static DeferredHolder<SoundEvent, SoundEvent> TELEKINESIS_CAST = registerSoundEvent("spell.telekinesis.cast");
    public static DeferredHolder<SoundEvent, SoundEvent> TELEKINESIS_LOOP = registerSoundEvent("spell.telekinesis.loop");
    public static DeferredHolder<SoundEvent, SoundEvent> PLANAR_SIGHT_CAST = registerSoundEvent("spell.planar_sight.cast");
    public static DeferredHolder<SoundEvent, SoundEvent> HEAT_SURGE_PREPARE = registerSoundEvent("spell.heat_surge.prepare");
    public static DeferredHolder<SoundEvent, SoundEvent> FROSTWAVE_PREPARE = registerSoundEvent("spell.frostwave.prepare");
    public static DeferredHolder<SoundEvent, SoundEvent> ARROW_VOLLEY_PREPARE = registerSoundEvent("spell.arrow_volley.prepare");
    public static DeferredHolder<SoundEvent, SoundEvent> BOW_SHOOT = registerSoundEvent("bow_shoot");
    public static DeferredHolder<SoundEvent, SoundEvent> RECALL_PREPARE = registerSoundEvent("spell.recall.prepare");
    public static DeferredHolder<SoundEvent, SoundEvent> ELDRITCH_BLAST = registerSoundEvent("spell.eldritch_blast.cast");
    public static DeferredHolder<SoundEvent, SoundEvent> FLAMING_STRIKE_UPSWING = registerSoundEvent("spell.flaming_strike.begin");
    public static DeferredHolder<SoundEvent, SoundEvent> FLAMING_STRIKE_SWING = registerSoundEvent("spell.flaming_strike.cast");
    public static DeferredHolder<SoundEvent, SoundEvent> SHOCKWAVE_CAST = registerSoundEvent("spell.shockwave.cast");
    public static DeferredHolder<SoundEvent, SoundEvent> SHOCKWAVE_PREPARE = registerSoundEvent("spell.shockwave.prepare");
    public static DeferredHolder<SoundEvent, SoundEvent> TRADER_YES = registerSoundEvent("entity.generic.trader.yes");
    public static DeferredHolder<SoundEvent, SoundEvent> TRADER_NO = registerSoundEvent("entity.generic.trader.no");
    public static DeferredHolder<SoundEvent, SoundEvent> SCORCH_PREPARE = registerSoundEvent("spell.scorch.prepare");
    public static DeferredHolder<SoundEvent, SoundEvent> FIERY_EXPLOSION = registerSoundEvent("entity.generic.fiery_explosion");
    public static DeferredHolder<SoundEvent, SoundEvent> ECHOING_STRIKE = registerSoundEvent("entity.echoing_strike.echoing_strike");
    public static DeferredHolder<SoundEvent, SoundEvent> SMALL_LIGHTNING_STRIKE = registerSoundEvent("entity.lightning_strike.strike");
    public static DeferredHolder<SoundEvent, SoundEvent> THUNDERSTORM_PREPARE = registerSoundEvent("spell.thunderstorm.prepare");
    public static DeferredHolder<SoundEvent, SoundEvent> BLOOD_EXPLOSION = registerSoundEvent("spell.sacrifice.blood_explosion");
    public static DeferredHolder<SoundEvent, SoundEvent> SUNBEAM_WINDUP = registerSoundEvent("entity.sunbeam.windup");
    public static DeferredHolder<SoundEvent, SoundEvent> SUNBEAM_IMPACT = registerSoundEvent("entity.sunbeam.impact");
    public static DeferredHolder<SoundEvent, SoundEvent> CLEANSE_CAST = registerSoundEvent("spell.cleanse.cast");
    public static DeferredHolder<SoundEvent, SoundEvent> ICE_SPIKE_EMERGE = registerSoundEvent("entity.ice_spike.emerge");
    public static DeferredHolder<SoundEvent, SoundEvent> FIRE_ARROW_CHARGE = registerSoundEvent("spell.fire_arrow.charge");
    public static DeferredHolder<SoundEvent, SoundEvent> FIRE_ARROW_CAST = registerSoundEvent("spell.fire_arrow.cast");

    public static DeferredHolder<SoundEvent, SoundEvent> DEAD_KING_SWING = registerSoundEvent("entity.dead_king.attack_swing");
    public static DeferredHolder<SoundEvent, SoundEvent> DEAD_KING_SLAM = registerSoundEvent("entity.dead_king.attack_slam");
    public static DeferredHolder<SoundEvent, SoundEvent> DEAD_KING_HIT = registerSoundEvent("entity.dead_king.attack_hit");
    public static DeferredHolder<SoundEvent, SoundEvent> DEAD_KING_RESURRECT = registerSoundEvent("entity.dead_king.resurrect");
    public static DeferredHolder<SoundEvent, SoundEvent> DEAD_KING_SPAWN = registerSoundEvent("entity.dead_king.spawn");
    public static DeferredHolder<SoundEvent, SoundEvent> DEAD_KING_FAKE_DEATH = registerSoundEvent("entity.dead_king.fake_death");
    public static DeferredHolder<SoundEvent, SoundEvent> DEAD_KING_DEATH = registerSoundEvent("entity.dead_king.death");
    public static DeferredHolder<SoundEvent, SoundEvent> DEAD_KING_HURT = registerSoundEvent("entity.dead_king.hurt");
    public static DeferredHolder<SoundEvent, SoundEvent> DEAD_KING_EXPLODE = registerSoundEvent("entity.dead_king.explode");
    public static DeferredHolder<SoundEvent, SoundEvent> DEAD_KING_DRUM_LOOP = registerSoundEvent("entity.dead_king.music.drum_loop");
    public static DeferredHolder<SoundEvent, SoundEvent> DEAD_KING_AMBIENCE = registerSoundEvent("entity.dead_king.ambience");
    public static DeferredHolder<SoundEvent, SoundEvent> DEAD_KING_MUSIC_INTRO = registerSoundEvent("entity.dead_king.music.intro");
    public static DeferredHolder<SoundEvent, SoundEvent> DEAD_KING_FIRST_PHASE_MELODY = registerSoundEvent("entity.dead_king.music.first_phase_melody");
    public static DeferredHolder<SoundEvent, SoundEvent> DEAD_KING_FIRST_PHASE_ACCENT_01 = registerSoundEvent("entity.dead_king.music.first_phase_accent_01");
    public static DeferredHolder<SoundEvent, SoundEvent> DEAD_KING_SECOND_PHASE_MELODY_ALT = registerSoundEvent("entity.dead_king.music.second_phase_melody_alt");
    public static DeferredHolder<SoundEvent, SoundEvent> DEAD_KING_SUSPENSE = registerSoundEvent("entity.dead_king.music.suspense");

    public static DeferredHolder<SoundEvent, SoundEvent> FIRE_CAST = registerSoundEvent("cast.generic.fire");
    public static DeferredHolder<SoundEvent, SoundEvent> ICE_CAST = registerSoundEvent("cast.generic.ice");
    public static DeferredHolder<SoundEvent, SoundEvent> LIGHTNING_CAST = registerSoundEvent("cast.generic.lightning");
    public static DeferredHolder<SoundEvent, SoundEvent> HOLY_CAST = registerSoundEvent("cast.generic.holy");
    public static DeferredHolder<SoundEvent, SoundEvent> ENDER_CAST = registerSoundEvent("cast.generic.ender");
    public static DeferredHolder<SoundEvent, SoundEvent> BLOOD_CAST = registerSoundEvent("cast.generic.blood");
    public static DeferredHolder<SoundEvent, SoundEvent> EVOCATION_CAST = registerSoundEvent("cast.generic.evocation");
    public static DeferredHolder<SoundEvent, SoundEvent> NATURE_CAST = registerSoundEvent("cast.generic.nature");
    public static DeferredHolder<SoundEvent, SoundEvent> POISON_CAST = registerSoundEvent("cast.generic.poison");

    public static DeferredHolder<SoundEvent, SoundEvent> LEARN_ELDRITCH_SPELL = registerSoundEvent("ui.learn_eldritch_spell");
    public static DeferredHolder<SoundEvent, SoundEvent> UI_TICK = registerSoundEvent("ui.tick");
    public static DeferredHolder<SoundEvent, SoundEvent> EQUIP_SPELL_BOOK = registerSoundEvent("item.spell_book.equip");

    public static DeferredHolder<SoundEvent, SoundEvent> MUSIC_DISC_DEAD_KING_LULLABY = registerSoundEvent("music.dead_king_lullaby");


    private static DeferredHolder<SoundEvent, SoundEvent> registerSoundEvent(String name) {
        return SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(IronsSpellbooks.MODID, name)));
    }
}
