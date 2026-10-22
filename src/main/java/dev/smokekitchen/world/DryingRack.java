package dev.smokekitchen.world;

import dev.smokekitchen.content.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.server.level.ServerLevel;

public final class DryingRack {
    public static final int DURATION=2400;
    private DryingRack(){}
    private static String input(ItemStack stack){String id=ResourceStates.baseId(stack);return id.equals("partially_dried_salted_pork")?"salted_pork":id;}
    public static String output(ItemStack stack){if(ResourceStates.spent(stack))return "";String id=input(stack);return java.util.Set.of("chili","shiitake","black_pepper","sichuan_pepper","cumin","salted_pork").contains(id)?"dried_"+id:"";}
    public static String appearanceType(ItemStack stack){String id=input(stack);if(id.startsWith("dried_"))id=id.substring(6);return java.util.Set.of("chili","shiitake","black_pepper","sichuan_pepper","cumin","salted_pork").contains(id)?id:"";}
    public static int duration(ItemStack stack){return input(stack).equals("salted_pork")?4800:DURATION;}
    public static boolean accepts(ItemStack stack){return !output(stack).isEmpty();}
    public static int progress(ItemStack stack){return Math.clamp(stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getIntOr("kitchen_dry_ticks",KitchenContent.name(stack.getItem()).startsWith("partially_dried_")||ResourceStates.baseId(stack).startsWith("partially_dried_")?duration(stack)/2:0),0,duration(stack));}
    private static void progress(ItemStack stack,int value){var tag=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();tag.putInt("kitchen_dry_ticks",value);stack.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));}
    public static boolean weatherSensitive(ServerLevel level,BlockPos pos){var at=pos.above();return level.canSeeSky(at)&&level.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING,at).getY()<=at.getY()&&level.getBiome(at).value().getPrecipitationAt(at,level.getSeaLevel())==Biome.Precipitation.RAIN;}
    public static void settle(KitchenBlockEntity rack){
        if(!(rack.getLevel() instanceof ServerLevel level)||!rack.station().id.equals("drying_rack"))return;
        long now=level.getGameTime();var history=DryWeatherHistory.get(level);
        if(rack.lastDryTick==Long.MIN_VALUE){
            // Old shared work belongs to at most one original item, never every new hanging slot.
            for(int i=0;i<6;i++)if(rack.items.get(i).getCount()==1&&accepts(rack.items.get(i))){if(progress(rack.items.get(i))==0&&rack.work>0)progress(rack.items.get(i),(int)Math.min(DURATION-1,(long)rack.work*20));break;}
            rack.work=0;rack.lastDryTick=now;rack.dryWeatherSensitive=weatherSensitive(level,rack.getBlockPos());rack.setChanged();return;
        }
        long elapsed=rack.dryWeatherSensitive?history.dryTicks(rack.lastDryTick,now):Math.max(0,now-rack.lastDryTick);boolean changed=false;
        for(int i=0;i<6;i++){
            var stack=rack.items.get(i);String out=output(stack);if(stack.getCount()!=1||out.isEmpty())continue;
            int limit=duration(stack),old=progress(stack),next=(int)Math.min(limit,old+Math.min(limit,elapsed));if(next==old)continue;
            int floor=KitchenFoodItem.freshnessStage(stack,now,stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getLongOr("kitchen_prepared",now));
            if(next>=limit){var result=stack.transmuteCopy(KitchenContent.item(out),1);var tag=result.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();tag.remove("kitchen_dry_ticks");result.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));rack.items.set(i,KitchenFoodItem.agedStack(result,now,floor));}else{var result=stack.transmuteCopy(KitchenContent.item("partially_dried_"+input(stack)),1);progress(result,next);rack.items.set(i,KitchenFoodItem.agedStack(result,now,floor));}changed=true;
        }
        rack.lastDryTick=now;rack.dryWeatherSensitive=weatherSensitive(level,rack.getBlockPos());rack.setChanged();
        if(changed)level.sendBlockUpdated(rack.getBlockPos(),rack.getBlockState(),rack.getBlockState(),Block.UPDATE_CLIENTS);
    }
    public static int slot(BlockPos pos,net.minecraft.world.level.block.state.BlockState state,BlockHitResult hit){int slot=SpiceRack.slot(pos,state,hit);return slot%3+(hit.getLocation().y-pos.getY()>=.5?3:0);}
    public static float[] position(int slot,Direction face){var p=SpiceRack.position(slot,face);if(face.getAxis()==Direction.Axis.X)p[0]=.5f;else p[1]=.5f;return p;}
    public static Component status(KitchenBlockEntity rack){
        int raw=0,ready=0,min=100;for(int i=0;i<6;i++){var s=rack.items.get(i);if(s.getCount()==1&&accepts(s)){raw++;min=Math.min(min,100*progress(s)/duration(s));}else if(!s.isEmpty())ready++;}
        boolean wet=rack.getLevel().isRainingAt(rack.getBlockPos().above());return Component.literal("晾晒："+raw+"件处理中 "+(raw==0?0:min)+"%｜"+ready+"格可取｜"+(wet?"露天雨水，暂停":"通风晾制")+"；对准格子放/取，潜行取旧余物");
    }
}
