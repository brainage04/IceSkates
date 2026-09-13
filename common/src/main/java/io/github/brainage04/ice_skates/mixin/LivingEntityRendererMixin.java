package io.github.brainage04.ice_skates.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.brainage04.ice_skates.item.ModItems;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {
    @Inject(method = "submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/LivingEntityRenderer;scale(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;)V", shift = At.Shift.AFTER))
    private void ice_skates$raiseWearer(LivingEntityRenderState state, PoseStack poses,
                                      SubmitNodeCollector collector, CameraRenderState camera, CallbackInfo ci) {
        if (!(state instanceof HumanoidRenderState humanoid)) return;
        ItemStack boots = humanoid.feetEquipment;
        if (!HumanoidArmorLayer.shouldRender(boots, EquipmentSlot.FEET)) return;
        float soleTop;
        if (boots.is(ModItems.iceSkates())) soleTop = 3.4F;
        else if (boots.is(ModItems.rollerSkates())) soleTop = 4.25F;
        else return;

        // Move the whole wearer, including held items and other armor, onto the runner/wheels.
        // This is a render offset only: collision, camera height, and skating physics are unchanged.
        poses.translate(0, -soleTop * state.ageScale / 16, 0);
    }
}
