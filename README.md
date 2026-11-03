# Litematic Generator

[![Modrinth Downloads](https://img.shields.io/modrinth/dt/litematic-generator?style=flat&logo=modrinth&label=Modrinth%20downloads)](https://modrinth.com/mod/litematic-generator)
[![Build](https://github.com/TPexcoTblu/litematic-generator/actions/workflows/build.yml/badge.svg)](https://github.com/TPexcoTblu/litematic-generator/actions/workflows/build.yml)
[Modrinth](https://modrinth.com/mod/litematic-generator) · [Changelog](CHANGELOG.md) · [Test results](TESTING.md) · [Report a bug](https://github.com/TPexcoTblu/litematic-generator/issues)

Client-side shape, text and formula generator for **Fabric, NeoForge and Quilt**.
Generates native, GZIP-compressed `.litematic` v7 files with an air/smooth-stone palette,
packed block states and the current Minecraft data version. Litematica is optional for
exporting and is needed separately to display or place a generated schematic.

> **2.0 is in development.** This branch builds `2.0.0-dev`; older published releases use commands and export `.nbt`. All 27 build targets have passed local compilation and package checks. Development-client checks on Minecraft 26.3 passed for all three loaders; other game versions still need runtime verification. See [TESTING.md](TESTING.md) for the exact scope.

## Use

Install one JAR matching your loader and exact Minecraft version in the instance's
`mods` folder, replacing the previous Litematic Generator JAR. Fabric API is not
required.

Press **K** while inside a world to open the generator. There are no generator buttons
in the title or pause menus. To change the binding, open **Mod Menu → Litematic Generator →
Configure** (Fabric/Quilt), or **Mods → Litematic Generator → Config** (NeoForge).
The configuration screen links to Minecraft's native key bindings under
**Miscellaneous → Litematic Generator**. Bindings persist in `options.txt` and are also
available directly through Minecraft's Controls settings. Mod Menu is optional.

Cycle the shape, enter dimensions, and select **Generate .litematic**. Generation runs
on a background thread. Files go to the current instance's `schematics` directory;
**Copy file path** copies the result's absolute path. **Load schematic** loads it at
your position when you are in a world and a compatible Litematica is installed.

Shapes: sphere, horizontal/vertical circle, parabolic arch, text, formula surface or
filled formula. Radius limits: 1–128 for circles/spheres; 2–96 for formulas. Arch
width/height: 1–256. Text: up to 1024 characters, font size 4–64.

Formula variables are `x`, `y`, `z` centered on the region, with `pi`, `e`, operators
`+ - * / ^`, parentheses, and `sin cos tan abs sqrt log exp`. For a sphere of radius
16 use `sqrt(x*x+y*y+z*z)-16`. Filled mode selects finite values ≤ 0; surface mode selects
finite values with absolute value ≤ 0.45. Different formulas may need scaling to
produce a continuous surface.

The common GUI does not depend on client command APIs or Fabric API. Quilt uses
Quilt Loader's Fabric-mod compatibility to load the same shared implementation;
its artifact is built and labeled separately; the development run uses Quilt Loader
and its pinned library set. Legacy command experiments remain
in `src/client` for reference and are excluded from the new builds.

## Build

A **JDK 25** is required to run the build. The core and Minecraft 1.21.x artifacts
target Java 21; Minecraft 26.x artifacts target Java 25. Python 3.10+ runs the build
planner. It detects common local JDK 25 installations; otherwise set `JAVA_HOME`
or pass `--java-home`.

```sh
python build_versions.py --list
python build_versions.py --all
python build_versions.py --latest
python build_versions.py 1.21.8 --loaders fabric
python build_versions.py 1.21.11 26.3 --loaders fabric neoforge quilt
python build_versions.py --all --dry-run
```

The checked-in matrix contains only stable Minecraft releases from **1.21.8 to
26.3**. 26.4 is currently a snapshot and is deliberately excluded. Some historical
Minecraft releases require a beta NeoForge loader because no stable loader was
published for that exact game version; `--list` shows the exact dependency.
`versions.json` pins dependencies and platform compatibility settings.

Builds run newest first, preserve tracked configuration, isolate outputs by
Minecraft version and loader, continue across failures, and exit nonzero if any
target fails. JARs are copied to `builds/<minecraft>/<loader>/`, per-target logs to
`builds/logs/`, and the requested run's results to `builds/report.json`.

Direct Gradle examples (quote `-P` arguments in PowerShell):

```sh
./gradlew build "-Ploader=fabric" "-Pminecraft_version=26.3"
./gradlew :platform:runClient "-Ploader=fabric" "-Pminecraft_version=26.3"
./gradlew copyJarAfterBuild "-PmodsDir=/path/to/instance/mods"
```

Close the development client before switching build targets or running the matrix: Gradle can remove a previous target's intermediate class files. Packaged JARs remain in `builds/`.

A normal build never installs into a game instance. Use an instance matching the
artifact's exact Minecraft version.

## Validation

```sh
python -m unittest discover -s tests -v
```

Tests compile the Java core independently, decode generated GZIP/NBT with a separate
Python reader, check geometry, axis order, packed word boundaries, metadata and the
stable build planner. They do not replace a launch test on each loader. See the
build report for actual compile results; a configured target is not by itself a
claim of tested runtime compatibility.

GitHub Actions runs the core/planner tests and the 27-target matrix on pushes and
pull requests. Each build job uploads its JARs, logs and report as a workflow
artifact. Local output in `builds/` and development worlds in `run/` are ignored
by Git; they are not published automatically.

## Contributing

Include your exact Minecraft version, loader/version, mod version and reproduction
steps when reporting a bug. For generation problems, include the shape settings or
formula. Attach only the relevant log excerpt and remove any private information.
For code changes, run the tests above and build the affected version/loader targets.

## Layout

- `src/main/java/.../generators` and `schematic`: loader-independent Java core.
- `src/sharedClient`: vanilla GUI, hotkeys and Options/Minecraft mixins shared across loaders.
- `src/fabricClient`: optional Mod Menu integration.
- `src/neoforge`: NeoForge entry point and native configuration-screen integration.
- `platforms`: Fabric/remapping, unobfuscated Fabric, NeoForge and shared build logic.
- `versions.json`: stable Minecraft release matrix and pinned loader versions.

The `.litematic` writer uses a packed array rather than allocating an NBT compound
for every block. Both the old `.nbt` exporter and the new writer use GZIP; file size
and speed gains depend on geometry. A [size comparison](docs/format-benchmark.md)
on three hollow spheres found 76.7–80.7% smaller compressed files.

[Modrinth](https://modrinth.com/mod/litematic-generator) · [Source](https://github.com/TPexcoTblu/litematic-generator)
