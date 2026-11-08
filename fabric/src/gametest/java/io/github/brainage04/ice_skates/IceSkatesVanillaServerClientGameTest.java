package io.github.brainage04.ice_skates;

import io.github.brainage04.ice_skates.client.ServerSupport;
import io.github.brainage04.ice_skates.item.ModItems;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.player.inventory.Hotbar;
import net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;

/**
 * Joins a plain vanilla server, which has no Ice Skates, and checks that none of its items can be
 * taken in creative there. Runs only when {@value #SERVER_ENV} names such a server ({@code
 * host:port}), which {@code tools/vanilla-server-check.sh} starts beside the client; without it the
 * test does nothing.
 *
 * <p>It ends by sending the server an Ice Skates item directly, past the mod's guard, and expects
 * the disconnect the owner saw on Skyport, so a pass also shows that the check would catch the
 * kick.
 */
@SuppressWarnings("UnstableApiUsage")
public final class IceSkatesVanillaServerClientGameTest implements FabricClientGameTest {
    private static final String SERVER_ENV = "ICE_SKATES_VANILLA_SERVER";
    private static final int TIMEOUT_TICKS = 1200;

    /** The first hotbar slot in the player's inventory menu. */
    private static final int HOTBAR_MENU_SLOT = 36;

    @Override
    public void runTest(ClientGameTestContext context) {
        String address = System.getenv(SERVER_ENV);
        if (address == null || address.isBlank()) {
            System.out.println("[ICE] " + SERVER_ENV + " is not set; vanilla server check skipped");
            return;
        }

        context.runOnClient(
                client ->
                        ConnectScreen.startConnecting(
                                client.gui.screen(),
                                client,
                                ServerAddress.parseString(address),
                                new ServerData("vanilla", address, ServerData.Type.OTHER),
                                false,
                                null));
        context.waitFor(
                client ->
                        client.level != null
                                && client.player != null
                                && client.gui.screen() == null,
                TIMEOUT_TICKS);
        context.waitTicks(20);

        context.runOnClient(
                client -> {
                    if (ServerSupport.serverHasIceSkates()) {
                        throw new AssertionError(
                                "A vanilla server must not count as having Ice Skates");
                    }
                    if (!client.player.hasInfiniteMaterials()) {
                        throw new AssertionError(
                                "The vanilla server must put the player in creative");
                    }

                    CreativeModeTabs.tryRebuildTabContents(
                            client.player.connection.enabledFeatures(),
                            false,
                            client.level.registryAccess());
                    for (CreativeModeTab tab : CreativeModeTabs.allTabs()) {
                        for (ItemStack stack : tab.getDisplayItems()) {
                            if (ServerSupport.isIceSkatesItem(stack)) {
                                throw new AssertionError(
                                        stack
                                                + " is offered in "
                                                + tab.getDisplayName().getString());
                            }
                        }
                        for (ItemStack stack : tab.getSearchTabDisplayItems()) {
                            if (ServerSupport.isIceSkatesItem(stack)) {
                                throw new AssertionError(stack + " is offered in the search");
                            }
                        }
                    }

                    // A hotbar saved with skates in it, as one saved in singleplayer would be.
                    client.player.getInventory().setItem(0, new ItemStack(ModItems.iceSkates()));
                    Hotbar hotbar = new Hotbar();
                    hotbar.storeFrom(client.player.getInventory(), client.level.registryAccess());
                    client.player.getInventory().setItem(0, ItemStack.EMPTY);
                    ItemStack restored = hotbar.load(client.level.registryAccess()).getFirst();
                    if (!restored.isEmpty()) {
                        throw new AssertionError(
                                "A saved hotbar must load its skates empty here, got " + restored);
                    }

                    // What taking the skates from a creative tab does.
                    client.gameMode.handleCreativeModeItemAdd(
                            new ItemStack(ModItems.iceSkates()), HOTBAR_MENU_SLOT);
                });
        // The server would answer an undecodable packet by dropping the connection at once.
        context.waitTicks(60);
        context.runOnClient(
                client -> {
                    if (client.level == null
                            || client.getConnection() == null
                            || !client.getConnection().isAcceptingMessages()) {
                        throw new AssertionError(
                                "Taking skates in creative must not disconnect the client");
                    }
                    if (!client.player
                            .inventoryMenu
                            .getSlot(HOTBAR_MENU_SLOT)
                            .getItem()
                            .isEmpty()) {
                        throw new AssertionError(
                                "The slot must stay empty, since the server never got the skates");
                    }
                    System.out.println("[ICE] Vanilla server: nothing offered, still connected");

                    // The packet the guard holds back, sent directly: the owner's kick.
                    client.getConnection()
                            .send(
                                    new ServerboundSetCreativeModeSlotPacket(
                                            HOTBAR_MENU_SLOT, new ItemStack(ModItems.iceSkates())));
                });
        context.waitFor(client -> client.level == null, TIMEOUT_TICKS);
        System.out.println("[ICE] Vanilla server: the unguarded packet disconnected the client");
        context.setScreen(TitleScreen::new);
    }
}
