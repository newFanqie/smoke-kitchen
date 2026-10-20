package dev.smokekitchen.content;

import com.google.gson.*;
import dev.smokekitchen.cooking.CookingBatch;
import dev.smokekitchen.cooking.DishRecipe;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class ContentCatalog {
    public static JsonObject DATA;
    public static final List<DishRecipe> DISHES = new ArrayList<>();
    public static void load() {
        try (var in = ContentCatalog.class.getResourceAsStream("/data/smoke_kitchen/catalog.json")) {
            if (in == null) throw new IllegalStateException("Missing content catalog");
            DATA = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            for (var element : DATA.getAsJsonArray("dishes")) {
                var d = element.getAsJsonObject();
                Map<String, Integer> counts = new LinkedHashMap<>();
                d.getAsJsonObject("ingredients").entrySet().forEach(e -> counts.put(e.getKey(), e.getValue().getAsInt()));
                Set<CookingBatch.Method> methods = EnumSet.noneOf(CookingBatch.Method.class);
                d.getAsJsonArray("methods").forEach(e -> methods.add(CookingBatch.Method.valueOf(e.getAsString())));
                DISHES.add(new DishRecipe(str(d,"id"), methods, counts, num(d,"servings",1),
                        flag(d,"oil"), flag(d,"salt"), flag(d,"soy"), flag(d,"vinegar")));
            }
        } catch (Exception e) { throw new IllegalStateException("Unable to load Smoke Kitchen catalog", e); }
    }
    public static String str(JsonObject o, String key) { return o.has(key) ? o.get(key).getAsString() : ""; }
    public static int num(JsonObject o, String key, int fallback) { return o.has(key) ? o.get(key).getAsInt() : fallback; }
    public static boolean flag(JsonObject o, String key) { return o.has(key) && o.get(key).getAsBoolean(); }
    private ContentCatalog() {}
}
