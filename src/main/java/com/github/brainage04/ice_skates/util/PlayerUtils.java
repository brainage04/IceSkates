package com.github.brainage04.ice_skates.util;

import com.github.brainage04.ice_skates.item.ModItems;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class PlayerUtils {
    public static boolean isWearingIceSkates(Player player) {
        return player.getItemBySlot(EquipmentSlot.FEET).is(ModItems.ICE_SKATES);
    }

    public static boolean isWearingRollerSkates(Player player) {
        return player.getItemBySlot(EquipmentSlot.FEET).is(ModItems.ROLLER_SKATES);
    }

    public static boolean canSkate(Player player) {
        return (isWearingIceSkates(player) || isWearingRollerSkates(player)) &&
                player.onGround() &&
                !player.isInLiquid() &&
                !player.isInPowderSnow;
    }
}
