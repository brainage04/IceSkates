package com.github.brainage04.iceskates.command.core;

import com.github.brainage04.iceskates.command.ExampleCommand;
import com.github.brainage04.iceskates.command.screen.OpenExampleScreenCommand;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;

public class ModCommands {
    public static void initialize() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            OpenExampleScreenCommand.initialize(dispatcher);

            ExampleCommand.initialize(dispatcher);
        });
    }
}
