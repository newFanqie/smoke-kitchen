package dev.smokekitchen.test;

import dev.smokekitchen.content.*;
import dev.smokekitchen.cooking.CookingBatch;
import dev.smokekitchen.entity.KitchenEntities;
import dev.smokekitchen.world.KitchenBlockEntity;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class KitchenGameTests {
    @GameTest public void contentLoads(GameTestHelper test) {
        test.assertTrue(ContentCatalog.DISHES.size()==29,"All named dishes must be present");
        test.assertTrue(KitchenContent.CROPS.size()==17,"All crops must register");
        test.assertTrue(KitchenEntities.DUCK.create(test.getLevel(),EntitySpawnReason.LOAD)!=null,"Duck attributes and constructor must work");
        test.succeed();
    }
    @GameTest(maxTicks=80) public void stoveHeatsPot(GameTestHelper test) {
        var p=new BlockPos(1,1,1); test.setBlock(p,KitchenContent.BLOCKS.get("brick_stove"));
        test.setBlock(p.above(),KitchenContent.BLOCKS.get("clay_pot"));
        var stove=test.getBlockEntity(p,KitchenBlockEntity.class); stove.heat=2; stove.fuelSeconds=500;
        var pot=test.getBlockEntity(p.above(),KitchenBlockEntity.class); pot.batch.water=6;
        pot.batch.add("rice",CookingBatch.Cut.DICE,1,true,false);
        test.runAfterDelay(45,()->{
            test.assertTrue(pot.batch.temperature>20,"Adjacent stove must heat pot");
            test.assertTrue(stove.fuelSeconds<500,"Heat must consume fuel");
            test.succeed();
        });
    }
    @GameTest public void savedPotRetainsSeparateCookedness(GameTestHelper test) {
        var p=new BlockPos(1,1,1); test.setBlock(p,KitchenContent.BLOCKS.get("wok"));
        var pot=test.getBlockEntity(p,KitchenBlockEntity.class);
        pot.batch.add("egg",CookingBatch.Cut.DICE,1,false,false); pot.batch.ingredients.getFirst().cooked=1;
        pot.batch.add("beef",CookingBatch.Cut.DICE,1.6,false,false);
        var tag=pot.saveWithFullMetadata(test.getLevel().registryAccess());
        var loaded=(KitchenBlockEntity)BlockEntity.loadStatic(p,pot.getBlockState(),tag,test.getLevel().registryAccess());
        test.assertTrue(loaded!=null && loaded.batch.ingredients.size()==2,"Both portions survive saving");
        test.assertTrue(loaded.batch.ingredients.getFirst().cooked==1 && loaded.batch.ingredients.getLast().cooked==0,"Cookedness must not be shared");
        test.succeed();
    }
    @GameTest(maxTicks=40) public void breakingStationReturnsIngredient(GameTestHelper test) {
        var p=new BlockPos(1,1,1); test.setBlock(p,KitchenContent.BLOCKS.get("wok"));
        var pot=test.getBlockEntity(p,KitchenBlockEntity.class);
        pot.items.set(0,new ItemStack(Items.EGG)); pot.batch.add("egg",CookingBatch.Cut.DICE,1,false,false);
        test.getLevel().destroyBlock(test.absolutePos(p),true);
        test.runAfterDelay(2,()->{test.assertItemEntityPresent(Items.EGG,p,2);test.succeed();});
    }
    @GameTest public void doubleStoveBurnersAreIndependent(GameTestHelper test) {
        var p=new BlockPos(1,1,1);var block=KitchenContent.BLOCKS.get("double_stove");
        test.setBlock(p,block);test.setBlock(p.east(),block.defaultBlockState().setValue(dev.smokekitchen.world.DoubleStoveBlock.SECONDARY,true));
        var a=test.getBlockEntity(p,KitchenBlockEntity.class);var b=test.getBlockEntity(p.east(),KitchenBlockEntity.class);
        a.heat=1;a.fuelSeconds=300;b.heat=3;b.fuelSeconds=600;
        test.assertTrue(a!=b && a.heat!=b.heat && a.fuelSeconds!=b.fuelSeconds,"Each burner must own its settings");
        test.succeed();
    }
    @GameTest public void iceboxConsumesIceAndPreservesFoodClock(GameTestHelper test) {
        var p=new BlockPos(1,1,1);test.setBlock(p,KitchenContent.BLOCKS.get("icebox"));
        var box=test.getBlockEntity(p,KitchenBlockEntity.class);var food=new ItemStack(KitchenContent.item("cooked_rice"));
        var tag=new net.minecraft.nbt.CompoundTag();long prepared=test.getLevel().getGameTime()-24000;
        tag.putLong("kitchen_prepared",prepared);food.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.of(tag));
        box.items.set(0,food);box.items.set(1,new ItemStack(Items.ICE));box.lastCoolingTime=test.getLevel().getGameTime()-20;
        for(int i=0;i<20;i++)box.serverTick();
        test.assertTrue(box.items.get(1).isEmpty() && box.coldTicks>0,"Ice must be consumed into a finite cooling budget");
        test.assertTrue(food.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA).copyTag().getLongOr("kitchen_prepared",0)>prepared,"Cooling must offset elapsed food age");
        test.succeed();
    }
}
