package com.tpexcotblu.litematicgen.client;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

public final class GeneratorHotkeys {
    // A literal brand name stays readable in vanilla Controls even without a resource-loader API.
    public static final KeyMapping OPEN = MinecraftCompat.createOpenKey();
    private GeneratorHotkeys() { }

    public static void tick() {
        Minecraft client = Minecraft.getInstance();
        while (OPEN.consumeClick()) {
            if (client.level != null && MinecraftCompat.currentScreen() == null) {
                MinecraftCompat.open(new GeneratorScreen(null));
            }
        }
    }
}
