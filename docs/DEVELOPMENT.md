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

Run the NeoForge GameTests on a NeoForge server against the release JAR:

```shell
heavy --label IceSkates-neoforge -- ./gradlew --no-daemon :neoforge:runProductionServerGameTest
```
