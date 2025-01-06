package io.redspace.ironsspellbooks.registries;

import io.redspace.ironsspellbooks.command.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.event.RegisterCommandsEvent;


@EventBusSubscriber()
public class CommandRegistry {
    @SubscribeEvent
    public static void onCommandsRegister(RegisterCommandsEvent event) {

        var commandDispatcher = event.getDispatcher();
        var commandBuildContext = event.getBuildContext();

        CreateScrollCommand.register(commandDispatcher);
        CreateSpellBookCommand.register(commandDispatcher);
        CreateImbuedSwordCommand.register(commandDispatcher, commandBuildContext);
        CreateDebugWizardCommand.register(commandDispatcher);
        CastCommand.register(commandDispatcher);
        ManaCommand.register(commandDispatcher);
        GenerateModList.register(commandDispatcher);
        LearnCommand.register(commandDispatcher);
        ClearCooldownCommand.register(commandDispatcher);
        ClearRecastsCommand.register(commandDispatcher);

        if (!FMLLoader.isProduction()) {
            ClearSpellSelectionCommand.register(commandDispatcher);
            IronsDebugCommand.register(commandDispatcher);
            GenerateSiteData.register(commandDispatcher);
        }
    }
}