package com.tpexcotblu.litematicgen.schematic;

/** A loader-independent, two-state region. X changes fastest, then Z, then Y. */
public final class SchematicVolume {
    public static final int MAX_VOLUME = 32 * 1024 * 1024;
    private final int width, height, depth;
    private final long[] states;
    private int blockCount;

    public SchematicVolume(int width, int height, int depth) {
        long plane = (long) width * height;
        if (width <= 0 || height <= 0 || depth <= 0 || plane > MAX_VOLUME || plane * depth > MAX_VOLUME) {
            throw new IllegalArgumentException("Schematic dimensions must be positive and volume <= " + MAX_VOLUME);
        }
        long volume = plane * depth;
        this.width = width;
        this.height = height;
        this.depth = depth;
        // Litematica requires at least two bits per palette entry, even for air + stone.
        states = new long[(int) ((volume * 2 + 63) / 64)];
    }

    public void setBlock(int x, int y, int z) {
        if (x < 0 || x >= width || y < 0 || y >= height || z < 0 || z >= depth) {
            throw new IllegalArgumentException("Block outside schematic: " + x + ", " + y + ", " + z);
        }
        int index = (y * depth + z) * width + x;
        int word = index >>> 5;
        long mask = 1L << ((index & 31) * 2);
        if ((states[word] & mask) == 0) {
            states[word] |= mask;
            blockCount++;
        }
    }

    public int width() { return width; }
    public int height() { return height; }
    public int depth() { return depth; }
    public int volume() { return width * height * depth; }
    public int blockCount() { return blockCount; }
    long[] states() { return states; }
}
