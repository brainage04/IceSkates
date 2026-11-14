package io.github.brainage04.ice_skates.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.From;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.constraints.DoubleRange;
import net.jqwik.api.constraints.FloatRange;
import net.jqwik.api.constraints.Size;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

class SkateMotionTest {
    private static final double EPSILON = 1.0E-9;

    /**
     * Mth.sin and Mth.cos read one sine table and truncate the scaled angle towards zero
     * (Mth.java:50-56 in 26.3), so for a negative angle they read entries one step (2 pi / 65536)
     * apart. A direction built from such a pair is up to about 4.8e-5 longer or shorter than one,
     * in SkateMotion's push and in vanilla's alike.
     */
    private static final double TABLE_TOLERANCE = 1.0E-4;

    /** Block friction in vanilla 26.3: the default and every block that sets its own. */
    private static final List<Float> VANILLA_FRICTIONS =
            List.of(
                    // BlockBehaviour.java:1005, every block that does not set its own
                    0.6F,
                    // SLIME_BLOCK, Blocks.java:3038
                    0.8F,
                    // ICE, PACKED_ICE and FROSTED_ICE, Blocks.java:2074, :3121 and :3706
                    0.98F,
                    // BLUE_ICE, Blocks.java:4202
                    0.989F);

    /** A tick's movement input: each axis forward or back, left or right, or neither. */
    record Tick(Vec3 input, float yRot) {}

    @Provide
    Arbitrary<Double> axisInputs() {
        // A held key gives 1, sneaking scales it to 0.3 and using an item further, but only its
        // sign matters to a skater.
        return Arbitraries.oneOf(
                Arbitraries.just(0.0),
                Arbitraries.doubles().between(0.01, 1.0),
                Arbitraries.doubles().between(-1.0, -0.01));
    }

    @Provide
    Arbitrary<Tick> ticks() {
        return Combinators.combine(
                        axisInputs(), axisInputs(), Arbitraries.floats().between(-360.0F, 360.0F))
                .as((strafe, forward, yRot) -> new Tick(new Vec3(strafe, 0.0, forward), yRot));
    }

    @Provide
    Arbitrary<Float> frictions() {
        return Arbitraries.oneOf(
                Arbitraries.of(VANILLA_FRICTIONS),
                Arbitraries.just(SkateMotion.ROLLER_SKATE_FRICTION),
                Arbitraries.floats().between(0.0F, 0.99F));
    }

    private static Vec3 horizontal(Vec3 velocity) {
        return new Vec3(velocity.x, 0.0, velocity.z);
    }

    @Property
    void withoutKeysASkaterKeepsTheFrictionShareOfItsSpeed(
            @ForAll("frictions") float friction,
            @ForAll @DoubleRange(min = -5.0, max = 5.0) double x,
            @ForAll @DoubleRange(min = -5.0, max = 5.0) double z,
            @ForAll @FloatRange(min = -360.0F, max = 360.0F) float yRot) {
        Vec3 velocity = new Vec3(x, 0.0, z);
        Vec3 next = SkateMotion.nextVelocity(velocity, friction, 0.08, Vec3.ZERO, yRot);
        double before = velocity.horizontalDistance();
        assertEquals(friction * before, next.horizontalDistance(), EPSILON);
        assertTrue(next.horizontalDistance() <= before);
    }

    @Property
    void aSkaterFallsFasterByTheGravityEachTick(
            @ForAll("ticks") Tick tick,
            @ForAll("frictions") float friction,
            @ForAll @DoubleRange(min = -3.0, max = 3.0) double y,
            @ForAll @DoubleRange(min = 0.0, max = 0.1) double gravity) {
        Vec3 next =
                SkateMotion.nextVelocity(
                        new Vec3(0.2, y, -0.1), friction, gravity, tick.input(), tick.yRot());
        assertEquals(y - gravity, next.y, 0.0);
    }

    @Property
    void anyMovementKeyAddsTheAccelerationAndADiagonalNoMore(
            @ForAll("ticks") Tick tick, @ForAll("frictions") float friction) {
        boolean moving = tick.input().x != 0.0 || tick.input().z != 0.0;
        Vec3 next = SkateMotion.nextVelocity(Vec3.ZERO, friction, 0.0, tick.input(), tick.yRot());
        assertEquals(
                moving ? SkateMotion.ACCELERATION : 0.0,
                next.horizontalDistance(),
                SkateMotion.ACCELERATION * TABLE_TOLERANCE,
                "input " + tick.input());
    }

