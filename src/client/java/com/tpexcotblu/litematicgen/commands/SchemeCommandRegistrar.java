package com.tpexcotblu.litematicgen.commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.tpexcotblu.litematicgen.generators.SchemeGenerator;
import com.tpexcotblu.litematicgen.generators.SphereGenerator;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtList;
import net.minecraft.text.Text;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public class SchemeCommandRegistrar {
    private static NbtList createDefaultPalette() {
        NbtList palette = new NbtList();

        var airEntry = new NbtCompound();
        airEntry.putString("Name", "minecraft:air");
        palette.add(airEntry);

        var stoneEntry = new NbtCompound();
        stoneEntry.putString("Name", "minecraft:smooth_stone");
        palette.add(stoneEntry);

        return palette;
    }

    private static int saveScheme(FabricClientCommandSource source, SchemeGenerator generator) {
        NbtCompound root = new NbtCompound();
        root.put("blocks", generator.generateBlocks());
        root.put("palette", createDefaultPalette());
        root.put("size", generator.generateSize());
        root.put("entities", new NbtList());
        root.putString("author", "TPexcoTblu");
        root.putInt("DataVersion", 3463);

        File dir = new File("schematics");
        if (!dir.exists()) dir.mkdirs();

        File outFile = new File(dir, generator.getFilename());

        try (FileOutputStream fos = new FileOutputStream(outFile)) {
            NbtIo.writeCompressed(root, fos);
            source.sendFeedback(Text.of(generator.getSuccessMessage() + ": " + outFile.getAbsolutePath()));
        } catch (IOException e) {
            source.sendError(Text.of("Error while saving: " + e.getMessage()));
            return 0;
        }

        return 1;
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

    public static void registerAllCommands() {
        registerCircleCommand();
        registerSphereCommand();
    }
}