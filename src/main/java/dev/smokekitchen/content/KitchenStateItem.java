package dev.smokekitchen.content;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.TooltipDisplay;
import java.util.function.Consumer;

/** Vanilla item behavior with exact container quantities in its ordinary tooltip. */
public final class KitchenStateItem extends Item {
    public KitchenStateItem(Properties properties){super(properties);}
    @Override public net.minecraft.world.InteractionResult useOn(net.minecraft.world.item.context.UseOnContext context) {
        var player=context.getPlayer();
        if(player!=null&&player.isShiftKeyDown()&&context.getHand()==net.minecraft.world.InteractionHand.MAIN_HAND&&context.getLevel().getBlockEntity(context.getClickedPos()) instanceof dev.smokekitchen.world.KitchenBlockEntity station) {
            String held=ResourceStates.baseId(context.getItemInHand());
            if(held.contains("knife")&&station.kind().equals("board")||held.equals("heat_cloth")&&(station.kind().equals("cook")||station.isBurner())) {
                if(context.getLevel().isClientSide())return net.minecraft.world.InteractionResult.SUCCESS;
                return station.interact(player,context.getItemInHand(),new net.minecraft.world.phys.BlockHitResult(context.getClickLocation(),context.getClickedFace(),context.getClickedPos(),context.isInside()));
            }
        }
        return super.useOn(context);
    }
    @Override public void appendHoverText(ItemStack stack,TooltipContext context,TooltipDisplay display,Consumer<Component> lines,TooltipFlag flag) {
        super.appendHoverText(stack,context,display,lines,flag);
        String id=KitchenContent.name(stack.getItem());
        if(id.contains("gas_canister"))lines.accept(Component.translatable("message.smoke_kitchen.gas_amount",ResourceStates.gasUnits(stack),600));
        else if(stack.isDamageableItem())lines.accept(Component.translatable("message.smoke_kitchen.uses_left",stack.getMaxDamage()-stack.getDamageValue(),stack.getMaxDamage()));
        if(ResourceStates.baseId(stack).equals("rendered_fat"))lines.accept(Component.translatable("message.smoke_kitchen.render_help"));
        if(java.util.Set.of("soaked_soybean","raw_bean_pulp","boiled_bean_pulp","soy_milk","soy_curds","okara").contains(ResourceStates.baseId(stack)))lines.accept(Component.translatable("message.smoke_kitchen.tofu_chain"));
        var tag=stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
        if(tag.getBooleanOr("kitchen_rolled",false))lines.accept(Component.translatable("message.smoke_kitchen.rolled"));
    }
}
