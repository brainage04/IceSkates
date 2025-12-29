package com.github.brainage04.ice_skates.mixin;

import com.github.brainage04.ice_skates.util.PlayerUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityMixin {
    @Shadow public abstract boolean onGround();
    @Shadow public abstract Vec3 getDeltaMovement();
    @Shadow public abstract void setDeltaMovement(Vec3 vec3);
    @Shadow public abstract float getYRot();
    @Shadow public abstract void move(MoverType type, Vec3 pos);
    @Shadow public abstract BlockPos getBlockPosBelowThatAffectsMyMovement();
    @Shadow public abstract Level level();
    @Shadow public abstract double getX();
    @Shadow public abstract double getY();
    @Shadow public abstract double getZ();

    @Unique private static final int DELAY_BETWEEN_FOOTSTEPS = 14;
    @Unique private int ticksSinceLastStep = 0;

    @Inject(method = "playStepSound", at = @At("HEAD"), cancellable = true)
    private void ice_skates$playStepSound(BlockPos pos, BlockState state, CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (!(self instanceof Player player)) return;

        if (!PlayerUtils.canSkate(player)) {
            ticksSinceLastStep = 0;
            return; // normal footsteps when not skating
        }

        // cancel all footsteps if skating
        ci.cancel();

        if (ticksSinceLastStep > DELAY_BETWEEN_FOOTSTEPS) {
            self.playSound(SoundEvents.BOAT_PADDLE_LAND);
            ticksSinceLastStep = 0;
        }
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void ice_skates$tick(CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (!(self instanceof Player)) return;

        ticksSinceLastStep++;
    }
}
