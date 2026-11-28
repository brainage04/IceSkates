package io.github.brainage04.ice_skates;

import com.mojang.logging.LogUtils;
import io.github.brainage04.ice_skates.item.ModItems;
import io.github.brainage04.ice_skates.util.PlayerUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

public final class IceSkatesGameTests {
    private static final Logger LOGGER = LogUtils.getLogger();

    /** Horizontal speed of the skater when it jumps, small enough to stay over the 3x3 floor. */
    private static final double TAKE_OFF_SPEED = 0.1;

    /** The most ticks one jump is followed; a jump lands after about twelve. */
    private static final int MAX_JUMP_TICKS = 20;

    /**
     * Ticks of the thrust phase: the throttle carries the skater fast, so it stops before the
     * test's walls.
     */
    private static final int THRUST_TICKS = 8;

    /** Airborne ticks a jump must give the checks, so a landing cannot hide a missing one. */
    private static final int MIN_AIRBORNE_TICKS = 6;

    private static final double SPEED_TOLERANCE = 1.0E-4;

    /** A cell above the 3x3 floor, where the yield test puts the skater in the air. */
    private static final BlockPos AIR_CELL = new BlockPos(2, 3, 2);

    /**
     * A ceiling the skater in the air cell hits head first when it rises at the fall test's speed.
     */
    private static final BlockPos CEILING_CELL = new BlockPos(2, 5, 2);

    private static final int LEVITATION_TICKS = 100;

    /** Upward speed and tick count of the fall test: the player stays clear of the floor. */
    private static final double FALL_START_SPEED = 0.3;

    private static final int FALL_TICKS = 4;

    /** Vanilla's vertical drag on a falling player, {@code LivingEntity.getAirDrag}. */
    private static final double VERTICAL_AIR_DRAG = 0.98;

    private IceSkatesGameTests() {}

    public static void skatesAndSwordsAreRegisteredWithDyeRecipes(GameTestHelper context) {
        requireItem(ModItems.iceSkates(), "ice skates");
        requireItem(ModItems.rollerSkates(), "roller skates");
        requireItem(ModItems.iceSword(), "ice sword");
        requireItem(ModItems.packedIceSword(), "packed ice sword");
        requireItem(ModItems.blueIceSword(), "blue ice sword");
        verifyItemData(context, ModItems.iceSkates(), "ice_skates_dyed");
        verifyItemData(context, ModItems.rollerSkates(), "roller_skates_dyed");

        ServerPlayer player = newPlayer(context, ModItems.iceSkates());
        verifyHungerBehavior(player);
        verifySkatingMovement(player);
        context.succeed();
    }

    public static void skatingControlsAndFriction(GameTestHelper context) {
        ServerPlayer player = (ServerPlayer) context.makeMockServerPlayer(GameType.CREATIVE);
        context.runBeforeTestEnd(player::discard);
        player.setNoGravity(true);
        for (Item skates : List.of(ModItems.iceSkates(), ModItems.rollerSkates())) {
            player.setItemSlot(EquipmentSlot.FEET, new ItemStack(skates));
            resetPlayer(context, player, Blocks.STONE);
            player.travel(new Vec3(0, 0, 1));
            Vec3 forward = player.getDeltaMovement();
            context.assertTrue(
                    forward.z > 0 && Math.abs(forward.x) < 0.00001, "Forward follows player yaw");

            resetPlayer(context, player, Blocks.STONE);
            player.travel(new Vec3(1, 0, 1));
            context.assertTrue(
                    Math.abs(
                                    player.getDeltaMovement().horizontalDistance()
                                            - forward.horizontalDistance())
                            < 0.00001,
                    "Diagonal input must not skate faster");

            resetPlayer(context, player, Blocks.STONE);
            player.setYRot(90);
            player.travel(new Vec3(0, 0, 1));
            context.assertTrue(
                    player.getDeltaMovement().x < 0
                            && Math.abs(player.getDeltaMovement().z) < 0.00001,
                    "Forward rotates with player yaw");

            resetPlayer(context, player, Blocks.STONE);
            player.travel(new Vec3(0, 0, -1));
            context.assertTrue(player.getDeltaMovement().z < 0, "Backward input reverses movement");

            double stone = coast(context, player, Blocks.STONE);
            double ice = coast(context, player, Blocks.ICE);
            double blueIce = coast(context, player, Blocks.BLUE_ICE);
            if (skates == ModItems.iceSkates()) {
                context.assertTrue(
                        stone < ice && ice < blueIce,
                        "Ice skates preserve surface friction differences");
            } else {
                context.assertTrue(
                        Math.abs(stone - ice) < 0.00001 && Math.abs(ice - blueIce) < 0.00001,
                        "Roller skates use the same friction across surfaces");
            }
        }
        context.succeed();
    }

