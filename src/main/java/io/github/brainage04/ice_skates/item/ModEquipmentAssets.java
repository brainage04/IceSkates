package io.github.brainage04.ice_skates.item;

import io.github.brainage04.ice_skates.IceSkatesCommon;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;

public class ModEquipmentAssets {
    public static final ResourceKey<EquipmentAsset> ICE_SKATES = ResourceKey.create(EquipmentAssets.ROOT_ID, IceSkatesCommon.id("ice_skates"));
    public static final ResourceKey<EquipmentAsset> ROLLER_SKATES = ResourceKey.create(EquipmentAssets.ROOT_ID, IceSkatesCommon.id("roller_skates"));
}
