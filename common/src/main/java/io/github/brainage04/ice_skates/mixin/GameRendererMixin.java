package io.github.brainage04.ice_skates.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.brainage04.ice_skates.util.PlayerUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
@Environment(EnvType.CLIENT)
public abstract class GameRendererMixin {
    @Inject(method = "bobView", at = @At("HEAD"), cancellable = true)
    private void ice_skates$bobView(
            CameraRenderState cameraRenderState, PoseStack poseStack, CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();

        if (client.player == null) return;

        if ((PlayerUtils.isWearingIceSkates(client.player)
                        || PlayerUtils.isWearingRollerSkates(client.player))
                && client.player.onGround()) {
            ci.cancel();
        }
    }
}
