package io.github.brainage04.ice_skates;

import io.github.brainage04.ice_skates.client.ServerSupport;
import io.github.brainage04.ice_skates.item.ModItems;
import io.github.brainage04.minecraftmodconventions.VanillaServerClientGameTests;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.world.item.ItemStack;

/**
 * Joins a plain vanilla server, which has no Ice Skates, and checks that none of its items can be
 * taken in creative there, with the conventions' creative-item checks. Runs only under {@code
 * :fabric:runVanillaServerClientGameTest}, which starts that server; in every other client run it
 * does nothing.
 */
@SuppressWarnings("UnstableApiUsage")
public final class IceSkatesVanillaServerClientGameTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        VanillaServerClientGameTests.checkCreativeItems(
                context,
                VanillaServerClientGameTests.CreativeItems.of(
                        "Ice Skates",
                        ServerSupport::serverHasIceSkates,
                        ServerSupport::isIceSkatesItem,
                        () -> new ItemStack(ModItems.iceSkates())));
    }
}
