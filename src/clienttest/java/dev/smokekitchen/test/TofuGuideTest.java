package dev.smokekitchen.test;

import dev.smokekitchen.content.KitchenContent;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

/** Actual book crafting and navigation; the separate contact sheet is a model preview only. */
public final class TofuGuideTest implements FabricClientGameTest {
    private static void next(Screen s){for(var child:s.children())if(child instanceof net.minecraft.client.gui.components.Button b&&b.getMessage().getString().equals("下一页")){b.onPress(null);return;}}
    private static boolean tofu(Screen s){try{var f=s.getClass().getDeclaredField("renderedPages");f.setAccessible(true);var i=s.getClass().getDeclaredField("page");i.setAccessible(true);var page=((java.util.List<?>)f.get(s)).get(i.getInt(s));var title=page.getClass().getDeclaredMethod("title");title.setAccessible(true);return "从大豆制作豆腐".equals(title.invoke(page));}catch(ReflectiveOperationException e){throw new AssertionError(e);}}
    @Override public void runTest(ClientGameTestContext c){
        var portion=new dev.smokekitchen.cooking.CookingBatch.Portion("tofu",dev.smokekitchen.cooking.CookingBatch.Cut.DICE,1,false,true);
        if(!dev.smokekitchen.client.KitchenStationRenderer.stage(portion,false).equals("raw"))throw new AssertionError("Safe-to-eat tofu must remain pale in a cold pan");
        portion.maximumTemperature=160;if(!dev.smokekitchen.client.KitchenStationRenderer.stage(portion,false).equals("browning"))throw new AssertionError("Dry hot tofu begins browning");
        if(!dev.smokekitchen.client.KitchenStationRenderer.stage(portion,true).equals("poached"))throw new AssertionError("Wet tofu must not look fried");
        try(var world=c.worldBuilder().create()){
            c.runOnClient(client->client.options.guiScale().set(2));var server=world.getServer();server.runCommand("gamemode survival @a");server.runCommand("give @a minecraft:book");server.runCommand("give @a minecraft:wheat_seeds");c.waitTicks(8);var p=new SurvivalPlayer(c,server);p.craft("smoke_kitchen:recipe_book","book","wheat_seeds");p.closeMenu();p.select("smoke_kitchen:recipe_book");c.runOnClient(client->client.gameMode.useItem(client.player,InteractionHand.MAIN_HAND));c.waitTicks(4);
            int captured=0;for(int n=0;n<60;n++){boolean here=c.computeOnClient(client->tofu(client.screen));if(here){c.runOnClient(client->client.getToastManager().clear());c.takeScreenshot("tofu-guide-"+(++captured)+"-0.1.13");}else if(captured>0)break;c.runOnClient(client->next(client.screen));c.waitTicks(3);}
            if(captured==0)throw new AssertionError("Actual crafted recipe book must expose tofu's complete process");c.runOnClient(client->client.setScreen(new Preview()));c.waitTicks(5);c.takeScreenshot("tofu-original-model-preview-0.1.13");c.runOnClient(client->client.setScreen(null));
            fluidFixture(c,server,p);
        }
    }
    private static void fluidFixture(ClientGameTestContext c,net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext server,SurvivalPlayer player){
        // Isolated visual/regression fixture. These granted intermediates are NOT production-chain evidence.
        var pos=new net.minecraft.core.BlockPos(0,100,0);server.runCommand("fill -4 99 -4 4 99 4 minecraft:smooth_stone");server.runCommand("fill -1 100 -3 1 101 -2 minecraft:smooth_stone");server.runCommand("setblock 0 100 0 smoke_kitchen:clay_stove");server.runCommand("tp @a 0.5 102 -1.5 0 50");server.runCommand("time set day");server.runCommand("weather clear");
        for(String id:new String[]{"clay_pot","soy_milk","rice_vinegar","wooden_lid"})server.runCommand("give @a smoke_kitchen:"+id);server.runCommand("give @a minecraft:coal 2");c.waitTicks(6);
        for(String id:new String[]{"smoke_kitchen:clay_pot","smoke_kitchen:soy_milk","coal"}){player.select(id);player.click(pos,net.minecraft.core.Direction.UP);}c.waitTicks(100);player.emptyHand();player.look(net.minecraft.world.phys.Vec3.atCenterOf(pos));c.takeScreenshot("tofu-fluid-fixture-unset-0.1.13");
        player.select("smoke_kitchen:rice_vinegar");player.click(pos,net.minecraft.core.Direction.UP);player.emptyHand();boolean partial=false;
        for(int n=0;n<400;n+=4){boolean[] at={false};server.runOnServer(s->{var e=(dev.smokekitchen.world.KitchenBlockEntity)s.overworld().getBlockEntity(pos);double progress=e.batch.ingredients.getFirst().cooked;at[0]=progress>=.3&&progress<1;});if(at[0]){partial=true;break;}c.waitTicks(4);}
        if(!partial)throw new AssertionError("Actual visible milk must form partial curds through heating and acid");c.waitTicks(2);var mid=snapshot(c,pos);if(mid.pieces.size()<2)throw new AssertionError("Coagulation must show several small fragments, not one floating jar");c.takeScreenshot("tofu-fluid-fixture-partial-0.1.13");c.waitTicks(200);var ready=snapshot(c,pos);if(ready.pieces.size()!=6)throw new AssertionError("Set milk must show six curd fragments");c.takeScreenshot("tofu-fluid-fixture-ready-0.1.13");
        player.select("smoke_kitchen:wooden_lid");player.click(pos,net.minecraft.core.Direction.UP);c.waitTicks(4);if(!snapshot(c,pos).pieces.isEmpty())throw new AssertionError("A closed pot must hide curds");c.takeScreenshot("tofu-fluid-fixture-covered-0.1.13");
    }
    private static dev.smokekitchen.client.KitchenStationRenderer.State snapshot(ClientGameTestContext c,net.minecraft.core.BlockPos pos){
        dev.smokekitchen.client.KitchenStationRenderer.State[] result={null};c.runOnClient(client->{var e=(dev.smokekitchen.world.KitchenBlockEntity)client.level.getBlockEntity(pos);var renderer=(dev.smokekitchen.client.KitchenStationRenderer)(Object)client.getBlockEntityRenderDispatcher().getRenderer(e);var state=renderer.createRenderState();renderer.extractRenderState(e,state,.5f,client.player.getEyePosition(),null);result[0]=state;});return result[0];
    }
    private static final class Preview extends Screen {
        Preview(){super(Component.literal("豆腐工序原创模型预览"));}
        @Override public void extractRenderState(GuiGraphicsExtractor g,int mouseX,int mouseY,float delta){
            g.fill(0,0,width,height,0xff273038);g.centeredText(font,"0.1.13 / 工序模型预览（非产物验收）",width/2,10,0xffffffff);
            String[] ids={"soybean","soaked_soybean","raw_bean_pulp","boiled_bean_pulp","soy_milk","soy_curds","tofu","okara"};int pitch=(width-24)/4;
            for(int n=0;n<ids.length;n++){var item=new ItemStack(KitchenContent.item(ids[n]));int x=12+n%4*pitch,y=32+n/4*100;g.fill(x,y,x+pitch-6,y+94,0xffc9c4b7);g.pose().pushMatrix();g.pose().translate(x+(pitch-54)/2f,y+3).scale(3);g.item(item,0,0);g.pose().popMatrix();int line=y+58;for(var text:font.split(item.getHoverName(),pitch-12)){g.text(font,text,x+5,line,0xff17232b,false);line+=10;}}
        }
    }
}
