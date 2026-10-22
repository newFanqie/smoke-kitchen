package dev.smokekitchen.world;

import dev.smokekitchen.content.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Block;
import java.util.*;

/** Per-portion dry salting in the existing sealed pickling jar, never a cooking shortcut. */
public final class MeatCuring {
    public static final int DURATION=2400;
    private MeatCuring(){}
    public static boolean meat(ItemStack s){return s.is(Items.PORKCHOP)||Set.of("salting_pork","salted_pork","partially_dried_salted_pork","dried_salted_pork").contains(ResourceStates.baseId(s));}
    public static boolean handles(KitchenBlockEntity e,ItemStack held){return e.kind().equals("pickle")&&(meat(held)||e.items.stream().anyMatch(MeatCuring::meat));}
    public static int progress(ItemStack s){return s.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getIntOr("kitchen_cure_ticks",0);}
    private static void sync(KitchenBlockEntity e){e.setChanged();e.getLevel().sendBlockUpdated(e.getBlockPos(),e.getBlockState(),e.getBlockState(),Block.UPDATE_CLIENTS);e.getLevel().updateNeighbourForOutputSignal(e.getBlockPos(),e.getBlockState().getBlock());}
    public static void settle(KitchenBlockEntity e){
        if(!(e.getLevel() instanceof ServerLevel level)||!e.kind().equals("pickle"))return;
        long now=level.getGameTime();boolean closed=e.getBlockState().getValue(KitchenBlock.LID),changed=false;
        for(int i=0;i<e.items.size();i++){
            var s=e.items.get(i);if(!ResourceStates.baseId(s).equals("salting_pork")||s.getCount()!=1)continue;
            var tag=s.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();int old=progress(s),next=(int)Math.min(DURATION,old+(closed?Math.min(DURATION,Math.max(0,now-tag.getLongOr("kitchen_cure_last",now))):0));
            tag.putLong("kitchen_cure_last",now);tag.putInt("kitchen_cure_ticks",next);s.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));
            if(next>=DURATION&&!ResourceStates.spent(s)){int floor=KitchenFoodItem.freshnessStage(s,now,tag.getLongOr("kitchen_prepared",now));var out=s.transmuteCopy(KitchenContent.item("salted_pork"),1);e.items.set(i,KitchenFoodItem.agedStack(out,now,floor));}
            changed|=next!=old;
        }
        if(changed)sync(e);
    }
    public static Component status(KitchenBlockEntity e){int raw=0,curing=0,ready=0,min=100;for(var s:e.items){if(s.is(Items.PORKCHOP))raw++;else if(ResourceStates.baseId(s).equals("salting_pork")){curing++;min=Math.min(min,progress(s)*100/DURATION);}else if(meat(s))ready++;}return Component.literal("盐腌猪肉：未用盐 "+raw+"｜腌制 "+curing+"份 "+(curing==0?0:min)+"%｜可取 "+ready+"；"+(e.getBlockState().getValue(KitchenBlock.LID)?"已密封":"开盖暂停")+"。切片后每份1次盐，腌透再挂干，加热后吃。");}
    public static InteractionResult interact(KitchenBlockEntity e,Player p,ItemStack held){
        settle(e);long now=e.getLevel().getGameTime();String id=ResourceStates.baseId(held);boolean closed=e.getBlockState().getValue(KitchenBlock.LID);
        if(id.equals("recipe_book")){RecipeGuide.show(p);return InteractionResult.SUCCESS;}
        if(held.isEmpty()){
            if(p.isShiftKeyDown()){if(closed){p.sendOverlayMessage(Component.literal("请先空手开盖，再潜行取回腌肉。"));return InteractionResult.SUCCESS;}for(int i=e.items.size()-1;i>=0;i--)if(!e.items.get(i).isEmpty()){var out=e.items.get(i);if(!p.getInventory().add(out))p.drop(out,false);e.items.set(i,ItemStack.EMPTY);sync(e);return InteractionResult.SUCCESS;}}
            else {e.getLevel().setBlock(e.getBlockPos(),e.getBlockState().cycle(KitchenBlock.LID),Block.UPDATE_ALL);for(var s:e.items)if(ResourceStates.baseId(s).equals("salting_pork")){var tag=s.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();tag.putLong("kitchen_cure_last",now);s.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));}sync(e);}
            p.sendOverlayMessage(status(e));return InteractionResult.SUCCESS;
        }
        if(closed){p.sendOverlayMessage(Component.literal("密封中不能投料，请先空手开盖。"));return InteractionResult.SUCCESS;}
        if(ResourceStates.spent(held)){p.sendOverlayMessage(Component.literal("变质肉不能重新腌成新鲜肉。"));return InteractionResult.SUCCESS;}
        if(id.equals("salt")){
            var targets=e.items.stream().filter(s->s.getCount()==1&&(s.is(Items.PORKCHOP)||ResourceStates.baseId(s).equals("salting_pork"))).toList();int needed=targets.size();
            if(needed==0||held.getMaxDamage()-held.getDamageValue()<needed){p.sendOverlayMessage(Component.literal("每份猪肉需要1次盐；盐不足时原物保留。"));return InteractionResult.SUCCESS;}
            if(targets.stream().anyMatch(s->KitchenFoodItem.freshnessStage(s,now,s.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getLongOr("kitchen_prepared",now))>0)){p.sendOverlayMessage(Component.literal("不新鲜的肉不能通过补盐重新变新。"));return InteractionResult.SUCCESS;}
            if(e.items.stream().filter(MeatCuring::meat).anyMatch(s->s.getCount()!=1)){p.sendOverlayMessage(Component.literal("旧叠堆请先取回，逐份重新放入。"));return InteractionResult.SUCCESS;}
            for(int i=0;i<e.items.size();i++){var s=e.items.get(i);if(!targets.contains(s))continue;var tag=s.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();tag.putInt("kitchen_meat_salt",tag.getIntOr("kitchen_meat_salt",0)+1);tag.putLong("kitchen_prepared",tag.getLongOr("kitchen_prepared",now));tag.putLong("kitchen_cure_last",now);var out=s.transmuteCopy(KitchenContent.item("salting_pork"),1);out.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));if(tag.getIntOr("kitchen_meat_salt",0)>1){KitchenFoodItem.stampMeal(out,1,List.of("salty"),tag.getLongOr("kitchen_prepared",now));}e.items.set(i,out);}
            if(!p.isCreative()){held.setDamageValue(held.getDamageValue()+needed);ResourceStates.replaceHeld(p,held,ResourceStates.usedStack(held,KitchenContent.item("empty_jar")));}sync(e);p.sendOverlayMessage(status(e));return InteractionResult.SUCCESS;
        }
        if(!meat(held)||!held.is(Items.PORKCHOP)&&!id.equals("salting_pork")&&!id.equals("salted_pork")){p.sendOverlayMessage(Component.literal("猪肉干腌不加水、不混蔬菜；腌透后放晾晒架。"));return InteractionResult.SUCCESS;}
        var tag=held.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
        if(held.is(Items.PORKCHOP)&&!tag.getStringOr("kitchen_cut","WHOLE").equals("SLICE")){p.sendOverlayMessage(Component.literal("先在砧板把猪肉切片，整块不能直接腌干。"));return InteractionResult.SUCCESS;}
        if(held.is(Items.PORKCHOP)&&tag.contains("kitchen_portion")){p.sendOverlayMessage(Component.literal("干腌使用未入锅的新鲜生猪肉，不能清除旧锅的品质或调味。"));return InteractionResult.SUCCESS;}
        if(e.batch.water>0||e.items.stream().anyMatch(s->!s.isEmpty()&&!meat(s))){p.sendOverlayMessage(Component.literal("请用无水且没有其他原料的腌菜缸做干腌。"));return InteractionResult.SUCCESS;}
        int slot=-1;for(int i=0;i<6;i++)if(e.items.get(i).isEmpty()){slot=i;break;}if(slot<0){p.sendOverlayMessage(Component.literal("最多逐份腌6份猪肉。"));return InteractionResult.SUCCESS;}
        var copy=held.copyWithCount(1);tag.putLong("kitchen_prepared",tag.getLongOr("kitchen_prepared",now));tag.putLong("kitchen_cure_last",now);copy.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));e.items.set(slot,copy);if(!p.isCreative())held.shrink(1);sync(e);p.sendOverlayMessage(status(e));return InteractionResult.SUCCESS;
    }
}
