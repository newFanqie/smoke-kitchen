package dev.smokekitchen.test;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.core.*;
import java.nio.file.*;
import com.google.gson.*;

/** Persisted early-survival milestones; the complete first-meal acceptance is still unfinished. */
public final class NaturalSurvivalProgressTest implements FabricClientGameTest {
    private static final Path RECORD=Path.of("survival-first-meal.json");
    private static void save(JsonObject record){try{Files.writeString(RECORD,new GsonBuilder().setPrettyPrinting().create().toJson(record));}catch(java.io.IOException e){throw new RuntimeException(e);}}
    @Override public void runTest(ClientGameTestContext c){
        JsonObject record;try{record=Files.exists(RECORD)?JsonParser.parseString(Files.readString(RECORD)).getAsJsonObject():new JsonObject();}catch(java.io.IOException e){throw new RuntimeException(e);}
        var world=record.has("save")?new net.fabricmc.fabric.impl.client.gametest.world.TestWorldSaveImpl(c,Path.of(record.get("save").getAsString())).open():c.worldBuilder().setUseConsistentSettings(false).adjustSettings(ui->{ui.setName("Smoke Kitchen Natural First Meal");ui.setWorldType(ui.getNormalPresetList().stream().filter(p->p.preset().is(WorldPresets.NORMAL)).findFirst().orElseThrow());ui.setSeed("1");ui.setGameMode(WorldCreationUiState.SelectedGameMode.SURVIVAL);ui.setDifficulty(Difficulty.EASY);ui.setAllowCommands(false);ui.setGenerateStructures(true);}).create();
        try(world){
            if(!record.has("save")){record.addProperty("save",world.getWorldSave().getSaveDirectory().toAbsolutePath().toString());record.addProperty("seed",1);record.addProperty("difficulty","easy");record.addProperty("commandsUsed",0);record.addProperty("stage",0);save(record);world.getServer().runOnServer(g->SurvivalPlayer.check(g.getPlayerList().getPlayers().getFirst().getInventory().getNonEquipmentItems().stream().allMatch(x->x.isEmpty()),"A fresh survival world must start without supplied inventory"));}
            var p=new SurvivalPlayer(c,world.getServer());p.note("Resuming natural survival stage "+record.get("stage"));
            if(record.get("stage").getAsInt()<1){
                if(p.count("oak_planks")<16&&p.count("oak_log")<4)p.harvestLogs(8);
                while(p.count("oak_planks")<16)p.craft("oak_planks","oak_log");
                if(p.count("stick")<4)p.craft("stick","oak_planks","","oak_planks","");
                BlockPos table=p.find(state->state.is(Blocks.CRAFTING_TABLE),12);
                if(record.has("table")){var a=record.getAsJsonArray("table");table=new BlockPos(a.get(0).getAsInt(),a.get(1).getAsInt(),a.get(2).getAsInt());}
                if(table==null){if(p.count("crafting_table")==0)p.craft("crafting_table","oak_planks","oak_planks","oak_planks","oak_planks");table=p.place("crafting_table");}
                record.add("table",new Gson().toJsonTree(new int[]{table.getX(),table.getY(),table.getZ()}));save(record);
                p.closeMenu();p.emptyHand();p.click(table,Direction.UP);
                if(p.count("wooden_pickaxe")==0)p.craft("wooden_pickaxe","oak_planks","oak_planks","oak_planks","","stick","","","stick","");
                p.closeMenu();record.addProperty("stage",1);save(record);c.takeScreenshot("survival-actual-wooden-tools");
            }
            if(p.count("cobblestone")==0){
                p.select("wooden_pickaxe");var stone=p.find(state->state.is(Blocks.STONE),40);p.note("Visible stone candidate "+stone);
                SurvivalPlayer.check(stone!=null,"Explore a visible stone face before continuing");p.mine(stone);
                SurvivalPlayer.check(p.count("cobblestone")>0,"The actually mined cobblestone must reach the survival inventory");
            }
            p.note("Actual first cobblestone count="+p.count("cobblestone"));c.takeScreenshot("survival-first-stone");
            record.addProperty("lastStageNote","Wooden tools and first cobblestone verified; stone tools, clay, salt, oil and the first meal remain unfinished");save(record);
        }
    }
}
