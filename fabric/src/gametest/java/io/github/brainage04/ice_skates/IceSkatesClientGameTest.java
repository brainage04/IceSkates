package io.github.brainage04.ice_skates;

import io.github.brainage04.ice_skates.client.ServerSupport;
import io.github.brainage04.ice_skates.client.SkateRenderState;
import io.github.brainage04.ice_skates.item.ModItems;
import io.github.brainage04.minecraftmodconventions.ClientGameTestRecorder;
import io.github.brainage04.minecraftmodconventions.ClientGameTestRecordingHud;
import io.github.brainage04.minecraftmodconventions.ClientGameTestServers;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.TestInput;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerContext;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.inventory.Hotbar;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.resources.palette.PalettedTextureManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.item.equipment.trim.ArmorTrim;
import net.minecraft.world.item.equipment.trim.TrimMaterials;
import net.minecraft.world.item.equipment.trim.TrimPatterns;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Films the skates for the showcase. For each skate type (ice skates, then roller skates) the
 * player wears the skates, holds them and stands in a ring of eight dropped ones, and is shown in
 * first person, third person from behind and third person from the front: still for a second, one
 * arm swing, still for a second. Then the player skates the whole runway (32 blocks of ice, then 32
 * of stone) in a straight line, first on ice skates and then on roller skates. Nothing of the other
 * skate type is in the world, in the inventory or on the player meanwhile.
 */
@SuppressWarnings("UnstableApiUsage")
public final class IceSkatesClientGameTest implements FabricClientGameTest {
    private static final int FLOOR_Y = 63;

    /** The stone pad the held, worn and dropped skates are shown on, east of the runway. */
    private static final int PAD_MIN_X = 26;

    private static final int PAD_MAX_X = 34;
    private static final int PAD_MIN_Z = -4;
    private static final int PAD_MAX_Z = 4;
    private static final Vec3 PAD_CENTRE = new Vec3(30.5D, 64.0D, 0.5D);
    private static final double RING_RADIUS = 2.0D;
    private static final int RING_SIZE = 8;

    /**
     * The runway is 16 blocks wide and 64 long, running north from z = -1: 32 blocks of ice (the
     * vanilla ice block, an IceBlock, which the skates' friction and snowflake particles are made
     * for), then 32 blocks of stone.
     */
    private static final int RUNWAY_MIN_X = -7;

    private static final int RUNWAY_MAX_X = 8;
    private static final int RUNWAY_START_Z = -1;
    private static final int RUNWAY_ICE_END_Z = -32;
    private static final int RUNWAY_END_Z = -64;

    /** On the first ice block, facing north along the runway. */
    private static final Vec3 RUN_START = new Vec3(0.5D, 64.0D, -0.35D);

    /** Where the skater should come to rest: inside the last stone block, clear of the edge. */
    private static final double RUN_STOP_Z = RUNWAY_END_Z + 0.9D;

    private static final float NORTH = 180.0F;
    private static final float RUN_PITCH = 18.0F;

    /** Looking down far enough that the ring of dropped skates is in view. */
    private static final float SHOWCASE_PITCH = 35.0F;

    /**
     * The third-person camera stays this far from the player, so the whole ring around the player
     * is in the picture.
     */
    private static final double CAMERA_DISTANCE = 5.0D;

    /**
     * The body is turned this far from the camera, so the held skate is not hidden behind the
     * player.
     */
    private static final float BODY_OFFSET = 30.0F;

    /** How long each view holds still before and after the swing, as the film shows it. */
    private static final long STILL_MILLIS = 1000L;

    /**
     * The cut keeps this much picture before a stage starts and after it ends (trim_stages.py), so
     * the still phases inside the stage are that much shorter.
     */
    private static final long CUT_LEAD_MILLIS = 100L;

    private static final long CUT_TRAIL_MILLIS = 120L;

    /** Ticks the skater stands before setting off and rests after stopping on the runway. */
    private static final int REST_TICKS = 20;

    private static final int SWING_TICKS = SwingAnimation.DEFAULT.duration();

