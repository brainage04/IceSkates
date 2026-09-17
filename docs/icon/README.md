# IceSkates icon

## What this is

`icon.png` — the IceSkates mod icon, 1024 x 1024 PNG, SHA-256
`16fb724eca739fdf34d1b5a10fd8862ff5d0f282badf9de429dba9be516b9e14`.

Copied byte-identically from `.local-icon-variants/provenance/from-round3/blender-f/ice-skates-improved-01.png`
(SHA-256 verified at the source, and again on the copy). The hash matches `png_sha256` in
`provenance/render-evidence.json`; `provenance/verification.json` records the same render together with
the source-texture hashes.

## How it was made

Blender render of a **design proposal**, not a screenshot and not the mod's current in-game geometry.

* Blender 5.1.1, headless `--background -noaudio`, Cycles CPU (no GPU), 64 samples, 1024 x 1024,
  34.003 s, no display server, no audio sink, no shader pack.
* The mod's `ice_skates` item is a flat `minecraft:item/generated` sprite; the mod has no bespoke
  volumetric skate model. `render_models.py` authors a **new volumetric voxel boot proposal** — open
  square cuffs, restrained dyed palette, two straps and buckles per boot, one thin central silver
  runner blade with two supports. This geometry exists only in this icon folder; it is not in the
  mod's code or assets. The exact per-cuboid definition is in
  `provenance/ice-skates-improved-01-metadata.json` (`geometry.parts`) and `provenance/improvements.json`.
* Camera: true isometric orthographic — azimuth 45 deg, elevation 35.26438968 deg, location
  `(86.42828369140625, -86.1301040649414, 92.94306182861328)`, `ortho_scale` 24.15215301513672.
  Candidate 1 uses the cool slate palette and the right isometric view.
* Imagery: the **real mod textures** from this repository —
  `common/src/main/resources/assets/ice_skates/textures/item/`:
  `ice_skates.png`, `ice_skates_overlay.png`, `ice_skate_blades.png`, `roller_skate_wheels.png`
  (the exact four the packed scene uses). Byte-identical snapshots are in `provenance/textures/`
  under their recorded snapshot names; the SHA-256 of each is in `provenance/provenance.json` and was
  re-verified against the repository files while these snapshots were made.
* Vanilla generated-item behaviour (front/back 7.5/8.5 quads and the alpha-clipped side quads) follows
  the Minecraft **26.2** client at `~/.gradle/caches/fabric-loom/26.2/minecraft-client.jar`
  (SHA-256 `40896ee9f1e2bec3c934daac7e93d41e9e3d9c2f8ae0ca366d52ffbfd1afa290`); the relevant member
  `assets/minecraft/models/item/generated.json` is snapshotted as `sources/minecraft-item-generated.json`.
  The jar itself is not copied and is not needed to re-render, because the textures the render reads
  are in `provenance/textures/` and the packed scene carries its own copies.

## Provenance files

* `ice-skates-improved-01.py` — entry point (`render_models.render('ice-skates-improved-01')`);
  `render_models.py` — scene author (voxel geometry, materials, camera, lights).
* `ice-skates-improved-01.blend` (packed, carries the four item textures),
  `ice-skates-improved-01-metadata.json` (geometry parts, camera, packed textures, validation).
* `provenance.json` — the mod-source snapshot manifest (source path, snapshot name, SHA-256);
  `sources/` — the item model/definition JSON snapshots; `textures/` — the item texture snapshots.
* `source-pixels.json`, `improvements.json` — measured source pixels and the documented model changes.
* `finalize_models.py` (writes `manifest.json`, `render-evidence.json`, `improvements.json`),
  `verify_models.py` (reopens scenes, re-checks texture hashes and PNGs), `inspect_assets.py`,
  `render_all.py` (recorded batch launcher), `resource-evidence.json`.
* `manifest.json`, `verification.json`, `render-evidence.json`, `visual-review.json` — curated to the
  selected candidate (`CURATION.json` records the dropped rows).
* `CURATION.json` — what was copied, what was filtered, and what was left in round-3.

## How to regenerate

From `<repo>/docs/icon/provenance`:

```sh
nix shell nixpkgs#blender --command blender --background -noaudio \
  --python-exit-code 1 --python ice-skates-improved-01.py
nix shell nixpkgs#blender --command blender --background -noaudio --python verify_models.py
```

`render_models.py` reads `provenance/textures/<snapshot name>.png` relative to its own directory, so
run it exactly from `docs/icon/provenance` (the entry point resolves its own path and will work from
anywhere, but the snapshot names must stay as recorded). Re-running overwrites
`ice-skates-improved-01.png`. `verify_models.py` re-checks the textures packed in the saved scene
against the hashes recorded in `provenance.json`, so if the mod textures ever change, re-snapshot them
under the same recorded names and update `provenance.json`.

## Notes

* Only the selected `ice-skates-improved-01` candidate is shipped. The `ice-skates-current-*`,
  `ice-skates-improved-02` and all `roller-skates-*` candidates are not copied, and the
  `magic-carpet-tiers*` candidates in the same round-3 folder belong to a different mod and were not
  copied either.
* The mod-source snapshot `sources/...roller_skates...json` entries are the mod's own item definitions
  and are kept for completeness of the texture provenance; the roller-skates *scenes and renders* are not.
* Excluded: Blender `.blend1` backups, run logs, the empty `textures/` snapshot directory that the
  original folder left behind (re-created above), and the empty blocker list.
* Nothing else in the mod repository was modified and nothing was committed.

## Working-tree note

The round-3 working tree that produced this icon was cleaned up after integration. Every file needed to regenerate the icon was copied into `provenance/`; the copies live under `provenance/from-round3/` when they came from the working tree. Any remaining `round3/...` mention records where something came from, not a path that still exists.