    /**
     * A skater that leaves the ground by a jump keeps its skating physics in the air: the speed
     * decays by the friction of the surface it left, and the throttle adds the skating
     * acceleration. Each line of the log is one tick of the jump: the speed, whether the player
     * stood on the ground when the tick began, and the vertical speed.
     */
    public static void skatersKeepSkatingPhysicsInTheAir(GameTestHelper context) {
        ServerPlayer player = (ServerPlayer) context.makeMockServerPlayer(GameType.SURVIVAL);
        context.runBeforeTestEnd(player::discard);
        for (Skate skate :
                List.of(
                        new Skate(ModItems.iceSkates(), Blocks.BLUE_ICE),
                        new Skate(ModItems.rollerSkates(), Blocks.STONE))) {
            player.setItemSlot(EquipmentSlot.FEET, new ItemStack(skate.item()));
            float friction =
                    skate.item() == ModItems.iceSkates()
                            ? skate.surface().getFriction()
                            : PlayerUtils.ROLLER_SKATE_FRICTION;

            List<Tick> coast = jump(context, player, skate, Vec3.ZERO, MAX_JUMP_TICKS);
            logJump(skate, "coast", coast);
            List<Tick> airborne = airborneTicks(coast);
            context.assertTrue(
                    airborne.size() >= MIN_AIRBORNE_TICKS,
                    skate.name() + " jump must give " + MIN_AIRBORNE_TICKS + " airborne ticks");
            for (Tick tick : airborne) {
                context.assertTrue(
                        Math.abs(tick.speed() - tick.before() * friction) < SPEED_TOLERANCE,
                        skate.name()
                                + " must keep friction "
                                + friction
                                + " in the air, speed went "
                                + tick.before()
                                + " to "
                                + tick.speed());
            }

            List<Tick> thrust = jump(context, player, skate, new Vec3(0, 0, 1), THRUST_TICKS);
            logJump(skate, "thrust", thrust);
            for (Tick tick : airborneTicks(thrust)) {
                context.assertTrue(
                        Math.abs(
                                        tick.speed()
                                                - (tick.before() * friction
                                                        + PlayerUtils.SKATE_ACCELERATION))
                                < SPEED_TOLERANCE,
                        skate.name()
                                + " must add the skating acceleration in the air, speed went "
                                + tick.before()
                                + " to "
                                + tick.speed());
            }

            Tick previous = null;
            for (Tick tick : coast) {
                if (previous != null && !tick.onGroundBefore() && !tick.onGroundAfter()) {
                    double gravity = player.getGravity();
                    context.assertTrue(
                            Math.abs(
                                            tick.vertical()
                                                    - (previous.vertical() - gravity)
                                                            * VERTICAL_AIR_DRAG)
                                    < SPEED_TOLERANCE,
                            skate.name() + " must fall by vanilla's gravity and air drag");
                }
                previous = tick;
            }
        }
        context.succeed();
    }

