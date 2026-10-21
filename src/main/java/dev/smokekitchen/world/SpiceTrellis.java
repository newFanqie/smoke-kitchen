package dev.smokekitchen.world;

import dev.smokekitchen.content.KitchenContent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** The frame supports a separate, soil-rooted vanilla-derived crop, never an inventory seed. */
public final class SpiceTrellis {
    private SpiceTrellis() {}
    public static BlockPos plot(BlockPos frame,net.minecraft.world.level.block.state.BlockState state){return frame.relative(state.getValue(KitchenBlock.FACING));}
    public static boolean supports(LevelReader level,BlockPos crop){
        for(var face:Direction.Plane.HORIZONTAL){var at=crop.relative(face);var state=level.getBlockState(at);if(state.getBlock()==KitchenContent.BLOCKS.get("spice_trellis")&&plot(at,state).equals(crop))return true;}
        return false;
    }
    public static InteractionResult plant(Player player,InteractionHand hand,Level level,BlockPos frame){
        if(!player.mayBuild())return InteractionResult.PASS;
        var stack=player.getItemInHand(hand);
        if(!stack.is(KitchenContent.item("black_pepper_seeds"))&&!stack.is(KitchenContent.item("sichuan_pepper_seeds"))){
            if(!level.isClientSide())player.sendOverlayMessage(Component.literal("种植架：在朝向的一侧准备耕地，持胡椒或花椒种子右键；空手可回收旧架库存。"));
            return InteractionResult.SUCCESS;
        }
        var target=plot(frame,level.getBlockState(frame));
        if(!player.isWithinBlockInteractionRange(target,0)||!player.mayUseItemAt(target,Direction.UP,stack)||!level.mayInteract(player,target))return InteractionResult.FAIL;
        if(!(level.getBlockState(target.below()).getBlock() instanceof net.minecraft.world.level.block.FarmlandBlock)||!level.isEmptyBlock(target)){
            if(!level.isClientSide())player.sendOverlayMessage(Component.literal("架子前方需要空出的种植位置，下方必须是真实耕地。"));
            return InteractionResult.SUCCESS;
        }
        // BlockItem retains the normal placement, permissions, seed consumption and advancement path.
        return stack.useOn(new UseOnContext(player,hand,new BlockHitResult(Vec3.atCenterOf(target.below()).add(0,.5,0),Direction.UP,target.below(),false)));
    }
}
