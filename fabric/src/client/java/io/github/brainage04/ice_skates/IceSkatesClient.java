package io.github.brainage04.ice_skates;

import io.github.brainage04.ice_skates.item.ModItems;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.world.item.CreativeModeTabs;

public final class IceSkatesClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.COMBAT).register(entries -> {
            entries.accept(ModItems.iceSkates()); entries.accept(ModItems.rollerSkates()); entries.accept(ModItems.iceSword()); entries.accept(ModItems.packedIceSword()); entries.accept(ModItems.blueIceSword());
        });
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS).register(entries -> { entries.accept(ModItems.iceSkateBlades()); entries.accept(ModItems.rollerSkateWheels()); });
    }
}
