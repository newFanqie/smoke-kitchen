package dev.smokekitchen.entity;

import dev.smokekitchen.content.KitchenContent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.fish.Cod;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class KitchenFish extends Cod {
    public KitchenFish(EntityType<? extends Cod> type,Level level) { super(type,level); }
    @Override public ItemStack getBucketItemStack() {
        return new ItemStack(KitchenContent.item(getType()==KitchenEntities.SHRIMP?"shrimp_bucket":"crab_bucket"));
    }
}
