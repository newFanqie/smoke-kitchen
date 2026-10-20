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
        if(!state.isEmpty())tag.putInt("kitchen_quality",1);
        if(state.equals("spoiled_")){result.remove(DataComponents.FOOD);result.remove(DataComponents.CONSUMABLE);}
        result.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));
        return result;
    }
}
