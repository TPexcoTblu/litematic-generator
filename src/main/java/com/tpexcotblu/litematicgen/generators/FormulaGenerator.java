package com.tpexcotblu.litematicgen.generators;

import com.tpexcotblu.litematicgen.schematic.SchematicVolume;

public class FormulaGenerator implements SchemeGenerator {
    private final int radius;
    private final String formula;
    private final FormulaExpression expression;
    private final double threshold;
    private final boolean filled;

    public FormulaGenerator(int radius, String formula) {
        this(radius, formula, false);
    }

    public FormulaGenerator(int radius, String formula, boolean filled) {
        if (radius < 2 || radius > 96) throw new IllegalArgumentException("Invalid generator dimensions or parameters");
        this.radius = radius;
        this.formula = formula;
        this.expression = new FormulaExpression(formula);
        this.threshold = 0.45;
        this.filled = filled;
    }

    @Override
    public SchematicVolume generate() {
        int size = radius * 2 + 1;
        SchematicVolume blocks = new SchematicVolume(size, size, size);

        for (int y = 0; y < size; y++) {
            for (int z = 0; z < size; z++) {
                for (int x = 0; x < size; x++) {
                    int xx = x - radius;
                    int yy = y - radius;
                    int zz = z - radius;
                    double value = expression.evaluate(xx, yy, zz);
                    boolean shouldPlace = filled ? value <= 0 : Math.abs(value) <= threshold;
                    if (Double.isFinite(value) && shouldPlace) {
                        blocks.setBlock(x, y, z);
                    }
                }
            }
        }

        return blocks;
    }

    @Override
    public String getFilename() {
        return "formula_" + Integer.toHexString(formula.hashCode()) + "_r" + radius + (filled ? "_filled" : "_surface") + ".litematic";
    }

    @Override
    public String getSuccessMessage() {
        return filled ? "Filled formula-based scheme saved" : "Formula-based scheme saved";
    }
}
