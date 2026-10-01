package io.github.brainage04.ice_skates;

import io.github.brainage04.ice_skates.item.ModItems;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

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
