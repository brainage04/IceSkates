package com.github.brainage04.ice_skates;

import com.github.brainage04.ice_skates.item.ModItems;
import net.fabricmc.api.ClientModInitializer;

public class IceSkatesClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		IceSkates.LOGGER.info("Initialising client...");

		ModItems.initialize();

		IceSkates.LOGGER.info("Initialised client.");
	}
}