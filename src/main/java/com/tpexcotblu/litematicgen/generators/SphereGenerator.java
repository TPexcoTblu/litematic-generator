package com.tpexcotblu.litematicgen.generators;

import com.tpexcotblu.litematicgen.schematic.SchematicVolume;

public class SphereGenerator implements SchemeGenerator {
    private final int radius;

    public SphereGenerator(int radius) {
        if (radius < 1 || radius > 128) throw new IllegalArgumentException("Invalid generator dimensions or parameters");
        this.radius = radius;
    }

    @Override
    public SchematicVolume generate() {
        int size = radius * 2 + 1;
        SchematicVolume blocks = new SchematicVolume(size, size, size);

        for (int y = 0; y < size; y++) {
            for (int z = 0; z < size; z++) {
                for (int x = 0; x < size; x++) {
                    int dx = x - radius;
                    int dy = y - radius;
                    int dz = z - radius;
                    double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);

                    if (Math.abs(dist - radius) < 0.5) {
                        blocks.setBlock(x, y, z);
                    }
                }
            }
        }

        return blocks;
    }

    @Override
    public String getFilename() {
        return "sphere_" + radius + ".litematic";
    }

    @Override
    public String getSuccessMessage() {
        return "The sphere is saved";
    }
}