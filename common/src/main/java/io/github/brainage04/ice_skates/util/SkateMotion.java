package io.github.brainage04.ice_skates.util;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/** One tick of skating, apart from the mixin so that unit tests can reach it. */
public final class SkateMotion {
    /** The share of its horizontal speed a roller skater keeps each tick, on any block. */
    public static final float ROLLER_SKATE_FRICTION = 0.925F;

    /** The speed held movement keys add in one tick, in blocks a tick. */
    public static final float ACCELERATION = 0.04F;

    /**
     * Vanilla's horizontal air drag, a literal in LivingEntity#travelInAir (LivingEntity.java:2472
     * in 26.3) that no method returns for a player: getAirDrag gives the vertical 0.98.
     */
    private static final float AIR_DRAG = 0.91F;

    private SkateMotion() {}

    /**
     * The share of horizontal speed a skater on the ground keeps each tick on a surface of {@code
     * friction}, given the player's {@code friction_modifier} and {@code air_drag_modifier}
     * attributes. The friction modifier scales the friction as vanilla's does. Skates otherwise
     * leave out vanilla's air drag, so a modifier that makes the surface slipperier than the
     * skate's own friction brings the air drag back, scaled by the air drag modifier, to keep the
     * top speed bounded; it never leaves the skater less than the surface's own friction.
     */
    public static float groundFriction(
            float friction, float frictionModifier, float airDragModifier) {
        float modified = modifiedFriction(friction, frictionModifier);
        if (modified <= friction) {
            return modified;
        }
        return Math.max(friction, modified * modifiedFriction(AIR_DRAG, airDragModifier));
    }

    /**
     * Vanilla's LivingEntity#computeModifiedFriction (LivingEntity.java:518-520 in 26.3), which is
     * private; unit tests run without Minecraft's mixin environment, so an invoker cannot reach it.
     */
    static float modifiedFriction(float friction, float modifier) {
        return Mth.clamp(1.0F - (1.0F - friction) * modifier, 0.0F, 1.0F);
    }

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
