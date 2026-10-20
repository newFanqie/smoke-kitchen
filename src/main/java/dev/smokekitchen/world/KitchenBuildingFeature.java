package dev.smokekitchen.world;

import dev.smokekitchen.content.KitchenContent;
import dev.smokekitchen.entity.KitchenEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.levelgen.feature.*;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import java.util.List;

/** Compact, bounded generation: refuses water, steep terrain and occupied footprints. */
public final class KitchenBuildingFeature extends Feature<NoneFeatureConfiguration> {
    private final String variant;
    public KitchenBuildingFeature(String variant) { super(NoneFeatureConfiguration.CODEC); this.variant=variant; }
    @Override public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        var world=context.level(); var origin=context.origin(); var random=context.random();
        if(!world.getBlockState(origin.below()).isSolid() || !world.getFluidState(origin).isEmpty())return false;
        for(int x=-1;x<=7;x++)for(int z=-1;z<=9;z++) {
            BlockPos p=origin.offset(x,0,z);
            if(!world.getFluidState(p).isEmpty() || !world.getBlockState(p.below(2)).isSolid())return false;
            if(!world.getBlockState(p).canBeReplaced() && !world.getBlockState(p).is(Blocks.SNOW))return false;
        }
        Block wall=variant.contains("salt")?Blocks.SANDSTONE:variant.contains("mushroom")?Blocks.SPRUCE_PLANKS:Blocks.OAK_PLANKS;
        for(int x=0;x<7;x++)for(int z=0;z<9;z++) {
            put(world,origin.offset(x,-1,z),Blocks.COBBLESTONE);
            put(world,origin.offset(x,0,z),Blocks.OAK_PLANKS);
            for(int y=1;y<=4;y++) {
                boolean edge=x==0||x==6||z==0||z==8;
                boolean door=(x==3&&z==0&&y<=2);
                put(world,origin.offset(x,y,z),edge&&!door?wall:Blocks.AIR);
            }
            put(world,origin.offset(x,5,z),variant.equals("farmstead")&&x>0&&x<6&&z>0&&z<4?Blocks.AIR:Blocks.SPRUCE_SLAB);
        }
        for(int x:new int[]{0,6})for(int z:new int[]{0,8})for(int y=1;y<=4;y++)put(world,origin.offset(x,y,z),Blocks.OAK_LOG);
        put(world,origin.offset(0,2,4),Blocks.GLASS_PANE);put(world,origin.offset(6,2,4),Blocks.GLASS_PANE);
        put(world,origin.offset(3,4,4),Blocks.LANTERN);
        placeStation(world,origin.offset(1,1,6),"brick_stove");placeStation(world,origin.offset(1,2,6),"clay_pot");
        placeStation(world,origin.offset(2,1,6),"counter");placeStation(world,origin.offset(2,2,6),"cutting_board");
        placeStation(world,origin.offset(4,1,6),"dining_table");placeStation(world,origin.offset(4,1,4),"chair");
        String special=switch(variant){case "spice_stall"->"spice_rack";case "saltworks"->"salt_pan";case "mushroom_cabin"->"drying_rack";case "old_inn"->"oven";case "abandoned_granary","raider_camp"->"grain_bin";default->"cabinet";};
        placeStation(world,origin.offset(5,1,2),special);
        put(world,origin.offset(1,1,2),Blocks.CHEST);
        if(world.getBlockEntity(origin.offset(1,1,2)) instanceof ChestBlockEntity chest) {
            var seeds=KitchenContent.CROPS.keySet().toArray(String[]::new);
            chest.setItem(0,new ItemStack(KitchenContent.item("recipe_book")));
            chest.setItem(1,new ItemStack(KitchenContent.item("stone_knife")));
            chest.setItem(2,new ItemStack(KitchenContent.item("salt")));
            for(int i=0;i<4;i++)chest.setItem(3+i,new ItemStack(KitchenContent.item(seeds[random.nextInt(seeds.length)]+"_seeds"),2));
            chest.setItem(7,new ItemStack(KitchenContent.item("rapeseed_oil")));
        }
        if(variant.equals("farmstead")) {
            var names=KitchenContent.CROPS.keySet().toArray(String[]::new);
            for(int x=1;x<6;x++)for(int z=1;z<4;z++) {
                BlockPos p=origin.offset(x,0,z);
                if(x==3){world.setBlock(p,Blocks.WATER.defaultBlockState(),2);continue;}
                world.setBlock(p,Blocks.FARMLAND.defaultBlockState().setValue(FarmlandBlock.MOISTURE,7),2);
                String crop=x<=2?"rice_paddy":names[((x-4)*3+z)%names.length];
                world.setBlock(p.above(),KitchenContent.CROPS.get(crop).getStateForAge(7),2);
            }
        }
        if(variant.equals("abandoned_granary")||variant.equals("raider_camp")) {
            var type=variant.equals("raider_camp")?KitchenEntities.GRAIN_RAIDER:KitchenEntities.CELLAR_SILVERFISH;
            var mob=type.create(world.getLevel(),EntitySpawnReason.STRUCTURE);
            if(mob!=null){mob.snapTo(origin.getX()+3.5,origin.getY()+1,origin.getZ()+4.5,0,0);world.addFreshEntity(mob);}
        }
        return true;
    }
    private void placeStation(WorldGenLevel world,BlockPos pos,String id) { world.setBlock(pos,KitchenContent.BLOCKS.get(id).defaultBlockState(),2); }
    private void put(WorldGenLevel world,BlockPos pos,Block block) { world.setBlock(pos,block.defaultBlockState(),2); }
}
