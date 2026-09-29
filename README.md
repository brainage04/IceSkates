# IceSkates

IceSkates is a Fabric and NeoForge mod for Minecraft 26.2 that adds craftable ice skates, roller skates, and ice-themed swords. Install the matching loader variant on both the client and server.

## Requirements

- Minecraft 26.2
- Java 25 or newer
- Either Fabric Loader 0.19.3 or newer with Fabric API, or NeoForge 26.2.0.88 or newer

## Migrating from the Fabric-only release

Install exactly one IceSkates JAR matching the loader used by the client and server: the Fabric JAR requires Fabric Loader and Fabric API, while the NeoForge JAR requires NeoForge and no Fabric API. Remove the old IceSkates JAR before switching loaders; never place both variants in one `mods` directory.

The mod ID remains `ice_skates`, so existing IceSkates item, recipe, advancement, and asset identifiers remain stable for worlds using the same Minecraft version. Install the same loader-specific IceSkates variant on every client and server that uses the mod.

For local builds, `./gradlew build` emits the Fabric and NeoForge production JARs in `build/libs`.

## Skating

Equip either pair of skates in the feet slot and move along the ground.

- Ice skates preserve the friction of the block below the player, making ice, packed ice, and blue ice especially slippery. Skating over ice also emits snowflake particles.
- Roller skates use consistent low friction on solid ground and emit dark dust particles.
- Both pairs stop new food exhaustion while actively skating without suspending normal hunger, healing, or starvation processing.
- Skating replaces normal footsteps with periodic skating sounds, reduces the walk animation, and suppresses grounded first-person view bobbing.
- Skating physics disengage in liquids, powder snow, and while airborne.

## Items and customization

- Ice skates and roller skates provide iron-equivalent boot protection and can be repaired with iron.
- Both pairs can be dyed in a crafting table, washed clean in a water cauldron, enchanted as boots, and customized with vanilla armor trims.
- Ice skate blades and roller skate wheels are crafting components.
- Ice, packed-ice, and blue-ice swords provide progressively greater durability and attack damage.

Recipes and advancements are available through the vanilla recipe book and advancement screen.

Both skate items use three-dimensional paired boot models with open cuffs, raised straps, and buckles. Ice skates have thin supported runners; roller skates have four stepped wheels and a toe stop per boot. Equipped skates follow each leg independently. The undyed upper is pale slate, with cyan ice-skate straps or amber roller-skate straps; dye changes the upper without recoloring the hardware. Armor-trim materials highlight the cuff rims, and equipped skates retain the selected smithing-template pattern on the upper panels.

## Development

Run the automated build and server integration checks for both loaders. Production JARs are collected in `build/libs`.

```shell
./gradlew build
```

Run the Fabric headless client GameTest that verifies gameplay contracts and records item-rendering stages:

```shell
./gradlew :fabric:runClientGameTest
./gradlew :fabric:recordClientGameTest
```

Run the NeoForge GameTest server:

```shell
./gradlew runNeoForgeGameTests
```

Release automation is documented in [docs/RELEASE.md](docs/RELEASE.md). Optional Modrinth publishing is documented in [docs/MODRINTH.md](docs/MODRINTH.md).
