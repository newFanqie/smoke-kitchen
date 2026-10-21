package dev.smokekitchen.world;

/** World-space measurements shared by interaction bounds and the client model placement. */
public final class KitchenGeometry {
    private KitchenGeometry() {}
    public static final float SOCKET_FLOOR=11/16f;
    public static final float COUNTER_TOP=13/16f;
    public static float vesselHeight(String id) {
        return switch(id) {
            case "frying_pan" -> 3/16f;
            case "wok" -> 4/16f;
            case "baking_tray" -> 2/16f;
            case "mixing_bowl" -> 4/16f;
            default -> 7/16f;
        };
    }
    public static float vesselBase(String id){return id.equals("baking_tray")||id.equals("salt_pan")?COUNTER_TOP:SOCKET_FLOOR;}
    public static float burnerTop(String vessel,boolean lid) {
        return vessel.isEmpty()?COUNTER_TOP:Math.max(COUNTER_TOP,vesselBase(vessel)+(vesselHeight(vessel)+(lid?2/16f:0)));
    }
}
