package dev.smokekitchen.world;

import dev.smokekitchen.content.KitchenContent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import java.util.*;

/** Native crop blocks on natural ground; player-sown seeds always remain cultivated crops. */
public final class KitchenWildCrops extends Feature<NoneFeatureConfiguration> {
    public record Habitat(String description,List<ResourceKey<Biome>> biomes) {
        public boolean matches(Holder<Biome> biome) { return biomes.stream().anyMatch(biome::is); }
    }
    public static final Map<String,Habitat> HABITATS=new LinkedHashMap<>();
    static {
        add("河流、沼泽岸边，根旁两格内有水",List.of(Biomes.RIVER,Biomes.SWAMP,Biomes.MANGROVE_SWAMP),"rice_paddy");
        add("平原与向日葵平原",List.of(Biomes.PLAINS,Biomes.SUNFLOWER_PLAINS),"tomato","soybean","rapeseed","cucumber","cabbage","garlic");
        add("平原、河岸与沼泽",List.of(Biomes.PLAINS,Biomes.RIVER,Biomes.SWAMP),"scallion");
        add("热带草原与稀疏丛林",List.of(Biomes.SAVANNA,Biomes.SAVANNA_PLATEAU,Biomes.SPARSE_JUNGLE),"chili","eggplant");
        add("热带草原、疏林干地",List.of(Biomes.SAVANNA,Biomes.SAVANNA_PLATEAU),"sesame","onion");
        add("热带草原、沙漠边缘",List.of(Biomes.SAVANNA,Biomes.DESERT),"cumin");
        add("丛林与竹林",List.of(Biomes.JUNGLE,Biomes.BAMBOO_JUNGLE,Biomes.SPARSE_JUNGLE),"ginger","black_pepper");
        add("山地草甸、风袭森林",List.of(Biomes.MEADOW,Biomes.WINDSWEPT_FOREST,Biomes.WINDSWEPT_HILLS),"sichuan_pepper");
        add("阴湿的针叶林、黑森林与蘑菇岛",List.of(Biomes.OLD_GROWTH_PINE_TAIGA,Biomes.OLD_GROWTH_SPRUCE_TAIGA,Biomes.DARK_FOREST,Biomes.MUSHROOM_FIELDS),"shiitake");
    }
    private static void add(String description,List<ResourceKey<Biome>> biomes,String... crops) { for(String crop:crops)HABITATS.put(crop,new Habitat(description,biomes)); }
    public static List<String> seedsFor(Holder<Biome> biome) { return HABITATS.entrySet().stream().filter(e->e.getValue().matches(biome)).map(Map.Entry::getKey).toList(); }
    private final String crop;
    public KitchenWildCrops(String crop) { super(NoneFeatureConfiguration.CODEC);this.crop=crop; }
    @Override public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        var world=context.level();var random=context.random();var block=KitchenContent.CROPS.get(crop);int placed=0;
        for(int attempt=0;attempt<(crop.equals("rice_paddy")?100:40)&&placed<5;attempt++) {
            int x=context.origin().getX()+random.nextInt(25)-12,z=context.origin().getZ()+random.nextInt(25)-12;
            int top=world.getHeight(Heightmap.Types.WORLD_SURFACE_WG,x,z);
            for(int depth=0;depth<33;depth++) {
                var pos=new BlockPos(x,top-depth,z);
                if(!block.wildSoil(world.getBlockState(pos.below())))continue;
                var existing=world.getBlockState(pos);
                boolean replaceable=existing.isAir()||existing.is(net.minecraft.world.level.block.Blocks.SHORT_GRASS)||existing.is(net.minecraft.world.level.block.Blocks.FERN);
                // FEATURES runs before chunk lighting, and WG heightmaps do not reliably
                // include trees placed earlier in this stage. Inspect real canopy blocks.
                boolean shaded=false;
                for(int up=1;up<=32;up++){var above=world.getBlockState(pos.above(up));if(above.is(net.minecraft.tags.BlockTags.LEAVES)||above.isSolidRender()){shaded=true;break;}}
                boolean inHabitat=HABITATS.get(crop).matches(world.getBiome(pos));
                boolean conditions=crop.equals("rice_paddy")?KitchenCropBlock.nearWater(world,pos):crop.equals("shiitake")?inHabitat:true;
                // River-origin patches may extend a few blocks onto the adjoining bank biome.
                if(replaceable&&shaded==crop.equals("shiitake")&&(crop.equals("rice_paddy")||inHabitat)&&conditions) {
                    world.setBlock(pos,block.getStateForAge(7).setValue(KitchenCropBlock.WILD,true),2);placed++;
                }
                break;
            }
        }
        return placed>0;
    }
}
