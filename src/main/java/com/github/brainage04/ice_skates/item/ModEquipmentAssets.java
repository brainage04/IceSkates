package com.github.brainage04.ice_skates.item;

import com.github.brainage04.ice_skates.IceSkates;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;

public class ModEquipmentAssets {
    public static final ResourceKey<EquipmentAsset> ICE_SKATES = ResourceKey.create(EquipmentAssets.ROOT_ID, IceSkates.id("ice_skates"));
}
