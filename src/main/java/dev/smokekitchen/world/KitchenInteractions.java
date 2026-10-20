package dev.smokekitchen.world;

import dev.smokekitchen.content.KitchenContent;
import dev.smokekitchen.cooking.CookingBatch;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.phys.BlockHitResult;

public final class KitchenInteractions {
    public static void register() {
        UseItemCallback.EVENT.register((player,world,hand)->{
            if(hand!=InteractionHand.MAIN_HAND)return InteractionResult.PASS;
            ItemStack held=player.getItemInHand(hand);String name=KitchenContent.name(held.getItem());
            if(name.equals("recipe_book")) { if(world.isClientSide())return InteractionResult.PASS;RecipeGuide.show(player);return InteractionResult.SUCCESS; }
            var hit=player.pick(5,0,true);
            if(!(hit instanceof BlockHitResult blockHit))return InteractionResult.PASS;
            var pos=blockHit.getBlockPos();
            if(name.equals("heat_cloth") && player.isShiftKeyDown() && world.getBlockEntity(pos) instanceof KitchenBlockEntity pot && pot.kind().equals("cook")) {
                if(!world.isClientSide()) {
                    ItemStack result=new ItemStack(pot.getBlockState().getBlock());
                    result.set(DataComponents.BLOCK_ENTITY_DATA,net.minecraft.world.item.component.TypedEntityData.of(KitchenContent.STATION_ENTITY,pot.saveWithFullMetadata(world.registryAccess())));
                    pot.items.clear();pot.batch=new CookingBatch();pot.attachedLid="";
                    world.removeBlock(pos,false);
                    if(!player.getInventory().add(result))player.drop(result,false);
                }
                return InteractionResult.SUCCESS;
            }
            if(!world.getFluidState(pos).is(FluidTags.WATER))return InteractionResult.PASS;
            String output=name.equals("empty_water_jug")?"water_jug":held.is(Items.WATER_BUCKET) && player.isShiftKeyDown() && world.getBiome(pos).is(BiomeTags.IS_OCEAN)?"saltwater_bucket":"";
            if(output.isEmpty())return InteractionResult.PASS;
            if(!world.isClientSide() && !player.isCreative()) {
                held.shrink(1);ItemStack result=new ItemStack(KitchenContent.item(output));
                if(!player.getInventory().add(result))player.drop(result,false);
            }
            return InteractionResult.SUCCESS;
        });
    }
    private KitchenInteractions() {}
}
