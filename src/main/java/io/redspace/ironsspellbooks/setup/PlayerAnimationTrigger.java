package io.redspace.ironsspellbooks.setup;

import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;
import io.redspace.ironsspellbooks.IronsSpellbooks;
import io.redspace.ironsspellbooks.entity.mobs.wizards.cursed_armor_stand.CursedArmorStandModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.client.event.ClientChatReceivedEvent;


/**
 * Example, how to trigger animations on specific players
 * Always trigger animation on client-side.  Maybe as a response to a network packet or event
 */
@EventBusSubscriber(modid = IronsSpellbooks.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class PlayerAnimationTrigger {

    //We need to know when to play an animation
    //This can be anything depending on your ideas (see Emotecraft, BetterCombat ...)
    @SubscribeEvent
    public static void onChatReceived(ClientChatReceivedEvent event) {
        //Test if it is a player (main or other) and the message
        if (!FMLLoader.isProduction()) {
            var str = event.getMessage().getString();
            if (str.contains("armorstand")) {
                int id = 0;
                int i = str.indexOf('[');
                double[] ad = new double[3];
                for (int c = 0; c < 100; c++) {
                    int j = str.indexOf(',', i + 1);
                    if (j >= 0) {
                        ad[id++] = Double.parseDouble(str.substring(i + 1, j));
                    } else {
                        ad[id] = Double.parseDouble(str.substring(i + 1, str.indexOf(']')));
                        break;
                    }
                    i = j;
                }
                CursedArmorStandModel.rightArmPos = ad;
            }

        }
        if (event.getMessage().contains(Component.literal("waving"))) {


            //Get the player from Minecraft, using the chat profile ID. From network packets, you'll receive entity IDs instead of UUIDs
            var player = Minecraft.getInstance().level.getPlayerByUUID(event.getSender());

            if (player == null)
                return; //The player can be null because it was a system message or because it is not loaded by this player.

            //Get the animation for that player
            var animation = (ModifierLayer<IAnimation>) PlayerAnimationAccess.getPlayerAssociatedData((AbstractClientPlayer) player).get(new ResourceLocation(IronsSpellbooks.MODID, "animation"));
            if (animation != null) {
                //You can set an animation from anywhere ON THE CLIENT
                //Do not attempt to do this on a server, that will only fail

                animation.setAnimation(new KeyframeAnimationPlayer((KeyframeAnimation) PlayerAnimationRegistry.getAnimation(new ResourceLocation(IronsSpellbooks.MODID, "waving"))));
                //You might use  animation.replaceAnimationWithFade(); to create fade effect instead of sudden change
                //See javadoc for details
            }
        }
    }

    //For server-side animation playing, see Emotecraft API
}