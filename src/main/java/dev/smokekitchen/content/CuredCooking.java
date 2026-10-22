package dev.smokekitchen.content;

import dev.smokekitchen.SmokeKitchen;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

/** Vanilla furnace recipes, with only freshness/quality validation and component retention added. */
public final class CuredCooking {
    public static final RecipeSerializer<SmeltingRecipe> SMELT=new RecipeSerializer<>(AbstractCookingRecipe.cookingMapCodec(Smelt::new,200),AbstractCookingRecipe.cookingStreamCodec(Smelt::new));
    public static final RecipeSerializer<SmokingRecipe> SMOKE=new RecipeSerializer<>(AbstractCookingRecipe.cookingMapCodec(Smoke::new,100),AbstractCookingRecipe.cookingStreamCodec(Smoke::new));
    public static void register(){Registry.register(BuiltInRegistries.RECIPE_SERIALIZER,SmokeKitchen.id("cured_smelting"),SMELT);Registry.register(BuiltInRegistries.RECIPE_SERIALIZER,SmokeKitchen.id("cured_smoking"),SMOKE);}
    private static boolean valid(SingleRecipeInput input,Level level){var s=input.item();var tag=s.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();return tag.getIntOr("kitchen_meat_salt",1)==1&&KitchenFoodItem.freshnessStage(s,level.getGameTime(),tag.getLongOr("kitchen_prepared",level.getGameTime()))==0;}
    private static ItemStack retain(ItemStack result,SingleRecipeInput input){var s=input.item();var tag=s.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();result.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));if(s.has(DataComponents.CUSTOM_NAME))result.set(DataComponents.CUSTOM_NAME,s.get(DataComponents.CUSTOM_NAME));if(tag.contains("kitchen_prepared"))KitchenFoodItem.stampMeal(result,tag.getIntOr("kitchen_quality",3),tag.getStringOr("kitchen_quality_reasons","").isEmpty()?java.util.List.of():java.util.Arrays.asList(tag.getStringOr("kitchen_quality_reasons","").split(",")),tag.getLongOr("kitchen_prepared",0));return result;}
    private static final class Smelt extends SmeltingRecipe {
        Smelt(Recipe.CommonInfo common,CookingBookInfo book,Ingredient in,ItemStackTemplate out,float xp,int time){super(common,book,in,out,xp,time);}
        @Override public boolean matches(SingleRecipeInput input,Level level){return super.matches(input,level)&&valid(input,level);}
        @Override public ItemStack assemble(SingleRecipeInput input){return retain(super.assemble(input),input);}
        @Override public RecipeSerializer<SmeltingRecipe> getSerializer(){return SMELT;}
    }
    private static final class Smoke extends SmokingRecipe {
        Smoke(Recipe.CommonInfo common,CookingBookInfo book,Ingredient in,ItemStackTemplate out,float xp,int time){super(common,book,in,out,xp,time);}
        @Override public boolean matches(SingleRecipeInput input,Level level){return super.matches(input,level)&&valid(input,level);}
        @Override public ItemStack assemble(SingleRecipeInput input){return retain(super.assemble(input),input);}
        @Override public RecipeSerializer<SmokingRecipe> getSerializer(){return SMOKE;}
    }
}
