package dev.smokekitchen.world;

import dev.smokekitchen.content.ContentCatalog;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

public final class RecipeGuide {
    public static void show(Player player) {
        player.sendSystemMessage(Component.translatable("guide.smoke_kitchen.intro"));
    }
    private RecipeGuide() {}
}
