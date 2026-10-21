package dev.smokekitchen.test;

import dev.smokekitchen.content.*;
import dev.smokekitchen.world.KitchenBlockEntity;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.Vec3;
import java.util.function.Predicate;

/** Raw recipe ingredients only. Vanilla tick-rate acceleration runs the complete long fermentation, without editing work. */
public final class FermentationFlowTest implements FabricClientGameTest {
    public static volatile boolean fastClock;
    private static void rate(ClientGameTestContext c,TestServerContext s,boolean fast){c.runOnClient(client->{fastClock=fast;if(fast){client.options.enableVsync().set(false);client.options.framerateLimit().set(260);client.options.inactivityFpsLimit().set(net.minecraft.client.InactivityFpsLimit.MINIMIZED);client.options.pauseOnLostFocus=false;}});s.runCommand("tick rate "+(fast?200:20));}
    private static final BlockPos TABLE=new BlockPos(0,100,-2),MILL=new BlockPos(-2,100,0),MIX=new BlockPos(0,100,2),SOY=new BlockPos(2,100,2),VINEGAR=new BlockPos(3,100,2),BEANS=new BlockPos(2,100,0),RICE=new BlockPos(-2,100,2),WATER=new BlockPos(4,99,-1);
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    private static KitchenBlockEntity station(net.minecraft.server.MinecraftServer g,BlockPos p){return (KitchenBlockEntity)g.overworld().getBlockEntity(p);}
    private static int stored(KitchenBlockEntity x,String id){return x.items.stream().filter(s->s.is(KitchenContent.item(id))).mapToInt(ItemStack::getCount).sum();}
    private static void waiting(net.minecraft.server.MinecraftServer g,String phase){
        int percent=phase.contains("cultures")?Math.min(stored(station(g,SOY),"starter")==2?1200:station(g,SOY).work,stored(station(g,VINEGAR),"vinegar_starter")==2?1200:station(g,VINEGAR).work)*100/1200:phase.contains("brews")?Math.min(stored(station(g,SOY),"soy_mash")==1?2400:station(g,SOY).work,stored(station(g,VINEGAR),"vinegar_mash")==1?2400:station(g,VINEGAR).work)*100/2400:phase.contains("Beans")?Math.min(station(g,BEANS).batch.progress(1),station(g,RICE).batch.progress(1)):station(g,RICE).batch.progress(1);
        String text=phase.contains("cultures")?"培养第一代引子":phase.contains("brews")?"密封发酵，之后过滤":phase.contains("Beans")?"豆和米正在煮熟":"正在煮使用新调料的饭菜";
        for(var player:g.getPlayerList().getPlayers())player.sendOverlayMessage(net.minecraft.network.chat.Component.literal("自动验收："+text+" "+Math.min(100,percent)+"% · 等待中，请勿操作"));
    }
    private static void until(ClientGameTestContext c,TestServerContext s,Predicate<net.minecraft.server.MinecraftServer> condition,int ticks,String message){for(int n=0;n<ticks;n+=20){boolean[] done={false};s.runOnServer(g->{done[0]=condition.test(g);if(!done[0])waiting(g,message);});if(done[0])return;if(n==24000&&message.contains("brews"))c.takeScreenshot("fermentation-wait-progress-0.1.12");c.waitTicks(20);}throw new AssertionError(message);}
    private static void spot(SurvivalPlayer p,BlockPos pos){int dz=pos.getZ()<=0?-2:2;p.s.runCommand("tp @a "+(pos.getX()+.5)+" 100 "+(pos.getZ()+.5+dz)+" 0 35");p.c.waitTicks(4);}
    private static void tap(SurvivalPlayer p,BlockPos pos,Direction face){spot(p,pos);p.click(pos,face);}
    private static void place(SurvivalPlayer p,String id,BlockPos pos){p.select(id);tap(p,pos.below(),Direction.UP);}
    private static void put(SurvivalPlayer p,String id,BlockPos pos,int count){for(int i=0;i<count;i++){p.select(id);tap(p,pos,Direction.UP);}}
    private static void takeAll(SurvivalPlayer p,BlockPos pos){
        p.emptyHand();p.c.getInput().holdKey(o->o.keyShift);p.c.waitTicks(3);
        for(int i=0;i<28;i++){boolean[] empty={false};p.s.runOnServer(g->empty[0]=station(g,pos).items.stream().allMatch(ItemStack::isEmpty));if(empty[0])break;p.emptyHand();tap(p,pos,Direction.UP);}
        p.c.getInput().releaseKey(o->o.keyShift);p.c.waitTicks(3);
    }
    private static void fillWater(SurvivalPlayer p,BlockPos pos){p.select("bucket");spot(p,WATER);p.look(Vec3.atCenterOf(WATER).add(0,.4,0));p.c.runOnClient(client->client.gameMode.useItem(client.player,InteractionHand.MAIN_HAND));p.c.waitTicks(6);check(p.count("water_bucket")==1,"The crafted bucket must actually fill");put(p,"water_bucket",pos,1);}
    private static void shut(SurvivalPlayer p,BlockPos pos){p.emptyHand();for(int i=0;i<2;i++)tap(p,pos,Direction.NORTH);}
    private static void view(ClientGameTestContext c,String name){c.runOnClient(client->client.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(client.player)));c.waitTicks(4);c.takeScreenshot(name);c.runOnClient(client->{client.player.closeContainer();client.setScreen(null);});c.waitTicks(4);}
    @Override public void runTest(ClientGameTestContext c){
        TestWorldSave save;int[] partial={0};long[] begun={0};
        try(var world=c.worldBuilder().create()){
            save=world.getWorldSave();var s=world.getServer();var p=new SurvivalPlayer(c,s);c.runOnClient(client->client.options.guiScale().set(2));
            s.runCommand("fill -6 99 -5 7 99 6 minecraft:smooth_stone");s.runCommand("fill -6 100 -5 7 105 6 minecraft:air");s.runCommand("time set day");s.runCommand("weather clear");s.runCommand("gamemode survival @a");s.runCommand("tp @a 0.5 100 -3.5 0 35");s.runCommand("fill 4 99 -1 5 99 0 minecraft:water");
            for(String raw:new String[]{"minecraft:oak_planks 40","minecraft:stick 24","minecraft:cobblestone 24","minecraft:clay_ball 24","minecraft:terracotta 6","minecraft:string 6","minecraft:smooth_stone 1","minecraft:glass 8","minecraft:iron_ingot 6","minecraft:coal 12","smoke_kitchen:soybean 4","smoke_kitchen:rice_paddy 6","minecraft:wheat 2","smoke_kitchen:shiitake 1","minecraft:sugar 1","minecraft:apple 1","smoke_kitchen:salt_crystals 4","minecraft:potato 2","minecraft:book 1","minecraft:wheat_seeds 1"})s.runCommand("give @a "+raw);c.waitTicks(8);
            if(net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("iris"))c.runOnClient(client->{try{var api=Class.forName("net.irisshaders.iris.api.v0.IrisApi");check((Boolean)api.getMethod("isShaderPackInUse").invoke(api.getMethod("getInstance").invoke(null)),"Iris shader must really be active");}catch(ReflectiveOperationException e){throw new AssertionError(e);}});
            p.craft("crafting_table","oak_planks","oak_planks","oak_planks","oak_planks");p.closeMenu();place(p,"crafting_table",TABLE);p.emptyHand();tap(p,TABLE,Direction.UP);
            for(int i=0;i<4;i++)p.craft("smoke_kitchen:wooden_lid","oak_planks","oak_planks","","","stick","");
            for(int i=0;i<2;i++)p.craft("smoke_kitchen:fermentation_crock","terracotta","terracotta","terracotta","smoke_kitchen:wooden_lid","string");
            p.craft("smoke_kitchen:sieve","stick","string");p.craft("smoke_kitchen:rolling_pin","oak_planks","oak_planks","stick","stick","stick");p.craft("bowl","oak_planks","","oak_planks","","oak_planks","");p.craft("smoke_kitchen:counter","oak_planks","oak_planks","smooth_stone");p.craft("smoke_kitchen:mixing_table","smoke_kitchen:counter","bowl","smoke_kitchen:rolling_pin");
            p.craft("smoke_kitchen:hand_mill","","stick","","cobblestone","cobblestone","cobblestone","cobblestone","cobblestone","cobblestone");
            for(int i=0;i<2;i++){p.craft("smoke_kitchen:clay_stove","clay_ball","stick","clay_ball","clay_ball","cobblestone","clay_ball","cobblestone","cobblestone","cobblestone");p.craft("smoke_kitchen:clay_pot","clay_ball","","clay_ball","clay_ball","","clay_ball","","clay_ball","");}
            p.craft("bucket","iron_ingot","","iron_ingot","","iron_ingot","");p.craft("smoke_kitchen:empty_water_jug","iron_ingot","iron_ingot","iron_ingot","glass","glass");p.craft("smoke_kitchen:empty_jar","glass","glass","glass","oak_planks");p.craft("glass_bottle","glass","","glass","","glass","");p.craft("smoke_kitchen:recipe_book","book","wheat_seeds");p.closeMenu();
            for(var row:new Object[][]{{MILL,"hand_mill"},{MIX,"mixing_table"},{SOY,"fermentation_crock"},{VINEGAR,"fermentation_crock"},{BEANS,"clay_stove"},{RICE,"clay_stove"}})place(p,"smoke_kitchen:"+row[1],(BlockPos)row[0]);for(var pos:new BlockPos[]{BEANS,RICE})put(p,"smoke_kitchen:clay_pot",pos,1);
            view(c,"fermentation-actual-crafting-0.1.12");
            put(p,"smoke_kitchen:soybean",BEANS,4);put(p,"coal",BEANS,1);c.waitTicks(80);s.runOnServer(g->check(station(g,BEANS).batch.ingredients.stream().allMatch(x->x.cooked==0),"Dry soybeans must remain raw under actual heat"));fillWater(p,BEANS);put(p,"smoke_kitchen:wooden_lid",BEANS,1);put(p,"coal",BEANS,3);
            put(p,"smoke_kitchen:rice_paddy",MILL,6);p.emptyHand();for(int i=0;i<6;i++)tap(p,MILL,Direction.UP);takeAll(p,MILL);put(p,"smoke_kitchen:rice",RICE,6);fillWater(p,RICE);put(p,"smoke_kitchen:wooden_lid",RICE,1);put(p,"coal",RICE,4);
            put(p,"wheat",MILL,2);p.emptyHand();tap(p,MILL,Direction.UP);tap(p,MILL,Direction.UP);takeAll(p,MILL);put(p,"smoke_kitchen:salt_crystals",MILL,4);put(p,"smoke_kitchen:empty_jar",MILL,1);p.emptyHand();tap(p,MILL,Direction.UP);takeAll(p,MILL);
            until(c,s,g->station(g,BEANS).batch.ready()&&station(g,RICE).batch.ready(),3600,"Beans and rice must actually finish their own wet cooking");
            for(var pos:new BlockPos[]{BEANS,RICE}){shut(p,pos);p.emptyHand();tap(p,pos,Direction.UP);takeAll(p,pos);}check(p.count("smoke_kitchen:cooked_soybean")==4&&p.count("smoke_kitchen:cooked_rice")==6,"Cooking must return four cooked beans and six loose cooked rice portions");view(c,"fermentation-wet-cooked-ingredients-0.1.12");
            put(p,"smoke_kitchen:cooked_rice",SOY,1);put(p,"smoke_kitchen:shiitake",SOY,1);put(p,"sugar",SOY,1);put(p,"smoke_kitchen:cooked_rice",VINEGAR,1);put(p,"apple",VINEGAR,1);
            p.select("smoke_kitchen:empty_water_jug");spot(p,WATER);p.look(Vec3.atCenterOf(WATER).add(0,.4,0));c.runOnClient(client->client.gameMode.useItem(client.player,InteractionHand.MAIN_HAND));c.waitTicks(6);put(p,"smoke_kitchen:water_jug",VINEGAR,1);p.emptyHand();tap(p,SOY,Direction.UP);tap(p,VINEGAR,Direction.UP);
            // Vanilla ticking executes every ordinary block tick. No work, timestamp or recipe duration is overwritten.
            rate(c,s,true);for(int n=0;n<1600;n+=20){s.runOnServer(g->waiting(g,"Initial cultures"));c.waitTicks(20);}s.runOnServer(g->{partial[0]=station(g,SOY).work;check(partial[0]>0&&stored(station(g,SOY),"starter")==0,"Partially aged first culture must not finish early");});rate(c,s,false);
        }
        try(var world=save.open()){
            var s=world.getServer();var p=new SurvivalPlayer(c,s);s.runOnServer(g->check(station(g,SOY).work>=partial[0]&&stored(station(g,SOY),"starter")==0,"Actual world reopening must retain partial fermentation without finishing it"));rate(c,s,true);until(c,s,g->stored(station(g,SOY),"starter")==2&&stored(station(g,VINEGAR),"vinegar_starter")==2,30000,"Initial cultures must run their full 1200 game seconds");rate(c,s,false);
            p.emptyHand();tap(p,SOY,Direction.UP);tap(p,VINEGAR,Direction.UP);takeAll(p,SOY);takeAll(p,VINEGAR);
            put(p,"smoke_kitchen:cooked_soybean",SOY,4);put(p,"smoke_kitchen:flour",SOY,2);put(p,"smoke_kitchen:used_salt",SOY,1);put(p,"smoke_kitchen:starter",SOY,1);put(p,"smoke_kitchen:empty_jar",SOY,1);put(p,"smoke_kitchen:water_jug",SOY,1);
            put(p,"smoke_kitchen:cooked_rice",VINEGAR,4);put(p,"smoke_kitchen:vinegar_starter",VINEGAR,1);put(p,"smoke_kitchen:empty_jar",VINEGAR,1);put(p,"smoke_kitchen:water_jug",VINEGAR,1);
            p.emptyHand();tap(p,SOY,Direction.UP);tap(p,VINEGAR,Direction.UP);s.runOnServer(g->{var soy=station(g,SOY);var vinegar=station(g,VINEGAR);check(stored(soy,"cooked_soybean")==4&&stored(soy,"flour")==2&&stored(soy,"starter")==1&&stored(soy,"empty_jar")==1&&soy.batch.water==1&&soy.getBlockState().getValue(dev.smokekitchen.world.KitchenBlock.LID),"Every real cooked bean, flour, culture, jar and water measure must be loaded before the full wait");check(stored(vinegar,"cooked_rice")==4&&stored(vinegar,"vinegar_starter")==1&&stored(vinegar,"empty_jar")==1&&vinegar.batch.water==1&&vinegar.getBlockState().getValue(dev.smokekitchen.world.KitchenBlock.LID),"All four separately cooked rice portions must be loaded into the sealed vinegar crock");begun[0]=g.overworld().getGameTime();});rate(c,s,true);
            until(c,s,g->stored(station(g,SOY),"soy_mash")==1&&stored(station(g,VINEGAR),"vinegar_mash")==1,54000,"Both real brews must finish their full 2400 game seconds");rate(c,s,false);s.runOnServer(g->check(g.overworld().getGameTime()-begun[0]>=47900&&stored(station(g,SOY),"soy_sauce")==0&&stored(station(g,VINEGAR),"rice_vinegar")==0,"Elapsed server ticks must produce only unfiltered mash, never finished bottles"));
            p.emptyHand();p.look(Vec3.atCenterOf(SOY));c.takeScreenshot("fermentation-finished-crocks-0.1.12");tap(p,SOY,Direction.UP);tap(p,VINEGAR,Direction.UP);takeAll(p,SOY);takeAll(p,VINEGAR);view(c,"fermentation-actual-unfiltered-mashes-0.1.12");
            for(String[] row:new String[][]{{"soy_mash","soy_sauce"},{"vinegar_mash","rice_vinegar"}}){put(p,"smoke_kitchen:"+row[0],MIX,1);put(p,"glass_bottle",MIX,1);p.emptyHand();tap(p,MIX,Direction.UP);check(p.count("smoke_kitchen:"+row[1])==0,"Hand mixing must not make filtered sauce");put(p,"smoke_kitchen:sieve",MIX,1);takeAll(p,MIX);check(p.count("smoke_kitchen:"+row[1])==1,"An actual sieve action must produce one filtered bottle");}
            check(p.count("smoke_kitchen:empty_jar")==3,"Both mash carrier jars must return without consuming the spare jar");s.runOnServer(g->{var sieve=g.getPlayerList().getPlayers().getFirst().getInventory().getNonEquipmentItems().stream().filter(x->x.is(KitchenContent.item("used_sieve"))).findFirst().orElseThrow();check(sieve.getDamageValue()==2,"Both actual filtering operations must use precisely two sieve durability");});view(c,"fermentation-filtered-bottles-0.1.12");
            put(p,"smoke_kitchen:soy_sauce",RICE,1);put(p,"smoke_kitchen:rice_vinegar",RICE,1);put(p,"potato",RICE,2);put(p,"coal",RICE,2);until(c,s,g->station(g,RICE).batch.ready(),2600,"The actually filtered seasonings must be usable in a real cooked meal");shut(p,RICE);put(p,"bowl",RICE,1);check(p.count("smoke_kitchen:mixed_stew")==1,"A full cooked meal must be served with the new condiments");s.runOnServer(g->g.getPlayerList().getPlayers().getFirst().getFoodData().setFoodLevel(8));p.select("smoke_kitchen:mixed_stew");c.runOnClient(client->client.player.setXRot(-70));c.getInput().holdKey(o->o.keyUse);c.waitTicks(40);c.getInput().releaseKey(o->o.keyUse);c.waitTicks(5);check(p.count("smoke_kitchen:mixed_stew")==0,"The actual new-condiment meal must be eaten");s.runOnServer(g->{var player=g.getPlayerList().getPlayers().getFirst();check(player.getFoodData().getFoodLevel()>8&&player.getInventory().countItem(Items.BOWL)==3,"Actual eating must restore hunger and return the original bowl");});view(c,"fermentation-after-real-meal-0.1.12");
        }
        try(var world=save.open()){
            c.waitTicks(8);world.getServer().runOnServer(g->{var inv=g.getPlayerList().getPlayers().getFirst().getInventory();for(String id:new String[]{"used_soy_sauce","used_rice_vinegar"}){var stack=inv.getNonEquipmentItems().stream().filter(x->x.is(KitchenContent.item(id))).findFirst().orElseThrow();check(stack.getMaxDamage()-stack.getDamageValue()==7,"Used condiment must preserve exactly seven measures after reopening");}check(inv.countItem(KitchenContent.item("empty_jar"))==3,"Returned jars must survive reopening");});c.takeScreenshot("fermentation-after-world-reopen-0.1.12");
        }
    }
}
