package dev.smokekitchen.content;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.TooltipDisplay;
import java.util.function.Consumer;

/** Vanilla item behavior with exact container quantities in its ordinary tooltip. */
public final class KitchenStateItem extends Item {
    public KitchenStateItem(Properties properties){super(properties);}
    @Override public void appendHoverText(ItemStack stack,TooltipContext context,TooltipDisplay display,Consumer<Component> lines,TooltipFlag flag) {
        super.appendHoverText(stack,context,display,lines,flag);
        String id=KitchenContent.name(stack.getItem());
        if(id.contains("gas_canister"))lines.accept(Component.translatable("message.smoke_kitchen.gas_amount",ResourceStates.gasUnits(stack),600));
        else if(stack.isDamageableItem())lines.accept(Component.translatable("message.smoke_kitchen.uses_left",stack.getMaxDamage()-stack.getDamageValue(),stack.getMaxDamage()));
    }
}
