package dev.smokekitchen.world;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import dev.smokekitchen.SmokeKitchen;
import dev.smokekitchen.content.ContentCatalog;
import dev.smokekitchen.content.KitchenContent;
import dev.smokekitchen.content.KitchenFoodItem;
import dev.smokekitchen.content.ResourceStates;
import dev.smokekitchen.cooking.CookingBatch;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.*;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.BlockHitResult;
import java.util.*;

public final class KitchenBlockEntity extends BlockEntity implements Container {
    private static final Gson GSON = new Gson();
    public final NonNullList<ItemStack> items = NonNullList.withSize(27,ItemStack.EMPTY);
    public CookingBatch batch = new CookingBatch();
    public int heat;
    public int fuelSeconds;
    public int work;
    public int cutProgress;
    public CookingBatch.Cut cut = CookingBatch.Cut.DICE;
    public int methodIndex;
    public long lastAction = -100;
    public long lastProcessTime;
    public String attachedLid="";
    public boolean saltwater;
    public int coldTicks;
    public long lastCoolingTime=Long.MIN_VALUE;
    private boolean settlingFreshness;
    public boolean lastRedstone;
    public String mountedCookware="";
    public boolean gasValveInitialized;
    public String lastTool="";
    public long lastFlipTime=-100;
    public long lastStirTime=-100;
    private int tickCounter;

    public KitchenBlockEntity(BlockPos pos, BlockState state) { super(KitchenContent.STATION_ENTITY,pos,state); if(state.getBlock() instanceof KitchenBlock b) { if(b.kind.equals("cook")) batch.method=methods()[0]; if(b.kind.equals("gas_deposit"))fuelSeconds=2400; } }
    public KitchenBlock station() { return (KitchenBlock)getBlockState().getBlock(); }
    public String kind() { return station().kind; }
    public String cookingId() { return mountedCookware.isEmpty()?station().id:mountedCookware; }
    private void say(Player p,String key,Object...args) { p.sendOverlayMessage(Component.translatable("message.smoke_kitchen."+key,args)); }
    private void give(Player p,ItemStack stack) { if (!p.getInventory().add(stack)) p.drop(stack,false); }
    private void take(Player p,ItemStack stack) { if (!p.isCreative()) stack.shrink(1); }
    private void sound() { level.playSound(null,worldPosition,SoundEvents.WOOD_PLACE,SoundSource.BLOCKS,.5f,1.1f); }

    public InteractionResult interact(Player p, ItemStack stack, BlockHitResult hit) {
        if (level == null || level.isClientSide()) return InteractionResult.SUCCESS;
        if (!p.mayBuild()) return InteractionResult.PASS;
        long now = level.getGameTime();
        if (now-lastAction < 4) return InteractionResult.SUCCESS;
        lastAction = now;
        settleFreshness();
        if(station().id.equals("spice_rack"))return rack(p,stack,hit);
        if(station().id.equals("spice_trellis")&&stack.isEmpty()){
            for(int i=items.size()-1;i>=0;i--)if(!items.get(i).isEmpty()){give(p,items.get(i));items.set(i,ItemStack.EMPTY);changed();return InteractionResult.SUCCESS;}
            p.sendOverlayMessage(net.minecraft.network.chat.Component.literal("种植架：前方耕地种胡椒或花椒；胡椒需要架子支撑和湿润耕地。"));return InteractionResult.SUCCESS;
        }
        if(ResourceStates.spent(stack)&&!kind().equals("compost")){say(p,"spent");return InteractionResult.SUCCESS;}
        String held = stack.isEmpty() ? "" : ResourceStates.baseId(stack);
        if (held.equals("recipe_book")) { RecipeGuide.show(p); return InteractionResult.SUCCESS; }
        if (held.equals("thermometer")) { showStatus(p); return InteractionResult.SUCCESS; }
        if(dev.smokekitchen.KitchenConfig.INSTANCE.burns&&stack.isEmpty()&&hit.getDirection()==Direction.UP&&!isOven()&&(isBurner()||kind().equals("cook"))&&batch.temperature>=80) {
            p.hurtServer((net.minecraft.server.level.ServerLevel)level,level.damageSources().hotFloor(),1);say(p,"hot_hands");return InteractionResult.SUCCESS;
        }
        if(isOven()&&stack.getItem() instanceof BlockItem b&&b.getBlock() instanceof KitchenBlock v&&v.id.equals("baking_tray")) {
            if(!mountedCookware.isEmpty()||!batch.ingredients.isEmpty()){say(p,"full");return InteractionResult.SUCCESS;}
            mountCookware(stack);take(p,stack);changed();say(p,"tray_mounted");return InteractionResult.SUCCESS;
        }
        if(isOven()&&held.equals("heat_cloth")&&p.isShiftKeyDown()&&!mountedCookware.isEmpty()){unmountCookware(p);return InteractionResult.SUCCESS;}
        if(isBurner()) {
            if(stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof KitchenBlock vessel && vessel.kind.equals("cook") && !vessel.id.equals("oven")) {
                if(!mountedCookware.isEmpty() || !batch.ingredients.isEmpty()){say(p,"full");return InteractionResult.SUCCESS;}
                mountCookware(stack);take(p,stack);changed();say(p,"mounted");return InteractionResult.SUCCESS;
            }
            if(held.equals("heat_cloth")&&p.isShiftKeyDown()&&!mountedCookware.isEmpty()){unmountCookware(p);return InteractionResult.SUCCESS;}
            boolean fuel=stack.is(Items.COAL)||stack.is(Items.CHARCOAL)||stack.is(Items.COAL_BLOCK)||stack.is(Items.STICK)||held.equals("rice_husk");
            boolean gasContainer=held.equals("gas_canister")||held.equals("partial_gas_canister")||held.equals("empty_gas_canister");
            if(!fuel&&!gasContainer && (!stack.isEmpty() || hit.getDirection()==Direction.UP)) {
                if(mountedCookware.isEmpty()&&stack.isEmpty()){if(batch.remainingServings>=0)say(p,"serving_started");else if(batch.ready()||p.isShiftKeyDown())retrieveCooked(p);else showStatus(p);return InteractionResult.SUCCESS;}
                if(mountedCookware.isEmpty())batch.method=CookingBatch.Method.FRY;
                return cook(p,stack,held);
            }
            return kind().equals("gas_stove")?gas(p,stack,held):stove(p,stack);
        }
        if (kind().startsWith("gas_")) return gas(p,stack,held);
        if (kind().equals("board")) return board(p,stack,held);
        if (kind().equals("cook")) return cook(p,stack,held);
        if(kind().equals("mix") && (held.equals("wooden_lid") || held.equals("rolling_pin") || held.equals("sieve") || held.equals("chopsticks") || held.contains("knife"))) {
            if(items.stream().anyMatch(x->ResourceStates.baseId(x).equals("soy_curds"))&&!held.equals("wooden_lid")){showStatus(p);return InteractionResult.SUCCESS;}
            lastTool=held;work+=5;tryProcess();wear(p,stack,Items.AIR);changed();sound();if(held.equals("sieve")&&items.stream().anyMatch(x->Set.of("soy_mash","vinegar_mash").contains(ResourceStates.baseId(x))))say(p,"filter_help");return InteractionResult.SUCCESS;
        }
        if(kind().equals("compost") && !stack.isEmpty()) {
            var definition=KitchenContent.DEFINITIONS.get(held);
            if(stack.has(DataComponents.FOOD) || held.equals("rice_husk") || held.equals("oil_cake") || definition!=null && (definition.has("ingredient") || ContentCatalog.flag(definition,"meal"))) {
                var remainder=stack.get(DataComponents.USE_REMAINDER);
                take(p,stack);if(!p.isCreative() && remainder!=null)give(p,remainder.convertInto().create());
                if(++work>=7){work=0;give(p,new ItemStack(Items.BONE_MEAL));}
                changed();sound();showStatus(p);return InteractionResult.SUCCESS;
            }
            say(p,"ingredient");return InteractionResult.SUCCESS;
        }
        if (kind().equals("plate") && stack.isEmpty() && !p.isShiftKeyDown() && !items.getFirst().isEmpty()) {
            var food = items.getFirst().get(DataComponents.FOOD);
            if (food != null && p.canEat(false)) {
                p.getFoodData().eat(food.nutrition(),food.saturation());
                items.getFirst().shrink(1); changed();
                return InteractionResult.SUCCESS;
            }
        }
        if (stack.isEmpty() && p.isShiftKeyDown()) { if(Set.of("ferment","pickle").contains(kind())&&getBlockState().getValue(KitchenBlock.LID)){say(p,"open_first");return InteractionResult.SUCCESS;}retrieve(p); return InteractionResult.SUCCESS; }
        if (stack.isEmpty()) {
            if(kind().equals("mix")&&items.stream().anyMatch(x->ResourceStates.baseId(x).equals("soy_curds"))){showStatus(p);return InteractionResult.SUCCESS;}
            if (Set.of("mill","press","mix").contains(kind())) work += 5;
            if (kind().equals("ferment") || kind().equals("pickle")) {
                level.setBlock(worldPosition,getBlockState().cycle(KitchenBlock.LID),Block.UPDATE_ALL);
                lastProcessTime=now;
            }
            tryProcess(); showStatus(p); changed();
            return InteractionResult.SUCCESS;
        }
        if(Set.of("ferment","pickle").contains(kind())&&getBlockState().getValue(KitchenBlock.LID)){say(p,"open_first");return InteractionResult.SUCCESS;}
        if(kind().equals("evaporate")&&stack.is(Items.BUCKET)&&batch.water>=6) {
            replaceContainer(p,stack,saltwater?KitchenContent.item("saltwater_bucket"):Items.WATER_BUCKET);batch.water=0;saltwater=false;work=0;lastProcessTime=now;changed();return InteractionResult.SUCCESS;
        }
        if(held.equals("water_jug")&&Set.of("mill","mix","ferment","pickle","evaporate").contains(kind())) {
            if(batch.water>=6||saltwater&&batch.water>0){say(p,"full");return InteractionResult.SUCCESS;}
            saltwater=false;batch.addWater(1);work=0;lastProcessTime=now;wear(p,stack,KitchenContent.item("empty_water_jug"));changed();return InteractionResult.SUCCESS;
        }
        if (stack.is(Items.WATER_BUCKET) || held.equals("saltwater_bucket")) {
            if(batch.water>0) { say(p,"full");return InteractionResult.SUCCESS; }
            batch.water = 6; saltwater=held.equals("saltwater_bucket");work=0;lastProcessTime=now; replaceContainer(p,stack,Items.BUCKET); changed(); return InteractionResult.SUCCESS;
        }
        if (insert(stack.copyWithCount(1))) { if(kind().equals("mix")){work=0;lastTool="";}if(Set.of("ferment","pickle").contains(kind())){work=0;lastProcessTime=now;}take(p,stack); changed(); sound(); }
        else say(p,"full");
        return InteractionResult.SUCCESS;
    }

