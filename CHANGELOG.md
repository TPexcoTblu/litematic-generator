# Changelog

## 2.0.0-dev — unreleased

- Export native Litematic v7 with GZIP, packed two-bit block states and current game data version.
- Share the Java 21 geometry/export core across Fabric, NeoForge and Quilt.
- Open the vanilla GUI with a configurable in-world hotkey (default K); generation runs on a bounded background worker.
- Add Mod Menu and NeoForge Mods configuration screens linked to native key-binding settings.
- Avoid duplicate background blur by using Minecraft's screen background lifecycle.
- Keep Litematica optional and provide an optional placement button.
- Add English/Russian UI text with a fallback that does not require Fabric API.
- Pin stable Minecraft targets from 1.21.8 through 26.3, with Java 21/25 and GUI compatibility adapters.
- Add newest-first matrix builds, version/loader selection, isolated outputs, logs and artifact validation.
- Replace hard-coded local installation with an explicit copy task.
- Fix unary signs in formulas, reuse primitive evaluation stacks and eliminate per-block NBT allocation.
- Fix odd-width arches, empty-text export and safe, bounded filenames.
- Add independent NBT/geometry tests and a CI matrix.
- Fix Quilt development launches by supplying Quilt's development-mode and split-classpath properties.

Legacy Fabric command experiments are retained in the source tree, excluded from the new packages.
Compile coverage and runtime verification are tracked separately; see [TESTING.md](TESTING.md) and the local `builds/report.json` for the current build run.
