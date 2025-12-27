package com.example.key.core;

import com.example.ExampleMod;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public class ModKeys {
    public static final KeyMapping.Category MAIN_CATEGORY = new KeyMapping.Category(Identifier.fromNamespaceAndPath(ExampleMod.MOD_ID, "main"));

    public static final KeyMapping TEST_KEY = KeyBindingHelper.registerKeyBinding(new KeyMapping(
            "key.%s.testKey".formatted(ExampleMod.MOD_ID),
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_UNKNOWN,
            MAIN_CATEGORY
    ));

    public static void initialize() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (TEST_KEY.consumeClick()) {
                // used for instant/discrete actions such as toggles
            }

            if (TEST_KEY.isDown()) {
                // used for continuous actions such as mining blocks
            }
        });
    }
}
