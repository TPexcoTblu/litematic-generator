package com.tpexcotblu.litematicgen.generators;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtInt;
import net.minecraft.nbt.NbtList;

public class ParabolaGenerator implements SchemeGenerator{
    private final int width;
    private final int height;

    public ParabolaGenerator(int width, int height) {
        this.width = width;
        this.height = height;
    }

    @Override
    public NbtList generateBlocks() {
        NbtList blocks = new NbtList();

        double a = (double) height / Math.pow(width / 2.0, 2);

        int lastX = -width / 2;
        int lastY = (int) Math.round(height - a * lastX * lastX);

        for (int xi = -width / 2 + 1; xi <= width / 2; xi++) {
            double fx = a * xi * xi;
            int yi = (int) Math.round(height - fx);

            // Брешенхем 2D
            int x0 = lastX + width / 2;
            int y0 = lastY;
            int x1 = xi + width / 2;
            int y1 = yi;

            int dx = Math.abs(x1 - x0);
            int dy = Math.abs(y1 - y0);
            int sx = x0 < x1 ? 1 : -1;
            int sy = y0 < y1 ? 1 : -1;
            int err = dx - dy;

            while (true) {
                NbtCompound block = new NbtCompound();
                NbtList pos = new NbtList();
                pos.add(NbtInt.of(x0));
                pos.add(NbtInt.of(y0));
                pos.add(NbtInt.of(0));
                block.put("pos", pos);
                block.putInt("state", 1);
                blocks.add(block);

                if (x0 == x1 && y0 == y1) break;

                int e2 = 2 * err;
                if (e2 > -dy) {
                    err -= dy;
                    x0 += sx;
                }
                if (e2 < dx) {
                    err += dx;
                    y0 += sy;
                }
            }

            lastX = xi;
            lastY = yi;
        }

        return blocks;
    }

    @Override
    public NbtList generateSize() {
        NbtList sizeList = new NbtList();
        sizeList.add(NbtInt.of(width + 1));  // X
        sizeList.add(NbtInt.of(height + 1)); // Y
        sizeList.add(NbtInt.of(1));          // Z
        return sizeList;
    }

    @Override
    public String getFilename() {
        return "parabola_w" + width + "_h" + height + ".nbt";
    }

    @Override
    public String getSuccessMessage() {
        return "The parabolic arch is preserved";
    }
}
