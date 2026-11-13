package io.github.brainage04.ice_skates;

import net.minecraft.resources.Identifier;

public final class IceSkatesCommon {
    public static final String MOD_ID = "ice_skates";

    private IceSkatesCommon() {}

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
