package com.tpexcotblu.litematicgen.schematic;

import com.tpexcotblu.litematicgen.generators.*;
import java.awt.Font;
import java.nio.file.Path;

/** Fixtures are decoded independently by tests/test_litematic.py. */
public final class CoreFixtures {
    public static void main(String[] args) throws Exception {
        Path directory = Path.of(args[0]);
        String[] expressions = {"x*-2", "-2^2", "2^-2", "--x", "sin(-pi/2)"};
        double[] expectedValues = {-6, -4, 0.25, 3, -1};
        for (int i = 0; i < expressions.length; i++) {
            if (Math.abs(new FormulaExpression(expressions[i]).evaluate(3, 0, 0) - expectedValues[i]) > 1e-10) {
                throw new AssertionError("Unary expression: " + expressions[i]);
            }
        }
        SchematicVolume boundary = new SchematicVolume(33, 2, 3);
        boundary.setBlock(31, 0, 0);
        boundary.setBlock(32, 0, 0);
        boundary.setBlock(0, 1, 2);
        boundary.setBlock(0, 1, 2);
        if (boundary.blockCount() != 3) throw new AssertionError("Duplicate block count");
        LitematicWriter.write(directory.resolve("boundary.litematic"), boundary, "Boundary", "Author", 4440);
        LitematicWriter.write(directory.resolve("boundary.litematic"), boundary, "Boundary", "Author", 4440);
        SchemeGenerator[] generators = {
            new CircleGenerator(4, "horizontal"), new CircleGenerator(4, "vertical"),
            new SphereGenerator(4), new ParabolaGenerator(8, 4), new ParabolaGenerator(1, 3),
            new FormulaGenerator(4, "x*x+y*y+z*z-16", true),
            new FormulaGenerator(4, "sqrt(x*x+y*y+z*z)-4"),
            new TextGenerator("Hello", 12, new Font("Dialog", Font.PLAIN, 12)),
            new TextGenerator("", 12, new Font("Dialog", Font.PLAIN, 12)),
            new TextGenerator("\uD83D\uDE00", 12, new Font("Dialog", Font.PLAIN, 12))
        };
        for (SchemeGenerator generator : generators) {
            var volume = generator.generate();
            LitematicWriter.write(directory.resolve(generator.getFilename()), volume,
                    generator.getFilename(), "Author", 4440);
        }
        try {
            new SchematicVolume(Integer.MAX_VALUE, 2, 2);
            throw new AssertionError("Oversized region accepted");
        } catch (IllegalArgumentException expected) { }
        try {
            boundary.setBlock(33, 0, 0);
            throw new AssertionError("Out of bounds block accepted");
        } catch (IllegalArgumentException expected) { }
    }
}