    private InteractionResult stove(Player p,ItemStack stack) {
        if (stack.isEmpty()) { heat = (heat+1)%4; changed(); showStatus(p); return InteractionResult.SUCCESS; }
        int seconds = stack.is(Items.COAL_BLOCK) ? 1440 : stack.is(Items.COAL) || stack.is(Items.CHARCOAL) ? 160 :
                stack.is(Items.STICK) || stack.is(KitchenContent.item("rice_husk")) ? 20 : 0;
        if (seconds <= 0) { say(p,"stove_help",heat,fuelSeconds); return InteractionResult.SUCCESS; }
        if (fuelSeconds >= 3600) { say(p,"full"); return InteractionResult.SUCCESS; }
        fuelSeconds += seconds; if(heat==0)heat=2; take(p,stack); changed(); sound(); showStatus(p);
        return InteractionResult.SUCCESS;
    }

    private InteractionResult rack(Player p,ItemStack held,BlockHitResult hit){
        int slot=SpiceRack.slot(worldPosition,getBlockState(),hit);
        if(held.is(KitchenContent.item("recipe_book"))){RecipeGuide.show(p);return InteractionResult.SUCCESS;}
        if(held.isEmpty()){
            if(p.isShiftKeyDown())for(int i=items.size()-1;i>=6;i--)if(!items.get(i).isEmpty()){give(p,items.get(i));items.set(i,ItemStack.EMPTY);changed();return InteractionResult.SUCCESS;}
            if(items.get(slot).isEmpty()){say(p,"rack_help");return InteractionResult.SUCCESS;}
            give(p,items.get(slot));items.set(slot,ItemStack.EMPTY);changed();sound();return InteractionResult.SUCCESS;
        }
        if(!SpiceRack.accepts(held)){say(p,"rack_only");return InteractionResult.SUCCESS;}
        if(!items.get(slot).isEmpty()){say(p,"rack_occupied");return InteractionResult.SUCCESS;}
        items.set(slot,held.copyWithCount(1));take(p,held);changed();sound();return InteractionResult.SUCCESS;
    }

    public boolean isBurner() { return kind().equals("stove") || kind().equals("gas_stove"); }
    public boolean isOven(){return station().id.equals("oven");}
    private InteractionResult gas(Player p,ItemStack stack,String held) {
        if(kind().equals("gas_stove") && stack.isEmpty()) {
            if(p.isShiftKeyDown()&&!items.get(26).isEmpty()){give(p,items.get(26));items.set(26,ItemStack.EMPTY);heat=0;}
            else heat=(heat+1)%4;
            gasValveInitialized=true;changed();showStatus(p);return InteractionResult.SUCCESS;
        }
        if(kind().equals("gas_stove") && ResourceStates.gasUnits(stack)>0) {
            if(!items.get(26).isEmpty()){say(p,"gas_remove");return InteractionResult.SUCCESS;}
            items.set(26,stack.copyWithCount(1));take(p,stack);heat=2;gasValveInitialized=true;changed();showStatus(p);return InteractionResult.SUCCESS;
        }
        if(Set.of("gas_tank","gas_extractor").contains(kind()) && ResourceStates.gasUnits(stack)>0 && fuelSeconds<7200) {
            int moved=Math.min(7200-fuelSeconds,ResourceStates.gasUnits(stack));fuelSeconds+=moved;
            ResourceStates.replaceHeld(p,stack,ResourceStates.gasStack(ResourceStates.gasUnits(stack)-moved));changed();showStatus(p);return InteractionResult.SUCCESS;
        }
        if(Set.of("gas_tank","gas_extractor").contains(kind()) && held.equals("empty_gas_canister") && fuelSeconds>0) {
            int moved=Math.min(600,fuelSeconds);fuelSeconds-=moved;ResourceStates.replaceHeld(p,stack,ResourceStates.gasStack(moved));changed();showStatus(p);return InteractionResult.SUCCESS;
        }
        showStatus(p);return InteractionResult.SUCCESS;
    }
    /** Pull conservatively from loaded, connected pipes. Each unit leaves its source once. */
    public int pullGas(int wanted,Set<String> sourceKinds) {
        if(level==null || wanted<=0)return 0;
        var queue=new ArrayDeque<BlockPos>();var seen=new HashSet<BlockPos>();queue.add(worldPosition);seen.add(worldPosition);
        int received=0;
        while(!queue.isEmpty() && seen.size()<=64 && received<wanted) {
            var current=queue.removeFirst();
            for(var direction:Direction.values()) {
                var next=current.relative(direction);
                if(seen.contains(next) || !level.hasChunkAt(next) || seen.size()>=64)continue;
                if(!(level.getBlockEntity(next) instanceof KitchenBlockEntity node))continue;
                if(!Set.of("gas_pipe","gas_tank","gas_extractor").contains(node.kind()))continue;
                seen.add(next);
                if(sourceKinds.contains(node.kind())) {
                    int moved=Math.min(wanted-received,node.fuelSeconds);node.fuelSeconds-=moved;received+=moved;
                    if(moved>0)node.changed();
                }
                if(Set.of("gas_pipe","gas_tank","gas_extractor").contains(node.kind()))queue.addLast(next);
            }
        }
        return received;
    }
    private void tickGas() {
        if(kind().equals("gas_extractor") && fuelSeconds<7200) {
            for(var direction:Direction.values()) {
                if(!(level.getBlockEntity(worldPosition.relative(direction)) instanceof KitchenBlockEntity deposit) || !deposit.kind().equals("gas_deposit"))continue;
                int extracted=Math.min(60,Math.min(7200-fuelSeconds,deposit.fuelSeconds));
                deposit.fuelSeconds-=extracted;fuelSeconds+=extracted;
                if(extracted>0)deposit.changed();break;
            }
        } else if(kind().equals("gas_tank")) fuelSeconds+=pullGas(Math.min(60,7200-fuelSeconds),Set.of("gas_extractor"));
        else if(kind().equals("gas_stove")) {
            if(!gasValveInitialized && heat==0){int supplied=pullGas(1,Set.of("gas_tank","gas_extractor"));if(supplied>0){fuelSeconds+=supplied;heat=2;gasValveInitialized=true;}}
            int canister=ResourceStates.gasUnits(items.get(26));
            if(heat>0 && canister>0)items.set(26,ResourceStates.gasStack(Math.max(0,canister-heat)));
            else if(heat>0){fuelSeconds+=pullGas(Math.max(0,heat*10-fuelSeconds),Set.of("gas_tank","gas_extractor"));fuelSeconds=Math.max(0,fuelSeconds-heat);}
        }
        syncState();setChanged();broadcast();
    }

