package io.github.brainage04.ice_skates.client;

import io.github.brainage04.ice_skates.IceSkatesCommon;
import io.github.brainage04.ice_skates.item.ModItemTags;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

/**
 * Whether the server the client is connected to has Ice Skates.
 *
 * <p>Neither loader stops a client with Ice Skates from joining a server without it: Fabric's
 * registry sync does nothing against a vanilla server and NeoForge accepts a vanilla server when no
 * mod needs a channel. The client then keeps its own item ids, and a creative-mode packet that
 * carries an Ice Skates item has an id the server does not know, which fails to decode and drops
 * the connection.
 *
 * <p>The signal is the server's item tags: every server sends all its item tags to the client while
 * it joins, before the player can open an inventory, and a server with Ice Skates always has the
 * mod's own {@code ice_skates:ice_tool_materials} tag from the mod's data. In singleplayer the
 * client shares the integrated server's registries, tags included. This needs no channel of its
 * own, so it also recognises servers running an older Ice Skates.
 */
public final class ServerSupport {
    private ServerSupport() {}

    public static boolean serverHasIceSkates() {
        // Without a connection there is no server to send an item to.
        if (Minecraft.getInstance().getConnection() == null) return true;
        return BuiltInRegistries.ITEM.get(ModItemTags.ICE_TOOL_MATERIALS).isPresent();
    }

    public static boolean isIceSkatesItem(ItemStack stack) {
        return !stack.isEmpty()
                && BuiltInRegistries.ITEM
                        .getKey(stack.getItem())
                        .getNamespace()
                        .equals(IceSkatesCommon.MOD_ID);
    }

    public static boolean isUnknownToServer(ItemStack stack) {
        return isIceSkatesItem(stack) && !serverHasIceSkates();
    }
}
