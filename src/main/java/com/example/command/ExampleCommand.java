package com.example.command;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.network.chat.Component;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public class ExampleCommand {
    public static int execute(FabricClientCommandSource source) {
        source.sendFeedback(Component.literal("This is an example command."));

        return 1;
    }

    public static void initialize(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(literal("example")
                .executes(context ->
                        execute(
                                context.getSource()
                        )
                )
        );
    }
}
