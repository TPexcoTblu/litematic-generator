package com.tpexcotblu.litematicgen.generators;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtInt;
import net.minecraft.nbt.NbtList;

public class SphereGenerator implements SchemeGenerator {
    private final int radius;

    public SphereGenerator(int radius) {
        this.radius = radius;
    }

    @Override
    public NbtList generateBlocks() {
        int size = radius * 2 + 1;
        NbtList blocks = new NbtList();

        for (int y = 0; y < size; y++) {
            for (int z = 0; z < size; z++) {
                for (int x = 0; x < size; x++) {
                    int dx = x - radius;
                    int dy = y - radius;
                    int dz = z - radius;
                    double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);

                    if (Math.abs(dist - radius) < 0.5) {
                        NbtCompound block = new NbtCompound();
                        NbtList posList = new NbtList();
                        posList.add(NbtInt.of(x));
                        posList.add(NbtInt.of(y));
                        posList.add(NbtInt.of(z));
                        block.put("pos", posList);
                        block.putInt("state", 1);
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
        sizeList.add(NbtInt.of(size));
        sizeList.add(NbtInt.of(size));
        sizeList.add(NbtInt.of(size));
        return sizeList;
    }

    @Override
    public String getFilename() {
        return "sphere_" + radius + ".nbt";
    }

    @Override
    public String getSuccessMessage() {
        return "The sphere is saved";
    }
}