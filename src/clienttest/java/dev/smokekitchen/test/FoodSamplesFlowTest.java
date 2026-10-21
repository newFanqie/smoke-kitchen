package dev.smokekitchen.test;

import dev.smokekitchen.content.*;
import dev.smokekitchen.entity.KitchenEntities;
import dev.smokekitchen.world.KitchenBlockEntity;
import dev.smokekitchen.client.KitchenStationRenderer;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.phys.Vec3;

/** Real survival production; the only supplied items are ordinary base recipe materials. */
public final class FoodSamplesFlowTest extends FirstMealFlowTest {
    private static final BlockPos SOURCE=new BlockPos(2,100,0),TABLE=new BlockPos(0,100,0),FURNACE=new BlockPos(-2,100,-1),SOUP=new BlockPos(4,100,-2),DUCK=new BlockPos(5,100,0);
    @Override protected String[] extraRawMaterials(){return new String[]{"minecraft:oak_log 2","minecraft:stone 2","minecraft:coal 1"};}
    private static void check(boolean b,String s){if(!b)throw new AssertionError(s);}
    private static KitchenBlockEntity be(net.minecraft.server.MinecraftServer s,BlockPos p){return (KitchenBlockEntity)s.overworld().getBlockEntity(p);}
    private static void near(SurvivalPlayer p,BlockPos pos){p.s.runCommand("tp @a "+(pos.getX()+.5)+" 100 "+(pos.getZ()+2.5)+" 180 35");p.c.waitTicks(5);}
    private static void tap(SurvivalPlayer p,BlockPos pos,Direction face){near(p,pos);p.click(pos,face);}
    private static void put(SurvivalPlayer p,String id,BlockPos pos){p.select(id);tap(p,pos,Direction.UP);}
    private static void take(SurvivalPlayer p,BlockPos pos){near(p,pos);p.emptyHand();p.c.getInput().holdKey(o->o.keyShift);p.c.waitTicks(3);p.click(pos,Direction.UP);p.c.getInput().releaseKey(o->o.keyShift);p.c.waitTicks(3);}
    private static void photo(SurvivalPlayer p,String name){p.s.runCommand("time set day");p.s.runCommand("tp @a 6.5 100 3.5");p.c.waitTicks(10);p.look(new Vec3(4.3,100.4,-.2));p.c.takeScreenshot("food-samples-"+name+"-0.1.17");}
    private static void owned(SurvivalPlayer p,String name){p.c.runOnClient(client->{client.getToastManager().clear();client.setScreen(new ProducedFoods());});p.c.waitTicks(4);p.c.takeScreenshot("food-samples-"+name+"-0.1.17");p.closeMenu();}
    private static void waitOutput(SurvivalPlayer p,String id,int count){for(int n=0;n<1100;n+=20){boolean[] done={false};p.s.runOnServer(s->{var f=(net.minecraft.world.level.block.entity.FurnaceBlockEntity)s.overworld().getBlockEntity(FURNACE);done[0]=f.getItem(2).is(SurvivalPlayer.item(id))&&f.getItem(2).getCount()==count;});if(done[0])return;p.c.waitTicks(20);}throw new AssertionError("Actual furnace output missing: "+id);}
    private static void upright(SurvivalPlayer p,BlockPos pos,String id){p.c.runOnClient(client->{var e=(KitchenBlockEntity)client.level.getBlockEntity(pos);var r=(KitchenStationRenderer)(Object)client.getBlockEntityRenderDispatcher().getRenderer(e);var st=r.createRenderState();r.extractRenderState(e,st,.5f,client.player.getEyePosition(),null);check(st.pieces.size()==1&&!st.pieces.getFirst().flat()&&st.pieces.getFirst().y()==1&&st.pieces.getFirst().appearance().equals("upright_food"),"Actual "+id+" must sit upright at the counter surface, not use a flat sprite pose");});}
    @Override protected boolean continueWithTransfer(SurvivalPlayer p){
        p.emptyHand();tap(p,SOURCE,Direction.UP);for(int i=0;i<2;i++){p.emptyHand();tap(p,SOURCE,Direction.NORTH);}p.s.runOnServer(s->check(be(s,SOURCE).heat==0,"Actual stove is shut off"));
        put(p,"minecraft:bowl",SOURCE);check(p.count("smoke_kitchen:mixed_stew")==1,"Actual first meal served for art review");
        p.s.runCommand("summon smoke_kitchen:duck 0.5 100 2.5 {NoAI:1b}");p.c.waitTicks(6);kill(p,KitchenEntities.DUCK,new Vec3(.5,100,2.5));check(p.count("smoke_kitchen:duck_meat")==1,"The extra real duck must drop a whole raw portion");owned(p,"actual-raw-duck-and-served-soup");
        p.emptyHand();tap(p,FURNACE,Direction.UP);p.putInMenu("smoke_kitchen:duck_meat",0,1);p.putInMenu("coal",1,1);waitOutput(p,"smoke_kitchen:cooked_duck",1);p.slot(2,0,ContainerInput.QUICK_MOVE);p.putInMenu("stone",0,2);waitOutput(p,"smooth_stone",2);p.slot(2,0,ContainerInput.QUICK_MOVE);p.closeMenu();check(p.count("smoke_kitchen:cooked_duck")==1&&p.count("smoke_kitchen:duck_meat")==0,"Real furnace cooking transforms precisely the actual duck drop");
        p.emptyHand();tap(p,TABLE,Direction.UP);while(p.count("oak_planks")<4)p.craft("oak_planks","oak_log");for(int i=0;i<2;i++)p.craft("smoke_kitchen:counter","oak_planks","oak_planks","smooth_stone");p.closeMenu();put(p,"smoke_kitchen:counter",SOUP.below());put(p,"smoke_kitchen:counter",DUCK.below());
        owned(p,"actual-cooked-duck-and-served-soup");put(p,"smoke_kitchen:mixed_stew",SOUP);put(p,"smoke_kitchen:cooked_duck",DUCK);p.c.waitTicks(12);upright(p,SOUP,"soup");upright(p,DUCK,"duck");photo(p,"actual-counter-contact");return true;
    }
    private static void eat(SurvivalPlayer p,String id){p.s.runOnServer(s->s.getPlayerList().getPlayers().getFirst().getFoodData().setFoodLevel(8));p.select(id);p.c.runOnClient(client->client.player.setXRot(-70));p.c.getInput().holdKey(o->o.keyUse);p.c.waitTicks(40);p.c.getInput().releaseKey(o->o.keyUse);p.c.waitTicks(5);check(p.count(id)==0,"Actual use must consume "+id);p.s.runOnServer(s->check(s.getPlayerList().getPlayers().getFirst().getFoodData().getFoodLevel()>8,"Actual eating must restore hunger"));}
    @Override public void runTest(ClientGameTestContext c){
        super.runTest(c);
        try(var world=savedWorld.open()){
            var p=new SurvivalPlayer(c,world.getServer());p.s.runOnServer(s->check(be(s,SOUP).getItem(0).is(KitchenContent.item("mixed_stew"))&&be(s,DUCK).getItem(0).is(KitchenContent.item("cooked_duck")),"Actual stored products survive real world reopening"));c.waitTicks(10);upright(p,SOUP,"reopened soup");upright(p,DUCK,"reopened duck");photo(p,"after-real-world-reopen");take(p,SOUP);take(p,DUCK);check(p.count("smoke_kitchen:mixed_stew")==1&&p.count("smoke_kitchen:cooked_duck")==1,"Physical retrieval returns the two actual products");eat(p,"smoke_kitchen:cooked_duck");eat(p,"smoke_kitchen:mixed_stew");check(p.count("bowl")==4,"Eating original bowl meal returns its original wooden bowl");put(p,"minecraft:bowl",SOURCE);check(p.count("smoke_kitchen:mixed_stew")==1,"Last real soup serving can still be served");p.select("smoke_kitchen:mixed_stew");photo(p,"actual-eaten-and-last-serving");
        }
        try(var world=savedWorld.open()){
            var p=new SurvivalPlayer(c,world.getServer());check(p.count("smoke_kitchen:mixed_stew")==1&&p.count("smoke_kitchen:cooked_duck")==0,"Final actual inventory retains only the uneaten soup serving");p.s.runOnServer(s->check(be(s,SOURCE).batch.ingredients.isEmpty()&&be(s,SOUP).isEmpty()&&be(s,DUCK).isEmpty(),"Both counters and original cooking batch are actually emptied"));owned(p,"last-actual-serving-after-reopen");
        }
    }
    private static final class ProducedFoods extends net.minecraft.client.gui.screens.Screen {
        ProducedFoods(){super(net.minecraft.network.chat.Component.literal("Actual produced foods"));}
        @Override public void extractRenderState(net.minecraft.client.gui.GuiGraphicsExtractor g,int x,int y,float delta){g.fill(0,0,width,height,0xff263038);g.centeredText(font,"0.1.17 / 实际制作的食物（大图与原始物品栏尺寸）",width/2,10,0xffffffff);int n=0;for(var item:minecraft.player.getInventory().getNonEquipmentItems())if(item.is(KitchenContent.item("duck_meat"))||item.is(KitchenContent.item("cooked_duck"))||item.is(KitchenContent.item("mixed_stew"))){int left=14+n++*(width/2);g.pose().pushMatrix();g.pose().translate(left+10,36).scale(5);g.item(item,0,0);g.pose().popMatrix();g.item(item,left+110,70);g.text(font,item.getHoverName(),left+8,128,0xfff4ead8,false);int line=148;for(var text:item.getTooltipLines(Item.TooltipContext.of(minecraft.level),minecraft.player,TooltipFlag.NORMAL))for(var row:font.split(text,width/2-20)){g.text(font,row,left+8,line,0xffded5c2,false);line+=11;}}}
    }
}
