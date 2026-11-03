package com.tpexcotblu.litematicgen.generators;

import com.tpexcotblu.litematicgen.schematic.SchematicVolume;

import java.awt.*;
import java.awt.image.BufferedImage;

public class TextGenerator implements SchemeGenerator {
    private final String text;
    private final int fontSize;
    private final Font baseFont;

    private int imageWidth;
    private int imageHeight;

    public TextGenerator(String text, int fontSize, Font baseFont) {
        if (text == null || text.length() > 1024 || fontSize < 4 || fontSize > 64 || baseFont == null) throw new IllegalArgumentException("Invalid generator dimensions or parameters");
        this.text = text;
        this.fontSize = fontSize;
        this.baseFont = baseFont;
    }

    @Override
    public SchematicVolume generate() {
        Font font = baseFont.deriveFont(Font.PLAIN, fontSize);

        BufferedImage temp = new BufferedImage(1, 1, BufferedImage.TYPE_BYTE_BINARY);
        Graphics2D g2d = temp.createGraphics();
        g2d.setFont(font);
        FontMetrics metrics = g2d.getFontMetrics();
        imageWidth = Math.max(1, metrics.stringWidth(text));
        imageHeight = metrics.getHeight();
        g2d.dispose();

        SchematicVolume blocks = new SchematicVolume(imageWidth, imageHeight, 1);
        BufferedImage image = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_BYTE_BINARY);
        g2d = image.createGraphics();
        g2d.setFont(font);
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, imageWidth, imageHeight);
        g2d.setColor(Color.BLACK);
        g2d.drawString(text, 0, metrics.getAscent());
        g2d.dispose();

        for (int y = 0; y < imageHeight; y++) {
            for (int x = 0; x < imageWidth; x++) {
                int rgb = image.getRGB(x, y) & 0xFFFFFF;
                if (rgb == 0x000000) {
                    blocks.setBlock(x, imageHeight - 1 - y, 0);
                }
            }
        }

        return blocks;
    }

    @Override
    public String getFilename() {
        String safe = text.replaceAll("[^a-zA-Z0-9]", "_");
        return "text_" + safe.substring(0, Math.min(48, safe.length()))
                + "_" + fontSize + "_" + Integer.toHexString(text.hashCode()) + ".litematic";
    }

    @Override
    public String getSuccessMessage() {
        return "The text layout is preserved";
    }
}