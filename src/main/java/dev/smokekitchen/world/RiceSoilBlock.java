package dev.smokekitchen.world;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.material.*;

/** Vanilla farmland and bucket/waterlogging behavior, with a shallow retained water layer. */
public final class RiceSoilBlock extends FarmlandBlock implements SimpleWaterloggedBlock {
    public static final MapCodec<FarmlandBlock> CODEC=simpleCodec(RiceSoilBlock::new);
    public static final BooleanProperty WATERLOGGED=BlockStateProperties.WATERLOGGED;
    public RiceSoilBlock(Properties props){super(props);registerDefaultState(defaultBlockState().setValue(MOISTURE,7).setValue(WATERLOGGED,false));}
    @Override public MapCodec<FarmlandBlock> codec(){return CODEC;}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){super.createBlockStateDefinition(b);b.add(WATERLOGGED);}
    @Override protected FluidState getFluidState(BlockState s){return s.getValue(WATERLOGGED)?Fluids.WATER.getSource(false):super.getFluidState(s);}
    @Override protected net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState s,BlockGetter world,BlockPos pos,net.minecraft.world.phys.shapes.CollisionContext context){return Block.box(0,0,0,16,13,16);}
    @Override protected BlockState updateShape(BlockState s,LevelReader world,ScheduledTickAccess ticks,BlockPos pos,Direction side,BlockPos next,BlockState neighbor,RandomSource random){
        if(s.getValue(WATERLOGGED))ticks.scheduleTick(pos,Fluids.WATER,Fluids.WATER.getTickDelay(world));
        return super.updateShape(s,world,ticks,pos,side,next,neighbor,random);
    }
}
