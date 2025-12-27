package com.github.brainage04.ice_skates;

import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class IceSkateState {
    private static final Map<UUID, IceSkateData> playerStates = new HashMap<>();

    public static IceSkateData get(Player player) {
        return playerStates.computeIfAbsent(player.getUUID(), k -> new IceSkateData());
    }

    public static void remove(UUID uuid) {
        playerStates.remove(uuid);
    }

    public static class IceSkateData {
        public boolean isGliding = false;
        public int ticksSinceOnIce = 0;
        public float currentSpeed = 0f;
        
        // grace period for air resistance reduction after jumping while gliding on ice
        public static final int AIR_GRACE_TICKS = 40;
        
        public boolean hasAirGrace() {
            return ticksSinceOnIce < AIR_GRACE_TICKS;
        }
    }
}