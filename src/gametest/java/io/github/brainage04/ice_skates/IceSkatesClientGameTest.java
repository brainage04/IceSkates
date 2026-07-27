package io.github.brainage04.ice_skates;

import io.github.brainage04.fabricmoddingconventions.ClientGameTestRecorder;
import io.github.brainage04.fabricmoddingconventions.ClientGameTestServers;
import io.github.brainage04.ice_skates.item.ModItems;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.Properties;

@SuppressWarnings("UnstableApiUsage")
public final class IceSkatesClientGameTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        Properties serverProperties = ClientGameTestServers.flatServerProperties();

        try (TestDedicatedServerContext server = context.worldBuilder().createServer(serverProperties)) {
            ClientGameTestServers.connectToDedicatedServer(context, server, "IceSkates item rendering GameTest");
            try {
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
                ClientGameTestServers.disconnectFromDedicatedServer(context);
            }
        }
    }

    private static void preparePlayer(ServerPlayer player) {
        player.getInventory().clearContent();
        ServerLevel level = player.level();
        verifyItemData(level, ModItems.iceSkates(), "ice_skates_dyed");
        verifyItemData(level, ModItems.rollerSkates(), "roller_skates_dyed");
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
        verifyHungerBehavior(player);
        verifySkatingMovement(player);
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

    private static void verifyItemData(ServerLevel level, Item item, String dyeRecipePath) {
        ItemStack stack = new ItemStack(item);
        if (!stack.is(ItemTags.FOOT_ARMOR)
                || !stack.is(ItemTags.TRIMMABLE_ARMOR)
                || !stack.is(ItemTags.CAULDRON_CAN_REMOVE_DYE)) {
            throw new AssertionError(item + " is missing required armor tags");
        }

        boolean dyeRecipeLoaded = level.getServer().getRecipeManager().getRecipes().stream()
                .anyMatch(recipe -> recipe.id().identifier().equals(IceSkatesCommon.id(dyeRecipePath)));
        if (!dyeRecipeLoaded) {
            throw new AssertionError("Missing dye recipe " + dyeRecipePath);
        }
    }

    private static void verifyHungerBehavior(ServerPlayer player) {
        FoodData foodData = player.getFoodData();
        foodData.setFoodLevel(20);
        foodData.setSaturation(0.0F);

        player.causeFoodExhaustion(5.0F);
        foodData.tick(player);
        if (foodData.getFoodLevel() != 20) {
            throw new AssertionError("Skating added food exhaustion");
        }

        foodData.addExhaustion(5.0F);
        foodData.tick(player);
        if (foodData.getFoodLevel() != 19) {
            throw new AssertionError("Skates stopped existing food exhaustion from being processed");
        }

        foodData.setFoodLevel(20);
        foodData.setSaturation(5.0F);
    }

    private static void verifySkatingMovement(ServerPlayer player) {
        double startingZ = player.getZ();
        player.setDeltaMovement(Vec3.ZERO);
        player.travel(new Vec3(0.0D, 0.0D, 1.0D));
        if (player.getZ() <= startingZ) {
            throw new AssertionError("Skates did not move the player forward");
        }

        player.teleportTo(0.5D, 64.0D, 0.5D);
        player.setOnGround(true);
        player.setDeltaMovement(Vec3.ZERO);
    }

}
