package dev.smokekitchen.content;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import java.util.Map;

/** A small set of registered visual states; exact quantities remain on the stack. */
public final class ResourceStates {
    public static String baseId(ItemStack stack) {
        String id=KitchenContent.name(stack.getItem());var definition=KitchenContent.DEFINITIONS.get(id);
        return definition!=null && definition.has("variant_of")?definition.get("variant_of").getAsString():id;
    }
    public static boolean spent(ItemStack stack) {
        var d=KitchenContent.DEFINITIONS.get(KitchenContent.name(stack.getItem()));
        return d!=null && (ContentCatalog.str(d,"state").equals("broken") || ContentCatalog.str(d,"state").equals("spoiled"));
    }
    public static int gasUnits(ItemStack stack) {
        String id=KitchenContent.name(stack.getItem());
        if(!id.equals("gas_canister")&&!id.equals("partial_gas_canister"))return 0;
        return Math.clamp(stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getIntOr("kitchen_gas",id.equals("gas_canister")?600:300),0,600);
    }
    public static ItemStack gasStack(int units) {
        units=Math.clamp(units,0,600);
        var result=new ItemStack(KitchenContent.item(units==0?"empty_gas_canister":units==600?"gas_canister":"partial_gas_canister"));
        var data=new net.minecraft.nbt.CompoundTag();data.putInt("kitchen_gas",units);
        result.set(DataComponents.CUSTOM_DATA,CustomData.of(data));return result;
    }
    public static ItemStack usedStack(ItemStack stack,Item emptyContainer) {
        String base=baseId(stack);
        if(stack.getDamageValue()>=stack.getMaxDamage()) {
            Item broken=KitchenContent.item("broken_"+base);
            return new ItemStack(emptyContainer!=Items.AIR?emptyContainer:broken);
        }
        Item used=KitchenContent.item("used_"+base);
        return used==Items.AIR?stack:stack.transmuteCopy(used);
    }
    public static void replaceHeld(Player player,ItemStack old,ItemStack replacement) {
        if(player.getMainHandItem()==old)player.setItemInHand(InteractionHand.MAIN_HAND,replacement);
        else if(player.getOffhandItem()==old)player.setItemInHand(InteractionHand.OFF_HAND,replacement);
        else {old.shrink(1);if(!replacement.isEmpty()&&!player.getInventory().add(replacement))player.drop(replacement,false);}
    }
    private static final Map<String,String> COOKED=Map.ofEntries(
        Map.entry("minecraft:chicken","minecraft:cooked_chicken"),Map.entry("minecraft:beef","minecraft:cooked_beef"),
        Map.entry("minecraft:porkchop","minecraft:cooked_porkchop"),Map.entry("minecraft:mutton","minecraft:cooked_mutton"),
        Map.entry("minecraft:rabbit","minecraft:cooked_rabbit"),Map.entry("minecraft:cod","minecraft:cooked_cod"),
        Map.entry("minecraft:salmon","minecraft:cooked_salmon"),Map.entry("minecraft:potato","minecraft:baked_potato"),
        Map.entry("smoke_kitchen:duck_meat","smoke_kitchen:cooked_duck"),Map.entry("smoke_kitchen:goose_meat","smoke_kitchen:cooked_goose"),
        Map.entry("smoke_kitchen:boar_meat","smoke_kitchen:cooked_boar"),Map.entry("smoke_kitchen:raw_shrimp","smoke_kitchen:cooked_shrimp"),
        Map.entry("smoke_kitchen:shrimp_meat","smoke_kitchen:cooked_shrimp"),Map.entry("smoke_kitchen:raw_crab","smoke_kitchen:cooked_crab"));
    public static ItemStack cookedStack(ItemStack input,boolean cooked,boolean burnt) {
        if(burnt)return new ItemStack(KitchenContent.item("burnt_food"),input.getCount());
        if(!cooked)return input.copy();
        if(baseId(input).equals("rice")) {
            var result=input.transmuteCopy(KitchenContent.item("cooked_rice"));result.remove(DataComponents.USE_REMAINDER);return result;
        }
        if(baseId(input).equals("dough")) {
            var data=input.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
            if(data.getBooleanOr("kitchen_rolled",false)&&data.getStringOr("kitchen_cut","").equals("SHRED")) {
                var result=input.copy();result.set(DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.translatable("message.smoke_kitchen.cooked_noodles"));return result;
            }
        }
        String target=COOKED.get(KitchenContent.fullName(input.getItem()));
        if(target==null)return input.copy();
        return input.transmuteCopy(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.parse(target)));
    }
    public static ItemStack cookedStack(ItemStack input,boolean cooked,boolean burnt,net.minecraft.world.level.Level level) {
        if(cooked&&!burnt && level instanceof net.minecraft.server.level.ServerLevel server) {
            var result=vanillaCookingResult(server,input,net.minecraft.world.item.crafting.RecipeType.CAMPFIRE_COOKING);
            if(result.isEmpty())result=vanillaCookingResult(server,input,net.minecraft.world.item.crafting.RecipeType.SMOKING);
            if(result.isEmpty())result=vanillaCookingResult(server,input,net.minecraft.world.item.crafting.RecipeType.SMELTING);
            if(!result.isEmpty()){result.setCount(input.getCount());return result;}
        }
        return cookedStack(input,cooked,burnt);
    }
    private static <T extends net.minecraft.world.item.crafting.AbstractCookingRecipe> ItemStack vanillaCookingResult(net.minecraft.server.level.ServerLevel level,ItemStack input,net.minecraft.world.item.crafting.RecipeType<T> type){
        var recipeInput=new net.minecraft.world.item.crafting.SingleRecipeInput(input);
        return level.recipeAccess().getRecipeFor(type,recipeInput,level).map(holder->holder.value().assemble(recipeInput)).orElse(ItemStack.EMPTY);
    }
    private ResourceStates(){}
}
