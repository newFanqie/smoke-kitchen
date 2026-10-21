package dev.smokekitchen.test;

import dev.smokekitchen.content.*;
import dev.smokekitchen.cooking.CookingBatch;
import dev.smokekitchen.entity.KitchenEntities;
import dev.smokekitchen.world.KitchenBlockEntity;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.Vec3;
import java.util.function.Predicate;

/** User-authorized raw fixture. Tools, devices, loot, processing, cooking and meals are actual player actions. */
public class FirstMealFlowTest implements FabricClientGameTest {
    protected TestWorldSave savedWorld;
    protected String[] extraRawMaterials(){return new String[0];}
    protected boolean continueWithTransfer(SurvivalPlayer p){return false;}
    private static final BlockPos TABLE=new BlockPos(0,100,0),FURNACE=new BlockPos(-2,100,-1),STOVE=new BlockPos(2,100,0),BOARD=new BlockPos(2,100,-2),MILL=new BlockPos(-2,100,1),ORE=new BlockPos(-3,100,3),WATER=new BlockPos(4,99,2);
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    private static KitchenBlockEntity station(MinecraftServer g,BlockPos pos){return (KitchenBlockEntity)g.overworld().getBlockEntity(pos);}
    private static void until(ClientGameTestContext c,TestServerContext s,Predicate<MinecraftServer> condition,int ticks,String message){
        for(int n=0;n<ticks;n+=20){boolean[] done={false};s.runOnServer(g->done[0]=condition.test(g));if(done[0])return;c.waitTicks(20);}boolean[] done={false};s.runOnServer(g->done[0]=condition.test(g));check(done[0],message);
    }
    private static void mount(SurvivalPlayer p,String id,BlockPos pos){p.select(id);p.click(pos.below(),Direction.UP);}
    private static void take(SurvivalPlayer p,BlockPos pos){p.emptyHand();p.walkNear(pos);p.c.getInput().holdKey(o->o.keyShift);p.c.waitTicks(3);p.click(pos,Direction.UP);p.c.getInput().releaseKey(o->o.keyShift);p.c.waitTicks(3);}
    private static void inventory(ClientGameTestContext c,String file){c.runOnClient(client->client.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(client.player)));c.waitTicks(4);c.takeScreenshot(file);c.runOnClient(client->{client.player.closeContainer();client.setScreen(null);});c.waitTicks(3);}
    private static void kill(SurvivalPlayer p,EntityType<?> type,Vec3 around){
        p.closeMenu();p.select("stone_axe");
        for(int hit=0;hit<8;hit++){
            Integer id=p.c.computeOnClient(client->{for(var entity:client.level.entitiesForRendering())if(entity.getType()==type&&entity.isAlive())return entity.getId();return null;});if(id==null)break;
            var pos=p.c.computeOnClient(client->client.level.getEntity(id).blockPosition());p.walkNear(pos,2.5);
            p.c.runOnClient(client->{var entity=client.level.getEntity(id);check(entity!=null&&client.player.distanceTo(entity)<3,"The real axe strike must be within ordinary player reach");client.gameMode.attack(client.player,entity);client.player.swing(InteractionHand.MAIN_HAND);});p.c.waitTicks(25);
        }
        p.c.waitTicks(22);p.pickUpAround(around);
    }
    private static void cut(SurvivalPlayer p,String id){
        for(int i=0;i<2;i++){
            p.select(id,stack->!stack.getOrDefault(DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.EMPTY).copyTag().contains("kitchen_cut"));p.click(BOARD,Direction.UP);p.select("smoke_kitchen:stone_knife");for(int click=0;click<4;click++)p.click(BOARD,Direction.UP);
        }
    }
    @Override public void runTest(ClientGameTestContext c){
        TestWorldSave save;double[] savedWater={0};int[] savedFuel={0};
        try(var world=c.worldBuilder().create()){
            save=world.getWorldSave();savedWorld=save;var s=world.getServer();var p=new SurvivalPlayer(c,s);
            c.runOnClient(client->client.options.guiScale().set(2));
            s.runCommand("time set day");s.runCommand("weather clear");s.runCommand("fill -6 99 -5 7 99 6 minecraft:smooth_stone");s.runCommand("fill -6 100 -5 7 105 6 minecraft:air");s.runCommand("tp @a 0.5 100 -2 0 25");s.runCommand("gamemode survival @a");
            // Base resources only. No tools, kitchen blocks, processed seasoning, meat or meals are granted.
            for(String raw:new String[]{"minecraft:oak_log 8","minecraft:cobblestone 32","minecraft:clay_ball 12","minecraft:coal 5","minecraft:iron_ingot 3","minecraft:sand 3","smoke_kitchen:tomato 2","minecraft:book 1","minecraft:wheat_seeds 1"})s.runCommand("give @a "+raw);c.waitTicks(10);
            for(String raw:extraRawMaterials())s.runCommand("give @a "+raw);c.waitTicks(4);
            if(net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("iris"))c.runOnClient(client->{try{var api=Class.forName("net.irisshaders.iris.api.v0.IrisApi");check((Boolean)api.getMethod("isShaderPackInUse").invoke(api.getMethod("getInstance").invoke(null)),"The first-meal Iris check must use an active shader pack");}catch(ReflectiveOperationException e){throw new AssertionError(e);}});
            while(p.count("oak_planks")<24)p.craft("oak_planks","oak_log");while(p.count("stick")<16)p.craft("stick","oak_planks","","oak_planks","");p.craft("crafting_table","oak_planks","oak_planks","oak_planks","oak_planks");p.closeMenu();mount(p,"crafting_table",TABLE);p.emptyHand();p.click(TABLE,Direction.UP);
            p.craft("stone_pickaxe","cobblestone","cobblestone","cobblestone","","stick","","","stick","");
            p.craft("stone_axe","cobblestone","cobblestone","","cobblestone","stick","","","stick","");
            p.craft("furnace","cobblestone","cobblestone","cobblestone","cobblestone","","cobblestone","cobblestone","cobblestone","cobblestone");
            p.craft("bucket","iron_ingot","","iron_ingot","","iron_ingot","");
            p.craft("bowl","oak_planks","","oak_planks","","oak_planks","");
            p.craft("smoke_kitchen:clay_stove","clay_ball","stick","clay_ball","clay_ball","cobblestone","clay_ball","cobblestone","cobblestone","cobblestone");
            p.craft("smoke_kitchen:clay_pot","clay_ball","","clay_ball","clay_ball","","clay_ball","","clay_ball","");
            p.craft("smoke_kitchen:stone_knife","","cobblestone","","cobblestone","","","stick","","");
            p.craft("smoke_kitchen:wooden_spatula","","oak_planks","","","stick","","stick","","");
            p.craft("smoke_kitchen:wooden_lid","oak_planks","oak_planks","","","stick","");
            p.craft("smoke_kitchen:hand_mill","","stick","","cobblestone","cobblestone","cobblestone","cobblestone","cobblestone","cobblestone");
            p.craft("smoke_kitchen:recipe_book","book","wheat_seeds");
            p.craft("smoke_kitchen:cutting_board","oak_planks","oak_planks","oak_planks","","stick","","stick","","");p.closeMenu();inventory(c,"first-meal-actual-crafting-0.1.11");
            mount(p,"furnace",FURNACE);p.emptyHand();p.click(FURNACE,Direction.UP);p.putInMenu("sand",0,3);p.putInMenu("coal",1,1);p.closeMenu();
            mount(p,"smoke_kitchen:clay_stove",STOVE);p.select("smoke_kitchen:clay_pot");p.click(STOVE,Direction.UP);mount(p,"smoke_kitchen:cutting_board",BOARD);mount(p,"smoke_kitchen:hand_mill",MILL);
            s.runCommand("summon smoke_kitchen:duck 0.5 100 2.5 {NoAI:1b}");s.runCommand("summon smoke_kitchen:duck 0.5 100 2.5 {NoAI:1b}");s.runCommand("summon minecraft:pig 1.5 100 2.5 {NoAI:1b}");c.waitTicks(8);kill(p,KitchenEntities.DUCK,new Vec3(.5,100,2.5));kill(p,EntityType.PIG,new Vec3(1.5,100,2.5));
            check(p.count("smoke_kitchen:duck_meat")==2&&p.count("smoke_kitchen:rendered_fat")==1&&p.count("porkchop")>0,"Two actual ducks and one actual pig must produce their real meat and fat loot");inventory(c,"first-meal-actual-animal-loot-0.1.11");
            s.runCommand("setblock -3 100 3 smoke_kitchen:salt_ore");p.select("stone_pickaxe");p.mine(ORE);check(p.count("smoke_kitchen:salt_ore_chunk")==2,"An actual crafted pickaxe must mine two ore chunks");p.select("smoke_kitchen:salt_ore_chunk");p.click(MILL,Direction.UP);p.click(MILL,Direction.UP);p.emptyHand();p.click(MILL,Direction.UP);p.click(MILL,Direction.UP);take(p,MILL);check(p.count("smoke_kitchen:salt_crystals")==8,"Actual milling must yield eight crystals");
            p.emptyHand();p.click(FURNACE,Direction.UP);until(c,s,g->{var f=(net.minecraft.world.level.block.entity.FurnaceBlockEntity)g.overworld().getBlockEntity(FURNACE);return f.getItem(2).is(Items.GLASS)&&f.getItem(2).getCount()==3;},800,"The real furnace must smelt all three supplied sand into glass");p.slot(2,0,ContainerInput.QUICK_MOVE);p.closeMenu();p.emptyHand();p.click(TABLE,Direction.UP);p.craft("smoke_kitchen:empty_jar","glass","glass","glass","oak_planks");p.closeMenu();check(p.count("smoke_kitchen:empty_jar")==4,"The actual glass recipe must make four empty jars");
            p.select("smoke_kitchen:salt_crystals");for(int i=0;i<4;i++)p.click(MILL,Direction.UP);p.select("smoke_kitchen:empty_jar");p.click(MILL,Direction.UP);p.emptyHand();p.click(MILL,Direction.UP);take(p,MILL);
            s.runOnServer(g->{var salt=g.getPlayerList().getPlayers().getFirst().getInventory().getNonEquipmentItems().stream().filter(x->x.is(KitchenContent.item("used_salt"))).findFirst().orElseThrow();check(salt.getMaxDamage()-salt.getDamageValue()==4,"The actual small batch must contain precisely four salt uses");});
            p.select("smoke_kitchen:rendered_fat");p.click(STOVE,Direction.UP);p.select("coal");for(int i=0;i<3;i++)p.click(STOVE,Direction.NORTH);p.emptyHand();for(int i=0;i<3;i++)p.click(STOVE,Direction.NORTH);
            until(c,s,g->station(g,STOVE).batch.ready(),1800,"The actual cold pot must render raw pig fat on low heat");p.select("smoke_kitchen:empty_jar");p.click(STOVE,Direction.UP);p.emptyHand();for(int i=0;i<3;i++)p.click(STOVE,Direction.NORTH);
            check(p.count("smoke_kitchen:used_lard")==1&&p.count("smoke_kitchen:fat_cracklings")==1,"One actual fat portion must yield four oil uses and one crackling");inventory(c,"first-meal-small-seasoning-batches-0.1.11");
            cut(p,"smoke_kitchen:duck_meat");cut(p,"smoke_kitchen:tomato");s.runOnServer(g->check(g.getPlayerList().getPlayers().getFirst().getInventory().getNonEquipmentItems().stream().filter(x->ResourceStates.baseId(x).equals("duck_meat")||ResourceStates.baseId(x).equals("tomato")).allMatch(x->x.getOrDefault(DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.EMPTY).copyTag().getStringOr("kitchen_cut","").equals("DICE")),"All actual duck and tomato portions must be cut on the board"));
            s.runCommand("setblock 4 99 2 minecraft:water");p.select("bucket");p.walkNear(WATER);p.look(Vec3.atCenterOf(WATER).add(0,.4,0));c.runOnClient(client->client.gameMode.useItem(client.player,InteractionHand.MAIN_HAND));c.waitTicks(8);check(p.count("water_bucket")==1,"A crafted bucket must actually fill from the water source");p.select("water_bucket");p.click(STOVE,Direction.UP);
            p.select("smoke_kitchen:used_salt");p.click(STOVE,Direction.UP);p.select("smoke_kitchen:used_lard");p.click(STOVE,Direction.UP);p.select("smoke_kitchen:duck_meat");p.click(STOVE,Direction.UP);p.click(STOVE,Direction.UP);p.select("smoke_kitchen:tomato");p.click(STOVE,Direction.UP);p.click(STOVE,Direction.UP);p.select("smoke_kitchen:wooden_lid");p.click(STOVE,Direction.UP);p.emptyHand();p.click(STOVE,Direction.NORTH);p.click(STOVE,Direction.NORTH);
            int bowls=p.count("bowl");p.select("bowl");p.click(STOVE,Direction.UP);check(p.count("bowl")==bowls&&p.count("smoke_kitchen:mixed_stew")==0,"Early serving must not consume the bowl or invent a cooked meal");
            p.select("smoke_kitchen:wooden_spatula");for(int i=0;i<3;i++){p.click(STOVE,Direction.UP);c.waitTicks(180);}until(c,s,g->station(g,STOVE).batch.ready(),4000,"The real duck and tomato must heat and cook independently before serving");
            if(continueWithTransfer(p))return;
            p.emptyHand();p.click(STOVE,Direction.UP);p.look(Vec3.atCenterOf(STOVE));world.getClientLevel().waitForChunksRender();c.takeScreenshot("first-meal-ready-clay-stove-0.1.11");p.select("bowl");p.click(STOVE,Direction.UP);check(p.count("smoke_kitchen:mixed_stew")==1,"The real cooked batch must serve one complete meal");p.emptyHand();p.click(STOVE,Direction.NORTH);p.click(STOVE,Direction.NORTH);
            s.runOnServer(g->g.getPlayerList().getPlayers().getFirst().getFoodData().setFoodLevel(8));p.select("smoke_kitchen:mixed_stew");c.runOnClient(client->client.player.setXRot(-70));c.getInput().holdKey(o->o.keyUse);c.waitTicks(40);c.getInput().releaseKey(o->o.keyUse);c.waitTicks(6);
            s.runOnServer(g->{var player=g.getPlayerList().getPlayers().getFirst();var pot=station(g,STOVE);check(player.getFoodData().getFoodLevel()>8&&player.getInventory().countItem(KitchenContent.item("mixed_stew"))==0,"Actual use must eat the cooked meal and restore hunger");check(player.getInventory().countItem(Items.BOWL)==4&&pot.batch.remainingServings==1&&pot.heat==0,"Eating returns the original bowl while one serving remains in a shut-off pot");savedWater[0]=pot.batch.water;savedFuel[0]=pot.fuelSeconds;});inventory(c,"first-meal-after-actual-eating-0.1.11");
        }
        try(var world=save.open()){
            var p=new SurvivalPlayer(c,world.getServer());c.waitTicks(8);world.getServer().runOnServer(g->{var pot=station(g,STOVE);check(pot.batch.remainingServings==1&&pot.batch.lockedDish.equals("mixed_stew")&&pot.batch.water==savedWater[0]&&pot.fuelSeconds==savedFuel[0],"Actual reopening must preserve the remaining meal, water and finite fuel");var inv=g.getPlayerList().getPlayers().getFirst().getInventory();for(String id:new String[]{"used_salt","used_lard"}){var jar=inv.getNonEquipmentItems().stream().filter(x->x.is(KitchenContent.item(id))).findFirst().orElseThrow();check(jar.getMaxDamage()-jar.getDamageValue()==3,"Reopening must preserve the actual three remaining uses of "+id);}});p.select("bowl");p.click(STOVE,Direction.UP);check(p.count("smoke_kitchen:mixed_stew")==1,"The saved last portion must actually be served after reopening");world.getServer().runOnServer(g->check(station(g,STOVE).batch.ingredients.isEmpty(),"The last serving must empty the cooked batch without duplicating raw food"));c.takeScreenshot("first-meal-after-world-reopen-0.1.11");
            p.select("smoke_kitchen:recipe_book");c.runOnClient(client->client.gameMode.useItem(client.player,InteractionHand.MAIN_HAND));c.waitTicks(4);
            for(int i=0;i<25;i++){
                boolean[] found={false};c.runOnClient(client->{try{
                    var screen=client.screen;var field=screen.getClass().getDeclaredField("renderedPages");field.setAccessible(true);var index=screen.getClass().getDeclaredField("page");index.setAccessible(true);var page=((java.util.List<?>)field.get(screen)).get(index.getInt(screen));var title=page.getClass().getDeclaredMethod("title");title.setAccessible(true);
                    found[0]="容器与盐水".equals(title.invoke(page));if(!found[0])for(var child:screen.children())if(child instanceof net.minecraft.client.gui.components.Button button&&button.getMessage().getString().equals("下一页")){button.onPress(null);break;}
                }catch(ReflectiveOperationException e){throw new AssertionError(e);}});c.waitTicks(3);
                if(found[0]){c.runOnClient(client->client.getToastManager().clear());c.takeScreenshot("first-meal-small-salt-guide-a-0.1.11");c.runOnClient(client->{for(var child:client.screen.children())if(child instanceof net.minecraft.client.gui.components.Button button&&button.getMessage().getString().equals("下一页")){button.onPress(null);break;}});c.waitTicks(3);c.takeScreenshot("first-meal-small-salt-guide-b-0.1.11");break;}check(i<24,"The actual crafted guide must open and reach the small-batch salt instructions");
            }
            c.runOnClient(client->client.setScreen(null));
        }
    }
}
