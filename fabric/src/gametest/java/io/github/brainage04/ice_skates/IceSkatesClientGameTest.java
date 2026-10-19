package io.github.brainage04.ice_skates;

import io.github.brainage04.fabricmoddingconventions.ClientGameTestRecorder;
import io.github.brainage04.fabricmoddingconventions.ClientGameTestServers;
import io.github.brainage04.ice_skates.item.ModItems;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.Properties;

@SuppressWarnings("UnstableApiUsage")
public final class IceSkatesClientGameTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        Properties serverProperties = ClientGameTestServers.flatServerProperties();

        ClientGameTestServers.withDedicatedServer(context, serverProperties, "IceSkates item rendering GameTest", server -> { try {
            server.runOnServer(minecraftServer -> preparePlayer(
                    minecraftServer.getPlayerList().getPlayers().getFirst()));
            ClientGameTestServers.assertClientWorldAndPlayerAvailable(context);
            context.waitTicks(20);
        
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
        } finally {
            ;
        } });
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
