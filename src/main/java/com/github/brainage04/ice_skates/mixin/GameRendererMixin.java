package com.github.brainage04.ice_skates.mixin;

import com.github.brainage04.ice_skates.util.PlayerUtils;
import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
@Environment(EnvType.CLIENT)
public abstract class GameRendererMixin {
    @Inject(method = "bobView", at = @At("HEAD"), cancellable = true)
    private void ice_skates$bobView(PoseStack poseStack, float partialTick, CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();
        
        if (client.player == null) return;

        // skip view bobbing if player is wearing skates and is grounded
        if ((PlayerUtils.isWearingIceSkates(client.player)
                || PlayerUtils.isWearingRollerSkates(client.player))
                && client.player.onGround()) {
            ci.cancel();
        }
    }
}