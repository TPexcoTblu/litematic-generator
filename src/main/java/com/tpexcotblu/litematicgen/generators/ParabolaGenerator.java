package com.tpexcotblu.litematicgen.generators;

import com.tpexcotblu.litematicgen.schematic.SchematicVolume;

public class ParabolaGenerator implements SchemeGenerator{
    private final int width;
    private final int height;

    public ParabolaGenerator(int width, int height) {
        if (width < 1 || width > 256 || height < 1 || height > 256) throw new IllegalArgumentException("Invalid generator dimensions or parameters");
        this.width = width;
        this.height = height;
    }

    @Override
    public SchematicVolume generate() {
        SchematicVolume blocks = new SchematicVolume(width + 1, height + 1, 1);

        double a = (double) height / Math.pow(width / 2.0, 2);

        int lastX = 0;
        int lastY = 0;

        for (int xi = 1; xi <= width; xi++) {
            double centered = xi - width / 2.0;
            double fx = a * centered * centered;
            int yi = (int) Math.round(height - fx);

            // Брешенхем 2D
            int x0 = lastX;
            int y0 = lastY;
            int x1 = xi;
            int y1 = yi;

            int dx = Math.abs(x1 - x0);
            int dy = Math.abs(y1 - y0);
            int sx = x0 < x1 ? 1 : -1;
            int sy = y0 < y1 ? 1 : -1;
            int err = dx - dy;

            while (true) {
                blocks.setBlock(x0, y0, 0);

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
    public String getFilename() {
        return "parabola_w" + width + "_h" + height + ".litematic";
    }

    @Override
    public String getSuccessMessage() {
        return "The parabolic arch is preserved";
    }
}
