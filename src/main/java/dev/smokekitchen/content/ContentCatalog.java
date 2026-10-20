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
                var cuts=new LinkedHashMap<String,Set<CookingBatch.Cut>>();
                if(d.has("cuts"))d.getAsJsonObject("cuts").entrySet().forEach(e->{var allowed=EnumSet.noneOf(CookingBatch.Cut.class);e.getValue().getAsJsonArray().forEach(v->allowed.add(CookingBatch.Cut.valueOf(v.getAsString())));cuts.put(e.getKey(),allowed);});
                var steps=new ArrayList<DishRecipe.AdditionRule>();
                if(d.has("steps"))d.getAsJsonArray("steps").forEach(e->{var step=e.getAsJsonObject();steps.add(new DishRecipe.AdditionRule(str(step,"ingredient"),str(step,"after"),step.has("cooked")?step.get("cooked").getAsDouble():0,num(step,"temperature",0),flag(step,"hot_oil")));});
                var finish=new LinkedHashMap<String,Double>();
                if(d.has("finish_seasonings"))d.getAsJsonObject("finish_seasonings").entrySet().forEach(e->finish.put(e.getKey(),e.getValue().getAsDouble()));
                DISHES.add(new DishRecipe(str(d,"id"),methods,counts,num(d,"servings",1),flag(d,"oil"),flag(d,"salt"),flag(d,"soy"),flag(d,"vinegar"),cuts,steps,finish,num(d,"stirs",0)));
            }
        } catch (Exception e) { throw new IllegalStateException("Unable to load Smoke Kitchen catalog", e); }
    }
    public static String str(JsonObject o, String key) { return o.has(key) ? o.get(key).getAsString() : ""; }
    public static int num(JsonObject o, String key, int fallback) { return o.has(key) ? o.get(key).getAsInt() : fallback; }
    public static boolean flag(JsonObject o, String key) { return o.has(key) && o.get(key).getAsBoolean(); }
    private ContentCatalog() {}
}
