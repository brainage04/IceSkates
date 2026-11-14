package io.github.brainage04.ice_skates;

import io.github.brainage04.ice_skates.item.ModItems;
import io.github.brainage04.minecraftmodconventions.ClientGameTestRecorder;
import io.github.brainage04.minecraftmodconventions.ClientGameTestServers;
import io.github.brainage04.minecraftmodconventions.ClientGameTestStages;
import io.github.brainage04.minecraftmodconventions.ClientGameTestWaits;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import net.fabricmc.fabric.api.client.gametest.v1.TestInput;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerContext;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.item.equipment.trim.ArmorTrim;
import net.minecraft.world.item.equipment.trim.TrimMaterials;
import net.minecraft.world.item.equipment.trim.TrimPatterns;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Films the skates for the showcase. For each skate type (ice skates, then roller skates) the
 * player wears the skates, holds them and stands in a ring of eight dropped ones, and is shown in
 * first person, third person from behind and third person from the front: still for a second, one
 * arm swing, still for a second. Then the player skates straight north along the runway (32 blocks
 * of ice, then 32 of stone) until about two seconds after reaching the stone, first on ice skates
 * and then on roller skates. Nothing of the other skate type is in the world, in the inventory or
 * on the player meanwhile.
 */
@SuppressWarnings("UnstableApiUsage")
public final class IceSkatesShowcase {
    private static final int FLOOR_Y = 63;

    private static final int PAD_MIN_X = 26;

    private static final int PAD_MAX_X = 34;
    private static final int PAD_MIN_Z = -4;
    private static final int PAD_MAX_Z = 4;
    private static final Vec3 PAD_CENTRE = new Vec3(30.5D, 64.0D, 0.5D);
    private static final double RING_RADIUS = 2.0D;
    private static final int RING_SIZE = 8;

    private static final int RUNWAY_MIN_X = -7;

    private static final int RUNWAY_MAX_X = 8;
    private static final int RUNWAY_START_Z = -1;
    public static final int RUNWAY_ICE_END_Z = -32;
    public static final int RUNWAY_END_Z = -64;

    public static final Vec3 RUN_START = new Vec3(0.5D, 64.0D, -0.35D);

    /** How long each runway run goes on once the skater is on the stone, as the film shows it. */
    private static final long ON_STONE_MILLIS = 2000L;

    private static final float NORTH = 180.0F;
    private static final float RUN_PITCH = 18.0F;

    private static final float SHOWCASE_PITCH = 35.0F;

    private static final double CAMERA_DISTANCE = 5.0D;

    private static final float BODY_OFFSET = 30.0F;

    private static final long STILL_MILLIS = 1000L;

    /**
     * The cut keeps this much picture before a stage starts and after it ends (trim_stages.py), so
     * the still phases inside the stage are that much shorter.
     */
    private static final long CUT_LEAD_MILLIS = 100L;

    private static final long CUT_TRAIL_MILLIS = 120L;

    /** Ticks the skater stands before setting off, and rests still after a run off camera. */
    private static final int REST_TICKS = 20;

    private static final int SWING_TICKS = SwingAnimation.DEFAULT.duration();

    private static final int RENDER_DISTANCE = 12;

    private static final int SETTLE_TICKS = 15;

    /**
     * Ticks a changed scene gets to arrive on the client before the film shows it; longer than the
     * cut padding.
     */
    private static final int SETTLE_AFTER_CHANGE_TICKS = 10;

    /**
     * Ticks the scene stays as it was after a stage ends, so the cut's trailing frames show that
     * stage.
     */
    private static final int HOLD_AFTER_END_TICKS = 6;

    /**
     * Ticks the new caption is on screen before a stage starts; longer than the cut's leading
     * padding, so the first frame of every stage already shows its caption.
     */
    private static final int CAPTION_LEAD_TICKS = 6;

    private static final int LOAD_TIMEOUT_TICKS = 2400;
    private static final int RUN_TIMEOUT_TICKS = 900;

    private enum Skates {
        ICE("ice_skates", "Ice skates"),
        ROLLER("roller_skates", "Roller skates");

        private final String id;
        private final String title;

        Skates(String id, String title) {
            this.id = id;
            this.title = title;
        }

        private Item item() {
            return this == ICE ? ModItems.iceSkates() : ModItems.rollerSkates();
        }
    }

