package dev.smokekitchen.cooking;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

/** Server-owned, deterministic cooking simulation. One ingredient entry is one portion. */
public final class CookingBatch {
    public enum Method { STIR_FRY, FRY, BOIL, STEAM, BAKE, STEW, MIX, FERMENT }
    public enum Cut { WHOLE, SLICE, DICE, SHRED, MINCE }
    public static final class Portion {
        public String id;
        public double cooked;
        public double charred;
        public Cut cut;
        public double resistance;
        public boolean needsWater;
        public boolean additionRecorded;
        public double temperatureOnAdd;
        public int oilOnAdd;
        public Map<String,Integer> priorCounts = new HashMap<>();
        public Map<String,Double> priorCooked = new HashMap<>();
        public Portion(String id, Cut cut, double resistance, boolean needsWater, boolean ready) {
            this.id = id;
            this.cut = cut;
            this.resistance = resistance;
            this.needsWater = needsWater;
            cooked = ready ? 1 : 0;
        }
    }

    public List<Portion> ingredients = new ArrayList<>();
    public Method method = Method.STIR_FRY;
    public double temperature = 20;
    public double water;
    public int oil;
    public int salt;
    public int sugar;
    public int soy;
    public int vinegar;
    public int spice;
    public int seconds;
    public int lastStir;
    public int stirs;
    public int stirsAtLastAddition;
    public Map<String,Double> seasoningProgress = new HashMap<>();
    public Map<String,Integer> seasoningCounts = new HashMap<>();
    public int remainingServings = -1;
    public String lockedDish = "";
    public int capacity = 6;

    public boolean add(String id, Cut cut, double resistance, boolean needsWater, boolean ready) {
        if (ingredients.size() >= capacity || remainingServings >= 0) return false;
        var portion=new Portion(id,cut,Math.max(.25,resistance),needsWater,ready);
        portion.additionRecorded=true;portion.temperatureOnAdd=temperature;portion.oilOnAdd=oil;
        for(var previous:ingredients){portion.priorCounts.merge(previous.id,1,Integer::sum);portion.priorCooked.merge(previous.id,previous.cooked,Math::min);}
        ingredients.add(portion);stirsAtLastAddition=stirs;
        temperature=Math.max(20,temperature-8);
        return true;
    }

    public void season(String seasoning) {
        seasoningProgress.put(seasoning,ingredients.stream().mapToDouble(p->p.cooked).min().orElse(-1));
        seasoningCounts.put(seasoning,ingredients.size());
    }

    public void stir() {
        lastStir = seconds;
        stirs++;
    }

    /** Called once per simulated second; heat is supplied by the adjacent stove. */
    public void tick(int heat, boolean lid) {
        seconds++;
        heat = Math.clamp(heat, 0, 3);
        double target = heat == 0 ? 20 : 70 + heat * 65;
        temperature += (target - temperature) * (lid ? .09 : .14);
        if (water > 0 && method != Method.BAKE && method != Method.FRY && method != Method.STIR_FRY) {
            temperature = Math.min(temperature, 100);
            if (temperature > 90) water = Math.max(0, water - (lid ? .004 : .012));
        }
        if (ingredients.isEmpty()) return;
        boolean wetMethod = method == Method.BOIL || method == Method.STEAM || method == Method.STEW;
        for (Portion p : ingredients) {
            double cutFactor = switch (p.cut) {
                case WHOLE -> .75;
                case DICE -> 1.0;
                case SLICE -> 1.2;
                case SHRED -> 1.4;
                case MINCE -> 1.5;
            };
            if (method == Method.MIX) continue;
            if ((p.needsWater || wetMethod) && water <= 0) continue;
            if (method == Method.FERMENT) continue;
            double effective = Math.max(0, temperature - 40);
            p.cooked += effective * cutFactor / (2800 * p.resistance);
            if (temperature > 165 && (!wetMethod || water <= 0)) {
                boolean uneven = seconds - lastStir > 12 || oil == 0;
                p.charred += (temperature - 165) / (uneven ? 4200 : 18000);
            }
        }
    }

    public boolean ready() {
        return !ingredients.isEmpty() && ingredients.stream().allMatch(p -> p.cooked >= 1);
    }
    public boolean burnt() {
        return ingredients.stream().anyMatch(p -> p.charred >= .75);
    }
    public String status() {
        if (ingredients.isEmpty()) return "empty";
        if (burnt()) return "burnt";
        return ready() ? "cooked" : "raw";
    }
    public int quality() {
        if (!ready() || burnt()) return 0;
        double saltRatio = (double) salt / Math.max(1, ingredients.size());
        boolean oversalted = saltRatio > .8 || soy > ingredients.size();
        if (oversalted || ingredients.stream().anyMatch(p -> p.cooked > 3.2)) return 1;
        boolean needsStir = method == Method.STIR_FRY || method == Method.FRY;
        if ((!needsStir || stirs >= 3) && saltRatio >= .1 && saltRatio <= .5) return 3;
        return 2;
    }
    public long count(String id) { return ingredients.stream().filter(p -> p.id.equals(id)).count(); }

    /** Freeze the yield on first serving, preventing refill-and-serve duplication. */
    public boolean serve(String dish, int servings) {
        if (!ready() && !burnt()) return false;
        if (remainingServings < 0) {
            remainingServings = Math.max(1, servings);
            lockedDish = dish;
        }
        if (remainingServings == 0) return false;
        remainingServings--;
        return true;
    }

    public void normalize() {
        if (ingredients == null) ingredients = new ArrayList<>();
        if (method == null) method = Method.STIR_FRY;
        if (lockedDish == null) lockedDish = "";
        if(seasoningProgress==null)seasoningProgress=new HashMap<>();
        if(seasoningCounts==null)seasoningCounts=new HashMap<>();
        ingredients.removeIf(p -> p == null || p.id == null || p.id.isBlank());
        ingredients = new ArrayList<>(ingredients.subList(0, Math.min(12, ingredients.size())));
        for (Portion p : ingredients) {
            if (p.cut == null) p.cut = Cut.WHOLE;
            if(p.priorCounts==null)p.priorCounts=new HashMap<>();
            if(p.priorCooked==null)p.priorCooked=new HashMap<>();
            p.resistance = Math.clamp(Double.isFinite(p.resistance) ? p.resistance : 1, .25, 5);
            p.cooked = Math.clamp(Double.isFinite(p.cooked) ? p.cooked : 0, 0, 100);
            p.charred = Math.clamp(Double.isFinite(p.charred) ? p.charred : 0, 0, 100);
        }
        temperature = Math.clamp(Double.isFinite(temperature) ? temperature : 20, 0, 500);
        water = Math.clamp(Double.isFinite(water) ? water : 0, 0, 12);
        oil = Math.clamp(oil, 0, 16);
        salt = Math.clamp(salt, 0, 32);
        sugar = Math.clamp(sugar, 0, 32);
        soy = Math.clamp(soy, 0, 32);
        vinegar = Math.clamp(vinegar, 0, 32);
        spice = Math.clamp(spice, 0, 32);
        capacity = Math.clamp(capacity, 1, 12);
        remainingServings = Math.clamp(remainingServings, -1, 24);
    }
}
