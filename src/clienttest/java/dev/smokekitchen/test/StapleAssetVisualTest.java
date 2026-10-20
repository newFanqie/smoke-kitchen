package dev.smokekitchen.test;

import dev.smokekitchen.KitchenConfig;
import dev.smokekitchen.content.*;
import dev.smokekitchen.world.KitchenBlockEntity;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.minecraft.core.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.*;

/** Final material-only checks supplement the full cooking/save workflows. */
public final class StapleAssetVisualTest implements FabricClientGameTest {
    private static final BlockPos BOARD=new BlockPos(0,100,0),OVEN=new BlockPos(2,100,0);
    private static void equip(ClientGameTestContext c,TestServerContext s,String id){c.runOnClient(client->client.player.getInventory().setSelectedSlot(8));s.runCommand("item replace entity @a hotbar.8 with "+id);c.waitTicks(5);}
    private static void click(ClientGameTestContext c,BlockPos p){c.runOnClient(client->client.gameMode.useItemOn(client.player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(p),Direction.UP,p,false)));c.waitTicks(6);}
    @Override public void runTest(ClientGameTestContext c) {
        boolean[] labels={true};c.runOnClient(client->{labels[0]=KitchenConfig.INSTANCE.showWorldLabels;KitchenConfig.INSTANCE.showWorldLabels=false;});
        try(var world=c.worldBuilder().create()) {
            var s=world.getServer();s.runCommand("time set day");s.runCommand("weather clear");s.runCommand("fill -5 99 -5 5 99 5 minecraft:smooth_stone");s.runCommand("tp @a 0 100 -1 0 55");s.runCommand("gamemode survival @a");c.waitTicks(10);
            equip(c,s,"smoke_kitchen:cutting_board");c.runOnClient(client->client.gameMode.useItemOn(client.player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(BOARD.below()),Direction.UP,BOARD.below(),false)));c.waitTicks(6);
            equip(c,s,"smoke_kitchen:dough");click(c,BOARD);equip(c,s,"smoke_kitchen:rolling_pin");click(c,BOARD);equip(c,s,"minecraft:air");c.waitTicks(10);c.takeScreenshot("asset-rolled-dough-0.1.7");
            equip(c,s,"smoke_kitchen:stone_knife");c.getInput().holdKey(o->o.keyShift);c.waitTicks(3);click(c,BOARD);c.getInput().releaseKey(o->o.keyShift);c.waitTicks(3);for(int i=0;i<4;i++)click(c,BOARD);
            c.runOnClient(client->{for(int i=0;i<8;i++)if(client.player.getInventory().getItem(i).is(KitchenContent.item("dough"))){client.player.getInventory().setSelectedSlot(i);return;}throw new AssertionError("Actual cut noodles missing");});c.waitTicks(3);click(c,BOARD);equip(c,s,"minecraft:air");c.waitTicks(10);c.takeScreenshot("asset-cut-noodles-0.1.7");
            equip(c,s,"smoke_kitchen:oven");c.runOnClient(client->client.gameMode.useItemOn(client.player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(OVEN.below()),Direction.UP,OVEN.below(),false)));c.waitTicks(6);
            equip(c,s,"smoke_kitchen:baking_tray");click(c,OVEN);for(int i=0;i<2;i++){equip(c,s,"smoke_kitchen:fermented_dough");click(c,OVEN);}equip(c,s,"minecraft:sugar");click(c,OVEN);equip(c,s,"minecraft:coal");click(c,OVEN);equip(c,s,"minecraft:air");s.runCommand("tp @a 2 100 -2 0 30");c.waitTicks(300);
            s.runOnServer(server->{var oven=(KitchenBlockEntity)server.overworld().getBlockEntity(OVEN);if(oven.heat!=2||oven.batch.ovenSeconds<1||!oven.mountedCookware.equals("baking_tray"))throw new AssertionError("The photographed oven must actually be heating its mounted tray");});c.takeScreenshot("asset-oven-burning-0.1.7");
            var colour=new BlockPos(-2,100,0);s.runCommand("setblock -2 100 -1 minecraft:smooth_stone");s.runCommand("tp @a -2 101 -1 0 60");equip(c,s,"smoke_kitchen:gas_stove");c.runOnClient(client->client.gameMode.useItemOn(client.player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(colour.below()),Direction.UP,colour.below(),false)));c.waitTicks(6);equip(c,s,"smoke_kitchen:soup_pot");click(c,colour);equip(c,s,"smoke_kitchen:rapeseed_oil");click(c,colour);equip(c,s,"minecraft:sugar");click(c,colour);equip(c,s,"minecraft:air");c.waitTicks(5);c.takeScreenshot("asset-oil-surface-0.1.7");equip(c,s,"smoke_kitchen:gas_canister");click(c,colour);equip(c,s,"minecraft:air");
            boolean[] coloured={false};for(int i=0;i<30;i++){s.runOnServer(server->coloured[0]=((KitchenBlockEntity)server.overworld().getBlockEntity(colour)).batch.caramelSeconds>=3);if(coloured[0])break;c.waitTicks(20);}if(!coloured[0])throw new AssertionError("Actual sugar must caramelise before its photograph");c.takeScreenshot("asset-caramel-surface-0.1.7");c.waitTicks(500);s.runOnServer(server->{if(((KitchenBlockEntity)server.overworld().getBlockEntity(colour)).batch.burntSugarSeconds<5)throw new AssertionError("Continued excessive heat must really burn sugar");});c.takeScreenshot("asset-burnt-sugar-surface-0.1.7");
        } finally {c.runOnClient(client->KitchenConfig.INSTANCE.showWorldLabels=labels[0]);}
    }
}
