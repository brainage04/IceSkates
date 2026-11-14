package io.github.brainage04.ice_skates.util;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/** One tick of skating, apart from the mixin so that unit tests can reach it. */
public final class SkateMotion {
    /** The share of its horizontal speed a roller skater keeps each tick, on any block. */
    public static final float ROLLER_SKATE_FRICTION = 0.925F;

    /** The speed held movement keys add in one tick, in blocks a tick. */
    public static final float ACCELERATION = 0.04F;

    private SkateMotion() {}

    /**
     * The velocity after one tick: the skater keeps {@code friction} of the horizontal velocity and
     * falls {@code gravity} faster, and the movement keys in {@code input} add {@link
     * #ACCELERATION} relative to the yaw {@code yRot} (degrees), no more on a diagonal.
     */
    public static Vec3 nextVelocity(
            Vec3 velocity, float friction, double gravity, Vec3 input, float yRot) {
        Vec3 next = new Vec3(velocity.x * friction, velocity.y - gravity, velocity.z * friction);

        float forwardAccel = 0.0F;
        if (input.z > 0) {
            forwardAccel += ACCELERATION;
        } else if (input.z < 0) {
            forwardAccel -= ACCELERATION;
        }

        float strafeAccel = 0.0F;
        if (input.x > 0) {
            strafeAccel += ACCELERATION;
        } else if (input.x < 0) {
            strafeAccel -= ACCELERATION;
        }

        float accelerationLength =
                Mth.sqrt(forwardAccel * forwardAccel + strafeAccel * strafeAccel);
        if (accelerationLength > ACCELERATION) {
            float scale = ACCELERATION / accelerationLength;
            forwardAccel *= scale;
            strafeAccel *= scale;
        }

        if (forwardAccel != 0.0F || strafeAccel != 0.0F) {
            float rad = yRot * ((float) Math.PI / 180F);

            double xForward = Mth.sin(-rad) * forwardAccel;
            double zForward = Mth.cos(rad) * forwardAccel;

            double xStrafe = Mth.cos(rad) * strafeAccel;
            double zStrafe = Mth.sin(rad) * strafeAccel;

            next = next.add(xForward + xStrafe, 0.0, zForward + zStrafe);
        }

        return next;
    }
}