    /**
     * The keys push the skater the way vanilla turns movement input into a push for the player's
     * yaw ({@code Entity#getInputVector}, Entity.java:1781-1791 in 26.3, which is protected), with
     * a held key's input as 1.
     */
    @Property
    void theKeysPushTheWayVanillaTurnsInputIntoMovement(@ForAll("ticks") Tick tick) {
        Vec3 keys = new Vec3(Math.signum(tick.input().x), 0.0, Math.signum(tick.input().z));
        Vec3 vanilla = vanillaInputVector(keys, SkateMotion.ACCELERATION, tick.yRot());
        Vec3 next = SkateMotion.nextVelocity(Vec3.ZERO, 0.5F, 0.0, tick.input(), tick.yRot());
        assertEquals(vanilla.x, next.x, SkateMotion.ACCELERATION * TABLE_TOLERANCE, "x");
        assertEquals(vanilla.z, next.z, SkateMotion.ACCELERATION * TABLE_TOLERANCE, "z");
    }

    private static Vec3 vanillaInputVector(Vec3 input, float speed, float yRot) {
        double length = input.lengthSqr();
        if (length < 1.0E-7) {
            return Vec3.ZERO;
        }
        Vec3 movement = (length > 1.0 ? input.normalize() : input).scale(speed);
        float sin = Mth.sin(yRot * (float) (Math.PI / 180.0));
        float cos = Mth.cos(yRot * (float) (Math.PI / 180.0));
        return new Vec3(
                movement.x * cos - movement.z * sin,
                movement.y,
                movement.z * cos + movement.x * sin);
    }

    /**
     * Whatever the keys and the turns, a skater starting at rest never goes faster than the speed
     * at which friction takes away what the keys add: ACCELERATION / (1 - friction).
     */
    @Property
    void noKeysOrTurnsPushASkaterPastTheTopSpeed(
            @ForAll("frictions") float friction,
            @ForAll @Size(max = 300) List<@From("ticks") Tick> ticks) {
        double topSpeed = SkateMotion.ACCELERATION / (1.0 - friction);
        Vec3 velocity = Vec3.ZERO;
        for (Tick tick : ticks) {
            velocity =
                    horizontal(
                            SkateMotion.nextVelocity(
                                    velocity, friction, 0.08, tick.input(), tick.yRot()));
            assertTrue(
                    velocity.horizontalDistance() <= topSpeed * (1.0 + TABLE_TOLERANCE),
                    velocity.horizontalDistance() + " above " + topSpeed);
        }
    }

    @Property
    void aHeldKeyNeverSlowsASkaterDownOnTheWayToTheTopSpeed(
            @ForAll("frictions") float friction,
            @ForAll @FloatRange(min = -360.0F, max = 360.0F) float yRot) {
        Vec3 forward = new Vec3(0.0, 0.0, 1.0);
        Vec3 velocity = Vec3.ZERO;
        double speed = 0.0;
        for (int i = 0; i < 1000; i++) {
            velocity =
                    horizontal(SkateMotion.nextVelocity(velocity, friction, 0.08, forward, yRot));
            assertTrue(velocity.horizontalDistance() >= speed - EPSILON, "tick " + i);
            speed = velocity.horizontalDistance();
        }
    }

    @Test
    void topSpeedsOnVanillaBlocks() {
        assertEquals(0.1, topSpeedAfter(0.6F, 2000), 1.0E-6, "ice skates on stone");
        assertEquals(0.2, topSpeedAfter(0.8F, 2000), 1.0E-6, "ice skates on slime");
        assertEquals(2.0, topSpeedAfter(0.98F, 2000), 1.0E-3, "ice skates on ice");
        assertEquals(
                0.04 / (1.0 - 0.925F),
                topSpeedAfter(SkateMotion.ROLLER_SKATE_FRICTION, 2000),
                1.0E-6,
                "roller skates");
    }

    private static double topSpeedAfter(float friction, int ticks) {
        Vec3 forward = new Vec3(0.0, 0.0, 1.0);
        Vec3 velocity = Vec3.ZERO;
        for (int i = 0; i < ticks; i++) {
            velocity =
                    horizontal(SkateMotion.nextVelocity(velocity, friction, 0.08, forward, 0.0F));
        }
        return velocity.horizontalDistance();
    }

