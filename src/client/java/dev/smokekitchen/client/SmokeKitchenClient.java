package dev.smokekitchen.client;

import dev.smokekitchen.content.KitchenContent;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;

public final class SmokeKitchenClient implements ClientModInitializer {
    @Override public void onInitializeClient() {
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
}
