package com.tpexcotblu.litematicgen.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;

public final class GeneratorSettingsScreen extends GeneratorScreenBase {
    private final Screen parent;
    public GeneratorSettingsScreen(Screen parent) {
        super(ClientText.text("litematicgen.settings"));
        this.parent = parent;
    }
    @Override protected void init() {
        int w = Math.min(320, width - 20), x = (width - w) / 2, y = height / 2 - 70;
        addRenderableWidget(new StringWidget(x, y, w, 20, title, font));
        addRenderableWidget(Button.builder(ClientText.text("litematicgen.key_binding",
                GeneratorHotkeys.OPEN.getTranslatedKeyMessage().getString()), button ->
                MinecraftCompat.open(new KeyBindsScreen(this, Minecraft.getInstance().options)))
                .bounds(x, y + 30, w, 20).build());
        addRenderableWidget(new StringWidget(x, y + 58, w, 20,
                ClientText.text("litematicgen.key_hint"), font));
        addRenderableWidget(Button.builder(ClientText.text("gui.done"), button -> onClose())
                .bounds(x, y + 96, w, 20).build());
    }
    @Override public void onClose() { MinecraftCompat.open(parent); }
}
