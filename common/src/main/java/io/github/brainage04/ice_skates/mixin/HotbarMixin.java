package io.github.brainage04.ice_skates.mixin;

import io.github.brainage04.ice_skates.client.ServerSupport;
import java.util.List;
import net.minecraft.client.player.inventory.Hotbar;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A hotbar saved where Ice Skates exists can still hold its items. On a server without Ice Skates
 * those slots load empty, both in the Saved Hotbars tab and when a hotbar is restored with its key,
 * so restoring one never sends the server an item it cannot decode.
 */
@Mixin(Hotbar.class)
public abstract class HotbarMixin {
    @Inject(method = "load", at = @At("RETURN"), cancellable = true)
    private void ice_skates$dropItemsTheServerLacks(
            HolderLookup.Provider registries, CallbackInfoReturnable<List<ItemStack>> cir) {
        if (ServerSupport.serverHasIceSkates()) return;
        cir.setReturnValue(
                cir.getReturnValue().stream()
                        .map(
                                stack ->
                                        ServerSupport.isIceSkatesItem(stack)
                                                ? ItemStack.EMPTY
                                                : stack)
                        .toList());
    }
}
