package dev.smokekitchen.content;

import com.google.gson.JsonObject;
import dev.smokekitchen.SmokeKitchen;
import dev.smokekitchen.world.KitchenBlock;
import dev.smokekitchen.world.KitchenBlockEntity;
import dev.smokekitchen.world.KitchenCropBlock;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import java.util.*;

public final class KitchenContent {
    public static final Map<String, Item> ITEMS = new LinkedHashMap<>();
    public static final Map<String, Block> BLOCKS = new LinkedHashMap<>();
    public static final Map<String, KitchenCropBlock> CROPS = new LinkedHashMap<>();
    public static final Map<String, JsonObject> DEFINITIONS = new HashMap<>();
    public static BlockEntityType<KitchenBlockEntity> STATION_ENTITY;
    public static void register() {
        for (var e : ContentCatalog.DATA.getAsJsonArray("items")) {
            var d = e.getAsJsonObject(); String name = ContentCatalog.str(d,"id");
            var props = new Item.Properties().setId(ResourceKey.create(Registries.ITEM, SmokeKitchen.id(name)));
            int nutrition = ContentCatalog.num(d,"nutrition",0);
            if (nutrition > 0) props.food(new FoodProperties.Builder().nutrition(nutrition).saturationModifier(.5f).build());
            if (ContentCatalog.flag(d,"meal")) props.usingConvertsTo(Items.BOWL);
            if(d.has("container"))props.usingConvertsTo(item(ContentCatalog.str(d,"container")));
            int uses = ContentCatalog.num(d,"uses",0);
            if (uses > 0) props.durability(uses);
            else if (ContentCatalog.flag(d,"unstackable")) props.stacksTo(1);
            ITEMS.put(name, Registry.register(BuiltInRegistries.ITEM, SmokeKitchen.id(name), nutrition>0?new KitchenFoodItem(props,nutrition):new KitchenStateItem(props)));
            DEFINITIONS.put(name,d);
        }
        for (var e : ContentCatalog.DATA.getAsJsonArray("stations")) {
            var d = e.getAsJsonObject(); String name = ContentCatalog.str(d,"id");
            var props = BlockBehaviour.Properties.of().strength(2.5f).noOcclusion().sound(SoundType.WOOD)
                    .setId(ResourceKey.create(Registries.BLOCK, SmokeKitchen.id(name)));
            if(ContentCatalog.str(d,"kind").equals("stove") || name.equals("gas_stove"))props.dynamicShape().lightLevel(state->state.getValue(KitchenBlock.HEAT)>0?12:0).sound(SoundType.STONE);
            if(name.equals("double_stove"))props.pushReaction(net.minecraft.world.level.material.PushReaction.BLOCK);
            Block block = Registry.register(BuiltInRegistries.BLOCK, SmokeKitchen.id(name), name.equals("double_stove")?new dev.smokekitchen.world.DoubleStoveBlock(props):new KitchenBlock(props, name, ContentCatalog.str(d,"kind")));
            BLOCKS.put(name,block); DEFINITIONS.put(name,d);
            registerBlockItem(name,block);
        }
        STATION_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, SmokeKitchen.id("station"),
                FabricBlockEntityTypeBuilder.create(KitchenBlockEntity::new, BLOCKS.values().toArray(Block[]::new)).build());
        for (var e : ContentCatalog.DATA.getAsJsonArray("crops")) {
            var d = e.getAsJsonObject(); String name = ContentCatalog.str(d,"id");
            var props = BlockBehaviour.Properties.of().noCollision().randomTicks().instabreak().sound(SoundType.CROP)
                    .setId(ResourceKey.create(Registries.BLOCK, SmokeKitchen.id(name+"_crop")));
            var crop = Registry.register(BuiltInRegistries.BLOCK, SmokeKitchen.id(name+"_crop"), new KitchenCropBlock(props, name));
            CROPS.put(name,crop);
            registerBlockItem(name+"_seeds",crop);
        }
    }
    public static void registerCreativeTab() {
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB,SmokeKitchen.id("kitchen"),
            net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab.builder()
                .title(net.minecraft.network.chat.Component.translatable("itemGroup.smoke_kitchen"))
                .icon(()->new ItemStack(item("brick_stove")))
                .displayItems((parameters,output)->{
                    BLOCKS.values().forEach(output::accept);
                    ITEMS.entrySet().stream().filter(e->!BLOCKS.containsKey(e.getKey())).forEach(e->{
                        var stack=new ItemStack(e.getValue());var def=DEFINITIONS.get(e.getKey());
                        if(def!=null&&ContentCatalog.str(def,"state").equals("used"))stack.setDamageValue(Math.max(1,stack.getMaxDamage()/2));
                        output.accept(stack);
                    });
                }).build());
    }
    private static void registerBlockItem(String name, Block block) {
        var key = ResourceKey.create(Registries.ITEM, SmokeKitchen.id(name));
        ITEMS.put(name,Registry.register(BuiltInRegistries.ITEM,key,new BlockItem(block,name.endsWith("_seeds")?new Item.Properties().setId(key):new Item.Properties().setId(key).useBlockDescriptionPrefix())));
    }
    public static Item item(String name) { return ITEMS.getOrDefault(name, Items.AIR); }
    public static String name(Item item) { return BuiltInRegistries.ITEM.getKey(item).getPath(); }
    public static String fullName(Item item) { return BuiltInRegistries.ITEM.getKey(item).toString(); }
    private KitchenContent() {}
}
