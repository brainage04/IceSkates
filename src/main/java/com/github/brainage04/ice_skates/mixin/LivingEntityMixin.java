package com.github.brainage04.ice_skates.mixin;

import com.github.brainage04.ice_skates.util.PlayerUtils;
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
    @Shadow protected abstract double getEffectiveGravity();

    @Inject(method = "travel", at = @At("HEAD"), cancellable = true)
    private void ice_skates$travel(Vec3 inputVector, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!(self instanceof Player player)) return;

        if (PlayerUtils.canSkate(player)) {
            // calculate gravity (vanilla logic)
            double gravity = this.getEffectiveGravity();

            float friction = 0.9F; // default air friction

            if (this.onGround()) {
                double particleDelta = 0.1;

                if (PlayerUtils.isWearingIceSkates(player)) {
                    BlockState state = this.level().getBlockState(this.getBlockPosBelowThatAffectsMyMovement());
                    Block block = state.getBlock();
                    friction = block.getFriction();

                    // particles
                    if (block instanceof IceBlock ||
                            block == Blocks.PACKED_ICE ||
                            block == Blocks.BLUE_ICE) {
                        spawnFeetParticles(particleDelta, ParticleTypes.SNOWFLAKE);
                    }
                } else if (PlayerUtils.isWearingRollerSkates(player)) {
                    friction = 0.925F;

                    spawnFeetParticles(particleDelta, new DustParticleOptions(0x0F0F0F, 1.0F));
                }
            }

            Vec3 velocity = this.getDeltaMovement();

            // apply friction and gravity
            velocity = new Vec3(
                    velocity.x * friction,
                    velocity.y - gravity,
                    velocity.z * friction
            );

            // forward/back and left/right input handling
            float forwardAccel = 0.0F;
            if (inputVector.z > 0) {
                forwardAccel += 0.04F;
            } else if (inputVector.z < 0) {
                forwardAccel -= 0.04F;
            }

            float strafeAccel = 0.0F;
            if (inputVector.x > 0) {
                strafeAccel += 0.04F;
            } else if (inputVector.x < 0) {
                strafeAccel -= 0.04F;
            }

            // apply rotation and acceleration
            if (forwardAccel != 0.0F || strafeAccel != 0.0F) {
                float yRot = this.getYRot();
                float rad = yRot * ((float) Math.PI / 180F);

                double xForward = Mth.sin(-rad) * forwardAccel;
                double zForward = Mth.cos(rad) * forwardAccel;

                double xStrafe = Mth.cos(rad) * strafeAccel;
                double zStrafe = Mth.sin(rad) * strafeAccel;

                velocity = velocity.add(
                        xForward + xStrafe,
                        0.0,
                        zForward + zStrafe
                );
            }

            this.setDeltaMovement(velocity);
            this.move(MoverType.SELF, this.getDeltaMovement());

            // todo: investigate why motion is [0 0 0] with skates on when it's [0 -0.08 0] without
            ci.cancel();
        }
    }

    @Unique
    private void spawnFeetParticles(double particleDelta, ParticleOptions particleOptions) {
        this.level().addParticle(
                particleOptions,
                this.getX(),
                this.getY() + 0.1F,
                this.getZ(),
                particleDelta,
                particleDelta,
                particleDelta
        );
    }

    @Unique
    private static final float WALK_ANIMATION_SPEED = 0.7F;

    @Inject(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/LivingEntity;tickHeadTurn(F)V"
            )
    )
    private void ice_skates$tickHeadTurn(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;

        if (!(self instanceof Player player)) return;

        if (PlayerUtils.isWearingIceSkates(player) || PlayerUtils.isWearingRollerSkates(player)) {
            WalkAnimationState walkAnim = player.walkAnimation;
            walkAnim.setSpeed(walkAnim.speed() * WALK_ANIMATION_SPEED);
        }
    }
}