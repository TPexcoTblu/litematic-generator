package com.tpexcotblu.litematicgen.client;

import com.tpexcotblu.litematicgen.generators.*;
import com.tpexcotblu.litematicgen.schematic.LitematicWriter;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.awt.Font;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/** Vanilla widgets shared by Fabric, Quilt and NeoForge, with generation off the render thread. */
public final class GeneratorScreen extends GeneratorScreenBase {
    private static final String[] SHAPES = {"sphere", "circle_horizontal", "circle_vertical", "parabola", "text", "formula"};
    private static final AtomicBoolean BUSY = new AtomicBoolean();
    private static final java.util.concurrent.ExecutorService WORKER = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "litematic-generator");
        thread.setDaemon(true);
        return thread;
    });
    private final Screen parent;
    private int shape;
    private String first = "16", second = "16", text = "Litematic", formula = "sqrt(x*x+y*y+z*z)-16";
    private boolean filled;
    private Button generate, copy, load;
    private StringWidget status;
    private Component message = ClientText.text("litematicgen.ready");
    private Path output;

    public GeneratorScreen(Screen parent) {
        super(ClientText.text("litematicgen.open"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int w = Math.min(340, width - 20), x = (width - w) / 2, y = Math.max(6, (height - 238) / 2);
        addRenderableWidget(new StringWidget(x, y, w, 20, title, font));
        addRenderableWidget(Button.builder(ClientText.text("litematicgen.shape." + SHAPES[shape]), button -> {
            shape = (shape + 1) % SHAPES.length;
            rebuildWidgets();
        }).bounds(x, y + 24, w, 20).build());

        String firstLabel = switch (shape) {
            case 3 -> "width";
            case 4 -> "font_size";
            case 5 -> "formula_radius";
            default -> "radius";
        };
        label(x, y + 50, w / 2 - 8, firstLabel);
        edit(x + w / 2, y + 50, w / 2, first, 4, value -> first = value);
        label(x, y + 76, w / 2 - 8, "height").visible = shape == 3;
        EditBox heightInput = edit(x + w / 2, y + 76, w / 2, second, 4, value -> second = value);
        heightInput.active = shape == 3;
        heightInput.visible = shape == 3;

        EditBox textInput = edit(x, y + 102, w, shape == 4 ? text : formula, 1024, value -> {
            if (shape == 4) text = value; else formula = value;
        });
        textInput.setHint(ClientText.text(shape == 4 ? "litematicgen.text" : "litematicgen.formula"));
        textInput.active = shape == 4 || shape == 5;
        textInput.visible = textInput.active;
        addRenderableWidget(Button.builder(ClientText.text(filled ? "litematicgen.filled" : "litematicgen.outline"), button -> {
            filled = !filled;
            button.setMessage(ClientText.text(filled ? "litematicgen.filled" : "litematicgen.outline"));
        }).bounds(x, y + 128, w, 20).build()).visible = shape == 5;

        generate = addRenderableWidget(Button.builder(ClientText.text("litematicgen.generate"), button -> generate())
                .bounds(x, y + 154, w, 20).build());
        generate.active = !BUSY.get();
        status = addRenderableWidget(new StringWidget(x, y + 179, w, 20, message, font));
        copy = addRenderableWidget(Button.builder(ClientText.text("litematicgen.copy_path"),
                button -> Minecraft.getInstance().keyboardHandler.setClipboard(output.toString()))
                .bounds(x, y + 206, w / 3 - 3, 20).build());
        copy.active = output != null;
        load = addRenderableWidget(Button.builder(ClientText.text("litematicgen.load"), button -> {
            try {
                LitematicaBridge.load(output);
                setStatus(ClientText.text("litematicgen.loaded"));
            } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
                setStatus(ClientText.text("litematicgen.load_failed"));
            }
        }).bounds(x + w / 3, y + 206, w / 3 - 3, 20).build());
        load.active = output != null && Minecraft.getInstance().player != null && LitematicaBridge.available();
        addRenderableWidget(Button.builder(ClientText.text("gui.done"), button -> onClose())
                .bounds(x + 2 * w / 3, y + 206, w / 3, 20).build());
    }

    private StringWidget label(int x, int y, int w, String key) {
        return addRenderableWidget(new StringWidget(x, y, w, 20, ClientText.text("litematicgen." + key), font));
    }

    private EditBox edit(int x, int y, int w, String value, int maxLength, Consumer<String> responder) {
        EditBox box = new EditBox(font, x, y, w, 20, Component.literal(value));
        box.setMaxLength(maxLength);
        box.setValue(value);
        box.setResponder(responder);
        return addRenderableWidget(box);
    }

    private SchemeGenerator generator() throws Exception {
        int a = Integer.parseInt(first);
        return switch (shape) {
            case 0 -> new SphereGenerator(a);
            case 1 -> new CircleGenerator(a, "horizontal");
            case 2 -> new CircleGenerator(a, "vertical");
            case 3 -> new ParabolaGenerator(a, Integer.parseInt(second));
            case 4 -> {
                try (var stream = GeneratorScreen.class.getResourceAsStream("/assets/litematic-generator/fonts/Minecraftia-Regular.ttf")) {
                    if (stream == null) throw new IllegalStateException("Embedded font is missing");
                    yield new TextGenerator(text, a, Font.createFont(Font.TRUETYPE_FONT, stream));
                }
            }
            case 5 -> new FormulaGenerator(a, formula, filled);
            default -> throw new IllegalStateException("Unknown shape");
        };
    }

    private void generate() {
        final SchemeGenerator generator;
        try {
            generator = generator();
        } catch (Exception e) {
            setStatus(ClientText.text("litematicgen.error", e.getMessage()));
            return;
        }
        if (!BUSY.compareAndSet(false, true)) return;
        generate.active = false;
        setStatus(ClientText.text("litematicgen.working"));
        Minecraft client = Minecraft.getInstance();
        Path target = client.gameDirectory.toPath().resolve("schematics").resolve(generator.getFilename());
        String author = client.getUser().getName();
        int dataVersion = SharedConstants.getCurrentVersion().dataVersion().version();
        CompletableFuture.runAsync(() -> {
            try {
                LitematicWriter.write(target, generator.generate(), generator.getFilename(), author, dataVersion);
            } catch (Exception e) {
                throw new java.util.concurrent.CompletionException(e);
            }
        }, WORKER).whenComplete((unused, error) -> {
            BUSY.set(false);
            client.execute(() -> {
                generate.active = true;
                if (error == null) {
                    output = target.toAbsolutePath();
                    copy.active = true;
                    setStatus(ClientText.text("litematicgen.saved"));
                } else {
                    Throwable cause = error.getCause() == null ? error : error.getCause();
                    setStatus(ClientText.text("litematicgen.error", cause.getMessage()));
                }
            });
        });
    }

    private void setStatus(Component value) {
        message = value;
        status.setMessage(value);
    }

    @Override
    public void tick() {
        generate.active = !BUSY.get();
        load.active = output != null && Minecraft.getInstance().player != null && LitematicaBridge.available();
    }

    @Override
    public void onClose() { MinecraftCompat.open(parent); }
}
