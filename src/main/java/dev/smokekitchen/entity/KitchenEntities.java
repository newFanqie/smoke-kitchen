package dev.smokekitchen.entity;

import dev.smokekitchen.SmokeKitchen;
import net.fabricmc.fabric.api.biome.v1.*;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.animal.fish.Cod;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.entity.monster.illager.Pillager;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.item.*;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.sounds.SoundEvents;
import dev.smokekitchen.content.KitchenContent;
import java.util.LinkedHashMap;
import java.util.Map;

public final class KitchenEntities {
    public static EntityType<KitchenBird> DUCK;
    public static EntityType<KitchenBird> GOOSE;
    public static EntityType<KitchenBoar> BOAR;
    public static EntityType<KitchenFish> SHRIMP;
    public static EntityType<KitchenFish> CRAB;
    public static EntityType<Pillager> GRAIN_RAIDER;
    public static EntityType<Silverfish> CELLAR_SILVERFISH;
    public static final Map<String,EntityType<?>> ALL=new LinkedHashMap<>();
    private static <T extends Entity> EntityType<T> register(String id,EntityType.Builder<T> builder) {
        var key=ResourceKey.create(Registries.ENTITY_TYPE,SmokeKitchen.id(id));
        var type=Registry.register(BuiltInRegistries.ENTITY_TYPE,key,builder.build(key)); ALL.put(id,type); return type;
    }
    public static void register() {
        DUCK=register("duck",EntityType.Builder.of(KitchenBird::new,MobCategory.CREATURE).sized(.5f,.7f));
        GOOSE=register("goose",EntityType.Builder.of(KitchenBird::new,MobCategory.CREATURE).sized(.6f,.9f));
        BOAR=register("boar",EntityType.Builder.of(KitchenBoar::new,MobCategory.CREATURE).sized(.9f,.9f));
        SHRIMP=register("river_shrimp",EntityType.Builder.of(KitchenFish::new,MobCategory.WATER_AMBIENT).sized(.25f,.15f));
        CRAB=register("river_crab",EntityType.Builder.of(KitchenFish::new,MobCategory.WATER_AMBIENT).sized(.4f,.2f));
        GRAIN_RAIDER=register("grain_raider",EntityType.Builder.of(Pillager::new,MobCategory.MONSTER).sized(.6f,1.95f));
        CELLAR_SILVERFISH=register("cellar_silverfish",EntityType.Builder.of(Silverfish::new,MobCategory.MONSTER).sized(.4f,.3f));
        FabricDefaultAttributeRegistry.register(DUCK,Chicken.createAttributes());
        FabricDefaultAttributeRegistry.register(GOOSE,Chicken.createAttributes().add(Attributes.MAX_HEALTH,8));
        FabricDefaultAttributeRegistry.register(BOAR,Pig.createAttributes().add(Attributes.ATTACK_DAMAGE,3));
        FabricDefaultAttributeRegistry.register(SHRIMP,Cod.createAttributes());
        FabricDefaultAttributeRegistry.register(CRAB,Cod.createAttributes());
        FabricDefaultAttributeRegistry.register(GRAIN_RAIDER,Pillager.createAttributes());
        FabricDefaultAttributeRegistry.register(CELLAR_SILVERFISH,Silverfish.createAttributes());
        SpawnPlacements.register(DUCK,SpawnPlacementTypes.ON_GROUND,Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,Animal::checkAnimalSpawnRules);
        SpawnPlacements.register(GOOSE,SpawnPlacementTypes.ON_GROUND,Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,Animal::checkAnimalSpawnRules);
        SpawnPlacements.register(BOAR,SpawnPlacementTypes.ON_GROUND,Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,Animal::checkAnimalSpawnRules);
        SpawnPlacements.register(SHRIMP,SpawnPlacementTypes.IN_WATER,Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,(type,world,reason,pos,random)->world.getFluidState(pos).is(FluidTags.WATER));
        SpawnPlacements.register(CRAB,SpawnPlacementTypes.IN_WATER,Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,(type,world,reason,pos,random)->world.getFluidState(pos).is(FluidTags.WATER));
        registerBucket("shrimp_bucket",SHRIMP);registerBucket("crab_bucket",CRAB);
        ALL.forEach((name,type)->{
            var key=ResourceKey.create(Registries.ITEM,SmokeKitchen.id(name+"_spawn_egg"));
            KitchenContent.ITEMS.put(name+"_spawn_egg",Registry.register(BuiltInRegistries.ITEM,key,
                    new SpawnEggItem(new Item.Properties().setId(key).spawnEgg(type))));
        });
        var wet=BiomeSelectors.includeByKey(Biomes.RIVER,Biomes.SWAMP,Biomes.MANGROVE_SWAMP);
        BiomeModifications.addSpawn(wet,MobCategory.CREATURE,DUCK,8,2,4);
        BiomeModifications.addSpawn(wet,MobCategory.CREATURE,GOOSE,4,2,3);
        BiomeModifications.addSpawn(BiomeSelectors.includeByKey(Biomes.FOREST,Biomes.DARK_FOREST),MobCategory.CREATURE,BOAR,5,1,3);
        BiomeModifications.addSpawn(BiomeSelectors.includeByKey(Biomes.RIVER),MobCategory.WATER_AMBIENT,SHRIMP,5,2,4);
        BiomeModifications.addSpawn(wet,MobCategory.WATER_AMBIENT,CRAB,3,1,2);
    }
    private static void registerBucket(String name,EntityType<? extends Mob> type) {
        var key=ResourceKey.create(Registries.ITEM,SmokeKitchen.id(name));
        KitchenContent.ITEMS.put(name,Registry.register(BuiltInRegistries.ITEM,key,new MobBucketItem(type,Fluids.WATER,SoundEvents.BUCKET_EMPTY_FISH,new Item.Properties().setId(key).stacksTo(1).craftRemainder(Items.BUCKET))));
    }
    private KitchenEntities() {}
}
