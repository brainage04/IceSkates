package com.github.brainage04.ice_skates.mixin;

import com.github.brainage04.ice_skates.IceSkateState;
import com.github.brainage04.ice_skates.util.PlayerUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class FootstepSoundMixin {
    @Unique
    private int ticksSinceLastStep = 0;

    @Inject(method = "playStepSound", at = @At("HEAD"), cancellable = true)
    private void onPlayStepSound(BlockPos pos, BlockState state, CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        
        if (!(self instanceof Player player)) return;
        if (!PlayerUtils.isWearingIceSkates(player)) return;
        
        IceSkateState.IceSkateData skateState = IceSkateState.get(player);
        
        if (!skateState.isGliding) {
            ticksSinceLastStep = 0;
            return; // normal footsteps when not gliding
        }
        
        float speed = skateState.currentSpeed;
        int requiredDelay = calculateStepDelay(speed);
        
        if (ticksSinceLastStep < requiredDelay) {
            ci.cancel(); // suppress this footstep
        } else {
            ticksSinceLastStep = 0; // reset/allow the sound
        }
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        
        if (self instanceof Player) ticksSinceLastStep++;
    }

    @Unique
    private int calculateStepDelay(float speed) {
        // normal stepping at very low speed
        if (speed < 0.1F) return 0;

        return Math.round(speed * 8f);
    }
}