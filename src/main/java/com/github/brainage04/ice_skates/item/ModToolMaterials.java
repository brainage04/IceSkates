package com.github.brainage04.ice_skates.item;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorMaterials;

public class ModToolMaterials {
    public static final ToolMaterial ICE = new ToolMaterial(
            BlockTags.INCORRECT_FOR_WOODEN_TOOL, 2, 6.0F, 6.0F, 26, ModItemTags.ICE_TOOL_MATERIALS
    );
    public static final ToolMaterial PACKED_ICE = new ToolMaterial(
            BlockTags.INCORRECT_FOR_WOODEN_TOOL, 2 * 9, 8.0F, 8.0F, 28, ModItemTags.PACKED_ICE_TOOL_MATERIALS
    );
    public static final ToolMaterial BLUE_ICE = new ToolMaterial(
            BlockTags.INCORRECT_FOR_WOODEN_TOOL, 2 * 9 * 9, 10.0F, 10.0F, 30, ModItemTags.BLUE_ICE_TOOL_MATERIALS
    );
}
