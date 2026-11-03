package com.tpexcotblu.litematicgen.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;

/** Vanilla resource packs take priority; the fallback also works without Fabric's resource loader API. */
public final class ClientText {
    private static final Map<String, JsonObject> LANGUAGES = Map.of("en_us", read("en_us"), "ru_ru", read("ru_ru"));
    private ClientText() { }

    public static Component text(String key, Object... arguments) {
        String translated = I18n.get(key, arguments);
        if (!translated.equals(key)) return Component.literal(translated);
        String locale = Minecraft.getInstance().options.languageCode;
        JsonObject language = LANGUAGES.getOrDefault(locale, LANGUAGES.get("en_us"));
        String value = language.has(key) ? language.get(key).getAsString() : key;
        return Component.literal(String.format(Locale.ROOT, value, arguments));
    }

    private static JsonObject read(String locale) {
        String path = "/assets/litematic-generator/lang/" + locale + ".json";
        try (var stream = ClientText.class.getResourceAsStream(path)) {
            if (stream == null) return new JsonObject();
            try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                return JsonParser.parseReader(reader).getAsJsonObject();
            }
        } catch (java.io.IOException e) {
            throw new IllegalStateException("Could not read bundled translations: " + locale, e);
        }
    }
}
