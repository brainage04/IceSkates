package com.github.brainage04.ice_skates.mixin;

import com.github.brainage04.ice_skates.util.PlayerUtils;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.food.FoodData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FoodData.class)
public class FoodDataMixin {
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    public void ice_skates$tick(ServerPlayer serverPlayer, CallbackInfo ci) {
        if (PlayerUtils.canSkate(serverPlayer)) ci.cancel();
    }
}
