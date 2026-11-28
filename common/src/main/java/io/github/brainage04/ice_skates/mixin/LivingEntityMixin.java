package io.github.brainage04.ice_skates.mixin;

import io.github.brainage04.ice_skates.util.PlayerUtils;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.WalkAnimationState;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.IceBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends EntityMixin {
    @Shadow
    protected abstract double getEffectiveGravity();

    @Shadow
    protected abstract float getAirDrag();

    /**
     * The friction of the surface the skater last skated on. A skater that leaves the ground keeps
     * it through the air instead of taking vanilla's air friction. NaN when the skater did not
     * leave the ground skating.
     */
    @Unique private float takeOffFriction = Float.NaN;

    @Inject(method = "travel", at = @At("HEAD"), cancellable = true)
    private void ice_skates$travel(Vec3 inputVector, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!(self instanceof Player player)) return;

        boolean grounded = PlayerUtils.canSkate(player);
        boolean skatingInAir =
                !grounded && PlayerUtils.canSkateInAir(player) && !Float.isNaN(takeOffFriction);
        if (grounded || skatingInAir) {
            double gravity = this.getEffectiveGravity();

            float friction = takeOffFriction;

            if (grounded) {
                double particleDelta = 0.1;

                if (PlayerUtils.isWearingIceSkates(player)) {
                    BlockState state =
                            this.level()
                                    .getBlockState(this.getBlockPosBelowThatAffectsMyMovement());
                    Block block = state.getBlock();
                    friction = block.getFriction();

                    if (block instanceof IceBlock
                            || block == Blocks.PACKED_ICE
                            || block == Blocks.BLUE_ICE) {
                        spawnFeetParticles(particleDelta, ParticleTypes.SNOWFLAKE);
                    }
                } else if (PlayerUtils.isWearingRollerSkates(player)) {
                    friction = PlayerUtils.ROLLER_SKATE_FRICTION;

                    spawnFeetParticles(particleDelta, new DustParticleOptions(0x0F0F0F, 1.0F));
                }

                takeOffFriction = friction;
            }

            Vec3 velocity = this.getDeltaMovement();

            velocity =
                    new Vec3(
                            velocity.x * friction,
                            grounded ? velocity.y - gravity : velocity.y,
                            velocity.z * friction);

            float forwardAccel = 0.0F;
            if (inputVector.z > 0) {
                forwardAccel += PlayerUtils.SKATE_ACCELERATION;
            } else if (inputVector.z < 0) {
                forwardAccel -= PlayerUtils.SKATE_ACCELERATION;
            }

            float strafeAccel = 0.0F;
            if (inputVector.x > 0) {
                strafeAccel += PlayerUtils.SKATE_ACCELERATION;
            } else if (inputVector.x < 0) {
                strafeAccel -= PlayerUtils.SKATE_ACCELERATION;
            }

            float accelerationLength =
                    Mth.sqrt(forwardAccel * forwardAccel + strafeAccel * strafeAccel);
            if (accelerationLength > PlayerUtils.SKATE_ACCELERATION) {
                float scale = PlayerUtils.SKATE_ACCELERATION / accelerationLength;
                forwardAccel *= scale;
                strafeAccel *= scale;
            }

            if (forwardAccel != 0.0F || strafeAccel != 0.0F) {
                float yRot = this.getYRot();
                float rad = yRot * ((float) Math.PI / 180F);

                double xForward = Mth.sin(-rad) * forwardAccel;
                double zForward = Mth.cos(rad) * forwardAccel;

                double xStrafe = Mth.cos(rad) * strafeAccel;
                double zStrafe = Mth.sin(rad) * strafeAccel;

                velocity = velocity.add(xForward + xStrafe, 0.0, zForward + zStrafe);
            }

            this.setDeltaMovement(velocity);
            this.move(MoverType.SELF, this.getDeltaMovement());

            if (!grounded) {
                // Vanilla moves first and only then applies gravity and the vertical air drag
                // (LivingEntity.travelInAir), so a jump or a fall keeps its vanilla trajectory.
                Vec3 moved = this.getDeltaMovement();
                this.setDeltaMovement(
                        new Vec3(moved.x, (moved.y - gravity) * this.getAirDrag(), moved.z));
            }

            ci.cancel();
        } else {
            takeOffFriction = Float.NaN;
        }
    }

    @Unique
    private void spawnFeetParticles(double particleDelta, ParticleOptions particleOptions) {
        this.level()
                .addParticle(
                        particleOptions,
                        this.getX(),
                        this.getY() + 0.1F,
                        this.getZ(),
                        particleDelta,
                        particleDelta,
                        particleDelta);
    }

    @Unique private static final float WALK_ANIMATION_SPEED = 0.7F;

    @Inject(
            method = "tick",
            at =
                    @At(
                            value = "INVOKE",
                            target = "Lnet/minecraft/world/entity/LivingEntity;tickHeadTurn(F)V"))
    private void ice_skates$tickHeadTurn(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;

        if (!(self instanceof Player player)) return;

        if (PlayerUtils.isWearingIceSkates(player) || PlayerUtils.isWearingRollerSkates(player)) {
            WalkAnimationState walkAnim = player.walkAnimation;
            walkAnim.setSpeed(walkAnim.speed() * WALK_ANIMATION_SPEED);
        }
    }
}
