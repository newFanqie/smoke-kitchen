package dev.smokekitchen.world;

import dev.smokekitchen.SmokeKitchen;
import dev.smokekitchen.content.KitchenContent;
import net.fabricmc.fabric.api.biome.v1.*;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import java.util.List;

public final class KitchenWorld {
    public static Block SALT_ORE;
    public static final List<String> BUILDINGS=List.of("village_kitchen","farmstead","spice_stall","mushroom_cabin","fishing_hut","saltworks","old_inn","abandoned_granary","raider_camp");
    public static void register() {
        net.fabricmc.fabric.api.loot.v3.LootTableEvents.MODIFY.register((key,table,source,registries)->{
            if(source.isBuiltin()&&key.identifier().equals(net.minecraft.resources.Identifier.withDefaultNamespace("entities/pig")))table.withPool(net.minecraft.world.level.storage.loot.LootPool.lootPool()
                .add(net.minecraft.world.level.storage.loot.entries.LootItem.lootTableItem(KitchenContent.item("rendered_fat"))));
        });
        var blockKey=ResourceKey.create(Registries.BLOCK,SmokeKitchen.id("salt_ore"));
        SALT_ORE=Registry.register(BuiltInRegistries.BLOCK,blockKey,new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.COAL_ORE).setId(blockKey)));
        var itemKey=ResourceKey.create(Registries.ITEM,SmokeKitchen.id("salt_ore"));
        KitchenContent.ITEMS.put("salt_ore",Registry.register(BuiltInRegistries.ITEM,itemKey,new BlockItem(SALT_ORE,new Item.Properties().setId(itemKey).useBlockDescriptionPrefix())));
        var ore=ResourceKey.create(Registries.PLACED_FEATURE,SmokeKitchen.id("salt_ore"));
        BiomeModifications.addFeature(BiomeSelectors.foundInOverworld(),GenerationStep.Decoration.UNDERGROUND_ORES,ore);
        BiomeModifications.addFeature(BiomeSelectors.foundInOverworld(),GenerationStep.Decoration.UNDERGROUND_ORES,ResourceKey.create(Registries.PLACED_FEATURE,SmokeKitchen.id("gas_deposit")));
        for(String name:BUILDINGS) {
            Registry.register(BuiltInRegistries.FEATURE,SmokeKitchen.id(name),new KitchenBuildingFeature(name));
            ResourceKey<PlacedFeature> key=ResourceKey.create(Registries.PLACED_FEATURE,SmokeKitchen.id(name));
            BiomeModifications.addFeature(BiomeSelectors.foundInOverworld(),GenerationStep.Decoration.SURFACE_STRUCTURES,key);
        }
        PlayerBlockBreakEvents.AFTER.register((world,player,pos,state,entity)->{
            if(player.isCreative())return;
            if((state.is(Blocks.SHORT_GRASS)||state.is(Blocks.FERN)||state.is(Blocks.TALL_GRASS)) && world.getRandom().nextInt(8)==0) {
                var names=KitchenContent.CROPS.keySet().toArray(String[]::new);
                Block.popResource(world,pos,new ItemStack(KitchenContent.item(names[world.getRandom().nextInt(names.length)]+"_seeds")));
            }
        });
    }
    private KitchenWorld() {}
}
