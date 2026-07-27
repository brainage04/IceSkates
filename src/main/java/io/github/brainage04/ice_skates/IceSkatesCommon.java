package io.github.brainage04.ice_skates;

import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class IceSkatesCommon {
    public static final String MOD_ID = "ice_skates";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private IceSkatesCommon() { }

    public static Identifier id(String path) { return Identifier.fromNamespaceAndPath(MOD_ID, path); }

    public static void initialize() { LOGGER.info("Initialising IceSkates"); }
}