    /** Where a runway run came to rest, read on the server once the skater stood still. */
    public record RunwayEnd(String title, Vec3 position, boolean onStone) {}

    private IceSkatesShowcase() {}

    public static Properties serverProperties() {
        Properties serverProperties = ClientGameTestServers.flatServerProperties();
        serverProperties.setProperty("view-distance", Integer.toString(RENDER_DISTANCE));
        return serverProperties;
    }

    /** Builds the pad and the runway and stops the clock at noon, before the client joins. */
    public static void buildStage(TestDedicatedServerContext server) {
        server.runOnServer(
                minecraftServer -> {
                    buildGround(minecraftServer.overworld());
                    CommandSourceStack source =
                            minecraftServer
                                    .createCommandSourceStack()
                                    .withPermission(PermissionSet.ALL_PERMISSIONS)
                                    .withSuppressedOutput();
                    minecraftServer
                            .getCommands()
                            .performPrefixedCommand(source, "gamerule advance_time false");
                    minecraftServer.getCommands().performPrefixedCommand(source, "time set noon");
                });
    }

    /**
     * Puts the player on the pad, wearing and holding ice skates with a copper coast trim, in the
     * ring of dropped ones, and waits until the client has all of it and the chunks around it.
     */
    public static void setUp(ClientGameTestContext context, TestDedicatedServerContext server) {
        server.runOnServer(
                minecraftServer -> {
                    ServerPlayer player = minecraftServer.getPlayerList().getPlayers().getFirst();
                    player.getAttribute(Attributes.CAMERA_DISTANCE).setBaseValue(CAMERA_DISTANCE);
                    useSkates(player, Skates.ICE);
                    standOnPad(player);
                });
        context.runOnClient(
                client -> {
                    client.options.renderDistance().set(RENDER_DISTANCE);
                    // The server only sends chunks out to the render distance the client last told
                    // it.
                    client.options.broadcastOptions();
                    // A section that has just been built would fade in over a second; the film
                    // never shows that.
                    client.options.chunkSectionFadeInTime().set(0.0D);
                    // Without occlusion culling every section in range is drawn, so none blinks
                    // out while the camera moves.
                    client.smartCull = false;
                    client.player
                            .getAttribute(Attributes.CAMERA_DISTANCE)
                            .setBaseValue(CAMERA_DISTANCE);
                });
        waitForChunkWindow(context);
        waitForSkatesOnClient(context, Skates.ICE);
        context.waitTicks(20);
    }

    /**
     * Films every view of both skate types and then both runway runs, and returns where each run
     * came to rest.
     */
    public static List<RunwayEnd> film(
            ClientGameTestContext context, TestDedicatedServerContext server) {
        assertRing(server, Skates.ICE);
        ClientGameTestWaits.waitForRenderSettled(context, SETTLE_TICKS, LOAD_TIMEOUT_TICKS);
        prepareView(context, Skates.ICE, CameraType.FIRST_PERSON);
        context.waitTicks(CAPTION_LEAD_TICKS);

        for (Skates skates : Skates.values()) {
            if (skates != Skates.ICE) {
                server.runOnServer(
                        minecraftServer ->
                                useSkates(
                                        minecraftServer.getPlayerList().getPlayers().getFirst(),
                                        skates));
                prepareView(context, skates, CameraType.FIRST_PERSON);
                waitForSkatesOnClient(context, skates);
                context.waitTicks(SETTLE_AFTER_CHANGE_TICKS);
            }
            for (CameraType view : CameraType.values()) {
                view(context, skates, view);
            }
            assertRing(server, skates);
        }

        List<RunwayEnd> runs = new ArrayList<>();
        for (Skates skates : Skates.values()) {
            runs.add(run(context, server, skates));
        }

        context.runOnClient(client -> client.options.setCameraType(CameraType.FIRST_PERSON));
        return runs;
    }

    private static String viewId(Skates skates, CameraType view) {
        return skates.id
                + switch (view) {
                    case FIRST_PERSON -> ".first_person";
                    case THIRD_PERSON_BACK -> ".third_person_back";
                    case THIRD_PERSON_FRONT -> ".third_person_front";
                };
    }

