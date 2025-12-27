package com.github.brainage04.iceskates;

import com.github.brainage04.iceskates.command.core.ModCommands;
import com.github.brainage04.iceskates.config.core.ModConfig;
import com.github.brainage04.iceskates.key.core.ModKeys;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.api.ClientModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IceSkates implements ClientModInitializer {
	public static final String MOD_ID = "iceskates";
	public static final String MOD_NAME = "IceSkates";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitializeClient() {
		LOGGER.info("{} initialising...", MOD_NAME);

		AutoConfig.register(ModConfig.class, GsonConfigSerializer::new);
		ModCommands.initialize();
		ModKeys.initialize();

		LOGGER.info("{} initialised.", MOD_NAME);
	}
}