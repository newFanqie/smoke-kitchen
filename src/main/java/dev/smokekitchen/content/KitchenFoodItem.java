package dev.smokekitchen.content;

import dev.smokekitchen.KitchenConfig;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;

public final class KitchenFoodItem extends Item {
    private final int nutrition;
    public KitchenFoodItem(Properties props,int nutrition) {super(props);this.nutrition=nutrition;}
    @Override public void appendHoverText(ItemStack stack,TooltipContext context,net.minecraft.world.item.component.TooltipDisplay display,java.util.function.Consumer<Component> lines,TooltipFlag flag) {
        super.appendHoverText(stack,context,display,lines,flag);
        appendQuality(stack,lines);
    }
    public static void appendQuality(ItemStack stack,java.util.function.Consumer<Component> lines) {
        var data=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
        if(data.contains("kitchen_quality")) {
            int quality=Math.clamp(data.getIntOr("kitchen_quality",0),0,3);
            lines.accept(Component.translatable("message.smoke_kitchen.quality_grade",Component.translatable("quality.smoke_kitchen."+quality),quality));
            for(String reason:data.getStringOr("kitchen_quality_reasons","").split(","))if(java.util.Set.of("raw","burnt","salty","strong","bland","overcooked","scorched","uneven","stale").contains(reason))lines.accept(Component.translatable("quality_reason.smoke_kitchen."+reason));
        }
    }
    public static void stampMeal(ItemStack stack,int quality,java.util.List<String> reasons,long now) {
        var tag=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
        tag.putInt("kitchen_quality",quality);tag.putString("kitchen_quality_reasons",String.join(",",reasons));tag.putLong("kitchen_prepared",now);
        stack.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));
        var definition=KitchenContent.DEFINITIONS.get(ResourceStates.baseId(stack));
        var original=stack.getItem().components().get(DataComponents.FOOD);
        if(definition!=null||original!=null) {
            int base=definition!=null?ContentCatalog.num(definition,"nutrition",1):original.nutrition();
            int nutrition=quality<=1?Math.max(1,(int)Math.floor(base*.6)):base;
            stack.set(DataComponents.FOOD,new FoodProperties.Builder().nutrition(nutrition).saturationModifier(quality>=3?.5f:quality==2?.4f:.2f).build());
        }
        if(definition!=null&&ContentCatalog.flag(definition,"uncooked")){stack.remove(DataComponents.FOOD);stack.remove(DataComponents.CONSUMABLE);}
    }
    @Override public void inventoryTick(ItemStack stack,ServerLevel level,Entity owner,EquipmentSlot slot) {
        super.inventoryTick(stack,level,owner,slot);
        if(level.getGameTime()%100!=0 || !KitchenConfig.INSTANCE.spoilage)return;
        var result=agedStack(stack,level.getGameTime());
        if(result==stack)return;
        if(slot!=null && owner instanceof net.minecraft.world.entity.LivingEntity living)living.setItemSlot(slot,result);
        else if(owner instanceof net.minecraft.world.entity.player.Player player)for(int i=0;i<player.getInventory().getContainerSize();i++)if(player.getInventory().getItem(i)==stack){player.getInventory().setItem(i,result);break;}
    }
    /** Preserve state even if an old save or a clock rollback would otherwise make food fresh again. */
    public static int freshnessStage(ItemStack stack,long now,long prepared) {
        var definition=KitchenContent.DEFINITIONS.get(KitchenContent.name(stack.getItem()));
        String state=definition==null?"":ContentCatalog.str(definition,"state");
        int floor=Math.max(state.equals("spoiled")?2:state.equals("stale")?1:0,stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getIntOr("kitchen_freshness_floor",0));
        if(!(stack.getItem() instanceof KitchenFoodItem)||!KitchenConfig.INSTANCE.spoilage)return floor;
        var base=KitchenContent.DEFINITIONS.get(ResourceStates.baseId(stack));int multiplier=base==null?1:ContentCatalog.num(base,"freshness_multiplier",1);
        return Math.max(floor,elapsedStage(now,prepared,Math.max(1,multiplier)));
    }
    public static int elapsedStage(long now,long prepared){
        return elapsedStage(now,prepared,1);
    }
    private static int elapsedStage(long now,long prepared,int multiplier){
        if(!KitchenConfig.INSTANCE.spoilage)return 0;
        long age=Math.max(0,now-prepared);
        return age>KitchenConfig.INSTANCE.spoiledDays*24000L*multiplier?2:age>KitchenConfig.INSTANCE.freshDays*24000L*multiplier?1:0;
    }
    public static ItemStack agedStack(ItemStack stack,long now) {return agedStack(stack,now,0);}
    public static ItemStack agedStack(ItemStack stack,long now,int minimumStage) {
        if(!(stack.getItem() instanceof KitchenFoodItem))return stack;
        var tag=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
        long prepared=tag.getLongOr("kitchen_prepared",now);
        tag.putLong("kitchen_prepared",prepared);stack.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));
        int stage=Math.max(minimumStage,freshnessStage(stack,now,prepared));
        if(stage==0)return stack;
        tag.putInt("kitchen_freshness_floor",stage);stack.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));
        var item=KitchenContent.item((stage>=2?"spoiled_":"stale_")+ResourceStates.baseId(stack));
        if(item==Items.AIR)return stack;
        var result=item==stack.getItem()?stack:stack.transmuteCopy(item);
        var reasons=new java.util.ArrayList<String>();
        for(String reason:tag.getStringOr("kitchen_quality_reasons","").split(","))if(!reason.isEmpty()&&!reasons.contains(reason))reasons.add(reason);
        if(!reasons.contains("stale"))reasons.add("stale");
        stampMeal(result,Math.min(1,tag.getIntOr("kitchen_quality",3)),reasons,prepared);
        if(stage>=2){result.remove(DataComponents.FOOD);result.remove(DataComponents.CONSUMABLE);}
        return result;
    }
}
