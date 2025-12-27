package com.github.brainage04.ice_skates.mixin;

import com.github.brainage04.ice_skates.IceSkateState;
import com.github.brainage04.ice_skates.IceSurfaceType;
import com.github.brainage04.ice_skates.util.PlayerUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class IceSkateMovementMixin {

    @Unique
    private static final float BASE_ACCEL = 0.04F;
    
    // doubled acceleration multipliers for ice
    @Unique
    private static final float ICE_ACCEL_MULTIPLIER = 2.0F;

    @Inject(method = "travel", at = @At("HEAD"), cancellable = true)
    private void onTravel(Vec3 movementInput, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        
        if (!(self instanceof Player player)) return;
        if (!PlayerUtils.isWearingIceSkates(player)) return;

        IceSkateState.IceSkateData state = IceSkateState.get(player);

        // Handle air movement with reduced resistance
        if (!player.onGround()) {
            if (state.hasAirGrace()) {
                applyReducedAirResistance(player, movementInput);
                state.ticksSinceOnIce++;
                state.isGliding = false;
                ci.cancel();
            }
            return;
        }

        BlockPos below = player.blockPosition().below();
        BlockState blockBelow = player.level().getBlockState(below);
        
        IceSurfaceType surfaceType = getIceSurfaceType(blockBelow);
        
        if (surfaceType == IceSurfaceType.NONE) {
            state.isGliding = false;
            state.ticksSinceOnIce++;
            return; // Use vanilla movement for non-ice
        }

        // We're on ice - apply skating physics
        state.isGliding = true;
        state.ticksSinceOnIce = 0;
        
        applyIceSkateMovement(player, movementInput, surfaceType, state);
        spawnIceParticles(player, state);
        
        ci.cancel();
    }

    @Unique
    private void applyIceSkateMovement(
        Player player,
        Vec3 input,
        IceSurfaceType surface,
        IceSkateState.IceSkateData state
    ) {
        float friction = surface.friction;
        float accelMultiplier = surface.isIce ? ICE_ACCEL_MULTIPLIER : 1.0F;
        float maxSpeed = surface.maxSpeed;
        
        float accel = BASE_ACCEL * accelMultiplier;
        
        // calculate push direction from input
        float strafeInput = (float) -input.x;
        float forwardInput = (float) input.z;
        
        float yawRad = player.getYRot() * (Mth.PI / 180F);
        float pushX = (-Mth.sin(yawRad) * forwardInput - Mth.cos(yawRad) * strafeInput) * accel;
        float pushZ = (Mth.cos(yawRad) * forwardInput - Mth.sin(yawRad) * strafeInput) * accel;

        // apply friction decay and add new input
        Vec3 delta = player.getDeltaMovement();
        double newX = delta.x * friction + pushX;
        double newY = delta.y - player.getGravity();
        double newZ = delta.z * friction + pushZ;
        
        // clamp to max speed (doubled for ice)
        double horizontalSpeed = Math.sqrt(newX * newX + newZ * newZ);
        if (horizontalSpeed > maxSpeed) {
            double scale = maxSpeed / horizontalSpeed;
            newX *= scale;
            newZ *= scale;
            horizontalSpeed = maxSpeed;
        }
        
        state.currentSpeed = (float) horizontalSpeed;
        
        player.setDeltaMovement(newX, newY, newZ);
        player.move(MoverType.SELF, player.getDeltaMovement());
    }

    @Unique
    private void applyReducedAirResistance(Player player, Vec3 input) {
        Vec3 delta = player.getDeltaMovement();
        
        // much higher friction in air = less slowdown (0.99 vs vanilla ~0.91)
        float airFriction = 0.99F;
        
        // minimal air control
        float airControl = 0.01F;
        float yawRad = player.getYRot() * (Mth.PI / 180F);
        float pushX = (-Mth.sin(yawRad) * (float) input.z) * airControl;
        float pushZ = (Mth.cos(yawRad) * (float) input.z) * airControl;
        
        double newX = delta.x * airFriction + pushX;
        double newY = delta.y - player.getGravity();
        double newZ = delta.z * airFriction + pushZ;
        
        player.setDeltaMovement(newX, newY, newZ);
        player.move(MoverType.SELF, player.getDeltaMovement());
    }

    @Unique
    private void spawnIceParticles(Player player, IceSkateState.IceSkateData state) {
        if (!player.level().isClientSide()) return;
        
        float speed = state.currentSpeed;
        if (speed < 0.01F) return;
        int particleCount = Math.max(1, Math.round(speed * 5));

        RandomSource random = player.getRandom();
        
        for (int i = 0; i < particleCount; i++) {
            double offsetX = (random.nextDouble() - 0.5) * 0.2;
            double offsetZ = (random.nextDouble() - 0.5) * 0.2;
            
            player.level().addParticle(
                ParticleTypes.SNOWFLAKE,
                player.getX() + offsetX,
                player.getY() + 0.1,
                player.getZ() + offsetZ,
                0, 0, 0
            );
        }
    }

    @Unique
    private IceSurfaceType getIceSurfaceType(BlockState state) {
        Block block = state.getBlock();
        
        if (block == Blocks.BLUE_ICE) return IceSurfaceType.BLUE_ICE;
        if (block == Blocks.PACKED_ICE) return IceSurfaceType.PACKED_ICE;
        if (block == Blocks.ICE || block == Blocks.FROSTED_ICE) return IceSurfaceType.ICE;
        
        return IceSurfaceType.NONE;
    }
}