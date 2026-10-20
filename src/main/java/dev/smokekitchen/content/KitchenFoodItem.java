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
        if(definition!=null) {
            int base=ContentCatalog.num(definition,"nutrition",1);
            int nutrition=quality<=1?Math.max(1,(int)Math.floor(base*.6)):base;
            stack.set(DataComponents.FOOD,new FoodProperties.Builder().nutrition(nutrition).saturationModifier(quality>=3?.5f:quality==2?.4f:.2f).build());
        }
    }
    @Override public void inventoryTick(ItemStack stack,ServerLevel level,Entity owner,EquipmentSlot slot) {
        super.inventoryTick(stack,level,owner,slot);
        if(level.getGameTime()%100!=0 || !KitchenConfig.INSTANCE.spoilage)return;
        var result=agedStack(stack,level.getGameTime());
        if(result==stack)return;
        if(slot!=null && owner instanceof net.minecraft.world.entity.LivingEntity living)living.setItemSlot(slot,result);
        else if(owner instanceof net.minecraft.world.entity.player.Player player)for(int i=0;i<player.getInventory().getContainerSize();i++)if(player.getInventory().getItem(i)==stack){player.getInventory().setItem(i,result);break;}
    }
    public static ItemStack agedStack(ItemStack stack,long now) {
        var tag=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
        if(!tag.contains("kitchen_prepared")) {tag.putLong("kitchen_prepared",now);stack.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));return stack;}
        long age=Math.max(0,now-tag.getLongOr("kitchen_prepared",now));
        String state=age>KitchenConfig.INSTANCE.spoiledDays*24000L?"spoiled_":age>KitchenConfig.INSTANCE.freshDays*24000L?"stale_":"";
        String target=state+ResourceStates.baseId(stack);
        var item=KitchenContent.item(target);
        if(item==Items.AIR || item==stack.getItem())return stack;
        var result=stack.transmuteCopy(item);
        result.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));
        if(!state.isEmpty())stampMeal(result,1,java.util.List.of("stale"),tag.getLongOr("kitchen_prepared",now));
        if(state.equals("spoiled_")){result.remove(DataComponents.FOOD);result.remove(DataComponents.CONSUMABLE);}
        return result;
    }
}
