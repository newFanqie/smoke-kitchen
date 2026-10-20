package dev.smokekitchen;

import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import java.nio.file.Files;

public final class KitchenConfig {
    public static KitchenConfig INSTANCE=new KitchenConfig();
    public boolean spoilage=true;
    public int freshDays=7;
    public int spoiledDays=14;
    public boolean showWorldLabels=true;
    public static void load() {
        var path=FabricLoader.getInstance().getConfigDir().resolve("smoke-kitchen.json");
        var gson=new GsonBuilder().setPrettyPrinting().create();
        try {
            if(Files.exists(path)) {var loaded=gson.fromJson(Files.readString(path),KitchenConfig.class);if(loaded!=null)INSTANCE=loaded;}
            else {Files.createDirectories(path.getParent());Files.writeString(path,gson.toJson(INSTANCE)+"\n");}
            INSTANCE.freshDays=Math.clamp(INSTANCE.freshDays,1,365);
            INSTANCE.spoiledDays=Math.clamp(INSTANCE.spoiledDays,INSTANCE.freshDays+1,730);
        } catch(Exception e) {SmokeKitchen.LOGGER.warn("Cannot load kitchen configuration; using defaults",e);}
    }
}
