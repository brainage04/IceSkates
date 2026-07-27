# Create a new release

1. Update `mod_version` in `gradle.properties`.
2. Commit and push that change.
3. Create a matching annotated tag in the form `vX.Y.Z`; its annotation becomes the release notes.
4. Push the tag.

For a short release note:

```shell
git tag -a v1.0.1 -m "Summarise the release here"
git push origin v1.0.1
```

For longer notes, put them in a file and use `git tag -a v1.0.1 -F RELEASE_NOTES.md`.

The release workflow validates that the tag and `mod_version` match, builds both loader JARs, and attaches the Fabric and NeoForge artifacts to the GitHub Release. If the tag has no annotation text, GitHub-generated notes are used as a fallback.

If `MODRINTH_TOKEN` is configured, the workflow publishes the Fabric release JAR to Modrinth. If both `CURSEFORGE_TOKEN` and `CURSEFORGE_PROJECT_ID` are configured, it also publishes that loader variant to CurseForge. Missing third-party credentials skip only that destination; the GitHub Release still contains both loader artifacts.
