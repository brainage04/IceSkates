package com.github.brainage04.ice_skates.event;

import com.github.brainage04.ice_skates.IceSkateState;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

public class ModServerEvents {
    public static void initialize() {
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            IceSkateState.remove(handler.getPlayer().getUUID());
        });
    }
}
