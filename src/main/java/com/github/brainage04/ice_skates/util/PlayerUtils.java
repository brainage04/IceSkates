package com.github.brainage04.ice_skates.util;

import com.github.brainage04.ice_skates.item.ModItems;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class PlayerUtils {
    public static boolean isWearingIceSkates(Player player) {
        ItemStack boots = player.getItemBySlot(EquipmentSlot.FEET);
        return boots.is(ModItems.ICE_SKATES);
    }
}
