package dev.smokekitchen.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.level.Level;

public final class KitchenBoar extends Pig {
    public KitchenBoar(EntityType<? extends Pig> type,Level level) { super(type,level); }
    @Override protected void registerGoals() {
        super.registerGoals(); goalSelector.addGoal(1,new MeleeAttackGoal(this,1.2,true));
        targetSelector.addGoal(1,new HurtByTargetGoal(this));
    }
    @Override public Pig getBreedOffspring(ServerLevel level,AgeableMob partner) { return (Pig)getType().create(level,EntitySpawnReason.BREEDING); }
}
