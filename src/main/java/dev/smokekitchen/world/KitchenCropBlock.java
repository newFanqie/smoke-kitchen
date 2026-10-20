package dev.smokekitchen.world;

import dev.smokekitchen.content.KitchenContent;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public final class KitchenCropBlock extends CropBlock {
    public final String crop;
    public KitchenCropBlock(Properties props, String crop) { super(props); this.crop = crop; }
    @Override protected ItemLike getBaseSeedId() { return KitchenContent.item(crop+"_seeds"); }
    @Override protected boolean mayPlaceOn(BlockState state, BlockGetter world, BlockPos pos) {
        if (crop.equals("shiitake")) return state.is(Blocks.FARMLAND) || state.is(Blocks.PODZOL) || state.is(Blocks.MYCELIUM);
        return super.mayPlaceOn(state,world,pos);
    }
}
