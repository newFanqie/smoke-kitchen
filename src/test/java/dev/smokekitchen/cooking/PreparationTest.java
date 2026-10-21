package dev.smokekitchen.cooking;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class PreparationTest {
    private DishRecipe catalogRecipe(String id){if(dev.smokekitchen.content.ContentCatalog.DISHES.isEmpty())dev.smokekitchen.content.ContentCatalog.load();return dev.smokekitchen.content.ContentCatalog.DISHES.stream().filter(r->r.id().equals(id)).findFirst().orElseThrow();}
    @Test void actualPotatoRecipeRequiresShredsGarlicFirstAndLateVinegar(){
        var r=catalogRecipe("sour_spicy_potato");var b=new CookingBatch();b.oil=1;b.salt=1;b.vinegar=1;b.temperature=150;
        b.add("garlic",CookingBatch.Cut.MINCE,1,false,false);b.add("potato",CookingBatch.Cut.SHRED,1,false,false);b.add("potato",CookingBatch.Cut.SHRED,1,false,false);b.add("chili",CookingBatch.Cut.WHOLE,1,false,false);
        b.ingredients.forEach(p->p.cooked=1);b.season("vinegar");b.stir();b.stir();b.stir();assertTrue(r.matches(b));
        b.ingredients.get(1).cut=CookingBatch.Cut.WHOLE;assertEquals("cut",r.problem(b).orElseThrow().key());
    }
    @Test void actualMapoRecipeRequiresMincedMeatCookedBeforeDicedTofu(){
        var r=catalogRecipe("mapo_tofu");var b=new CookingBatch();b.oil=1;b.soy=1;b.water=1;b.temperature=140;
        b.add("pork",CookingBatch.Cut.MINCE,1,false,false);b.ingredients.getFirst().cooked=.7;
        b.add("tofu",CookingBatch.Cut.DICE,1,false,true);b.add("tofu",CookingBatch.Cut.DICE,1,false,true);b.add("chili",CookingBatch.Cut.WHOLE,1,false,false);b.add("sichuan_pepper",CookingBatch.Cut.WHOLE,1,false,false);b.stir();b.stir();b.stir();
        assertTrue(r.matches(b));b.water=0;assertEquals("water",r.problem(b).orElseThrow().key());b.water=1;var pepper=b.ingredients.removeLast();assertEquals("missing",r.problem(b).orElseThrow().key());b.ingredients.add(pepper);b.ingredients.getFirst().cut=CookingBatch.Cut.SLICE;assertEquals("cut",r.problem(b).orElseThrow().key());
        b.ingredients.getFirst().cut=CookingBatch.Cut.MINCE;b.ingredients.get(1).priorCooked.put("pork",.1);assertEquals("stage",r.problem(b).orElseThrow().key());
    }
    private DishRecipe riceRecipe(){return new DishRecipe("fried_rice",Set.of(CookingBatch.Method.STIR_FRY),Map.of("egg",1,"rice_cooked",2,"scallion",1),2,true,true,false,false,
        Map.of("scallion",Set.of(CookingBatch.Cut.DICE)),List.of(new DishRecipe.AdditionRule("egg","",0,110,true),new DishRecipe.AdditionRule("rice_cooked","egg",.4,0,false),new DishRecipe.AdditionRule("scallion","rice_cooked",0,0,false)),Map.of(),3);}
    private CookingBatch correctlyStartedRice(){
        var b=new CookingBatch();b.oil=1;b.salt=1;b.temperature=130;b.add("egg",CookingBatch.Cut.WHOLE,1,false,false);b.ingredients.getFirst().cooked=.5;
        b.add("rice_cooked",CookingBatch.Cut.WHOLE,1,false,true);b.add("rice_cooked",CookingBatch.Cut.WHOLE,1,false,true);b.add("scallion",CookingBatch.Cut.DICE,1,false,false);
        b.stir();b.stir();b.stir();return b;
    }
    @Test void cookedRiceAndLateScallionMatch(){assertTrue(riceRecipe().matches(correctlyStartedRice()));}
    @Test void coldEggCannotBeFixedByHeatingAfterAdding(){var b=correctlyStartedRice();b.ingredients.getFirst().temperatureOnAdd=20;b.temperature=200;assertEquals("preheat",riceRecipe().problem(b).orElseThrow().key());}
    @Test void riceMustFollowPartlyCookedEgg(){var b=new CookingBatch();b.oil=1;b.salt=1;b.temperature=130;b.add("egg",CookingBatch.Cut.WHOLE,1,false,false);b.add("rice_cooked",CookingBatch.Cut.WHOLE,1,false,true);assertEquals("stage",riceRecipe().problem(b).orElseThrow().key());b.ingredients.getFirst().cooked=1;assertEquals("stage",riceRecipe().problem(b).orElseThrow().key());}
    @Test void rawRiceCannotStandForCookedRice(){var b=correctlyStartedRice();b.ingredients.get(1).id="rice";assertFalse(riceRecipe().matches(b));}
    @Test void wholeScallionFailsCutAndEarlyScallionFailsOrder(){var b=correctlyStartedRice();b.ingredients.getLast().cut=CookingBatch.Cut.WHOLE;assertEquals("cut",riceRecipe().problem(b).orElseThrow().key());b.ingredients.getLast().cut=CookingBatch.Cut.DICE;b.ingredients.getLast().priorCounts.clear();assertEquals("order",riceRecipe().problem(b).orElseThrow().key());}
    @Test void StirringAnEmptyPotDoesNotCountForLaterFood(){var b=correctlyStartedRice();b.add("onion",CookingBatch.Cut.DICE,1,false,true);var r=new DishRecipe("mixed",Set.of(b.method),Map.of("egg",1,"rice_cooked",2,"scallion",1,"onion",1),2,true,true,false,false,Map.of(),List.of(),Map.of(),3);assertEquals("stir",r.problem(b).orElseThrow().key());}
    @Test void vinegarMustBeAddedAfterIngredientsAreMostlyCooked(){var r=new DishRecipe("potatoes",Set.of(CookingBatch.Method.STIR_FRY),Map.of("potato",1),1,true,false,false,true,Map.of("potato",Set.of(CookingBatch.Cut.SHRED)),List.of(),Map.of("vinegar",.7),0);var b=new CookingBatch();b.oil=1;b.vinegar=1;b.add("potato",CookingBatch.Cut.SHRED,1,false,false);b.season("vinegar");b.ingredients.getFirst().cooked=1;assertEquals("finish",r.problem(b).orElseThrow().key());b.season("vinegar");assertTrue(r.matches(b));}
    @Test void oldSaveWithoutStageEvidenceHasAnExplicitReason(){var b=correctlyStartedRice();b.ingredients.getFirst().additionRecorded=false;assertEquals("history",riceRecipe().problem(b).orElseThrow().key());}
}
