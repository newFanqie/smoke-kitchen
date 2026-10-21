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
    @GameTest public void vanillaWoodenPickaxeAndKitchenBoardAreDistinctRecipes(GameTestHelper test) {
        var slots=new java.util.ArrayList<ItemStack>();for(int i=0;i<9;i++)slots.add(ItemStack.EMPTY);
        for(int i=0;i<3;i++)slots.set(i,new ItemStack(Items.OAK_PLANKS));slots.set(4,new ItemStack(Items.STICK));slots.set(7,new ItemStack(Items.STICK));
        var input=net.minecraft.world.item.crafting.CraftingInput.of(3,3,slots);var manager=test.getLevel().getServer().getRecipeManager();
        var pick=manager.getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING,input,test.getLevel()).orElseThrow().value().assemble(input);
        test.assertTrue(pick.is(Items.WOODEN_PICKAXE),"The starting vanilla wooden pickaxe must remain craftable with the kitchen mod installed");
        slots.set(7,ItemStack.EMPTY);slots.set(6,new ItemStack(Items.STICK));var boardInput=net.minecraft.world.item.crafting.CraftingInput.of(3,3,slots);
        var board=manager.getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING,boardInput,test.getLevel()).orElseThrow().value().assemble(boardInput);
        test.assertTrue(board.is(KitchenContent.item("cutting_board")),"Offsetting the lower handle must craft the board with the same material cost");test.succeed();
    }
    private static net.minecraft.world.item.crafting.CraftingInput craftingExample(net.minecraft.world.item.crafting.CraftingRecipe recipe) {
        java.util.List<ItemStack> slots;int width,height;
        if(recipe instanceof net.minecraft.world.item.crafting.ShapedRecipe shaped){width=shaped.getWidth();height=shaped.getHeight();slots=shaped.getIngredients().stream().map(optional->optional.map(ingredient->new ItemStack(ingredient.items().findFirst().orElseThrow().value())).orElse(ItemStack.EMPTY)).toList();}
        else if(recipe instanceof net.minecraft.world.item.crafting.ShapelessRecipe){var ingredients=recipe.placementInfo().ingredients();width=Math.min(3,ingredients.size());height=(ingredients.size()+2)/3;slots=new java.util.ArrayList<>();for(var ingredient:ingredients)slots.add(new ItemStack(ingredient.items().findFirst().orElseThrow().value()));while(slots.size()<width*height)slots.add(ItemStack.EMPTY);}
        else return null;
        return net.minecraft.world.item.crafting.CraftingInput.of(width,height,slots);
    }
    @GameTest public void kitchenCraftingInputsDoNotShadowAnyVanillaRecipe(GameTestHelper test) {
        var recipes=test.getLevel().getServer().getRecipeManager().getRecipes();int checked=0;var clashes=new java.util.ArrayList<String>();
        for(var holder:recipes)if(holder.id().identifier().getNamespace().equals("smoke_kitchen")&&holder.value() instanceof net.minecraft.world.item.crafting.CraftingRecipe recipe) {
            var input=craftingExample(recipe);test.assertTrue(input!=null,"Every kitchen crafting recipe needs an auditable ingredient layout");checked++;
            for(var other:recipes)if(other.id().identifier().getNamespace().equals("minecraft")&&other.value() instanceof net.minecraft.world.item.crafting.CraftingRecipe vanilla){
                if(vanilla.matches(input,test.getLevel()))clashes.add(holder.id().identifier()+" overlaps "+other.id().identifier());
                var nativeInput=craftingExample(vanilla);
                if(nativeInput!=null&&recipe.matches(nativeInput,test.getLevel()))clashes.add(other.id().identifier()+" is accepted by "+holder.id().identifier());
            }
        }
        test.assertTrue(clashes.isEmpty(),"Crafting collisions: "+String.join("; ",clashes));test.assertTrue(checked>50,"The audit must examine the actual loaded kitchen crafting recipes");test.succeed();
    }
    @GameTest public void riceNeedsRootWaterForNaturalGrowthAndBonemeal(GameTestHelper test) {
        var p=new BlockPos(1,1,1);var crop=KitchenContent.CROPS.get("rice_paddy");var world=test.getLevel();var abs=test.absolutePos(p);
        test.setBlock(p.below(),net.minecraft.world.level.block.Blocks.FARMLAND);test.setBlock(p,crop);
        for(int i=0;i<100;i++){test.randomTick(p);crop.growCrops(world,abs,world.getBlockState(abs));}
        test.assertTrue(crop.getAge(world.getBlockState(abs))==0&&!crop.isValidBonemealTarget(world,abs,world.getBlockState(abs)),"Neither natural ticks nor bone meal can grow rice without water");
        test.setBlock(p.offset(2,-1,0),net.minecraft.world.level.block.Blocks.WATER);crop.growCrops(world,abs,world.getBlockState(abs));int age=crop.getAge(world.getBlockState(abs));
        test.assertTrue(age>0&&!world.getBlockState(abs).getValue(dev.smokekitchen.world.KitchenCropBlock.WILD),"Root water must permit ordinary cultivated rice growth");
        test.setBlock(p.offset(2,-1,0),net.minecraft.world.level.block.Blocks.STONE);crop.growCrops(world,abs,world.getBlockState(abs));test.assertTrue(crop.getAge(world.getBlockState(abs))==age&&world.getBlockState(abs).canSurvive(world,abs),"Removing irrigation must pause rather than destroy a cultivated seedling");test.succeed();
    }
    @GameTest(maxTicks=2000) public void shiitakeNeedsShadeAndMoistureButCanLiveOnNativePodzol(GameTestHelper test) {
        var p=new BlockPos(1,1,1);var crop=KitchenContent.CROPS.get("shiitake");var world=test.getLevel();var abs=test.absolutePos(p);
        test.setBlock(p.below(),net.minecraft.world.level.block.Blocks.PODZOL);test.setBlock(p,crop);
        // A complete opaque box gives a deterministic shaded environment independent of the world's time.
        for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++){test.setBlock(p.offset(x,2,z),net.minecraft.world.level.block.Blocks.STONE);if(Math.abs(x)==1||Math.abs(z)==1)for(int y=0;y<2;y++)test.setBlock(p.offset(x,y,z),net.minecraft.world.level.block.Blocks.STONE);}
        test.succeedWhen(()->{test.assertTrue(crop.growingConditions(world,abs),"Waiting for the opaque room lighting to settle");crop.growCrops(world,abs,world.getBlockState(abs));test.assertTrue(crop.getAge(world.getBlockState(abs))>0,"Podzol in shade must support mushroom cultivation without farmland: state="+world.getBlockState(abs)+", soil="+world.getBlockState(abs.below())+", sky="+world.canSeeSky(abs)+", light="+world.getMaxLocalRawBrightness(abs)+", conditions="+crop.growingConditions(world,abs));test.setBlock(p.below(),net.minecraft.world.level.block.Blocks.FARMLAND);test.assertTrue(!crop.growingConditions(world,abs),"Dry farmland alone must not replace moisture for mushrooms");test.setBlock(p.offset(1,-1,0),net.minecraft.world.level.block.Blocks.WATER);test.assertTrue(crop.growingConditions(world,abs),"Watered and shaded farmland must be a renewable mushroom alternative");});
    }
    @GameTest public void wildPlantsKeepNaturalSoilAndHarvestEnoughSeedToExpand(GameTestHelper test) {
        var p=new BlockPos(1,1,1);var crop=KitchenContent.CROPS.get("tomato");var world=test.getLevel();var abs=test.absolutePos(p);test.setBlock(p.below(),net.minecraft.world.level.block.Blocks.GRASS_BLOCK);var state=crop.getStateForAge(7).setValue(dev.smokekitchen.world.KitchenCropBlock.WILD,true);test.setBlock(p,state);
        test.assertTrue(state.canSurvive(world,abs)&&!crop.defaultBlockState().canSurvive(world,abs),"Only a generated wild plant may occupy natural grass; player seeds require cultivated soil");
        var drops=net.minecraft.world.level.block.Block.getDrops(state,world,abs,null);int seeds=drops.stream().filter(s->s.is(KitchenContent.item("tomato_seeds"))).mapToInt(ItemStack::getCount).sum(),fruit=drops.stream().filter(s->s.is(KitchenContent.item("tomato"))).mapToInt(ItemStack::getCount).sum();test.assertTrue(seeds>=2&&seeds<=3&&fruit>=2&&fruit<=3,"Mature harvest must provide food plus enough actual seeds to expand a field");test.assertTrue(state.is(net.minecraft.tags.BlockTags.MAINTAINS_FARMLAND),"Native farmland must recognize kitchen crops instead of reverting dry occupied beds to dirt");var rice=KitchenContent.CROPS.get("rice_paddy");test.setBlock(p.below(),net.minecraft.world.level.block.Blocks.MUD);test.setBlock(p.offset(1,-1,0),net.minecraft.world.level.block.Blocks.WATER);var wildRice=rice.getStateForAge(7).setValue(dev.smokekitchen.world.KitchenCropBlock.WILD,true);test.setBlock(p,wildRice);test.assertTrue(wildRice.canSurvive(world,abs),"Native mangrove mud beside water must support generated wild rice");test.succeed();
    }
    @GameTest public void wildCropHabitatsCoverEveryCropWithoutGlobalSeedMixing(GameTestHelper test) {
        var habitats=dev.smokekitchen.world.KitchenWildCrops.HABITATS;test.assertTrue(habitats.keySet().equals(KitchenContent.CROPS.keySet()),"All seventeen crops need an explicit natural habitat");var biomes=test.getLevel().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.BIOME);var river=dev.smokekitchen.world.KitchenWildCrops.seedsFor(biomes.getOrThrow(net.minecraft.world.level.biome.Biomes.RIVER));var desert=dev.smokekitchen.world.KitchenWildCrops.seedsFor(biomes.getOrThrow(net.minecraft.world.level.biome.Biomes.DESERT));test.assertTrue(river.contains("rice_paddy")&&!desert.contains("rice_paddy")&&desert.equals(java.util.List.of("cumin")),"River rice and desert-edge cumin must not become the same global grass seed lottery");test.succeed();
    }
    @GameTest(padding=3) public void farmsteadReallyContainsIrrigatedRice(GameTestHelper test) {
        var world=test.getLevel();var origin=test.absolutePos(new BlockPos(0,1,0));for(int x=-1;x<=7;x++)for(int z=-1;z<=9;z++){world.setBlock(origin.offset(x,-2,z),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),2);world.setBlock(origin.offset(x,-1,z),net.minecraft.world.level.block.Blocks.GRASS_BLOCK.defaultBlockState(),2);world.setBlock(origin.offset(x,0,z),net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),2);}
        boolean placed=new dev.smokekitchen.world.KitchenBuildingFeature("farmstead").place(new net.minecraft.world.level.levelgen.feature.FeaturePlaceContext<>(java.util.Optional.empty(),world,world.getChunkSource().getGenerator(),world.getRandom(),origin,net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration.INSTANCE));test.assertTrue(placed&&world.isEmptyBlock(origin.offset(1,5,1)),"A valid flat farmstead must generate a courtyard open to the sky");int rice=0;for(int x=1;x<6;x++)for(int z=1;z<4;z++){var pos=origin.offset(x,1,z);if(world.getBlockState(pos).is(KitchenContent.CROPS.get("rice_paddy"))){rice++;test.assertTrue(dev.smokekitchen.world.KitchenCropBlock.nearWater(world,pos),"Every farmstead rice plant needs real root-level irrigation");}}test.assertTrue(rice==6,"Farmstead crop indexing must guarantee six rice plants");test.succeed();
    }
    @GameTest public void millingMergesPreviousOutputBeforeReusingTheConsumedInputSlot(GameTestHelper test) {
        var p=new BlockPos(1,1,1);test.setBlock(p,KitchenContent.BLOCKS.get("hand_mill"));var player=test.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);for(int i=0;i<4;i++)use(test,p,player,new ItemStack(KitchenContent.item("salt_ore_chunk")),net.minecraft.core.Direction.UP);for(int i=0;i<4;i++)use(test,p,player,ItemStack.EMPTY,net.minecraft.core.Direction.UP);var mill=test.getBlockEntity(p,KitchenBlockEntity.class);test.assertTrue(mill.items.stream().filter(x->x.is(KitchenContent.item("salt_crystals"))).count()==1&&mill.items.stream().filter(x->x.is(KitchenContent.item("salt_crystals"))).mapToInt(ItemStack::getCount).sum()==16,"Repeated milling must merge all sixteen crystals even when the input slot becomes empty");test.assertTrue(KitchenBlockEntity.ingredient(new ItemStack(KitchenContent.item("rice_paddy"))).isEmpty(),"Unhulled grain must require the mill before it can become rice");test.succeed();
    }
    @GameTest public void cookedRiceTransitionRetainsEvidenceWithoutCreatingABowl(GameTestHelper test) {
        var a=new BlockPos(1,1,1);var b=new BlockPos(2,1,1);test.setBlock(a,KitchenContent.BLOCKS.get("soup_pot"));test.setBlock(b,KitchenContent.BLOCKS.get("wok"));var source=test.getBlockEntity(a,KitchenBlockEntity.class);source.batch.add("rice",CookingBatch.Cut.WHOLE,1,true,false);var p=source.batch.ingredients.getFirst();p.cooked=1.5;p.charred=.2;p.maximumTemperature=100;p.lowHeatSeconds=15;source.items.set(0,new ItemStack(KitchenContent.item("rice")));var player=test.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);player.setShiftKeyDown(true);use(test,a,player,ItemStack.EMPTY,net.minecraft.core.Direction.UP);var rice=player.getInventory().getNonEquipmentItems().stream().filter(x->x.is(KitchenContent.item("cooked_rice"))).findFirst().orElseThrow();player.setShiftKeyDown(false);use(test,b,player,rice,net.minecraft.core.Direction.UP);var q=test.getBlockEntity(b,KitchenBlockEntity.class).batch.ingredients.getFirst();test.assertTrue(q.id.equals("rice_cooked")&&q.cooked==1.5&&q.charred==.2&&q.lowHeatSeconds==15&&!q.needsWater,"Rice identity transition must retain history and permit dry frying");test.assertTrue(player.getInventory().countItem(Items.BOWL)==0,"Loose cooked rice must not create a bowl");test.succeed();
    }
    @GameTest(maxTicks=1500) public void actualLowHeatRendersFiniteFatAndRetainsTheJar(GameTestHelper test) {
        var p=new BlockPos(1,1,1);test.setBlock(p,KitchenContent.BLOCKS.get("clay_stove"));var player=test.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);player.getInventory().setSelectedSlot(8);use(test,p,player,new ItemStack(KitchenContent.item("clay_pot")),net.minecraft.core.Direction.UP);for(int i=0;i<3;i++)use(test,p,player,new ItemStack(KitchenContent.item("rendered_fat")),net.minecraft.core.Direction.UP);use(test,p,player,new ItemStack(Items.COAL),net.minecraft.core.Direction.NORTH);for(int i=0;i<3;i++)use(test,p,player,ItemStack.EMPTY,net.minecraft.core.Direction.NORTH);use(test,p,player,new ItemStack(KitchenContent.item("empty_jar")),net.minecraft.core.Direction.UP);test.assertTrue(test.getBlockEntity(p,KitchenBlockEntity.class).batch.ingredients.size()==3,"Raw fat must not instantly make oil");
        test.runAfterDelay(1450,()->{var stove=test.getBlockEntity(p,KitchenBlockEntity.class);test.assertTrue(stove.batch.ready()&&stove.heat==1,"Actual low heat must render in a dry clay pot");use(test,p,player,new ItemStack(KitchenContent.item("empty_jar")),net.minecraft.core.Direction.UP);use(test,p,player,new ItemStack(KitchenContent.item("empty_jar")),net.minecraft.core.Direction.UP);var inv=player.getInventory();test.assertTrue(stove.batch.ingredients.isEmpty()&&inv.countItem(KitchenContent.item("lard"))==1&&inv.countItem(KitchenContent.item("used_lard"))==1&&inv.countItem(KitchenContent.item("fat_cracklings"))==3,"Three fat must yield 8+4 oil uses and three cracklings");var oil=inv.getNonEquipmentItems().stream().filter(x->x.is(KitchenContent.item("used_lard"))).findFirst().orElseThrow();test.assertTrue(oil.getDamageValue()==4,"Half jar must retain exactly four uses");for(int slot=0;slot<36;slot++)if(inv.getItem(slot)==oil)inv.setItem(slot,ItemStack.EMPTY);for(int i=0;i<4;i++){use(test,p,player,oil,net.minecraft.core.Direction.UP);oil=player.getMainHandItem();}test.assertTrue(player.getMainHandItem().is(KitchenContent.item("empty_jar"))&&stove.batch.oil==4,"Four oil additions must return the original empty jar");test.succeed();});
    }
    @GameTest public void scorchedOrWetFatCannotProduceUsableOil(GameTestHelper test) {
        var p=new BlockPos(1,1,1);test.setBlock(p,KitchenContent.BLOCKS.get("frying_pan"));var pot=test.getBlockEntity(p,KitchenBlockEntity.class);pot.batch.add("fat",CookingBatch.Cut.WHOLE,1,false,true);pot.items.set(0,new ItemStack(KitchenContent.item("rendered_fat")));var player=test.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);for(int i=0;i<2;i++){pot.batch.water=i==0?1:0;pot.batch.ingredients.getFirst().maximumTemperature=i==0?100:200;var jar=new ItemStack(KitchenContent.item("empty_jar"));use(test,p,player,jar,net.minecraft.core.Direction.UP);test.assertTrue(jar.getCount()==1&&pot.batch.ingredients.size()==1,"Wet or overheated fat must retain its input and jar");}test.succeed();
    }
    @GameTest public void waterJugMeasuresIntoProcessingAndSaltPanCanReturnWrongWater(GameTestHelper test) {
        var p=new BlockPos(1,1,1);test.setBlock(p,KitchenContent.BLOCKS.get("mixing_bowl"));var player=test.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);var jug=new ItemStack(KitchenContent.item("water_jug"));for(int i=0;i<3;i++){use(test,p,player,jug,net.minecraft.core.Direction.UP);jug=player.getMainHandItem();}test.assertTrue(test.getBlockEntity(p,KitchenBlockEntity.class).batch.water==3&&jug.getDamageValue()==3,"Processing stations must accept exact single jug measures");var panPos=new BlockPos(2,1,1);test.setBlock(panPos,KitchenContent.BLOCKS.get("salt_pan"));use(test,panPos,player,new ItemStack(Items.WATER_BUCKET),net.minecraft.core.Direction.UP);var pan=test.getBlockEntity(panPos,KitchenBlockEntity.class);pan.work=120;use(test,panPos,player,ItemStack.EMPTY,net.minecraft.core.Direction.UP);test.assertTrue(pan.items.stream().noneMatch(x->x.is(KitchenContent.item("salt_crystals"))),"Ordinary water must not create salt");use(test,panPos,player,new ItemStack(Items.BUCKET),net.minecraft.core.Direction.UP);test.assertTrue(player.getMainHandItem().is(Items.WATER_BUCKET)&&pan.batch.water==0&&pan.work==0&&!pan.saltwater,"Wrong water must be recoverable without another bucket");test.succeed();
    }
    @GameTest(maxTicks=2500) public void saltwaterActuallyEvaporatesOverAVanillaCampfire(GameTestHelper test) {
        var p=new BlockPos(1,2,1);test.setBlock(p.below(),net.minecraft.world.level.block.Blocks.CAMPFIRE);test.setBlock(p,KitchenContent.BLOCKS.get("salt_pan"));var player=test.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);use(test,p,player,new ItemStack(KitchenContent.item("saltwater_bucket")),net.minecraft.core.Direction.UP);test.assertTrue(player.getMainHandItem().is(Items.BUCKET),"Adding saltwater must retain the bucket");test.runAfterDelay(2450,()->{var pan=test.getBlockEntity(p,KitchenBlockEntity.class);test.assertTrue(pan.items.stream().filter(x->x.is(KitchenContent.item("salt_crystals"))).mapToInt(ItemStack::getCount).sum()==4&&pan.batch.water==0&&!pan.saltwater,"Actual campfire heat must evaporate six water measures into four crystals");test.succeed();});
    }
    @GameTest(maxTicks=24200) public void firstStarterNeedsNoOldStarterAndReturnsOnlyItsActualBowl(GameTestHelper test) {
        var p=new BlockPos(1,1,1);test.setBlock(p,KitchenContent.BLOCKS.get("fermentation_crock"));var player=test.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);for(String id:new String[]{"cooked_rice","shiitake"})use(test,p,player,new ItemStack(KitchenContent.item(id)),net.minecraft.core.Direction.UP);use(test,p,player,new ItemStack(Items.SUGAR),net.minecraft.core.Direction.UP);use(test,p,player,ItemStack.EMPTY,net.minecraft.core.Direction.UP);test.runAfterDelay(24100,()->{var crock=test.getBlockEntity(p,KitchenBlockEntity.class);test.assertTrue(crock.items.stream().filter(x->x.is(KitchenContent.item("starter"))).mapToInt(ItemStack::getCount).sum()==2&&crock.items.stream().filter(x->x.is(Items.BOWL)).mapToInt(ItemStack::getCount).sum()==1,"First starter must really ferment without an old starter and return exactly the consumed bowl");test.succeed();});
    }
    @GameTest public void plainPorridgeServesWithGoodQualityWithoutSaltOrSoy(GameTestHelper test) {
        var p=new BlockPos(1,1,1);test.setBlock(p,KitchenContent.BLOCKS.get("soup_pot"));var pot=test.getBlockEntity(p,KitchenBlockEntity.class);pot.batch.method=CookingBatch.Method.STEW;pot.batch.addWater(6);pot.batch.add("rice",CookingBatch.Cut.WHOLE,1,true,false);pot.batch.ingredients.getFirst().cooked=1.4;pot.batch.stir();pot.batch.stir();pot.items.set(0,new ItemStack(KitchenContent.item("rice")));var player=test.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);use(test,p,player,new ItemStack(Items.BOWL),net.minecraft.core.Direction.UP);var meal=player.getInventory().getNonEquipmentItems().stream().filter(x->x.is(KitchenContent.item("rice_porridge"))).findFirst().orElseThrow();var tag=meal.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA).copyTag();test.assertTrue(tag.getIntOr("kitchen_quality",-1)==3&&!tag.getStringOr("kitchen_quality_reasons","").contains("bland"),"Plain porridge must not require savory seasoning to retain proper food value");test.succeed();
    }
    @GameTest public void staleQualityDoesNotRenewTheFoodClockOrRestoreItsNutrition(GameTestHelper test) {
        var meal=new ItemStack(KitchenContent.item("mixed_stir_fry"));long prepared=10;KitchenFoodItem.stampMeal(meal,3,java.util.List.of(),prepared);int fresh=meal.get(net.minecraft.core.component.DataComponents.FOOD).nutrition();var aged=KitchenFoodItem.agedStack(meal,prepared+(dev.smokekitchen.KitchenConfig.INSTANCE.freshDays+1)*24000L);var tag=aged.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA).copyTag();test.assertTrue(aged.is(KitchenContent.item("stale_mixed_stir_fry"))&&tag.getLongOr("kitchen_prepared",0)==prepared&&tag.getStringOr("kitchen_quality_reasons","").equals("stale"),"Declining quality must explain age while retaining the original prepared time");test.assertTrue(aged.get(net.minecraft.core.component.DataComponents.FOOD).nutrition()<fresh,"Stale quality cannot retain a fresh good meal's full food value");test.succeed();
    }
    @GameTest public void looseCookedMeatRetainsCutsAndPreparationWhenReturningToAnotherPan(GameTestHelper test) {
        var a=new BlockPos(1,1,1);var b=new BlockPos(2,1,1);test.setBlock(a,KitchenContent.BLOCKS.get("frying_pan"));test.setBlock(b,KitchenContent.BLOCKS.get("frying_pan"));var first=test.getBlockEntity(a,KitchenBlockEntity.class);var second=test.getBlockEntity(b,KitchenBlockEntity.class);var player=test.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        first.batch.temperature=190;first.batch.add("pork",CookingBatch.Cut.DICE,1.6,false,false);var p=first.batch.ingredients.getFirst();p.cooked=1.2;p.charred=.2;p.turnedAfterSetting=true;p.cookedAtTurn=.4;p.marinatedSeconds=12;p.caramelOnAdd=true;first.items.set(0,new ItemStack(Items.PORKCHOP));player.setShiftKeyDown(true);use(test,a,player,ItemStack.EMPTY,net.minecraft.core.Direction.UP);
        var loose=player.getInventory().getNonEquipmentItems().stream().filter(s->s.is(Items.COOKED_PORKCHOP)).findFirst().orElseThrow();test.assertTrue(loose.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA).copyTag().getStringOr("kitchen_cut","").equals("DICE"),"Vanilla cooked output must retain its actual cut");player.setShiftKeyDown(false);use(test,b,player,loose,net.minecraft.core.Direction.UP);var q=second.batch.ingredients.getFirst();test.assertTrue(q.cooked==1.2&&q.charred==.2&&q.cut==CookingBatch.Cut.DICE&&q.turnedAfterSetting&&q.caramelOnAdd&&q.marinatedSeconds==12&&second.batch.maximumFoodTemperature>=190,"Loose food must preserve its preparation and heat history in another pan");test.assertTrue(first.batch.ingredients.isEmpty()&&first.batch.maximumFoodTemperature==0,"Removing the last portion must clear the empty batch's food-temperature history");test.succeed();
    }
    @GameTest public void aHotVegetableCannotBecomeColdSaladByChangingContainers(GameTestHelper test) {
        var p=new BlockPos(1,1,1);test.setBlock(p,KitchenContent.BLOCKS.get("mixing_bowl"));var bowl=test.getBlockEntity(p,KitchenBlockEntity.class);var player=test.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);var hot=new CookingBatch.Portion("cucumber",CookingBatch.Cut.DICE,1,false,true);hot.maximumTemperature=100;hot.cooked=1;var stack=prepared(new ItemStack(KitchenContent.item("cucumber")),"DICE",false);KitchenBlockEntity.applyPortion(stack,hot);use(test,p,player,stack,net.minecraft.core.Direction.UP);test.assertTrue(bowl.batch.maximumFoodTemperature>=100,"Cold new cookware must not erase the previous heating of loose vegetables");test.succeed();
    }
    @GameTest public void servingQualityChangesFoodValueAndAddsReadableReasons(GameTestHelper test) {
        int[] values=new int[2];for(int i=0;i<2;i++){var p=new BlockPos(1+i,1,1);test.setBlock(p,KitchenContent.BLOCKS.get("wok"));var pot=test.getBlockEntity(p,KitchenBlockEntity.class);pot.batch.oil=1;pot.batch.salt=i==0?1:4;for(int j=0;j<4;j++){pot.batch.add("chicken",CookingBatch.Cut.DICE,1.6,false,true);pot.items.set(j,new ItemStack(Items.CHICKEN));}pot.batch.stir();pot.batch.stir();pot.batch.stir();var player=test.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);use(test,p,player,new ItemStack(Items.BOWL),net.minecraft.core.Direction.UP);var meal=player.getInventory().getNonEquipmentItems().stream().filter(s->s.is(KitchenContent.item("mixed_stir_fry"))).findFirst().orElseThrow();var data=meal.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA).copyTag();test.assertTrue(data.getIntOr("kitchen_quality",-1)==(i==0?3:1),"Correct mixing and excess salt must produce different quality");if(i==1)test.assertTrue(data.getStringOr("kitchen_quality_reasons","").contains("salty"),"The food must retain why it is poor");values[i]=meal.get(net.minecraft.core.component.DataComponents.FOOD).nutrition();}test.assertTrue(values[0]>values[1],"Poor cooking must affect the actual food component, not only its tooltip");test.succeed();
    }
    @GameTest public void optionalBurnsBlockHotBareHandsAndDisablingThemRestoresRetrieval(GameTestHelper test) {
        boolean original=dev.smokekitchen.KitchenConfig.INSTANCE.burns;try{var p=new BlockPos(1,1,1);test.setBlock(p,KitchenContent.BLOCKS.get("frying_pan"));var pan=test.getBlockEntity(p,KitchenBlockEntity.class);pan.batch.temperature=150;pan.batch.add("chicken",CookingBatch.Cut.WHOLE,1.6,false,true);pan.items.set(0,new ItemStack(Items.CHICKEN));var player=test.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);player.setShiftKeyDown(true);dev.smokekitchen.KitchenConfig.INSTANCE.burns=true;use(test,p,player,ItemStack.EMPTY,net.minecraft.core.Direction.UP);test.assertTrue(pan.batch.ingredients.size()==1,"Hot bare-hand contact must not move ingredients");dev.smokekitchen.KitchenConfig.INSTANCE.burns=false;use(test,p,player,ItemStack.EMPTY,net.minecraft.core.Direction.UP);test.assertTrue(pan.batch.ingredients.isEmpty()&&player.getInventory().countItem(Items.COOKED_CHICKEN)==1,"Disabling optional burns must preserve normal cooking retrieval");}finally{dev.smokekitchen.KitchenConfig.INSTANCE.burns=original;}test.succeed();
    }
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