    private static void prepareView(ClientGameTestContext context, Skates skates, CameraType view) {
        String id = viewId(skates, view);
        String title =
                skates.title
                        + switch (view) {
                            case FIRST_PERSON -> ", first person";
                            case THIRD_PERSON_BACK -> ", third person back";
                            case THIRD_PERSON_FRONT -> ", third person front";
                        };
        String subtitle =
                switch (view) {
                    case FIRST_PERSON -> "Worn, held by the cuffs and dropped";
                    case THIRD_PERSON_BACK -> "Camera behind the player (F5 once)";
                    case THIRD_PERSON_FRONT -> "Camera in front of the player (F5 twice)";
                };
        context.runOnClient(
                client -> {
                    client.options.setCameraType(view);
                    client.player.setYRot(NORTH);
                    // The front camera looks back at the player, so it looks down only when the
                    // player looks up.
                    client.player.setXRot(
                            view == CameraType.THIRD_PERSON_FRONT
                                    ? -SHOWCASE_PITCH
                                    : SHOWCASE_PITCH);
                    turnBody(client, view);
                });
        ClientGameTestRecorder.showStep(context, id, title, subtitle);
    }

    /**
     * Views are cut, not switched on camera: the caption and the camera of the next view change
     * between two stages, which the delivered video leaves out, because the caption can reach the
     * screen a frame after the new camera does.
     */
    private static void view(ClientGameTestContext context, Skates skates, CameraType view) {
        String id = viewId(skates, view);
        if (view != CameraType.FIRST_PERSON) {
            prepareView(context, skates, view);
            for (int tick = 0; tick < CAPTION_LEAD_TICKS; tick++) {
                context.runOnClient(client -> turnBody(client, view));
                context.waitTick();
            }
        }
        ClientGameTestStages.markStage(id);
        // The still phases are timed by the clock, as the film is, not by ticks, which run
        // slower than twenty a second under load.
        holdStill(context, view, STILL_MILLIS - CUT_LEAD_MILLIS);
        context.runOnClient(
                client ->
                        client.player.swing(
                                InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT, true));
        for (int tick = 0; tick < SWING_TICKS; tick++) {
            context.runOnClient(client -> turnBody(client, view));
            context.waitTick();
        }
        holdStill(context, view, STILL_MILLIS - CUT_TRAIL_MILLIS);
        ClientGameTestStages.endStage(context, id, HOLD_AFTER_END_TICKS);
    }

    private static void holdStill(ClientGameTestContext context, CameraType view, long millis) {
        long end = System.currentTimeMillis() + millis;
        while (System.currentTimeMillis() < end) {
            context.runOnClient(client -> turnBody(client, view));
            context.waitTick();
        }
    }

