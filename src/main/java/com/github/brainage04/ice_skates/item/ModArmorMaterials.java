package com.github.brainage04.ice_skates.item;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorMaterials;

public class ModArmorMaterials {
    public static final ArmorMaterial ICE_SKATES = new ArmorMaterial(
            15, ArmorMaterials.makeDefense(2, 5, 6, 2, 5), 9, SoundEvents.ARMOR_EQUIP_IRON, 0.0F, 0.0F, ItemTags.REPAIRS_IRON_ARMOR, ModEquipmentAssets.ICE_SKATES
    );
}
