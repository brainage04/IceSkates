package io.github.brainage04.ice_skates;

import io.github.brainage04.minecraftmodconventions.ClientGameTestRecorder;
import io.github.brainage04.minecraftmodconventions.ClientGameTestServers;
import io.github.brainage04.ice_skates.item.ModItems;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;

import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Properties;

@SuppressWarnings("UnstableApiUsage")
public final class IceSkatesClientGameTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        Properties serverProperties = ClientGameTestServers.flatServerProperties();

        ClientGameTestServers.withDedicatedServer(context, serverProperties, "IceSkates item rendering GameTest", server -> {
            server.runOnServer(minecraftServer -> preparePlayer(
                    minecraftServer.getPlayerList().getPlayers().getFirst()));
            ClientGameTestServers.assertClientWorldAndPlayerAvailable(context);
            context.waitTicks(20);
            verifyItemModels(context);
        
            ClientGameTestRecorder.startRecording(context);
            ClientGameTestRecorder.showStep(
                    context,
                    "ice_skates.held",
                    "Ice skates item layers",
                    "Boot and blade layers must both render in hand and hotbar"
            );
            context.waitTicks(40);
        
            context.runOnClient(client -> client.player.getInventory().setSelectedSlot(1));
            ClientGameTestRecorder.showStep(
                    context,
                    "roller_skates.held",
                    "Roller skates item layers",
                    "Boot and wheel layers must both render in hand and hotbar"
            );
            context.waitTicks(40);
        
            context.runOnClient(client -> client.player.getInventory().setSelectedSlot(0));
            ClientGameTestRecorder.showStep(
                    context,
                    "skates.dropped",
                    "Dropped skates",
                    "The dropped roller skates ahead retain both item texture layers"
            );
            context.waitTicks(50);
        });
    }

    private static void verifyItemModels(ClientGameTestContext context) {
        context.runOnClient(client -> {
            RandomSource random = RandomSource.create(0);
            for (Item item : List.of(ModItems.iceSkates(), ModItems.rollerSkates())) {
                for (ItemDisplayContext display : List.of(ItemDisplayContext.GUI, ItemDisplayContext.GROUND,
                        ItemDisplayContext.FIRST_PERSON_RIGHT_HAND, ItemDisplayContext.NONE)) {
                    for (boolean dyed : List.of(false, true)) {
                        ItemStack stack = new ItemStack(item);
                        if (dyed) stack.set(DataComponents.DYED_COLOR, new DyedItemColor(0xCC2222));
                        ItemStackRenderState state = new ItemStackRenderState();
                        client.getItemModelResolver().updateForTopItem(state, stack, display, client.level, client.player, 0);
                        if (state.isEmpty() || state.getModelBoundingBox().getSize() <= 0
                                || state.pickParticleMaterial(random).sprite().contents().name()
                                .equals(MissingTextureAtlasSprite.getLocation())) {
                            throw new AssertionError("Missing skate model or texture: " + item + " in " + display);
                        }
                    }
                }
            }
        });
    }

    private static void preparePlayer(ServerPlayer player) {
        player.getInventory().clearContent();
        ServerLevel level = player.level();
        for (BlockPos position : BlockPos.betweenClosed(-4, 63, -4, 4, 66, 4)) {
            level.setBlock(
                    position,
                    position.getY() == 63 ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(),
                    3
            );
        }

        player.getInventory().setItem(0, new ItemStack(ModItems.iceSkates()));
        player.getInventory().setItem(1, new ItemStack(ModItems.rollerSkates()));
        player.getInventory().setSelectedSlot(0);
        player.setItemSlot(EquipmentSlot.FEET, new ItemStack(ModItems.iceSkates()));
        player.teleportTo(0.5D, 64.0D, 0.5D);
        player.setOnGround(true);
        player.setYRot(0.0F);
        player.setXRot(20.0F);
        player.setDeltaMovement(Vec3.ZERO);

        ItemEntity droppedSkates = new ItemEntity(
                level,
                player.getX(),
                player.getY(),
                player.getZ() + 2.0D,
                new ItemStack(ModItems.rollerSkates())
        );
        droppedSkates.setPickUpDelay(200);
        player.level().addFreshEntity(droppedSkates);
    }

}
