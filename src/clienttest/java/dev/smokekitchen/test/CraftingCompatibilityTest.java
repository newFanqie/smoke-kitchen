package dev.smokekitchen.test;

import dev.smokekitchen.SmokeKitchen;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;

/** Supplied raw materials; actual workbench packets, discovery, outputs and save/reload. */
public final class CraftingCompatibilityTest implements FabricClientGameTest {
    private static final String P="oak_planks",S="stick",I="iron_ingot",N="iron_nugget";
    @Override public void runTest(ClientGameTestContext c){
        net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave save;
        try(var world=c.worldBuilder().create()){
            save=world.getWorldSave();var server=world.getServer();
            server.runCommand("time set day");server.runCommand("weather clear");server.runCommand("gamemode survival @a");
            server.runCommand("fill -5 99 -5 7 99 6 minecraft:smooth_stone");server.runCommand("fill -5 100 -5 7 105 6 minecraft:air");server.runCommand("tp @a 0.5 100 -2 0 30");server.runCommand("setblock 0 100 0 minecraft:crafting_table");
            server.runCommand("give @a minecraft:oak_planks 64");server.runCommand("give @a minecraft:stick 64");server.runCommand("give @a minecraft:iron_ingot 16");server.runCommand("give @a minecraft:iron_nugget 16");c.waitTicks(15);
            if(net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("iris"))c.runOnClient(client->{try{var api=Class.forName("net.irisshaders.iris.api.v0.IrisApi");SurvivalPlayer.check((Boolean)api.getMethod("isShaderPackInUse").invoke(api.getMethod("getInstance").invoke(null)),"The Iris recipe/ore visual check needs an active shader pack");}catch(ReflectiveOperationException e){throw new AssertionError(e);}});
            server.runOnServer(g->{var book=g.getPlayerList().getPlayers().getFirst().getRecipeBook();for(String id:new String[]{"cutting_board","wooden_lid","ladle","tongs","dining_table","dish_rack"})SurvivalPlayer.check(book.contains(ResourceKey.create(Registries.RECIPE,SmokeKitchen.id(id))),"Native inventory change must discover "+id+" without recipe commands");});
            var p=new SurvivalPlayer(c,server);p.emptyHand();p.click(new BlockPos(0,100,0),Direction.UP);
            p.craft("wooden_pickaxe",P,P,P,"",S,"","",S,"");
            p.craft("wooden_sword",P,"","",P,"","",S,"","");
            p.craft("iron_sword",I,"","",I,"","",S,"","");
            p.craft("iron_chain",N,"","",I,"","",N,"","");
            p.craft("wooden_hoe",P,P,"","",S,"","",S,"");
            p.craft("oak_fence_gate",S,P,S,S,P,S);
            p.nextCraftScreenshot="crafting-compatible-board-0.1.10";
            p.craft("smoke_kitchen:cutting_board",P,P,P,"",S,"",S,"","");
            p.craft("smoke_kitchen:wooden_lid",P,P,"","",S,"");
            p.craft("smoke_kitchen:ladle",I,I,"","",S,"");
            p.craft("smoke_kitchen:tongs",N,"",N,"",I,"");
            p.craft("smoke_kitchen:dining_table",P,P,"",S,S,"");
            p.craft("smoke_kitchen:dish_rack",S,S,S,P,"",P,"",S,"");p.closeMenu();
            c.runOnClient(client->client.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(client.player)));c.waitTicks(5);c.takeScreenshot("crafting-compatible-products-0.1.10");p.closeMenu();
            server.runCommand("setblock 0 100 3 minecraft:stone");server.runCommand("setblock 2 100 3 smoke_kitchen:salt_ore");server.runCommand("setblock 4 100 3 minecraft:diorite");server.runCommand("tp @a 2.5 100 -1.5 0 15");c.waitTicks(30);p.look(new net.minecraft.world.phys.Vec3(2.5,100.5,3.5));world.getClientLevel().waitForChunksRender();c.takeScreenshot("salt-ore-distinct-0.1.10");
            for(String id:new String[]{"wooden_pickaxe","wooden_sword","iron_sword","iron_chain","wooden_hoe","oak_fence_gate","smoke_kitchen:cutting_board","smoke_kitchen:wooden_lid","smoke_kitchen:ladle","smoke_kitchen:tongs","smoke_kitchen:dining_table","smoke_kitchen:dish_rack"})SurvivalPlayer.check(p.count(id)==1,"Exactly one actually crafted "+id+" must remain");
        }
        try(var world=save.open()){
            var p=new SurvivalPlayer(c,world.getServer());c.waitTicks(8);
            for(String id:new String[]{"wooden_pickaxe","wooden_sword","iron_sword","iron_chain","wooden_hoe","oak_fence_gate","smoke_kitchen:cutting_board","smoke_kitchen:wooden_lid","smoke_kitchen:ladle","smoke_kitchen:tongs","smoke_kitchen:dining_table","smoke_kitchen:dish_rack"})SurvivalPlayer.check(p.count(id)==1,"Reload must preserve the actually crafted "+id);
        }
    }
}
