package dev.smokekitchen.test;

import dev.smokekitchen.content.*;
import dev.smokekitchen.world.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.core.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.*;

/** Actual tools, irrigation, rice growth/harvest, exact used seasoning and targeted rack cells. */
public class FarmRackFlowTest extends FirstMealFlowTest {
    private static final BlockPos RACK=new BlockPos(4,100,-2),WET=new BlockPos(0,99,-4),DRY=new BlockPos(6,99,-4),TABLE=new BlockPos(0,100,0),FURNACE=new BlockPos(-2,100,-1);
    private static void check(boolean b,String text){if(!b)throw new AssertionError(text);}
    @Override protected String[] extraRawMaterials(){return new String[]{"minecraft:oak_log 4","minecraft:sand 4","minecraft:coal 1","minecraft:dirt 2","minecraft:bone_meal 16","smoke_kitchen:rice_paddy_seeds 2"};}
    private static void near(SurvivalPlayer p,BlockPos pos){p.s.runCommand("tp @a "+(pos.getX()+.5)+" 100 "+(pos.getZ()+2.5)+" 180 35");p.c.waitTicks(5);}
    private static void tap(SurvivalPlayer p,BlockPos pos){near(p,pos);p.click(pos,Direction.UP);}
    private static void mineGround(SurvivalPlayer p,BlockPos pos){
        if(p.c.computeOnClient(client->client.level.isEmptyBlock(pos)))return;
        near(p,pos);p.select("stone_pickaxe");p.look(Vec3.atCenterOf(pos));p.c.runOnClient(client->client.gameMode.startDestroyBlock(pos,Direction.UP));
        for(int i=0;i<160;i++){if(p.c.computeOnClient(client->client.level.isEmptyBlock(pos)))break;p.c.runOnClient(client->client.gameMode.continueDestroyBlock(pos,Direction.UP));p.c.waitTicks(1);}p.c.runOnClient(client->client.gameMode.stopDestroyBlock());check(p.c.computeOnClient(client->client.level.isEmptyBlock(pos)),"Actually excavate the rice plot, without trying to pathfind below the floor to collect unneeded stone");
    }
    private static void bucket(SurvivalPlayer p,BlockPos soil,String held){near(p,soil);p.select(held);if(p.c.computeOnClient(client->client.level.getBlockState(soil).getBlock() instanceof RiceSoilBlock)){p.click(soil.above(),Direction.UP);}else{p.look(new Vec3(soil.getX()+.5,soil.getY()+.9,soil.getZ()+.5));p.c.runOnClient(client->client.gameMode.useItem(client.player,InteractionHand.MAIN_HAND));}p.c.waitTicks(8);}
    private static void cell(SurvivalPlayer p,int slot){
        var face=p.c.computeOnClient(client->client.level.getBlockState(RACK).getValue(KitchenBlock.FACING));p.s.runCommand("tp @a "+(RACK.getX()+.5+face.getStepX()*2.5)+" 100 "+(RACK.getZ()+.5+face.getStepZ()*2.5));p.c.waitTicks(5);
        var xz=SpiceRack.position(slot,face);double x=RACK.getX()+xz[0],z=RACK.getZ()+xz[1];if(face.getAxis()==Direction.Axis.X)x=RACK.getX()+(face==Direction.EAST?.99:.01);else z=RACK.getZ()+(face==Direction.SOUTH?.875:.125);
        var at=new Vec3(x,RACK.getY()+(slot<3?.3:.7),z);p.look(at);p.c.runOnClient(client->client.gameMode.useItemOn(client.player,InteractionHand.MAIN_HAND,new BlockHitResult(at,face,RACK,false)));p.c.waitTicks(6);
    }
    private static KitchenBlockEntity rack(net.minecraft.server.MinecraftServer s){return (KitchenBlockEntity)s.overworld().getBlockEntity(RACK);}
    private static void photo(SurvivalPlayer p,String name,BlockPos target){p.s.runCommand("time set day");p.s.runCommand("tp @a "+(target.getX()+2.5)+" 100 "+(target.getZ()+3.5));p.c.waitTicks(8);p.look(Vec3.atCenterOf(target));p.c.runOnClient(client->client.getToastManager().clear());p.c.takeScreenshot("farm-rack-"+name+"-0.1.18");}
    @Override protected boolean continueWithTransfer(SurvivalPlayer p){
        p.s.runCommand("fill -6 98 -6 10 98 6 minecraft:smooth_stone");p.s.runCommand("fill 8 99 -5 10 99 6 minecraft:smooth_stone");
        var source=new BlockPos(2,100,0);p.emptyHand();tap(p,source);for(int i=0;i<2;i++){p.emptyHand();near(p,source);p.click(source,Direction.NORTH);}p.select("bowl");tap(p,source);check(p.count("smoke_kitchen:mixed_stew")==1,"Serve the actual first meal before farming");p.s.runOnServer(s->s.getPlayerList().getPlayers().getFirst().getFoodData().setFoodLevel(8));p.select("smoke_kitchen:mixed_stew");p.c.runOnClient(client->client.player.setXRot(-70));p.c.getInput().holdKey(o->o.keyUse);p.c.waitTicks(40);p.c.getInput().releaseKey(o->o.keyUse);p.c.waitTicks(5);check(p.count("smoke_kitchen:mixed_stew")==0&&p.count("bowl")==4,"Actually eat the meal and return the original bowl");
        p.emptyHand();tap(p,FURNACE);p.putInMenu("sand",0,4);p.putInMenu("coal",1,1);for(int ticks=0;ticks<1050;ticks+=20){if(p.c.computeOnClient(client->client.player.containerMenu.getSlot(2).getItem().getCount()==4))break;p.c.waitTicks(20);}check(p.c.computeOnClient(client->client.player.containerMenu.getSlot(2).getItem().is(Items.GLASS)&&client.player.containerMenu.getSlot(2).getItem().getCount()==4),"Actual sand smelts into four additional glass");p.slot(2,0,ContainerInput.QUICK_MOVE);p.closeMenu();
        farmWork(p);return true;
    }
    protected void farmWork(SurvivalPlayer p){
        boolean[] exists={false};p.s.runOnServer(s->exists[0]=s.overworld().getBlockState(RACK).is(KitchenContent.BLOCKS.get("spice_rack")));
        if(!exists[0]){
        p.emptyHand();tap(p,TABLE);while(p.count("oak_planks")<12)p.craft("oak_planks","oak_log");while(p.count("stick")<6)p.craft("stick","oak_planks","","","oak_planks","","");if(p.count("stone_hoe")==0)p.craft("stone_hoe","cobblestone","cobblestone","","","stick","","","stick","");
        p.craft("smoke_kitchen:spice_rack","oak_planks","oak_planks","oak_planks","stick","glass","stick","","stick","");p.craft("smoke_kitchen:empty_jar","glass","glass","glass","oak_planks");p.closeMenu();
        p.select("smoke_kitchen:spice_rack");tap(p,RACK.below());
        }
        boolean[] filled={false};p.s.runOnServer(s->filled[0]=rack(s).items.stream().filter(x->!x.isEmpty()).count()==6);
        if(!filled[0]){for(int i:new int[]{0,1,3,5}){p.select("smoke_kitchen:empty_jar");cell(p,i);}p.select("smoke_kitchen:salt");cell(p,4);p.select("smoke_kitchen:lard");cell(p,2);}
        p.s.runOnServer(s->check(rack(s).items.get(4).getDamageValue()==13&&rack(s).items.get(2).getDamageValue()==5&&rack(s).items.stream().filter(x->!x.isEmpty()).count()==6,"Six actual bottles include3-dose salt and3-dose lard without consumption"));
        int raw=p.count("porkchop");p.select("porkchop");cell(p,0);check(p.count("porkchop")==raw,"Rack rejects raw food without swallowing it");photo(p,"actual-six-bottles",RACK);
        p.s.runCommand("fill 3 99 2 4 99 3 minecraft:water");
        for(var pos:new BlockPos[]{WET,DRY}){
            if(p.c.computeOnClient(client->client.level.getBlockState(pos).getBlock() instanceof RiceSoilBlock))continue;
            mineGround(p,pos);p.select("dirt");tap(p,pos.below());p.select("stone_hoe");tap(p,pos);
            bucket(p,new BlockPos(4,99,2),"bucket");p.select("water_bucket");tap(p,pos);p.s.runOnServer(s->check(s.overworld().getBlockState(pos).getBlock() instanceof RiceSoilBlock&&s.overworld().getBlockState(pos).getValue(RiceSoilBlock.WATERLOGGED),"Real hoe and water bucket form shallow rice soil"));
            p.select("smoke_kitchen:rice_paddy_seeds");tap(p,pos);
        }
        bucket(p,DRY,"bucket");p.s.runOnServer(s->check(!s.overworld().getBlockState(DRY).getValue(RiceSoilBlock.WATERLOGGED),"Real empty bucket drains the retained source"));
        int dryAge=p.c.computeOnClient(client->KitchenContent.CROPS.get("rice_paddy").getAge(client.level.getBlockState(DRY.above())));check(dryAge<7,"Dry-rice negative case must use a plant that could otherwise grow");
        p.select("bone_meal");int count=p.count("bone_meal");for(int i=0;i<2;i++)tap(p,DRY.above());check(p.count("bone_meal")==count,"Dry isolated rice rejects actual bone meal without consumption");
        p.s.runOnServer(s->check(KitchenContent.CROPS.get("rice_paddy").getAge(s.overworld().getBlockState(DRY.above()))==dryAge,"Drained rice retains its prior age and really pauses"));photo(p,"actual-drained-rice",DRY);
        bucket(p,DRY,"water_bucket");p.s.runOnServer(s->check(s.overworld().getBlockState(DRY).getValue(RiceSoilBlock.WATERLOGGED),"Real water bucket refills the same soil without removing rice"));
        for(var pos:new BlockPos[]{WET,DRY}){p.select("bone_meal");for(int i=0;i<7;i++){boolean[] ready={false};p.s.runOnServer(s->ready[0]=KitchenContent.CROPS.get("rice_paddy").isMaxAge(s.overworld().getBlockState(pos.above())));if(ready[0])break;tap(p,pos.above());}}
        photo(p,"actual-mature-rice",WET);harvestAndGuide(p);
    }
    protected void harvestAndGuide(SurvivalPlayer p){
        if(p.c.computeOnClient(client->client.level.getBlockState(WET.above()).is(KitchenContent.CROPS.get("rice_paddy"))&&KitchenContent.CROPS.get("rice_paddy").isMaxAge(client.level.getBlockState(WET.above())))){
            near(p,WET);p.emptyHand();p.look(Vec3.atCenterOf(WET.above()));p.c.runOnClient(client->client.gameMode.startDestroyBlock(WET.above(),Direction.UP));p.c.waitTicks(4);p.c.runOnClient(client->client.gameMode.stopDestroyBlock());
        }
        p.s.runCommand("tp @a 0.5 99.8125 -3.5");p.c.waitTicks(30);
        check(p.count("smoke_kitchen:rice_paddy")>=2&&p.count("smoke_kitchen:rice_paddy_seeds")>=2,"Physical collection of actual grown rice yields grain and replanting seed");
        if(p.c.computeOnClient(client->client.level.isEmptyBlock(WET.above()))){p.select("smoke_kitchen:rice_paddy_seeds");tap(p,WET);}
        p.select("smoke_kitchen:recipe_book");p.c.runOnClient(client->client.gameMode.useItem(client.player,InteractionHand.MAIN_HAND));p.c.waitTicks(5);p.c.takeScreenshot("farm-rack-guide-0.1.18");p.closeMenu();
    }
    @Override public void runTest(ClientGameTestContext c){
        super.runTest(c);
        verifySaved(c);
    }
    protected void verifySaved(ClientGameTestContext c){
        try(var w=savedWorld.open()){
            var p=new SurvivalPlayer(c,w.getServer());p.s.runOnServer(s->{check(s.overworld().getBlockState(WET).getValue(RiceSoilBlock.WATERLOGGED)&&s.overworld().getBlockState(DRY).getValue(RiceSoilBlock.WATERLOGGED),"Both real irrigated plots retain their water after reopening");check(KitchenContent.CROPS.get("rice_paddy").isMaxAge(s.overworld().getBlockState(DRY.above())),"Actual grown rice age survives reopening");check(rack(s).items.get(4).getDamageValue()==13&&rack(s).items.get(2).getDamageValue()==5,"Used seasoning amounts survive actual world reopening");});
            p.emptyHand();cell(p,4);check(p.count("smoke_kitchen:used_salt")==1,"Targeted upper-middle cell returns the actual used salt");p.s.runOnServer(s->check(rack(s).items.get(4).isEmpty(),"Taking the targeted cell actually empties it"));
            p.select("smoke_kitchen:salt");cell(p,4);p.s.runOnServer(s->check(rack(s).items.get(4).getDamageValue()==13,"Repeated take/put does not refill or consume salt"));photo(p,"actual-world-reopen",RACK);
        }
        try(var w=savedWorld.open()){w.getServer().runOnServer(s->check(rack(s).items.get(4).getDamageValue()==13,"Second real reopen preserves returned bottle"));c.takeScreenshot("farm-rack-final-reopen-0.1.18");}
    }
}
