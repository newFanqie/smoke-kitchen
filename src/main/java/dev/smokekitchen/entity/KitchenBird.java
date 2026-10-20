package dev.smokekitchen.entity;

import dev.smokekitchen.content.KitchenContent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public final class KitchenBird extends Chicken {
    private int layingTime=6000;
    public KitchenBird(EntityType<? extends Chicken> type,Level level) { super(type,level); }
    @Override public void aiStep() {
        eggTime=Integer.MAX_VALUE;
        super.aiStep();
        if(level() instanceof ServerLevel server && isAlive() && !isBaby() && --layingTime<=0) {
            spawnAtLocation(server,new ItemStack(KitchenContent.item(getType()==KitchenEntities.GOOSE?"goose_egg":"duck_egg")));
            layingTime=6000+getRandom().nextInt(6000);
        }
    }
    @Override public Chicken getBreedOffspring(ServerLevel level,AgeableMob partner) {
        return (Chicken)getType().create(level,EntitySpawnReason.BREEDING);
    }
    @Override public boolean isFood(ItemStack stack) { return super.isFood(stack) || KitchenContent.name(stack.getItem()).endsWith("_seeds") || stack.is(KitchenContent.item("rice_paddy")); }
    @Override public void addAdditionalSaveData(ValueOutput out) { super.addAdditionalSaveData(out); out.putInt("KitchenEggTime",layingTime); }
    @Override public void readAdditionalSaveData(ValueInput in) { super.readAdditionalSaveData(in); layingTime=Math.clamp(in.getIntOr("KitchenEggTime",6000),1,12000); }
}
