package dev.smokekitchen.test;

import dev.smokekitchen.content.*;
import dev.smokekitchen.cooking.CookingBatch;
import dev.smokekitchen.world.KitchenBlockEntity;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.Vec3;
import java.util.function.Predicate;

/** Continues actual soybean-to-tofu production; no processed tofu, meat, oil or sauce is granted. */
public final class MapoFlowTest extends TofuFlowTest {
    private static final BlockPos TABLE=new BlockPos(0,100,-2),BOARD=new BlockPos(0,100,0),STOVE=new BlockPos(4,100,3);
    @Override protected boolean reserveTofuForDish(){return true;}
    private static void check(boolean b,String s){if(!b)throw new AssertionError(s);}
    private static KitchenBlockEntity be(net.minecraft.server.MinecraftServer s,BlockPos p){return (KitchenBlockEntity)s.overworld().getBlockEntity(p);}
    private static void tap(SurvivalPlayer p,BlockPos pos,Direction face){p.s.runCommand("tp @a "+(pos.getX()+.5)+" 100 "+(pos.getZ()+2.5)+" 180 40");p.c.waitTicks(4);p.click(pos,face);}
    private static void put(SurvivalPlayer p,String id,BlockPos pos,int n){for(int i=0;i<n;i++){p.select(id.contains(":")?id:"smoke_kitchen:"+id);tap(p,pos,Direction.UP);}}
    private static void until(SurvivalPlayer p,Predicate<net.minecraft.server.MinecraftServer> condition,int ticks,String phase){for(int i=0;i<ticks;i+=20){boolean[] ok={false};p.s.runOnServer(s->{ok[0]=condition.test(s);if(!ok[0])s.getPlayerList().getPlayers().getFirst().sendOverlayMessage(net.minecraft.network.chat.Component.literal("自动验收："+phase+" "+Math.min(100,be(s,STOVE).batch.progress(1))+"% · 请勿操作"));});if(ok[0])return;p.c.waitTicks(20);}throw new AssertionError(phase+" did not finish by real cooking");}
    private static void photo(SurvivalPlayer p,String phase){p.emptyHand();p.s.runCommand("tp @a 6.5 101 2.5 90 50");p.c.waitTicks(5);p.look(Vec3.atCenterOf(STOVE).add(0,.35,0));p.c.takeScreenshot("mapo-"+phase+"-0.1.14");}
    private static void inventory(SurvivalPlayer p,String phase){p.c.runOnClient(client->client.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(client.player)));p.c.waitTicks(5);p.c.takeScreenshot("mapo-"+phase+"-0.1.14");p.closeMenu();}
    private static void mode(SurvivalPlayer p,CookingBatch.Cut cut){p.select("smoke_kitchen:stone_knife");for(int n=0;n<5;n++){boolean[] right={false};p.s.runOnServer(s->right[0]=be(s,BOARD).cut==cut);if(right[0])return;p.c.getInput().holdKey(o->o.keyShift);p.c.waitTicks(2);tap(p,BOARD,Direction.UP);p.c.getInput().releaseKey(o->o.keyShift);p.c.waitTicks(2);}throw new AssertionError("Cut mode cannot be selected through sneaking knife actions");}
    private static void cut(SurvivalPlayer p,String id,CookingBatch.Cut cut){mode(p,cut);p.select(id,stack->!stack.getOrDefault(DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.EMPTY).copyTag().contains("kitchen_cut"));tap(p,BOARD,Direction.UP);put(p,"stone_knife",BOARD,4);}
    @Override public void runTest(ClientGameTestContext c){
        super.runTest(c);
        try(var world=savedWorld.open()){
            var p=new SurvivalPlayer(c,world.getServer());check(p.count("smoke_kitchen:tofu")==2,"Both actually pressed tofu must remain for the dish");
            // Only basic construction resources, fresh spices and a live pig are fixtures.
            for(String id:new String[]{"oak_planks","stick","cobblestone","clay_ball","iron_ingot","coal","terracotta","string"})p.s.runCommand("clear @a minecraft:"+id);
            for(String raw:new String[]{"minecraft:oak_planks 4","minecraft:stick 9","minecraft:cobblestone 9","minecraft:clay_ball 7","minecraft:iron_ingot 5","minecraft:coal 4","smoke_kitchen:chili 1","smoke_kitchen:sichuan_pepper 1"})p.s.runCommand("give @a "+raw);c.waitTicks(8);
            p.emptyHand();tap(p,TABLE,Direction.UP);
            p.craft("stone_axe","cobblestone","cobblestone","","cobblestone","stick","","","stick","");
            p.craft("smoke_kitchen:stone_knife","","cobblestone","","cobblestone","","","stick","","");
            p.craft("smoke_kitchen:cutting_board","oak_planks","oak_planks","oak_planks","","stick","","stick","","");
            p.craft("smoke_kitchen:wooden_spatula","","oak_planks","","","stick","","stick","","");
            p.craft("smoke_kitchen:wok","iron_ingot","","iron_ingot","iron_ingot","iron_ingot","iron_ingot","","stick","");
            p.craft("smoke_kitchen:clay_stove","clay_ball","stick","clay_ball","clay_ball","cobblestone","clay_ball","cobblestone","cobblestone","cobblestone");
            p.craft("smoke_kitchen:ceramic_plate","clay_ball","clay_ball","clay_ball");p.closeMenu();inventory(p,"crafted-kitchen");
            p.select("smoke_kitchen:cutting_board");tap(p,BOARD.below(),Direction.UP);p.select("smoke_kitchen:clay_stove");tap(p,STOVE.below(),Direction.UP);put(p,"wok",STOVE,1);
            p.s.runCommand("fill 6 100 1 7 100 2 minecraft:smooth_stone");p.s.runCommand("summon minecraft:pig -4.5 100 -3.5 {NoAI:1b}");p.s.runCommand("tp @a -4.5 100 -1.5 180 20");c.waitTicks(8);p.select("stone_axe");
            for(int n=0;n<8;n++){Integer id=c.computeOnClient(client->{for(var e:client.level.entitiesForRendering())if(e.getType()==EntityType.PIG&&e.isAlive())return e.getId();return null;});if(id==null)break;p.c.runOnClient(client->{var e=client.level.getEntity(id);check(e!=null&&client.player.distanceTo(e)<3,"Pig must be struck within actual reach");client.gameMode.attack(client.player,e);client.player.swing(InteractionHand.MAIN_HAND);});c.waitTicks(25);}c.waitTicks(22);p.pickUpAround(new Vec3(-4.5,100,-3.5));check(p.count("porkchop")>0&&p.count("smoke_kitchen:rendered_fat")==1,"A real pig kill must drop pork and fat");inventory(p,"actual-pig-loot");
            put(p,"rendered_fat",STOVE,1);put(p,"minecraft:coal",STOVE,3);p.emptyHand();for(int n=0;n<3;n++)tap(p,STOVE,Direction.NORTH);until(p,s->be(s,STOVE).batch.ready(),2200,"小火炼猪油");put(p,"empty_jar",STOVE,1);check(p.count("smoke_kitchen:used_lard")==1&&p.count("smoke_kitchen:fat_cracklings")==1,"Actual fat must render four-use lard and crackling");p.emptyHand();for(int n=0;n<3;n++)tap(p,STOVE,Direction.NORTH);
            cut(p,"porkchop",CookingBatch.Cut.MINCE);for(int n=0;n<2;n++)cut(p,"smoke_kitchen:tofu",CookingBatch.Cut.DICE);inventory(p,"actual-mince-and-dice");
            put(p,"lard",STOVE,1);p.emptyHand();tap(p,STOVE,Direction.NORTH);until(p,s->be(s,STOVE).batch.temperature>=110,1000,"热锅热油");
            p.select("porkchop",x->x.getOrDefault(DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.EMPTY).copyTag().getStringOr("kitchen_cut","").equals("MINCE"));tap(p,STOVE,Direction.UP);photo(p,"raw-mince-in-hot-oil");until(p,s->be(s,STOVE).batch.ingredients.getFirst().cooked>=.65,1400,"炒香肉末");
            put(p,"water_jug",STOVE,1);put(p,"tofu",STOVE,2);put(p,"chili",STOVE,1);put(p,"sichuan_pepper",STOVE,1);put(p,"soy_sauce",STOVE,1);put(p,"wooden_spatula",STOVE,3);photo(p,"tofu-spices-and-light-sauce");
            p.s.runOnServer(s->{var b=be(s,STOVE).batch;var r=ContentCatalog.DISHES.stream().filter(x->x.id().equals("mapo_tofu")).findFirst().orElseThrow();check(r.matches(b)&&!b.ready(),"Real preparation must satisfy recipe while the last spices still need heat");});
        }
        try(var world=savedWorld.open()){
            var p=new SurvivalPlayer(c,world.getServer());p.s.runOnServer(s->{var b=be(s,STOVE).batch;check(b.count("tofu")==2&&b.count("sichuan_pepper")==1&&b.stirs-b.stirsAtLastAddition>=3&&b.ingredients.get(1).priorCooked.get("pork")>=.6,"Partial dish must preserve cuts, spice, order and stirs after actual reopen");});
            until(p,s->be(s,STOVE).batch.ready(),2200,"豆腐与香料煨熟");photo(p,"ready-in-wok");p.s.runOnServer(s->check(be(s,STOVE).batch.quality()==3,"Proper real cooking must make highest-quality unburnt Mapo tofu"));
            put(p,"ceramic_plate",STOVE,1);check(p.count("smoke_kitchen:mapo_tofu")==1,"Actual ceramic serving must name the dish");p.emptyHand();for(int n=0;n<3;n++)tap(p,STOVE,Direction.NORTH);inventory(p,"plated-original-model");
            p.s.runOnServer(s->s.getPlayerList().getPlayers().getFirst().getFoodData().setFoodLevel(8));p.select("smoke_kitchen:mapo_tofu");c.runOnClient(client->client.player.setXRot(-70));c.getInput().holdKey(o->o.keyUse);c.waitTicks(35);c.getInput().releaseKey(o->o.keyUse);c.waitTicks(5);check(p.count("smoke_kitchen:mapo_tofu")==0&&p.count("smoke_kitchen:ceramic_plate")==2,"Eating must consume the real meal and return its ceramic plate");p.s.runOnServer(s->check(s.getPlayerList().getPlayers().getFirst().getFoodData().getFoodLevel()>8&&be(s,STOVE).batch.remainingServings==1&&be(s,STOVE).heat==0,"Eating restores hunger and leaves one serving with heat off"));
        }
        try(var world=savedWorld.open()){
            var p=new SurvivalPlayer(c,world.getServer());p.s.runOnServer(s->check(be(s,STOVE).batch.remainingServings==1&&be(s,STOVE).batch.lockedDish.equals("mapo_tofu"),"Saved last serving must retain its dish"));put(p,"ceramic_plate",STOVE,1);check(p.count("smoke_kitchen:mapo_tofu")==1&&p.count("smoke_kitchen:ceramic_plate")==1,"Last saved portion must serve without duplication");inventory(p,"last-serving-after-reopen");
        }
    }
}
