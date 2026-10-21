package dev.smokekitchen.test;

import dev.smokekitchen.content.*;
import dev.smokekitchen.world.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.minecraft.core.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.Vec3;
import java.util.function.Predicate;

/** Continues an actually crafted and fermented kitchen. Extras are only raw beans, planks, stick and coal. */
public class TofuFlowTest extends FermentationFlowTest {
    protected boolean reserveTofuForDish(){return false;}
    private static final BlockPos CROCK=new BlockPos(2,100,2),MILL=new BlockPos(-2,100,0),MIX=new BlockPos(0,100,2),POT=new BlockPos(-2,100,2);
    private static void check(boolean b,String s){if(!b)throw new AssertionError(s);}
    private static KitchenBlockEntity be(net.minecraft.server.MinecraftServer s,BlockPos p){return (KitchenBlockEntity)s.overworld().getBlockEntity(p);}
    private static int stored(KitchenBlockEntity e,String id){return e.items.stream().filter(x->x.is(KitchenContent.item(id))).mapToInt(ItemStack::getCount).sum();}
    private static void tap(SurvivalPlayer p,BlockPos pos,Direction face){p.s.runCommand("tp @a "+(pos.getX()+.5)+" 100 "+(pos.getZ()+2.5)+" 180 35");p.c.waitTicks(4);p.click(pos,face);}
    private static void put(SurvivalPlayer p,String id,BlockPos pos,int n){for(int i=0;i<n;i++){p.select(id.contains(":")?id:"smoke_kitchen:"+id);tap(p,pos,Direction.UP);}}
    private static void take(SurvivalPlayer p,BlockPos pos){p.c.getInput().holdKey(o->o.keyShift);p.c.waitTicks(3);for(int n=0;n<26;n++){boolean[] empty={false};p.s.runOnServer(s->empty[0]=be(s,pos).items.stream().allMatch(ItemStack::isEmpty));if(empty[0])break;p.emptyHand();tap(p,pos,Direction.UP);}p.c.getInput().releaseKey(o->o.keyShift);p.c.waitTicks(3);}
    private static void until(SurvivalPlayer p,Predicate<net.minecraft.server.MinecraftServer> condition,int ticks,String phase){for(int i=0;i<ticks;i+=20){boolean[] done={false};p.s.runOnServer(s->{done[0]=condition.test(s);if(!done[0]){var e=be(s,phase.equals("浸泡")?CROCK:POT);int percent=phase.equals("浸泡")?e.work*100/120:e.batch.progress(1);s.getPlayerList().getPlayers().getFirst().sendOverlayMessage(net.minecraft.network.chat.Component.literal("自动验收："+phase+" "+Math.min(100,percent)+"% · 等待中，请勿操作"));}});if(done[0])return;p.c.waitTicks(20);}throw new AssertionError(phase+" did not finish through ordinary ticks");}
    private static void photo(SurvivalPlayer p,String name){p.emptyHand();p.s.runCommand("tp @a -2.5 101 4.5 180 48");p.c.waitTicks(6);p.look(Vec3.atCenterOf(POT));p.c.takeScreenshot("tofu-"+name+"-0.1.13");}
    private static void shut(SurvivalPlayer p){p.emptyHand();tap(p,POT,Direction.NORTH);tap(p,POT,Direction.NORTH);}
    @Override protected String[] extraRawMaterials(){return new String[]{"smoke_kitchen:soybean 4","minecraft:oak_planks 2","minecraft:stick 1","minecraft:coal 6"};}
    @Override public void runTest(ClientGameTestContext c){
        super.runTest(c);int[] savedSoak={0};double[] savedCurd={0};
        try(var world=savedWorld.open()){
            var p=new SurvivalPlayer(c,world.getServer());p.emptyHand();tap(p,new BlockPos(0,100,-2),Direction.UP);p.craft("smoke_kitchen:wooden_lid","oak_planks","oak_planks","","","stick","");p.closeMenu();boolean[] covered={false};p.s.runOnServer(s->covered[0]=be(s,POT).getBlockState().getValue(KitchenBlock.LID));if(covered[0])put(p,"wooden_lid",POT,1);put(p,"soybean",CROCK,4);put(p,"water_jug",CROCK,1);p.emptyHand();tap(p,CROCK,Direction.UP);c.waitTicks(600);
            p.s.runOnServer(s->{savedSoak[0]=be(s,CROCK).work;check(savedSoak[0]>0&&savedSoak[0]<120&&stored(be(s,CROCK),"soaked_soybean")==0,"Actual soaking must be partially complete before saving");});
        }
        try(var world=savedWorld.open()){
            var p=new SurvivalPlayer(c,world.getServer());p.s.runOnServer(s->check(be(s,CROCK).work>=savedSoak[0]&&stored(be(s,CROCK),"soybean")==4,"Reopening preserves incomplete soaking and all four beans"));until(p,s->stored(be(s,CROCK),"soaked_soybean")==4,2600,"浸泡");p.emptyHand();tap(p,CROCK,Direction.UP);take(p,CROCK);check(p.count("smoke_kitchen:soaked_soybean")==4,"Actually retrieve four soaked beans");
            put(p,"soaked_soybean",MILL,4);put(p,"water_jug",MILL,1);p.emptyHand();tap(p,MILL,Direction.UP);tap(p,MILL,Direction.UP);take(p,MILL);check(p.count("smoke_kitchen:raw_bean_pulp")==1,"Wet grinding must create exactly one pulp batch");
            put(p,"raw_bean_pulp",POT,1);put(p,"water_jug",POT,2);put(p,"minecraft:coal",POT,3);photo(p,"raw-pulp-in-pot");until(p,s->be(s,POT).batch.ready(),3000,"煮浆");photo(p,"boiled-pulp");shut(p);take(p,POT);check(p.count("smoke_kitchen:boiled_bean_pulp")==1,"Actual heated pulp must be retrieved as boiled pulp");
            put(p,"boiled_bean_pulp",MIX,1);put(p,"empty_jar",MIX,1);put(p,"sieve",MIX,1);take(p,MIX);check(p.count("smoke_kitchen:soy_milk")==1&&p.count("smoke_kitchen:okara")==1&&p.count("smoke_kitchen:empty_jar")==2,"Filtering fills one of three existing jars and leaves one okara batch");
            put(p,"soy_milk",POT,1);check(p.count("smoke_kitchen:empty_jar")==3,"Pouring back returns the original carrier jar");put(p,"minecraft:coal",POT,1);c.waitTicks(100);p.s.runOnServer(s->check(be(s,POT).batch.progress(1)==0,"Heat without vinegar cannot set milk"));photo(p,"milk-before-acid");put(p,"rice_vinegar",POT,1);until(p,s->be(s,POT).batch.progress(1)>=30,700,"点浆");p.s.runOnServer(s->{savedCurd[0]=be(s,POT).batch.ingredients.getFirst().cooked;check(savedCurd[0]<1&&be(s,POT).batch.vinegar==1,"Save during actual partial coagulation, without editing progress");});photo(p,"partial-curds");
        }
        try(var world=savedWorld.open()){
            var p=new SurvivalPlayer(c,world.getServer());p.s.runOnServer(s->check(be(s,POT).batch.ingredients.getFirst().cooked>=savedCurd[0]&&be(s,POT).batch.vinegar==1,"Actual world reopen retains partial coagulation and acid"));until(p,s->be(s,POT).batch.ready(),700,"点浆");photo(p,"ready-curds");shut(p);take(p,POT);check(p.count("smoke_kitchen:soy_curds")==1,"Actual warm acid treatment produces curds rather than finished tofu");put(p,"soy_curds",MIX,1);put(p,"wooden_lid",MIX,3);p.s.runOnServer(s->check(be(s,MIX).work==15&&stored(be(s,MIX),"tofu")==0,"Three physical lid actions are incomplete"));
        }
        try(var world=savedWorld.open()){
            var p=new SurvivalPlayer(c,world.getServer());p.s.runOnServer(s->check(be(s,MIX).work==15&&stored(be(s,MIX),"soy_curds")==1,"Actual reopen retains partial pressing"));put(p,"wooden_lid",MIX,1);take(p,MIX);check(p.count("smoke_kitchen:tofu")==2,"Fourth real press action yields exactly two tofu");c.runOnClient(client->client.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(client.player)));c.waitTicks(4);c.takeScreenshot("tofu-actual-finished-inventory-0.1.13");p.closeMenu();
            if(!reserveTofuForDish()){
            p.s.runOnServer(s->s.getPlayerList().getPlayers().getFirst().getFoodData().setFoodLevel(8));p.select("smoke_kitchen:tofu");c.runOnClient(client->client.player.setXRot(-70));c.getInput().holdKey(o->o.keyUse);c.waitTicks(35);c.getInput().releaseKey(o->o.keyUse);c.waitTicks(4);check(p.count("smoke_kitchen:tofu")==1,"One actually produced tofu must be eaten");p.s.runOnServer(s->{var inv=s.getPlayerList().getPlayers().getFirst().getInventory();check(s.getPlayerList().getPlayers().getFirst().getFoodData().getFoodLevel()>8,"Produced tofu restores actual hunger");check(inv.countItem(KitchenContent.item("empty_jar"))==3,"The full tofu chain returns exactly the one carrier jar");var vinegar=inv.getNonEquipmentItems().stream().filter(x->ResourceStates.baseId(x).equals("rice_vinegar")).findFirst().orElseThrow();check(vinegar.getMaxDamage()-vinegar.getDamageValue()==6,"Exactly one additional vinegar measure is used for tofu");});
            }
        }
        try(var world=savedWorld.open()){
            var p=new SurvivalPlayer(c,world.getServer());check(p.count("smoke_kitchen:tofu")== (reserveTofuForDish()?2:1)&&p.count("smoke_kitchen:okara")==1,"Finished tofu and okara survive final world reopen");c.takeScreenshot("tofu-after-reopen-0.1.13");
        }
    }
}
