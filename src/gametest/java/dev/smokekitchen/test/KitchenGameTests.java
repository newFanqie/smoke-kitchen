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
    @GameTest public void porridgeWaitsForSofteningWithoutConsumingTheBowl(GameTestHelper test) {
        var p=new BlockPos(1,1,1);test.setBlock(p,KitchenContent.BLOCKS.get("soup_pot"));var pot=test.getBlockEntity(p,KitchenBlockEntity.class);pot.batch.method=CookingBatch.Method.STEW;pot.batch.addWater(6);pot.batch.add("rice",CookingBatch.Cut.WHOLE,1,true,false);pot.batch.ingredients.getFirst().cooked=1;pot.batch.stir();pot.batch.stir();pot.items.set(0,new ItemStack(KitchenContent.item("rice")));
        var player=test.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);var bowl=new ItemStack(Items.BOWL);use(test,p,player,bowl,net.minecraft.core.Direction.UP);
        test.assertTrue(bowl.getCount()==1&&pot.batch.remainingServings==-1&&player.getInventory().countItem(KitchenContent.item("mixed_stew"))==0,"Ordinary cookedness must not irreversibly serve unsoftened porridge");pot.batch.ingredients.getFirst().cooked=1.4;use(test,p,player,bowl,net.minecraft.core.Direction.UP);test.assertTrue(player.getInventory().countItem(KitchenContent.item("rice_porridge"))==1,"Softened porridge must serve normally");test.succeed();
    }
    @GameTest public void actualCondimentsPreserveTheirIdentityAndHoneyReturnsItsBottle(GameTestHelper test) {
        var p=new BlockPos(1,1,1);test.setBlock(p,KitchenContent.BLOCKS.get("frying_pan"));var player=test.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);use(test,p,player,new ItemStack(KitchenContent.item("pepper_powder")),net.minecraft.core.Direction.UP);use(test,p,player,new ItemStack(KitchenContent.item("sesame_oil")),net.minecraft.core.Direction.UP);use(test,p,player,new ItemStack(Items.HONEY_BOTTLE),net.minecraft.core.Direction.UP);var b=test.getBlockEntity(p,KitchenBlockEntity.class).batch;
        test.assertTrue(b.seasoningCounts.containsKey("pepper_powder")&&b.seasoningCounts.containsKey("sesame_oil")&&b.seasoningCounts.containsKey("honey")&&player.getInventory().countItem(Items.GLASS_BOTTLE)==1,"Actual pouring must retain condiment type and return the consumed honey bottle once");test.succeed();
    }
    @GameTest public void tongsActuallyTurnAndSaveTheWarmTurnEvidence(GameTestHelper test) {
        var p=new BlockPos(1,1,1);test.setBlock(p,KitchenContent.BLOCKS.get("frying_pan"));var pot=test.getBlockEntity(p,KitchenBlockEntity.class);pot.batch.add("fish",CookingBatch.Cut.WHOLE,1.6,false,false);pot.batch.ingredients.getFirst().cooked=.45;var player=test.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);use(test,p,player,new ItemStack(KitchenContent.item("tongs")),net.minecraft.core.Direction.UP);
        var restored=(KitchenBlockEntity)BlockEntity.loadStatic(p,pot.getBlockState(),pot.saveWithFullMetadata(test.getLevel().registryAccess()),test.getLevel().registryAccess());test.assertTrue(restored.batch.turns==1&&restored.batch.ingredients.getFirst().turnedAfterSetting&&restored.batch.ingredients.getFirst().cookedAtTurn==.45,"Real tool interaction and saving must retain the qualified turn");test.succeed();
    }
    @GameTest public void skewersRequirePreparedMeatOnionSticksAndTheirAssemblyTool(GameTestHelper test) {
        var p=new BlockPos(1,1,1);test.setBlock(p,KitchenContent.BLOCKS.get("mixing_table"));var table=test.getBlockEntity(p,KitchenBlockEntity.class);var player=test.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);table.items.set(0,prepared(new ItemStack(Items.PORKCHOP,3),"DICE",false));table.items.set(1,prepared(new ItemStack(KitchenContent.item("onion")),"DICE",false));table.work=10;use(test,p,player,new ItemStack(KitchenContent.item("chopsticks")),net.minecraft.core.Direction.UP);test.assertTrue(table.items.stream().noneMatch(s->s.is(KitchenContent.item("raw_skewers"))),"Skewers must not appear without their sticks");table.items.set(2,new ItemStack(Items.STICK,3));use(test,p,player,new ItemStack(KitchenContent.item("chopsticks")),net.minecraft.core.Direction.UP);test.assertTrue(table.items.stream().filter(s->s.is(KitchenContent.item("raw_skewers"))).mapToInt(ItemStack::getCount).sum()==3&&table.items.stream().noneMatch(s->s.is(Items.STICK)),"Prepared inputs and three sticks must assemble exactly three skewers");test.succeed();
    }
    private static void fillPickle(GameTestHelper test,BlockPos p,net.minecraft.world.entity.player.Player player) {
        use(test,p,player,new ItemStack(Items.WATER_BUCKET),net.minecraft.core.Direction.UP);for(int i=0;i<3;i++)use(test,p,player,prepared(new ItemStack(KitchenContent.item("cabbage")),"SHRED",false),net.minecraft.core.Direction.UP);use(test,p,player,prepared(new ItemStack(KitchenContent.item("chili")),"SLICE",false),net.minecraft.core.Direction.UP);use(test,p,player,prepared(new ItemStack(KitchenContent.item("garlic")),"MINCE",false),net.minecraft.core.Direction.UP);use(test,p,player,new ItemStack(KitchenContent.item("salt")),net.minecraft.core.Direction.UP);
    }
    @GameTest(maxTicks=24200) public void actualSealedPickleCrockFinishesOnlyAfterItsFullGameTime(GameTestHelper test) {
        var p=new BlockPos(1,1,1);test.setBlock(p,KitchenContent.BLOCKS.get("pickling_crock"));var player=test.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);fillPickle(test,p,player);use(test,p,player,ItemStack.EMPTY,net.minecraft.core.Direction.UP);var crock=test.getBlockEntity(p,KitchenBlockEntity.class);
        test.runAfterDelay(23900,()->test.assertTrue(crock.items.stream().noneMatch(s->s.is(KitchenContent.item("pickled_cabbage"))),"Pickles must not finish before 1200 actual game seconds"));
        test.runAfterDelay(24100,()->{test.assertTrue(crock.items.stream().filter(s->s.is(KitchenContent.item("pickled_cabbage"))).mapToInt(ItemStack::getCount).sum()==4,"The sealed and prepared brine batch must actually produce four pickles after elapsed game ticks");test.succeed();});
    }
    @GameTest(maxTicks=1100) public void emptyCrockTimeCannotBeBankedForNewIngredients(GameTestHelper test) {
        var p=new BlockPos(1,1,1);test.setBlock(p,KitchenContent.BLOCKS.get("pickling_crock"));var player=test.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);use(test,p,player,ItemStack.EMPTY,net.minecraft.core.Direction.UP);
        test.runAfterDelay(1000,()->{use(test,p,player,ItemStack.EMPTY,net.minecraft.core.Direction.UP);fillPickle(test,p,player);use(test,p,player,ItemStack.EMPTY,net.minecraft.core.Direction.UP);var crock=test.getBlockEntity(p,KitchenBlockEntity.class);test.assertTrue(crock.work==0&&crock.items.stream().noneMatch(s->s.is(KitchenContent.item("pickled_cabbage"))),"New brine and ingredients must reset banked empty-crock work");test.succeed();});
    }
    @GameTest public void sealedCrocksRejectInsertionAndWholeCabbageCannotBePickled(GameTestHelper test) {
        var p=new BlockPos(1,1,1);test.setBlock(p,KitchenContent.BLOCKS.get("pickling_crock"));var crock=test.getBlockEntity(p,KitchenBlockEntity.class);var player=test.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);fillPickle(test,p,player);for(var stack:crock.items)if(stack.is(KitchenContent.item("cabbage")))stack.remove(net.minecraft.core.component.DataComponents.CUSTOM_DATA);use(test,p,player,ItemStack.EMPTY,net.minecraft.core.Direction.UP);crock.work=1200;for(int i=0;i<20;i++)crock.serverTick();test.assertTrue(crock.items.stream().noneMatch(s->s.is(KitchenContent.item("pickled_cabbage"))),"Whole cabbage must not pass the shredded-cabbage process");var extra=new ItemStack(KitchenContent.item("garlic"));use(test,p,player,extra,net.minecraft.core.Direction.UP);test.assertTrue(extra.getCount()==1,"A sealed lid must prevent insertion without consuming the item");test.succeed();
    }
    private static ItemStack prepared(ItemStack stack,String cut,boolean rolled){var tag=new net.minecraft.nbt.CompoundTag();tag.putString("kitchen_cut",cut);tag.putBoolean("kitchen_rolled",rolled);stack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.of(tag));return stack;}
    @GameTest public void dumplingProcessRejectsUncutMeatAndUnrolledDough(GameTestHelper test) {
        var p=new BlockPos(1,1,1);test.setBlock(p,KitchenContent.BLOCKS.get("mixing_table"));var table=test.getBlockEntity(p,KitchenBlockEntity.class);var player=test.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        table.items.set(0,new ItemStack(KitchenContent.item("dough"),2));table.items.set(1,new ItemStack(Items.PORKCHOP));table.items.set(2,new ItemStack(KitchenContent.item("cabbage")));table.work=10;
        use(test,p,player,new ItemStack(KitchenContent.item("rolling_pin")),net.minecraft.core.Direction.UP);
        test.assertTrue(table.items.stream().noneMatch(s->s.is(KitchenContent.item("raw_dumplings"))),"Whole meat and unrolled dough cannot make dumplings");
        table.items.set(0,prepared(new ItemStack(KitchenContent.item("dough"),2),"WHOLE",true));table.items.set(1,prepared(new ItemStack(Items.PORKCHOP),"MINCE",false));table.items.set(2,prepared(new ItemStack(KitchenContent.item("cabbage")),"SHRED",false));
        use(test,p,player,new ItemStack(KitchenContent.item("rolling_pin")),net.minecraft.core.Direction.UP);
        test.assertTrue(table.items.stream().filter(s->s.is(KitchenContent.item("raw_dumplings"))).mapToInt(ItemStack::getCount).sum()==2,"Actual prepared inputs must make two raw dumplings");test.succeed();
    }
    @GameTest public void pieRequiresPreparedCrustSlicedAppleAndRollingPin(GameTestHelper test) {
        var p=new BlockPos(1,1,1);test.setBlock(p,KitchenContent.BLOCKS.get("mixing_table"));var table=test.getBlockEntity(p,KitchenBlockEntity.class);var player=test.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        table.items.set(0,prepared(new ItemStack(KitchenContent.item("dough"),2),"WHOLE",true));table.items.set(1,new ItemStack(Items.APPLE,2));table.items.set(2,new ItemStack(Items.SUGAR));table.work=10;
        use(test,p,player,new ItemStack(KitchenContent.item("rolling_pin")),net.minecraft.core.Direction.UP);test.assertTrue(table.items.stream().noneMatch(s->s.is(KitchenContent.item("raw_apple_pie"))),"Whole apples cannot pass filling preparation");
        table.items.set(1,prepared(new ItemStack(Items.APPLE,2),"SLICE",false));use(test,p,player,new ItemStack(KitchenContent.item("rolling_pin")),net.minecraft.core.Direction.UP);
        test.assertTrue(table.items.stream().anyMatch(s->s.is(KitchenContent.item("raw_apple_pie"))),"Prepared crust and apple slices must make the filled pie");test.succeed();
    }
    @GameTest public void processingBowlRiceReturnsExactlyTheConsumedBowl(GameTestHelper test) {
        var p=new BlockPos(1,1,1);test.setBlock(p,KitchenContent.BLOCKS.get("fermentation_crock"));var table=test.getBlockEntity(p,KitchenBlockEntity.class);table.items.set(0,new ItemStack(KitchenContent.item("cooked_rice")));table.items.set(1,new ItemStack(KitchenContent.item("shiitake")));table.items.set(2,new ItemStack(Items.SUGAR));table.work=1200;
        use(test,p,test.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL),ItemStack.EMPTY,net.minecraft.core.Direction.UP);
        test.assertTrue(table.items.stream().filter(s->s.is(Items.BOWL)).mapToInt(ItemStack::getCount).sum()==1,"Consuming bowl rice in processing must return one bowl");test.succeed();
    }
    @GameTest public void looseCookedRiceDoesNotGenerateAnotherBowl(GameTestHelper test) {
        var rice=ResourceStates.cookedStack(new ItemStack(KitchenContent.item("rice")),true,false,test.getLevel());
        test.assertTrue(rice.is(KitchenContent.item("cooked_rice"))&&!rice.has(net.minecraft.core.component.DataComponents.USE_REMAINDER),"Cooked raw rice must become a cooked item without generating a bowl");
        var noodles=ResourceStates.cookedStack(prepared(new ItemStack(KitchenContent.item("dough")),"SHRED",true),true,false);
        test.assertTrue(net.minecraft.network.chat.Component.translatable("message.smoke_kitchen.cooked_noodles").equals(noodles.get(net.minecraft.core.component.DataComponents.CUSTOM_NAME)),"Cooked rolled noodles must no longer be labelled raw");test.succeed();
    }
    @GameTest public void bareStoveAndOvenRejectWaterWithoutConsumingTheBucket(GameTestHelper test) {
        var player=test.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);for(String id:new String[]{"gas_stove","oven"}){var p=new BlockPos(1,1,1);test.setBlock(p,KitchenContent.BLOCKS.get(id));var water=new ItemStack(Items.WATER_BUCKET);use(test,p,player,water,net.minecraft.core.Direction.UP);test.assertTrue(test.getBlockEntity(p,KitchenBlockEntity.class).batch.water==0&&water.getCount()==1,"Only a suitable vessel may hold cooking water");}test.succeed();
    }
    @GameTest public void ovenActuallyMountsTrayAndPreservesItWhenLifting(GameTestHelper test) {
        var p=new BlockPos(1,1,1);test.setBlock(p,KitchenContent.BLOCKS.get("oven"));var player=test.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);use(test,p,player,new ItemStack(KitchenContent.item("baking_tray")),net.minecraft.core.Direction.UP);var oven=test.getBlockEntity(p,KitchenBlockEntity.class);
        test.assertTrue(oven.mountedCookware.equals("baking_tray")&&oven.batch.method==CookingBatch.Method.BAKE,"A tray must really mount inside the oven");oven.batch.ovenSeconds=16;oven.fuelSeconds=80;player.setShiftKeyDown(true);use(test,p,player,new ItemStack(KitchenContent.item("heat_cloth")),net.minecraft.core.Direction.UP);
        test.assertTrue(oven.mountedCookware.isEmpty()&&oven.fuelSeconds==80&&player.getInventory().countItem(KitchenContent.item("baking_tray"))==1,"Lifting the tray must not move the oven or its fuel");test.succeed();
    }
    @GameTest public void lastSteamedServingRetainsReservoirWater(GameTestHelper test) {
        var p=new BlockPos(1,1,1);test.setBlock(p,KitchenContent.BLOCKS.get("steamer"));var pot=test.getBlockEntity(p,KitchenBlockEntity.class);pot.batch.water=5;pot.batch.add("dumplings",CookingBatch.Cut.WHOLE,1,true,true);pot.batch.add("dumplings",CookingBatch.Cut.WHOLE,1,true,true);pot.batch.salt=1;
        var player=test.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);use(test,p,player,new ItemStack(Items.BOWL),net.minecraft.core.Direction.UP);use(test,p,player,new ItemStack(Items.BOWL),net.minecraft.core.Direction.UP);
        test.assertTrue(pot.batch.ingredients.isEmpty()&&pot.batch.water==5,"Serving steamed food must not discard the steamer water reservoir");test.succeed();
    }
    @GameTest(maxTicks=40) public void sneakingToolsReachTheirVanillaItemUseEntry(GameTestHelper test) {
        var boardPos=new BlockPos(1,1,1);var stovePos=new BlockPos(2,1,1);test.setBlock(boardPos,KitchenContent.BLOCKS.get("cutting_board"));test.setBlock(stovePos,KitchenContent.BLOCKS.get("gas_stove"));
        var player=test.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);use(test,stovePos,player,new ItemStack(KitchenContent.item("soup_pot")),net.minecraft.core.Direction.UP);
        player.setShiftKeyDown(true);var knife=new ItemStack(KitchenContent.item("stone_knife"));player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,knife);
        knife.useOn(new net.minecraft.world.item.context.UseOnContext(player,net.minecraft.world.InteractionHand.MAIN_HAND,new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(test.absolutePos(boardPos)),net.minecraft.core.Direction.UP,test.absolutePos(boardPos),false)));
        test.assertTrue(test.getBlockEntity(boardPos,KitchenBlockEntity.class).cut==CookingBatch.Cut.SHRED,"The knife's real Item.useOn path must cycle the board cut while sneaking");
        test.runAfterDelay(5,()->{var cloth=new ItemStack(KitchenContent.item("heat_cloth"));player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,cloth);cloth.useOn(new net.minecraft.world.item.context.UseOnContext(player,net.minecraft.world.InteractionHand.MAIN_HAND,new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(test.absolutePos(stovePos)),net.minecraft.core.Direction.UP,test.absolutePos(stovePos),false)));test.assertTrue(test.getBlockEntity(stovePos,KitchenBlockEntity.class).mountedCookware.isEmpty(),"The heat cloth's real Item.useOn path must lift cookware while sneaking");test.succeed();});
    }
    @GameTest public void pumpkinBlockIsInsertedInsteadOfBeingPlacedOnThePot(GameTestHelper test) {
        var p=new BlockPos(1,1,1);test.setBlock(p,KitchenContent.BLOCKS.get("soup_pot"));var player=test.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        use(test,p,player,new ItemStack(Items.PUMPKIN),net.minecraft.core.Direction.UP);
        test.assertTrue(test.getBlockEntity(p,KitchenBlockEntity.class).batch.count("pumpkin")==1&&test.getBlockState(p.above()).isAir(),"An edible block item must enter the pot instead of becoming a block above it");test.succeed();
    }
    @GameTest public void aCoalBlockCanActuallyBeInsertedThroughNormalBlockUse(GameTestHelper test) {
        var p=new BlockPos(1,1,1);test.setBlock(p,KitchenContent.BLOCKS.get("brick_stove"));var player=test.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        use(test,p,player,new ItemStack(Items.COAL_BLOCK),net.minecraft.core.Direction.NORTH);
        var stove=test.getBlockEntity(p,KitchenBlockEntity.class);test.assertTrue(stove.fuelSeconds==1440&&stove.heat==2,"The vanilla coal-block item must load as fuel rather than place a coal block");test.succeed();
    }
    @GameTest public void waterBucketCoolsThePotAndReturnsOneBucket(GameTestHelper test) {
        var p=new BlockPos(1,1,1);test.setBlock(p,KitchenContent.BLOCKS.get("soup_pot"));var pot=test.getBlockEntity(p,KitchenBlockEntity.class);pot.batch.temperature=200;
        var player=test.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);use(test,p,player,new ItemStack(Items.WATER_BUCKET),net.minecraft.core.Direction.UP);
        test.assertTrue(pot.batch.water==6&&pot.batch.temperature<50&&player.getInventory().countItem(Items.BUCKET)==1,"Adding a real water bucket must cool the pot and return the container once");test.succeed();
    }
    @GameTest public void emptyHandCanOpenAndReturnTheExistingLid(GameTestHelper test) {
        var p=new BlockPos(1,1,1);test.setBlock(p,KitchenContent.BLOCKS.get("gas_stove"));var player=test.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        use(test,p,player,new ItemStack(KitchenContent.item("steamer")),net.minecraft.core.Direction.UP);use(test,p,player,new ItemStack(KitchenContent.item("pot_lid")),net.minecraft.core.Direction.UP);
        use(test,p,player,ItemStack.EMPTY,net.minecraft.core.Direction.UP);var stove=test.getBlockEntity(p,KitchenBlockEntity.class);
        test.assertTrue(stove.attachedLid.isEmpty()&&!stove.getBlockState().getValue(dev.smokekitchen.world.KitchenBlock.LID)&&player.getInventory().countItem(KitchenContent.item("pot_lid"))==1,"The existing lid must open by hand without requiring a spare lid");test.succeed();
    }
    @GameTest public void waterAtAdditionAndCoveredCookingSurviveSaving(GameTestHelper test) {
        var p=new BlockPos(1,1,1);test.setBlock(p,KitchenContent.BLOCKS.get("steamer"));var pot=test.getBlockEntity(p,KitchenBlockEntity.class);pot.batch.addWater(6);pot.batch.add("fish",CookingBatch.Cut.WHOLE,1.6,false,false);pot.batch.ingredients.getFirst().coveredCookingSeconds=17;
        var restored=(KitchenBlockEntity)BlockEntity.loadStatic(p,pot.getBlockState(),pot.saveWithFullMetadata(test.getLevel().registryAccess()),test.getLevel().registryAccess());
        test.assertTrue(restored!=null&&restored.batch.ingredients.getFirst().waterOnAdd==6&&restored.batch.ingredients.getFirst().coveredCookingSeconds==17,"Water and covered-heat evidence must remain after saving");test.succeed();
    }
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
