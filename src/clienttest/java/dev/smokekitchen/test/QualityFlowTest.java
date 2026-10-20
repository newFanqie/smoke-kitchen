package dev.smokekitchen.test;

import dev.smokekitchen.KitchenConfig;
import dev.smokekitchen.content.*;
import dev.smokekitchen.cooking.CookingBatch;
import dev.smokekitchen.world.KitchenBlockEntity;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.phys.*;
import java.util.*;
import java.util.function.Predicate;

/** Real cooking comparison, optional contact damage and loose-food transfer through normal packets. */
public final class QualityFlowTest implements FabricClientGameTest {
    private static final BlockPos[] PANS={new BlockPos(0,100,0),new BlockPos(2,100,0),new BlockPos(4,100,0)};
    private static final BlockPos FIRST=new BlockPos(6,100,0),SECOND=new BlockPos(8,100,0);
    private static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
    private static KitchenBlockEntity pot(MinecraftServer s,BlockPos p){return (KitchenBlockEntity)s.overworld().getBlockEntity(p);}
    private static void equip(ClientGameTestContext c,TestServerContext s,String id){c.runOnClient(client->client.player.getInventory().setSelectedSlot(8));s.runCommand("item replace entity @a hotbar.8 with "+id);c.waitTicks(5);}
    private static void click(ClientGameTestContext c,BlockPos p,Direction f){c.runOnClient(client->client.gameMode.useItemOn(client.player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(p),f,p,false)));c.waitTicks(6);}
    private static boolean observe(TestServerContext s,Predicate<MinecraftServer> p){boolean[] b={false};s.runOnServer(g->b[0]=p.test(g));return b[0];}
    private static void until(ClientGameTestContext c,TestServerContext s,Predicate<MinecraftServer> p,int ticks,String message){for(int i=0;i<ticks;i+=20){if(observe(s,p))return;c.waitTicks(20);}check(observe(s,p),message);}
    private static void off(ClientGameTestContext c,TestServerContext s,BlockPos p){equip(c,s,"minecraft:air");for(int i=0;i<4&&!observe(s,g->pot(g,p).heat==0);i++)click(c,p,Direction.NORTH);check(observe(s,g->pot(g,p).heat==0),"Actual controls must turn off the burner");}
    private static void select(ClientGameTestContext c,Predicate<ItemStack> predicate){c.runOnClient(client->{for(int i=0;i<36;i++)if(i!=8&&predicate.test(client.player.getInventory().getItem(i))){if(i>8){client.gameMode.handleContainerInput(client.player.inventoryMenu.containerId,i,7,net.minecraft.world.inventory.ContainerInput.SWAP,client.player);client.player.getInventory().setSelectedSlot(7);}else client.player.getInventory().setSelectedSlot(i);return;}throw new AssertionError("Actual prepared food is missing");});c.waitTicks(5);}
    private static String reasons(ItemStack s){return s.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getStringOr("kitchen_quality_reasons","");}
    private static int grade(ItemStack s){return s.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getIntOr("kitchen_quality",-1);}
    @Override public void runTest(ClientGameTestContext c) {
        boolean burns=KitchenConfig.INSTANCE.burns,hints=KitchenConfig.INSTANCE.detailedHints;TestWorldSave save;int[] gas=new int[5];double[] looseState=new double[3];
        try {
            KitchenConfig.INSTANCE.burns=false;KitchenConfig.INSTANCE.detailedHints=true;
            try(var world=c.worldBuilder().create()) {
                save=world.getWorldSave();var s=world.getServer();s.runCommand("time set day");s.runCommand("weather clear");s.runCommand("fill -3 99 -5 11 99 5 minecraft:smooth_stone");s.runCommand("tp @a 4 100 -3 0 25");s.runCommand("gamemode survival @a");c.waitTicks(10);
                for(var p:new BlockPos[]{PANS[0],PANS[1],PANS[2],FIRST,SECOND}){equip(c,s,"smoke_kitchen:gas_stove");click(c,p.below(),Direction.UP);equip(c,s,"smoke_kitchen:"+(p.equals(FIRST)||p.equals(SECOND)?"frying_pan":"wok"));click(c,p,Direction.UP);}
                for(int i=0;i<3;i++){var p=PANS[i];equip(c,s,"smoke_kitchen:rapeseed_oil");click(c,p,Direction.UP);equip(c,s,"smoke_kitchen:salt");for(int n=0;n<(i==2?4:1);n++)click(c,p,Direction.UP);for(int n=0;n<4;n++){equip(c,s,"minecraft:chicken");click(c,p,Direction.UP);}}
                for(var p:PANS){equip(c,s,"smoke_kitchen:gas_canister");click(c,p,Direction.NORTH);}
                for(int n=0;n<30&&!observe(s,g->Arrays.stream(PANS).allMatch(p->pot(g,p).batch.ready()));n++){equip(c,s,"smoke_kitchen:wooden_spatula");for(var p:new BlockPos[]{PANS[0],PANS[2]})for(int j=0;j<3;j++)click(c,p,Direction.UP);c.waitTicks(70);}
                check(observe(s,g->Arrays.stream(PANS).allMatch(p->pot(g,p).batch.ready()&&!pot(g,p).batch.burnt())),"All three actual batches must finish before burning");for(var p:PANS)off(c,s,p);
                s.runOnServer(g->{check(pot(g,PANS[0]).batch.quality()==3,"Timely stirring and balanced salt must produce good food");check(pot(g,PANS[1]).batch.quality()<3&&pot(g,PANS[1]).batch.qualityReasons().contains("uneven"),"Neglected food must explain the missing stirring");check(pot(g,PANS[2]).batch.quality()==1&&pot(g,PANS[2]).batch.qualityReasons().contains("salty"),"Excess salt must remain a distinct cause");KitchenConfig.INSTANCE.burns=true;KitchenConfig.INSTANCE.detailedHints=false;});
                float[] health={0};s.runOnServer(g->health[0]=g.getPlayerList().getPlayers().getFirst().getHealth());equip(c,s,"minecraft:air");s.runCommand("tp @a 0 100 -1 0 55");c.getInput().holdKey(o->o.keyShift);c.waitTicks(3);click(c,PANS[0],Direction.UP);c.getInput().releaseKey(o->o.keyShift);s.runOnServer(g->{check(g.getPlayerList().getPlayers().getFirst().getHealth()<health[0],"Enabled hot-floor damage must really lower player health");check(pot(g,PANS[0]).batch.ingredients.size()==4,"Hot contact must not consume or transfer food");});c.takeScreenshot("quality-hot-bare-hands-0.1.7");s.runOnServer(g->KitchenConfig.INSTANCE.burns=false);
                for(var p:PANS){equip(c,s,"minecraft:bowl");click(c,p,Direction.UP);}
                s.runOnServer(g->{var inventory=g.getPlayerList().getPlayers().getFirst().getInventory().getNonEquipmentItems();var meals=inventory.stream().filter(x->x.is(KitchenContent.item("mixed_stir_fry"))).toList();check(meals.size()==3,"Three real dishes must remain separate by their actual quality components");check(meals.stream().anyMatch(x->grade(x)==3)&&meals.stream().anyMatch(x->reasons(x).contains("salty"))&&meals.stream().anyMatch(x->reasons(x).contains("uneven")),"Serving with detailed hints disabled must retain all quality explanations");});
                c.runOnClient(client->{var meals=new ArrayList<ItemStack>();for(int i=0;i<36;i++){var stack=client.player.getInventory().getItem(i);if(stack.is(KitchenContent.item("mixed_stir_fry")))meals.add(stack.copy());}check(meals.size()==3,"Client must receive the actual three server-made dishes");client.setScreen(new Comparison(meals));});c.waitTicks(120);c.takeScreenshot("quality-real-dish-comparison-0.1.7");c.runOnClient(client->client.setScreen(null));
                s.runCommand("tp @a 6 100 -1 0 55");for(var p:new BlockPos[]{FIRST,SECOND})for(String id:new String[]{"rapeseed_oil","salt"}){equip(c,s,"smoke_kitchen:"+id);click(c,p,Direction.UP);}equip(c,s,"smoke_kitchen:gas_canister");click(c,FIRST,Direction.NORTH);until(c,s,g->pot(g,FIRST).batch.temperature>=130,600,"Transfer test oil must actually preheat");equip(c,s,"minecraft:cod");click(c,FIRST,Direction.UP);until(c,s,g->pot(g,FIRST).batch.ingredients.getFirst().cooked>=.4,1600,"The fish must actually set before turning");equip(c,s,"smoke_kitchen:tongs");click(c,FIRST,Direction.UP);off(c,s,FIRST);equip(c,s,"minecraft:air");c.getInput().holdKey(o->o.keyShift);c.waitTicks(3);click(c,FIRST,Direction.UP);c.getInput().releaseKey(o->o.keyShift);c.waitTicks(3);
                s.runOnServer(g->{var loose=g.getPlayerList().getPlayers().getFirst().getInventory().getNonEquipmentItems().stream().filter(x->x.is(Items.COD)&&x.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().contains("kitchen_portion")).findFirst().orElseThrow();var p=new com.google.gson.Gson().fromJson(loose.get(DataComponents.CUSTOM_DATA).copyTag().getStringOr("kitchen_portion",""),CookingBatch.Portion.class);looseState[0]=p.cooked;looseState[1]=p.charred;looseState[2]=p.maximumTemperature;check(p.turnedAfterSetting&&p.cooked<1,"Loose intermediate food must retain the actual warm turn while still raw");});
                select(c,x->x.is(Items.COD)&&x.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().contains("kitchen_portion"));click(c,SECOND,Direction.UP);s.runOnServer(g->{var p=pot(g,SECOND).batch.ingredients.getFirst();check(Math.abs(p.cooked-looseState[0])<.001&&p.charred==looseState[1]&&p.maximumTemperature>=looseState[2]&&p.turnedAfterSetting,"Another cold pan must preserve exact intermediate state and heating history");check(p.priorCounts.isEmpty(),"The new pan must record its own actual ingredient neighbours");});s.runCommand("tp @a 8 100 -1 0 55");c.waitTicks(3);c.takeScreenshot("quality-actual-half-cooked-transfer-0.1.7");equip(c,s,"smoke_kitchen:gas_canister");click(c,SECOND,Direction.NORTH);equip(c,s,"minecraft:air");until(c,s,g->pot(g,SECOND).batch.ready(),1600,"The moved food must continue heating to actual cookedness");off(c,s,SECOND);equip(c,s,"minecraft:air");c.getInput().holdKey(o->o.keyShift);c.waitTicks(3);click(c,SECOND,Direction.UP);c.getInput().releaseKey(o->o.keyShift);c.waitTicks(3);s.runOnServer(g->check(g.getPlayerList().getPlayers().getFirst().getInventory().contains(x->x.is(Items.COOKED_COD)&&x.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().contains("kitchen_portion")),"Continued cooking after transfer must produce real vanilla cooked fish with its retained record"));
                select(c,x->x.is(KitchenContent.item("mixed_stir_fry"))&&grade(x)==3);s.runOnServer(g->g.getPlayerList().getPlayers().getFirst().getFoodData().setFoodLevel(8));c.runOnClient(client->client.player.setXRot(-70));c.getInput().holdKey(o->o.keyUse);c.waitTicks(40);c.getInput().releaseKey(o->o.keyUse);c.waitTicks(5);s.runOnServer(g->{check(g.getPlayerList().getPlayers().getFirst().getFoodData().getFoodLevel()>8,"The actual good meal must restore hunger");for(int i=0;i<5;i++)gas[i]=ResourceStates.gasUnits(pot(g,new BlockPos[]{PANS[0],PANS[1],PANS[2],FIRST,SECOND}[i]).items.get(26));});
            }
            try(var world=save.open()) {c.waitTicks(10);world.getServer().runOnServer(g->{for(int i=0;i<5;i++){var p=pot(g,new BlockPos[]{PANS[0],PANS[1],PANS[2],FIRST,SECOND}[i]);check(ResourceStates.gasUnits(p.items.get(26))==gas[i],"Fuel must remain exact after a real world reopen");if(i<3)check(p.batch.remainingServings==1,"Remaining meals must survive actual world reopening");}var items=g.getPlayerList().getPlayers().getFirst().getInventory().getNonEquipmentItems();check(items.stream().anyMatch(x->reasons(x).contains("salty"))&&items.stream().anyMatch(x->reasons(x).contains("uneven")),"Stored quality reasons must survive saving");check(items.stream().anyMatch(x->x.is(Items.COOKED_COD)&&x.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getStringOr("kitchen_portion","").contains("turnedAfterSetting")),"The cooked loose-food record must survive saving");});world.getServer().runCommand("tp @a 4 100 -3 0 25");c.waitTicks(5);c.takeScreenshot("quality-after-world-reopen-0.1.7");}
        } finally {KitchenConfig.INSTANCE.burns=burns;KitchenConfig.INSTANCE.detailedHints=hints;}
    }
    private static final class Comparison extends Screen {
        private final List<ItemStack> meals;
        Comparison(List<ItemStack> meals){super(Component.literal("Actual cooking quality"));this.meals=meals;}
        @Override public void extractRenderState(GuiGraphicsExtractor g,int x,int y,float delta){
            g.fill(0,0,width,height,0xff263039);g.centeredText(font,"0.1.7 | actual cooked dishes / normal item tooltips",width/2,8,0xffffffff);int pitch=(width-12)/3;
            for(int i=0;i<meals.size();i++){int left=6+i*pitch;g.fill(left,30,left+pitch-5,height-12,0xff35434f);g.item(meals.get(i),left+8,40);int top=65;for(var line:meals.get(i).getTooltipLines(Item.TooltipContext.of(minecraft.level),minecraft.player,TooltipFlag.NORMAL))for(var wrapped:font.split(line,pitch-16)){g.text(font,wrapped,left+5,top,0xffeee8dd,false);top+=11;}}
        }
    }
}
