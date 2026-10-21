package dev.smokekitchen.test;

import dev.smokekitchen.KitchenConfig;
import dev.smokekitchen.content.*;
import dev.smokekitchen.world.KitchenBlockEntity;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.Vec3;

/** Actual first-meal production; world ticks advance normally, including actual chunk unload. */
public final class FreshnessFlowTest extends FirstMealFlowTest {
    private static final BlockPos SOURCE=new BlockPos(2,100,0),TABLE=new BlockPos(0,100,0),CABINET=new BlockPos(4,100,-2),ICEBOX=new BlockPos(5,100,0);
    private long prepared;
    @Override protected String[] extraRawMaterials(){return new String[]{"minecraft:oak_log 5","minecraft:ice 3","minecraft:iron_ingot 1"};}
    private static void check(boolean b,String s){if(!b)throw new AssertionError(s);}
    private static KitchenBlockEntity be(net.minecraft.server.MinecraftServer s,BlockPos p){return (KitchenBlockEntity)s.overworld().getBlockEntity(p);}
    private static long time(ItemStack food){return food.get(DataComponents.CUSTOM_DATA).copyTag().getLongOr("kitchen_prepared",Long.MIN_VALUE);}
    private static void near(SurvivalPlayer p,BlockPos pos){p.s.runCommand("tp @a "+(pos.getX()+.5)+" 100 "+(pos.getZ()+2.5)+" 180 35");p.c.waitTicks(4);}
    private static void tap(SurvivalPlayer p,BlockPos pos,Direction face){near(p,pos);p.click(pos,face);}
    private static void put(SurvivalPlayer p,String id,BlockPos pos){p.select(id);tap(p,pos,Direction.UP);}
    private static void take(SurvivalPlayer p,BlockPos pos){near(p,pos);p.emptyHand();p.c.getInput().holdKey(o->o.keyShift);p.c.waitTicks(3);p.click(pos,Direction.UP);p.c.getInput().releaseKey(o->o.keyShift);p.c.waitTicks(3);}
    private static void photo(SurvivalPlayer p,String name){p.emptyHand();near(p,ICEBOX);p.look(Vec3.atCenterOf(new BlockPos(4,100,-1)));p.c.waitTicks(5);p.c.takeScreenshot("freshness-"+name+"-0.1.16");}
    private static void waitWorld(SurvivalPlayer p,int ticks,String phase,boolean unloaded){
        long[] begun={0};p.s.runOnServer(s->begun[0]=s.overworld().getGameTime());
        p.c.runOnClient(client->{FermentationFlowTest.fastClock=true;client.options.enableVsync().set(false);client.options.framerateLimit().set(260);client.options.inactivityFpsLimit().set(net.minecraft.client.InactivityFpsLimit.MINIMIZED);client.options.pauseOnLostFocus=false;});p.s.runCommand("tick rate 200");
        try{
            for(int n=0;n<ticks+2000;n+=20){boolean[] done={false};p.s.runOnServer(s->{long elapsed=s.overworld().getGameTime()-begun[0];done[0]=elapsed>=ticks;if(unloaded)check(!s.overworld().hasChunkAt(SOURCE),"Kitchen must genuinely remain unloaded during freshness catch-up");for(var player:s.getPlayerList().getPlayers())player.sendOverlayMessage(net.minecraft.network.chat.Component.literal("自动验收："+phase+" "+Math.min(100,100*elapsed/ticks)+"% · 完整运行游戏刻，请勿操作"));});if(done[0])return;if(n==ticks/2/20*20)p.c.takeScreenshot("freshness-wait-"+(unloaded?"unloaded":"unserved")+"-0.1.16");p.c.waitTicks(20);}
            throw new AssertionError("World clock did not complete "+phase);
        }finally{p.s.runCommand("tick rate 20");p.c.runOnClient(client->FermentationFlowTest.fastClock=false);}
    }
    @Override protected boolean continueWithTransfer(SurvivalPlayer p){
        p.emptyHand();tap(p,SOURCE,Direction.UP);for(int i=0;i<2;i++){p.emptyHand();tap(p,SOURCE,Direction.NORTH);}
        p.s.runCommand("difficulty peaceful");
        p.s.runOnServer(s->{var pot=be(s,SOURCE);check(pot.heat==0,"Actual side clicks shut off the stove");prepared=pot.batch.ingredients.stream().mapToLong(x->{check(x.preparedAt!=null,"Every actually cooked ingredient records its first ready time");return x.preparedAt;}).min().orElseThrow();});
        p.emptyHand();tap(p,TABLE,Direction.UP);while(p.count("oak_planks")<25)p.craft("oak_planks","oak_log");for(int i=0;i<2;i++)p.craft("chest","oak_planks","oak_planks","oak_planks","oak_planks","","oak_planks","oak_planks","oak_planks","oak_planks");p.craft("smoke_kitchen:cabinet","chest","oak_planks");p.craft("smoke_kitchen:icebox","chest","ice","iron_ingot");p.closeMenu();put(p,"smoke_kitchen:cabinet",CABINET.below());put(p,"smoke_kitchen:icebox",ICEBOX.below());
        waitWorld(p,12000,"熟食留锅，验证装盘不能重置保鲜",false);photo(p,"unserved-after-half-day");return true;
    }
    @Override public void runTest(ClientGameTestContext c){
        int fresh=KitchenConfig.INSTANCE.freshDays,spoiled=KitchenConfig.INSTANCE.spoiledDays;boolean enabled=KitchenConfig.INSTANCE.spoilage;
        KitchenConfig.INSTANCE.freshDays=1;KitchenConfig.INSTANCE.spoiledDays=2;KitchenConfig.INSTANCE.spoilage=true;
        try{
            super.runTest(c);
            try(var world=savedWorld.open()){
                var p=new SurvivalPlayer(c,world.getServer());p.s.runOnServer(s->check(be(s,SOURCE).batch.ingredients.stream().mapToLong(x->x.preparedAt).min().orElseThrow()==prepared,"Unserved food date survives an actual world reopen"));
                put(p,"minecraft:bowl",SOURCE);put(p,"smoke_kitchen:mixed_stew",CABINET);put(p,"minecraft:bowl",SOURCE);put(p,"smoke_kitchen:mixed_stew",ICEBOX);put(p,"minecraft:ice",ICEBOX);put(p,"minecraft:ice",ICEBOX);check(p.count("ice")==0,"Both actual ice blocks must enter the icebox, not be placed outside");
                p.s.runOnServer(s->{check(time(be(s,CABINET).getItem(0))==prepared&&time(be(s,ICEBOX).getItem(0))>=prepared,"Two actual served meals retain their cooked date");check(be(s,SOURCE).batch.ingredients.isEmpty(),"Serving consumes precisely four real food portions");});photo(p,"actual-two-meals-stored");
            }
            try(var world=savedWorld.open()){
                var p=new SurvivalPlayer(c,world.getServer());
                p.s.runCommand("forceload add 2000 2000");c.waitTicks(60);p.s.runCommand("fill 2000 99 2000 2003 99 2003 minecraft:smooth_stone");p.s.runCommand("setworldspawn 2001 100 2001");p.s.runCommand("tp @a 2001.5 100 2001.5");p.s.runCommand("forceload remove 2000 2000");
                boolean unloaded=false;for(int n=0;n<1800;n+=20){boolean[] empty={false};p.s.runOnServer(s->empty[0]=!s.overworld().hasChunkAt(SOURCE));if(empty[0]){unloaded=true;break;}c.waitTicks(20);}check(unloaded,"Travel must actually unload the kitchen, not merely hide it");
                waitWorld(p,48040,"离开厨房两日：厨柜变质与有限冰块冷藏",true);
                p.s.runCommand("tp @a 6.5 100 2.5");c.waitTicks(60);
                p.s.runOnServer(s->{check(s.getPlayerList().getPlayers().getFirst().isAlive(),"Travel returns onto solid floor, away from the bucket water-source hole");var old=be(s,CABINET).getItem(0);var cold=be(s,ICEBOX).getItem(0);check(old.is(KitchenContent.item("spoiled_mixed_stew"))&&!old.has(DataComponents.FOOD),"Unloaded cabinet meal actually spoils and becomes inedible");check(cold.is(KitchenContent.item("mixed_stew")),"Two actual ice blocks keep the same cooked meal fresh over two unloaded days");check(be(s,ICEBOX).items.stream().noneMatch(x->x.is(Items.ICE))&&be(s,ICEBOX).coldTicks==0,"Exactly two ice days are exhausted; no infinite cooling");check(time(old)==prepared&&time(cold)-prepared==48000,"Original cooked date plus precisely finite cooling survives unload/reopen");});
                take(p,CABINET);take(p,ICEBOX);check(p.count("smoke_kitchen:spoiled_mixed_stew")==1&&p.count("smoke_kitchen:mixed_stew")==1,"Both actual stored meals must be physically retrieved before tooltip capture");c.runOnClient(client->{client.getToastManager().clear();client.setScreen(new ActualStoredFood());});c.waitTicks(5);c.takeScreenshot("freshness-actual-spoiled-and-cold-tooltips-0.1.16");p.closeMenu();
                p.s.runOnServer(s->s.getPlayerList().getPlayers().getFirst().getFoodData().setFoodLevel(8));p.select("smoke_kitchen:mixed_stew");c.runOnClient(client->client.player.setXRot(-70));c.getInput().holdKey(o->o.keyUse);c.waitTicks(40);c.getInput().releaseKey(o->o.keyUse);c.waitTicks(5);check(p.count("smoke_kitchen:mixed_stew")==0&&p.count("bowl")==3&&p.count("smoke_kitchen:spoiled_mixed_stew")==1,"Actual cold meal is eaten, returns its bowl, and leaves the spoiled meal inedible");photo(p,"cold-meal-eaten-bowl-returned");
            }
            try(var world=savedWorld.open()){
                var p=new SurvivalPlayer(c,world.getServer());check(p.count("smoke_kitchen:spoiled_mixed_stew")==1&&p.count("bowl")==3,"Final reopen preserves spoiled item and actual returned bowls");p.s.runOnServer(s->check(be(s,CABINET).isEmpty()&&be(s,ICEBOX).isEmpty(),"Actual retrieval empties both saved stores"));photo(p,"final-world-reopen");
            }
        }finally{KitchenConfig.INSTANCE.freshDays=fresh;KitchenConfig.INSTANCE.spoiledDays=spoiled;KitchenConfig.INSTANCE.spoilage=enabled;FermentationFlowTest.fastClock=false;}
    }
    private static final class ActualStoredFood extends net.minecraft.client.gui.screens.Screen {
        ActualStoredFood(){super(net.minecraft.network.chat.Component.literal("Actual stored meals"));}
        @Override public void extractRenderState(net.minecraft.client.gui.GuiGraphicsExtractor g,int x,int y,float delta){g.fill(0,0,width,height,0xff263039);g.centeredText(font,"0.1.16 / 同锅实际制作 · 厨柜腐坏与有限冰块冷藏",width/2,12,0xffffffff);int top=42;for(var food:minecraft.player.getInventory().getNonEquipmentItems())if(food.is(KitchenContent.item("mixed_stew"))||food.is(KitchenContent.item("spoiled_mixed_stew"))){g.item(food,24,top);for(var line:food.getTooltipLines(Item.TooltipContext.of(minecraft.level),minecraft.player,TooltipFlag.NORMAL))for(var text:font.split(line,width-80)){g.text(font,text,52,top,0xffeee8dd,false);top+=12;}top+=24;}}
    }
}
