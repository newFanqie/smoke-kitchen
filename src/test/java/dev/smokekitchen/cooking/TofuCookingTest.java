package dev.smokekitchen.cooking;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TofuCookingTest {
    private CookingBatch milk(int vinegar){var b=new CookingBatch();b.method=CookingBatch.Method.BOIL;b.addWater(1);b.add("soy_milk",CookingBatch.Cut.WHOLE,1,false,false);b.vinegar=vinegar;return b;}
    @Test void acidAndWarmthAreBothNecessary(){
        var plain=milk(0);var cold=milk(1);
        for(int i=0;i<100;i++){plain.tick(2,false);cold.tick(0,false);}
        assertEquals(0,plain.ingredients.getFirst().cooked);assertEquals(0,cold.ingredients.getFirst().cooked);
        var excess=milk(2);excess.temperature=90;for(int i=0;i<10;i++)excess.tick(2,false);assertTrue(excess.ready(),"Excess acid still sets curds; the press retains its quality penalty");
    }
    @Test void tenActualWarmSecondsSetOneBatchAndMixedFoodCannotSubstitute(){
        var b=milk(1);b.temperature=90;for(int i=0;i<9;i++)b.tick(2,false);assertFalse(b.ready());b.tick(2,false);assertTrue(b.ready());
        var mixed=milk(1);mixed.temperature=90;mixed.add("potato",CookingBatch.Cut.DICE,1,false,false);for(int i=0;i<100;i++)mixed.tick(2,false);assertEquals(0,mixed.ingredients.getFirst().cooked);
    }
    @Test void wetGrindingPasteCannotCookWithoutWater(){
        var b=new CookingBatch();b.method=CookingBatch.Method.BOIL;b.add("bean_pulp",CookingBatch.Cut.WHOLE,1.5,true,false);for(int i=0;i<100;i++)b.tick(2,true);assertEquals(0,b.ingredients.getFirst().cooked);
        b.addWater(6);for(int i=0;i<120;i++)b.tick(2,true);assertTrue(b.ready());
    }
}
