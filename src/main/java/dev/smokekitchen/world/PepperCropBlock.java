package dev.smokekitchen.world;

import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;

/** Keeps vanilla crop aging/loot/bonemeal, adding only the visible direction of its support. */
public final class PepperCropBlock extends KitchenCropBlock {
    public static final EnumProperty<Direction> FACING=BlockStateProperties.HORIZONTAL_FACING;
    public PepperCropBlock(Properties props){super(props,"black_pepper");registerDefaultState(defaultBlockState().setValue(FACING,Direction.NORTH));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){super.createBlockStateDefinition(b);b.add(FACING);}
    private static BlockState oriented(BlockState state,LevelReader world,BlockPos pos){
        if(state.getBlock() instanceof PepperCropBlock&&!state.getValue(WILD))for(var side:Direction.Plane.HORIZONTAL){var at=pos.relative(side);var frame=world.getBlockState(at);if(frame.getBlock()==dev.smokekitchen.content.KitchenContent.BLOCKS.get("spice_trellis")&&SpiceTrellis.plot(at,frame).equals(pos))return state.setValue(FACING,side);}
        return state;
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext c){var state=super.getStateForPlacement(c);return state==null?null:oriented(state,c.getLevel(),c.getClickedPos());}
    @Override protected BlockState updateShape(BlockState state,LevelReader world,ScheduledTickAccess ticks,BlockPos pos,Direction side,BlockPos next,BlockState neighbor,RandomSource random){return oriented(super.updateShape(state,world,ticks,pos,side,next,neighbor,random),world,pos);}
    private static void orientAfterGrowth(Level world,BlockPos pos){var state=world.getBlockState(pos);var next=oriented(state,world,pos);if(next!=state)world.setBlock(pos,next,Block.UPDATE_CLIENTS);}
    @Override public void growCrops(Level world,BlockPos pos,BlockState state){super.growCrops(world,pos,state);orientAfterGrowth(world,pos);}
    @Override protected void randomTick(BlockState state,ServerLevel world,BlockPos pos,RandomSource random){super.randomTick(state,world,pos,random);orientAfterGrowth(world,pos);}
}
