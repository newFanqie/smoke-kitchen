package dev.smokekitchen.test;

import dev.smokekitchen.content.KitchenContent;
import dev.smokekitchen.world.*;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.*;

/** Raw materials only for the real garden; separately labelled old-save compatibility fixture. */
public final class TrellisFlowTest implements FabricClientGameTest {
    private static final BlockPos TABLE=new BlockPos(0,100,2),WET=new BlockPos(3,100,0),DRY=new BlockPos(9,100,0),BUSH=new BlockPos(-3,100,0),LEGACY=new BlockPos(10,100,3);
    private static BlockPos plot(BlockPos frame){return frame.north();}
    private static void check(boolean b,String s){if(!b)throw new AssertionError(s);}
    private static void near(SurvivalPlayer p,BlockPos pos){p.s.runCommand("tp @a "+(pos.getX()+.5)+" 100 "+(pos.getZ()-2.5));p.c.waitTicks(5);}
    private static void tap(SurvivalPlayer p,BlockPos pos){near(p,pos);p.click(pos,Direction.UP);}
    private static void frame(SurvivalPlayer p,BlockPos at){near(p,at);var hit=new Vec3(at.getX()+.125,at.getY()+.65,at.getZ()+.01);p.look(hit);p.c.runOnClient(m->m.gameMode.useItemOn(m.player,InteractionHand.MAIN_HAND,new BlockHitResult(hit,Direction.NORTH,at,false)));p.c.waitTicks(6);}
    private static boolean observe(SurvivalPlayer p,java.util.function.Predicate<net.minecraft.server.MinecraftServer> test){boolean[] v={false};p.s.runOnServer(g->v[0]=test.test(g));return v[0];}
    private static void photo(SurvivalPlayer p,String name,BlockPos at){p.emptyHand();p.s.runCommand("time set noon");p.s.runCommand("tp @a "+(at.getX()+2.6)+" 100 "+(at.getZ()-3.5));p.c.waitTicks(25);p.look(new Vec3(at.getX()+.5,at.getY()+.45,at.getZ()-.2));p.c.runOnClient(m->{m.options.fov().set(45);m.getToastManager().clear();});p.c.waitTicks(5);p.c.takeScreenshot("trellis-"+name+"-0.1.20");p.c.runOnClient(m->m.options.fov().set(70));}
    private static void hydrate(SurvivalPlayer p,BlockPos soil){
        long[] start={0};p.s.runOnServer(g->start[0]=g.overworld().getGameTime());
        p.c.runOnClient(m->{FermentationFlowTest.fastClock=true;m.options.enableVsync().set(false);m.options.framerateLimit().set(260);m.options.pauseOnLostFocus=false;});p.s.runCommand("tick rate 200");
        try{for(int n=0;n<1400;n++){
            if(observe(p,g->g.overworld().getBlockState(soil).is(Blocks.FARMLAND)&&g.overworld().getBlockState(soil).getValue(FarmlandBlock.MOISTURE)>0))return;
            boolean[] timeout={false};p.s.runOnServer(g->{long elapsed=g.overworld().getGameTime()-start[0];timeout[0]=elapsed>18000;g.getPlayerList().getPlayers().forEach(x->x.sendOverlayMessage(Component.literal("自动验收：原版耕地吸水 · "+Math.min(100,elapsed*100/18000)+"% 等待上限；不改湿度或成熟度")));});check(!timeout[0],"Vanilla farmland must actually hydrate from nearby placed/source water");p.c.waitTicks(5);
        }throw new AssertionError("Hydration clock stalled");}finally{p.s.runCommand("tick rate 20");p.c.runOnClient(m->FermentationFlowTest.fastClock=false);}
    }
    private static void grow(SurvivalPlayer p,BlockPos at,String crop){p.select("bone_meal");for(int i=0;i<8&&!observe(p,g->KitchenContent.CROPS.get(crop).isMaxAge(g.overworld().getBlockState(at)));i++)tap(p,at);check(observe(p,g->KitchenContent.CROPS.get(crop).isMaxAge(g.overworld().getBlockState(at))),"Real bone meal must grow "+crop);}
    private static void mine(SurvivalPlayer p,BlockPos at){near(p,at);p.select("stone_axe");p.look(Vec3.atCenterOf(at));p.c.runOnClient(m->m.gameMode.startDestroyBlock(at,Direction.NORTH));for(int i=0;i<180&&!p.c.computeOnClient(m->m.level.isEmptyBlock(at));i++){p.c.runOnClient(m->m.gameMode.continueDestroyBlock(at,Direction.NORTH));p.c.waitTicks(1);}p.c.runOnClient(m->m.gameMode.stopDestroyBlock());check(p.c.computeOnClient(m->m.level.isEmptyBlock(at)),"Actual axe must break the frame");p.s.runCommand("tp @a "+(at.getX()+.5)+" 100 "+(at.getZ()+.5));p.c.waitTicks(25);}
    @Override public void runTest(ClientGameTestContext c){
        TestWorldSave save;
        try(var w=c.worldBuilder().create()){
            save=w.getWorldSave();var s=w.getServer();var p=new SurvivalPlayer(c,s);s.runCommand("gamemode survival @a");s.runCommand("difficulty peaceful");s.runCommand("fill -8 98 -6 14 99 7 minecraft:smooth_stone");s.runCommand("fill 0 99 -4 1 99 -3 minecraft:water");s.runCommand("setblock 8 99 -1 minecraft:air");for(var at:new BlockPos[]{WET,DRY,BUSH})s.runCommand("setblock "+at.getX()+" 99 "+(at.getZ()-1)+" minecraft:dirt");s.runCommand("time set noon");s.runCommand("weather clear");
            for(String raw:new String[]{"oak_log 8","string 4","cobblestone 3","iron_ingot 3","book","wheat_seeds 2","bone_meal 32","smoke_kitchen:black_pepper_seeds 8","smoke_kitchen:sichuan_pepper_seeds 2"})s.runCommand("give @a "+(raw.startsWith("smoke_kitchen:")?raw:"minecraft:"+raw));near(p,TABLE);
            if(net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("iris"))c.runOnClient(m->{try{var a=Class.forName("net.irisshaders.iris.api.v0.IrisApi");check((Boolean)a.getMethod("isShaderPackInUse").invoke(a.getMethod("getInstance").invoke(null)),"Iris must actually use a shader");}catch(ReflectiveOperationException e){throw new AssertionError(e);}});
            for(int i=0;i<8;i++)p.craft("oak_planks","oak_log");p.craft("crafting_table","oak_planks","oak_planks","oak_planks","oak_planks");p.closeMenu();p.select("crafting_table");tap(p,TABLE.below());p.emptyHand();tap(p,TABLE);
            for(int i=0;i<4;i++)p.craft("stick","oak_planks","","","oak_planks","","");
            p.craft("wooden_hoe","oak_planks","oak_planks","","","stick","","","stick","");p.craft("stone_axe","cobblestone","cobblestone","","cobblestone","stick","","","stick","");p.craft("bucket","iron_ingot","","iron_ingot","","iron_ingot");p.craft("smoke_kitchen:recipe_book","book","wheat_seeds");
            for(int i=0;i<2;i++){p.nextCraftScreenshot="trellis-actual-recipe-"+i+"-0.1.20";p.craft("smoke_kitchen:spice_trellis","stick","","stick","stick","string","stick","stick","string","stick");}p.closeMenu();check(p.count("smoke_kitchen:spice_trellis")==4,"Two real recipes yield four frames");
            for(var at:new BlockPos[]{WET,DRY,BUSH}){p.select("smoke_kitchen:spice_trellis");tap(p,at.below());check(observe(p,g->g.overworld().getBlockState(at).getValue(KitchenBlock.FACING)==Direction.NORTH),"Actual placed frame faces the player");}
            photo(p,"actual-empty-frame",WET);
            p.select("smoke_kitchen:black_pepper_seeds");int before=p.count("smoke_kitchen:black_pepper_seeds");frame(p,WET);check(p.count("smoke_kitchen:black_pepper_seeds")==before&&observe(p,g->g.overworld().isEmptyBlock(plot(WET))),"Untilled dirt cannot consume a seed or create an inventory plant");p.select("wheat_seeds");before=p.count("wheat_seeds");frame(p,WET);check(p.count("wheat_seeds")==before,"Unsupported seed is not consumed");
            for(var at:new BlockPos[]{WET,BUSH}){p.select("wooden_hoe");tap(p,plot(at).below());}hydrate(p,plot(WET).below());
            p.select("wooden_hoe");tap(p,plot(DRY).below());
            p.select("smoke_kitchen:black_pepper_seeds");int seeds=p.count("smoke_kitchen:black_pepper_seeds");frame(p,WET);check(p.count("smoke_kitchen:black_pepper_seeds")==seeds-1,"Real frame planting consumes exactly one seed");frame(p,WET);check(p.count("smoke_kitchen:black_pepper_seeds")==seeds-1,"Occupied front plot consumes no extra seed");frame(p,DRY);
            p.select("smoke_kitchen:sichuan_pepper_seeds");frame(p,BUSH);photo(p,"actual-seedlings",WET);
            p.select("bone_meal");int meal=p.count("bone_meal");tap(p,plot(DRY));check(p.count("bone_meal")==meal&&observe(p,g->KitchenContent.CROPS.get("black_pepper").getAge(g.overworld().getBlockState(plot(DRY)))==0),"Dry-rooted pepper pauses and does not consume bone meal");
            p.select("bone_meal");tap(p,plot(WET));photo(p,"actual-growing-vine",WET);grow(p,plot(WET),"black_pepper");mine(p,BUSH);grow(p,plot(BUSH),"sichuan_pepper");
            check(observe(p,g->g.overworld().getBlockState(plot(WET)).getValue(PepperCropBlock.FACING)==Direction.SOUTH),"Actual growth keeps vine oriented toward its real frame");photo(p,"actual-mature-pepper",WET);photo(p,"actual-mature-shrub",BUSH);
            p.select("bucket");near(p,new BlockPos(0,99,-3));p.look(new Vec3(.5,99.9,-3.5));c.runOnClient(m->m.gameMode.useItem(m.player,InteractionHand.MAIN_HAND));c.waitTicks(8);check(p.count("water_bucket")==1,"Actually collect source water");p.select("water_bucket");p.s.runCommand("tp @a 7.5 100 -0.5");c.waitTicks(5);p.look(new Vec3(8.5,99,-.5));c.runOnClient(m->m.gameMode.useItem(m.player,InteractionHand.MAIN_HAND));c.waitTicks(8);check(observe(p,g->g.overworld().getFluidState(new BlockPos(8,99,-1)).is(net.minecraft.tags.FluidTags.WATER)),"Actually place irrigation beside dry roots");hydrate(p,plot(DRY).below());grow(p,plot(DRY),"black_pepper");photo(p,"actual-irrigation-recovery",DRY);
            int harvestSeedsBefore=p.count("smoke_kitchen:black_pepper_seeds");p.emptyHand();near(p,WET);p.look(Vec3.atCenterOf(plot(WET)));c.runOnClient(m->m.gameMode.startDestroyBlock(plot(WET),Direction.UP));c.waitTicks(5);c.runOnClient(m->m.gameMode.stopDestroyBlock());s.runCommand("tp @a 3.5 99.9375 -0.5");c.waitTicks(30);check(p.count("smoke_kitchen:black_pepper")>=2&&p.count("smoke_kitchen:black_pepper_seeds")>=harvestSeedsBefore+2,"Physical harvest yields actual pepper fruit and new replanting seeds");p.select("smoke_kitchen:black_pepper_seeds");frame(p,WET);check(observe(p,g->g.overworld().getBlockState(plot(WET)).is(KitchenContent.CROPS.get("black_pepper"))),"Actually replant harvested seed");
            mine(p,WET);p.select("bone_meal");meal=p.count("bone_meal");tap(p,plot(WET));check(p.count("bone_meal")==meal&&observe(p,g->g.overworld().getBlockState(plot(WET)).is(KitchenContent.CROPS.get("black_pepper"))),"Removing real support pauses rather than deleting the seedling");photo(p,"actual-support-removed",WET);
            // Explicit old-save fixture: preserves all27 slots; never counted as crafted garden evidence.
            s.runOnServer(g->{g.overworld().setBlockAndUpdate(LEGACY,KitchenContent.BLOCKS.get("spice_trellis").defaultBlockState());var e=(KitchenBlockEntity)g.overworld().getBlockEntity(LEGACY);e.items.set(0,new ItemStack(Items.COBBLESTONE,64));e.items.set(5,new ItemStack(Items.APPLE,17));var tool=new ItemStack(Items.IRON_PICKAXE);tool.setDamageValue(23);tool.set(DataComponents.CUSTOM_NAME,Component.literal("旧种植架的镐"));e.items.set(26,tool);e.setChanged();});
            p.select("smoke_kitchen:recipe_book");c.runOnClient(m->m.gameMode.useItem(m.player,InteractionHand.MAIN_HAND));c.waitTicks(6);c.takeScreenshot("trellis-actual-crafted-guide-0.1.20");p.closeMenu();
        }
        try(var w=save.open()){
            var p=new SurvivalPlayer(c,w.getServer());c.waitTicks(12);check(observe(p,g->g.overworld().isEmptyBlock(WET)&&g.overworld().getBlockState(plot(WET)).is(KitchenContent.CROPS.get("black_pepper"))&&KitchenContent.CROPS.get("black_pepper").isMaxAge(g.overworld().getBlockState(plot(DRY)))),"Actual reopen preserves harvested/replanted paused vine and recovered mature plot");
            p.emptyHand();frame(p,LEGACY);check(observe(p,g->g.getPlayerList().getPlayers().getFirst().getInventory().getNonEquipmentItems().stream().anyMatch(x->x.is(Items.IRON_PICKAXE)&&x.getDamageValue()==23&&x.getHoverName().getString().equals("旧种植架的镐"))),"Actual old slot26 tool returns with original name and damage");mine(p,LEGACY);check(p.count("apple")==17&&p.count("cobblestone")==64,"Actual breaking returns remaining old stacks without truncation");
            p.select("smoke_kitchen:spice_trellis");tap(p,WET.below());grow(p,plot(WET),"black_pepper");photo(p,"actual-reopen-restored-support",WET);
        }
        try(var w=save.open()){w.getServer().runOnServer(g->{check(KitchenContent.CROPS.get("black_pepper").isMaxAge(g.overworld().getBlockState(plot(WET)))&&g.overworld().isEmptyBlock(LEGACY),"Second actual reopen preserves restored garden and removed legacy frame");});c.takeScreenshot("trellis-second-reopen-0.1.20");}
    }
}
