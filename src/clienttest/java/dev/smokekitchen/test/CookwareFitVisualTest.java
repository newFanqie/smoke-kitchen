package dev.smokekitchen.test;

import dev.smokekitchen.world.*;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.minecraft.core.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.*;

/** Cold geometry fixture only. Does not substitute for the real cooking/crafting flows. */
public final class CookwareFitVisualTest implements FabricClientGameTest {
    private static void equip(ClientGameTestContext c,TestServerContext s,String id){c.runOnClient(m->m.player.getInventory().setSelectedSlot(8));s.runCommand("item replace entity @a hotbar.8 with "+id);c.waitTicks(4);}
    private static void click(ClientGameTestContext c,BlockPos p){c.runOnClient(m->m.gameMode.useItemOn(m.player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(p),Direction.UP,p,false)));c.waitTicks(6);}
    private static void photo(ClientGameTestContext c,TestServerContext s,String label,String camera){equip(c,s,"minecraft:air");s.runCommand(camera);c.waitTicks(8);c.takeScreenshot("fit-"+label+"-0.1.12");}
    @Override public void runTest(ClientGameTestContext c){
        try(var w=c.worldBuilder().create()){
            var s=w.getServer();s.runCommand("time set day");s.runCommand("weather clear");s.runCommand("fill -3 99 -5 16 99 8 minecraft:smooth_stone");s.runCommand("gamemode survival @a");
            String[] ids={"clay_pot","soup_pot","wok","frying_pan","steamer","baking_tray","mixing_bowl"};
            for(int i=0;i<ids.length;i++){
                int x=i*2;var mounted=new BlockPos(x,100,0);var loose=new BlockPos(x,100,3);s.runCommand("tp @a "+(x+.5)+" 100 -1 0 30");c.waitTicks(3);
                equip(c,s,"smoke_kitchen:"+new String[]{"clay_stove","brick_stove","gas_stove"}[i%3]);click(c,mounted.below());equip(c,s,"smoke_kitchen:"+ids[i]);click(c,mounted);
                s.runCommand("tp @a "+(x+.5)+" 100 2 0 30");c.waitTicks(3);equip(c,s,"smoke_kitchen:"+ids[i]);click(c,loose.below());
            }
            photo(c,s,"open-left","tp @a 2.5 102 -3 0 40");photo(c,s,"open-right","tp @a 10.5 102 -3 0 40");
            for(int i=0;i<ids.length;i++)for(int z:new int[]{0,3}){
                int x=i*2;var p=new BlockPos(x,100,z);s.runCommand("tp @a "+(x+.5)+" 100 "+(z-1)+" 0 30");c.waitTicks(3);equip(c,s,"smoke_kitchen:pot_lid");click(c,p);
                s.runOnServer(g->{var e=(KitchenBlockEntity)g.overworld().getBlockEntity(p);if(!e.getBlockState().getValue(KitchenBlock.LID))throw new AssertionError("Actual lid action failed at "+p);});
            }
            photo(c,s,"covered-left","tp @a 2.5 102 -3 0 40");photo(c,s,"covered-right","tp @a 10.5 102 -3 0 40");
            photo(c,s,"pan-lid-side","tp @a 6.5 100 -1.8 0 22");photo(c,s,"standalone-pan-lid","tp @a 6.5 100 1.2 0 35");
        }
    }
}
