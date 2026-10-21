package dev.smokekitchen.world;

import dev.smokekitchen.content.*;
import net.minecraft.core.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Shared six-cell layout for interaction, rendering and hopper admission. */
public final class SpiceRack {
    public static boolean accepts(ItemStack stack){
        var id=ResourceStates.baseId(stack);var d=KitchenContent.DEFINITIONS.get(id);
        return d!=null&&d.has("seasoning")||id.equals("empty_jar")||stack.is(Items.GLASS_BOTTLE)||stack.is(Items.HONEY_BOTTLE);
    }
    public static int slot(BlockPos pos,BlockState state,BlockHitResult hit){
        double x=hit.getLocation().x-pos.getX(),z=hit.getLocation().z-pos.getZ();
        double across=switch(state.getValue(KitchenBlock.FACING)){case EAST->z;case SOUTH->1-x;case WEST->1-z;default->x;};
        return Math.clamp((int)(across*3),0,2)+(hit.getLocation().y-pos.getY()>=9/16.0?3:0);
    }
    public static float[] position(int slot,Direction facing){
        float x=(3+5*(slot%3))/16f,z=.55f;
        return switch(facing){case EAST->new float[]{1-z,x};case SOUTH->new float[]{1-x,1-z};case WEST->new float[]{z,1-x};default->new float[]{x,z};};
    }
    public static String material(ItemStack stack){
        var id=ResourceStates.baseId(stack);var d=KitchenContent.DEFINITIONS.get(id);
        if(id.equals("empty_jar")||stack.is(Items.GLASS_BOTTLE))return "empty";
        if(stack.is(Items.HONEY_BOTTLE))return "honey";
        return d!=null&&d.has("seasoning")?ContentCatalog.str(d,"seasoning"):"empty";
    }
    private SpiceRack(){}
}
