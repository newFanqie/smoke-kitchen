package dev.smokekitchen.world;

import dev.smokekitchen.content.KitchenContent;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.tags.FluidTags;

public final class KitchenCropBlock extends CropBlock {
    public static final BooleanProperty WILD=BooleanProperty.create("wild");
    public final String crop;
    public KitchenCropBlock(Properties props, String crop) { super(props); this.crop = crop; registerDefaultState(defaultBlockState().setValue(WILD,false)); }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder) { super.createBlockStateDefinition(builder);builder.add(WILD); }
    @Override protected ItemLike getBaseSeedId() { return KitchenContent.item(crop+"_seeds"); }
    @Override protected boolean mayPlaceOn(BlockState state, BlockGetter world, BlockPos pos) {
        if(crop.equals("rice_paddy")&&state.getBlock() instanceof RiceSoilBlock)return true;
        if (crop.equals("shiitake")) return state.is(Blocks.FARMLAND) || state.is(Blocks.PODZOL) || state.is(Blocks.MYCELIUM);
        return super.mayPlaceOn(state,world,pos);
    }
    public boolean wildSoil(BlockState soil) {
        return soil.is(Blocks.GRASS_BLOCK)||soil.is(Blocks.DIRT)||soil.is(Blocks.PODZOL)||soil.is(Blocks.MYCELIUM)||soil.is(Blocks.COARSE_DIRT)||soil.is(Blocks.ROOTED_DIRT)||soil.is(Blocks.MOSS_BLOCK)||crop.equals("rice_paddy")&&soil.is(Blocks.MUD)||(crop.equals("cumin")||crop.equals("rice_paddy"))&&soil.is(Blocks.SAND);
    }
    /** Water must be at the roots' height or just above it, within two blocks. */
    public static boolean nearWater(BlockGetter world,BlockPos pos) {
        for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++)for(int y=-1;y<=0;y++)
            if(world.getFluidState(pos.offset(x,y,z)).is(FluidTags.WATER))return true;
        return false;
    }
    public boolean growingConditions(LevelReader world,BlockPos pos) {
        if(crop.equals("rice_paddy"))return nearWater(world,pos);
        if(crop.equals("shiitake")) {
            var soil=world.getBlockState(pos.below());
            return !world.canSeeSky(pos)&&world.getMaxLocalRawBrightness(pos)<=12&&(soil.is(Blocks.PODZOL)||soil.is(Blocks.MYCELIUM)||nearWater(world,pos));
        }
        return true;
    }
    @Override protected boolean canSurvive(BlockState state,LevelReader world,BlockPos pos) {
        if(state.getValue(WILD))return wildSoil(world.getBlockState(pos.below()))&&(crop.equals("shiitake")?true:hasSufficientLight(world,pos)&&(!crop.equals("rice_paddy")||nearWater(world,pos)));
        // Cultivated plants pause in unsuitable conditions; removing water must not delete the seedling.
        if(crop.equals("shiitake"))return mayPlaceOn(world.getBlockState(pos.below()),world,pos.below());
        return super.canSurvive(state,world,pos);
    }
    @Override protected void randomTick(BlockState state,ServerLevel world,BlockPos pos,RandomSource random) {
        if(!growingConditions(world,pos)||state.getValue(WILD))return;
        if(crop.equals("shiitake")) { if(!isMaxAge(state)&&random.nextInt(8)==0)world.setBlock(pos,getStateForAge(getAge(state)+1),2); }
        else super.randomTick(state,world,pos,random);
    }
    @Override public boolean isValidBonemealTarget(LevelReader world,BlockPos pos,BlockState state) { return !state.getValue(WILD)&&growingConditions(world,pos)&&super.isValidBonemealTarget(world,pos,state); }
    @Override public void growCrops(Level world,BlockPos pos,BlockState state) { if(!state.getValue(WILD)&&growingConditions(world,pos))super.growCrops(world,pos,state); }
}
