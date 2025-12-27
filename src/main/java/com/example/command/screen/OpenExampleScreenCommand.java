package com.example.command.screen;

import com.example.screen.ExampleScreen;
import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public class OpenExampleScreenCommand {
    public static int execute(FabricClientCommandSource source) {
        Minecraft client = source.getClient();
        // client.send is used because it delays the opening of the screen by a tick
        // if you do not do this, the client will attempt to open ExampleScreen before
        // the current screen (ChatScreen) closes, and as a result the ExampleScreen will
        // close on the same tick it opens (meaning you will not see it)
        client.schedule(() ->
                client.setScreen(new ExampleScreen(client.screen))
        );

        source.sendFeedback(Component.literal("Opened example screen."));

        return 1;
    }

    public static void initialize(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(literal("openexamplescreen")
                .executes(context ->
                        execute(
                                context.getSource()
                        )
                )
        );
    }
}
