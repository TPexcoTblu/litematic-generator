# Build the shape. Save the schematic.

**Litematic Generator** turns shapes, text and mathematical formulas into Minecraft schematics. The **2.0 update** introduces native **.litematic** export and a simple in-game interface for **Fabric, NeoForge and Quilt**.

> **Release transition:** This guide describes version 2.0. New builds are being prepared for publication. Older downloads use the previous command-based workflow and export `.nbt` files. Check the version number and changelog before downloading.

---

## What can you create?

| Generator | Make it your own |
| :--- | :--- |
| **Sphere** | Choose a radius for a hollow sphere |
| **Circle** | Generate a horizontal or vertical outline |
| **Parabolic arch** | Set the width and height independently |
| **Text** | Turn a message into block lettering with an adjustable font size |
| **Formula** | Create an implicit 3D surface or fill its interior |

Generated shapes use **smooth stone**. Files are saved locally, ready for your schematic workflow.

## From idea to schematic

1. Install the JAR for your **exact Minecraft version and mod loader** in your instance's **mods** folder. Replace the previous Litematic Generator JAR.
2. Enter a world and press **K**.
3. Choose a generator and enter your dimensions, text or formula.
4. Select **Generate .litematic**. Generation runs in the background.
5. Find the result in your instance's **schematics** folder. Use **Copy file path** to locate it, or **Load schematic** when a compatible Litematica is installed.

**Change the shortcut:** Open Minecraft's **Controls → Key Binds → Miscellaneous → Litematic Generator**. You can also reach this page through **Mod Menu → Litematic Generator → Configure** on Fabric/Quilt, or **Mods → Litematic Generator → Config** on NeoForge.

The generator opens inside a world. There are no generator buttons in the title or pause menus.

## Create with formulas

Use coordinates **x**, **y** and **z**, centered on the generated region. For a sphere with radius 16, try:

~~~text
sqrt(x*x+y*y+z*z)-16
~~~

- **Surface mode** selects finite values close to zero: absolute value ≤ 0.45.
- **Filled mode** selects finite values ≤ 0.
- Supported operators: **+ − * / ^**, parentheses, constants **pi** and **e**.
- Supported functions: **sin, cos, tan, abs, sqrt, log, exp**.

Choose a region radius large enough to contain your shape. Scaling an expression changes the thickness selected in surface mode.

## Native .litematic export

Version 2.0 writes compressed **Litematic v7** files directly, with a compact block palette and packed block states. Each file includes the Minecraft data version of the game that generated it.

**Litematica is optional for generating and saving files.** To view a schematic in the world, install a compatible schematic viewer separately. The **Load schematic** integration requires a compatible Litematica installation; availability depends on your Minecraft version and loader. Loading a schematic does not automatically build it in the world.

## Compatibility & requirements

| | Version 2.0 |
| :--- | :--- |
| **Environment** | Client-side; no server installation needed |
| **Loaders** | Separate Fabric, NeoForge and Quilt builds |
| **Minecraft build matrix** | 1.21.8, 1.21.9, 1.21.10, 1.21.11, 26.1, 26.1.1, 26.1.2, 26.2, 26.3 |
| **Required library mods** | Fabric API is not required |
| **Optional integrations** | Mod Menu for settings; compatible Litematica for viewing/loading |
| **Interface languages** | English and Russian |

Download availability is shown in the **Versions** tab. Match both the loader and exact game version; snapshots are outside this update's build matrix.

### Using an older release?

The previous releases export **.nbt** files and use client commands:

~~~text
/generatecircle <radius> <orientation>
/generatesphere <radius>
/generateparabola <width> <height>
/generatetext <fontsize> <text>
~~~

The instructions above for **K**, the GUI and **.litematic** apply to **2.0**.

---

Found a bug or have a shape in mind? [Open an issue](https://github.com/TPexcoTblu/litematic-generator/issues) with your Minecraft version, loader and steps to reproduce it.

[Source code](https://github.com/TPexcoTblu/litematic-generator) · **MIT license**