    /** Large enough that the pad and the whole runway are in view from either. */
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

        /** Ice skates on the friction of the block below; roller skates on their own 0.925. */
        private double stoneFriction() {
            return this == ICE ? 0.6D : 0.925D;
        }
    }

    @Override
    public void runTest(ClientGameTestContext context) {
        Properties serverProperties = ClientGameTestServers.flatServerProperties();
        // The pad and the whole runway stay inside the view, so no chunk arrives while filming.
        serverProperties.setProperty("view-distance", Integer.toString(RENDER_DISTANCE));

        ClientGameTestServers.withDedicatedServer(
                context,
                serverProperties,
                "IceSkates item rendering GameTest",
                server ->
                        server.runOnServer(
                                minecraftServer -> {
                                    buildGround(minecraftServer.overworld());
                                    // A bright, still noon, so the showcase is not lit by a setting
                                    // sun.
                                    CommandSourceStack source =
                                            minecraftServer
                                                    .createCommandSourceStack()
                                                    .withPermission(PermissionSet.ALL_PERMISSIONS)
                                                    .withSuppressedOutput();
                                    minecraftServer
                                            .getCommands()
                                            .performPrefixedCommand(
                                                    source, "gamerule advance_time false");
                                    minecraftServer
                                            .getCommands()
                                            .performPrefixedCommand(source, "time set noon");
                                }),
                server -> {
                    server.runOnServer(
                            minecraftServer -> {
                                ServerPlayer player =
                                        minecraftServer.getPlayerList().getPlayers().getFirst();
                                player.getAttribute(Attributes.CAMERA_DISTANCE)
                                        .setBaseValue(CAMERA_DISTANCE);
                                useSkates(player, Skates.ICE);
                                standOnPad(player);
                            });
                    ClientGameTestServers.assertClientWorldAndPlayerAvailable(context);
                    context.runOnClient(
                            client -> {
                                client.options.renderDistance().set(RENDER_DISTANCE);
                                // The server only sends chunks out to the render distance the
                                // client last told it.
                                client.options.broadcastOptions();
                                // A section that has just been built would fade in over a second;
                                // the film never shows that.
                                client.options.chunkSectionFadeInTime().set(0.0D);
                                // Without occlusion culling every section in range is drawn, so
                                // none blinks out while the camera moves.
                                client.smartCull = false;
                                client.player
                                        .getAttribute(Attributes.CAMERA_DISTANCE)
                                        .setBaseValue(CAMERA_DISTANCE);
                            });
                    waitForChunkWindow(context);
                    waitForSkatesOnClient(context, Skates.ICE);
                    context.waitTicks(20);
                    verifyItemModels(context);
                    verifyTrimTexture(context);
                    verifyCreativeOnServerWithIceSkates(context);
                    assertRing(server, Skates.ICE);

                    waitForMeshesIdle(context);
                    // Starting the recorder clears the caption, so the first scene and its
                    // caption are set after it and are final before the first stage starts.
                    ClientGameTestRecorder.startRecording(context);
                    prepareView(context, Skates.ICE, CameraType.FIRST_PERSON);
                    context.waitTicks(CAPTION_LEAD_TICKS);

                    for (Skates skates : Skates.values()) {
                        if (skates != Skates.ICE) {
                            server.runOnServer(
                                    minecraftServer ->
                                            useSkates(
                                                    minecraftServer
                                                            .getPlayerList()
                                                            .getPlayers()
                                                            .getFirst(),
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

                    for (Skates skates : Skates.values()) {
                        run(context, server, skates);
                    }

                    context.runOnClient(
                            client -> client.options.setCameraType(CameraType.FIRST_PERSON));
                });
    }

    private static String viewId(Skates skates, CameraType view) {
        return skates.id
                + switch (view) {
                    case FIRST_PERSON -> ".first_person";
                    case THIRD_PERSON_BACK -> ".third_person_back";
                    case THIRD_PERSON_FRONT -> ".third_person_front";
                };
    }

    /**
     * Sets the camera, the look and the caption of one view in a single client tick, so the picture
     * and the caption change on the same frame.
     */
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
                    ClientGameTestRecordingHud.showStep(id, title, subtitle);
                });
        System.out.println("[CLIENT_GAMETEST_RECORDER] " + id + " | " + title + " | " + subtitle);
    }

    /**
     * One view of the worn, held and dropped skates: still for a second, one normal arm swing,
     * still for a second. Views are cut, not switched on camera: the caption and the camera of the
     * next view change between two stages, which the delivered video leaves out, because the
     * caption can reach the screen a frame after the new camera does.
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
        stageStart(id);
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
        stageEnd(id);
        // The cut keeps a moment of picture after the end; the scene must not change in it.
        context.waitTicks(HOLD_AFTER_END_TICKS);
    }

    private static void holdStill(ClientGameTestContext context, CameraType view, long millis) {
        long end = System.currentTimeMillis() + millis;
        while (System.currentTimeMillis() < end) {
            context.runOnClient(client -> turnBody(client, view));
            context.waitTick();
        }
    }

    /**
     * Skates the whole runway straight north, from the first ice block to the last stone one,
     * filmed from behind. Forward is held until the skater would just come to rest inside the last
     * block; the caption names the surface under the skater.
     */
    private static void run(
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
                    ClientGameTestRecordingHud.showStep(id, title, iceSubtitle);
                });
        System.out.println(
                "[CLIENT_GAMETEST_RECORDER] " + id + " | " + title + " | " + iceSubtitle);
        context.waitFor(
                client ->
                        client.player.position().distanceTo(RUN_START) < 0.01D
                                && client.player.getItemBySlot(EquipmentSlot.FEET).is(skates.item())
                                && client.player.getMainHandItem().is(skates.item()),
                LOAD_TIMEOUT_TICKS);
        waitForChunkWindow(context);
        waitForMeshesIdle(context);
        context.waitTicks(CAPTION_LEAD_TICKS);
        stageStart(id);
        context.waitTicks(REST_TICKS / 2);

        TestInput input = context.getInput();
        input.holdKey(options -> options.keyUp);
        boolean held = true;
        boolean onStone = false;
        int still = 0;
        for (int tick = 0; tick < RUN_TIMEOUT_TICKS && still < REST_TICKS; tick++) {
            Vec3 position = context.computeOnClient(client -> client.player.position());
            Vec3 motion = context.computeOnClient(client -> client.player.getDeltaMovement());
            if (!onStone && position.z < RUNWAY_ICE_END_Z) {
                onStone = true;
                context.runOnClient(
                        client -> ClientGameTestRecordingHud.showStep(id, title, stoneSubtitle));
            }
            // Once on stone, the skater slows by its friction each tick; forward is let go when
            // the remaining glide would end just inside the last block.
            double friction = skates.stoneFriction();
            double glide = -motion.z * friction / (1.0D - friction);
            if (held && onStone && position.z - glide <= RUN_STOP_Z) {
                input.releaseKey(options -> options.keyUp);
                held = false;
            }
            still = !held && Math.abs(motion.z) < 0.001D ? still + 1 : 0;
            context.waitTick();
        }
        if (held) {
            input.releaseKey(options -> options.keyUp);
            throw new AssertionError(title + " never reached the end of the runway");
        }
        stageEnd(id);
        context.waitTicks(HOLD_AFTER_END_TICKS);

        Vec3 end =
                server.computeOnServer(
                        minecraftServer ->
                                minecraftServer.getPlayerList().getPlayers().getFirst().position());
        boolean onRunwayStone =
                server.computeOnServer(
                        minecraftServer -> {
                            ServerPlayer player =
                                    minecraftServer.getPlayerList().getPlayers().getFirst();
                            return player.level()
                                    .getBlockState(
                                            BlockPos.containing(
                                                    player.getX(),
                                                    player.getY() - 0.1D,
                                                    player.getZ()))
                                    .is(Blocks.STONE);
                        });
        System.out.println("[ICE] " + title + " ended at " + end + ", on stone: " + onRunwayStone);
        // The whole length, straight: from the first block to the last, without leaving the
        // runway or turning off its centre line.
        if (!onRunwayStone
                || end.z > RUNWAY_END_Z + 2.0D
                || end.z < RUNWAY_END_Z + 0.3D
                || Math.abs(end.x - RUN_START.x) > 0.5D) {
            throw new AssertionError(
                    title + " must skate straight to the far end of the runway: " + end);
        }
    }

    /** The ring of dropped skates: two blocks out from the pad centre, every 45 degrees. */
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
            // The ice skates carry a copper coast trim, the same on the feet, in the hand and on
            // the ground.
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

    /**
     * Makes every skate in the world of one type: the worn pair, the only item in the inventory and
     * the eight dropped ones. The dropped skates keep their places; only their item changes.
     */
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
                // A fresh item entity gets a random push; these stay exactly where they are put,
                // are never picked up and never despawn.
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

    /** The ring is complete, in place and of the given skate type, as the server sees it. */
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

    /** Waits until the client shows the given skates on the feet, in the hand and in the ring. */
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

    /**
     * Marks the start of a filmed stage; the delivered video keeps only the time between a start
     * and an end.
     */
    private static void stageStart(String id) {
        System.out.println("STAGE_START " + System.currentTimeMillis() + " " + id);
    }

    private static void stageEnd(String id) {
        System.out.println("STAGE_END " + System.currentTimeMillis() + " " + id);
    }

    /**
     * Waits until the client holds every chunk in the circle two chunks inside the render distance
     * of the player.
     */
    private static void waitForChunkWindow(ClientGameTestContext context) {
        context.waitFor(
                client -> {
                    int centreX = SectionPos.blockToSectionCoord(client.player.getX());
                    int centreZ = SectionPos.blockToSectionCoord(client.player.getZ());
                    // The server sends chunks in a circle, not a square, and the ring at the edge
                    // may still be on its way.
                    int radius = RENDER_DISTANCE - 2;
                    for (int chunkX = centreX - radius; chunkX <= centreX + radius; chunkX++) {
                        for (int chunkZ = centreZ - radius; chunkZ <= centreZ + radius; chunkZ++) {
                            int dx = chunkX - centreX;
                            int dz = chunkZ - centreZ;
                            if (dx * dx + dz * dz > radius * radius) continue;
                            // ClientLevel.hasChunk is true for every position, so ask the chunk
                            // cache.
                            if (client.level
                                            .getChunkSource()
                                            .getChunk(chunkX, chunkZ, ChunkStatus.FULL, false)
                                    == null) {
                                return false;
                            }
                        }
                    }
                    return true;
                },
                LOAD_TIMEOUT_TICKS);
    }

    /**
     * Waits until the renderer has had nothing to build for a while, so the next frame is a
     * finished one.
     */
    private static void waitForMeshesIdle(ClientGameTestContext context) {
        int idleTicks = 0;
        for (int tick = 0; tick < LOAD_TIMEOUT_TICKS; tick++) {
            boolean idle =
                    context.computeOnClient(
                            client -> client.levelRenderer.hasRenderedAllSections());
            idleTicks = idle ? idleTicks + 1 : 0;
            if (idleTicks >= SETTLE_TICKS) return;
            context.waitTick();
        }
        throw new AssertionError("The client did not finish building the chunks around the player");
    }

    /**
     * From behind, the body is turned away from the camera so the held skates are not hidden behind
     * the player; in the other views the body faces where the player looks.
     */
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

    /**
     * One column of a platform: its top block, solid stone below so it reads as a slab, and air
     * above.
     */
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

    /**
     * On a server with Ice Skates nothing is hidden: the signal says so, the tabs and the search
     * hold every item, and a saved hotbar with skates loads them. The side that hides them needs a
     * server without the mod, which the test servers cannot be; ServerSupport says how it is told
     * apart.
     */
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

    /**
     * The worn skates' trim must resolve to the real pattern texture, not the paletted texture
     * manager's fallback.
     */
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