    /**
     * Skates straight north along the runway from the first ice block, filmed from behind, and ends
     * the stage about two seconds after the skater reaches the stone; the caption names the surface
     * under the skater. Forward stays held until the stage ends, and the skater comes to rest off
     * camera.
     */
    private static RunwayEnd run(
            ClientGameTestContext context, TestDedicatedServerContext server, Skates skates) {
        String id = skates.id + ".runway";
        String title = skates.title + " on the runway";
        String iceSubtitle =
                skates == Skates.ICE
                        ? "32 blocks of ice: long, fast glide"
                        : "32 blocks of ice: wheels roll at their own pace";
        String stoneSubtitle =
                skates == Skates.ICE
                        ? "32 blocks of stone: blades grip, slow going"
                        : "32 blocks of stone: wheels keep gliding";
        server.runOnServer(
                minecraftServer -> {
                    ServerPlayer player = minecraftServer.getPlayerList().getPlayers().getFirst();
                    useSkates(player, skates);
                    player.teleportTo(RUN_START.x, RUN_START.y, RUN_START.z);
                    player.setDeltaMovement(Vec3.ZERO);
                    player.setYRot(NORTH);
                    player.setXRot(RUN_PITCH);
                });
        context.runOnClient(
                client -> {
                    client.options.setCameraType(CameraType.THIRD_PERSON_BACK);
                    client.player.setYRot(NORTH);
                    client.player.setXRot(RUN_PITCH);
                });
        ClientGameTestRecorder.showStep(context, id, title, iceSubtitle);
        context.waitFor(
                client ->
                        client.player.position().distanceTo(RUN_START) < 0.01D
                                && client.player.getItemBySlot(EquipmentSlot.FEET).is(skates.item())
                                && client.player.getMainHandItem().is(skates.item()),
                LOAD_TIMEOUT_TICKS);
        waitForChunkWindow(context);
        ClientGameTestWaits.waitForRenderSettled(context, SETTLE_TICKS, LOAD_TIMEOUT_TICKS);
        context.waitTicks(CAPTION_LEAD_TICKS);
        ClientGameTestStages.markStage(id);
        context.waitTicks(REST_TICKS / 2);

        TestInput input = context.getInput();
        input.holdKey(options -> options.keyUp);
        boolean onStone = false;
        for (int tick = 0; tick < RUN_TIMEOUT_TICKS && !onStone; tick++) {
            if (context.computeOnClient(client -> client.player.getZ()) < RUNWAY_ICE_END_Z) {
                onStone = true;
                ClientGameTestRecorder.showStep(context, id, title, stoneSubtitle);
            } else {
                context.waitTick();
            }
        }
        if (!onStone) {
            input.releaseKey(options -> options.keyUp);
            throw new AssertionError(title + " never reached the stone");
        }
        ClientGameTestWaits.holdFor(context, ON_STONE_MILLIS - CUT_TRAIL_MILLIS);
        ClientGameTestStages.endStage(id);
        // Forward is the only key held; letting go as the stage ends, off camera, keeps the coast
        // short, so the skater stops well before the far edge.
        input.releaseKey(options -> options.keyUp);
        context.waitTicks(HOLD_AFTER_END_TICKS);
        int still = 0;
        for (int tick = 0; tick < RUN_TIMEOUT_TICKS && still < REST_TICKS; tick++) {
            Vec3 motion = context.computeOnClient(client -> client.player.getDeltaMovement());
            still = Math.abs(motion.z) < 0.001D ? still + 1 : 0;
            context.waitTick();
        }

        return server.computeOnServer(
                minecraftServer -> {
                    ServerPlayer player = minecraftServer.getPlayerList().getPlayers().getFirst();
                    boolean endsOnStone =
                            player.level()
                                    .getBlockState(
                                            BlockPos.containing(
                                                    player.getX(),
                                                    player.getY() - 0.1D,
                                                    player.getZ()))
                                    .is(Blocks.STONE);
                    return new RunwayEnd(title, player.position(), endsOnStone);
                });
    }

    private static List<Vec3> ringPositions() {
        List<Vec3> positions = new ArrayList<>();
        for (int index = 0; index < RING_SIZE; index++) {
            double angle = index * 2.0D * Math.PI / RING_SIZE;
            positions.add(
                    new Vec3(
                            PAD_CENTRE.x + RING_RADIUS * Math.sin(angle),
                            PAD_CENTRE.y,
                            PAD_CENTRE.z + RING_RADIUS * Math.cos(angle)));
        }
        return positions;
    }

    private static AABB padBox() {
        return new AABB(PAD_MIN_X, FLOOR_Y, PAD_MIN_Z, PAD_MAX_X + 1, FLOOR_Y + 4, PAD_MAX_Z + 1);
    }

    private static ItemStack stackOf(ServerLevel level, Skates skates) {
        ItemStack stack = new ItemStack(skates.item());
        if (skates == Skates.ICE) {
            stack.set(
                    DataComponents.TRIM,
                    new ArmorTrim(
                            level.registryAccess()
                                    .lookupOrThrow(Registries.TRIM_MATERIAL)
                                    .getOrThrow(TrimMaterials.COPPER),
                            level.registryAccess()
                                    .lookupOrThrow(Registries.TRIM_PATTERN)
                                    .getOrThrow(TrimPatterns.COAST)));
        }
        return stack;
    }

    private static void useSkates(ServerPlayer player, Skates skates) {
        ServerLevel level = player.level();
        ItemStack stack = stackOf(level, skates);
        player.getInventory().clearContent();
        player.getInventory().setItem(0, stack.copy());
        player.getInventory().setSelectedSlot(0);
        player.setItemSlot(EquipmentSlot.FEET, stack.copy());

        List<ItemEntity> dropped = level.getEntitiesOfClass(ItemEntity.class, padBox());
        if (dropped.isEmpty()) {
            for (Vec3 position : ringPositions()) {
                ItemEntity entity =
                        new ItemEntity(level, position.x, position.y, position.z, stack.copy());
                // A fresh item entity gets a random push.
                entity.setDeltaMovement(Vec3.ZERO);
                entity.setNeverPickUp();
                entity.setUnlimitedLifetime();
                level.addFreshEntity(entity);
            }
        } else {
            dropped.forEach(entity -> entity.setItem(stack.copy()));
        }
    }

