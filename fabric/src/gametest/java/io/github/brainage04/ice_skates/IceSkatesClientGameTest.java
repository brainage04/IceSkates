package io.github.brainage04.ice_skates;

import io.github.brainage04.ice_skates.client.ServerSupport;
import io.github.brainage04.ice_skates.client.SkateRenderState;
import io.github.brainage04.ice_skates.item.ModItems;
import io.github.brainage04.minecraftmodconventions.ClientGameTestServers;
import java.util.List;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.player.inventory.Hotbar;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.resources.palette.PalettedTextureManager;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.equipment.trim.ArmorTrim;
import net.minecraft.world.phys.Vec3;

/**
 * Checks the skates' item models, the worn trim's texture and the creative tabs and saved hotbar on
 * a server with Ice Skates, then films the showcase ({@link IceSkatesShowcase}) and checks that
 * each runway run, skated with the forward key, carries the skater straight onto the stone and
 * stops there.
 */
@SuppressWarnings("UnstableApiUsage")
public final class IceSkatesClientGameTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        ClientGameTestServers.withDedicatedServer(
                context,
                IceSkatesShowcase.serverProperties(),
                "IceSkates item rendering GameTest",
                IceSkatesShowcase::buildStage,
                server -> {
                    IceSkatesShowcase.setUp(context, server);
                    verifyItemModels(context);
                    verifyTrimTexture(context);
                    verifyCreativeOnServerWithIceSkates(context);
                    for (IceSkatesShowcase.RunwayEnd run :
                            IceSkatesShowcase.film(context, server)) {
                        verifyRunway(run);
                    }
                });
    }

    private static void verifyRunway(IceSkatesShowcase.RunwayEnd run) {
        Vec3 end = run.position();
        System.out.println(
                "[ICE] " + run.title() + " ended at " + end + ", on stone: " + run.onStone());
        // Straight, onto the stone and to rest on it: without leaving the runway or turning off its
        // centre line.
        if (!run.onStone()
                || end.z > IceSkatesShowcase.RUNWAY_ICE_END_Z
                || end.z < IceSkatesShowcase.RUNWAY_END_Z + 0.3D
                || Math.abs(end.x - IceSkatesShowcase.RUN_START.x) > 0.5D) {
            throw new AssertionError(
                    run.title() + " must skate straight onto the stone and stop on it: " + end);
        }
    }

    private static void verifyCreativeOnServerWithIceSkates(ClientGameTestContext context) {
        context.runOnClient(
                client -> {
                    if (!ServerSupport.serverHasIceSkates()) {
                        throw new AssertionError("The test server has Ice Skates");
                    }
                    List<Item> items =
                            List.of(
                                    ModItems.iceSkates(),
                                    ModItems.rollerSkates(),
                                    ModItems.iceSword(),
                                    ModItems.packedIceSword(),
                                    ModItems.blueIceSword(),
                                    ModItems.iceSkateBlades(),
                                    ModItems.rollerSkateWheels());
                    for (Item item : items) {
                        if (!ServerSupport.isIceSkatesItem(new ItemStack(item))) {
                            throw new AssertionError(item + " must count as an Ice Skates item");
                        }
                    }
                    if (ServerSupport.isIceSkatesItem(new ItemStack(Items.IRON_BOOTS))
                            || ServerSupport.isIceSkatesItem(ItemStack.EMPTY)) {
                        throw new AssertionError("Vanilla items are not Ice Skates items");
                    }

                    CreativeModeTabs.tryRebuildTabContents(
                            client.player.connection.enabledFeatures(),
                            true,
                            client.level.registryAccess());
                    CreativeModeTab combat =
                            BuiltInRegistries.CREATIVE_MODE_TAB.getValueOrThrow(
                                    CreativeModeTabs.COMBAT);
                    CreativeModeTab ingredients =
                            BuiltInRegistries.CREATIVE_MODE_TAB.getValueOrThrow(
                                    CreativeModeTabs.INGREDIENTS);
                    for (Item item : items) {
                        ItemStack stack = new ItemStack(item);
                        if (!(combat.contains(stack) || ingredients.contains(stack))
                                || !CreativeModeTabs.searchTab().contains(stack)) {
                            throw new AssertionError(
                                    item + " must be in its creative tab and the search");
                        }
                    }

                    Hotbar hotbar = new Hotbar();
                    hotbar.storeFrom(client.player.getInventory(), client.level.registryAccess());
                    ItemStack restored = hotbar.load(client.level.registryAccess()).getFirst();
                    if (!restored.is(ModItems.iceSkates())) {
                        throw new AssertionError(
                                "A saved hotbar must keep its skates, got " + restored);
                    }
                });
    }

    private static void verifyItemModels(ClientGameTestContext context) {
        context.runOnClient(
                client -> {
                    RandomSource random = RandomSource.create(0);
                    for (Item item : List.of(ModItems.iceSkates(), ModItems.rollerSkates())) {
                        for (ItemDisplayContext display :
                                List.of(
                                        ItemDisplayContext.GUI,
                                        ItemDisplayContext.GROUND,
                                        ItemDisplayContext.FIRST_PERSON_RIGHT_HAND,
                                        ItemDisplayContext.NONE)) {
                            for (boolean dyed : List.of(false, true)) {
                                ItemStack stack = new ItemStack(item);
                                if (dyed)
                                    stack.set(
                                            DataComponents.DYED_COLOR, new DyedItemColor(0xCC2222));
                                ItemStackRenderState state = new ItemStackRenderState();
                                client.getItemModelResolver()
                                        .updateForTopItem(
                                                state,
                                                stack,
                                                display,
                                                client.level,
                                                client.player,
                                                0);
                                if (state.isEmpty()
                                        || state.getModelBoundingBox().getSize() <= 0
                                        || state.pickParticleMaterial(random)
                                                .sprite()
                                                .contents()
                                                .name()
                                                .equals(MissingTextureAtlasSprite.getLocation())) {
                                    throw new AssertionError(
                                            "Missing skate model or texture: "
                                                    + item
                                                    + " in "
                                                    + display);
                                }
                            }
                        }
                    }
                });
    }

    private static void verifyTrimTexture(ClientGameTestContext context) {
        context.runOnClient(
                client -> {
                    ArmorTrim trim =
                            client.player
                                    .getItemBySlot(EquipmentSlot.FEET)
                                    .get(DataComponents.TRIM);
                    if (trim == null) {
                        throw new AssertionError(
                                "The worn skates must reach the client with their trim");
                    }
                    PalettedTextureManager.Handle texture =
                            ((SkateRenderState) new HumanoidRenderState())
                                    .ice_skates$trimTexture(trim);
                    PalettedTextureManager.Handle missing =
                            client.getPalettedTextureManager()
                                    .getOrPrepare(
                                            Identifier.fromNamespaceAndPath(
                                                    "ice_skates",
                                                    "trims/entity/humanoid/no_such_pattern"),
                                            trim.material().value().paletteId());
                    if (texture == null
                            || texture.textureLocation() == null
                            || texture == missing) {
                        throw new AssertionError(
                                "The skate trim must use the pattern's real texture, got "
                                        + texture);
                    }
                });
    }
}
