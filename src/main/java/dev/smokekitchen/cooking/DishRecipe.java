package dev.smokekitchen.cooking;

import java.util.*;

/** Recipe naming checks preparation evidence recorded when ingredients enter the pot. */
public record DishRecipe(String id,Set<CookingBatch.Method> methods,Map<String,Integer> ingredients,
                         int servings,boolean oil,boolean salt,boolean soy,boolean vinegar,
                         Map<String,Set<CookingBatch.Cut>> cuts,List<AdditionRule> steps,
                         Map<String,Double> finishSeasonings,int minimumStirs,double minimumWater,int minimumCoveredSeconds,StapleRule staple) {
    public record StapleRule(double waterOnAddMin,double waterOnAddMax,double cooked,int lowHeatSeconds,int sugar,boolean rolled,int ovenSeconds) {
        public static final StapleRule NONE=new StapleRule(0,12,0,0,0,false,0);
    }
    public DishRecipe(String id,Set<CookingBatch.Method> methods,Map<String,Integer> ingredients,int servings,boolean oil,boolean salt,boolean soy,boolean vinegar,Map<String,Set<CookingBatch.Cut>> cuts,List<AdditionRule> steps,Map<String,Double> finishSeasonings,int minimumStirs,double minimumWater,int minimumCoveredSeconds) {
        this(id,methods,ingredients,servings,oil,salt,soy,vinegar,cuts,steps,finishSeasonings,minimumStirs,minimumWater,minimumCoveredSeconds,StapleRule.NONE);
    }
    public record AdditionRule(String ingredient,String after,double cooked,double temperature,boolean hotOil,boolean boilingWater) {
        public AdditionRule(String ingredient,String after,double cooked,double temperature,boolean hotOil){this(ingredient,after,cooked,temperature,hotOil,false);}
    }
    public record Problem(String key,String ingredient,String related) {}
    public DishRecipe(String id,Set<CookingBatch.Method> methods,Map<String,Integer> ingredients,
                      int servings,boolean oil,boolean salt,boolean soy,boolean vinegar) {
        this(id,methods,ingredients,servings,oil,salt,soy,vinegar,Map.of(),List.of(),Map.of(),0,0,0);
    }
    public DishRecipe(String id,Set<CookingBatch.Method> methods,Map<String,Integer> ingredients,int servings,boolean oil,boolean salt,boolean soy,boolean vinegar,Map<String,Set<CookingBatch.Cut>> cuts,List<AdditionRule> steps,Map<String,Double> finishSeasonings,int minimumStirs) {
        this(id,methods,ingredients,servings,oil,salt,soy,vinegar,cuts,steps,finishSeasonings,minimumStirs,0,0);
    }
    public boolean matches(CookingBatch batch){return problem(batch).isEmpty();}
    public int distance(CookingBatch batch) {
        var ids=new HashSet<>(ingredients.keySet());batch.ingredients.forEach(p->ids.add(p.id));
        return ids.stream().mapToInt(key->Math.abs(ingredients.getOrDefault(key,0)-(int)batch.count(key))).sum();
    }
    public Optional<Problem> problem(CookingBatch batch) {
        if(!methods.contains(batch.method))return issue("method","","");
        if(batch.water<minimumWater)return issue("water","",Double.toString(minimumWater));
        for(var portion:batch.ingredients) {
            var required=cuts.get(portion.id);
            if(required!=null&&!required.contains(portion.cut))return issue("cut",portion.id,required.iterator().next().name());
            for(var step:steps)if(step.ingredient.equals(portion.id)) {
                if(!portion.additionRecorded)return issue("history",portion.id,"");
                if(step.boilingWater&&(portion.waterOnAdd<1||Math.round(portion.temperatureOnAdd)<step.temperature))return issue("boiling",portion.id,Integer.toString((int)step.temperature));
                if(Math.round(portion.temperatureOnAdd)<step.temperature || step.hotOil&&portion.oilOnAdd<1)return issue("preheat",portion.id,Integer.toString((int)step.temperature));
                if(!step.after.isEmpty()) {
                    if(portion.priorCounts.getOrDefault(step.after,0)<ingredients.getOrDefault(step.after,1))return issue("order",portion.id,step.after);
                    if(portion.priorCooked.getOrDefault(step.after,-1d)<step.cooked)return issue("stage",portion.id,step.after);
                }
            }
        }
        for(var entry:ingredients.entrySet())if(batch.count(entry.getKey())<entry.getValue())return issue("missing",entry.getKey(),Integer.toString(entry.getValue()-(int)batch.count(entry.getKey())));
        if(distance(batch)!=0)return issue("extra","","");
        if(oil&&batch.oil<1)return issue("seasoning","oil","");
        if(salt&&batch.salt<1)return issue("seasoning","salt","");
        if(soy&&batch.soy<1)return issue("seasoning","soy","");
        if(vinegar&&batch.vinegar<1)return issue("seasoning","vinegar","");
        if(batch.sugar<staple.sugar)return issue("seasoning","sugar","");
        for(var p:batch.ingredients) {
            if(p.waterOnAdd<staple.waterOnAddMin||p.waterOnAdd>staple.waterOnAddMax)return issue("water_ratio",p.id,staple.waterOnAddMin+"–"+staple.waterOnAddMax);
            if(p.cooked<staple.cooked)return issue("soften",p.id,Integer.toString((int)Math.round(staple.cooked*100)));
            if(p.lowHeatSeconds<staple.lowHeatSeconds)return issue("low_heat","",Integer.toString(staple.lowHeatSeconds));
            if(staple.rolled&&p.id.equals("dough")&&!p.rolled)return issue("roll",p.id,"");
        }
        if(batch.ovenSeconds<staple.ovenSeconds)return issue("oven","",Integer.toString(staple.ovenSeconds));
        for(var entry:finishSeasonings.entrySet())if(batch.seasoningCounts.getOrDefault(entry.getKey(),0)<batch.ingredients.size() || batch.seasoningProgress.getOrDefault(entry.getKey(),-1d)<entry.getValue())return issue("finish",entry.getKey(),Integer.toString((int)Math.round(entry.getValue()*100)));
        if(batch.ingredients.stream().anyMatch(p->p.coveredCookingSeconds<minimumCoveredSeconds))return issue("cover","",Integer.toString(minimumCoveredSeconds));
        if(batch.stirs-batch.stirsAtLastAddition<minimumStirs)return issue("stir","",Integer.toString(minimumStirs));
        return Optional.empty();
    }
    private static Optional<Problem> issue(String key,String ingredient,String related){return Optional.of(new Problem(key,ingredient,related));}
}
