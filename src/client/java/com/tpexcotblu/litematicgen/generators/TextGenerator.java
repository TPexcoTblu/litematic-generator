package com.tpexcotblu.litematicgen.generators;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtInt;
import net.minecraft.nbt.NbtList;

import java.awt.*;
import java.awt.image.BufferedImage;

public class TextGenerator implements SchemeGenerator {
    private final String text;
    private final int fontSize;
    private final Font baseFont;

    private int imageWidth;
    private int imageHeight;

    public TextGenerator(String text, int fontSize, Font baseFont) {
        this.text = text;
        this.fontSize = fontSize;
        this.baseFont = baseFont;
    }

    @Override
    public NbtList generateBlocks() {
        Font font = baseFont.deriveFont(Font.PLAIN, fontSize);

        BufferedImage temp = new BufferedImage(1, 1, BufferedImage.TYPE_BYTE_BINARY);
        Graphics2D g2d = temp.createGraphics();
        g2d.setFont(font);
        FontMetrics metrics = g2d.getFontMetrics();
        imageWidth = metrics.stringWidth(text);
        imageHeight = metrics.getHeight();
        g2d.dispose();

        BufferedImage image = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_BYTE_BINARY);
        g2d = image.createGraphics();
        g2d.setFont(font);
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, imageWidth, imageHeight);
        g2d.setColor(Color.BLACK);
        g2d.drawString(text, 0, metrics.getAscent());
        g2d.dispose();

        NbtList blocks = new NbtList();

        for (int y = 0; y < imageHeight; y++) {
            for (int x = 0; x < imageWidth; x++) {
                int rgb = image.getRGB(x, y) & 0xFFFFFF;
                if (rgb == 0x000000) {
                    NbtCompound b = new NbtCompound();
                    NbtList pos = new NbtList();
                    pos.add(NbtInt.of(x));
                    pos.add(NbtInt.of(imageHeight - 1 - y));
                    pos.add(NbtInt.of(0));
                    b.put("pos", pos);
                    b.putInt("state", 1);
                    blocks.add(b);
                }
            }
        }

        return blocks;
    }

    @Override
    public NbtList generateSize() {
        NbtList size = new NbtList();
        size.add(NbtInt.of(imageWidth));
        size.add(NbtInt.of(imageHeight));
        size.add(NbtInt.of(1));
        return size;
    }

    @Override
    public String getFilename() {
        return "text_" + text.replaceAll("[^a-zA-Z0-9]", "_") + ".nbt";
    }

    @Override
    public String getSuccessMessage() {
        return "The text layout is preserved";
    }
}