# Test review

## Frozen rubric

- Keep: exercises production behavior or loaded game data, or supplies a distinct loader/runtime boundary or visual scenario.
- Redundant: repeats the same production behavior and failure condition in the same relevant runtime; name the retained coverage before removing it.
- Tautological: checks a copied algorithm, a mock echo, a constant, or wiring without an observable behavior.
- A loader adapter is not a separate behavioral test and stays when needed to run retained coverage on that loader.
- A recording scene is visual evidence, not an automated rendering assertion.

## Existing coverage classification

- Keep `IceSkatesGameTests.skatesAndSwordsAreRegisteredWithDyeRecipes`: loaded armor tags and dye recipes, actual player movement, and hunger processing.
- Keep Fabric `IceSkatesGameTest` and NeoForge `IceSkatesNeoForgeGameTest`: adapters for the shared test on both loaders.
- Keep `IceSkatesClientGameTest.runTest`: dedicated-server client lifecycle and three visual scenes (`ice_skates.held`, `roller_skates.held`, `skates.dropped`).
- Redundant client `verifyItemData`: the retained shared test checks the same loaded tags and dye recipe IDs on both server loaders.
- Redundant client `verifyHungerBehavior`: the retained shared test calls the same server-side exhaustion and food-tick paths with the same expectations.
- Redundant client `verifySkatingMovement`: the retained shared test calls the same server-player travel path and checks forward displacement.

The three redundant client helpers and their calls were removed. No complete GameTest or visual scene was removed. There were no unit tests and no tautological tests selected for removal.

## Coverage gaps and additions

The old suite did not cover roller-skate movement, steering, diagonal speed, surface friction differences, exhaustion outside skating, actual dye crafting or washing, or client model resolution.

- Added shared `skatingControlsAndFriction`: both skates, forward/backward steering, yaw, diagonal normalization, and ice-versus-roller friction across stone, ice and blue ice.
- Added shared `exhaustionFollowsSkatingState`: both skates preserve hunger processing; airborne, powder snow, water and ordinary boots retain normal exhaustion.
- Added shared `skatesCanBeDyedAndWashed`: actual recipe matching and assembly, unchanged crafting input, and water-cauldron interaction for both pairs.
- Added client model checks for both pairs, dyed and undyed, in GUI, ground, first-person and worn contexts; missing geometry or the missing-texture sprite fails the test.

All shared tests run through both loader adapters and use local test structures and disposable players. There are no commands, gamerules, configuration files, background threads or shutdown resources in this mod.

Remaining manual coverage: skating sounds and particles, visual trim-pattern placement, leg animation, view bobbing, sword combat and crafting advancements. The conventions do not supply a NeoForge client GameTest task, so NeoForge rendering is not automatically exercised; its gameplay tests use the same shared production code and assets as Fabric.

## Code review

Fixed three combat advancement predicates, with shared `combatAdvancementsSeparateMeleeAndArrows` coverage on both loaders:

- `skate_and_run` accepted arrow hits because it had no damage-source restriction.
- `skate_and_run_2` accepted arrow kills for the same reason.
- `skate_by_shooting_2` put a hurt-event `damage` predicate on a kill event, which reads `killing_blow` instead; melee kills could therefore award the ranged-kill advancement.

The regression dispatches real loaded advancement triggers with player-melee and arrow damage sources, both skate types, and ordinary boots. It checks all four hit/kill advancements, including rejected combinations.

Judgement calls left unchanged: combat advancements interpret "whilst skating" as wearing skates, without checking movement or ground state; item-acquisition advancements say "Craft" but use inventory triggers, so loot or gifts also qualify; advancement titles and descriptions are English literals. Movement deliberately uses fixed acceleration by input direction, so sneak/analogue-input scaling and movement-speed effects are not represented in skating acceleration. Changing those would alter existing gameplay rather than correct a documented contract.
