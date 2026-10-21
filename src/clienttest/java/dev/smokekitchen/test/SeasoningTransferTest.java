package dev.smokekitchen.test;

import dev.smokekitchen.content.*;
import dev.smokekitchen.cooking.CookingBatch;
import dev.smokekitchen.world.KitchenBlockEntity;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.Vec3;

/** Real first-meal production followed by salted loose portions, two pots and actual save/reopen. */
public final class SeasoningTransferTest extends FirstMealFlowTest {
    private static final BlockPos SOURCE=new BlockPos(2,100,0),DEST=new BlockPos(4,100,-2),TABLE=new BlockPos(0,100,0);
    @Override protected String[] extraRawMaterials(){return new String[]{"minecraft:clay_ball 6","minecraft:cobblestone 4"};}
    private static void check(boolean b,String s){if(!b)throw new AssertionError(s);}
    private static KitchenBlockEntity be(net.minecraft.server.MinecraftServer s,BlockPos p){return (KitchenBlockEntity)s.overworld().getBlockEntity(p);}
    private static void near(SurvivalPlayer p,BlockPos pos){p.s.runCommand("tp @a "+(pos.getX()+.5)+" 100 "+(pos.getZ()+2.5)+" 180 35");p.c.waitTicks(4);}
    private static void tap(SurvivalPlayer p,BlockPos pos,Direction face){near(p,pos);p.click(pos,face);}
    private static void put(SurvivalPlayer p,String id,BlockPos pos,int n){for(int i=0;i<n;i++){p.select(id.contains(":")?id:"smoke_kitchen:"+id);tap(p,pos,Direction.UP);}}
    private static void takeOne(SurvivalPlayer p,BlockPos pos){p.emptyHand();p.c.getInput().holdKey(o->o.keyShift);p.c.waitTicks(3);tap(p,pos,Direction.UP);p.c.getInput().releaseKey(o->o.keyShift);p.c.waitTicks(3);}
    private static void putLoose(SurvivalPlayer p,BlockPos pos,int n){
        for(int i=0;i<n;i++){
            String id=p.c.computeOnClient(client->{for(var x:client.player.getInventory().getNonEquipmentItems())if(x.getOrDefault(DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.EMPTY).copyTag().contains("kitchen_portion"))return net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(x.getItem()).toString();throw new AssertionError("Actual retrieved loose food is missing");});
            p.select(id,x->x.getOrDefault(DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.EMPTY).copyTag().contains("kitchen_portion"));tap(p,pos,Direction.UP);
        }
    }
    private static void amounts(SurvivalPlayer p,int sourceCount,int destCount){p.s.runOnServer(s->{var a=be(s,SOURCE).batch;var b=be(s,DEST).batch;check(a.ingredients.size()==sourceCount&&b.ingredients.size()==destCount,"Actual portion counts must match both pots");check(Math.abs(a.salt-sourceCount)<1e-8&&Math.abs(b.salt-destCount)<1e-8,"Every transferred portion must retain exactly one of the four salt doses");check(Math.abs(a.oil-sourceCount*.25)<1e-8&&Math.abs(b.oil-destCount*.25)<1e-8,"Oil must remain one quarter dose per portion");if(sourceCount>0)check(a.quality()==1&&a.qualityReasons().contains("salty"),"Source keeps its real salt concentration");if(destCount>0)check(b.quality()==1&&b.qualityReasons().contains("salty"),"A clean pot cannot erase saltiness");});}
    private static void photo(SurvivalPlayer p,String name){p.emptyHand();near(p,DEST);p.look(Vec3.atCenterOf(DEST));p.c.takeScreenshot("transfer-"+name+"-0.1.15");}
    @Override protected boolean continueWithTransfer(SurvivalPlayer p){
        p.emptyHand();tap(p,SOURCE,Direction.UP); // Actually remove the existing wooden lid.
        p.emptyHand();tap(p,SOURCE,Direction.NORTH);tap(p,SOURCE,Direction.NORTH); // Mid heat -> off.
        p.emptyHand();tap(p,TABLE,Direction.UP);p.craft("smoke_kitchen:clay_stove","clay_ball","stick","clay_ball","clay_ball","cobblestone","clay_ball","cobblestone","cobblestone","cobblestone");p.craft("smoke_kitchen:clay_pot","clay_ball","","clay_ball","clay_ball","","clay_ball","","clay_ball","");p.closeMenu();p.select("smoke_kitchen:clay_stove");tap(p,DEST.below(),Direction.UP);put(p,"clay_pot",DEST,1);
        put(p,"salt",SOURCE,3);amounts(p,4,0);takeOne(p,SOURCE);amounts(p,3,0);
        p.s.runOnServer(s->{var inv=s.getPlayerList().getPlayers().getFirst().getInventory();var loose=inv.getNonEquipmentItems().stream().filter(x->x.getOrDefault(DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.EMPTY).copyTag().contains("kitchen_portion")).findFirst().orElseThrow();var tag=loose.get(DataComponents.CUSTOM_DATA).copyTag();var portion=new com.google.gson.Gson().fromJson(tag.getStringOr("kitchen_portion",""),CookingBatch.Portion.class);check(Math.abs(portion.seasonings.get("salt")-1)<1e-8&&Math.abs(portion.seasonings.get("oil")-.25)<1e-8&&tag.getIntOr("kitchen_quality",-1)==1,"First real loose portion carries its share and poor-quality explanation");});
        p.c.runOnClient(client->{var item=client.player.getInventory().getNonEquipmentItems().stream().filter(x->x.getOrDefault(DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.EMPTY).copyTag().contains("kitchen_portion")).findFirst().orElseThrow().copy();client.setScreen(new ActualPortion(item));});p.c.waitTicks(4);p.c.takeScreenshot("transfer-actual-loose-tooltip-0.1.15");p.closeMenu();photo(p,"first-loose-share-before-save");return true;
    }
    private static final class ActualPortion extends net.minecraft.client.gui.screens.Screen {
        private final ItemStack item;ActualPortion(ItemStack item){super(net.minecraft.network.chat.Component.literal("Actual retrieved food"));this.item=item;}
        @Override public void extractRenderState(net.minecraft.client.gui.GuiGraphicsExtractor g,int x,int y,float delta){g.fill(0,0,width,height,0xff263039);g.centeredText(font,"0.1.15 / 实际取出的食材与正常物品提示",width/2,12,0xffffffff);g.item(item,30,45);int top=80;for(var line:item.getTooltipLines(Item.TooltipContext.of(minecraft.level),minecraft.player,TooltipFlag.NORMAL))for(var text:font.split(line,width-60)){g.text(font,text,30,top,0xffeee8dd,false);top+=12;}}
    }
    @Override public void runTest(ClientGameTestContext c){
        super.runTest(c);
        try(var world=savedWorld.open()){
            var p=new SurvivalPlayer(c,world.getServer());amounts(p,3,0);putLoose(p,DEST,1);for(int i=0;i<3;i++){takeOne(p,SOURCE);putLoose(p,DEST,1);}amounts(p,0,4);photo(p,"all-four-in-clean-pot");
        }
        try(var world=savedWorld.open()){
            var p=new SurvivalPlayer(c,world.getServer());amounts(p,0,4);for(int i=0;i<2;i++){takeOne(p,DEST);putLoose(p,SOURCE,1);}amounts(p,2,2);photo(p,"two-pots-half-each");
        }
        try(var world=savedWorld.open()){
            var p=new SurvivalPlayer(c,world.getServer());amounts(p,2,2);for(int i=0;i<2;i++){takeOne(p,DEST);putLoose(p,SOURCE,1);}amounts(p,4,0);put(p,"minecraft:bowl",SOURCE,1);
            p.s.runOnServer(s->{var meal=s.getPlayerList().getPlayers().getFirst().getInventory().getNonEquipmentItems().stream().filter(x->x.is(KitchenContent.item("mixed_stew"))).findFirst().orElseThrow();var tag=meal.get(DataComponents.CUSTOM_DATA).copyTag();check(tag.getIntOr("kitchen_quality",-1)==1&&tag.getStringOr("kitchen_quality_reasons","").contains("salty"),"Serving after round trips cannot erase oversalting");int base=ContentCatalog.num(KitchenContent.DEFINITIONS.get("mixed_stew"),"nutrition",1);check(meal.get(DataComponents.FOOD).nutrition()==Math.max(1,(int)Math.floor(base*.6)),"Actual food value retains its penalty");s.getPlayerList().getPlayers().getFirst().getFoodData().setFoodLevel(8);});
            p.select("smoke_kitchen:mixed_stew");c.runOnClient(client->client.player.setXRot(-70));c.getInput().holdKey(o->o.keyUse);c.waitTicks(40);c.getInput().releaseKey(o->o.keyUse);c.waitTicks(5);check(p.count("smoke_kitchen:mixed_stew")==0&&p.count("bowl")==4,"Actual salty meal must be eaten and return its bowl");p.s.runOnServer(s->check(s.getPlayerList().getPlayers().getFirst().getFoodData().getFoodLevel()>8&&be(s,SOURCE).batch.remainingServings==1,"One saved serving remains after real eating"));photo(p,"after-eating-salty-meal");
        }
        try(var world=savedWorld.open()){
            var p=new SurvivalPlayer(c,world.getServer());put(p,"minecraft:bowl",SOURCE,1);check(p.count("smoke_kitchen:mixed_stew")==1,"Actually reopen and serve last portion");p.s.runOnServer(s->check(be(s,SOURCE).batch.ingredients.isEmpty()&&be(s,DEST).batch.ingredients.isEmpty(),"Two servings consume exactly the original four food portions"));photo(p,"last-serving-after-reopen");
        }
    }
}
