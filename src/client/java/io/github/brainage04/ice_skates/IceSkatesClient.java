package io.github.brainage04.ice_skates;

import io.github.brainage04.ice_skates.item.ModItems;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.world.item.CreativeModeTabs;

public class IceSkatesClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		IceSkates.LOGGER.info("Initialising client...");

		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.COMBAT).register(entries -> {
			entries.accept(ModItems.ICE_SKATES);
			entries.accept(ModItems.ROLLER_SKATES);
			entries.accept(ModItems.ICE_SWORD);
			entries.accept(ModItems.PACKED_ICE_SWORD);
			entries.accept(ModItems.BLUE_ICE_SWORD);
		});
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS).register(entries -> {
			entries.accept(ModItems.ICE_SKATE_BLADES);
			entries.accept(ModItems.ROLLER_SKATE_WHEELS);
		});

		IceSkates.LOGGER.info("Initialised client.");
	}
}