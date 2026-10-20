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

/** Concurrent real-time soup and steaming, followed by eating and a real save/reopen. */
public final class WetRecipeFlowTest implements FabricClientGameTest {
    private static final BlockPos SOUP=new BlockPos(0,100,0),STEAM=new BlockPos(2,100,0),BOARD=new BlockPos(-1,100,0);
    private int boardCut=CookingBatch.Cut.DICE.ordinal();
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    private static void equip(ClientGameTestContext c,TestServerContext s,String id){c.runOnClient(client->client.player.getInventory().setSelectedSlot(8));s.runCommand("item replace entity @a hotbar.8 with "+id);c.waitTicks(5);}
    private static void click(ClientGameTestContext c,BlockPos p,Direction face){c.runOnClient(client->client.gameMode.useItemOn(client.player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(p),face,p,false)));c.waitTicks(6);}
    private static void select(ClientGameTestContext c,Item item){c.runOnClient(client->{for(int i=0;i<8;i++)if(client.player.getInventory().getItem(i).is(item)){client.player.getInventory().setSelectedSlot(i);return;}throw new AssertionError("Prepared item missing: "+item);});c.waitTicks(3);}
    private void cut(ClientGameTestContext c,TestServerContext s,String id,CookingBatch.Cut cut) {
        equip(c,s,"smoke_kitchen:"+id);click(c,BOARD,Direction.UP);equip(c,s,"smoke_kitchen:stone_knife");
        c.getInput().holdKey(options->options.keyShift);c.waitTicks(3);
        while(boardCut!=cut.ordinal()){click(c,BOARD,Direction.UP);boardCut=(boardCut+1)%CookingBatch.Cut.values().length;}
        c.getInput().releaseKey(options->options.keyShift);c.waitTicks(3);for(int i=0;i<4;i++)click(c,BOARD,Direction.UP);
        s.runOnServer(game->check(game.getPlayerList().getPlayers().getFirst().getInventory().contains(stack->stack.is(KitchenContent.item(id))&&stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.EMPTY).copyTag().getStringOr("kitchen_cut","").equals(cut.name())),"Real sneaking knife input must return "+id+" cut as "+cut));
    }
    private static KitchenBlockEntity station(net.minecraft.server.MinecraftServer game,BlockPos p){return (KitchenBlockEntity)game.overworld().getBlockEntity(p);}
    @Override public void runTest(ClientGameTestContext context) {
        TestWorldSave save;int[] gas=new int[2];double[] water=new double[2];
        try(var world=context.worldBuilder().create()) {
            save=world.getWorldSave();var server=world.getServer();server.runCommand("time set day");server.runCommand("weather clear");server.runCommand("fill -6 99 -6 6 99 6 minecraft:smooth_stone");server.runCommand("tp @a 1 100 -3 0 20");server.runCommand("gamemode survival @a");context.waitTicks(10);
            for(var p:new BlockPos[]{SOUP,STEAM}){equip(context,server,"smoke_kitchen:gas_stove");click(context,p.below(),Direction.UP);}
            equip(context,server,"smoke_kitchen:soup_pot");click(context,SOUP,Direction.UP);equip(context,server,"smoke_kitchen:steamer");click(context,STEAM,Direction.UP);
            equip(context,server,"smoke_kitchen:cutting_board");click(context,BOARD.below(),Direction.UP);
            for(var p:new BlockPos[]{SOUP,STEAM}){equip(context,server,"minecraft:water_bucket");click(context,p,Direction.UP);}
            server.runCommand("tp @a 1 100 -1 0 48");context.waitTicks(5);context.takeScreenshot("wet-visible-water-0.1.7");server.runCommand("tp @a 1 100 -3 0 20");context.waitTicks(5);
            cut(context,server,"ginger",CookingBatch.Cut.SLICE);select(context,KitchenContent.item("ginger"));click(context,STEAM,Direction.UP);
            cut(context,server,"scallion",CookingBatch.Cut.DICE);select(context,KitchenContent.item("scallion"));click(context,STEAM,Direction.UP);
            for(int i=0;i<2;i++){equip(context,server,"minecraft:cod");click(context,STEAM,Direction.UP);cut(context,server,"tomato",CookingBatch.Cut.DICE);select(context,KitchenContent.item("tomato"));click(context,SOUP,Direction.UP);}
            equip(context,server,"smoke_kitchen:pot_lid");click(context,STEAM,Direction.UP);
            for(var p:new BlockPos[]{SOUP,STEAM}){equip(context,server,"smoke_kitchen:salt");click(context,p,Direction.UP);equip(context,server,"smoke_kitchen:gas_canister");click(context,p,Direction.NORTH);}
            equip(context,server,"minecraft:air");context.waitTicks(700);
            server.runOnServer(game->check(station(game,SOUP).batch.ingredients.stream().filter(p->p.id.equals("tomato")).allMatch(p->p.cooked>=.5),"Tomato must actually cook to the required stage before adding egg"));
            equip(context,server,"minecraft:egg");click(context,SOUP,Direction.UP);server.runOnServer(game->check(station(game,SOUP).batch.ingredients.getLast().cooked<.1,"A late egg must start with its own raw cookedness"));
            context.takeScreenshot("wet-late-egg-and-steam-0.1.7");equip(context,server,"minecraft:air");context.waitTicks(1600);
            server.runOnServer(game->{check(station(game,STEAM).batch.ready(),"Covered steam must really cook the fish");check(station(game,SOUP).batch.ready(),"The egg soup must really finish cooking");});
            equip(context,server,"smoke_kitchen:soy_sauce");click(context,STEAM,Direction.UP);
            server.runOnServer(game->{for(String id:new String[]{"steamed_fish","tomato_egg_soup"}){var recipe=ContentCatalog.DISHES.stream().filter(r->r.id().equals(id)).findFirst().orElseThrow();check(recipe.matches(station(game,id.equals("steamed_fish")?STEAM:SOUP).batch),"Actual player preparation must satisfy "+id+": "+recipe.problem(station(game,id.equals("steamed_fish")?STEAM:SOUP).batch));}});
            equip(context,server,"minecraft:air");click(context,STEAM,Direction.UP);context.takeScreenshot("wet-cooked-fish-and-soup-0.1.7");
            for(var p:new BlockPos[]{SOUP,STEAM}){equip(context,server,"minecraft:bowl");click(context,p,Direction.UP);equip(context,server,"minecraft:air");click(context,p,Direction.NORTH);click(context,p,Direction.NORTH);}
            select(context,KitchenContent.item("steamed_fish"));server.runOnServer(game->game.getPlayerList().getPlayers().getFirst().getFoodData().setFoodLevel(8));
            context.runOnClient(client->client.player.setXRot(-70));context.getInput().holdKey(options->options.keyUse);context.waitTicks(40);context.getInput().releaseKey(options->options.keyUse);context.waitTicks(5);
            server.runOnServer(game->{check(game.getPlayerList().getPlayers().getFirst().getFoodData().getFoodLevel()>8,"The prepared fish must actually be eaten");for(int i=0;i<2;i++){var stove=station(game,i==0?SOUP:STEAM);check(stove.heat==0,"Heat must be off before saving");gas[i]=ResourceStates.gasUnits(stove.items.get(26));water[i]=stove.batch.water;}});
        }
        try(var world=save.open()) {
            context.waitTicks(10);world.getServer().runOnServer(game->{for(int i=0;i<2;i++){var stove=station(game,i==0?SOUP:STEAM);check(stove.batch.lockedDish.equals(i==0?"tomato_egg_soup":"steamed_fish")&&stove.batch.remainingServings==(i==0?2:1),"Actual world reopening must preserve the soup/fish and remaining servings");check(ResourceStates.gasUnits(stove.items.get(26))==gas[i]&&Math.abs(stove.batch.water-water[i])<.001,"Reopening must preserve exact fuel and water");}check(station(game,STEAM).batch.ingredients.getFirst().coveredCookingSeconds>=15,"Covered-cooking evidence must survive reopening");});
            world.getServer().runCommand("tp @a 1 100 -2 0 35");context.waitTicks(5);context.takeScreenshot("wet-after-world-reload-0.1.7");
            equip(context,world.getServer(),"smoke_kitchen:heat_cloth");context.getInput().holdKey(options->options.keyShift);context.waitTicks(5);click(context,SOUP,Direction.UP);context.getInput().releaseKey(options->options.keyShift);context.waitTicks(5);
            world.getServer().runOnServer(game->check(station(game,SOUP).mountedCookware.isEmpty(),"A real sneaking player must be able to lift cookware with heat cloth"));
            select(context,KitchenContent.item("soup_pot"));click(context,SOUP,Direction.UP);
            world.getServer().runOnServer(game->check(station(game,SOUP).batch.remainingServings==2&&station(game,SOUP).batch.lockedDish.equals("tomato_egg_soup"),"Real remounting must preserve the saved soup"));
        }
    }
}