    /**
     * A skater that left the ground skating moves as the same player in ordinary boots would once
     * something else takes over the movement in the air: creative flight, gliding, a ladder,
     * levitation and a mount.
     */
    public static void skatingYieldsToOtherMovementInTheAir(GameTestHelper context) {
        record Takeover(String name, Consumer<Player> apply) {}
        List<Takeover> takeovers =
                List.of(
                        new Takeover(
                                "Creative flight", player -> player.getAbilities().flying = true),
                        new Takeover("Gliding", Player::startFallFlying),
                        new Takeover(
                                "A ladder", player -> context.setBlock(AIR_CELL, Blocks.LADDER)),
                        new Takeover(
                                "Levitation",
                                player ->
                                        player.addEffect(
                                                new MobEffectInstance(
                                                        MobEffects.LEVITATION, LEVITATION_TICKS))),
                        new Takeover(
                                "A mount",
                                player -> {
                                    Entity pig =
                                            context.spawn(
                                                    EntityTypes.PIG,
                                                    new BlockPos(
                                                            AIR_CELL.getX(), 1, AIR_CELL.getZ()));
                                    player.startRiding(pig, true, false);
                                }));
        List<String> differing = new ArrayList<>();
        for (Takeover takeover : takeovers) {
            Vec3 skating = airTravel(context, ModItems.iceSkates(), takeover.apply());
            Vec3 boots = airTravel(context, Items.IRON_BOOTS, takeover.apply());
            LOGGER.info("IceSkatesAir {} skates {} boots {}", takeover.name(), skating, boots);
            if (skating.distanceTo(boots) >= SPEED_TOLERANCE) differing.add(takeover.name());
            context.setBlock(AIR_CELL, Blocks.AIR);
        }
        context.assertTrue(
                differing.isEmpty(),
                "Skates must yield to the takeover, but did not for " + differing);
        context.succeed();
    }

    /**
     * A skater's rise and fall is the same player's in ordinary boots: vanilla moves first and
     * applies gravity and the vertical air drag after, so a jump keeps its height and its landing
     * tick. Each entry is the player's height and vertical speed after one tick in the air.
     */
    public static void skatersFallAsVanillaDoes(GameTestHelper context) {
        assertFallMatchesBoots(context, "open air", player -> {});
        context.setBlock(CEILING_CELL, Blocks.STONE);
        assertFallMatchesBoots(
                context,
                "a ceiling with slow falling",
                player ->
                        player.addEffect(
                                new MobEffectInstance(MobEffects.SLOW_FALLING, LEVITATION_TICKS)));
        context.setBlock(CEILING_CELL, Blocks.AIR);
        context.succeed();
    }

    private static void assertFallMatchesBoots(
            GameTestHelper context, String name, Consumer<Player> setup) {
        List<Vec3> skating = fall(context, ModItems.iceSkates(), setup);
        List<Vec3> boots = fall(context, Items.IRON_BOOTS, setup);
        LOGGER.info("IceSkatesAir fall {} skates {} boots {}", name, skating, boots);
        for (int i = 0; i < skating.size(); i++) {
            context.assertTrue(
                    skating.get(i).distanceTo(boots.get(i)) < SPEED_TOLERANCE,
                    "Tick " + i + " of the fall in " + name + " must match ordinary boots");
        }
    }

    private static List<Vec3> fall(GameTestHelper context, Item boots, Consumer<Player> setup) {
        Player player = playerInAir(context, boots);
        setup.accept(player);
        player.setDeltaMovement(new Vec3(0, FALL_START_SPEED, TAKE_OFF_SPEED));
        List<Vec3> ticks = new ArrayList<>();
        for (int i = 0; i < FALL_TICKS; i++) {
            player.travel(Vec3.ZERO);
            ticks.add(new Vec3(player.getY(), player.getDeltaMovement().y, 0));
        }
        return ticks;
    }

    /**
     * One tick in the air after one skating tick on blue ice, with the takeover applied, in the
     * given boots.
     */
    private static Vec3 airTravel(GameTestHelper context, Item boots, Consumer<Player> takeover) {
        Player player = playerInAir(context, boots);
        takeover.accept(player);
        player.travel(Vec3.ZERO);
        return player.getDeltaMovement();
    }

    /** A player in the given boots who skated one tick on blue ice and is now above it. */
    private static Player playerInAir(GameTestHelper context, Item boots) {
        Player player = context.makeMockPlayer(GameType.SURVIVAL);
        player.setItemSlot(EquipmentSlot.FEET, new ItemStack(boots));
        resetPlayer(context, player, Blocks.BLUE_ICE);
        player.setDeltaMovement(new Vec3(0, 0, TAKE_OFF_SPEED));
        player.travel(Vec3.ZERO);

        player.setPos(context.absoluteVec(Vec3.atBottomCenterOf(AIR_CELL)));
        player.setOnGround(false);
        player.setDeltaMovement(new Vec3(0, 0, TAKE_OFF_SPEED));
        return player;
    }