    private static void standOnPad(ServerPlayer player) {
        player.teleportTo(PAD_CENTRE.x, PAD_CENTRE.y, PAD_CENTRE.z);
        player.setDeltaMovement(Vec3.ZERO);
        player.setYRot(NORTH);
        player.setXRot(SHOWCASE_PITCH);
        player.setOnGround(true);
    }

    /**
     * The shot shows eight dropped skates of the current type in a ring; fails when it would not.
     */
    private static void assertRing(TestDedicatedServerContext server, Skates skates) {
        List<ItemEntity> dropped =
                server.computeOnServer(
                        minecraftServer ->
                                minecraftServer
                                        .overworld()
                                        .getEntitiesOfClass(ItemEntity.class, padBox()));
        if (dropped.size() != RING_SIZE) {
            throw new AssertionError(
                    "Expected " + RING_SIZE + " dropped skates, found " + dropped.size());
        }
        for (Vec3 expected : ringPositions()) {
            boolean found =
                    dropped.stream()
                            .anyMatch(
                                    entity ->
                                            entity.getItem().is(skates.item())
                                                    && Math.abs(entity.getX() - expected.x) < 0.05D
                                                    && Math.abs(entity.getZ() - expected.z) < 0.05D
                                                    && Math.abs(entity.getY() - expected.y)
                                                            < 0.05D);
            if (!found) {
                throw new AssertionError(
                        "No dropped " + skates.id + " at " + expected + " in the ring");
            }
        }
    }

    private static void waitForSkatesOnClient(ClientGameTestContext context, Skates skates) {
        context.waitFor(
                client -> {
                    Player player = client.player;
                    if (!player.getItemBySlot(EquipmentSlot.FEET).is(skates.item())
                            || !player.getMainHandItem().is(skates.item())) {
                        return false;
                    }
                    List<ItemEntity> dropped =
                            client.level.getEntitiesOfClass(ItemEntity.class, padBox());
                    return dropped.size() == RING_SIZE
                            && dropped.stream()
                                    .allMatch(entity -> entity.getItem().is(skates.item()));
                },
                LOAD_TIMEOUT_TICKS);
    }

    private static void waitForChunkWindow(ClientGameTestContext context) {
        // The server sends chunks in a circle, and the ring at its edge may still be on its way.
        ClientGameTestWaits.waitForChunkWindow(context, RENDER_DISTANCE - 2, LOAD_TIMEOUT_TICKS);
    }

    private static void turnBody(Minecraft client, CameraType view) {
        client.player.setYRot(NORTH);
        client.player.setYBodyRot(
                view == CameraType.THIRD_PERSON_BACK ? NORTH + BODY_OFFSET : NORTH);
        client.player.yBodyRotO = client.player.yBodyRot;
        client.player.setYHeadRot(NORTH);
    }

    private static void buildGround(ServerLevel level) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int z = RUNWAY_END_Z; z <= RUNWAY_START_Z; z++) {
            for (int x = RUNWAY_MIN_X; x <= RUNWAY_MAX_X; x++) {
                platformColumn(
                        level, cursor, x, z, z >= RUNWAY_ICE_END_Z ? Blocks.ICE : Blocks.STONE);
            }
        }
        for (int z = PAD_MIN_Z; z <= PAD_MAX_Z; z++) {
            for (int x = PAD_MIN_X; x <= PAD_MAX_X; x++) {
                platformColumn(level, cursor, x, z, Blocks.STONE);
            }
        }
    }

    private static void platformColumn(
            ServerLevel level, BlockPos.MutableBlockPos cursor, int x, int z, Block top) {
        level.setBlock(cursor.set(x, FLOOR_Y, z), top.defaultBlockState(), Block.UPDATE_CLIENTS);
        for (int y = FLOOR_Y - 4; y < FLOOR_Y; y++) {
            level.setBlock(
                    cursor.set(x, y, z), Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
        for (int y = FLOOR_Y + 1; y <= FLOOR_Y + 3; y++) {
            level.setBlock(
                    cursor.set(x, y, z), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
    }
}
