package io.github.brainage04.ice_skates.util;

import io.github.brainage04.ice_skates.item.ModItems;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;

public class PlayerUtils {
    /** Horizontal speed kept each tick on roller skates, whatever the block. */
    public static final float ROLLER_SKATE_FRICTION = 0.925F;

    /** Horizontal speed a held movement key adds each tick while skating. */
    public static final float SKATE_ACCELERATION = 0.04F;

    public static boolean isWearingIceSkates(Player player) {
        return player.getItemBySlot(EquipmentSlot.FEET).is(ModItems.iceSkates());
    }

    public static boolean isWearingRollerSkates(Player player) {
        return player.getItemBySlot(EquipmentSlot.FEET).is(ModItems.rollerSkates());
    }

    public static boolean canSkate(Player player) {
        return (isWearingIceSkates(player) || isWearingRollerSkates(player))
                && player.onGround()
                && !player.isInLiquid()
                && !player.isInPowderSnow;
    }

    /**
     * Whether a skater off the ground is still skating: it left the ground wearing skates and
     * nothing else moves it, so liquid, powder snow, gliding, flight, a ladder, a mount and
     * levitation take over as in vanilla.
     */
    public static boolean canSkateInAir(Player player) {
        return (isWearingIceSkates(player) || isWearingRollerSkates(player))
                && !player.onGround()
                && !player.isInLiquid()
                && !player.isInPowderSnow
                && !player.isFallFlying()
                && !player.getAbilities().flying
                && !player.onClimbable()
                && !player.isPassenger()
                && !player.hasEffect(MobEffects.LEVITATION);
    }
}
