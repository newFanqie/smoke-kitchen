package dev.smokekitchen.world;

import dev.smokekitchen.content.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import java.util.*;

/** Real dry material and one reusable jar; inventory changes are transactional. */
public final class SpiceMilling {
    public static final Map<String,String> POWDERS=Map.of("dried_chili","chili_powder","dried_black_pepper","pepper_powder","dried_cumin","cumin_powder","dried_shiitake","mushroom_powder");
    private SpiceMilling(){}
    public static int remaining(ItemStack stack){return Math.max(0,stack.getMaxDamage()-stack.getDamageValue());}
    public static boolean process(KitchenBlockEntity mill){
        String raw="";for(var s:mill.items){String id=KitchenContent.name(s.getItem());if(POWDERS.containsKey(id)){if(!raw.isEmpty()&&!raw.equals(id))return true;raw=id;}}
        if(raw.isEmpty())return false;
        if(mill.work<5)return true;
        String powder=POWDERS.get(raw);int jar=-1,available=0;
        for(int i=0;i<mill.items.size();i++){var s=mill.items.get(i);if(KitchenContent.name(s.getItem()).equals(raw))available+=s.getCount();if(ResourceStates.baseId(s).equals(powder)&&remaining(s)<=s.getMaxDamage()-4&&jar<0)jar=i;}
        boolean refill=jar>=0;
        if(!refill)for(int i=0;i<mill.items.size();i++)if(mill.items.get(i).is(KitchenContent.item("empty_jar"))){jar=i;break;}
        if(jar<0)return true;
        var container=mill.items.get(jar);int before=refill?remaining(container):0,max=KitchenContent.item(powder).getDefaultInstance().getMaxDamage(),count=Math.min(available,(max-before)/4);
        if(count<1)return true;
        var backup=new ArrayList<ItemStack>();mill.items.forEach(s->backup.add(s.copy()));
        var result=container.transmuteCopy(KitchenContent.item(before+count*4==max?powder:"used_"+powder),1);result.setDamageValue(max-before-count*4);
        int left=count;for(var s:mill.items)if(KitchenContent.name(s.getItem()).equals(raw)&&left>0){int used=Math.min(left,s.getCount());s.shrink(used);left-=used;}
        if(refill)mill.items.set(jar,result);
        else {container.shrink(1);int output=-1;for(int i=0;i<mill.items.size();i++)if(mill.items.get(i).isEmpty()){output=i;break;}if(output<0){for(int i=0;i<backup.size();i++)mill.items.set(i,backup.get(i));return true;}mill.items.set(output,result);}
        mill.work=0;mill.setChanged();return true;
    }
    public static Component status(KitchenBlockEntity mill){
        var raw=new HashSet<String>();for(var s:mill.items)if(POWDERS.containsKey(KitchenContent.name(s.getItem())))raw.add(KitchenContent.name(s.getItem()));
        if(raw.size()>1)return Component.literal("手磨：请分开研磨不同香料，原料与罐子均已保留。");
        if(raw.size()==1){String powder=POWDERS.get(raw.iterator().next());boolean can=mill.items.stream().anyMatch(s->s.is(KitchenContent.item("empty_jar"))||ResourceStates.baseId(s).equals(powder)&&remaining(s)<=s.getMaxDamage()-4);return Component.literal(can?"手磨：空手右键研磨装罐；1份干料=4次，满罐16次。":"手磨：加入空调料罐或同种使用中罐（至少余4次空间）；原料未消耗。");}
        if(mill.items.stream().anyMatch(s->Set.of("black_pepper","cumin","chili","shiitake").contains(ResourceStates.baseId(s))))return Component.literal("手磨：请先在晾晒架把香料/菌菇晾干；半干原料不能研磨装罐。");
        return null;
    }
}
