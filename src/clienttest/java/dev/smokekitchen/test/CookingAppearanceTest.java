package dev.smokekitchen.test;

import dev.smokekitchen.client.KitchenStationRenderer;
import dev.smokekitchen.content.KitchenContent;
import dev.smokekitchen.world.*;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;
import net.minecraft.core.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.*;
import java.util.function.Predicate;

/** Actual knife cuts, elapsed heating, stirring, turning, serving and reopening. */
public final class CookingAppearanceTest implements FabricClientGameTest {
    private static final BlockPos WOK=new BlockPos(0,100,0),PAN=new BlockPos(2,100,0),OVEN=new BlockPos(4,100,0),CROCK=new BlockPos(0,100,2),BOARD=new BlockPos(-1,100,0);
    private static KitchenBlockEntity pot(MinecraftServer g,BlockPos p){return (KitchenBlockEntity)g.overworld().getBlockEntity(p);}
    private static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
    private static void equip(ClientGameTestContext c,TestServerContext s,String id){c.runOnClient(m->m.player.getInventory().setSelectedSlot(8));s.runCommand("item replace entity @a hotbar.8 with "+id);c.waitTicks(4);}
    private static void click(ClientGameTestContext c,BlockPos p,Direction face){c.runOnClient(m->m.gameMode.useItemOn(m.player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(p),face,p,false)));c.waitTicks(6);}
    private static boolean observe(TestServerContext s,Predicate<MinecraftServer> p){boolean[] b={false};s.runOnServer(g->b[0]=p.test(g));return b[0];}
    private static void until(ClientGameTestContext c,TestServerContext s,Predicate<MinecraftServer> p,int ticks,String message){for(int n=0;n<ticks;n+=20){if(observe(s,p))return;c.waitTicks(20);}check(observe(s,p),message);}
    private static double cooked(KitchenBlockEntity e){return e.batch.ingredients.stream().mapToDouble(p->p.cooked).min().orElse(0);}
    private static KitchenStationRenderer.State snapshot(ClientGameTestContext c,BlockPos pos){
        KitchenStationRenderer.State[] result={null};c.runOnClient(m->{var e=(KitchenBlockEntity)m.level.getBlockEntity(pos);var renderer=(KitchenStationRenderer)(Object)m.getBlockEntityRenderDispatcher().getRenderer(e);var state=renderer.createRenderState();renderer.extractRenderState(e,state,.5f,m.player.getEyePosition(),null);result[0]=state;});return result[0];
    }
    private static void photo(ClientGameTestContext c,TestServerContext s,String name){equip(c,s,"minecraft:air");s.runCommand(name.contains("oven")&&!name.contains("raw")?"tp @a 4.5 101 -1.6 0 48":name.contains("fish")||name.equals("scorched")?"tp @a 2.5 101 -1.6 0 48":"tp @a 0.5 101 -1.6 0 48");c.waitTicks(6);c.takeScreenshot("appearance-"+name+"-0.1.12");}
    private static void cut(ClientGameTestContext c,TestServerContext s,String id){
        equip(c,s,id);click(c,BOARD,Direction.UP);equip(c,s,"smoke_kitchen:iron_knife");for(int n=0;n<4;n++)click(c,BOARD,Direction.UP);
        select(c,id.equals("minecraft:porkchop")?Items.PORKCHOP:KitchenContent.item("tomato"));click(c,BOARD,Direction.UP);
        check(snapshot(c,BOARD).pieces.size()==4,"Actual dice cutting must show four pieces on the board, not the unchanged whole icon");
        equip(c,s,"minecraft:air");c.getInput().holdKey(o->o.keyShift);c.waitTicks(2);click(c,BOARD,Direction.UP);c.getInput().releaseKey(o->o.keyShift);c.waitTicks(3);
    }
    private static void select(ClientGameTestContext c,Item item){c.runOnClient(m->{for(int i=0;i<36;i++)if(i!=8&&m.player.getInventory().getItem(i).is(item)){if(i>8){m.gameMode.handleContainerInput(m.player.inventoryMenu.containerId,i,7,net.minecraft.world.inventory.ContainerInput.SWAP,m.player);m.player.getInventory().setSelectedSlot(7);}else m.player.getInventory().setSelectedSlot(i);return;}throw new AssertionError("Actual cut ingredient missing: "+item);});c.waitTicks(4);}
    @Override public void runTest(ClientGameTestContext c){
        TestWorldSave save;
        try(var w=c.worldBuilder().create()){
            save=w.getWorldSave();var s=w.getServer();s.runCommand("time set day");s.runCommand("weather clear");s.runCommand("fill -5 99 -5 8 99 6 minecraft:smooth_stone");s.runCommand("gamemode survival @a");s.runCommand("tp @a 1 100 -1 0 25");c.waitTicks(8);
            if(net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("iris"))c.runOnClient(m->{try{var t=Class.forName("net.irisshaders.iris.api.v0.IrisApi");var api=t.getMethod("getInstance").invoke(null);check((Boolean)t.getMethod("isShaderPackInUse").invoke(api),"Iris appearance run must actually use a shader pack");}catch(ReflectiveOperationException e){throw new AssertionError(e);}});
            for(var p:new BlockPos[]{WOK,PAN,OVEN,BOARD,CROCK}){String id=p.equals(WOK)?"clay_stove":p.equals(PAN)?"brick_stove":p.equals(OVEN)?"oven":p.equals(BOARD)?"cutting_board":"fermentation_crock";equip(c,s,"smoke_kitchen:"+id);click(c,p.below(),Direction.UP);}
            equip(c,s,"smoke_kitchen:wok");click(c,WOK,Direction.UP);equip(c,s,"smoke_kitchen:frying_pan");click(c,PAN,Direction.UP);equip(c,s,"smoke_kitchen:baking_tray");click(c,OVEN,Direction.UP);
            for(String id:new String[]{"minecraft:porkchop","smoke_kitchen:tomato"})cut(c,s,id);
            select(c,Items.PORKCHOP);click(c,WOK,Direction.UP);select(c,KitchenContent.item("tomato"));click(c,WOK,Direction.UP);
            equip(c,s,"minecraft:cod");click(c,PAN,Direction.UP);equip(c,s,"minecraft:potato");click(c,OVEN,Direction.UP);
            equip(c,s,"smoke_kitchen:soybean");click(c,CROCK,Direction.UP);equip(c,s,"minecraft:air");click(c,CROCK,Direction.UP);
            var closed=snapshot(c,CROCK);check(closed.covered&&closed.pieces.isEmpty(),"Closed crock must hide every ingredient, not render it above its lid");
            s.runCommand("tp @a 0 100 1 0 48");c.waitTicks(4);c.takeScreenshot("appearance-closed-round-crock-0.1.12");click(c,CROCK,Direction.UP);check(!snapshot(c,CROCK).pieces.isEmpty(),"Opening the actual crock must reveal contents inside");c.takeScreenshot("appearance-open-round-crock-0.1.12");
            var raw=snapshot(c,WOK);check(raw.pieces.size()==8&&raw.pieces.stream().allMatch(p->p.appearance().endsWith("_raw")),"Two actually diced raw portions must show eight raw pieces");
            check(raw.vesselBase==KitchenGeometry.SOCKET_FLOOR,"Mounted model origin must sit directly on the socket floor");
            for(var p:raw.pieces)check(Math.hypot(p.x()-.5,p.z()-.5)<=.231&&p.y()+p.modelHeight()*p.scale()<=raw.vesselBase+KitchenGeometry.vesselHeight("wok"),"Every settled piece must stay in the bowl footprint and below the rim");
            photo(c,s,"raw-inset-wok-pan-oven");s.runCommand("tp @a 1.1 100 -1.8 0 22");c.waitTicks(6);c.takeScreenshot("appearance-contact-side-0.1.12");
            s.runCommand("tp @a 1 100 -1 0 30");equip(c,s,"smoke_kitchen:iron_spatula");click(c,WOK,Direction.UP);c.waitTicks(20);var mixed=snapshot(c,WOK);
            check(mixed.pieces.getFirst().x()!=raw.pieces.getFirst().x()||mixed.pieces.getFirst().z()!=raw.pieces.getFirst().z(),"A real spatula action must rearrange ingredient positions");photo(c,s,"after-stir");
            for(var p:new BlockPos[]{WOK,PAN}){equip(c,s,"smoke_kitchen:rapeseed_oil");click(c,p,Direction.UP);}
            s.runCommand("tp @a 1 100 -1 0 30");equip(c,s,"minecraft:coal");click(c,WOK,Direction.NORTH);equip(c,s,"minecraft:air");
            until(c,s,g->cooked(pot(g,WOK))>=.35,1800,"Diced food must actually reach partial cookedness");c.waitTicks(21);check(snapshot(c,WOK).pieces.stream().anyMatch(p->p.appearance().endsWith("_setting")||p.appearance().endsWith("_browning")),"Partial heating must visibly change ingredients before they are ready");photo(c,s,"part-cooked");
            for(double target:new double[]{.4,.7,.95}){until(c,s,g->cooked(pot(g,WOK))>=target,1200,"Stir-fry must progress through its actual stages");equip(c,s,"smoke_kitchen:iron_spatula");click(c,WOK,Direction.UP);}
            equip(c,s,"minecraft:air");until(c,s,g->pot(g,WOK).batch.ready(),800,"Actual stir-fry must finish");c.waitTicks(21);check(snapshot(c,WOK).pieces.stream().anyMatch(x->x.appearance().endsWith("_cooked")),"Finished stir-fry must visibly cook");photo(c,s,"cooked-wok");
            equip(c,s,"minecraft:air");c.getInput().holdKey(o->o.keyShift);c.waitTicks(2);click(c,WOK,Direction.UP);c.getInput().releaseKey(o->o.keyShift);c.waitTicks(3);for(int n=0;n<4&&observe(s,g->pot(g,WOK).heat!=0);n++)click(c,WOK,Direction.NORTH);
            equip(c,s,"minecraft:coal");click(c,PAN,Direction.NORTH);equip(c,s,"minecraft:air");
            until(c,s,g->cooked(pot(g,PAN))>=.35,1800,"Fish must actually reach partial cookedness");c.waitTicks(21);photo(c,s,"part-cooked-fish");
            s.runCommand("tp @a 1 100 -1 0 30");equip(c,s,"smoke_kitchen:tongs");click(c,PAN,Direction.UP);equip(c,s,"minecraft:air");
            until(c,s,g->pot(g,PAN).batch.ready(),1800,"Actual frying must finish");c.waitTicks(21);check(snapshot(c,PAN).pieces.stream().anyMatch(x->x.appearance().endsWith("_cooked")),"Finished fish must show its browned appearance");photo(c,s,"cooked-fish");
            s.runCommand("tp @a 1 100 -1 0 30");equip(c,s,"minecraft:air");c.getInput().holdKey(o->o.keyShift);c.waitTicks(2);click(c,PAN,Direction.UP);c.getInput().releaseKey(o->o.keyShift);c.waitTicks(3);
            s.runOnServer(g->check(g.getPlayerList().getPlayers().getFirst().getInventory().contains(x->x.is(Items.COOKED_COD)),"The changed fish visual must still yield the actual vanilla cooked item"));
            equip(c,s,"minecraft:coal");click(c,OVEN,Direction.NORTH);equip(c,s,"minecraft:air");
            until(c,s,g->pot(g,OVEN).batch.ready(),1800,"Actual baking must finish");c.waitTicks(21);check(snapshot(c,OVEN).pieces.stream().anyMatch(x->x.appearance().endsWith("_cooked")),"Baked potato must show its cooked model");photo(c,s,"cooked-oven");
            // A second real raw fish is deliberately left dry and unturned until visibly scorched.
            equip(c,s,"minecraft:cod");click(c,PAN,Direction.UP);for(int n=0;n<3;n++){equip(c,s,"minecraft:coal");click(c,PAN,Direction.NORTH);}equip(c,s,"minecraft:air");
            until(c,s,g->pot(g,PAN).batch.ingredients.getFirst().charred>=.25,2200,"Unattended hot frying must actually scorch");c.waitTicks(21);check(snapshot(c,PAN).pieces.getFirst().appearance().endsWith("_charred"),"Scorching must darken the actual in-pan fish");photo(c,s,"scorched");
            s.runCommand("tp @a 1 100 -1 0 25");equip(c,s,"minecraft:air");for(var p:new BlockPos[]{WOK,PAN,OVEN})for(int n=0;n<4&&observe(s,g->pot(g,p).heat!=0);n++)click(c,p,Direction.NORTH);
        }
        try(var w=save.open()){
            c.waitTicks(12);check(snapshot(c,WOK).pieces.size()==4,"Saved cut shapes and remaining portions must reappear after actual world reopening");
            w.getServer().runCommand("tp @a 1.2 101 -1.8 0 44");c.waitTicks(6);c.takeScreenshot("appearance-after-world-reopen-0.1.12");
        }
    }
}
