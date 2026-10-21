package dev.smokekitchen.test;

import dev.smokekitchen.content.KitchenContent;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

/** Crafted book and sieve; separate model-only previews do not count as produced food. */
public final class FermentationGuideTest implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext c){
        try(var world=c.worldBuilder().create()){
            c.runOnClient(client->client.options.guiScale().set(2));var s=world.getServer();s.runCommand("gamemode survival @a");for(String raw:new String[]{"minecraft:book","minecraft:wheat_seeds","minecraft:stick","minecraft:string"})s.runCommand("give @a "+raw);c.waitTicks(8);var p=new SurvivalPlayer(c,s);p.craft("smoke_kitchen:sieve","stick","string");p.craft("smoke_kitchen:recipe_book","book","wheat_seeds");p.closeMenu();p.select("smoke_kitchen:recipe_book");c.runOnClient(client->client.gameMode.useItem(client.player,InteractionHand.MAIN_HAND));c.waitTicks(4);
            boolean found=false;for(int i=0;i<30;i++){
                boolean[] at={false};c.runOnClient(client->{try{var screen=client.screen;var pages=screen.getClass().getDeclaredField("renderedPages");pages.setAccessible(true);var index=screen.getClass().getDeclaredField("page");index.setAccessible(true);var entry=((java.util.List<?>)pages.get(screen)).get(index.getInt(screen));var title=entry.getClass().getDeclaredMethod("title");title.setAccessible(true);at[0]="酱油与米醋分步制作".equals(title.invoke(entry));if(!at[0])next(screen);}catch(ReflectiveOperationException e){throw new AssertionError(e);}});c.waitTicks(3);
                if(at[0]){found=true;c.runOnClient(client->client.getToastManager().clear());c.takeScreenshot("fermentation-guide-a-0.1.12");c.runOnClient(client->next(client.screen));c.waitTicks(3);c.takeScreenshot("fermentation-guide-b-0.1.12");break;}
            }
            if(!found)throw new AssertionError("The actually crafted book must expose the full fermentation and filtering guide");c.runOnClient(client->client.setScreen(new Preview()));c.waitTicks(5);c.takeScreenshot("fermentation-model-preview-0.1.12");c.runOnClient(client->client.setScreen(new KitchenVisualTest.ToolSheet()));c.waitTicks(4);c.takeScreenshot("kitchen-final-tools-0.1.12");c.runOnClient(client->client.setScreen(null));
        }
    }
    private static void next(Screen screen){for(var child:screen.children())if(child instanceof net.minecraft.client.gui.components.Button b&&b.getMessage().getString().equals("下一页")){b.onPress(null);break;}}
    private static final class Preview extends Screen {
        Preview(){super(Component.literal("发酵工序模型预览"));}
        @Override public void extractRenderState(GuiGraphicsExtractor g,int mouseX,int mouseY,float delta){
            g.fill(0,0,width,height,0xff273038);g.centeredText(font,"0.1.12 / 独立工序物品模型预览",width/2,10,0xffffffff);String[] ids={"soybean","cooked_soybean","soy_mash","vinegar_mash","soy_sauce","rice_vinegar"};int pitch=(width-24)/3;
            for(int i=0;i<ids.length;i++){var item=new ItemStack(KitchenContent.item(ids[i]));int x=12+i%3*pitch,y=32+i/3*100;g.fill(x,y,x+pitch-6,y+94,0xffc9c4b7);g.pose().pushMatrix();g.pose().translate(x+(pitch-54)/2f,y+3).scale(3);g.item(item,0,0);g.pose().popMatrix();int line=y+58;for(var text:font.split(item.getHoverName(),pitch-12)){g.text(font,text,x+5,line,0xff17232b,false);line+=10;}}
        }
    }
}
