package dev.smokekitchen.test;

import dev.smokekitchen.client.KitchenStationRenderer;
import dev.smokekitchen.content.KitchenContent;
import dev.smokekitchen.world.*;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.*;

/** Explicit legacy-save fixture, not raw-material production. Retrieval uses real survival clicks. */
public final class LegacyRackTest implements FabricClientGameTest {
    private static final Direction[] FACES={Direction.NORTH,Direction.EAST,Direction.SOUTH,Direction.WEST};
    private static BlockPos pos(int i){return new BlockPos(i*3,100,0);}
    private static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
    private static ItemStack original(int slot){
        var stack=switch(slot){case 0->new ItemStack(Items.APPLE,17);case 1->new ItemStack(Items.IRON_PICKAXE);case 2->new ItemStack(Items.OAK_PLANKS,3);case 3->new ItemStack(KitchenContent.item("used_salt"));case 4->new ItemStack(Items.GLASS_BOTTLE);default->new ItemStack(Items.OAK_STAIRS,2);};
        if(slot==1){stack.setDamageValue(23);stack.set(DataComponents.CUSTOM_NAME,Component.literal("旧架里的旧镐"));}if(slot==3)stack.setDamageValue(7);return stack;
    }
    private static KitchenStationRenderer.State snapshot(ClientGameTestContext c,BlockPos pos){return c.computeOnClient(m->{var e=(KitchenBlockEntity)m.level.getBlockEntity(pos);var r=(KitchenStationRenderer)(Object)m.getBlockEntityRenderDispatcher().getRenderer(e);var state=r.createRenderState();r.extractRenderState(e,state,0,m.player.getEyePosition(),null);return state;});}
    private static void inspect(ClientGameTestContext c,int i){
        var state=snapshot(c,pos(i));check(state.pieces.size()==6,"Six legacy cells remain visible");
        for(int slot=0;slot<6;slot++){
            var p=state.pieces.get(slot);check(p.appearance().equals(slot==3?"rack/salt_used":slot==4?"rack/empty_empty":"rack/legacy_item"),"Original legacy model rather than substituted empty jar, slot "+slot);
            if(p.appearance().equals("rack/legacy_item")){
                var b=p.item().getModelBoundingBox();double bottom=p.y()+p.scale()*(.5+b.minY);
                check(Math.abs(bottom-(slot<3?3:9)/16d)<.00001,"Original model rests on its actual shelf");
                check(b.getYsize()*p.scale()<=.28001&&Math.max(b.getXsize(),b.getZsize())*p.scale()<=.26001,"Original bounds fit the shelf");
            }
        }
    }
    private static void cell(SurvivalPlayer p,int i,int slot){
        var at=pos(i);var face=FACES[i];p.s.runCommand("tp @a "+(at.getX()+.5+face.getStepX()*2.5)+" 100 "+(at.getZ()+.5+face.getStepZ()*2.5));p.c.waitTicks(4);var xz=SpiceRack.position(slot,face);
        double x=at.getX()+xz[0],z=at.getZ()+xz[1];if(face.getAxis()==Direction.Axis.X)x=at.getX()+(face==Direction.EAST?.99:.01);else z=at.getZ()+(face==Direction.SOUTH?.875:.125);
        var hit=new Vec3(x,at.getY()+(slot<3?.3:.7),z);p.look(hit);p.c.runOnClient(m->m.gameMode.useItemOn(m.player,InteractionHand.MAIN_HAND,new BlockHitResult(hit,face,at,false)));p.c.waitTicks(5);
    }
    @Override public void runTest(ClientGameTestContext c){
        TestWorldSave save;
        try(var w=c.worldBuilder().create()){
            save=w.getWorldSave();var s=w.getServer();s.runCommand("fill -4 99 -5 13 99 5 minecraft:smooth_stone");s.runCommand("gamemode survival @a");s.runCommand("time set noon");s.runCommand("weather clear");
            s.runOnServer(g->{for(int i=0;i<4;i++){g.overworld().setBlockAndUpdate(pos(i),KitchenContent.BLOCKS.get("spice_rack").defaultBlockState().setValue(KitchenBlock.FACING,FACES[i]));var e=(KitchenBlockEntity)g.overworld().getBlockEntity(pos(i));for(int slot=0;slot<6;slot++)e.items.set(slot,original(slot));e.setChanged();g.overworld().sendBlockUpdated(pos(i),e.getBlockState(),e.getBlockState(),3);}});c.waitTicks(15);
            if(net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("iris"))c.runOnClient(m->{try{var a=Class.forName("net.irisshaders.iris.api.v0.IrisApi");check((Boolean)a.getMethod("isShaderPackInUse").invoke(a.getMethod("getInstance").invoke(null)),"Iris must have an active shader");}catch(ReflectiveOperationException e){throw new AssertionError(e);}});
            var p=new SurvivalPlayer(c,s);
            c.runOnClient(m->m.options.fov().set(40));
            for(int i=0;i<4;i++){inspect(c,i);var f=FACES[i];var at=pos(i);s.runCommand("tp @a "+(at.getX()+.5+f.getStepX()*3.5-f.getStepZ())+" 100 "+(at.getZ()+.5+f.getStepZ()*3.5+f.getStepX()));c.waitTicks(6);p.look(Vec3.atCenterOf(at));c.runOnClient(m->m.getToastManager().clear());c.takeScreenshot("legacy-rack-fixture-"+f.getName()+"-0.1.19");}
            c.runOnClient(m->m.options.fov().set(70));
        }
        try(var w=save.open()){
            c.waitTicks(12);var p=new SurvivalPlayer(c,w.getServer());
            for(int i=0;i<4;i++){inspect(c,i);for(int slot=0;slot<6;slot++){
                p.emptyHand();cell(p,i,slot);int index=i,cell=slot;
                p.s.runOnServer(g->{var e=(KitchenBlockEntity)g.overworld().getBlockEntity(pos(index));check(e.items.get(cell).isEmpty(),"Real targeted click empties the original legacy cell");var expected=original(cell);int n=g.getPlayerList().getPlayers().getFirst().getInventory().getNonEquipmentItems().stream().filter(x->ItemStack.isSameItemSameComponents(x,expected)).mapToInt(ItemStack::getCount).sum();check(n==expected.getCount()*(index+1),"Actual retrieval preserves exact count, name, damage and components");});
            }}c.takeScreenshot("legacy-rack-actual-retrieval-0.1.19");
        }
        try(var w=save.open()){w.getServer().runOnServer(g->{for(int i=0;i<4;i++)check(((KitchenBlockEntity)g.overworld().getBlockEntity(pos(i))).items.stream().allMatch(ItemStack::isEmpty),"Second real reopen does not restore or duplicate retrieved legacy items");});c.takeScreenshot("legacy-rack-second-reopen-0.1.19");}
    }
}
