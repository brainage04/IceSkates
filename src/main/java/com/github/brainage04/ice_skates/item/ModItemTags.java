package com.github.brainage04.ice_skates.item;

import com.github.brainage04.ice_skates.IceSkates;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public class ModItemTags {
    public static final TagKey<Item> ICE_TOOL_MATERIALS = TagKey.create(Registries.ITEM, IceSkates.id("ice_tool_materials"));
    public static final TagKey<Item> PACKED_ICE_TOOL_MATERIALS = TagKey.create(Registries.ITEM, IceSkates.id("packed_ice_tool_materials"));
    public static final TagKey<Item> BLUE_ICE_TOOL_MATERIALS = TagKey.create(Registries.ITEM, IceSkates.id("blue_ice_tool_materials"));
}
