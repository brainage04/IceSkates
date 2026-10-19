package io.github.brainage04.ice_skates;

import io.github.brainage04.ice_skates.item.ModItems;
import io.github.brainage04.ice_skates.util.PlayerUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Loader-neutral server GameTest bodies. Both loaders compile this source set into their GameTest
 * mods: Fabric runs them through {@code @GameTest} methods, NeoForge through registered test
 * functions and {@code test_instance} data.
 */
public final class IceSkatesGameTests {
    private IceSkatesGameTests() { }

    public static void skatesAndSwordsAreRegisteredWithDyeRecipes(GameTestHelper context) {
        requireItem(ModItems.iceSkates(), "ice skates");
        requireItem(ModItems.rollerSkates(), "roller skates");
        requireItem(ModItems.iceSword(), "ice sword");
        requireItem(ModItems.packedIceSword(), "packed ice sword");
        requireItem(ModItems.blueIceSword(), "blue ice sword");
        verifyItemData(context, ModItems.iceSkates(), "ice_skates_dyed");
        verifyItemData(context, ModItems.rollerSkates(), "roller_skates_dyed");

        ServerPlayer player = context.makeMockServerPlayerInLevel();
        player.setItemSlot(EquipmentSlot.FEET, new ItemStack(ModItems.iceSkates()));
        player.setOnGround(true);
        verifyHungerBehavior(player);
        verifySkatingMovement(player);
        context.succeed();
    }

    public static void skatingControlsAndFriction(GameTestHelper context) {
        ServerPlayer player = context.makeMockServerPlayerInLevel();
        context.runBeforeTestEnd(player::discard);
        player.setNoGravity(true);
        for (Item skates : List.of(ModItems.iceSkates(), ModItems.rollerSkates())) {
            player.setItemSlot(EquipmentSlot.FEET, new ItemStack(skates));
            resetPlayer(context, player, Blocks.STONE);
            player.travel(new Vec3(0, 0, 1));
            Vec3 forward = player.getDeltaMovement();
            context.assertTrue(forward.z > 0 && Math.abs(forward.x) < 0.00001, "Forward follows player yaw");

            resetPlayer(context, player, Blocks.STONE);
            player.travel(new Vec3(1, 0, 1));
            context.assertTrue(Math.abs(player.getDeltaMovement().horizontalDistance() - forward.horizontalDistance()) < 0.00001,
                    "Diagonal input must not skate faster");

            resetPlayer(context, player, Blocks.STONE);
            player.setYRot(90);
            player.travel(new Vec3(0, 0, 1));
            context.assertTrue(player.getDeltaMovement().x < 0 && Math.abs(player.getDeltaMovement().z) < 0.00001,
                    "Forward rotates with player yaw");

            resetPlayer(context, player, Blocks.STONE);
            player.travel(new Vec3(0, 0, -1));
            context.assertTrue(player.getDeltaMovement().z < 0, "Backward input reverses movement");

            double stone = coast(context, player, Blocks.STONE);
            double ice = coast(context, player, Blocks.ICE);
            double blueIce = coast(context, player, Blocks.BLUE_ICE);
            if (skates == ModItems.iceSkates()) {
                context.assertTrue(stone < ice && ice < blueIce, "Ice skates preserve surface friction differences");
            } else {
                context.assertTrue(Math.abs(stone - ice) < 0.00001 && Math.abs(ice - blueIce) < 0.00001,
                        "Roller skates use the same friction across surfaces");
            }
        }
        context.succeed();
    }

    public static void exhaustionFollowsSkatingState(GameTestHelper context) {
        for (Item skates : List.of(ModItems.iceSkates(), ModItems.rollerSkates())) {
            ServerPlayer skating = newPlayer(context, skates);
            verifyHungerBehavior(skating);

            ServerPlayer airborne = newPlayer(context, skates);
            airborne.setOnGround(false);
            verifyNormalExhaustion(context, airborne, "Airborne skates");

            ServerPlayer powderSnow = newPlayer(context, skates);
            powderSnow.setIsInPowderSnow(true);
            verifyNormalExhaustion(context, powderSnow, "Powder snow");

            ServerPlayer swimming = newPlayer(context, skates);
            resetPlayer(context, swimming, Blocks.STONE);
            context.setBlock(new BlockPos(2, 2, 2), Blocks.WATER);
            swimming.baseTick();
            swimming.setOnGround(true);
            context.assertTrue(swimming.isInLiquid(), "Liquid setup must put the player in water");
            verifyNormalExhaustion(context, swimming, "Water");
            context.setBlock(new BlockPos(2, 2, 2), Blocks.AIR);
        }
        verifyNormalExhaustion(context, newPlayer(context, Items.IRON_BOOTS), "Ordinary boots");
        context.succeed();
    }

