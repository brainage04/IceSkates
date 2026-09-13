package io.github.brainage04.ice_skates.client;

import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.trim.ArmorTrim;

public interface SkateRenderState {
    ItemStackRenderState ice_skates$boot();

    Identifier ice_skates$trimSprite(ArmorTrim trim, ResourceKey<EquipmentAsset> asset);
}
