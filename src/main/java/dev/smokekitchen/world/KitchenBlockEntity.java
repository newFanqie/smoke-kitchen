package dev.smokekitchen.world;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import dev.smokekitchen.SmokeKitchen;
import dev.smokekitchen.content.ContentCatalog;
import dev.smokekitchen.content.KitchenContent;
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
    public long lastCoolingTime;
    public boolean lastRedstone;
    private int tickCounter;

    public KitchenBlockEntity(BlockPos pos, BlockState state) { super(KitchenContent.STATION_ENTITY,pos,state); if(state.getBlock() instanceof KitchenBlock b && b.kind.equals("cook")) batch.method=methods()[0]; }
    public KitchenBlock station() { return (KitchenBlock)getBlockState().getBlock(); }
    public String kind() { return station().kind; }
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
        if(station().id.equals("icebox"))coolInventory();
        String held = stack.isEmpty() ? "" : KitchenContent.name(stack.getItem());
        if (held.equals("recipe_book")) { RecipeGuide.show(p); return InteractionResult.SUCCESS; }
        if (held.equals("thermometer")) { showStatus(p); return InteractionResult.SUCCESS; }
        if (kind().equals("stove")) return stove(p,stack);
        if (kind().equals("board")) return board(p,stack,held);
        if (kind().equals("cook")) return cook(p,stack,held);
        if(kind().equals("mix") && (held.equals("rolling_pin") || held.equals("sieve") || held.contains("knife"))) {
            work+=5;tryProcess();wear(p,stack,Items.AIR);changed();sound();return InteractionResult.SUCCESS;
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
        if (stack.isEmpty() && p.isShiftKeyDown()) { retrieve(p); return InteractionResult.SUCCESS; }
        if (stack.isEmpty()) {
            if (Set.of("mill","press","mix").contains(kind())) work += 5;
            if (kind().equals("ferment") || kind().equals("pickle")) {
                level.setBlock(worldPosition,getBlockState().cycle(KitchenBlock.LID),Block.UPDATE_ALL);
            }
            tryProcess(); showStatus(p); changed();
            return InteractionResult.SUCCESS;
        }
        if (stack.is(Items.WATER_BUCKET) || held.equals("saltwater_bucket")) {
            if(batch.water>0) { say(p,"full");return InteractionResult.SUCCESS; }
            batch.water = 6; saltwater=held.equals("saltwater_bucket"); replaceContainer(p,stack,Items.BUCKET); changed(); return InteractionResult.SUCCESS;
        }
        if (insert(stack.copyWithCount(1))) { take(p,stack); changed(); sound(); }
        else say(p,"full");
        return InteractionResult.SUCCESS;
    }

    private InteractionResult stove(Player p,ItemStack stack) {
        if (stack.isEmpty()) { heat = (heat+1)%4; changed(); showStatus(p); return InteractionResult.SUCCESS; }
        int seconds = stack.is(Items.COAL_BLOCK) ? 720 : stack.is(Items.COAL) || stack.is(Items.CHARCOAL) ? 80 :
                stack.is(Items.STICK) || stack.is(KitchenContent.item("rice_husk")) ? 5 : 0;
        if (seconds <= 0) { say(p,"fuel"); return InteractionResult.SUCCESS; }
        if (fuelSeconds >= 3600) { say(p,"full"); return InteractionResult.SUCCESS; }
        fuelSeconds += seconds; take(p,stack); changed(); sound();
        return InteractionResult.SUCCESS;
    }

    private InteractionResult board(Player p,ItemStack stack,String held) {
        if (held.contains("knife")) {
            if (p.isShiftKeyDown()) { cut = CookingBatch.Cut.values()[(cut.ordinal()+1)%CookingBatch.Cut.values().length]; say(p,"cut",cut.name()); changed(); return InteractionResult.SUCCESS; }
            if (items.getFirst().isEmpty()) { say(p,"ingredient"); return InteractionResult.SUCCESS; }
            cutProgress++;
            sound();
            if (cutProgress >= 4) {
                ItemStack result = items.getFirst().copy();
                CompoundTag data = result.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
                data.putString("kitchen_cut",cut.name());
                result.set(DataComponents.CUSTOM_DATA,CustomData.of(data));
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
            if (p.isShiftKeyDown()) {
                if (batch.remainingServings >= 0) { say(p,"serving_started"); return InteractionResult.SUCCESS; }
                for (int i = batch.ingredients.size()-1; i >= 0; i--) {
                    var item = items.get(i);
                    if (!item.isEmpty()) { applyPortion(item,batch.ingredients.remove(i)); give(p,item); items.set(i,ItemStack.EMPTY); break; }
                }
            } else {
                var methods = methods(); methodIndex = (methodIndex+1)%methods.length; batch.method = methods[methodIndex];
            }
            changed(); showStatus(p); return InteractionResult.SUCCESS;
        }
        if (held.contains("spatula") || held.equals("ladle") || held.equals("tongs") || held.equals("chopsticks")) {
            batch.stir(); wear(p,stack,Items.AIR); sound(); changed(); showStatus(p); return InteractionResult.SUCCESS;
        }
        if (held.equals("pot_lid") || held.equals("wooden_lid")) {
            if(!getBlockState().getValue(KitchenBlock.LID)) { attachedLid=held; take(p,stack); }
            else if(!attachedLid.isEmpty()) { give(p,new ItemStack(KitchenContent.item(attachedLid))); attachedLid=""; }
            level.setBlock(worldPosition,getBlockState().cycle(KitchenBlock.LID),Block.UPDATE_ALL); changed(); return InteractionResult.SUCCESS;
        }
        if (stack.is(Items.BOWL) || held.equals("ceramic_plate") || held.equals("ceramic_bowl")) {
            serve(p,stack); return InteractionResult.SUCCESS;
        }
        if (batch.remainingServings >= 0) { say(p,"serving_started"); return InteractionResult.SUCCESS; }
        if (stack.is(Items.WATER_BUCKET) || held.equals("water_jug")) {
            if (batch.water >= 6) { say(p,"full"); return InteractionResult.SUCCESS; }
            batch.water = Math.min(6,batch.water+(held.equals("water_jug")?1:6));
            if (stack.is(Items.WATER_BUCKET)) replaceContainer(p,stack,Items.BUCKET); else wear(p,stack,KitchenContent.item("empty_water_jug"));
            changed(); return InteractionResult.SUCCESS;
        }
        var def = KitchenContent.DEFINITIONS.get(held);
        if (def != null && def.has("seasoning")) {
            String seasoning = ContentCatalog.str(def,"seasoning");
            boolean accepted = switch (seasoning) {
                case "salt" -> { if(batch.salt>=16) yield false; batch.salt++; yield true; }
                case "oil" -> { if(batch.oil>=8) yield false; batch.oil++; yield true; }
                case "soy" -> { if(batch.soy>=16) yield false; batch.soy++; yield true; }
                case "vinegar" -> { if(batch.vinegar>=16) yield false; batch.vinegar++; yield true; }
                default -> { if(batch.spice>=16) yield false; batch.spice++; yield true; }
            };
            if (accepted) { wear(p,stack,seasoning.equals("salt") || seasoning.equals("spice") ? KitchenContent.item("empty_jar") : Items.GLASS_BOTTLE); changed(); }
            else say(p,"full");
            showStatus(p); return InteractionResult.SUCCESS;
        }
        if (stack.is(Items.SUGAR) || stack.is(Items.HONEY_BOTTLE)) {
            if (batch.sugar < 16) { batch.sugar++; if(stack.is(Items.HONEY_BOTTLE)) replaceContainer(p,stack,Items.GLASS_BOTTLE); else take(p,stack); changed(); }
            return InteractionResult.SUCCESS;
        }
        var spec = ingredient(stack);
        if (spec.isEmpty()) { say(p,"ingredient"); return InteractionResult.SUCCESS; }
        var s = spec.get();
        var data = stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
        CookingBatch.Cut chosen = CookingBatch.Cut.WHOLE;
        try { chosen = CookingBatch.Cut.valueOf(data.getStringOr("kitchen_cut","WHOLE")); } catch (IllegalArgumentException ignored) {}
        boolean safeRaw=Set.of("cucumber","garlic","tomato","cabbage","scallion","onion").contains(s.id);
        if (batch.add(s.id,chosen,s.resistance,s.water,s.ready || batch.method==CookingBatch.Method.MIX && safeRaw)) {
            int index = batch.ingredients.size()-1;
            if (data.contains("kitchen_portion")) {
                try { var saved = GSON.fromJson(data.getStringOr("kitchen_portion",""),CookingBatch.Portion.class);
                    if (saved != null && saved.id.equals(s.id)) batch.ingredients.set(index,saved);
                } catch (RuntimeException ignored) {}
                batch.normalize();
            }
            items.set(index,stack.is(Items.MILK_BUCKET)?new ItemStack(KitchenContent.item("milk_portion")):stack.copyWithCount(1));
            if(stack.is(Items.MILK_BUCKET)) replaceContainer(p,stack,Items.BUCKET); else take(p,stack);
            changed(); sound();
        } else say(p,"full");
        return InteractionResult.SUCCESS;
    }

    public record Ingredient(String id,double resistance,boolean water,boolean ready) {}
    public static Optional<Ingredient> ingredient(ItemStack stack) {
        String n = KitchenContent.name(stack.getItem());
        var d = KitchenContent.DEFINITIONS.get(n);
        if (d != null && d.has("ingredient")) return Optional.of(new Ingredient(ContentCatalog.str(d,"ingredient"),
                d.has("resistance") ? d.get("resistance").getAsDouble() : 1,ContentCatalog.flag(d,"needs_water"),ContentCatalog.flag(d,"ready")));
        String canonical = switch (n) {
            case "egg" -> "egg";
            case "potato" -> "potato";
            case "carrot" -> "carrot";
            case "beef" -> "beef";
            case "porkchop" -> "pork";
            case "chicken" -> "chicken";
            case "cod","salmon","tropical_fish" -> "fish";
            case "brown_mushroom","red_mushroom" -> "mushroom";
            case "pumpkin" -> "pumpkin";
            case "milk_bucket" -> "milk";
            case "apple" -> "apple";
            case "dried_kelp" -> "kelp";
            default -> "";
        };
        return canonical.isEmpty() ? Optional.empty() : Optional.of(new Ingredient(canonical,
                Set.of("beef","pork","chicken","fish").contains(canonical)?1.6:1,false,canonical.equals("apple") || canonical.equals("milk") || canonical.equals("kelp")));
    }

    private CookingBatch.Method[] methods() {
        return switch (station().id) {
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
        if (!batch.ready() && !batch.burnt()) { say(p,"not_ready"); return; }
        String dish = batch.burnt() ? "burnt_food" : "mixed_stir_fry";
        int servings = Math.max(1,batch.ingredients.size()/2);
        for (var r : ContentCatalog.DISHES) if (r.matches(batch)) { dish=r.id(); servings=r.servings(); break; }
        if (batch.burnt()) dish="burnt_food";
        if (batch.remainingServings >= 0 && !batch.burnt()) dish=batch.lockedDish;
        int quality = batch.quality();
        if (!batch.serve(dish,servings)) { say(p,"empty"); return; }
        var result = new ItemStack(KitchenContent.item(dish));
        result.set(DataComponents.USE_REMAINDER,new net.minecraft.world.item.component.UseRemainder(net.minecraft.world.item.ItemStackTemplate.fromNonEmptyStack(container.copyWithCount(1))));
        var tag = new CompoundTag(); tag.putInt("kitchen_quality",quality); tag.putLong("kitchen_prepared",level.getGameTime());
        result.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));
        take(p,container); give(p,result);
        if (batch.remainingServings == 0) { items.clear(); batch=new CookingBatch(); batch.method=methods()[0]; }
        changed(); sound();
    }
    private void movePot(Player p) {
        var result=new ItemStack(getBlockState().getBlock());
        result.set(DataComponents.BLOCK_ENTITY_DATA,net.minecraft.world.item.component.TypedEntityData.of(KitchenContent.STATION_ENTITY,saveWithFullMetadata(level.registryAccess())));
        items.clear();batch=new CookingBatch();attachedLid="";
        level.removeBlock(worldPosition,false);give(p,result);
    }

    private void replaceContainer(Player p,ItemStack stack,Item replacement) {
        if (p.isCreative()) return;
        stack.shrink(1); give(p,new ItemStack(replacement));
    }
    private void wear(Player p,ItemStack stack,Item replacement) {
        if (p.isCreative() || !stack.isDamageableItem()) return;
        stack.setDamageValue(stack.getDamageValue()+1);
        if (stack.getDamageValue()>=stack.getMaxDamage()) {
            stack.shrink(1); if (replacement != Items.AIR) give(p,new ItemStack(replacement));
        }
    }
    private boolean insert(ItemStack stack) {
        for (int i=0;i<items.size();i++) {
            if (items.get(i).isEmpty()) { items.set(i,stack); return true; }
            if (ItemStack.isSameItemSameComponents(items.get(i),stack) && items.get(i).getCount()<items.get(i).getMaxStackSize()) { items.get(i).grow(1); return true; }
        }
        return false;
    }
    private void retrieve(Player p) {
        for (int i=items.size()-1;i>=0;i--) if (!items.get(i).isEmpty()) {
            give(p,items.get(i)); items.set(i,ItemStack.EMPTY); changed(); return;
        }
        say(p,"empty");
    }
    public void showStatus(Player p) {
        if (kind().equals("cook")) say(p,"cook_status",Math.round(batch.temperature),batch.ingredients.size(),batch.water > 0 ? String.format(java.util.Locale.ROOT,"%.1f",batch.water) : "0",batch.salt,batch.oil,
                Component.translatable("state.smoke_kitchen."+batch.status()));
        else if(kind().equals("stove")) say(p,"stove_status",heat,fuelSeconds);
        else if(kind().equals("board")) say(p,"board_status",cut.name(),cutProgress);
        else say(p,"station_status",items.stream().filter(s->!s.isEmpty()).mapToInt(ItemStack::getCount).sum(),work,getBlockState().getValue(KitchenBlock.LID)?"✓":"×");
    }
    public void serverTick() {
        if (++tickCounter % 20 != 0) return;
        if (kind().equals("stove")) {
            boolean powered=level.hasNeighborSignal(worldPosition);
            if(powered!=lastRedstone){heat=powered?2:0;lastRedstone=powered;}
            if (fuelSeconds>0 && heat>0) fuelSeconds=Math.max(0,fuelSeconds-heat);
            if(fuelSeconds>0 && heat>0 && level instanceof net.minecraft.server.level.ServerLevel server) server.sendParticles(net.minecraft.core.particles.ParticleTypes.FLAME,worldPosition.getX()+.5,worldPosition.getY()+.9,worldPosition.getZ()+.5,1,.15,.02,.15,.01);
            syncState(); setChanged(); return;
        }
        if (kind().equals("cook")) {
            int sourceHeat=0;
            if (level.getBlockEntity(worldPosition.below()) instanceof KitchenBlockEntity stove && stove.kind().equals("stove") && stove.fuelSeconds>0) sourceHeat=stove.heat;
            if(station().id.equals("oven") && fuelSeconds>0){sourceHeat=heat;fuelSeconds=Math.max(0,fuelSeconds-heat);}
            batch.tick(sourceHeat,getBlockState().getValue(KitchenBlock.LID));
            if(batch.temperature>90 && !batch.ingredients.isEmpty() && level instanceof net.minecraft.server.level.ServerLevel server) server.sendParticles(batch.burnt()?net.minecraft.core.particles.ParticleTypes.SMOKE:net.minecraft.core.particles.ParticleTypes.CLOUD,worldPosition.getX()+.5,worldPosition.getY()+.5,worldPosition.getZ()+.5,1,.1,.05,.1,.005);
            syncState(); setChanged();
        } else if (Set.of("ferment","pickle","dry","evaporate").contains(kind())) {
            long now=level.getGameTime();
            boolean allowed=kind().equals("dry")?!level.isRainingAt(worldPosition.above()):kind().equals("evaporate")?
                    level.getBlockEntity(worldPosition.below()) instanceof KitchenBlockEntity stove && stove.kind().equals("stove") && stove.heat>0 && stove.fuelSeconds>0:getBlockState().getValue(KitchenBlock.LID);
            if(lastProcessTime>0 && allowed) work+=(int)Math.min(72000,Math.max(1,(now-lastProcessTime)/20));
            lastProcessTime=now; tryProcess(); setChanged();
        }
        if(station().id.equals("icebox"))coolInventory();
        if(tickCounter%40==0) broadcast();
    }
    private void coolInventory() {
        if(level==null)return;
        long now=level.getGameTime();
        if(lastCoolingTime==0){lastCoolingTime=now;return;}
        long elapsed=Math.clamp(now-lastCoolingTime,0,24000L*365);lastCoolingTime=now;
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
        for (var element : ContentCatalog.DATA.getAsJsonArray("processes")) {
            var recipe=element.getAsJsonObject();
            if(!ContentCatalog.str(recipe,"station").equals(kind())) continue;
            if(ContentCatalog.flag(recipe,"saltwater") && !saltwater) continue;
            if(work<ContentCatalog.num(recipe,"seconds",5) || batch.water<ContentCatalog.num(recipe,"water",0)) continue;
            var inputs=recipe.getAsJsonObject("inputs");
            boolean matches=true;
            for(var input:inputs.entrySet()) {
                int found=items.stream().filter(s->KitchenContent.fullName(s.getItem()).equals(input.getKey())).mapToInt(this::units).sum();
                if(found<input.getValue().getAsInt()) { matches=false; break; }
            }
            if(!matches) continue;
            // Only mutate after every input and every output slot has been validated.
            var backup=NonNullList.withSize(items.size(),ItemStack.EMPTY);
            for(int i=0;i<items.size();i++) backup.set(i,items.get(i).copy());
            for(var input:inputs.entrySet()) {
                int needed=input.getValue().getAsInt();
                for(int slot=0;slot<items.size() && needed>0;slot++) {
                    var s=items.get(slot);
                    if(KitchenContent.fullName(s.getItem()).equals(input.getKey())) {
                        int used=Math.min(needed,units(s));needed-=used;
                        if(isSeasoning(s)) {
                            s.setDamageValue(s.getDamageValue()+used);
                            if(s.getDamageValue()>=s.getMaxDamage())items.set(slot,new ItemStack(emptyContainer(s)));
                        } else s.shrink(used);
                    }
                }
            }
            boolean fit=true;
            for(var output:recipe.getAsJsonObject("outputs").entrySet()) {
                var item=net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.parse(output.getKey()));
                for(int i=0;i<output.getValue().getAsInt();i++) if(!insert(new ItemStack(item))) fit=false;
            }
            if(!fit) { for(int i=0;i<items.size();i++) items.set(i,backup.get(i)); return; }
            batch.water-=ContentCatalog.num(recipe,"water",0); work=0; changed(); return;
        }
    }
    private boolean isSeasoning(ItemStack stack) { var d=KitchenContent.DEFINITIONS.get(KitchenContent.name(stack.getItem()));return d!=null && d.has("seasoning"); }
    private int units(ItemStack stack) { return isSeasoning(stack)?stack.getMaxDamage()-stack.getDamageValue():stack.getCount(); }
    private Item emptyContainer(ItemStack stack) {
        String s=ContentCatalog.str(KitchenContent.DEFINITIONS.get(KitchenContent.name(stack.getItem())),"seasoning");
        return s.equals("salt") || s.equals("spice")?KitchenContent.item("empty_jar"):Items.GLASS_BOTTLE;
    }
    public static void applyPortion(ItemStack stack,CookingBatch.Portion portion) {
        var tag=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
        tag.putString("kitchen_portion",GSON.toJson(portion)); stack.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));
    }
    public void dropContents() {
        if(level==null || level.isClientSide()) return;
        if(kind().equals("cook") && batch.remainingServings>=0) {
            if(batch.remainingServings>0) Block.popResource(level,worldPosition,new ItemStack(KitchenContent.item(batch.burnt()?"burnt_food":batch.lockedDish),batch.remainingServings));
        } else {
            for(int i=0;i<items.size();i++) if(!items.get(i).isEmpty()) {
                if(kind().equals("cook") && i<batch.ingredients.size()) applyPortion(items.get(i),batch.ingredients.get(i));
                Block.popResource(level,worldPosition,items.get(i));
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
        int visibleHeat=(kind().equals("stove") || station().id.equals("oven")) && fuelSeconds>0 ? heat : 0;
        var next=current.setValue(KitchenBlock.HEAT,visibleHeat).setValue(KitchenBlock.CONTENTS,contents);
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
    }
    @Override protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in); items.clear(); ContainerHelper.loadAllItems(in,items);
        try { batch=GSON.fromJson(in.getStringOr("batch","{}"),CookingBatch.class); if(batch==null) batch=new CookingBatch(); batch.normalize(); }
        catch(RuntimeException e) { SmokeKitchen.LOGGER.warn("Invalid saved cooking state at {}",worldPosition); batch=new CookingBatch(); }
        heat=Math.clamp(in.getIntOr("heat",0),0,3); fuelSeconds=Math.clamp(in.getIntOr("fuel",0),0,7200);
        work=Math.clamp(in.getIntOr("work",0),0,100000); cutProgress=Math.clamp(in.getIntOr("cutProgress",0),0,4); lastProcessTime=in.getLongOr("lastProcess",0);
        attachedLid=in.getStringOr("attachedLid","");
        saltwater=in.getBooleanOr("saltwater",false);
        coldTicks=Math.clamp(in.getIntOr("coldTicks",0),0,216000);lastCoolingTime=in.getLongOr("lastCoolingTime",0);lastRedstone=in.getBooleanOr("lastRedstone",false);
        try { cut=CookingBatch.Cut.valueOf(in.getStringOr("cut","DICE")); } catch(IllegalArgumentException ignored) { cut=CookingBatch.Cut.DICE; }
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveWithoutMetadata(registries); }
    @Override public Packet<ClientGamePacketListener> getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
    @Override public int getContainerSize() { return items.size(); }
    @Override public boolean isEmpty() { return items.stream().allMatch(ItemStack::isEmpty); }
    @Override public ItemStack getItem(int slot) { return items.get(slot); }
    @Override public ItemStack removeItem(int slot,int count) {
        if(!kind().equals("storage") && !kind().equals("rack")) return ItemStack.EMPTY;
        var result=ContainerHelper.removeItem(items,slot,count); if(!result.isEmpty()) changed(); return result;
    }
    @Override public ItemStack removeItemNoUpdate(int slot) { return removeItem(slot,items.get(slot).getCount()); }
    @Override public void setItem(int slot,ItemStack stack) { items.set(slot,stack); changed(); }
    @Override public boolean stillValid(Player player) { return Container.stillValidBlockEntity(this,player); }
    @Override public void clearContent() { items.clear(); changed(); }
    @Override public boolean canPlaceItem(int slot,ItemStack stack) { return kind().equals("storage") || kind().equals("rack"); }
}
