package io.github.brainage04.ice_skates;

import io.github.brainage04.ice_skates.client.ServerSupport;
import io.github.brainage04.ice_skates.item.ModItems;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.world.item.CreativeModeTabs;

public final class IceSkatesClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // The tabs are rebuilt for every connection, so on a server without Ice Skates neither
        // the tabs nor the search show its items (see ServerSupport).
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.COMBAT)
                .register(
                        entries -> {
                            if (!ServerSupport.serverHasIceSkates()) return;
                            entries.accept(ModItems.iceSkates());
                            entries.accept(ModItems.rollerSkates());
                            entries.accept(ModItems.iceSword());
                            entries.accept(ModItems.packedIceSword());
                            entries.accept(ModItems.blueIceSword());
                        });
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS)
                .register(
                        entries -> {
                            if (!ServerSupport.serverHasIceSkates()) return;
                            entries.accept(ModItems.iceSkateBlades());
                            entries.accept(ModItems.rollerSkateWheels());
                        });
    }
}
