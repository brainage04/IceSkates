# Development

Production JARs are collected in `build/libs`.

Check `heavy status` before running Gradle. Run the build and all development and production GameTests for both loaders:

```shell
heavy --label IceSkates-tests -- ./gradlew --no-daemon build runAllGameTests
```

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
