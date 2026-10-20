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
        var tag=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
        long now=level.getGameTime();
        if(!tag.contains("kitchen_prepared")) {tag.putLong("kitchen_prepared",now);stack.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));return;}
        long age=Math.max(0,now-tag.getLongOr("kitchen_prepared",now));
        if(age>KitchenConfig.INSTANCE.spoiledDays*24000L && !tag.getBooleanOr("kitchen_spoiled",false)) {
            stack.set(DataComponents.CUSTOM_NAME,Component.translatable("food.smoke_kitchen.spoiled",stack.getHoverName()));
            stack.remove(DataComponents.FOOD);stack.remove(DataComponents.CONSUMABLE);
            tag.putBoolean("kitchen_spoiled",true);stack.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));
        } else if(age>KitchenConfig.INSTANCE.freshDays*24000L && !tag.getBooleanOr("kitchen_stale",false)) {
            stack.set(DataComponents.CUSTOM_NAME,Component.translatable("food.smoke_kitchen.stale",stack.getHoverName()));
            stack.set(DataComponents.FOOD,new FoodProperties.Builder().nutrition(Math.max(1,nutrition/2)).saturationModifier(.25f).build());
            tag.putBoolean("kitchen_stale",true);tag.putInt("kitchen_quality",1);stack.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));
        }
    }
}
