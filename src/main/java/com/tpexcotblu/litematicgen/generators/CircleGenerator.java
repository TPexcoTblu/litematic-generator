package com.tpexcotblu.litematicgen.generators;

import com.tpexcotblu.litematicgen.schematic.SchematicVolume;

public class CircleGenerator implements SchemeGenerator {
    private final int radius;
    private final String orientation;

    public CircleGenerator(int radius, String orientation) {
        if (radius < 1 || radius > 128) throw new IllegalArgumentException("Invalid generator dimensions or parameters");
        this.radius = radius;
        if (orientation == null || !orientation.equalsIgnoreCase("vertical") && !orientation.equalsIgnoreCase("horizontal")) {
            throw new IllegalArgumentException("Invalid orientation: " + orientation);
        }
        this.orientation = orientation.toLowerCase(java.util.Locale.ROOT);
    }

    @Override
    public SchematicVolume generate() {
        int size = radius * 2 + 1;
        SchematicVolume blocks = orientation.equals("horizontal") ? new SchematicVolume(size, 1, size) : new SchematicVolume(1, size, size);

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

                        blocks.setBlock(x, y, z);
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

                        blocks.setBlock(x, y, z);
                    }
                }
            }
        }

        return blocks;
    }

    @Override
    public String getFilename() {
        return "circle_" + radius + "_" + orientation + ".litematic";
    }

    @Override
    public String getSuccessMessage() {
        return "Scheme " + orientation + " rings saved";
    }
}