package com.github.brainage04.ice_skates.mixin;

import com.github.brainage04.ice_skates.IceSkateState;
import com.github.brainage04.ice_skates.util.PlayerUtils;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.WalkAnimationState;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class SlowGaitMixin {
    @Inject(
        method = "tick",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;tickHeadTurn(F)V"
        )
    )
    private void slowDownWalkAnimation(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        
        if (!(self instanceof Player player)) return;
        if (!PlayerUtils.isWearingIceSkates(player)) return;
        
        IceSkateState.IceSkateData state = IceSkateState.get(player);
        
        if (state.isGliding) {
            WalkAnimationState walkAnim = player.walkAnimation;
            
            // Reduce animation speed to 15% - creates a slow glide look
            // instead of frantic running legs
            float reducedSpeed = walkAnim.speed() * 0.75F;
            walkAnim.setSpeed(reducedSpeed);
        }
    }
}