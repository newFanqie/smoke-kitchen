package dev.smokekitchen.cooking;

import java.util.*;

/** Recipe naming checks preparation evidence recorded when ingredients enter the pot. */
public record DishRecipe(String id,Set<CookingBatch.Method> methods,Map<String,Integer> ingredients,
                         int servings,boolean oil,boolean salt,boolean soy,boolean vinegar,
                         Map<String,Set<CookingBatch.Cut>> cuts,List<AdditionRule> steps,
                         Map<String,Double> finishSeasonings,int minimumStirs) {
    public record AdditionRule(String ingredient,String after,double cooked,double temperature,boolean hotOil) {}
    public record Problem(String key,String ingredient,String related) {}
    public DishRecipe(String id,Set<CookingBatch.Method> methods,Map<String,Integer> ingredients,
                      int servings,boolean oil,boolean salt,boolean soy,boolean vinegar) {
        this(id,methods,ingredients,servings,oil,salt,soy,vinegar,Map.of(),List.of(),Map.of(),0);
    }
    public boolean matches(CookingBatch batch){return problem(batch).isEmpty();}
    public int distance(CookingBatch batch) {
        var ids=new HashSet<>(ingredients.keySet());batch.ingredients.forEach(p->ids.add(p.id));
        return ids.stream().mapToInt(key->Math.abs(ingredients.getOrDefault(key,0)-(int)batch.count(key))).sum();
    }
    public Optional<Problem> problem(CookingBatch batch) {
        if(!methods.contains(batch.method))return issue("method","","");
        for(var portion:batch.ingredients) {
            var required=cuts.get(portion.id);
            if(required!=null&&!required.contains(portion.cut))return issue("cut",portion.id,required.iterator().next().name());
            for(var step:steps)if(step.ingredient.equals(portion.id)) {
                if(!portion.additionRecorded)return issue("history",portion.id,"");
                if(portion.temperatureOnAdd<step.temperature || step.hotOil&&portion.oilOnAdd<1)return issue("preheat",portion.id,Integer.toString((int)step.temperature));
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
        for(var entry:finishSeasonings.entrySet())if(batch.seasoningCounts.getOrDefault(entry.getKey(),0)<batch.ingredients.size() || batch.seasoningProgress.getOrDefault(entry.getKey(),-1d)<entry.getValue())return issue("finish",entry.getKey(),"");
        if(batch.stirs-batch.stirsAtLastAddition<minimumStirs)return issue("stir","",Integer.toString(minimumStirs));
        return Optional.empty();
    }
    private static Optional<Problem> issue(String key,String ingredient,String related){return Optional.of(new Problem(key,ingredient,related));}
}