    /**
     * Frictions of 0.5 and above, where 1 - (1 - friction) is exact in float arithmetic. Below 0.5
     * vanilla's formula moves the friction by a rounding step even at modifier 1, for vanilla's
     * players as well.
     */
    @Provide
    Arbitrary<Float> skateFrictions() {
        return Arbitraries.oneOf(
                Arbitraries.of(VANILLA_FRICTIONS),
                Arbitraries.just(SkateMotion.ROLLER_SKATE_FRICTION),
                Arbitraries.floats().between(0.5F, 0.99F));
    }

    /** LivingEntity#computeModifiedFriction, LivingEntity.java:518-520 in 26.3. */
    private static float vanillaModifiedFriction(float friction, float modifier) {
        return Mth.clamp(1.0F - (1.0F - friction) * modifier, 0.0F, 1.0F);
    }

    @Property
    void defaultAttributesKeepTheSkateFrictionExactly(@ForAll("skateFrictions") float friction) {
        assertEquals(
                Float.floatToIntBits(friction),
                Float.floatToIntBits(SkateMotion.groundFriction(friction, 1.0F, 1.0F)));
    }

    @Property
    void aFrictionModifierAboveOneScalesTheFrictionAsVanilla(
            @ForAll("skateFrictions") float friction,
            @ForAll @FloatRange(min = 1.0F, max = 20.0F) float frictionModifier,
            @ForAll @FloatRange(min = 0.0F, max = 5.0F) float airDragModifier) {
        assertEquals(
                vanillaModifiedFriction(friction, frictionModifier),
                SkateMotion.groundFriction(friction, frictionModifier, airDragModifier));
    }

    @Property
    void aFrictionModifierBelowOneKeepsTheTopSpeedBoundedByTheAirDrag(
            @ForAll("skateFrictions") float friction,
            @ForAll @FloatRange(min = 0.0F, max = 1.0F) float frictionModifier,
            @ForAll @FloatRange(min = 0.01F, max = 5.0F) float airDragModifier) {
        float share = SkateMotion.groundFriction(friction, frictionModifier, airDragModifier);
        float airDrag = vanillaModifiedFriction(0.91F, airDragModifier);
        assertTrue(share >= friction, share + " below " + friction);
        assertTrue(share <= Math.max(friction, airDrag), share + " above the air drag");
        assertTrue(share < 1.0F);
    }

    @Property
    void aLowerFrictionModifierNeverSlowsASkater(
            @ForAll("skateFrictions") float friction,
            @ForAll @FloatRange(min = 0.0F, max = 4.0F) float a,
            @ForAll @FloatRange(min = 0.0F, max = 4.0F) float b,
            @ForAll @FloatRange(min = 0.0F, max = 5.0F) float airDragModifier) {
        assertTrue(
                SkateMotion.groundFriction(friction, Math.min(a, b), airDragModifier)
                        >= SkateMotion.groundFriction(friction, Math.max(a, b), airDragModifier));
    }

    @Test
    void topSpeedsWithFrictionModifiers() {
        float ice = 0.98F;
        float roller = SkateMotion.ROLLER_SKATE_FRICTION;
        assertEquals(2.0, topSpeedAfter(SkateMotion.groundFriction(ice, 0.0F, 1.0F), 2000), 1e-3);
        assertEquals(2.0, topSpeedAfter(SkateMotion.groundFriction(ice, 1.0F, 1.0F), 2000), 1e-3);
        assertEquals(1.0, topSpeedAfter(SkateMotion.groundFriction(ice, 2.0F, 1.0F), 2000), 1e-3);
        double rollerTop = 0.04 / (1.0 - 0.925F);
        assertEquals(
                rollerTop,
                topSpeedAfter(SkateMotion.groundFriction(roller, 0.0F, 1.0F), 2000),
                1e-6);
        assertEquals(
                rollerTop,
                topSpeedAfter(SkateMotion.groundFriction(roller, 1.0F, 1.0F), 2000),
                1e-6);
        assertEquals(
                0.04 / (1.0 - 0.85F),
                topSpeedAfter(SkateMotion.groundFriction(roller, 2.0F, 1.0F), 2000),
                1e-6);
        assertEquals(
                0.04 / (1.0 - 0.91F),
                topSpeedAfter(SkateMotion.groundFriction(0.6F, 0.0F, 1.0F), 2000),
                1e-6,
                "ice skates on stone");
    }
}