    private InteractionResult board(Player p,ItemStack stack,String held) {
        if(held.equals("rolling_pin")) {
            var dough=items.getFirst();
            if(!ResourceStates.baseId(dough).equals("dough")){say(p,"roll_dough");return InteractionResult.SUCCESS;}
            var tag=dough.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();tag.putBoolean("kitchen_rolled",true);
            dough.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));dough.set(DataComponents.CUSTOM_NAME,Component.translatable("message.smoke_kitchen.dough_sheet"));dough.set(DataComponents.ITEM_MODEL,SmokeKitchen.id("dough_sheet"));
            wear(p,stack,Items.AIR);changed();sound();say(p,"rolled");return InteractionResult.SUCCESS;
        }
        if (held.contains("knife")) {
            if (p.isShiftKeyDown()) { cut = CookingBatch.Cut.values()[(cut.ordinal()+1)%CookingBatch.Cut.values().length]; say(p,"cut",Component.translatable("cut.smoke_kitchen."+cut.name())); changed(); return InteractionResult.SUCCESS; }
            if (items.getFirst().isEmpty()) { say(p,"ingredient"); return InteractionResult.SUCCESS; }
            cutProgress++;
            sound();
            if (cutProgress >= 4) {
                ItemStack result = items.getFirst().copy();
                CompoundTag data = result.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
                if(ResourceStates.baseId(result).equals("dough")&&(!data.getBooleanOr("kitchen_rolled",false)||cut!=CookingBatch.Cut.SHRED)) {cutProgress=0;say(p,"roll_then_shred");changed();return InteractionResult.SUCCESS;}
                data.putString("kitchen_cut",cut.name());
                result.set(DataComponents.CUSTOM_DATA,CustomData.of(data));
                if(ResourceStates.baseId(result).equals("dough")){result.set(DataComponents.CUSTOM_NAME,Component.translatable("message.smoke_kitchen.raw_noodles"));result.set(DataComponents.ITEM_MODEL,SmokeKitchen.id("raw_noodles"));}
                items.set(0,ItemStack.EMPTY); cutProgress = 0; give(p,result); wear(p,stack,Items.AIR); say(p,"cut_done");
            }
            changed(); return InteractionResult.SUCCESS;
        }
        if (stack.isEmpty() && p.isShiftKeyDown()) { retrieve(p); return InteractionResult.SUCCESS; }
        if (stack.isEmpty()) { showStatus(p); return InteractionResult.SUCCESS; }
        if (!items.getFirst().isEmpty()) { say(p,"full"); return InteractionResult.SUCCESS; }
        if (!ingredient(stack).isPresent()) { say(p,"ingredient"); return InteractionResult.SUCCESS; }
        items.set(0,stack.copyWithCount(1)); take(p,stack); changed(); return InteractionResult.SUCCESS;
    }

    private InteractionResult cook(Player p,ItemStack stack,String held) {
        if(held.equals("heat_cloth") && p.isShiftKeyDown()) { movePot(p);return InteractionResult.SUCCESS; }
        if(station().id.equals("oven") && (stack.isEmpty() && !p.isShiftKeyDown() || stack.is(Items.COAL) || stack.is(Items.CHARCOAL) || stack.is(Items.COAL_BLOCK)))return stove(p,stack);
        if (stack.isEmpty()) {
            if(!p.isShiftKeyDown()&&!attachedLid.isEmpty()) {
                give(p,new ItemStack(KitchenContent.item(attachedLid)));attachedLid="";
                level.setBlock(worldPosition,getBlockState().setValue(KitchenBlock.LID,false),Block.UPDATE_ALL);changed();return InteractionResult.SUCCESS;
            }
            if (p.isShiftKeyDown()) {
                if (batch.remainingServings >= 0) { say(p,"serving_started"); return InteractionResult.SUCCESS; }
                retrieveCooked(p);
            } else {
                var methods = methods(); methodIndex = (methodIndex+1)%methods.length; batch.method = methods[methodIndex];
            }
            changed(); showStatus(p); return InteractionResult.SUCCESS;
        }
        if (held.contains("spatula") || held.equals("ladle") || held.equals("tongs") || held.equals("chopsticks")) {
            if((batch.method==CookingBatch.Method.FRY||batch.method==CookingBatch.Method.BAKE)&&!held.equals("ladle")){batch.turn();lastFlipTime=level.getGameTime();}else batch.stir();
            lastStirTime=level.getGameTime();
            wear(p,stack,Items.AIR); sound(); changed(); showStatus(p); return InteractionResult.SUCCESS;
        }
        if (held.equals("pot_lid") || held.equals("wooden_lid")) {
            if(!getBlockState().getValue(KitchenBlock.LID)) { attachedLid=held; take(p,stack); }
            else if(!attachedLid.isEmpty()) { give(p,new ItemStack(KitchenContent.item(attachedLid))); attachedLid=""; }
            level.setBlock(worldPosition,getBlockState().cycle(KitchenBlock.LID),Block.UPDATE_ALL); changed(); return InteractionResult.SUCCESS;
        }
        if(held.equals("empty_jar")&&batch.count("fat")>0){collectLard(p,stack);return InteractionResult.SUCCESS;}
        if (stack.is(Items.BOWL) || held.equals("ceramic_plate") || held.equals("ceramic_bowl")) {
            if(batch.count("fat")>0){say(p,"render_help");return InteractionResult.SUCCESS;}
            serve(p,stack); return InteractionResult.SUCCESS;
        }
        if (batch.remainingServings >= 0) { say(p,"serving_started"); return InteractionResult.SUCCESS; }
        if (stack.is(Items.WATER_BUCKET) || held.equals("water_jug")) {
            if(isBurner()&&mountedCookware.isEmpty()||isOven()||cookingId().equals("baking_tray")){say(p,"water_vessel");return InteractionResult.SUCCESS;}
            if (batch.water >= 6) { say(p,"full"); return InteractionResult.SUCCESS; }
            batch.addWater(held.equals("water_jug")?1:6);
            if (stack.is(Items.WATER_BUCKET)) replaceContainer(p,stack,Items.BUCKET); else wear(p,stack,KitchenContent.item("empty_water_jug"));
            changed(); return InteractionResult.SUCCESS;
        }
        var def = KitchenContent.DEFINITIONS.get(held);
        if(def!=null&&ContentCatalog.num(def,"liquid_water",0)>0&&(isBurner()&&mountedCookware.isEmpty()||isOven()||cookingId().equals("baking_tray"))){say(p,"water_vessel");return InteractionResult.SUCCESS;}
        if (def != null && def.has("seasoning")) {
            String seasoning = ContentCatalog.str(def,"seasoning");
            boolean accepted = switch (seasoning) {
                case "salt" -> { if(batch.salt>=16) yield false; batch.salt++; yield true; }
                case "oil" -> { if(batch.oil>=8) yield false; batch.oil++; yield true; }
                case "soy" -> { if(batch.soy>=16) yield false; batch.soy++; yield true; }
                case "vinegar" -> { if(batch.vinegar>=16) yield false; batch.vinegar++; yield true; }
                default -> { if(batch.spice>=16) yield false; batch.spice++; yield true; }
            };
            if (accepted) { batch.season(seasoning);batch.season(held);wear(p,stack,seasoning.equals("salt") || seasoning.equals("spice") || held.equals("lard") ? KitchenContent.item("empty_jar") : Items.GLASS_BOTTLE); changed(); }
            else say(p,"full");
            showStatus(p); return InteractionResult.SUCCESS;
        }
        if (stack.is(Items.SUGAR) || stack.is(Items.HONEY_BOTTLE)) {
            if (batch.sugar < 16) { batch.sugar++;batch.season(stack.is(Items.HONEY_BOTTLE)?"honey":"sugar"); if(stack.is(Items.HONEY_BOTTLE)) replaceContainer(p,stack,Items.GLASS_BOTTLE); else take(p,stack); changed(); }
            return InteractionResult.SUCCESS;
        }
        var spec = ingredient(stack);
        if (spec.isEmpty()) { say(p,"ingredient"); return InteractionResult.SUCCESS; }
        if(isOven()&&mountedCookware.isEmpty()){say(p,"tray_needed");return InteractionResult.SUCCESS;}
        var s = spec.get();
        var data = stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
        CookingBatch.Cut chosen = CookingBatch.Cut.WHOLE;
        try { chosen = CookingBatch.Cut.valueOf(data.getStringOr("kitchen_cut","WHOLE")); } catch (IllegalArgumentException ignored) {}
        boolean safeRaw=Set.of("cucumber","garlic","tomato","cabbage","scallion","onion").contains(s.id);
        if (batch.add(s.id,chosen,s.resistance,s.water,s.ready || batch.method==CookingBatch.Method.MIX && safeRaw)) {
            int index = batch.ingredients.size()-1;
            batch.ingredients.get(index).rolled=data.getBooleanOr("kitchen_rolled",false);
            if (data.contains("kitchen_portion")) {
                try { var saved = GSON.fromJson(data.getStringOr("kitchen_portion",""),CookingBatch.Portion.class);
                    if (saved != null) {
                        batch.restorePortion(index,saved);
                    }
                } catch (RuntimeException ignored) {}
                batch.normalize();
            }
            if(data.contains("kitchen_quality")){
                int grade=data.getIntOr("kitchen_quality",3);String reasons=data.getStringOr("kitchen_quality_reasons","");
                if(reasons.contains("stale")||grade<=1&&reasons.contains("strong")){var portion=batch.ingredients.get(index);portion.qualityLimit=grade;portion.inheritedReasons=new ArrayList<>(List.of(reasons.contains("stale")?"stale":"strong"));}
            }
            var portion=batch.ingredients.get(index);
            if(portion.cooked>=1&&portion.preparedAt==null)portion.preparedAt=data.getLongOr("kitchen_prepared",level.getGameTime());
            var stored=stack.is(Items.MILK_BUCKET)?new ItemStack(KitchenContent.item("milk_portion")):stack.copyWithCount(1);
            var remainder=stack.get(DataComponents.USE_REMAINDER);
            if(remainder!=null)stored.remove(DataComponents.USE_REMAINDER);
            items.set(index,stored);
            if(def!=null)batch.addWater(Math.clamp(data.getDoubleOr("kitchen_liquid_water",ContentCatalog.num(def,"liquid_water",0)),0,ContentCatalog.num(def,"liquid_water",0)));
            if(stack.is(Items.MILK_BUCKET)) replaceContainer(p,stack,Items.BUCKET);
            else {take(p,stack);if(!p.isCreative()&&remainder!=null)give(p,remainder.convertInto().create());}
            changed(); sound();showRecipeHint(p);
        } else say(p,"full");
        return InteractionResult.SUCCESS;
    }

    /** Rendering consumes the actual fat, never the burner fuel slot or a second container. */
    private void collectLard(Player player,ItemStack jar) {
        if(batch.remainingServings>=0||batch.ingredients.isEmpty()||batch.count("fat")!=batch.ingredients.size()||batch.water>0||batch.oil>0||batch.salt+batch.soy+batch.vinegar+batch.spice+batch.sugar>0||!Set.of("frying_pan","wok","clay_pot","soup_pot").contains(cookingId())){say(player,"render_help");return;}
        if(batch.ingredients.stream().anyMatch(p->p.charred>=.25||p.maximumTemperature>165)){say(player,"render_burnt");return;}
        if(!batch.ready()){say(player,"render_wait",batch.progress(1));return;}
        int count=Math.min(2,batch.ingredients.size());
        for(int n=0;n<count;n++){int i=batch.ingredients.size()-1;batch.ingredients.remove(i);items.set(i,ItemStack.EMPTY);}
        var oil=new ItemStack(KitchenContent.item("lard"));oil.setDamageValue(8-count*4);
        if(count==1)oil=ResourceStates.usedStack(oil,KitchenContent.item("empty_jar"));
        take(player,jar);give(player,oil);give(player,new ItemStack(KitchenContent.item("fat_cracklings"),count));
        batch.refreshTemperatureHistory();changed();sound();say(player,"render_done",count*4,count);
    }

    public record Ingredient(String id,double resistance,boolean water,boolean ready) {}
    public static Optional<Ingredient> ingredient(ItemStack stack) {
        if(ResourceStates.spent(stack))return Optional.empty();
        String n = ResourceStates.baseId(stack);
        var d = KitchenContent.DEFINITIONS.get(n);
        if (d != null && d.has("ingredient")) return Optional.of(new Ingredient(ContentCatalog.str(d,"ingredient"),
                d.has("resistance") ? d.get("resistance").getAsDouble() : 1,ContentCatalog.flag(d,"needs_water"),ContentCatalog.flag(d,"ready")));
        String canonical = switch (n) {
            case "egg" -> "egg";
            case "potato" -> "potato";
            case "carrot" -> "carrot";
            case "beef" -> "beef";
            case "porkchop" -> "pork";
            case "chicken","cooked_chicken" -> "chicken";
            case "mutton","rabbit" -> "pork";
            case "cod","salmon","tropical_fish","cooked_cod","cooked_salmon" -> "fish";
            case "cooked_beef" -> "beef";
            case "cooked_porkchop","cooked_mutton","cooked_rabbit" -> "pork";
            case "baked_potato" -> "potato";
            case "brown_mushroom","red_mushroom" -> "mushroom";
            case "pumpkin" -> "pumpkin";
            case "milk_bucket" -> "milk";
            case "apple" -> "apple";
            case "dried_kelp" -> "kelp";
            default -> "";
        };
        return canonical.isEmpty() ? Optional.empty() : Optional.of(new Ingredient(canonical,
                Set.of("beef","pork","chicken","fish").contains(canonical)?1.6:1,false,n.startsWith("cooked_")||n.equals("baked_potato")||canonical.equals("apple") || canonical.equals("milk") || canonical.equals("kelp")));
    }

    private CookingBatch.Method[] methods() {
        return switch (cookingId()) {
            case "wok" -> new CookingBatch.Method[]{CookingBatch.Method.STIR_FRY};
            case "frying_pan" -> new CookingBatch.Method[]{CookingBatch.Method.FRY};
            case "steamer" -> new CookingBatch.Method[]{CookingBatch.Method.STEAM};
            case "baking_tray" -> new CookingBatch.Method[]{CookingBatch.Method.BAKE};
            case "oven" -> new CookingBatch.Method[]{CookingBatch.Method.BAKE};
            case "mixing_bowl" -> new CookingBatch.Method[]{CookingBatch.Method.MIX};
            default -> new CookingBatch.Method[]{CookingBatch.Method.BOIL,CookingBatch.Method.STEW};
        };
    }
    private void serve(Player p,ItemStack container) {
        settleFreshness();
        if (!batch.ready() && !batch.burnt()) { say(p,"not_ready"); return; }
        if(!batch.burnt()&&batch.remainingServings<0) {
            var expected=closestRecipe();
            if(expected!=null&&expected.distance(batch)==0&&expected.methods().contains(batch.method)) {
                var problem=expected.problem(batch);
                if(problem.isPresent()&&Set.of("soften","turn","cover","low_heat","oven","roast","stir","ferment").contains(problem.get().key())) {
                    p.sendOverlayMessage(recipeProblem(problem.get()));return;
                }
            }
        }
        String dish = batch.burnt() ? "burnt_food" : Set.of(CookingBatch.Method.BOIL,CookingBatch.Method.STEW,CookingBatch.Method.STEAM).contains(batch.method)?"mixed_stew":"mixed_stir_fry";
        boolean named=false;
        int servings = Math.max(1,batch.ingredients.size()/2);
        for (var r : ContentCatalog.DISHES) if (r.matches(batch)) { dish=r.id(); servings=r.servings();named=true; break; }
        if (batch.burnt()) dish="burnt_food";
        if (batch.remainingServings >= 0 && !batch.burnt()) dish=batch.lockedDish;
        int quality = batch.quality();
        if(!named&&!batch.burnt()&&batch.remainingServings<0) {
            var closest=closestRecipe();
            var fallback=new ItemStack(KitchenContent.item(dish)).getHoverName();
            if(closest==null)p.sendSystemMessage(Component.translatable("message.smoke_kitchen.recipe_generic",fallback));
            else closest.problem(batch).ifPresent(problem->p.sendSystemMessage(Component.translatable("message.smoke_kitchen.recipe_fallback",fallback,new ItemStack(KitchenContent.item(closest.id())).getHoverName(),recipeProblem(problem))));
        }
        if (!batch.serve(dish,servings)) { say(p,"empty"); return; }
        var result = new ItemStack(KitchenContent.item(dish));
        result.set(DataComponents.USE_REMAINDER,new net.minecraft.world.item.component.UseRemainder(net.minecraft.world.item.ItemStackTemplate.fromNonEmptyStack(container.copyWithCount(1))));
        KitchenFoodItem.stampMeal(result,quality,batch.qualityReasons(),preparedTime());
        result=KitchenFoodItem.agedStack(result,level.getGameTime(),batchFreshness());
        take(p,container); give(p,result);
        if (batch.remainingServings == 0) {
            for(int i=0;i<(isBurner()?26:items.size());i++)items.set(i,ItemStack.EMPTY);
            double water=batch.method==CookingBatch.Method.STEAM?batch.water:0,temperature=batch.temperature;
            batch=new CookingBatch();batch.method=methods()[0];batch.water=water;batch.temperature=temperature;
        }
        changed(); sound();
    }
    private dev.smokekitchen.cooking.DishRecipe closestRecipe() {
        var exact=ContentCatalog.DISHES.stream().filter(r->r.distance(batch)==0).min(Comparator.comparingInt(r->r.methods().contains(batch.method)?0:1));
        if(exact.isPresent())return exact.get();
        return ContentCatalog.DISHES.stream().filter(r->r.methods().contains(batch.method)&&r.ingredients().keySet().stream().anyMatch(id->batch.count(id)>0)).min(Comparator.comparingInt(r->r.distance(batch))).filter(r->r.distance(batch)<=3).orElse(null);
    }
    private Component recipeProblem(dev.smokekitchen.cooking.DishRecipe.Problem problem) {
        Component ingredient=Component.translatable("ingredient.smoke_kitchen."+problem.ingredient());
        Component related=problem.key().equals("cut")?Component.translatable("cut.smoke_kitchen."+problem.related()):Component.translatable("ingredient.smoke_kitchen."+problem.related());
        if(Set.of("preheat","missing","boiling","finish").contains(problem.key()))return Component.translatable("recipe_problem.smoke_kitchen."+problem.key(),ingredient,problem.related());
        if(Set.of("stir","water","cover","low_heat","oven","cold_mix","roast","ferment","cold").contains(problem.key()))return Component.translatable("recipe_problem.smoke_kitchen."+problem.key(),problem.related());
        if(Set.of("water_ratio","soften","marinate").contains(problem.key()))return Component.translatable("recipe_problem.smoke_kitchen."+problem.key(),ingredient,problem.related());
        return Component.translatable("recipe_problem.smoke_kitchen."+problem.key(),ingredient,related);
    }
    private void showRecipeHint(Player player) {
        if(!dev.smokekitchen.KitchenConfig.INSTANCE.detailedHints)return;
        if(batch.renderingFat()){showStatus(player);return;}
        var closest=closestRecipe();if(closest==null)return;
        closest.problem(batch).ifPresent(problem->player.sendOverlayMessage(Component.translatable("message.smoke_kitchen.recipe_hint",new ItemStack(KitchenContent.item(closest.id())).getHoverName(),recipeProblem(problem))));
    }
    private void movePot(Player p) {
        var result=new ItemStack(getBlockState().getBlock());
        result.set(DataComponents.BLOCK_ENTITY_DATA,net.minecraft.world.item.component.TypedEntityData.of(KitchenContent.STATION_ENTITY,saveWithFullMetadata(level.registryAccess())));
        items.clear();batch=new CookingBatch();attachedLid="";
        level.removeBlock(worldPosition,false);give(p,result);
    }
    private void unmountCookware(Player player) {
        var vessel=(KitchenBlock)KitchenContent.BLOCKS.get(mountedCookware);
        var saved=new KitchenBlockEntity(worldPosition,vessel.defaultBlockState().setValue(KitchenBlock.LID,getBlockState().getValue(KitchenBlock.LID)));
        saved.batch=batch;saved.methodIndex=methodIndex;saved.attachedLid=attachedLid;saved.lastFlipTime=lastFlipTime;saved.lastStirTime=lastStirTime;for(int i=0;i<26;i++){saved.items.set(i,items.get(i));items.set(i,ItemStack.EMPTY);}
        var result=new ItemStack(vessel);result.set(DataComponents.BLOCK_ENTITY_DATA,net.minecraft.world.item.component.TypedEntityData.of(KitchenContent.STATION_ENTITY,saved.saveWithFullMetadata(level.registryAccess())));
        give(player,result);mountedCookware="";batch=new CookingBatch();attachedLid="";level.setBlock(worldPosition,getBlockState().setValue(KitchenBlock.LID,false),Block.UPDATE_ALL);changed();
    }
    private void retrieveCooked(Player player) {
        for(int i=batch.ingredients.size()-1;i>=0;i--)if(!items.get(i).isEmpty()) {
            var result=displayedFood(i);stampLoose(result,batch.portionBatch(i));var portion=batch.removePortion(i);
            if(ResourceStates.baseId(result).equals("soy_milk"))batch.water=Math.max(0,batch.water-result.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getDoubleOr("kitchen_liquid_water",0));
            result=KitchenFoodItem.agedStack(result,level.getGameTime(),portion.freshnessFloor);
            applyPortion(result,portion);give(player,result);items.set(i,ItemStack.EMPTY);batch.refreshTemperatureHistory();changed();return;
        }
        say(player,"empty");
    }
    private void stampLoose(ItemStack result,CookingBatch individual){
        if(!result.has(DataComponents.FOOD))return;
        var data=result.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
        KitchenFoodItem.stampMeal(result,individual.quality(),individual.qualityReasons(),individual.ingredients.getFirst().preparedAt!=null?individual.ingredients.getFirst().preparedAt:data.getLongOr("kitchen_prepared",level.getGameTime()));
    }
    public ItemStack displayedFood(int slot) {
        var input=items.get(slot);
        if(slot<batch.ingredients.size()){
            var portion=batch.ingredients.get(slot);var result=ResourceStates.cookedStack(input,portion.cooked>=1,portion.charred>=.75,level);
            if(ResourceStates.baseId(result).equals("soy_milk")){var data=result.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();data.putDouble("kitchen_liquid_water",Math.min(1,batch.water/Math.max(1,batch.count("soy_milk"))));result.set(DataComponents.CUSTOM_DATA,CustomData.of(data));}
            if(portion.id.equals("soy_milk")&&ResourceStates.baseId(result).equals("soy_curds")){var data=result.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();data.putInt("kitchen_tofu_acid",(int)Math.ceil(batch.vinegar));result.set(DataComponents.CUSTOM_DATA,CustomData.of(data));}
            if(portion.preparedAt!=null){var tag=result.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();tag.putLong("kitchen_prepared",portion.preparedAt);result.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));}
            return KitchenFoodItem.agedStack(result,level==null?0:level.getGameTime(),portion.freshnessFloor);
        }
        return input.copy();
    }

    private void replaceContainer(Player p,ItemStack stack,Item replacement) {
        if (p.isCreative()) return;
        stack.shrink(1); give(p,new ItemStack(replacement));
    }
    private void wear(Player p,ItemStack stack,Item replacement) {
        if (!stack.isDamageableItem()) return;
        stack.setDamageValue(stack.getDamageValue()+1);
        ResourceStates.replaceHeld(p,stack,ResourceStates.usedStack(stack,replacement));
    }
    private boolean insert(ItemStack stack) {
        if(level!=null)stack=KitchenFoodItem.agedStack(stack,level.getGameTime());
        for (int i=0;i<items.size();i++) {
            if (ItemStack.isSameItemSameComponents(items.get(i),stack) && items.get(i).getCount()<items.get(i).getMaxStackSize()) { items.get(i).grow(1); return true; }
        }
        for(int i=0;i<items.size();i++)if(items.get(i).isEmpty()){items.set(i,stack);return true;}
        return false;
    }
    private void retrieve(Player p) {
        for (int i=items.size()-1;i>=0;i--) if (!items.get(i).isEmpty()) {
            give(p,items.get(i)); items.set(i,ItemStack.EMPTY); changed(); return;
        }
        say(p,"empty");
    }
    public void showStatus(Player p) {
        if((kind().equals("cook")||isBurner())&&batchFreshness()>0){say(p,batchFreshness()>=2?"meal_spoiled":"meal_stale");return;}
        if(kind().equals("ferment")&&items.stream().anyMatch(x->ResourceStates.baseId(x).equals("soybean"))&&items.stream().filter(x->!x.isEmpty()).allMatch(x->ResourceStates.baseId(x).equals("soybean"))){say(p,"soy_soak",Math.min(100,100*work/120),work);return;}
        if(kind().equals("mix")&&items.stream().anyMatch(x->ResourceStates.baseId(x).equals("soy_curds"))){say(p,"tofu_press",Math.min(100,100*work/20));return;}
        if(kind().equals("mix")&&items.stream().anyMatch(x->Set.of("raw_bean_pulp","boiled_bean_pulp").contains(ResourceStates.baseId(x)))){say(p,"tofu_filter");return;}
        if(batch.count("soy_milk")>0){say(p,"tofu_milk",batch.progress(1),batch.displayedTemperature(),String.format(Locale.ROOT,"%.1f",batch.water),batch.vinegar);return;}
        if(kind().equals("ferment")){
            if(items.stream().anyMatch(x->Set.of("soy_mash","vinegar_mash").contains(ResourceStates.baseId(x)))){say(p,"ferment_ready");return;}
            if(items.stream().anyMatch(x->ResourceStates.baseId(x).equals("soybean"))){say(p,"ferment_raw_soy");return;}
            if(getBlockState().getValue(KitchenBlock.LID)&&!items.stream().allMatch(ItemStack::isEmpty)){
                for(var entry:ContentCatalog.DATA.getAsJsonArray("processes")){
                    var recipe=entry.getAsJsonObject();if(!ContentCatalog.str(recipe,"station").equals("ferment")||batch.water<ContentCatalog.num(recipe,"water",0))continue;
                    boolean complete=true;for(var input:recipe.getAsJsonObject("inputs").entrySet())if(items.stream().filter(x->processInputMatches(x,input.getKey(),recipe)).mapToInt(this::units).sum()<input.getValue().getAsInt())complete=false;
                    if(complete){int seconds=ContentCatalog.num(recipe,"seconds",5);say(p,"ferment_progress",Math.min(100,100*work/seconds),work,seconds);return;}
                }
                if(items.stream().anyMatch(x->Set.of("starter","vinegar_starter","fermented_dough").contains(ResourceStates.baseId(x))))say(p,"ferment_check");else say(p,"ferment_missing");return;
            }
        }
        if(kind().equals("mix")&&items.stream().anyMatch(x->Set.of("soy_mash","vinegar_mash").contains(ResourceStates.baseId(x)))){say(p,"filter_help");return;}
        if(batch.renderingFat()){say(p,"render_status",batch.displayedTemperature(),batch.progress(1),batch.ingredients.size());return;}
        var expected=closestRecipe();double target=expected!=null&&expected.distance(batch)==0&&expected.methods().contains(batch.method)?Math.max(1,expected.staple().cooked()):1;
        if(isBurner()) {
            int progress=batch.progress(target);
            var water=Set.of(CookingBatch.Method.BOIL,CookingBatch.Method.STEW,CookingBatch.Method.STEAM).contains(batch.method)?Component.translatable("message.smoke_kitchen.water_status",String.format(Locale.ROOT,"%.1f",batch.water),Component.translatable("water.smoke_kitchen."+batch.waterStatus())):Component.empty();
            var flame=Component.translatable(getFuelUnits()>0&&heat>0?"state.smoke_kitchen.heating":"state.smoke_kitchen.no_heat").append(" / ").append(Component.translatable("heat.smoke_kitchen."+heat));
            if(target>1)flame.append(" / ").append(Component.translatable("message.smoke_kitchen.softening"));
            if(batch.ingredients.isEmpty()&&batch.sugar>0&&batch.oil>0)flame.append(" / ").append(Component.translatable("message.smoke_kitchen.caramel_progress",Math.min(3,batch.caramelSeconds),batch.burntSugarSeconds>=5?"×":"✓"));
            say(p,"burner_status",flame,batch.displayedTemperature(),progress,getFuelUnits(),water);
        }
        else if (kind().equals("cook")) say(p,"cook_status",Math.round(batch.temperature),batch.ingredients.size(),batch.water > 0 ? String.format(java.util.Locale.ROOT,"%.1f",batch.water) : "0",batch.salt,batch.oil,
                target>1?Component.translatable("message.smoke_kitchen.softening_progress",batch.progress(target)):Component.translatable("state.smoke_kitchen."+batch.status()));
        else if(kind().startsWith("gas_")) say(p,"gas_status",fuelSeconds,heat);
        else if(kind().equals("stove")) say(p,"stove_status",heat,fuelSeconds/Math.max(1,heat));
        else if(kind().equals("board")) say(p,"board_status",Component.translatable("cut.smoke_kitchen."+cut.name()),cutProgress);
        else say(p,"station_status",items.stream().filter(s->!s.isEmpty()).mapToInt(ItemStack::getCount).sum(),work,getBlockState().getValue(KitchenBlock.LID)?"✓":"×");
    }
    public void serverTick() {
        if (++tickCounter % 20 != 0) return;
        settleFreshness();
        if(isBurner())migrateLegacyCookware();
        if(kind().startsWith("gas_")) { tickGas();if(isBurner())tickCooking(getFuelUnits()>0?heat:0);showTargetedStatus();return; }
        if (kind().equals("stove")) {
            boolean powered=level.hasNeighborSignal(worldPosition);
            if(powered!=lastRedstone){heat=powered?2:0;lastRedstone=powered;}
            if (fuelSeconds>0 && heat>0) fuelSeconds=Math.max(0,fuelSeconds-heat);
            tickCooking(fuelSeconds>0?heat:0);showTargetedStatus();return;
        }
        if (kind().equals("cook")) {
            int sourceHeat=0;
            sourceHeat=heatBelow();
            if(station().id.equals("oven") && fuelSeconds>0){sourceHeat=heat;fuelSeconds=Math.max(0,fuelSeconds-heat);}
            tickCooking(sourceHeat);
            showTargetedStatus();
        } else if (Set.of("ferment","pickle","dry","evaporate").contains(kind())) {
            long now=level.getGameTime();
            boolean allowed=kind().equals("dry")?!level.isRainingAt(worldPosition.above()):kind().equals("evaporate")?
                    heatBelow()>0:getBlockState().getValue(KitchenBlock.LID);
            if(lastProcessTime>0 && allowed) work+=(int)Math.min(72000,Math.max(1,(now-lastProcessTime)/20));
            lastProcessTime=now; tryProcess(); setChanged();
        }
        if(Set.of("board","ferment","mix").contains(kind()))showTargetedStatus();
        if(tickCounter%40==0) broadcast();
    }
    public int getFuelUnits(){return fuelSeconds+(kind().equals("gas_stove")?ResourceStates.gasUnits(items.get(26)):0);}
    private int heatBelow() {
        if(level.getBlockEntity(worldPosition.below()) instanceof KitchenBlockEntity stove&&stove.isBurner())return stove.getFuelUnits()>0?stove.heat:0;
        var below=level.getBlockState(worldPosition.below());
        return (below.is(net.minecraft.world.level.block.Blocks.CAMPFIRE)||below.is(net.minecraft.world.level.block.Blocks.SOUL_CAMPFIRE)||below.is(net.minecraft.world.level.block.Blocks.FURNACE)||below.is(net.minecraft.world.level.block.Blocks.SMOKER)||below.is(net.minecraft.world.level.block.Blocks.BLAST_FURNACE))&&below.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT)?2:0;
    }
    private void tickCooking(int sourceHeat) {
        batch.tick(sourceHeat,getBlockState().getValue(KitchenBlock.LID));
        settleFreshness();
        if(isOven()&&mountedCookware.equals("baking_tray")&&sourceHeat>0&&batch.displayedTemperature()>=140&&!batch.ingredients.isEmpty())batch.ovenSeconds++;
        if(batch.temperature>90&&(!batch.ingredients.isEmpty()||batch.water>0) && level instanceof net.minecraft.server.level.ServerLevel server) {
            boolean wet=batch.water>0;server.sendParticles(wet?net.minecraft.core.particles.ParticleTypes.WHITE_SMOKE:net.minecraft.core.particles.ParticleTypes.SMOKE,
                worldPosition.getX()+.5,worldPosition.getY()+(isBurner()?1.05:.4),worldPosition.getZ()+.5,1,.07,.015,.07,.005);
            if(wet&&batch.waterStatus().equals("boiling"))server.sendParticles(net.minecraft.core.particles.ParticleTypes.BUBBLE_POP,worldPosition.getX()+.5,worldPosition.getY()+(isBurner()?1.06:.28),worldPosition.getZ()+.5,2,.12,.01,.12,0);
        }
        syncState();setChanged();broadcast();
    }
    private void showTargetedStatus() {
        if(!dev.smokekitchen.KitchenConfig.INSTANCE.detailedHints)return;
        for(var player:level.players())if(player.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(worldPosition))<36 && player.pick(5,0,false) instanceof BlockHitResult hit && hit.getBlockPos().equals(worldPosition)) {
            if(player.isShiftKeyDown()&&!items.getFirst().isEmpty()) {
                var contents=Component.translatable("message.smoke_kitchen.contents");int shown=0;
                if(batch.remainingServings>=0){contents.append(KitchenFoodItem.agedStack(new ItemStack(KitchenContent.item(batch.lockedDish)),level.getGameTime(),batchFreshness()).getHoverName()).append(" ×"+batch.remainingServings);player.sendOverlayMessage(contents);continue;}
                for(int i=0;i<26&&shown<4;i++)if(!items.get(i).isEmpty()){if(shown++>0)contents.append("，");contents.append(displayedFood(i).getHoverName()).append(" ×"+items.get(i).getCount());}
                player.sendOverlayMessage(contents);continue;
            }
            if(batch.method==CookingBatch.Method.STEAM&&!batch.ingredients.isEmpty()&&!getBlockState().getValue(KitchenBlock.LID))say(player,"steam_lid");else showStatus(player);
        }
    }
    private void copyCookingFrom(KitchenBlockEntity source) {
        mountedCookware=source.station().id;batch=source.batch;methodIndex=source.methodIndex;attachedLid=source.attachedLid;lastFlipTime=source.lastFlipTime;lastStirTime=source.lastStirTime;
        for(int i=0;i<26;i++){items.set(i,source.items.get(i));source.items.set(i,ItemStack.EMPTY);}
        source.batch=new CookingBatch();source.attachedLid="";
        level.setBlock(worldPosition,getBlockState().setValue(KitchenBlock.LID,!attachedLid.isEmpty()),Block.UPDATE_ALL);
    }
    public void mountCookware(ItemStack vesselStack) {
        var vessel=(KitchenBlock)((BlockItem)vesselStack.getItem()).getBlock();
        var source=new KitchenBlockEntity(worldPosition,vessel.defaultBlockState());
        var data=vesselStack.get(DataComponents.BLOCK_ENTITY_DATA);
        if(data!=null)data.loadInto(source,level.registryAccess());
        copyCookingFrom(source);
    }
    private void migrateLegacyCookware() {
        if(isOven()||!mountedCookware.isEmpty()||!batch.ingredients.isEmpty())return;
        if(level.getBlockEntity(worldPosition.above()) instanceof KitchenBlockEntity vessel && vessel.kind().equals("cook")&&!vessel.station().id.equals("oven")) {
            copyCookingFrom(vessel);level.removeBlock(worldPosition.above(),false);changed();
        }
    }
    private long preparedTime(){return batch.ingredients.stream().filter(p->p.preparedAt!=null).mapToLong(p->p.preparedAt).min().orElse(level.getGameTime());}
    private int batchFreshness(){return batch.ingredients.stream().mapToInt(p->p.freshnessFloor).max().orElse(0);}
    /** Settle against world game time before any inventory mutation, including hopper access. */
    private void settleFreshness(){
        if(level==null||level.isClientSide()||settlingFreshness)return;
        settlingFreshness=true;
        try{
            long now=level.getGameTime();
            if(station().id.equals("icebox"))coolInventory();
            for(int i=0;i<items.size();i++){
                var stack=items.get(i);if(stack.isEmpty())continue;
                if(i<batch.ingredients.size()){
                    var portion=batch.ingredients.get(i);
                    var tag=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
                    if(!tag.contains("kitchen_prepared")){tag.putLong("kitchen_prepared",now);stack.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));}
                    if((portion.cooked>=1||portion.charred>=.75)&&portion.preparedAt==null)portion.preparedAt=now;
                    long prepared=portion.preparedAt==null?tag.getLongOr("kitchen_prepared",now):portion.preparedAt;
                    portion.freshnessFloor=Math.max(portion.freshnessFloor,portion.preparedAt!=null?KitchenFoodItem.elapsedStage(now,prepared):KitchenFoodItem.freshnessStage(stack,now,prepared));
                    if(portion.freshnessFloor>0){portion.qualityLimit=Math.min(1,portion.qualityLimit==null?3:portion.qualityLimit);if(!portion.inheritedReasons.contains("stale"))portion.inheritedReasons.add("stale");}
                }else items.set(i,KitchenFoodItem.agedStack(stack,now));
            }
            if(!isEmpty())setChanged();
        }finally{settlingFreshness=false;}
    }
    private void coolInventory() {
        if(level==null)return;
        long now=level.getGameTime();
        if(lastCoolingTime==Long.MIN_VALUE){lastCoolingTime=now;return;}
        long elapsed=Math.max(0,now-lastCoolingTime);lastCoolingTime=now;
        if(elapsed<=0)return;
        long capacity=coldTicks;
        while(capacity<elapsed){
            ItemStack ice=items.stream().filter(s->s.is(Items.ICE)||s.is(Items.PACKED_ICE)||s.is(Items.BLUE_ICE)).findFirst().orElse(ItemStack.EMPTY);
            if(ice.isEmpty())break;
            capacity+=ice.is(Items.BLUE_ICE)?216000:ice.is(Items.PACKED_ICE)?72000:24000;ice.shrink(1);
        }
        long cooled=Math.min(elapsed,capacity);coldTicks=(int)Math.min(216000,capacity-cooled);
        if(cooled>0)for(var stack:items) {
            if(!(stack.getItem() instanceof dev.smokekitchen.content.KitchenFoodItem))continue;
            var tag=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
            long prepared=tag.getLongOr("kitchen_prepared",now);
            tag.putLong("kitchen_prepared",Math.min(now,prepared+cooled));stack.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));
        }
        setChanged();
    }

    private void tryProcess() {
        if(Set.of("ferment","pickle").contains(kind())&&!getBlockState().getValue(KitchenBlock.LID))return;
        for (var element : ContentCatalog.DATA.getAsJsonArray("processes")) {
            var recipe=element.getAsJsonObject();
            if(!ContentCatalog.str(recipe,"station").equals(kind())) continue;
            if(recipe.has("tool")&&!ContentCatalog.str(recipe,"tool").equals(lastTool))continue;
            if(ContentCatalog.flag(recipe,"saltwater") && !saltwater) continue;
            if(work<ContentCatalog.num(recipe,"seconds",5) || batch.water<ContentCatalog.num(recipe,"water",0)) continue;
            var inputs=recipe.getAsJsonObject("inputs");
            if(ContentCatalog.flag(recipe,"exclusive")&&items.stream().filter(s->!s.isEmpty()).anyMatch(s->inputs.entrySet().stream().noneMatch(input->processInputMatches(s,input.getKey(),recipe))))continue;
            boolean matches=true;
            for(var input:inputs.entrySet()) {
                int found=items.stream().filter(s->processInputMatches(s,input.getKey(),recipe)).mapToInt(this::units).sum();
                if(found<input.getValue().getAsInt()) { matches=false; break; }
            }
            if(!matches) continue;
            boolean excessTofuAcid=recipe.getAsJsonObject("outputs").has("smoke_kitchen:tofu")&&items.stream().anyMatch(s->ResourceStates.baseId(s).equals("soy_curds")&&s.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getIntOr("kitchen_tofu_acid",1)>1);
            // Only mutate after every input and every output slot has been validated.
            var backup=NonNullList.withSize(items.size(),ItemStack.EMPTY);
            for(int i=0;i<items.size();i++) backup.set(i,items.get(i).copy());
            for(var input:inputs.entrySet()) {
                int needed=input.getValue().getAsInt();
                for(int slot=0;slot<items.size() && needed>0;slot++) {
                    var s=items.get(slot);
                    if(processInputMatches(s,input.getKey(),recipe)) {
                        int used=Math.min(needed,units(s));needed-=used;
                        if(isSeasoning(s)) {
                            s.setDamageValue(s.getDamageValue()+used);
                            items.set(slot,ResourceStates.usedStack(s,emptyContainer(s)));
                        } else {
                            var remainder=s.get(DataComponents.USE_REMAINDER);s.shrink(used);
                            if(remainder!=null)for(int j=0;j<used;j++)if(!insert(remainder.convertInto().create())){for(int i=0;i<items.size();i++)items.set(i,backup.get(i));return;}
                        }
                    }
                }
            }
            boolean fit=true;
            for(var output:recipe.getAsJsonObject("outputs").entrySet()) {
                var item=net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.parse(output.getKey()));
                var result=new ItemStack(item);
                if(excessTofuAcid&&output.getKey().equals("smoke_kitchen:tofu"))KitchenFoodItem.stampMeal(result,1,List.of("strong"),level.getGameTime());
                if(recipe.has("output_uses")&&recipe.getAsJsonObject("output_uses").has(output.getKey())) {
                    int uses=recipe.getAsJsonObject("output_uses").get(output.getKey()).getAsInt();
                    if(!result.isDamageableItem()||uses<1||uses>result.getMaxDamage()){fit=false;continue;}
                    if(uses<result.getMaxDamage()){result.setDamageValue(result.getMaxDamage()-uses);result=ResourceStates.usedStack(result,emptyContainer(result));}
                }
                for(int i=0;i<output.getValue().getAsInt();i++) if(!insert(result.copy())) fit=false;
            }
            if(!fit) { for(int i=0;i<items.size();i++) items.set(i,backup.get(i)); return; }
            batch.water-=ContentCatalog.num(recipe,"water",0);if(batch.water<=0)saltwater=false;work=0;lastTool=""; changed(); return;
        }
    }
    private boolean processInputMatches(ItemStack stack,String id,JsonObject recipe) {
        if(ResourceStates.spent(stack)||!(KitchenContent.fullName(stack.getItem()).equals(id)||("smoke_kitchen:"+ResourceStates.baseId(stack)).equals(id)))return false;
        var tag=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
        if(recipe.has("cuts")&&recipe.getAsJsonObject("cuts").has(id)) {
            boolean allowed=false;for(var value:recipe.getAsJsonObject("cuts").getAsJsonArray(id))if(value.getAsString().equals(tag.getStringOr("kitchen_cut","WHOLE")))allowed=true;
            if(!allowed)return false;
        }
        return !recipe.has("rolled")||!recipe.getAsJsonArray("rolled").contains(new com.google.gson.JsonPrimitive(id))||tag.getBooleanOr("kitchen_rolled",false);
    }
    private boolean isSeasoning(ItemStack stack) { var d=KitchenContent.DEFINITIONS.get(KitchenContent.name(stack.getItem()));return d!=null && d.has("seasoning"); }
    private int units(ItemStack stack) { return isSeasoning(stack)?stack.getMaxDamage()-stack.getDamageValue():stack.getCount(); }
    private Item emptyContainer(ItemStack stack) {
        String s=ContentCatalog.str(KitchenContent.DEFINITIONS.get(KitchenContent.name(stack.getItem())),"seasoning");
        return s.equals("salt") || s.equals("spice") || ResourceStates.baseId(stack).equals("lard")?KitchenContent.item("empty_jar"):Items.GLASS_BOTTLE;
    }
    public static void applyPortion(ItemStack stack,CookingBatch.Portion portion) {
        var tag=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
        tag.putString("kitchen_cut",portion.cut.name());
        tag.putString("kitchen_portion",GSON.toJson(portion)); stack.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));
        if(portion.rolled){tag.putBoolean("kitchen_rolled",true);stack.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));}
    }
    public void dropContents() {
        if(level==null || level.isClientSide()) return;
        settleFreshness();
        if((isBurner()||isOven())&&!mountedCookware.isEmpty()){Block.popResource(level,worldPosition,new ItemStack(KitchenContent.item(mountedCookware)));mountedCookware="";}
        if((kind().equals("cook") || isBurner()) && batch.remainingServings>=0) {
            if(batch.remainingServings>0){var loose=new ItemStack(KitchenContent.item(batch.burnt()?"burnt_food":batch.lockedDish),batch.remainingServings);loose.remove(DataComponents.USE_REMAINDER);KitchenFoodItem.stampMeal(loose,batch.quality(),batch.qualityReasons(),preparedTime());loose=KitchenFoodItem.agedStack(loose,level.getGameTime(),batchFreshness());Block.popResource(level,worldPosition,loose);}
            if(!items.get(26).isEmpty())Block.popResource(level,worldPosition,items.get(26));
        } else {
            for(int i=0;i<items.size();i++) if(!items.get(i).isEmpty()) {
                var result=displayedFood(i);if(i<batch.ingredients.size()){stampLoose(result,batch.portionBatch(i));applyPortion(result,batch.prepareTransfer(i));}
                Block.popResource(level,worldPosition,result);
            }
        }
        items.clear(); batch=new CookingBatch();
        if(!attachedLid.isEmpty()) { Block.popResource(level,worldPosition,new ItemStack(KitchenContent.item(attachedLid))); attachedLid=""; }
    }
    @Override public void preRemoveSideEffects(BlockPos pos,BlockState state) { dropContents(); super.preRemoveSideEffects(pos,state); }
    private void syncState() {
        if(level==null) return;
        BlockState current=getBlockState();
        var contents=KitchenBlock.Contents.valueOf(batch.status().toUpperCase(Locale.ROOT));
        int visibleHeat=(isBurner() || station().id.equals("oven")) && getFuelUnits()>0 ? heat : 0;
        var next=current.setValue(KitchenBlock.HEAT,visibleHeat).setValue(KitchenBlock.FUELED,getFuelUnits()>0).setValue(KitchenBlock.CONTENTS,contents);
        if(next!=current) level.setBlock(worldPosition,next,Block.UPDATE_CLIENTS);
    }
    private void changed() { syncState(); setChanged(); broadcast();if(level!=null)level.updateNeighbourForOutputSignal(worldPosition,getBlockState().getBlock()); }
    private void broadcast() { if(level!=null) level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),Block.UPDATE_CLIENTS); }
    @Override protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out); ContainerHelper.saveAllItems(out,items);
        out.putString("batch",GSON.toJson(batch)); out.putInt("heat",heat); out.putInt("fuel",fuelSeconds);
        out.putInt("work",work); out.putInt("cutProgress",cutProgress); out.putString("cut",cut.name()); out.putLong("lastProcess",lastProcessTime);
        out.putString("attachedLid",attachedLid);
        out.putBoolean("saltwater",saltwater);
        out.putInt("coldTicks",coldTicks);out.putLong("lastCoolingTime",lastCoolingTime);out.putBoolean("lastRedstone",lastRedstone);
        out.putString("mountedCookware",mountedCookware);out.putBoolean("gasValveInitialized",gasValveInitialized);
        out.putInt("methodIndex",methodIndex);
        out.putString("lastTool",lastTool);
        out.putLong("lastFlipTime",lastFlipTime);out.putLong("lastStirTime",lastStirTime);
    }
    @Override protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in); items.clear(); ContainerHelper.loadAllItems(in,items);
        try { batch=GSON.fromJson(in.getStringOr("batch","{}"),CookingBatch.class); if(batch==null) batch=new CookingBatch(); batch.normalize(); }
        catch(RuntimeException e) { SmokeKitchen.LOGGER.warn("Invalid saved cooking state at {}",worldPosition); batch=new CookingBatch(); }
        heat=Math.clamp(in.getIntOr("heat",0),0,3); fuelSeconds=Math.clamp(in.getIntOr("fuel",0),0,7200);
        work=Math.clamp(in.getIntOr("work",0),0,100000); cutProgress=Math.clamp(in.getIntOr("cutProgress",0),0,4); lastProcessTime=in.getLongOr("lastProcess",0);
        attachedLid=in.getStringOr("attachedLid","");
        saltwater=in.getBooleanOr("saltwater",false);
        coldTicks=Math.clamp(in.getIntOr("coldTicks",0),0,216000);lastCoolingTime=in.getLongOr("lastCoolingTime",Long.MIN_VALUE);lastRedstone=in.getBooleanOr("lastRedstone",false);
        mountedCookware=in.getStringOr("mountedCookware","");if(!KitchenContent.BLOCKS.containsKey(mountedCookware))mountedCookware="";
        methodIndex=Math.clamp(in.getIntOr("methodIndex",Arrays.asList(methods()).indexOf(batch.method)),0,methods().length-1);
        gasValveInitialized=in.getBooleanOr("gasValveInitialized",false);
        lastTool=in.getStringOr("lastTool","");
        lastFlipTime=in.getLongOr("lastFlipTime",-100);lastStirTime=in.getLongOr("lastStirTime",-100);
        try { cut=CookingBatch.Cut.valueOf(in.getStringOr("cut","DICE")); } catch(IllegalArgumentException ignored) { cut=CookingBatch.Cut.DICE; }
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveWithoutMetadata(registries); }
    @Override public Packet<ClientGamePacketListener> getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
    @Override public int getContainerSize() { return station().id.equals("spice_rack")?6:items.size(); }
    @Override public int getMaxStackSize(){return station().id.equals("spice_rack")?1:Container.super.getMaxStackSize();}
    @Override public boolean isEmpty() { return items.stream().allMatch(ItemStack::isEmpty); }
    @Override public ItemStack getItem(int slot) { settleFreshness();return items.get(slot); }
    @Override public ItemStack removeItem(int slot,int count) {
        if(!kind().equals("storage") && !kind().equals("rack")) return ItemStack.EMPTY;
        settleFreshness();var result=ContainerHelper.removeItem(items,slot,count); if(!result.isEmpty()) changed(); return result;
    }
    @Override public ItemStack removeItemNoUpdate(int slot) { return removeItem(slot,items.get(slot).getCount()); }
    @Override public void setItem(int slot,ItemStack stack) { settleFreshness();items.set(slot,level==null?stack:KitchenFoodItem.agedStack(stack,level.getGameTime())); changed(); }
    @Override public boolean stillValid(Player player) { return Container.stillValidBlockEntity(this,player); }
    @Override public void clearContent() { items.clear(); changed(); }
    @Override public boolean canPlaceItem(int slot,ItemStack stack) { if(station().id.equals("spice_trellis"))return false;return station().id.equals("spice_rack")?slot>=0&&slot<6&&items.get(slot).isEmpty()&&stack.getCount()==1&&SpiceRack.accepts(stack):kind().equals("storage") || kind().equals("rack"); }
}
