package dev.smokekitchen.client;

import dev.smokekitchen.content.KitchenContent;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;

public final class SmokeKitchenClient implements ClientModInitializer {
    @Override public void onInitializeClient() {
        net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback.EVENT.register((stack,context,flag,lines)->{
            var data=stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
            if(data.contains("kitchen_cut"))lines.add(net.minecraft.network.chat.Component.translatable("message.smoke_kitchen.cut",net.minecraft.network.chat.Component.translatable("cut.smoke_kitchen."+data.getStringOr("kitchen_cut","WHOLE"))));
        });
        net.fabricmc.fabric.api.client.screen.v1.ScreenEvents.AFTER_INIT.register((client,screen,width,height)->{
            if(firstCreativeOpen && screen instanceof net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen) {
                var tab=net.minecraft.core.registries.BuiltInRegistries.CREATIVE_MODE_TAB.getValue(dev.smokekitchen.SmokeKitchen.id("kitchen"));
                if(tab!=null && ((net.fabricmc.fabric.api.client.creativetab.v1.FabricCreativeModeInventoryScreen)screen).setSelectedTab(tab))firstCreativeOpen=false;
            }
        });
        BlockEntityRenderers.register(KitchenContent.STATION_ENTITY,KitchenStationRenderer::new);
        var entities=dev.smokekitchen.entity.KitchenEntities.class;
        net.minecraft.client.renderer.entity.EntityRenderers.register(dev.smokekitchen.entity.KitchenEntities.DUCK,net.minecraft.client.renderer.entity.ChickenRenderer::new);
        net.minecraft.client.renderer.entity.EntityRenderers.register(dev.smokekitchen.entity.KitchenEntities.GOOSE,net.minecraft.client.renderer.entity.ChickenRenderer::new);
        net.minecraft.client.renderer.entity.EntityRenderers.register(dev.smokekitchen.entity.KitchenEntities.BOAR,net.minecraft.client.renderer.entity.PigRenderer::new);
        net.minecraft.client.renderer.entity.EntityRenderers.register(dev.smokekitchen.entity.KitchenEntities.SHRIMP,net.minecraft.client.renderer.entity.CodRenderer::new);
        net.minecraft.client.renderer.entity.EntityRenderers.register(dev.smokekitchen.entity.KitchenEntities.CRAB,net.minecraft.client.renderer.entity.CodRenderer::new);
        net.minecraft.client.renderer.entity.EntityRenderers.register(dev.smokekitchen.entity.KitchenEntities.GRAIN_RAIDER,net.minecraft.client.renderer.entity.PillagerRenderer::new);
        net.minecraft.client.renderer.entity.EntityRenderers.register(dev.smokekitchen.entity.KitchenEntities.CELLAR_SILVERFISH,net.minecraft.client.renderer.entity.SilverfishRenderer::new);
        net.fabricmc.fabric.api.event.player.UseItemCallback.EVENT.register((player,world,hand)->{
            if(world.isClientSide() && player.getItemInHand(hand).is(KitchenContent.item("recipe_book"))) {
                net.minecraft.client.Minecraft.getInstance().setScreen(new RecipeBookScreen());return net.minecraft.world.InteractionResult.SUCCESS;
            }
            return net.minecraft.world.InteractionResult.PASS;
        });
        net.fabricmc.fabric.api.event.player.UseBlockCallback.EVENT.register((player,world,hand,hit)->{
            if(world.isClientSide() && player.getItemInHand(hand).is(KitchenContent.item("recipe_book"))) {
                net.minecraft.client.Minecraft.getInstance().setScreen(new RecipeBookScreen());return net.minecraft.world.InteractionResult.SUCCESS;
            }
            return net.minecraft.world.InteractionResult.PASS;
        });
    }
    private static boolean firstCreativeOpen=true;
}
