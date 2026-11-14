# Development

Production JARs are collected in `build/libs`.

Check `heavy status` before running Gradle. Run the build and all development and production GameTests for both loaders:

```shell
heavy --label IceSkates-tests -- ./gradlew --no-daemon build runAllGameTests
```

`:fabric:test` runs the unit tests. `SkateMotionTest` checks one tick of skating in `SkateMotion` with examples and jqwik properties: without keys a skater keeps the friction share of its speed, held keys add 0.04 blocks a tick in the direction vanilla's `Entity#getInputVector` gives (no more on a diagonal), and no keys or turns push a skater past the top speed, 0.04 / (1 - friction). Ice skates take the friction of the block below (`Block#getFriction`) as the share of speed kept each tick. Vanilla keeps that friction, scaled by the player's `friction_modifier` attribute, times an air drag of 0.91 scaled by the player's `air_drag_modifier` attribute (LivingEntity.java:2453 and 2471-2473 in 26.3). Ice skates keep the block's friction and roller skates keep 0.925 on any block, both scaled by `friction_modifier` the way vanilla scales it. Skates leave out the air drag, so when `friction_modifier` makes the surface slipperier than the skate's own friction, `SkateMotion#groundFriction` brings back the 0.91 air drag scaled by `air_drag_modifier`, which keeps the top speed bounded, and never keeps less than the skate's own friction. With both attributes at 1 the skate motion is the same as without them. `:fabric:pitest` runs mutation tests on `SkateMotion` against them; it is not part of `check`, and the report is written to `fabric/build/reports/pitest`.

Run the Fabric headless client GameTest that verifies gameplay contracts and records item-rendering stages:

```shell
heavy --label IceSkates-client -- ./gradlew --no-daemon :fabric:runProductionClientGameTest
heavy --label IceSkates-recording -- ./gradlew --no-daemon :fabric:recordClientGameTest
```

The film itself (the pad and runway, cameras, captions and stage markers) is `IceSkatesShowcase` in `fabric/src/recording`. `IceSkatesClientGameTest` keeps the checks: item models, the worn trim's texture, the creative tabs and saved hotbar, and where each runway run comes to rest.

Run the same client GameTests against a plain vanilla server without Ice Skates (MinecraftModConventions starts it). `IceSkatesVanillaServerClientGameTest` checks that no Ice Skates item is offered in creative there and that taking one does not get the player kicked, then sends the held-back packet and expects the kick, so the check would catch it. It is not part of `runAllGameTests`:

```shell
heavy --label IceSkates-vanilla -- ./gradlew --no-daemon :fabric:runVanillaServerClientGameTest
```

Run the NeoForge GameTests on a NeoForge server against the release JAR:

```shell
heavy --label IceSkates-neoforge -- ./gradlew --no-daemon :neoforge:runProductionServerGameTest
```
