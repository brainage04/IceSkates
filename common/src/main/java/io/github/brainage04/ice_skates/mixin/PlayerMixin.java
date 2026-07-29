package io.github.brainage04.ice_skates.mixin;

import io.github.brainage04.ice_skates.util.PlayerUtils;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class PlayerMixin {
    @Inject(method = "causeFoodExhaustion", at = @At("HEAD"), cancellable = true)
    private void ice_skates$preventSkatingExhaustion(float exhaustion, CallbackInfo ci) {
        Player player = (Player) (Object) this;
        if (PlayerUtils.canSkate(player)) {
            ci.cancel();
        }
    }
}
