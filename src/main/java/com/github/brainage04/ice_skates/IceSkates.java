package com.github.brainage04.ice_skates;

import com.github.brainage04.ice_skates.item.ModItems;
import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IceSkates implements ModInitializer {
	public static final String MOD_ID = "ice_skates";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	@Override
	public void onInitialize() {
		IceSkates.LOGGER.info("Initialising...");

		ModItems.initialize();

		IceSkates.LOGGER.info("Initialised.");
	}
}