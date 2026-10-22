package dev.smokekitchen;

import dev.smokekitchen.content.KitchenContent;
import dev.smokekitchen.content.ContentCatalog;
import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SmokeKitchen implements ModInitializer {
    public static final String ID = "smoke_kitchen";
    public static final Logger LOGGER = LoggerFactory.getLogger(ID);
    public static Identifier id(String path) { return Identifier.fromNamespaceAndPath(ID, path); }
    @Override public void onInitialize() {
        KitchenConfig.load();
        ContentCatalog.load();
        KitchenContent.register();
        dev.smokekitchen.content.CuredCooking.register();
        dev.smokekitchen.entity.KitchenEntities.register();
        dev.smokekitchen.world.KitchenWorld.register();
        KitchenContent.registerCreativeTab();
        dev.smokekitchen.world.KitchenInteractions.register();
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_LEVEL_TICK.register(level->dev.smokekitchen.world.DryWeatherHistory.get(level));
        LOGGER.info("Smoke Kitchen: {} items, {} stations, {} recipes", KitchenContent.ITEMS.size(), KitchenContent.BLOCKS.size(), ContentCatalog.DISHES.size());
    }
}