    private record Skate(Item item, Block surface) {
        String name() {
            return item == ModItems.iceSkates() ? "Ice skates" : "Roller skates";
        }
    }

    private record Tick(
            boolean onGroundBefore,
            boolean onGroundAfter,
            double before,
            double speed,
            double vertical) {}

    private static List<Tick> jump(
            GameTestHelper context, ServerPlayer player, Skate skate, Vec3 input, int maxTicks) {
        resetPlayer(context, player, skate.surface());
        player.setDeltaMovement(new Vec3(0, 0, TAKE_OFF_SPEED));
        player.jumpFromGround();
        List<Tick> ticks = new ArrayList<>();
        for (int i = 0; i < maxTicks; i++) {
            boolean onGround = player.onGround();
            double before = player.getDeltaMovement().horizontalDistance();
            player.travel(input);
            Vec3 after = player.getDeltaMovement();
            ticks.add(
                    new Tick(
                            onGround,
                            player.onGround(),
                            before,
                            after.horizontalDistance(),
                            after.y));
            if (player.onGround() && i > 0) break;
        }
        return ticks;
    }

    private static List<Tick> airborneTicks(List<Tick> ticks) {
        return ticks.stream().filter(tick -> !tick.onGroundBefore()).toList();
    }

    private static void logJump(Skate skate, String label, List<Tick> ticks) {
        for (int i = 0; i < ticks.size(); i++) {
            Tick tick = ticks.get(i);
            LOGGER.info(
                    "IceSkatesAir {} {} tick {}: onGround={} speed {} -> {} ({}x) vertical {}",
                    skate.name(),
                    label,
                    i,
                    tick.onGroundBefore(),
                    tick.before(),
                    tick.speed(),
                    tick.before() == 0 ? 0 : tick.speed() / tick.before(),
                    tick.vertical());
        }
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
        ServerPlayer player = (ServerPlayer) context.makeMockServerPlayer(GameType.CREATIVE);
        context.runBeforeTestEnd(player::discard);
        for (Item skates : List.of(ModItems.iceSkates(), ModItems.rollerSkates())) {
            ItemStack undyed = new ItemStack(skates);
            CraftingInput input =
                    CraftingInput.of(2, 1, List.of(undyed, new ItemStack(Items.DYE.red())));
            ItemStack dyed =
                    context.getLevel()
                            .getServer()
                            .getRecipeManager()
                            .getRecipeFor(RecipeType.CRAFTING, input, context.getLevel())
                            .orElseThrow()
                            .value()
                            .assemble(input);
            context.assertTrue(
                    dyed.is(skates) && dyed.has(DataComponents.DYED_COLOR),
                    "Dye recipe must color the skates");
            context.assertFalse(
                    undyed.has(DataComponents.DYED_COLOR),
                    "Dye crafting must not mutate its input");
            player.setItemInHand(InteractionHand.MAIN_HAND, dyed);
            BlockPos cauldron = new BlockPos(1, 1, 1);
            context.setBlock(cauldron, Blocks.WATER_CAULDRON);
            context.useBlock(cauldron, player);
            context.assertFalse(
                    player.getMainHandItem().has(DataComponents.DYED_COLOR),
                    "Water cauldron must wash skate dye");
        }
        context.succeed();
    }

