package dev.smokekitchen.test;

import dev.smokekitchen.content.*;
import dev.smokekitchen.world.KitchenBlockEntity;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.*;
import java.util.List;

/** Real client networking, creative screen and rendered cooking results. */
public final class KitchenVisualTest implements FabricClientGameTest {
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    private static void equip(ClientGameTestContext context,TestServerContext server,String id){server.runCommand("item replace entity @a weapon.mainhand with "+id);context.waitTicks(4);}
    private static void click(ClientGameTestContext context,BlockPos pos,Direction face){
        context.runOnClient(client->client.gameMode.useItemOn(client.player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(pos),face,pos,false)));context.waitTicks(6);
    }
    private static void slotClick(ClientGameTestContext context,int slot,int button,net.minecraft.world.inventory.ContainerInput type) {
        context.runOnClient(client->client.gameMode.handleContainerInput(client.player.containerMenu.containerId,slot,button,type,client.player));context.waitTicks(3);
    }
    @Override public void runTest(ClientGameTestContext context) {
        context.runOnClient(client->client.options.guiScale().set(2));
        try(var world=context.worldBuilder().create()) {
            if(net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("iris"))context.runOnClient(client->{
                try {
                var api=Class.forName("net.irisshaders.iris.api.v0.IrisApi");var instance=api.getMethod("getInstance").invoke(null);
                check((Boolean)api.getMethod("isShaderPackInUse").invoke(instance),"Iris compatibility run must have a shader pack actually active");
                } catch(ReflectiveOperationException exception) {
                    throw new AssertionError("Cannot verify the active Iris shader pack",exception);
                }
            });
            var server=world.getServer();server.runCommand("gamemode creative @a");context.waitTicks(5);
            context.setScreen(()->{var client=net.minecraft.client.Minecraft.getInstance();return new CreativeModeInventoryScreen(client.player,client.level.enabledFeatures(),false);});context.waitTicks(5);
            context.runOnClient(client->{
                var screen=(net.fabricmc.fabric.api.client.creativetab.v1.FabricCreativeModeInventoryScreen)client.screen;
                check(screen.getSelectedTab()==BuiltInRegistries.CREATIVE_MODE_TAB.getValue(dev.smokekitchen.SmokeKitchen.id("kitchen")),"The kitchen tab must be discoverable on first inventory open");
                check(screen.getSelectedTab().getDisplayItems().size()==KitchenContent.ITEMS.size(),"Every mod item and state must be present in the one kitchen tab");
            });
            context.takeScreenshot("kitchen-creative-0.1.4");
            context.setScreen(ToolSheet::new);context.waitTicks(5);context.takeScreenshot("kitchen-tools-and-states-0.1.4");
            context.runOnClient(client->client.setScreen(null));
            server.runCommand("time set day");server.runCommand("weather clear");
            server.runCommand("fill -8 99 -8 8 99 8 minecraft:smooth_stone");server.runCommand("fill -8 100 -8 8 105 8 minecraft:air");
            server.runCommand("tp @a 1 100 -3 0 15");server.runCommand("gamemode survival @a");context.waitTicks(10);
            equip(context,server,"smoke_kitchen:brick_stove");click(context,new BlockPos(0,99,0),Direction.UP);
            equip(context,server,"smoke_kitchen:gas_stove");click(context,new BlockPos(2,99,0),Direction.UP);
            server.runCommand("setblock -1 100 -1 minecraft:crafting_table");equip(context,server,"minecraft:air");click(context,new BlockPos(-1,100,-1),Direction.UP);
            server.runCommand("item replace entity @a hotbar.0 with minecraft:iron_ingot 3");server.runCommand("item replace entity @a hotbar.1 with minecraft:stick");context.waitTicks(5);
            slotClick(context,1,0,net.minecraft.world.inventory.ContainerInput.SWAP);slotClick(context,1,0,net.minecraft.world.inventory.ContainerInput.PICKUP);
            for(int slot:new int[]{1,2,3})slotClick(context,slot,1,net.minecraft.world.inventory.ContainerInput.PICKUP);
            slotClick(context,5,1,net.minecraft.world.inventory.ContainerInput.SWAP);
            context.waitTicks(4);context.takeScreenshot("kitchen-crafting-pan-0.1.4");slotClick(context,0,0,net.minecraft.world.inventory.ContainerInput.QUICK_MOVE);
            server.runOnServer(game->check(game.getPlayerList().getPlayers().getFirst().getInventory().contains(stack->stack.is(KitchenContent.item("frying_pan"))),"The normal crafting table must produce a frying pan from three iron ingots and one stick"));
            context.runOnClient(client->client.player.closeContainer());context.waitTicks(5);
            equip(context,server,"smoke_kitchen:frying_pan");click(context,new BlockPos(0,100,0),Direction.UP);
            equip(context,server,"minecraft:coal");click(context,new BlockPos(0,100,0),Direction.NORTH);
            equip(context,server,"minecraft:chicken");click(context,new BlockPos(0,100,0),Direction.UP);
            equip(context,server,"smoke_kitchen:gas_canister");click(context,new BlockPos(2,100,0),Direction.NORTH);
            equip(context,server,"smoke_kitchen:frying_pan");click(context,new BlockPos(2,100,0),Direction.UP);
            equip(context,server,"minecraft:chicken");click(context,new BlockPos(2,100,0),Direction.UP);
            equip(context,server,"minecraft:air");
            server.runOnServer(game->{
                for(int x:new int[]{0,2}){var stove=(KitchenBlockEntity)game.overworld().getBlockEntity(new BlockPos(x,100,0));check(stove.mountedCookware.equals("frying_pan")&&stove.batch.ingredients.size()==1&&stove.heat==2,"Normal client clicks must mount cookware, load food and ignite");}
            });
            world.getClientLevel().waitForChunksRender();context.takeScreenshot("kitchen-low-counter-raw-0.1.4");
            context.waitTicks(6);context.takeScreenshot("kitchen-native-fire-next-frame-0.1.4");
            context.waitTicks(1100);
            server.runOnServer(game->{for(int x:new int[]{0,2}){var stove=(KitchenBlockEntity)game.overworld().getBlockEntity(new BlockPos(x,100,0));check(stove.batch.ready()&&!stove.batch.burnt()&&stove.displayedFood(0).is(Items.COOKED_CHICKEN),"Both coal and gas must produce actual cooked chicken");}});
            context.waitTicks(4);context.takeScreenshot("kitchen-low-counter-cooked-0.1.4");
            context.getInput().holdKey(options->options.keyShift);context.waitTicks(3);
            click(context,new BlockPos(0,100,0),Direction.UP);context.getInput().releaseKey(options->options.keyShift);context.waitTicks(3);
            server.runOnServer(game->check(game.getPlayerList().getPlayers().getFirst().getInventory().contains(stack->stack.is(Items.COOKED_CHICKEN)),"A real client must retrieve cooked chicken into inventory"));
            equip(context,server,"minecraft:air");
            context.getInput().holdKey(options->options.keyShift);context.waitTicks(3);click(context,new BlockPos(2,100,0),Direction.NORTH);context.getInput().releaseKey(options->options.keyShift);context.waitTicks(3);
            server.runOnServer(game->check(game.getPlayerList().getPlayers().getFirst().getInventory().contains(stack->stack.is(KitchenContent.item("partial_gas_canister"))),"A real client must retrieve the used gas canister"));
            server.runCommand("setblock 3 100 0 smoke_kitchen:gas_pipe");server.runCommand("setblock 4 100 0 smoke_kitchen:gas_tank");
            equip(context,server,"smoke_kitchen:gas_canister");click(context,new BlockPos(4,100,0),Direction.NORTH);equip(context,server,"minecraft:air");click(context,new BlockPos(2,100,0),Direction.NORTH);context.waitTicks(45);
            server.runOnServer(game->{var tank=(KitchenBlockEntity)game.overworld().getBlockEntity(new BlockPos(4,100,0));var stove=(KitchenBlockEntity)game.overworld().getBlockEntity(new BlockPos(2,100,0));check(tank.fuelSeconds>0&&tank.fuelSeconds<600&&stove.getFuelUnits()>0,"Filling a tank through real client interaction must feed the stove through a pipe");});
            context.takeScreenshot("kitchen-working-gas-pipe-0.1.4");
        }
    }
    private static final class ToolSheet extends Screen {
        private final List<String> ids=List.of("stone_knife","iron_knife","refined_knife","wooden_spatula","iron_spatula","ladle","tongs","chopsticks","rolling_pin","heat_cloth","sieve","thermometer","gas_canister","partial_gas_canister","empty_gas_canister","used_iron_knife","broken_iron_knife","used_salt");
        ToolSheet(){super(Component.literal("Smoke Kitchen 0.1.4"));}
        @Override public void extractRenderState(GuiGraphicsExtractor g,int x,int y,float delta){
            g.fill(0,0,width,height,0xff262e35);g.centeredText(font,"SMOKE KITCHEN 0.1.4 | vanilla tools and container states",width/2,8,0xffffffff);
            int columns=6,pitch=(width-12)/columns;
            for(int i=0;i<ids.size();i++){
                int px=6+(i%columns)*pitch,py=27+(i/columns)*70;var stack=new ItemStack(KitchenContent.item(ids.get(i)));
                if(ids.get(i).startsWith("used_"))stack.setDamageValue(Math.max(1,stack.getMaxDamage()/2));
                g.fill(px,py,px+pitch-4,py+65,0xffaaa9a6);g.pose().pushMatrix();g.pose().translate(px+(pitch-44)/2f,py+4).scale(2.5f);g.item(stack,0,0);g.itemDecorations(font,stack,0,0);g.pose().popMatrix();
                var lines=font.split(stack.getHoverName(),pitch-8);int lineY=py+47;for(var line:lines){g.text(font,line,px+3,lineY,0xff151c22,false);lineY+=9;}
            }
        }
    }
}
