# IceSkates

IceSkates is a Fabric mod for Minecraft 26.2 that adds craftable ice skates, roller skates, and ice-themed swords. Install it on both the client and server.

## Requirements

- Minecraft 26.2
- Fabric Loader 0.19.3 or newer
- Fabric API
- Java 25 or newer

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

## Development

Run the automated build and server integration checks:

```shell
./gradlew clean build
```

Run the headless client GameTest that verifies gameplay contracts and records item-rendering stages:

```shell
./gradlew runClientGameTest
./gradlew recordClientGameTest
```

Release automation is documented in [docs/RELEASE.md](docs/RELEASE.md). Optional Modrinth publishing is documented in [docs/MODRINTH.md](docs/MODRINTH.md).
