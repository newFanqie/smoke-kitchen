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
    @GameTest public void coalClickIgnitesAndRetainsFuel(GameTestHelper test) {
        var p=new BlockPos(1,1,1);test.setBlock(p,KitchenContent.BLOCKS.get("brick_stove"));
        var stove=test.getBlockEntity(p,KitchenBlockEntity.class);
        var player=test.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var coal=new ItemStack(Items.COAL,2);
        var hit=new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(test.absolutePos(p)),net.minecraft.core.Direction.NORTH,test.absolutePos(p),false);
        stove.interact(player,coal,hit);
        test.assertTrue(stove.fuelSeconds==80 && stove.heat==2 && coal.getCount()==1,"A coal click must consume one coal, store fuel, and ignite");
        test.assertBlockProperty(p,dev.smokekitchen.world.KitchenBlock.HEAT,2);
        test.assertBlockProperty(p,dev.smokekitchen.world.KitchenBlock.FUELED,true);
        stove.lastAction=-100;stove.interact(player,new ItemStack(Items.CHICKEN),hit);
        test.assertTrue(stove.fuelSeconds==80 && stove.heat==2,"Holding meat must not clear already loaded fuel");
        test.succeed();
    }
    @GameTest public void allSpawnEggsPointToRegisteredAnimals(GameTestHelper test) {
        KitchenEntities.ALL.forEach((id,type)->{
            var egg=KitchenContent.item(id+"_spawn_egg");
            test.assertTrue(egg instanceof net.minecraft.world.item.SpawnEggItem,"Missing spawn egg: "+id);
            test.assertTrue(net.minecraft.world.item.SpawnEggItem.spawnsEntity(new ItemStack(egg),type),"Wrong entity in spawn egg: "+id);
            test.assertTrue(type.create(test.getLevel(),EntitySpawnReason.SPAWN_ITEM_USE)!=null,"Entity cannot spawn: "+id);
        });
        test.succeed();
    }
    @GameTest public void connectedGasConservesFuelAndStopsWhenDisconnected(GameTestHelper test) {
        var p=new BlockPos(1,1,1);
        test.setBlock(p,KitchenContent.BLOCKS.get("gas_tank"));
        test.setBlock(p.east(),KitchenContent.BLOCKS.get("gas_pipe"));
        test.setBlock(p.east(2),KitchenContent.BLOCKS.get("gas_stove"));
        var tank=test.getBlockEntity(p,KitchenBlockEntity.class);tank.fuelSeconds=600;
        var stove=test.getBlockEntity(p.east(2),KitchenBlockEntity.class);stove.heat=2;
        for(int i=0;i<20;i++)stove.serverTick();
        test.assertTrue(tank.fuelSeconds+stove.fuelSeconds==598,"Only exactly two gas units must burn, without duplication");
        test.assertBlockProperty(p.east(2),dev.smokekitchen.world.KitchenBlock.HEAT,2);
        test.getLevel().removeBlock(test.absolutePos(p.east()),false);
        for(int i=0;i<220;i++)stove.serverTick();
        test.assertTrue(tank.fuelSeconds==580 && stove.fuelSeconds==0,"Disconnected pipe must not supply more gas");
        test.assertBlockProperty(p.east(2),dev.smokekitchen.world.KitchenBlock.HEAT,0);
        test.succeed();
    }
    @GameTest public void gasExtractorDepletesDeposit(GameTestHelper test) {
        var p=new BlockPos(1,1,1);test.setBlock(p,KitchenContent.BLOCKS.get("gas_deposit"));test.setBlock(p.above(),KitchenContent.BLOCKS.get("gas_extractor"));
        var deposit=test.getBlockEntity(p,KitchenBlockEntity.class);var pump=test.getBlockEntity(p.above(),KitchenBlockEntity.class);
        for(int i=0;i<20;i++)pump.serverTick();
        test.assertTrue(deposit.fuelSeconds==2340 && pump.fuelSeconds==60,"Extraction must transfer finite gas from the rock");
        deposit.fuelSeconds=0;for(int i=0;i<20;i++)pump.serverTick();
        test.assertTrue(pump.fuelSeconds==60,"Depleted deposits must not generate free gas");test.succeed();
    }
    @GameTest(maxTicks=80) public void gasLineHeatsCookware(GameTestHelper test) {
        var p=new BlockPos(1,1,1);test.setBlock(p,KitchenContent.BLOCKS.get("gas_tank"));test.setBlock(p.east(),KitchenContent.BLOCKS.get("gas_stove"));
        test.setBlock(p.east().above(),KitchenContent.BLOCKS.get("soup_pot"));
        var tank=test.getBlockEntity(p,KitchenBlockEntity.class);tank.fuelSeconds=600;
        var stove=test.getBlockEntity(p.east(),KitchenBlockEntity.class);stove.heat=2;
        var pot=test.getBlockEntity(p.east().above(),KitchenBlockEntity.class);pot.batch.water=6;
        test.runAfterDelay(45,()->{test.assertTrue(pot.batch.temperature>20 && tank.fuelSeconds<600,"Connected gas burner must heat cookware while drawing actual gas");test.succeed();});
    }
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
