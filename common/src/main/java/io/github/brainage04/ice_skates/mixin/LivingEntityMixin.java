package io.github.brainage04.ice_skates.mixin;

import io.github.brainage04.ice_skates.util.PlayerUtils;
import io.github.brainage04.ice_skates.util.SkateMotion;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
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

    @Inject(method = "travel", at = @At("HEAD"), cancellable = true)
    private void ice_skates$travel(Vec3 inputVector, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!(self instanceof Player player)) return;

        if (PlayerUtils.canSkate(player)) {
            double gravity = this.getEffectiveGravity();

            float friction = 0.9F;

            if (this.onGround()) {
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
                    friction = SkateMotion.ROLLER_SKATE_FRICTION;

                    spawnFeetParticles(particleDelta, new DustParticleOptions(0x0F0F0F, 1.0F));
                }
            }

            this.setDeltaMovement(
                    SkateMotion.nextVelocity(
                            this.getDeltaMovement(),
                            friction,
                            gravity,
                            inputVector,
                            this.getYRot()));
            this.move(MoverType.SELF, this.getDeltaMovement());

            ci.cancel();
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
