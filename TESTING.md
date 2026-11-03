# Validation — 2026-11-03

Final build runner exit status: **0**. All **27/27** requested stable targets passed compilation and production JAR validation.

| Minecraft | Fabric | NeoForge | Quilt |
| --- | --- | --- | --- |
| 1.21.8 | passed | passed | passed |
| 1.21.9 | passed | passed | passed |
| 1.21.10 | passed | passed | passed |
| 1.21.11 | passed | passed | passed |
| 26.1 | passed | passed | passed |
| 26.1.1 | passed | passed | passed |
| 26.1.2 | passed | passed | passed |
| 26.2 | passed | passed | passed |
| 26.3 | passed | passed | passed |

Build command: `python build_versions.py --all`. The full report, per-target SHA-256 hashes and artifact paths are in `builds/report.json`; logs are in `builds/logs/`. Copied production JARs were independently hashed and matched the report.

Package checks cover Minecraft metadata, client side, Java 21/25 class versions, the Java 21 shared core, bundled font/translations, required GUI/mixin classes, mixin compatibility level and legacy Fabric/Quilt intermediary screen selectors.

Unit command: `python -m unittest discover -s tests -v` — **11 tests passed**. This includes independent GZIP/NBT decoding, geometry, formula modes and unary signs, packed word boundaries, metadata, empty/Unicode text filenames, and build selection.

Interactive verification completed on **Fabric 26.3** in an isolated development instance with Mod Menu, Litematica and MaLiLib. All seven GUI exports (sphere, both circle orientations, arch, text, formula surface and filled formula) were independently decoded; block counts and Minecraft data version matched. Loading the sphere through Litematica displayed the schematic in the world. Mod Menu configuration, rebinding and persistence across a full restart were verified. The generator opens only inside a world; the default binding is K.

Additional interactive smoke checks completed on **NeoForge 26.3.0.101 / Minecraft 26.3** and **Quilt Loader 0.30.2 / Minecraft 26.3**, without optional mods. Both loaded the isolated test world, opened the generator using K and exported a radius-16 sphere. Independent GZIP/NBT decoding verified 3,338 occupied blocks, matching metadata and MinecraftDataVersion 5023. NeoForge's native Mods → Config integration also opened the settings page. Both clients were closed normally after testing.

The first Quilt development launch did not discover the split source outputs. Fixed `platforms/fabric/shared.gradle` to pass Quilt's `loader.development` and `loader.classPathGroups` properties, then repeated the launch successfully. This changes the development launcher only, not the packaged mod. Screenshots: `builds/screenshots/neoforge-26.3-export.png` and `builds/screenshots/quilt-26.3-export.png`; logs: `builds/runtime-neoforge-26.3.log` and `builds/runtime-quilt-26.3-fixed.log`.

These checks used Gradle development clients; production JAR installation and runtime coverage on other Minecraft versions remain **incomplete**. Compilation/package checks do not certify runtime behavior on every target. Current artifacts are labeled **2.0.0-dev**; a stable release still requires a release-version rebuild.

On 2026-11-03 all 27 copied JARs were checked again against the SHA-256 values in the report and matched. Runtime screenshot: `builds/screenshots/fabric-26.3-litematica.png`.

After the Quilt development-launch fix, rebuilt all **9/9 Quilt targets** with `python build_versions.py --all --loaders quilt` (exit 0). The focused run is preserved in `builds/quilt-rebuild-report.json`. `builds/report.json` aggregates these results with the existing Fabric/NeoForge builds, records per-target build timestamps, and all **27** production JARs passed package validation and SHA-256 comparison again. No runtime mod sources changed during this rebuild.

See [the format comparison](docs/format-benchmark.md) for the measured file-size reductions. No speed benchmark was claimed.
