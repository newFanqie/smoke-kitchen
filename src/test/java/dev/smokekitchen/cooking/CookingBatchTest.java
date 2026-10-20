package dev.smokekitchen.cooking;

import org.junit.jupiter.api.Test;
import java.util.Map;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class CookingBatchTest {
    @Test void boilingAndSteamingRequireWaterEvenForMeat() {
        for(var method:new CookingBatch.Method[]{CookingBatch.Method.BOIL,CookingBatch.Method.STEAM}) {
            var b=new CookingBatch();b.method=method;b.add("chicken",CookingBatch.Cut.WHOLE,1.6,false,false);
            for(int i=0;i<150;i++)b.tick(2,true);
            assertFalse(b.ready());
            b=new CookingBatch();b.method=method;b.add("chicken",CookingBatch.Cut.WHOLE,1.6,false,false);b.addWater(6);
            for(int i=0;i<150;i++)b.tick(2,true);
            assertTrue(b.ready());assertFalse(b.burnt());
        }
    }
    private CookingBatch batch() {
        var b = new CookingBatch();
        b.oil = 1;
        b.salt = 1;
        b.add("egg", CookingBatch.Cut.DICE, 1, false, false);
        return b;
    }
    @Test void lateMeatDoesNotInheritCookedness() {
        var b = batch();
        for (int i = 0; i < 90; i++) { b.stir(); b.tick(2, false); }
        assertTrue(b.ready());
        b.add("beef", CookingBatch.Cut.DICE, 1.8, false, false);
        assertFalse(b.ready());
        assertEquals(0, b.ingredients.getLast().cooked);
    }
    @Test void riceCannotCookWithoutWater() {
        var b = new CookingBatch(); b.method = CookingBatch.Method.BOIL;
        b.add("rice", CookingBatch.Cut.WHOLE, 1, true, false);
        for (int i = 0; i < 180; i++) b.tick(2, true);
        assertFalse(b.ready());
        b.water = 6;
        for (int i = 0; i < 100; i++) b.tick(2, true);
        assertTrue(b.ready());
    }
    @Test void neglectedHighHeatBurnsFood() {
        var b = batch();
        for (int i = 0; i < 180; i++) b.tick(3, false);
        assertTrue(b.burnt());
    }
    @Test void stirringReducesBurning() {
        var a = batch(); var b = batch();
        for (int i = 0; i < 120; i++) {
            a.tick(3, false);
            if (i % 6 == 0) b.stir();
            b.tick(3, false);
        }
        assertTrue(a.ingredients.getFirst().charred > b.ingredients.getFirst().charred);
    }
    @Test void residualHeatSurvivesSwitchingOff() {
        var b = batch(); for (int i = 0; i < 20; i++) b.tick(3, false);
        double before = b.ingredients.getFirst().cooked;
        b.tick(0, false);
        assertTrue(b.temperature > 20);
        assertTrue(b.ingredients.getFirst().cooked > before);
    }
    @Test void firstServingLocksYieldAndRejectsRefills() {
        var b = batch(); b.ingredients.getFirst().cooked = 1;
        assertTrue(b.serve("fried_rice", 2));
        assertFalse(b.add("rice", CookingBatch.Cut.DICE, 1, false, true));
        assertTrue(b.serve("fried_rice", 200));
        assertFalse(b.serve("fried_rice", 200));
        assertEquals(0, b.remainingServings);
    }
    @Test void oversaltingReducesQualityAndCanBeDilutedBeforeServing() {
        var b = batch(); b.ingredients.getFirst().cooked = 1; b.stirs = 5; b.salt = 2;
        assertEquals(1, b.quality());
        for (int i = 0; i < 3; i++) b.add("rice", CookingBatch.Cut.DICE, 1, false, true);
        b.stir();b.stir();b.stir();
        assertEquals(3, b.quality());
    }
    @Test void fullPanDoesNotConsumeMoreIngredients() {
        var b = batch();
        for (int i = 0; i < 5; i++) assertTrue(b.add("rice", CookingBatch.Cut.DICE, 1, false, false));
        assertFalse(b.add("rice", CookingBatch.Cut.DICE, 1, false, false));
        assertEquals(6, b.ingredients.size());
    }
    @Test void namedRecipesRejectExtraIngredients() {
        var b = batch();
        var recipe = new DishRecipe("omelette", Set.of(CookingBatch.Method.STIR_FRY), Map.of("egg", 1), 1, true, true, false, false);
        assertTrue(recipe.matches(b));
        b.add("beef", CookingBatch.Cut.DICE, 1, false, false);
        assertFalse(recipe.matches(b));
    }
    @Test void malformedSavedDataIsBounded() {
        var b = batch(); b.temperature = Double.NaN; b.water = -5; b.salt = 1000;
        b.ingredients.getFirst().resistance = 0; b.normalize();
        assertEquals(20, b.temperature); assertEquals(0, b.water); assertEquals(32, b.salt);
        assertTrue(b.ingredients.getFirst().resistance > 0);
    }
}
