package com.tpexcotblu.litematicgen.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import java.nio.file.Path;

/** Optional integration; schematic export never depends on Litematica being installed. */
final class LitematicaBridge {
    private LitematicaBridge() { }

    private static final boolean AVAILABLE = detect();

    static boolean available() { return AVAILABLE; }

    private static boolean detect() {
        try {
            Class.forName("fi.dy.masa.litematica.data.SchematicHolder", false, LitematicaBridge.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException | LinkageError missing) {
            return false;
        }
    }

    static void load(Path file) throws ReflectiveOperationException {
        var player = Minecraft.getInstance().player;
        if (player == null) throw new IllegalStateException("Enter a world before loading a schematic");
        Class<?> schematicType = Class.forName("fi.dy.masa.litematica.schematic.LitematicaSchematic");
        Class<?> holderType = Class.forName("fi.dy.masa.litematica.data.SchematicHolder");
        Object holder = holderType.getMethod("getInstance").invoke(null);
        Object schematic = holderType.getMethod("getOrLoad", Path.class).invoke(holder, file);
        if (schematic == null) throw new IllegalStateException("Litematica could not read the saved file");
        Object manager = Class.forName("fi.dy.masa.litematica.data.DataManager")
                .getMethod("getSchematicPlacementManager").invoke(null);
        Class<?> managerType = Class.forName("fi.dy.masa.litematica.schematic.placement.SchematicPlacementManager");
        managerType.getMethod("removeAllPlacementsOfSchematic", schematicType).invoke(manager, schematic);
        Class<?> placementType = Class.forName("fi.dy.masa.litematica.schematic.placement.SchematicPlacement");
        String name = file.getFileName().toString().replaceFirst("\\.litematic$", "");
        Object placement = placementType.getMethod("createFor", schematicType, BlockPos.class, String.class,
                boolean.class, boolean.class).invoke(null, schematic, player.blockPosition(), name, true, true);
        managerType.getMethod("addSchematicPlacement", placementType, boolean.class).invoke(manager, placement, false);
        managerType.getMethod("setSelectedSchematicPlacement", placementType).invoke(manager, placement);
    }
}
