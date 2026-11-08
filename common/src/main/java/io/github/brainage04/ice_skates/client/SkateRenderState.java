package io.github.brainage04.ice_skates.client;

import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.resources.palette.PalettedTextureManager;
import net.minecraft.world.item.equipment.trim.ArmorTrim;

public interface SkateRenderState {
    ItemStackRenderState ice_skates$boot();

    PalettedTextureManager.Handle ice_skates$trimTexture(ArmorTrim trim);
}
