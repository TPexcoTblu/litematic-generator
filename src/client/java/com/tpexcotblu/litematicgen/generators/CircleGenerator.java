package com.tpexcotblu.litematicgen.generators;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtInt;
import net.minecraft.nbt.NbtList;

public class CircleGenerator implements SchemeGenerator {
    private final int radius;
    private final String orientation;

    public CircleGenerator(int radius, String orientation) {
        this.radius = radius;
        if (!orientation.equalsIgnoreCase("vertical") && !orientation.equalsIgnoreCase("horizontal")) {
            throw new IllegalArgumentException("Invalid orientation: " + orientation);
        }
        this.orientation = orientation.toLowerCase();
    }

    @Override
    public NbtList generateBlocks() {
        int size = radius * 2 + 1;
        NbtList blocks = new NbtList();

        if (orientation.equals("horizontal")) {
            int height = 1;
            for (int y = 0; y < height; y++) {
                for (int z = 0; z < size; z++) {
                    for (int x = 0; x < size; x++) {
                        int dx = x - radius;
                        int dz = z - radius;
                        double dist = Math.sqrt(dx * dx + dz * dz);
                        int state = (Math.abs(dist - radius) < 0.5) ? 1 : 0;
                        if (state == 0) continue;

                        NbtCompound block = new NbtCompound();
                        NbtList posList = new NbtList();
                        posList.add(NbtInt.of(x)); // X
                        posList.add(NbtInt.of(y)); // Y
                        posList.add(NbtInt.of(z)); // Z
                        block.put("pos", posList);
                        block.putInt("state", state);
                        blocks.add(block);
                    }
                }
            }
        } else {
            int width = 1;
            for (int x = 0; x < width; x++) {
                for (int z = 0; z < size; z++) {
                    for (int y = 0; y < size; y++) {
                        int dy = y - radius;
                        int dz = z - radius;
                        double dist = Math.sqrt(dy * dy + dz * dz);
                        int state = (Math.abs(dist - radius) < 0.5) ? 1 : 0;
                        if (state == 0) continue;

                        NbtCompound block = new NbtCompound();
                        NbtList posList = new NbtList();
                        posList.add(NbtInt.of(x)); // X
                        posList.add(NbtInt.of(y)); // Y
                        posList.add(NbtInt.of(z)); // Z
                        block.put("pos", posList);
                        block.putInt("state", state);
                        blocks.add(block);
                    }
                }
            }
        }

        return blocks;
    }

    @Override
    public NbtList generateSize() {
        int size = radius * 2 + 1;
        NbtList sizeList = new NbtList();

        if (orientation.equals("horizontal")) {
            sizeList.add(NbtInt.of(size)); // X
            sizeList.add(NbtInt.of(1));// Y
            sizeList.add(NbtInt.of(size)); // Z
        } else {
            sizeList.add(NbtInt.of(1));// X
            sizeList.add(NbtInt.of(size)); // Y
            sizeList.add(NbtInt.of(size)); // Z
        }

        return sizeList;
    }

    @Override
    public String getFilename() {
        return "circle_" + radius + "_" + orientation + ".nbt";
    }

    @Override
    public String getSuccessMessage() {
        return "Scheme " + orientation + " rings saved";
    }
}