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
    @GameTest public void decantingCookedRiceReturnsExactlyOneBowl(GameTestHelper test) {
        var p=new BlockPos(1,1,1);test.setBlock(p,KitchenContent.BLOCKS.get("wok"));var player=test.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        use(test,p,player,new ItemStack(KitchenContent.item("cooked_rice")),net.minecraft.core.Direction.UP);
        var wok=test.getBlockEntity(p,KitchenBlockEntity.class);
        test.assertTrue(player.getInventory().countItem(Items.BOWL)==1&&!wok.items.getFirst().has(net.minecraft.core.component.DataComponents.USE_REMAINDER),"Pouring rice into a wok returns its bowl once, and the loose rice must not carry another bowl");test.succeed();
    }
    @GameTest public void recookingAPortionDoesNotCarryAnotherPotsOrderEvidence(GameTestHelper test) {
        var p=new BlockPos(1,1,1);test.setBlock(p,KitchenContent.BLOCKS.get("wok"));var player=test.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var source=new CookingBatch();source.add("egg",CookingBatch.Cut.WHOLE,1,false,true);source.add("rice_cooked",CookingBatch.Cut.WHOLE,1,false,true);
        var rice=new ItemStack(KitchenContent.item("cooked_rice"));KitchenBlockEntity.applyPortion(rice,source.ingredients.getLast());use(test,p,player,rice,net.minecraft.core.Direction.UP);
        var portion=test.getBlockEntity(p,KitchenBlockEntity.class).batch.ingredients.getFirst();test.assertTrue(portion.cooked==1&&portion.priorCounts.isEmpty(),"A loose portion keeps its cookedness but must record the new pot's actual order");test.succeed();
    }
    @GameTest public void preparationEvidenceSurvivesBlockEntitySave(GameTestHelper test) {
        var p=new BlockPos(1,1,1);test.setBlock(p,KitchenContent.BLOCKS.get("wok"));var wok=test.getBlockEntity(p,KitchenBlockEntity.class);
        wok.batch.temperature=130;wok.batch.oil=1;wok.batch.add("egg",CookingBatch.Cut.WHOLE,1,false,false);wok.batch.ingredients.getFirst().cooked=.6;wok.batch.add("rice_cooked",CookingBatch.Cut.WHOLE,1,false,true);wok.batch.season("vinegar");
        var loaded=(KitchenBlockEntity)BlockEntity.loadStatic(p,wok.getBlockState(),wok.saveWithFullMetadata(test.getLevel().registryAccess()),test.getLevel().registryAccess());
        test.assertTrue(loaded!=null&&loaded.batch.ingredients.getFirst().temperatureOnAdd==130&&loaded.batch.ingredients.getLast().priorCooked.get("egg")==.6&&loaded.batch.seasoningCounts.get("vinegar")==2,"Save data must preserve the evidence used to validate order, heat and finishing seasonings");test.succeed();
    }
    @GameTest public void liftingAndRemountingPreservesPotWithoutMovingItsGasCanister(GameTestHelper test) {
        var p=new BlockPos(1,1,1);test.setBlock(p,KitchenContent.BLOCKS.get("gas_stove"));
        var player=test.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        use(test,p,player,ResourceStates.gasStack(271),net.minecraft.core.Direction.NORTH);
        use(test,p,player,new ItemStack(KitchenContent.item("soup_pot")),net.minecraft.core.Direction.UP);
        use(test,p,player,new ItemStack(Items.CHICKEN),net.minecraft.core.Direction.UP);
        use(test,p,player,new ItemStack(KitchenContent.item("pot_lid")),net.minecraft.core.Direction.UP);
        var stove=test.getBlockEntity(p,KitchenBlockEntity.class);stove.batch.water=4;stove.batch.ingredients.getFirst().cooked=.42;
        stove.methodIndex=1;stove.batch.method=CookingBatch.Method.STEW;
        player.setShiftKeyDown(true);use(test,p,player,new ItemStack(KitchenContent.item("heat_cloth")),net.minecraft.core.Direction.UP);
        test.assertTrue(stove.mountedCookware.isEmpty()&&stove.batch.ingredients.isEmpty()&&ResourceStates.gasUnits(stove.items.get(26))==271,"Lifting cookware must leave its gas canister on the stove");
        ItemStack saved=ItemStack.EMPTY;for(var stack:player.getInventory())if(stack.is(KitchenContent.item("soup_pot")))saved=stack.copy();
        test.assertTrue(!saved.isEmpty(),"The full pot must be returned as a portable block item");
        player.setShiftKeyDown(false);use(test,p,player,saved,net.minecraft.core.Direction.UP);
        test.assertTrue(stove.batch.water==4&&stove.batch.ingredients.getFirst().cooked==.42&&stove.methodIndex==1&&stove.batch.method==CookingBatch.Method.STEW,"Remounting must restore water, individual cookedness and the selected cooking method");
        test.assertTrue(stove.attachedLid.equals("pot_lid")&&stove.getBlockState().getValue(dev.smokekitchen.world.KitchenBlock.LID),"The lid must survive moving the pot");test.succeed();
    }
    @GameTest public void finishingServingsKeepsTheAttachedGasCanister(GameTestHelper test) {
        var p=new BlockPos(1,1,1);test.setBlock(p,KitchenContent.BLOCKS.get("gas_stove"));
        var stove=test.getBlockEntity(p,KitchenBlockEntity.class);stove.mountedCookware="frying_pan";stove.items.set(26,ResourceStates.gasStack(271));
        stove.batch.add("egg",CookingBatch.Cut.DICE,1,false,true);stove.items.set(0,new ItemStack(Items.EGG));stove.batch.serve("fried_rice",2);
        var player=test.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        use(test,p,player,new ItemStack(Items.BOWL),net.minecraft.core.Direction.UP);
        test.assertTrue(stove.batch.ingredients.isEmpty()&&ResourceStates.gasUnits(stove.items.get(26))==271,"Finishing a meal must clear food slots without deleting the connected fuel container");test.succeed();
    }
    @GameTest public void freshnessUsesSeparateItemsAndSpoiledMealsReturnTheirBowlWhenComposted(GameTestHelper test) {
        var meal=new ItemStack(KitchenContent.item("fried_rice"));KitchenFoodItem.agedStack(meal,1000);
        var stale=KitchenFoodItem.agedStack(meal,1001+dev.smokekitchen.KitchenConfig.INSTANCE.freshDays*24000L);
        test.assertTrue(stale.is(KitchenContent.item("stale_fried_rice")),"Old meals must become the separate stale item");
        var spoiled=KitchenFoodItem.agedStack(stale,1001+dev.smokekitchen.KitchenConfig.INSTANCE.spoiledDays*24000L);
        test.assertTrue(spoiled.is(KitchenContent.item("spoiled_fried_rice"))&&!spoiled.has(net.minecraft.core.component.DataComponents.FOOD)&&ResourceStates.spent(spoiled),"Spoiled meals must be separate and inedible");
        var p=new BlockPos(1,1,1);test.setBlock(p,KitchenContent.BLOCKS.get("compost_bin"));
        var player=test.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);use(test,p,player,spoiled,net.minecraft.core.Direction.UP);
        test.assertTrue(spoiled.isEmpty()&&player.getInventory().contains(s->s.is(Items.BOWL)),"Composting a spoiled meal must return its reusable bowl");test.succeed();
    }
    @GameTest(maxTicks=80) public void vanillaCampfireHeatsTheExistingPot(GameTestHelper test) {
        var p=new BlockPos(1,1,1);test.setBlock(p,net.minecraft.world.level.block.Blocks.CAMPFIRE);
        test.setBlock(p.above(),KitchenContent.BLOCKS.get("soup_pot"));var pot=test.getBlockEntity(p.above(),KitchenBlockEntity.class);pot.batch.water=6;
        test.runAfterDelay(45,()->{test.assertTrue(pot.batch.temperature>20,"A lit vanilla campfire must supply heat without another custom furnace implementation");test.succeed();});
    }
    @GameTest(maxTicks=40) public void breakingMountedMealCannotReturnAlreadyServedRawIngredients(GameTestHelper test) {
        var p=new BlockPos(1,1,1);test.setBlock(p,KitchenContent.BLOCKS.get("brick_stove"));var stove=test.getBlockEntity(p,KitchenBlockEntity.class);
        stove.mountedCookware="wok";stove.items.set(0,new ItemStack(Items.EGG));stove.batch.add("egg",CookingBatch.Cut.DICE,1,false,true);
        stove.batch.serve("fried_rice",3);test.getLevel().destroyBlock(test.absolutePos(p),true);
        test.runAfterDelay(2,()->{test.assertItemEntityNotPresent(Items.EGG,p,2);test.assertItemEntityCountIs(KitchenContent.item("fried_rice"),p,2,2);test.succeed();});
    }
    private static void use(GameTestHelper test,BlockPos pos,net.minecraft.world.entity.player.Player player,ItemStack stack,net.minecraft.core.Direction face) {
        var station=test.getBlockEntity(pos,KitchenBlockEntity.class);station.lastAction=-100;
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,stack);
        test.useBlock(pos,player,new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(test.absolutePos(pos)),face,test.absolutePos(pos),false));
    }
    @GameTest(maxTicks=1300) public void oneCoalActuallyCooksAndReturnsVanillaChicken(GameTestHelper test) {
        var pos=new BlockPos(1,1,1);test.setBlock(pos,KitchenContent.BLOCKS.get("brick_stove"));
        var player=test.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        use(test,pos,player,new ItemStack(KitchenContent.item("frying_pan")),net.minecraft.core.Direction.UP);
        var stove=test.getBlockEntity(pos,KitchenBlockEntity.class);
        test.assertTrue(stove.mountedCookware.equals("frying_pan")&&test.getBlockState(pos.above()).isAir(),"Placing cookware must mount inside one counter-height station");
        use(test,pos,player,new ItemStack(Items.COAL),net.minecraft.core.Direction.NORTH);
        use(test,pos,player,new ItemStack(Items.CHICKEN),net.minecraft.core.Direction.UP);
        test.runAfterDelay(1100,()->{
            test.assertTrue(stove.batch.ready()&&!stove.batch.burnt(),"One coal must actually cook a whole chicken before burning it");
            test.assertTrue(stove.displayedFood(0).is(Items.COOKED_CHICKEN),"The visible item must become cooked chicken");
            player.setShiftKeyDown(true);use(test,pos,player,ItemStack.EMPTY,net.minecraft.core.Direction.UP);
            test.assertTrue(player.getInventory().contains(s->s.is(Items.COOKED_CHICKEN)),"Retrieving must give the real vanilla cooked food, not raw food carrying hidden data");
            test.succeed();
        });
    }
    @GameTest(maxTicks=1300) public void attachedGasCanisterCooksAndReturnsPartialState(GameTestHelper test) {
        var pos=new BlockPos(1,1,1);test.setBlock(pos,KitchenContent.BLOCKS.get("gas_stove"));
        var player=test.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        use(test,pos,player,ResourceStates.gasStack(600),net.minecraft.core.Direction.NORTH);
        use(test,pos,player,new ItemStack(KitchenContent.item("frying_pan")),net.minecraft.core.Direction.UP);
        use(test,pos,player,new ItemStack(Items.CHICKEN),net.minecraft.core.Direction.UP);
        var stove=test.getBlockEntity(pos,KitchenBlockEntity.class);
        test.runAfterDelay(1100,()->{
            test.assertTrue(stove.batch.ready()&&!stove.batch.burnt(),"A canister connected directly to the stove must cook real food");
            test.assertTrue(stove.items.get(26).is(KitchenContent.item("partial_gas_canister")),"A used canister must have the partial-state registered item");
            int remaining=ResourceStates.gasUnits(stove.items.get(26));
            var tag=stove.saveWithFullMetadata(test.getLevel().registryAccess());
            var restored=(KitchenBlockEntity)BlockEntity.loadStatic(pos,stove.getBlockState(),tag,test.getLevel().registryAccess());
            test.assertTrue(restored!=null&&restored.mountedCookware.equals("frying_pan")&&ResourceStates.gasUnits(restored.items.get(26))==remaining,"Saving must preserve the exact gas amount and mounted cookware");
            player.setShiftKeyDown(true);use(test,pos,player,ItemStack.EMPTY,net.minecraft.core.Direction.NORTH);
            test.assertTrue(player.getInventory().contains(s->s.is(KitchenContent.item("partial_gas_canister"))&&ResourceStates.gasUnits(s)==remaining),"The partial canister must be retrievable as its separate item with exact remaining fuel");test.succeed();
        });
    }
    @GameTest public void pipeSearchCountsPipesInsteadOfSurroundingAir(GameTestHelper test) {
        var pos=new BlockPos(1,1,1);test.setBlock(pos,KitchenContent.BLOCKS.get("gas_stove"));
        for(int i=1;i<=18;i++)test.setBlock(pos.above(i),KitchenContent.BLOCKS.get("gas_pipe"));
        test.setBlock(pos.above(19),KitchenContent.BLOCKS.get("gas_tank"));
        var tank=test.getBlockEntity(pos.above(19),KitchenBlockEntity.class);tank.fuelSeconds=600;
        var stove=test.getBlockEntity(pos,KitchenBlockEntity.class);
        test.assertTrue(stove.pullGas(100,java.util.Set.of("gas_tank"))==100 && tank.fuelSeconds==500,"An 18-pipe run must not exhaust its budget by counting air cells");test.succeed();
    }
    @GameTest public void toolAndSeasoningStatesAreSeparateItems(GameTestHelper test) {
        var knife=new ItemStack(KitchenContent.item("iron_knife"));knife.setDamageValue(1);
        var used=ResourceStates.usedStack(knife,Items.AIR);
        test.assertTrue(used.is(KitchenContent.item("used_iron_knife"))&&used.getDamageValue()==1,"A used knife must preserve exact durability on its own state item");
        used.setDamageValue(used.getMaxDamage());var broken=ResourceStates.usedStack(used,Items.AIR);
        test.assertTrue(broken.is(KitchenContent.item("broken_iron_knife"))&&ResourceStates.spent(broken),"A broken tool must have an unusable broken-state item");
        test.assertTrue(ResourceStates.gasUnits(ResourceStates.gasStack(271))==271,"Representative 50% icons must not round the actual stored fuel");
        test.succeed();
    }
    @GameTest public void coalClickIgnitesAndRetainsFuel(GameTestHelper test) {
        var p=new BlockPos(1,1,1);test.setBlock(p,KitchenContent.BLOCKS.get("brick_stove"));
        var stove=test.getBlockEntity(p,KitchenBlockEntity.class);
        var player=test.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var coal=new ItemStack(Items.COAL,2);
        var hit=new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(test.absolutePos(p)),net.minecraft.core.Direction.NORTH,test.absolutePos(p),false);
        stove.interact(player,coal,hit);
        test.assertTrue(stove.fuelSeconds==160 && stove.heat==2 && coal.getCount()==1,"A coal click must consume one coal, store fuel, and ignite");
        test.assertBlockProperty(p,dev.smokekitchen.world.KitchenBlock.HEAT,2);
        test.assertBlockProperty(p,dev.smokekitchen.world.KitchenBlock.FUELED,true);
        stove.lastAction=-100;stove.interact(player,new ItemStack(Items.CHICKEN),hit);
        test.assertTrue(stove.fuelSeconds==160 && stove.heat==2,"Holding meat must not clear already loaded fuel");
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
        test.runAfterDelay(45,()->{test.assertTrue(stove.batch.temperature>20 && stove.mountedCookware.equals("soup_pot") && tank.fuelSeconds<600,"Connected gas burner must heat mounted cookware while drawing actual gas");test.succeed();});
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
            test.assertTrue(stove.batch.temperature>20 && stove.mountedCookware.equals("clay_pot"),"Legacy pot must mount at counter height and receive heat");
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