    public static void skatesCanBeDyedAndWashed(GameTestHelper context) {
        ServerPlayer player = context.makeMockServerPlayerInLevel();
        context.runBeforeTestEnd(player::discard);
        for (Item skates : List.of(ModItems.iceSkates(), ModItems.rollerSkates())) {
            ItemStack undyed = new ItemStack(skates);
            CraftingInput input = CraftingInput.of(2, 1, List.of(undyed, new ItemStack(Items.RED_DYE)));
            ItemStack dyed = context.getLevel().getServer().getRecipeManager()
                    .getRecipeFor(RecipeType.CRAFTING, input, context.getLevel()).orElseThrow().value().assemble(input);
            context.assertTrue(dyed.is(skates) && dyed.has(DataComponents.DYED_COLOR), "Dye recipe must color the skates");
            context.assertFalse(undyed.has(DataComponents.DYED_COLOR), "Dye crafting must not mutate its input");
            player.setItemInHand(InteractionHand.MAIN_HAND, dyed);
            BlockPos cauldron = new BlockPos(1, 1, 1);
            context.setBlock(cauldron, Blocks.WATER_CAULDRON);
            context.useBlock(cauldron, player);
            context.assertFalse(player.getMainHandItem().has(DataComponents.DYED_COLOR), "Water cauldron must wash skate dye");
        }
        context.succeed();
    }

    private static ServerPlayer newPlayer(GameTestHelper context, Item boots) {
        ServerPlayer player = context.makeMockServerPlayerInLevel();
        context.runBeforeTestEnd(player::discard);
        player.setItemSlot(EquipmentSlot.FEET, new ItemStack(boots));
        player.setOnGround(true);
        return player;
    }

    private static void verifyNormalExhaustion(GameTestHelper context, ServerPlayer player, String state) {
        context.assertFalse(PlayerUtils.canSkate(player), state + " must disengage skating");
        player.getFoodData().setFoodLevel(20);
        player.getFoodData().setSaturation(0);
        player.causeFoodExhaustion(5);
        player.getFoodData().tick(player);
        context.assertTrue(player.getFoodData().getFoodLevel() == 19, state + " must retain normal exhaustion");
    }

    private static void resetPlayer(GameTestHelper context, ServerPlayer player, Block surface) {
        for (int x = 1; x <= 3; x++) {
            for (int z = 1; z <= 3; z++) {
                context.setBlock(new BlockPos(x, 1, z), surface);
                context.setBlock(new BlockPos(x, 2, z), Blocks.AIR);
                context.setBlock(new BlockPos(x, 3, z), Blocks.AIR);
            }
        }
        player.setPos(context.absoluteVec(new Vec3(2.5, 2, 2.5)));
        player.setYRot(0);
        player.setOnGround(true);
        player.setDeltaMovement(Vec3.ZERO);
    }

    private static double coast(GameTestHelper context, ServerPlayer player, Block surface) {
        resetPlayer(context, player, surface);
        player.setDeltaMovement(new Vec3(0.2, 0, 0));
        player.travel(Vec3.ZERO);
        return player.getDeltaMovement().x;
    }

    private static void requireItem(Item item, String name) {
        if (item == null) throw new AssertionError("Missing " + name);
    }

    private static void verifyItemData(GameTestHelper context, Item item, String path) {
        ItemStack stack = new ItemStack(item);
        if (!stack.is(ItemTags.FOOT_ARMOR) || !stack.is(ItemTags.TRIMMABLE_ARMOR) || !stack.is(ItemTags.CAULDRON_CAN_REMOVE_DYE)) {
            throw new AssertionError(item + " is missing required armor tags");
        }
        boolean loaded = context.getLevel().getServer().getRecipeManager().getRecipes().stream()
                .anyMatch(recipe -> recipe.id().identifier().equals(IceSkatesCommon.id(path)));
        if (!loaded) throw new AssertionError("Missing dye recipe " + path);
    }

    private static void verifyHungerBehavior(ServerPlayer player) {
        FoodData foodData = player.getFoodData();
        foodData.setFoodLevel(20);
        foodData.setSaturation(0.0F);
        player.causeFoodExhaustion(5.0F);
        foodData.tick(player);
        if (foodData.getFoodLevel() != 20) throw new AssertionError("Skating added food exhaustion");
        foodData.addExhaustion(5.0F);
        foodData.tick(player);
        if (foodData.getFoodLevel() != 19) throw new AssertionError("Skates stopped existing food exhaustion from being processed");
        foodData.setFoodLevel(20);
        foodData.setSaturation(5.0F);
    }

    private static void verifySkatingMovement(ServerPlayer player) {
        double startingZ = player.getZ();
        player.setDeltaMovement(Vec3.ZERO);
        player.travel(new Vec3(0.0D, 0.0D, 1.0D));
        if (player.getZ() <= startingZ) throw new AssertionError("Skates did not move the player forward");
        player.setDeltaMovement(Vec3.ZERO);
    }
}
