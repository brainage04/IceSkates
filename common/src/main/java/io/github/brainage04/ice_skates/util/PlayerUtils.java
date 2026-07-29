package io.github.brainage04.ice_skates.util;

import io.github.brainage04.ice_skates.item.ModItems;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;

public class PlayerUtils {
    public static boolean isWearingIceSkates(Player player) {
        return player.getItemBySlot(EquipmentSlot.FEET).is(ModItems.iceSkates());
    }

    public static boolean isWearingRollerSkates(Player player) {
        return player.getItemBySlot(EquipmentSlot.FEET).is(ModItems.rollerSkates());
    }

    public static boolean canSkate(Player player) {
        return (isWearingIceSkates(player) || isWearingRollerSkates(player)) &&
                player.onGround() &&
                !player.isInLiquid() &&
                !player.isInPowderSnow;
    }
}
