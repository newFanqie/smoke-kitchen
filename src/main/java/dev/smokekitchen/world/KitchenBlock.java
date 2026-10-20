package dev.smokekitchen.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;
import java.util.Map;

public class KitchenBlock extends Block implements EntityBlock {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final IntegerProperty HEAT = IntegerProperty.create("heat",0,3);
    public static final BooleanProperty LID = BooleanProperty.create("lid");
    public static final BooleanProperty FUELED = BooleanProperty.create("fueled");
    public static final EnumProperty<Contents> CONTENTS = EnumProperty.create("contents",Contents.class);
    public enum Contents implements net.minecraft.util.StringRepresentable {
        EMPTY, RAW, COOKED, BURNT;
        @Override public String getSerializedName() { return name().toLowerCase(java.util.Locale.ROOT); }
    }
    public final String id;
    public final String kind;
    public KitchenBlock(Properties props, String id, String kind) {
        super(props); this.id = id; this.kind = kind;
        registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH).setValue(HEAT,0).setValue(LID,false).setValue(FUELED,false).setValue(CONTENTS,Contents.EMPTY));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder) { builder.add(FACING,HEAT,LID,FUELED,CONTENTS); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) { return defaultBlockState().setValue(FACING,context.getHorizontalDirection().getOpposite()); }
    @Override protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if(hit.getDirection()==Direction.UP && stack.getItem() instanceof net.minecraft.world.item.BlockItem placed && placed.getBlock() instanceof KitchenBlock block
                && java.util.Set.of("cook","board","plate").contains(block.kind) && kind.equals("storage")) return InteractionResult.PASS;
        if(stack.getItem() instanceof net.minecraft.world.item.BlockItem placed && (!(placed.getBlock() instanceof KitchenBlock block)||!block.kind.equals("cook")))return InteractionResult.PASS;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        return level.getBlockEntity(pos) instanceof KitchenBlockEntity station ? station.interact(player, stack, hit) : InteractionResult.PASS;
    }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        return level.getBlockEntity(pos) instanceof KitchenBlockEntity station ? station.interact(player, ItemStack.EMPTY, hit) : InteractionResult.PASS;
    }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new KitchenBlockEntity(pos,state); }
    @Override public void setPlacedBy(Level level,BlockPos pos,BlockState state,net.minecraft.world.entity.LivingEntity entity,ItemStack stack) {
        super.setPlacedBy(level,pos,state,entity,stack);
        if(level.getBlockEntity(pos) instanceof KitchenBlockEntity pot && !pot.attachedLid.isEmpty()) level.setBlock(pos,state.setValue(LID,true),Block.UPDATE_ALL);
    }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) return null;
        return (world,pos,s,entity) -> { if (entity instanceof KitchenBlockEntity station) station.serverTick(); };
    }
    @Override protected VoxelShape getShape(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos, CollisionContext context) {
        if(id.equals("oven"))return Shapes.block();
        if(kind.equals("stove")||kind.equals("gas_stove"))return Block.box(0,0,0,16,level.getBlockEntity(pos) instanceof KitchenBlockEntity station && !station.mountedCookware.isEmpty()?18:14,16);
        return switch (kind) {
            case "stove","gas_stove" -> Block.box(0,0,0,16,14,16);
            case "gas_pipe" -> Block.box(6,0,6,10,16,10);
            case "board" -> Block.box(0,0,0,16,2,16);
            case "cook" -> Block.box(1,0,1,15,7,15);
            case "plate" -> Block.box(1,0,1,15,2,15);
            case "rack" -> Block.box(0,0,2,16,15,14);
            default -> Shapes.block();
        };
    }
    @Override public void animateTick(BlockState state,Level level,BlockPos pos,net.minecraft.util.RandomSource random) {
        if(state.getValue(HEAT)==0 || !(kind.equals("stove")||kind.equals("gas_stove")))return;
        var face=state.getValue(FACING);
        double x=pos.getX()+.5+face.getStepX()*.51,z=pos.getZ()+.5+face.getStepZ()*.51;
        if(kind.equals("stove")&&random.nextInt(3)==0){
            level.addParticle(net.minecraft.core.particles.ParticleTypes.SMALL_FLAME,x,pos.getY()+.35,z,0,.01,0);
            level.addParticle(net.minecraft.core.particles.ParticleTypes.SMOKE,x,pos.getY()+.5,z,0,.015,0);
        }
        if(random.nextInt(20)==0)level.playLocalSound(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,net.minecraft.sounds.SoundEvents.FURNACE_FIRE_CRACKLE,net.minecraft.sounds.SoundSource.BLOCKS,.35f,.9f+random.nextFloat()*.2f,false);
    }
    @Override protected boolean hasAnalogOutputSignal(BlockState state) {return true;}
    @Override protected int getAnalogOutputSignal(BlockState state,Level level,BlockPos pos,Direction direction) {
        if(!(level.getBlockEntity(pos) instanceof KitchenBlockEntity station))return 0;
        if(kind.equals("stove"))return Math.min(15,station.fuelSeconds/240);
        if(kind.equals("cook"))return station.batch.burnt()?15:station.batch.ready()?12:Math.min(10,station.batch.ingredients.size()*2);
        return Math.min(15,station.items.stream().mapToInt(ItemStack::getCount).sum()/4);
    }
}
