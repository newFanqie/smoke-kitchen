package dev.smokekitchen.test;

import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.*;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.*;
import java.util.*;
import java.util.function.Predicate;

/** Test-only player driver. Sends ordinary survival input; never grants items or changes blocks/time. */
final class SurvivalPlayer {
    final ClientGameTestContext c;final TestServerContext s;String nextCraftScreenshot;
    SurvivalPlayer(ClientGameTestContext c,TestServerContext s){this.c=c;this.s=s;}
    static void check(boolean value,String text){if(!value)throw new AssertionError(text);}
    static Item item(String id){return BuiltInRegistries.ITEM.getValue(Identifier.parse(id.contains(":")?id:"minecraft:"+id));}
    int count(String id){return c.computeOnClient(client->client.player.getInventory().countItem(item(id)));}
    BlockPos feet(){return c.computeOnClient(client->client.player.blockPosition());}
    void note(String text){org.slf4j.LoggerFactory.getLogger("KitchenPlayerActions").info("{} at {}",text,feet());}
    void select(String id){select(id,stack->true);}
    void select(String id,Predicate<ItemStack> allowed){c.runOnClient(client->{for(var slot:client.player.inventoryMenu.slots)if(slot.container==client.player.getInventory()&&allowed.test(slot.getItem())&&(slot.getItem().is(item(id))||id.startsWith("smoke_kitchen:")&&dev.smokekitchen.content.ResourceStates.baseId(slot.getItem()).equals(id.substring(14))&&!dev.smokekitchen.content.ResourceStates.spent(slot.getItem()))){int i=slot.getContainerSlot();if(i<9)client.player.getInventory().setSelectedSlot(i);else{client.gameMode.handleContainerInput(client.player.inventoryMenu.containerId,slot.index,8,ContainerInput.SWAP,client.player);client.player.getInventory().setSelectedSlot(8);}return;}throw new AssertionError("Not actually owned: "+id);});c.waitTicks(4);}
    void emptyHand(){c.runOnClient(client->{for(int i=0;i<9;i++)if(client.player.getInventory().getItem(i).isEmpty()){client.player.getInventory().setSelectedSlot(i);return;}client.gameMode.handleContainerInput(client.player.inventoryMenu.containerId,44,0,ContainerInput.QUICK_MOVE,client.player);client.player.getInventory().setSelectedSlot(8);});c.waitTicks(3);}
    void look(Vec3 target){c.runOnClient(client->{var d=target.subtract(client.player.getEyePosition());client.player.setYRot((float)(Math.toDegrees(Math.atan2(d.z,d.x))-90));client.player.setXRot((float)-Math.toDegrees(Math.atan2(d.y,Math.sqrt(d.x*d.x+d.z*d.z))));});c.waitTicks(2);}
    void stop(){c.getInput().releaseKey(o->o.keyUp);c.getInput().releaseKey(o->o.keyJump);c.getInput().releaseKey(o->o.keySprint);}
    void walkNear(BlockPos target){walkNear(target,3.6);}
    void walkNear(BlockPos target,double reach){
        for(int attempt=0;attempt<8;attempt++) {
            if(c.computeOnClient(client->client.player.getEyePosition().distanceTo(Vec3.atCenterOf(target))<reach))return;
            Path[] route={null};s.runOnServer(g->{var player=g.getPlayerList().getPlayers().getFirst();var probe=EntityType.ZOMBIE.create(g.overworld(),EntitySpawnReason.LOAD);check(probe!=null,"Cannot create a navigation-only probe");probe.setPos(player.position());probe.setOnGround(true);probe.getAttribute(Attributes.FOLLOW_RANGE).setBaseValue(96);probe.getNavigation().setCanFloat(true);route[0]=probe.getNavigation().createPath(target,reach<2.5?0:1);});
            check(route[0]!=null,"No walkable route to "+target+" from "+feet());
            boolean advanced=false;
            for(int i=1;i<route[0].getNodeCount();i++) {
                var node=route[0].getNodePos(i);int stuck=0;double previous=1000;
                for(int tick=0;tick<65;tick+=2) {
                    double distance=c.computeOnClient(client->{var d=new Vec3(node.getX()+.5,client.player.getY(),node.getZ()+.5).subtract(client.player.position());return d.horizontalDistance();});
                    if(distance<.35){advanced=true;break;}
                    c.runOnClient(client->{var d=new Vec3(node.getX()+.5,client.player.getY(),node.getZ()+.5).subtract(client.player.position());client.player.setYRot((float)(Math.toDegrees(Math.atan2(d.z,d.x))-90));client.player.setXRot(0);});
                    c.getInput().holdKey(o->o.keyUp);c.getInput().holdKey(o->o.keySprint);
                    if(c.computeOnClient(client->node.getY()>client.player.getY()+.3||client.player.isInWater()))c.getInput().holdKey(o->o.keyJump);else c.getInput().releaseKey(o->o.keyJump);
                    c.waitTicks(2);if(Math.abs(previous-distance)<.015)stuck++;else stuck=0;previous=distance;if(stuck>8)break;
                }
                if(c.computeOnClient(client->client.player.getEyePosition().distanceTo(Vec3.atCenterOf(target))<reach)){stop();return;}
            }
            stop();check(advanced,"Walking made no progress toward "+target);
        }
        throw new AssertionError("Could not approach "+target+" from "+feet());
    }
    void mine(BlockPos pos){closeMenu();walkNear(pos,4.4);look(Vec3.atCenterOf(pos));c.runOnClient(client->client.gameMode.startDestroyBlock(pos,Direction.UP));for(int tick=0;tick<220;tick++){if(c.computeOnClient(client->client.level.isEmptyBlock(pos)))break;c.runOnClient(client->client.gameMode.continueDestroyBlock(pos,Direction.UP));c.waitTicks(1);}c.runOnClient(client->client.gameMode.stopDestroyBlock());check(c.computeOnClient(client->client.level.isEmptyBlock(pos)),"Normal mining did not break "+pos);c.waitTicks(30);var drops=c.computeOnClient(client->{var found=new ArrayList<net.minecraft.world.entity.item.ItemEntity>();for(var entity:client.level.entitiesForRendering())if(entity instanceof net.minecraft.world.entity.item.ItemEntity drop&&drop.position().distanceTo(Vec3.atCenterOf(pos))<7)found.add(drop);found.sort(Comparator.comparingDouble(x->x.distanceToSqr(client.player)));return found.stream().map(x->x.blockPosition()).toList();});for(var drop:drops){boolean remains=c.computeOnClient(client->{for(var entity:client.level.entitiesForRendering())if(entity instanceof net.minecraft.world.entity.item.ItemEntity&&entity.blockPosition().equals(drop))return true;return false;});if(remains){walkNear(drop,1.6);c.waitTicks(12);}}}
    void pickUpAround(Vec3 center){
        var drops=c.computeOnClient(client->{var found=new ArrayList<BlockPos>();for(var entity:client.level.entitiesForRendering())if(entity instanceof net.minecraft.world.entity.item.ItemEntity&&entity.position().distanceTo(center)<7)found.add(entity.blockPosition());return found;});
        for(var pos:drops){walkNear(pos,1.4);c.waitTicks(10);}
    }
    void click(BlockPos pos,Direction face){walkNear(pos);look(Vec3.atCenterOf(pos));c.runOnClient(client->client.gameMode.useItemOn(client.player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(pos),face,pos,false)));c.waitTicks(6);}
    BlockPos place(String id){closeMenu();select(id);BlockPos[] ground={null};c.runOnClient(client->{var origin=client.player.blockPosition();for(var direction:Direction.Plane.HORIZONTAL){var p=origin.relative(direction,2);if(client.level.getBlockState(p).canBeReplaced()&&client.level.getFluidState(p).isEmpty()&&client.level.getBlockState(p.above()).canBeReplaced()&&client.level.getBlockState(p.below()).isSolid()){ground[0]=p.below();break;}}});check(ground[0]!=null,"No free placement ground");click(ground[0],Direction.UP);return ground[0].above();}
    void slot(int slot,int button,ContainerInput action){c.runOnClient(client->client.gameMode.handleContainerInput(client.player.containerMenu.containerId,slot,button,action,client.player));c.waitTicks(2);}
    void putInMenu(String id,int destination,int amount){
        int from=c.computeOnClient(client->{for(var slot:client.player.containerMenu.slots)if(slot.container==client.player.getInventory()&&slot.getItem().is(item(id))&&slot.getItem().getCount()>=amount)return slot.index;throw new AssertionError("Missing actual menu input "+id);});
        slot(from,0,ContainerInput.PICKUP);for(int i=0;i<amount;i++)slot(destination,1,ContainerInput.PICKUP);slot(from,0,ContainerInput.PICKUP);
    }
    void craft(String output,String... pattern){
        c.runOnClient(client->{if(client.player.containerMenu==client.player.inventoryMenu&&!(client.screen instanceof net.minecraft.client.gui.screens.inventory.InventoryScreen))client.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(client.player));});c.waitTicks(3);
        c.runOnClient(client->{check(client.player.containerMenu.getCarried().isEmpty(),"Crafting must start with an empty cursor");});
        var grid=c.computeOnClient(client->{if(client.player.containerMenu instanceof net.minecraft.world.inventory.AbstractCraftingMenu menu)return menu.getInputGridSlots().stream().map(x->x.index).toList();throw new AssertionError("Normal crafting menu is not open");});
        for(int slot:grid)if(c.computeOnClient(client->!client.player.containerMenu.getSlot(slot).getItem().isEmpty()))slot(slot,0,ContainerInput.QUICK_MOVE);
        check(pattern.length<=grid.size(),"Recipe does not fit the open crafting grid");
        for(int i=0;i<pattern.length;i++)if(!pattern[i].isBlank()) {
            String ingredient=pattern[i];int from=c.computeOnClient(client->{for(var slot:client.player.containerMenu.slots)if(slot.container==client.player.getInventory()&&slot.getItem().is(item(ingredient)))return slot.index;throw new AssertionError("Missing actual crafting input "+ingredient);});
            slot(from,0,ContainerInput.PICKUP);slot(grid.get(i),1,ContainerInput.PICKUP);slot(from,0,ContainerInput.PICKUP);
        }
        c.waitTicks(4);boolean ready=c.computeOnClient(client->client.player.containerMenu.getSlot(0).getItem().is(item(output)));if(!ready){note(c.computeOnClient(client->"Crafting mismatch menu="+client.player.containerMenu.getClass().getSimpleName()+" grid="+grid+" values="+grid.stream().map(i->i+":"+client.player.containerMenu.getSlot(i).getItem()).toList()+" output="+client.player.containerMenu.getSlot(0).getItem()));c.takeScreenshot("survival-crafting-mismatch");}check(ready,"Actual crafting recipe did not produce "+output);if(nextCraftScreenshot!=null){c.takeScreenshot(nextCraftScreenshot);nextCraftScreenshot=null;}slot(0,0,ContainerInput.QUICK_MOVE);note("Crafted "+output);
    }
    void closeMenu(){c.runOnClient(client->{client.player.closeContainer();client.setScreen(null);});c.waitTicks(4);}
}
