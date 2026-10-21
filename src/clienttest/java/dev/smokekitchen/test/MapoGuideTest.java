package dev.smokekitchen.test;

import dev.smokekitchen.content.KitchenContent;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

/** Actual book craft/navigation. The separate enlarged contact sheet explicitly previews models only. */
public final class MapoGuideTest implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext c){
        try(var world=c.worldBuilder().create()){
            c.runOnClient(client->client.options.guiScale().set(2));var s=world.getServer();s.runCommand("gamemode survival @a");s.runCommand("give @a minecraft:book");s.runCommand("give @a minecraft:wheat_seeds");c.waitTicks(8);var p=new SurvivalPlayer(c,s);p.craft("smoke_kitchen:recipe_book","book","wheat_seeds");p.closeMenu();p.select("smoke_kitchen:recipe_book");c.runOnClient(client->client.gameMode.useItem(client.player,InteractionHand.MAIN_HAND));c.waitTicks(4);
            int captured=0;
            for(int n=0;n<120;n++){
                boolean here=c.computeOnClient(client->{try{var screen=client.screen;var f=screen.getClass().getDeclaredField("renderedPages");f.setAccessible(true);var i=screen.getClass().getDeclaredField("page");i.setAccessible(true);var page=((java.util.List<?>)f.get(screen)).get(i.getInt(screen));var title=page.getClass().getDeclaredMethod("title");title.setAccessible(true);return "麻婆豆腐".equals(title.invoke(page));}catch(ReflectiveOperationException e){throw new AssertionError(e);}});
                if(here){c.runOnClient(client->client.getToastManager().clear());c.takeScreenshot("mapo-guide-"+(++captured)+"-0.1.14");}else if(captured>0)break;
                c.runOnClient(client->{for(var child:client.screen.children())if(child instanceof net.minecraft.client.gui.components.Button button&&button.getMessage().getString().equals("下一页")){button.onPress(null);break;}});c.waitTicks(3);
            }
            if(captured==0)throw new AssertionError("Crafted guide must expose the complete Mapo tofu method");c.runOnClient(client->client.setScreen(new Preview()));c.waitTicks(5);c.takeScreenshot("mapo-original-model-preview-0.1.14");c.runOnClient(client->client.setScreen(null));
            sauceFixture(c,p);
        }
    }
    private static void sauceFixture(ClientGameTestContext c,SurvivalPlayer p){
        // Granted intermediates below are ONLY for a fast visual/regression fixture, never production evidence.
        var pos=new net.minecraft.core.BlockPos(0,100,0);p.s.runCommand("fill -4 99 -4 4 99 4 minecraft:smooth_stone");p.s.runCommand("fill -1 100 -3 1 101 -2 minecraft:smooth_stone");p.s.runCommand("setblock 0 100 0 smoke_kitchen:clay_stove");p.s.runCommand("time set day");p.s.runCommand("weather clear");p.s.runCommand("tp @a 0.5 102 -1.5 0 50");
        for(String id:new String[]{"wok","tofu 2","chili","sichuan_pepper","soy_sauce","lard","water_jug","wooden_lid"})p.s.runCommand("give @a smoke_kitchen:"+id);p.s.runCommand("give @a minecraft:coal");c.waitTicks(6);
        for(String id:new String[]{"wok","water_jug","lard","soy_sauce","tofu","tofu","chili","sichuan_pepper"}){p.select("smoke_kitchen:"+id);p.click(pos,net.minecraft.core.Direction.UP);}p.emptyHand();p.look(net.minecraft.world.phys.Vec3.atCenterOf(pos));c.takeScreenshot("mapo-sauce-fixture-before-heating-0.1.14");p.select("coal");p.click(pos,net.minecraft.core.Direction.UP);p.emptyHand();
        boolean colored=false;for(int n=0;n<900;n+=20){var state=snapshot(c,pos);if(state.liquidAppearance.equals("pot_mapo_sauce")){if(state.pieces.stream().filter(x->x.appearance().equals("food/sichuan_pepper")).count()!=4)throw new AssertionError("Four small pepper fragments must replace the flat crop icon");colored=true;break;}c.waitTicks(20);}if(!colored)throw new AssertionError("Real heated chili in tofu sauce must color the liquid");c.takeScreenshot("mapo-sauce-fixture-heated-0.1.14");p.select("smoke_kitchen:wooden_lid");p.click(pos,net.minecraft.core.Direction.UP);c.waitTicks(4);var covered=snapshot(c,pos);if(!covered.pieces.isEmpty()||!covered.liquidAppearance.isEmpty())throw new AssertionError("Covering must hide both sauce and ingredients");c.takeScreenshot("mapo-sauce-fixture-covered-0.1.14");
    }
    private static dev.smokekitchen.client.KitchenStationRenderer.State snapshot(ClientGameTestContext c,net.minecraft.core.BlockPos pos){
        dev.smokekitchen.client.KitchenStationRenderer.State[] result={null};c.runOnClient(client->{var e=(dev.smokekitchen.world.KitchenBlockEntity)client.level.getBlockEntity(pos);var renderer=(dev.smokekitchen.client.KitchenStationRenderer)(Object)client.getBlockEntityRenderDispatcher().getRenderer(e);var state=renderer.createRenderState();renderer.extractRenderState(e,state,.5f,client.player.getEyePosition(),null);result[0]=state;});return result[0];
    }
    private static final class Preview extends Screen {
        Preview(){super(Component.literal("麻婆豆腐原创模型"));}
        @Override public void extractRenderState(GuiGraphicsExtractor g,int mouseX,int mouseY,float delta){
            g.fill(0,0,width,height,0xff273038);g.centeredText(font,"0.1.14 / 原创菜肴模型预览（非产物验收）",width/2,12,0xffffffff);
            String[] ids={"tofu","mapo_tofu"};int pitch=(width-30)/2;
            for(int n=0;n<ids.length;n++){int x=10+n*pitch,y=38;var item=new ItemStack(KitchenContent.item(ids[n]));g.fill(x,y,x+pitch-5,height-30,0xffc9c4b7);g.pose().pushMatrix();g.pose().translate(x+(pitch-96)/2f,y+15).scale(6);g.item(item,0,0);g.pose().popMatrix();g.centeredText(font,item.getHoverName(),x+pitch/2,y+120,0xff17232b);}
        }
    }
}
