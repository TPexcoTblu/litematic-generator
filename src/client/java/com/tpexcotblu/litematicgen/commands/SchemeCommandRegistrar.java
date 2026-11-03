package com.tpexcotblu.litematicgen.commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.tpexcotblu.litematicgen.generators.ParabolaGenerator;
import com.tpexcotblu.litematicgen.generators.SchemeGenerator;
import com.tpexcotblu.litematicgen.generators.SphereGenerator;
import com.tpexcotblu.litematicgen.generators.TextGenerator;
import com.tpexcotblu.litematicgen.generators.FormulaGenerator;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import com.tpexcotblu.litematicgen.schematic.LitematicWriter;
import net.minecraft.SharedConstants;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public class SchemeCommandRegistrar {
    private static final String LITEMATICA_SCHEMATIC_CLASS = "fi.dy.masa.litematica.schematic.LitematicaSchematic";

    private static String normalizeFormulaInput(String raw) {
        String trimmed = raw == null ? "" : raw.trim();
        if (trimmed.length() >= 2) {
            boolean hasDoubleQuotes = trimmed.startsWith("\"") && trimmed.endsWith("\"");
            boolean hasSingleQuotes = trimmed.startsWith("'") && trimmed.endsWith("'");
            if (hasDoubleQuotes || hasSingleQuotes) {
                return trimmed.substring(1, trimmed.length() - 1).trim();
            }
        }
        return trimmed;
    }

    private static File schematicDirectory() {
        return FabricLoader.getInstance().getGameDir().resolve("schematics").toFile();
    }

    private static int saveScheme(FabricClientCommandSource source, SchemeGenerator generator) {
        File outFile = new File(schematicDirectory(), generator.getFilename());
        try {
            var volume = generator.generate();
            LitematicWriter.write(outFile.toPath(), volume, placementNameFromFile(outFile),
                    source.getPlayer().getName().getString(),
                    SharedConstants.getGameVersion().dataVersion().getId());
            source.sendFeedback(Text.of(generator.getSuccessMessage() + ": " + outFile.getAbsolutePath()));
            if (FabricLoader.getInstance().isModLoaded("litematica")) {
                loadSchematicToWorld(source, outFile);
            }
            return 1;
        } catch (IOException | IllegalArgumentException e) {
            source.sendError(Text.of("Error while generating schematic: " + e.getMessage()));
            return 0;
        }
    }

    private static String placementNameFromFile(File file) {
        String name = file.getName();
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }

    private static List<String> listSchematicBaseNames() {
        List<String> names = new ArrayList<>();
        File dir = schematicDirectory();
        File[] files = dir.listFiles((d, n) -> {
            String lower = n.toLowerCase();
            return lower.endsWith(".litematic") || lower.endsWith(".nbt");
        });
        if (files == null) {
            return names;
        }

        for (File file : files) {
            names.add(placementNameFromFile(file));
        }

        return names;
    }

    private static File resolveSchematicFileByName(String name) {
        File dir = schematicDirectory();
        if (name.isBlank() || name.contains("/") || name.contains("\\") || name.contains(":") || name.equals("..")) {
            throw new IllegalArgumentException("Use a schematic filename without directory components");
        }
        String lower = name.toLowerCase(java.util.Locale.ROOT);
        if (lower.endsWith(".litematic") || lower.endsWith(".nbt")) {
            return new File(dir, name);
        }

        File litematicFile = new File(dir, name + ".litematic");
        if (litematicFile.exists()) {
            return litematicFile;
        }

        File nbtFile = new File(dir, name + ".nbt");
        if (nbtFile.exists()) {
            return nbtFile;
        }

        return litematicFile;
    }

    private static void loadSchematicToWorld(FabricClientCommandSource source, File schematicFile) {
        try {
            Path schematicPath = schematicFile.toPath().toAbsolutePath().normalize();

            Class<?> schematicClass = Class.forName(LITEMATICA_SCHEMATIC_CLASS);
            Class<?> schematicHolderClass = Class.forName("fi.dy.masa.litematica.data.SchematicHolder");
            Class<?> dataManagerClass = Class.forName("fi.dy.masa.litematica.data.DataManager");
            Class<?> placementManagerClass = Class.forName("fi.dy.masa.litematica.schematic.placement.SchematicPlacementManager");
            Class<?> placementClass = Class.forName("fi.dy.masa.litematica.schematic.placement.SchematicPlacement");

            Method holderGetInstance = schematicHolderClass.getMethod("getInstance");
            Object holder = holderGetInstance.invoke(null);

            Method getOrLoad = schematicHolderClass.getMethod("getOrLoad", Path.class);
            Object schematic = getOrLoad.invoke(holder, schematicPath);
            if (schematic == null) {
                source.sendError(Text.of("Litematica could not load schematic file: " + schematicPath));
                return;
            }

            Method getPlacementManager = dataManagerClass.getMethod("getSchematicPlacementManager");
            Object placementManager = getPlacementManager.invoke(null);

            Method removeAllPlacements = placementManagerClass.getMethod("removeAllPlacementsOfSchematic", schematicClass);
            removeAllPlacements.invoke(placementManager, schematic);

            BlockPos origin = source.getPlayer().getBlockPos();
            String placementName = placementNameFromFile(schematicFile);

            Method createFor = placementClass.getMethod(
                    "createFor",
                    schematicClass,
                    BlockPos.class,
                    String.class,
                    boolean.class,
                    boolean.class
            );
            Object placement = createFor.invoke(null, schematic, origin, placementName, true, true);

            Method addPlacement = placementManagerClass.getMethod("addSchematicPlacement", placementClass, boolean.class);
            addPlacement.invoke(placementManager, placement, false);

            Method setSelected = placementManagerClass.getMethod("setSelectedSchematicPlacement", placementClass);
            setSelected.invoke(placementManager, placement);

            source.sendFeedback(Text.of("Loaded into Litematica at " + origin.toShortString()));
        } catch (Throwable t) {
            source.sendError(Text.of("Litematica auto-load failed: " + t.getClass().getSimpleName() + ": " + t.getMessage()));
        }
    }

    private static int removeSchematic(FabricClientCommandSource source, String name) {
        File schematicFile;
        try {
            schematicFile = resolveSchematicFileByName(name);
        } catch (IllegalArgumentException e) {
            source.sendError(Text.of(e.getMessage()));
            return 0;
        }
        if (!schematicFile.exists()) {
            source.sendError(Text.of("Schematic not found: " + schematicFile.getName()));
            return 0;
        }

        try {
            Path schematicPath = schematicFile.toPath().toAbsolutePath().normalize();

            Class<?> schematicClass = Class.forName(LITEMATICA_SCHEMATIC_CLASS);
            Class<?> schematicHolderClass = Class.forName("fi.dy.masa.litematica.data.SchematicHolder");
            Class<?> dataManagerClass = Class.forName("fi.dy.masa.litematica.data.DataManager");
            Class<?> placementManagerClass = Class.forName("fi.dy.masa.litematica.schematic.placement.SchematicPlacementManager");

            Method holderGetInstance = schematicHolderClass.getMethod("getInstance");
            Object holder = holderGetInstance.invoke(null);

            Method getOrLoad = schematicHolderClass.getMethod("getOrLoad", Path.class);
            Object schematic = getOrLoad.invoke(holder, schematicPath);
            if (schematic != null) {
                Method getPlacementManager = dataManagerClass.getMethod("getSchematicPlacementManager");
                Object placementManager = getPlacementManager.invoke(null);

                Method removeAllPlacements = placementManagerClass.getMethod("removeAllPlacementsOfSchematic", schematicClass);
                removeAllPlacements.invoke(placementManager, schematic);

                Method removeSchematic = schematicHolderClass.getMethod("removeSchematic", schematicClass);
                removeSchematic.invoke(holder, schematic);
            }
        } catch (Throwable t) {
            source.sendError(Text.of("Litematica cleanup failed: " + t.getClass().getSimpleName() + ": " + t.getMessage()));
            return 0;
        }

        if (!schematicFile.delete()) {
            source.sendError(Text.of("Could not delete file: " + schematicFile.getAbsolutePath()));
            return 0;
        }

        source.sendFeedback(Text.of("Removed schematic: " + schematicFile.getName()));
        return 1;
    }

    private static void registerRemoveSchematicCommand() {
        SuggestionProvider<FabricClientCommandSource> schemSuggestions = (context, builder) -> {
            for (String name : listSchematicBaseNames()) {
                builder.suggest(name);
            }
            return builder.buildFuture();
        };

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(
                    literal("removeschem")
                            .then(argument("name", StringArgumentType.word())
                                    .suggests(schemSuggestions)
                                    .executes(ctx -> {
                                        String name = StringArgumentType.getString(ctx, "name");
                                        return removeSchematic(ctx.getSource(), name);
                                    })
                            )
            );
        });
    }

    private static void registerCircleCommand() {
        SuggestionProvider<FabricClientCommandSource> orientationSuggestions = (context, builder) -> {
            return builder.suggest("horizontal").suggest("vertical").buildFuture();
        };

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(
                    literal("generatecircle")
                            .then(argument("radius", IntegerArgumentType.integer(1, 128))
                                    .then(argument("orientation", StringArgumentType.word())
                                            .suggests(orientationSuggestions)
                                            .executes(ctx -> {
                                                int radius = IntegerArgumentType.getInteger(ctx, "radius");
                                                String orientation = StringArgumentType.getString(ctx, "orientation");
                                                var generator = new com.tpexcotblu.litematicgen.generators.CircleGenerator(radius, orientation);
                                                return saveScheme(ctx.getSource(), generator);
                                            })
                                    )
                            )
            );
        });
    }

    private static void registerSphereCommand() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(
                    literal("generatesphere")
                            .then(argument("radius", IntegerArgumentType.integer(1, 128))
                                    .executes(ctx -> {
                                        int radius = IntegerArgumentType.getInteger(ctx, "radius");
                                        SphereGenerator generator = new SphereGenerator(radius);
                                        return saveScheme(ctx.getSource(), generator);
                                    })
                            )
            );
        });
    }

    public static void registerParabolaCommand() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(
                    literal("generateparabola")
                            .then(argument("width", IntegerArgumentType.integer(1, 256))
                                    .then(argument("height", IntegerArgumentType.integer(1, 256))
                                            .executes(ctx -> {
                                                int width = IntegerArgumentType.getInteger(ctx, "width");
                                                int height = IntegerArgumentType.getInteger(ctx, "height");
                                                var generator = new ParabolaGenerator(width, height);
                                                return saveScheme(ctx.getSource(), generator);
                                            })
                                    )
                            )
            );
        });
    }

    public static void registerTextCommand() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(
                    literal("generatetext")
                            .then(argument("fontsize", IntegerArgumentType.integer(4, 64))
                                    .then(argument("text", StringArgumentType.greedyString())
                                            .executes(ctx -> {
                                                int fontSize = IntegerArgumentType.getInteger(ctx, "fontsize");
                                                String text = StringArgumentType.getString(ctx, "text");

                                                InputStream fontStream = SchemeCommandRegistrar.class.getResourceAsStream(
                                                        "/assets/litematic-generator/fonts/Minecraftia-Regular.ttf"
                                                );

                                                if (fontStream == null) {
                                                    ctx.getSource().sendError(Text.of("Could not find embedded font."));
                                                    return 0;
                                                }

                                                Font font;
                                                try (fontStream) {
                                                    font = Font.createFont(Font.TRUETYPE_FONT, fontStream)
                                                            .deriveFont(Font.PLAIN, fontSize);
                                                } catch (Exception e) {
                                                    ctx.getSource().sendError(Text.of("Error loading font: " + e.getMessage()));
                                                    return 0;
                                                }

                                                var generator = new TextGenerator(text, fontSize, font);
                                                return saveScheme(ctx.getSource(), generator);
                                            })
                                    )
                            )
            );
        });
    }

    public static void registerFormulaCommand() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(
                    literal("generateformula")
                            .then(argument("radius", IntegerArgumentType.integer(2, 96))
                                    .then(argument("formula", StringArgumentType.greedyString())
                                            .executes(ctx -> {
                                                int radius = IntegerArgumentType.getInteger(ctx, "radius");
                                                String formula = normalizeFormulaInput(StringArgumentType.getString(ctx, "formula"));
                                                try {
                                                    var generator = new FormulaGenerator(radius, formula);
                                                    return saveScheme(ctx.getSource(), generator);
                                                } catch (IllegalArgumentException e) {
                                                    ctx.getSource().sendError(Text.of("Invalid formula: " + e.getMessage()));
                                                    return 0;
                                                }
                                            })
                                    )
                            )
            );
        });
    }

    public static void registerFilledFormulaCommand() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(
                    literal("generateformulafilled")
                            .then(argument("radius", IntegerArgumentType.integer(2, 96))
                                    .then(argument("formula", StringArgumentType.greedyString())
                                            .executes(ctx -> {
                                                int radius = IntegerArgumentType.getInteger(ctx, "radius");
                                                String formula = normalizeFormulaInput(StringArgumentType.getString(ctx, "formula"));
                                                try {
                                                    var generator = new FormulaGenerator(radius, formula, true);
                                                    return saveScheme(ctx.getSource(), generator);
                                                } catch (IllegalArgumentException e) {
                                                    ctx.getSource().sendError(Text.of("Invalid formula: " + e.getMessage()));
                                                    return 0;
                                                }
                                            })
                                    )
                            )
            );
        });
    }


    public static void registerAllCommands() {
        registerCircleCommand();
        registerSphereCommand();
        registerParabolaCommand();
        registerTextCommand();
        registerFormulaCommand();
        registerFilledFormulaCommand();
        registerRemoveSchematicCommand();
    }
}