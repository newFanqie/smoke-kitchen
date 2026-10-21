package dev.smokekitchen.test;

import dev.smokekitchen.content.KitchenContent;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

/** Actual book crafting/navigation and readable transfer rules. */
public final class TransferGuideTest implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext c){
        try(var world=c.worldBuilder().create()){
            c.runOnClient(client->client.options.guiScale().set(2));var s=world.getServer();s.runCommand("gamemode survival @a");s.runCommand("give @a minecraft:book");s.runCommand("give @a minecraft:wheat_seeds");c.waitTicks(8);var p=new SurvivalPlayer(c,s);p.craft("smoke_kitchen:recipe_book","book","wheat_seeds");p.closeMenu();p.select("smoke_kitchen:recipe_book");c.runOnClient(client->client.gameMode.useItem(client.player,InteractionHand.MAIN_HAND));c.waitTicks(4);
            int captured=0;
            for(int n=0;n<120;n++){
                boolean here=c.computeOnClient(client->{try{var screen=client.screen;var f=screen.getClass().getDeclaredField("renderedPages");f.setAccessible(true);var i=screen.getClass().getDeclaredField("page");i.setAccessible(true);var page=((java.util.List<?>)f.get(screen)).get(i.getInt(screen));var title=page.getClass().getDeclaredMethod("title");title.setAccessible(true);return "散装食材与换锅".equals(title.invoke(page));}catch(ReflectiveOperationException e){throw new AssertionError(e);}});
                if(here){c.runOnClient(client->client.getToastManager().clear());c.takeScreenshot("transfer-guide-"+(++captured)+"-0.1.15");}else if(captured>0)break;
                c.runOnClient(client->{for(var child:client.screen.children())if(child instanceof net.minecraft.client.gui.components.Button button&&button.getMessage().getString().equals("下一页")){button.onPress(null);break;}});c.waitTicks(3);
            }
            if(captured==0)throw new AssertionError("Crafted guide must expose loose ingredient transfer rules");c.runOnClient(client->client.setScreen(null));
        }
    }
}
