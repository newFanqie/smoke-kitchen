package dev.smokekitchen.test;

import dev.smokekitchen.content.KitchenContent;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import java.util.List;

/** Uses Minecraft's actual item renderer so missing transforms and textures are visible. */
public final class KitchenVisualTest implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext context) {
        context.runOnClient(client->client.options.guiScale().set(2));
        try(var world=context.worldBuilder().create()) {
            context.setScreen(IconSheet::new);context.waitTicks(10);
            context.takeScreenshot("kitchen-inventory-0.1.1");
            context.runOnClient(client->client.setScreen(null));
            var server=world.getServer();
            server.runCommand("gamemode creative @a");
            server.runCommand("time set day");
            server.runCommand("weather clear");
            server.runCommand("fill -8 99 -8 8 99 8 minecraft:smooth_stone");
            server.runCommand("fill -8 100 -8 8 105 8 minecraft:air");
            for(int i=0;i<3;i++) {
                String id=List.of("clay_stove","brick_stove","double_stove").get(i);
                server.runCommand("setblock "+(i*2-2)+" 100 0 smoke_kitchen:"+id+"[facing=north,heat=2,fueled=true]");
            }
            server.runCommand("setblock 3 100 0 smoke_kitchen:double_stove[facing=north,secondary=true]");
            server.runCommand("setblock 4 100 0 smoke_kitchen:gas_stove[facing=north,heat=2]");
            server.runOnServer(game->{
                var level=game.overworld();
                for(int x:new int[]{-2,0,2,3,4})if(level.getBlockEntity(new net.minecraft.core.BlockPos(x,100,0)) instanceof dev.smokekitchen.world.KitchenBlockEntity stove){stove.heat=2;stove.fuelSeconds=1000;}
            });
            server.runCommand("tp @a 1 102 -5 0 20");
            world.getClientLevel().waitForChunksRender();context.waitTicks(10);
            context.takeScreenshot("kitchen-stoves-0.1.1");
        }
    }
    private static final class IconSheet extends Screen {
        private final List<ItemStack> icons=KitchenContent.ITEMS.values().stream().map(ItemStack::new).toList();
        IconSheet(){super(Component.literal("Smoke Kitchen 0.1.1"));}
        @Override public void extractRenderState(GuiGraphicsExtractor g,int x,int y,float delta) {
            g.fill(0,0,width,height,0xff242c34);
            g.text(font,"SMOKE KITCHEN 0.1.1 | "+icons.size()+" independent item models",8,6,0xffffffff);
            int columns=Math.max(1,(width-16)/22);
            for(int i=0;i<icons.size();i++) {
                int px=8+(i%columns)*22,py=24+(i/columns)*22;
                g.fill(px-1,py-1,px+18,py+18,0xffaaaaaa);g.item(icons.get(i),px,py);
            }
        }
    }
}
