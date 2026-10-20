package dev.smokekitchen.test;

import dev.smokekitchen.content.*;
import dev.smokekitchen.cooking.CookingBatch;
import dev.smokekitchen.world.KitchenBlockEntity;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;
import net.minecraft.core.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.*;

/** Ingredient fixtures, normal packets and elapsed ticks throughout the preparation chain. */
public final class StapleFlowTest implements FabricClientGameTest {
    private static final BlockPos POT=new BlockPos(0,100,0),OVEN=new BlockPos(2,100,0),STEAM=new BlockPos(4,100,0),BOARD=new BlockPos(-1,100,0),MIX=new BlockPos(0,100,2),CROCK=new BlockPos(2,100,2),CROCK2=new BlockPos(3,100,2);
    private int cut=CookingBatch.Cut.DICE.ordinal();
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    private static KitchenBlockEntity station(net.minecraft.server.MinecraftServer server,BlockPos p){return (KitchenBlockEntity)server.overworld().getBlockEntity(p);}
    private static void equip(ClientGameTestContext c,TestServerContext s,String id){c.runOnClient(client->client.player.getInventory().setSelectedSlot(8));s.runCommand("item replace entity @a hotbar.8 with "+id);c.waitTicks(5);}
    private static void click(ClientGameTestContext c,BlockPos p,Direction face){c.runOnClient(client->client.gameMode.useItemOn(client.player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(p),face,p,false)));c.waitTicks(6);}
    private static void select(ClientGameTestContext c,Item item){c.runOnClient(client->{for(int i=0;i<36;i++)if(i!=8&&client.player.getInventory().getItem(i).is(item)){if(i>8){client.gameMode.handleContainerInput(client.player.inventoryMenu.containerId,i,7,net.minecraft.world.inventory.ContainerInput.SWAP,client.player);client.player.getInventory().setSelectedSlot(7);}else client.player.getInventory().setSelectedSlot(i);return;}throw new AssertionError("Prepared item missing: "+item);});c.waitTicks(5);}
    private void prepare(ClientGameTestContext c,TestServerContext s,String id,boolean roll,boolean shred) {
        if(id.equals("dough"))select(c,KitchenContent.item(id));else equip(c,s,"smoke_kitchen:"+id);
        click(c,BOARD,Direction.UP);
        if(roll){equip(c,s,"smoke_kitchen:rolling_pin");click(c,BOARD,Direction.UP);}
        if(shred){equip(c,s,"smoke_kitchen:stone_knife");c.getInput().holdKey(o->o.keyShift);c.waitTicks(3);while(cut!=CookingBatch.Cut.SHRED.ordinal()){click(c,BOARD,Direction.UP);cut=(cut+1)%CookingBatch.Cut.values().length;}c.getInput().releaseKey(o->o.keyShift);c.waitTicks(3);for(int i=0;i<4;i++)click(c,BOARD,Direction.UP);}
        else {equip(c,s,"minecraft:air");c.getInput().holdKey(o->o.keyShift);c.waitTicks(3);click(c,BOARD,Direction.UP);c.getInput().releaseKey(o->o.keyShift);c.waitTicks(3);}
    }
    private static void retrieve(ClientGameTestContext c,TestServerContext s,BlockPos p,int count){equip(c,s,"minecraft:air");c.getInput().holdKey(o->o.keyShift);c.waitTicks(3);for(int i=0;i<count;i++)click(c,p,Direction.UP);c.getInput().releaseKey(o->o.keyShift);c.waitTicks(3);}
    @Override public void runTest(ClientGameTestContext c) {
        TestWorldSave save;double[] water={0};int[] gas={0};
        try(var world=c.worldBuilder().create()) {
            save=world.getWorldSave();var s=world.getServer();s.runCommand("time set day");s.runCommand("weather clear");s.runCommand("fill -6 99 -6 7 99 7 minecraft:smooth_stone");s.runCommand("tp @a 1 100 1 0 25");s.runCommand("gamemode survival @a");c.waitTicks(10);
            for(var p:new BlockPos[]{POT,STEAM}){equip(c,s,"smoke_kitchen:gas_stove");click(c,p.below(),Direction.UP);}
            for(var row:new Object[][]{{OVEN,"oven"},{BOARD,"cutting_board"},{MIX,"mixing_table"},{CROCK,"fermentation_crock"},{CROCK2,"fermentation_crock"}}){equip(c,s,"smoke_kitchen:"+row[1]);click(c,((BlockPos)row[0]).below(),Direction.UP);}
            for(var p:new BlockPos[]{CROCK,CROCK2}){equip(c,s,"smoke_kitchen:dough");click(c,p,Direction.UP);equip(c,s,"smoke_kitchen:dough");click(c,p,Direction.UP);equip(c,s,"smoke_kitchen:starter");click(c,p,Direction.UP);equip(c,s,"minecraft:air");click(c,p,Direction.UP);}
            equip(c,s,"minecraft:water_bucket");click(c,MIX,Direction.UP);for(int i=0;i<2;i++){equip(c,s,"smoke_kitchen:flour");click(c,MIX,Direction.UP);}equip(c,s,"smoke_kitchen:rolling_pin");click(c,MIX,Direction.UP);retrieve(c,s,MIX,1);
            for(int i=0;i<2;i++){prepare(c,s,"dough",true,true);}
            s.runOnServer(game->check(game.getPlayerList().getPlayers().getFirst().getInventory().contains(stack->stack.is(KitchenContent.item("dough"))&&stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.EMPTY).copyTag().getBooleanOr("kitchen_rolled",false)),"Real board rolling must survive in prepared noodle stacks"));
            prepare(c,s,"cabbage",false,true);
            equip(c,s,"smoke_kitchen:soup_pot");click(c,POT,Direction.UP);equip(c,s,"minecraft:water_bucket");click(c,POT,Direction.UP);
            for(int i=0;i<2;i++){select(c,KitchenContent.item("dough"));click(c,POT,Direction.UP);}select(c,KitchenContent.item("cabbage"));click(c,POT,Direction.UP);equip(c,s,"smoke_kitchen:salt");click(c,POT,Direction.UP);equip(c,s,"smoke_kitchen:gas_canister");click(c,POT,Direction.NORTH);
            equip(c,s,"minecraft:air");s.runCommand("tp @a 0 100 -1 0 55");c.waitTicks(1600);s.runOnServer(game->check(ContentCatalog.DISHES.stream().filter(r->r.id().equals("noodle_soup")).findFirst().orElseThrow().matches(station(game,POT).batch)&&station(game,POT).batch.ready(),"Rolled and cut noodles must actually cook into named noodle soup"));c.takeScreenshot("staple-actual-noodles-0.1.5");
            equip(c,s,"minecraft:bowl");click(c,POT,Direction.UP);equip(c,s,"minecraft:air");click(c,POT,Direction.NORTH);click(c,POT,Direction.NORTH);c.waitTicks(600);
            s.runCommand("tp @a 1 100 1 0 25");c.waitTicks(5);for(var p:new BlockPos[]{CROCK,CROCK2})retrieve(c,s,p,2);
            s.runOnServer(game->check(game.getPlayerList().getPlayers().getFirst().getInventory().countItem(KitchenContent.item("fermented_dough"))==4,"Two actually sealed crocks must ferment four dough portions over elapsed ticks"));
            equip(c,s,"smoke_kitchen:baking_tray");click(c,OVEN,Direction.NORTH);for(int i=0;i<2;i++){select(c,KitchenContent.item("fermented_dough"));click(c,OVEN,Direction.NORTH);}equip(c,s,"minecraft:sugar");click(c,OVEN,Direction.NORTH);equip(c,s,"minecraft:coal");click(c,OVEN,Direction.NORTH);
            equip(c,s,"smoke_kitchen:steamer");click(c,STEAM,Direction.UP);equip(c,s,"minecraft:water_bucket");click(c,STEAM,Direction.UP);for(int i=0;i<2;i++){select(c,KitchenContent.item("fermented_dough"));click(c,STEAM,Direction.UP);}equip(c,s,"smoke_kitchen:pot_lid");click(c,STEAM,Direction.UP);equip(c,s,"smoke_kitchen:gas_canister");click(c,STEAM,Direction.NORTH);
            equip(c,s,"minecraft:air");s.runCommand("tp @a 2 100 -3 0 15");c.waitTicks(1100);s.runOnServer(game->check(station(game,OVEN).batch.ready()&&!station(game,OVEN).batch.burnt(),"Observe bread cookedness before shutting its oven off"));click(c,OVEN,Direction.NORTH);click(c,OVEN,Direction.NORTH);c.waitTicks(600);
            s.runOnServer(game->{for(var row:new Object[][]{{OVEN,"homestyle_bread"},{STEAM,"steamed_buns"}}){var pot=station(game,(BlockPos)row[0]);check(pot.batch.ready()&&!pot.batch.burnt(),"Actual "+row[1]+" must finish without burning: cooked="+pot.batch.ingredients.getFirst().cooked);var recipe=ContentCatalog.DISHES.stream().filter(r->r.id().equals(row[1])).findFirst().orElseThrow();check(recipe.matches(pot.batch),"Actual fermented dish must match "+row[1]+": "+recipe.problem(pot.batch));}});c.takeScreenshot("staple-oven-and-buns-0.1.5");
            for(var p:new BlockPos[]{OVEN,STEAM}){equip(c,s,"minecraft:bowl");click(c,p,Direction.NORTH);equip(c,s,"minecraft:air");if(p.equals(STEAM)){click(c,p,Direction.NORTH);click(c,p,Direction.NORTH);}}select(c,KitchenContent.item("homestyle_bread"));s.runOnServer(game->game.getPlayerList().getPlayers().getFirst().getFoodData().setFoodLevel(8));c.runOnClient(client->client.player.setXRot(-70));c.getInput().holdKey(o->o.keyUse);c.waitTicks(40);c.getInput().releaseKey(o->o.keyUse);c.waitTicks(80);
            s.runOnServer(game->{check(game.getPlayerList().getPlayers().getFirst().getFoodData().getFoodLevel()>8,"Actual home bread must be edible");check(station(game,POT).batch.remainingServings==1,"Half the noodle soup must remain");water[0]=station(game,POT).batch.water;gas[0]=ResourceStates.gasUnits(station(game,STEAM).items.get(26));check(station(game,STEAM).batch.water>5,"Serving buns must retain the steam reservoir");});
        }
        try(var world=save.open()) {
            c.waitTicks(10);world.getServer().runOnServer(game->{check(station(game,POT).batch.lockedDish.equals("noodle_soup")&&station(game,POT).batch.remainingServings==1&&Math.abs(station(game,POT).batch.water-water[0])<.001,"Actual save reopening must retain soup and reduced water");check(station(game,OVEN).batch.lockedDish.equals("homestyle_bread")&&station(game,OVEN).batch.remainingServings==1&&station(game,OVEN).mountedCookware.equals("baking_tray"),"Actual save reopening must retain the oven tray and remaining bread");check(station(game,STEAM).batch.lockedDish.equals("steamed_buns")&&station(game,STEAM).batch.remainingServings==1&&ResourceStates.gasUnits(station(game,STEAM).items.get(26))==gas[0],"Remaining buns and exact gas must persist");});
            world.getServer().runCommand("tp @a 2 100 -3 0 15");c.waitTicks(5);c.takeScreenshot("staple-after-reopen-0.1.5");equip(c,world.getServer(),"smoke_kitchen:heat_cloth");c.getInput().holdKey(o->o.keyShift);c.waitTicks(3);click(c,OVEN,Direction.NORTH);c.getInput().releaseKey(o->o.keyShift);c.waitTicks(3);select(c,KitchenContent.item("baking_tray"));click(c,OVEN,Direction.NORTH);world.getServer().runOnServer(game->check(station(game,OVEN).batch.remainingServings==1&&station(game,OVEN).batch.ovenSeconds>=15,"Actual hot tray lift and remount must preserve remaining bread and oven history"));
        }
    }
}
