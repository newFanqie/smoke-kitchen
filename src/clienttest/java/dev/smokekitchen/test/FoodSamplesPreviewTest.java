package dev.smokekitchen.test;
import dev.smokekitchen.content.KitchenContent;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.world.item.ItemStack;
/** Explicitly isolated model-state preview; no food is granted and this is not an aging/production test. */
public final class FoodSamplesPreviewTest implements FabricClientGameTest {
 @Override public void runTest(ClientGameTestContext c){try(var world=c.worldBuilder().create()){c.runOnClient(client->{client.options.guiScale().set(1);client.getToastManager().clear();client.setScreen(new Preview());});c.waitTicks(8);c.takeScreenshot("food-samples-model-states-preview-only-0.1.17");c.runOnClient(client->client.setScreen(null));}}
 private static final class Preview extends net.minecraft.client.gui.screens.Screen {
  Preview(){super(net.minecraft.network.chat.Component.literal("Model preview only"));}
  @Override public void extractRenderState(net.minecraft.client.gui.GuiGraphicsExtractor g,int x,int y,float delta){g.fill(0,0,width,height,0xff263038);g.centeredText(font,"模型状态预览 · 不代表实际制作或保鲜流程",width/2,12,0xfff4ead8);String[] bases={"duck_meat","cooked_duck","mixed_stew"},prefixes={"","stale_","spoiled_"};int pitch=width/3,row=(height-34)/3;for(int i=0;i<3;i++)for(int j=0;j<3;j++){int left=j*pitch,top=30+i*row;var item=new ItemStack(KitchenContent.item(prefixes[j]+bases[i]));g.fill(left+8,top,left+pitch-8,top+row-8,0xffd2cbbd);g.pose().pushMatrix();g.pose().translate(left+pitch/2f-40,top+3).scale(5);g.item(item,0,0);g.pose().popMatrix();g.item(item,left+22,top+row-30);g.centeredText(font,item.getHoverName(),left+pitch/2,top+row-24,0xff263038);}}
 }
}
