package com.github.brainage04.ice_skates;

public enum IceSurfaceType {
    BLUE_ICE(0.999F, 2.0F, true),    // Friction, Max Speed (doubled), Is Ice
    PACKED_ICE(0.995F, 1.5F, true),
    ICE(0.98F, 1.0F, true),
    NONE(0.6F, 0.2F, false);         // Very slow on non-ice

    public final float friction;
    public final float maxSpeed;
    public final boolean isIce;

    IceSurfaceType(float friction, float maxSpeed, boolean isIce) {
        this.friction = friction;
        this.maxSpeed = maxSpeed;
        this.isIce = isIce;
    }
}
