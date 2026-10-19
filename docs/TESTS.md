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