    public static void combatAdvancementsSeparateMeleeAndArrows(GameTestHelper context) {
        var target = context.spawn(EntityTypes.PIG, new BlockPos(1, 2, 1));
        var arrow = context.spawn(EntityTypes.ARROW, new BlockPos(1, 3, 1));
        context.runBeforeTestEnd(target::discard);
        context.runBeforeTestEnd(arrow::discard);
        List<String> paths =
                List.of(
                        "skate_and_run",
                        "skate_and_run_2",
                        "skate_by_shooting",
                        "skate_by_shooting_2");
        for (Item boots :
                List.of(ModItems.iceSkates(), ModItems.rollerSkates(), Items.IRON_BOOTS)) {
            for (boolean projectile : List.of(false, true)) {
                ServerPlayer player = newPlayer(context, boots);
                for (String path : paths) {
                    var advancement =
                            context.getLevel()
                                    .getServer()
                                    .getAdvancements()
                                    .get(IceSkatesCommon.id(path));
                    if (advancement == null)
                        throw new AssertionError("Missing advancement " + path);
                    for (String criterion : advancement.value().criteria().keySet()) {
                        player.getAdvancements().revoke(advancement, criterion);
                    }
                }
                var source =
                        projectile
                                ? player.damageSources().arrow(arrow, player)
                                : player.damageSources().playerAttack(player);
                CriteriaTriggers.PLAYER_HURT_ENTITY.trigger(player, target, source, 4, 4, false);
                CriteriaTriggers.PLAYER_KILLED_ENTITY.trigger(player, target, source);
                boolean wearingSkates = boots != Items.IRON_BOOTS;
                for (String path : paths) {
                    var advancement =
                            context.getLevel()
                                    .getServer()
                                    .getAdvancements()
                                    .get(IceSkatesCommon.id(path));
                    boolean expected =
                            wearingSkates && projectile == path.startsWith("skate_by_shooting");
                    context.assertTrue(
                            player.getAdvancements().getOrStartProgress(advancement).isDone()
                                    == expected,
                            path
                                    + " awarded for wrong attack or footwear: projectile="
                                    + projectile
                                    + ", boots="
                                    + boots);
                }
            }
        }
        context.succeed();
    }

    private static ServerPlayer newPlayer(GameTestHelper context, Item boots) {
        ServerPlayer player = (ServerPlayer) context.makeMockServerPlayer(GameType.SURVIVAL);
        context.runBeforeTestEnd(player::discard);
        player.setItemSlot(EquipmentSlot.FEET, new ItemStack(boots));
        player.setOnGround(true);
        return player;
    }

    private static void verifyNormalExhaustion(
            GameTestHelper context, ServerPlayer player, String state) {
        context.assertFalse(PlayerUtils.canSkate(player), state + " must disengage skating");
        player.getFoodData().setFoodLevel(20);
        player.getFoodData().setSaturation(0);
        player.causeFoodExhaustion(5);
        player.getFoodData().tick(player);
        context.assertTrue(
                player.getFoodData().getFoodLevel() == 19,
                state + " must retain normal exhaustion");
    }

    private static void resetPlayer(GameTestHelper context, Player player, Block surface) {
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
        if (!stack.is(ItemTags.FOOT_ARMOR)
                || !stack.is(ItemTags.TRIMMABLE_ARMOR)
                || !stack.is(ItemTags.CAULDRON_CAN_REMOVE_DYE)) {
            throw new AssertionError(item + " is missing required armor tags");
        }
        boolean loaded =
                context.getLevel().getServer().getRecipeManager().getRecipes().stream()
                        .anyMatch(
                                recipe ->
                                        recipe.id().identifier().equals(IceSkatesCommon.id(path)));
        if (!loaded) throw new AssertionError("Missing dye recipe " + path);
    }

    private static void verifyHungerBehavior(ServerPlayer player) {
        FoodData foodData = player.getFoodData();
        foodData.setFoodLevel(20);
        foodData.setSaturation(0.0F);
        player.causeFoodExhaustion(5.0F);
        foodData.tick(player);
        if (foodData.getFoodLevel() != 20)
            throw new AssertionError("Skating added food exhaustion");
        foodData.addExhaustion(5.0F);
        foodData.tick(player);
        if (foodData.getFoodLevel() != 19)
            throw new AssertionError(
                    "Skates stopped existing food exhaustion from being processed");
        foodData.setFoodLevel(20);
        foodData.setSaturation(5.0F);
    }

    private static void verifySkatingMovement(ServerPlayer player) {
        double startingZ = player.getZ();
        player.setDeltaMovement(Vec3.ZERO);
        player.travel(new Vec3(0.0D, 0.0D, 1.0D));
        if (player.getZ() <= startingZ)
            throw new AssertionError("Skates did not move the player forward");
        player.setDeltaMovement(Vec3.ZERO);
    }
}
