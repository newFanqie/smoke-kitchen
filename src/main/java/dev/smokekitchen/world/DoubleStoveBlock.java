package dev.smokekitchen.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/** Each half has its own block entity, fuel and heat setting. */
public final class DoubleStoveBlock extends KitchenBlock {
    public static final BooleanProperty SECONDARY=BooleanProperty.create("secondary");
    public DoubleStoveBlock(Properties props) {super(props,"double_stove","stove");registerDefaultState(defaultBlockState().setValue(SECONDARY,false));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder) {super.createBlockStateDefinition(builder);builder.add(SECONDARY);}
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing=context.getHorizontalDirection().getOpposite();
        if(!context.getLevel().getBlockState(context.getClickedPos().relative(facing.getClockWise())).canBeReplaced())return null;
        return defaultBlockState().setValue(FACING,facing);
    }
    @Override public void setPlacedBy(Level level,BlockPos pos,BlockState state,LivingEntity entity,ItemStack stack) {
        super.setPlacedBy(level,pos,state,entity,stack);
        if(!state.getValue(SECONDARY))level.setBlock(pos.relative(state.getValue(FACING).getClockWise()),state.setValue(SECONDARY,true),Block.UPDATE_ALL);
    }
    @Override protected void affectNeighborsAfterRemoval(BlockState state,ServerLevel level,BlockPos pos,boolean moved) {
        super.affectNeighborsAfterRemoval(state,level,pos,moved);
        Direction offset=state.getValue(FACING).getClockWise();
        var other=pos.relative(state.getValue(SECONDARY)?offset.getOpposite():offset);
        if(level.getBlockState(other).is(this)) {
            if(state.getValue(SECONDARY))Block.popResource(level,pos,new ItemStack(asItem()));
            level.destroyBlock(other,false);
        }
    }
}
