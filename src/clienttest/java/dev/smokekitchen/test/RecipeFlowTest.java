package dev.smokekitchen.test;

import dev.smokekitchen.content.*;
import dev.smokekitchen.world.KitchenBlockEntity;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;
import net.minecraft.core.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.*;

/** Inputs are fixtures; every preparation, cooking, serving and eating action uses client packets. */
public final class RecipeFlowTest implements FabricClientGameTest {
    private static final BlockPos POT=new BlockPos(0,100,0),WOK=new BlockPos(2,100,0),BOARD=new BlockPos(-1,100,0);
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    private static void equip(ClientGameTestContext c,TestServerContext s,String id){
        c.runOnClient(client->client.player.getInventory().setSelectedSlot(8));
        s.runCommand("item replace entity @a hotbar.8 with "+id);c.waitTicks(5);
    }
    private static void click(ClientGameTestContext c,BlockPos p,Direction face){
        c.runOnClient(client->client.gameMode.useItemOn(client.player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(p),face,p,false)));c.waitTicks(6);
    }
    private static void select(ClientGameTestContext c,Item item){c.runOnClient(client->{for(int i=0;i<8;i++)if(client.player.getInventory().getItem(i).is(item)){client.player.getInventory().setSelectedSlot(i);return;}throw new AssertionError("Prepared item missing from hotbar: "+item);});c.waitTicks(3);}
    private static void stir(ClientGameTestContext c,TestServerContext s,int count){equip(c,s,"smoke_kitchen:wooden_spatula");for(int i=0;i<count;i++)click(c,WOK,Direction.UP);}
    @Override public void runTest(ClientGameTestContext context) {
        TestWorldSave savedWorld;int[] savedGas={0};
        try(var world=context.worldBuilder().create()) {
            savedWorld=world.getWorldSave();var server=world.getServer();
            server.runCommand("time set day");server.runCommand("weather clear");server.runCommand("fill -6 99 -6 6 99 6 minecraft:smooth_stone");
            server.runCommand("tp @a 1 100 -3 0 20");server.runCommand("gamemode survival @a");context.waitTicks(10);
            for(var p:new BlockPos[]{POT,WOK}){equip(context,server,"smoke_kitchen:gas_stove");click(context,p.below(),Direction.UP);}
            equip(context,server,"smoke_kitchen:cutting_board");click(context,BOARD.below(),Direction.UP);
            equip(context,server,"smoke_kitchen:clay_pot");click(context,POT,Direction.UP);
            equip(context,server,"smoke_kitchen:wok");click(context,WOK,Direction.UP);
            equip(context,server,"smoke_kitchen:gas_canister");click(context,POT,Direction.NORTH);
            equip(context,server,"smoke_kitchen:water_jug");click(context,POT,Direction.UP);click(context,POT,Direction.UP);
            equip(context,server,"smoke_kitchen:rice");click(context,POT,Direction.UP);
            equip(context,server,"smoke_kitchen:pot_lid");click(context,POT,Direction.UP);
            equip(context,server,"smoke_kitchen:scallion");click(context,BOARD,Direction.UP);
            equip(context,server,"smoke_kitchen:stone_knife");for(int i=0;i<4;i++)click(context,BOARD,Direction.UP);
            server.runOnServer(game->check(game.getPlayerList().getPlayers().getFirst().getInventory().contains(stack->stack.is(KitchenContent.item("scallion"))&&stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.EMPTY).copyTag().getStringOr("kitchen_cut","").equals("DICE")),"Actual board use must return cut scallion"));
            equip(context,server,"minecraft:air");context.waitTicks(400);click(context,POT,Direction.NORTH);click(context,POT,Direction.NORTH);click(context,POT,Direction.NORTH);context.waitTicks(1200);
            server.runOnServer(game->check(((KitchenBlockEntity)game.overworld().getBlockEntity(POT)).batch.ready(),"Raw rice and water must really cook before being used for fried rice"));
            context.takeScreenshot("recipe-boiled-rice-0.1.8");
            for(int i=0;i<2;i++){equip(context,server,"minecraft:bowl");click(context,POT,Direction.UP);}
            equip(context,server,"minecraft:air");click(context,POT,Direction.NORTH);click(context,POT,Direction.NORTH);
            equip(context,server,"smoke_kitchen:gas_canister");click(context,WOK,Direction.NORTH);
            equip(context,server,"smoke_kitchen:rapeseed_oil");click(context,WOK,Direction.UP);context.waitTicks(200);
            equip(context,server,"minecraft:egg");click(context,WOK,Direction.UP);context.waitTicks(280);
            for(int i=0;i<2;i++){select(context,KitchenContent.item("cooked_rice"));click(context,WOK,Direction.UP);}
            select(context,KitchenContent.item("scallion"));click(context,WOK,Direction.UP);
            equip(context,server,"smoke_kitchen:salt");click(context,WOK,Direction.UP);
            for(int i=0;i<3;i++){stir(context,server,1);context.waitTicks(140);}
            server.runOnServer(game->{var stove=(KitchenBlockEntity)game.overworld().getBlockEntity(WOK);check(stove.batch.ready()&&!stove.batch.burnt(),"The complete meal must actually cook without burning");check(ContentCatalog.DISHES.stream().filter(r->r.id().equals("fried_rice")).findFirst().orElseThrow().matches(stove.batch),"Actual preheating, egg first, cooked rice, cut scallion and stirring must satisfy the named recipe");});
            context.takeScreenshot("recipe-fried-rice-ready-0.1.8");
            equip(context,server,"minecraft:bowl");click(context,WOK,Direction.UP);
            equip(context,server,"minecraft:air");click(context,WOK,Direction.NORTH);click(context,WOK,Direction.NORTH);
            select(context,KitchenContent.item("fried_rice"));
            server.runOnServer(game->{var player=game.getPlayerList().getPlayers().getFirst();player.getFoodData().setFoodLevel(8);check(player.getInventory().contains(stack->stack.is(KitchenContent.item("fried_rice"))),"Serving must produce real fried rice");});
            context.runOnClient(client->client.player.setXRot(-70));context.getInput().holdKey(options->options.keyUse);context.waitTicks(40);context.getInput().releaseKey(options->options.keyUse);context.waitTicks(5);
            server.runOnServer(game->{var player=game.getPlayerList().getPlayers().getFirst();check(player.getFoodData().getFoodLevel()>8,"The prepared meal must actually be eaten and restore hunger");var stove=(KitchenBlockEntity)game.overworld().getBlockEntity(WOK);check(stove.batch.remainingServings==1&&stove.heat==0,"One portion must remain in the switched-off pot");savedGas[0]=ResourceStates.gasUnits(stove.items.get(26));});
        }
        try(var reopened=savedWorld.open()) {
            context.waitTicks(10);reopened.getServer().runOnServer(game->{var stove=(KitchenBlockEntity)game.overworld().getBlockEntity(WOK);check(stove.batch.remainingServings==1&&stove.batch.lockedDish.equals("fried_rice"),"Closing and reopening the actual world must retain the last meal portion");check(ResourceStates.gasUnits(stove.items.get(26))==savedGas[0],"Reopening must retain exact remaining gas without offline fuel loss");check(ContentCatalog.DISHES.stream().filter(r->r.id().equals("fried_rice")).findFirst().orElseThrow().matches(stove.batch),"Preparation evidence must survive an actual world reload");});
            reopened.getServer().runCommand("tp @a 1 100 -3 0 20");context.waitTicks(5);context.takeScreenshot("recipe-after-world-reload-0.1.8");
            equip(context,reopened.getServer(),"minecraft:bowl");click(context,WOK,Direction.UP);
            reopened.getServer().runOnServer(game->check(((KitchenBlockEntity)game.overworld().getBlockEntity(WOK)).batch.ingredients.isEmpty(),"The last saved portion must remain retrievable after reopening"));
        }
    }
}
