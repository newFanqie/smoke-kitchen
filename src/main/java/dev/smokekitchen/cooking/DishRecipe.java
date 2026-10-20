package dev.smokekitchen.cooking;

import java.util.Map;
import java.util.Set;

public record DishRecipe(String id, Set<CookingBatch.Method> methods, Map<String, Integer> ingredients,
                         int servings, boolean oil, boolean salt, boolean soy, boolean vinegar) {
    public boolean matches(CookingBatch batch) {
        if (!methods.contains(batch.method)) return false;
        if (oil && batch.oil < 1 || salt && batch.salt < 1 || soy && batch.soy < 1 || vinegar && batch.vinegar < 1) return false;
        int expected = ingredients.values().stream().mapToInt(Integer::intValue).sum();
        if (batch.ingredients.size() != expected) return false;
        return ingredients.entrySet().stream().allMatch(e -> batch.count(e.getKey()) == e.getValue());
    }
}
