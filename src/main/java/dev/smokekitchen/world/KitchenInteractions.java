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
        net.fabricmc.fabric.api.event.player.UseBlockCallback.EVENT.register((player,world,hand,hit)->{
            var stack=player.getItemInHand(hand);var pos=hit.getBlockPos();
            var soilPos=world.getBlockState(pos).getBlock() instanceof RiceSoilBlock?pos:pos.below();
            if(hand==InteractionHand.MAIN_HAND&&(stack.is(Items.BUCKET)||stack.is(Items.WATER_BUCKET))&&world.getBlockState(soilPos).getBlock() instanceof RiceSoilBlock soil&&(pos.equals(soilPos)||world.getBlockState(pos).is(KitchenContent.CROPS.get("rice_paddy")))&&player.mayBuild()&&player.mayUseItemAt(soilPos,hit.getDirection(),stack)){
                if(!world.isClientSide()){
                    var state=world.getBlockState(soilPos);
                    if(stack.is(Items.BUCKET)&&state.getValue(RiceSoilBlock.WATERLOGGED)){
                        var filled=soil.pickupBlock(player,world,soilPos,state);
                        player.setItemInHand(hand,net.minecraft.world.item.ItemUtils.createFilledResult(stack,player,filled));
                        world.playSound(null,soilPos,net.minecraft.sounds.SoundEvents.BUCKET_FILL,net.minecraft.sounds.SoundSource.BLOCKS,1,1);
                    }else if(stack.is(Items.WATER_BUCKET)&&!state.getValue(RiceSoilBlock.WATERLOGGED)&&soil.placeLiquid(world,soilPos,state,net.minecraft.world.level.material.Fluids.WATER.getSource(false))){
                        player.setItemInHand(hand,net.minecraft.world.item.ItemUtils.createFilledResult(stack,player,new ItemStack(Items.BUCKET)));
                        world.playSound(null,soilPos,net.minecraft.sounds.SoundEvents.BUCKET_EMPTY,net.minecraft.sounds.SoundSource.BLOCKS,1,1);
                    }
                }
                return InteractionResult.SUCCESS;
            }
            if(hand!=InteractionHand.MAIN_HAND||!stack.is(Items.WATER_BUCKET)||hit.getDirection()!=net.minecraft.core.Direction.UP||!world.getBlockState(pos).is(net.minecraft.world.level.block.Blocks.FARMLAND)||!player.mayBuild()||!player.mayUseItemAt(pos,hit.getDirection(),stack))return InteractionResult.PASS;
            var above=world.getBlockState(pos.above());
            if(!above.isAir()&&!above.is(KitchenContent.CROPS.get("rice_paddy")))return InteractionResult.PASS;
            if(!world.isClientSide()){
                world.setBlock(pos,KitchenContent.BLOCKS.get("rice_soil").defaultBlockState().setValue(RiceSoilBlock.WATERLOGGED,true),net.minecraft.world.level.block.Block.UPDATE_ALL);
                if(!player.isCreative())player.setItemInHand(hand,new ItemStack(Items.BUCKET));
                world.playSound(null,pos,net.minecraft.sounds.SoundEvents.BUCKET_EMPTY,net.minecraft.sounds.SoundSource.BLOCKS,1,1);
                player.sendOverlayMessage(net.minecraft.network.chat.Component.translatable("message.smoke_kitchen.rice_soil"));
            }
            return InteractionResult.SUCCESS;
        });
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
