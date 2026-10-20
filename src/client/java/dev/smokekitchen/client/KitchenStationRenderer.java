package dev.smokekitchen.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.smokekitchen.world.KitchenBlockEntity;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import java.util.ArrayList;
import java.util.List;

public final class KitchenStationRenderer implements BlockEntityRenderer<KitchenBlockEntity,KitchenStationRenderer.State> {
    public static final class State extends BlockEntityRenderState {
        List<Component> names=List.of();
        float height;
        int color=0xfff3e6cb;
        List<ItemStackRenderState> items=List.of();
        float stirAngle;
        ItemStackRenderState vessel,lid,gas,water;
        float waterHeight;
        float vesselY;
        boolean oven;
        boolean cookingSurface;
        float flipDegrees;
    }
    private final Font font;
    private final ItemModelResolver resolver;
    public KitchenStationRenderer(BlockEntityRendererProvider.Context context) { font=context.font(); resolver=context.itemModelResolver(); }
    @Override public State createRenderState() { return new State(); }
    @Override public void extractRenderState(KitchenBlockEntity entity,State state,float partialTick,Vec3 camera,ModelFeatureRenderer.CrumblingOverlay overlay) {
        BlockEntityRenderer.super.extractRenderState(entity,state,partialTick,camera,overlay);
        state.oven=entity.isOven();state.vesselY=state.oven?.6875f:1.1875f;
        state.cookingSurface=entity.isBurner()||java.util.Set.of("cook","board","plate").contains(entity.kind());
        float phase=Math.clamp((entity.getLevel().getGameTime()-entity.lastFlipTime+partialTick)/12f,0,1);
        state.flipDegrees=entity.batch.turns>0?180*((entity.batch.turns-1)%2)+(entity.batch.turns%2==1?180:-180)*phase:0;
        int visible=entity.batch.remainingServings>=0&&entity.batch.totalServings>0?(int)Math.ceil((double)entity.batch.ingredients.size()*entity.batch.remainingServings/entity.batch.totalServings):26;
        var names=new ArrayList<Component>();
        for(int i=0;i<visible;i++)if(!entity.items.get(i).isEmpty() && names.size()<4)names.add(entity.displayedFood(i).getHoverName());
        state.names=List.copyOf(names);
        var rendered=new ArrayList<ItemStackRenderState>();
        for(int i=0;i<visible;i++) if(!entity.items.get(i).isEmpty() && rendered.size()<6) {
            var itemState=new ItemStackRenderState();
            resolver.updateForTopItem(itemState,entity.displayedFood(i),ItemDisplayContext.FIXED,entity.getLevel(),null,rendered.size());
            rendered.add(itemState);
        }
        state.items=List.copyOf(rendered);
        int sinceStir=entity.batch.seconds-entity.batch.lastStir;
        state.stirAngle=sinceStir<2 && entity.batch.stirs>0 ? (float)Math.sin((sinceStir+partialTick/20)*Math.PI)*25:0;
        state.height=entity.isBurner()?(entity.mountedCookware.isEmpty()?.89f:switch(entity.mountedCookware){case "frying_pan","baking_tray"->.965f;case "wok"->.99f;default->1.07f;}):switch(entity.kind()) { case "board" -> .13f; case "cook","plate" -> .23f; default -> 1.015f; };
        if(state.oven)state.height=.47f;
        state.vessel=null;state.lid=null;state.gas=null;state.water=null;
        if(!entity.isOven()&&(entity.batch.water>0||entity.batch.oil>0||entity.batch.ingredients.stream().anyMatch(p->p.id.equals("fat")&&p.cooked>=.4)) && (entity.isBurner()&&!entity.mountedCookware.isEmpty() || entity.kind().equals("cook"))) {
            var stack=new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.PAPER);
            String liquid=entity.batch.water>0?"pot_water":entity.batch.burntSugarSeconds>=5?"pot_burnt_sugar":entity.batch.caramelSeconds>=3?"pot_caramel":"pot_oil";
            stack.set(net.minecraft.core.component.DataComponents.ITEM_MODEL,dev.smokekitchen.SmokeKitchen.id(liquid));state.water=resolve(stack,entity);
            float fill=entity.batch.water>0?(float)Math.min(1,entity.batch.water/6):.1f;
            state.waterHeight=entity.isBurner()?switch(entity.mountedCookware){case "steamer"->.92f;case "frying_pan","wok","baking_tray"->.92f+.03f*fill;default->.92f+.12f*fill;}:entity.cookingId().equals("steamer")?.07f:.06f+.13f*fill;
        }
        if(!entity.mountedCookware.isEmpty())state.vessel=resolve(new net.minecraft.world.item.ItemStack(dev.smokekitchen.content.KitchenContent.item(entity.mountedCookware)),entity);
        if(entity.isBurner()&&!entity.attachedLid.isEmpty())state.lid=resolve(new net.minecraft.world.item.ItemStack(dev.smokekitchen.content.KitchenContent.item("pot_lid")),entity);
        if(entity.kind().equals("gas_stove")&&!entity.items.get(26).isEmpty())state.gas=resolve(entity.items.get(26),entity);
        state.color=entity.batch.burnt()?0xffbda195:entity.batch.ready()?0xffffd47c:0xfff3e6cb;
    }
    private ItemStackRenderState resolve(net.minecraft.world.item.ItemStack stack,KitchenBlockEntity entity){var result=new ItemStackRenderState();resolver.updateForTopItem(result,stack,ItemDisplayContext.FIXED,entity.getLevel(),null,0);return result;}
    @Override public void submit(State state,PoseStack poses,SubmitNodeCollector queue,CameraRenderState camera) {
        if(state.vessel!=null){poses.pushPose();poses.translate(.5,state.vesselY,.5);poses.scale(1.25f,1.25f,1.25f);state.vessel.submit(poses,queue,state.lightCoords,OverlayTexture.NO_OVERLAY,0);if(state.lid!=null)state.lid.submit(poses,queue,state.lightCoords,OverlayTexture.NO_OVERLAY,0);poses.popPose();}
        if(state.gas!=null){poses.pushPose();poses.translate(.96,.4,.5);poses.scale(.7f,.7f,.7f);state.gas.submit(poses,queue,state.lightCoords,OverlayTexture.NO_OVERLAY,0);poses.popPose();}
        if(state.water!=null){poses.pushPose();poses.translate(.5,state.waterHeight+.3125f,.5);poses.scale(1.25f,1.25f,1.25f);state.water.submit(poses,queue,state.lightCoords,OverlayTexture.NO_OVERLAY,0);poses.popPose();}
        if(state.names.isEmpty())return;
        for(int i=0;i<state.items.size();i++) {
            poses.pushPose();
            poses.translate(.28+(i%3)*.22,state.height+.015,.36+(i/3)*.27);
            poses.mulPose(Axis.YP.rotationDegrees(state.stirAngle));
            poses.mulPose(Axis.XP.rotationDegrees(90+state.stirAngle));
            poses.mulPose(Axis.XP.rotationDegrees(state.flipDegrees));
            poses.scale(.28f,.28f,.28f);
            state.items.get(i).submit(poses,queue,state.lightCoords,OverlayTexture.NO_OVERLAY,0);
            poses.popPose();
        }
        if(!dev.smokekitchen.KitchenConfig.INSTANCE.showWorldLabels||state.oven||state.cookingSurface)return;
        poses.pushPose();poses.translate(.5,state.height,.5);poses.mulPose(Axis.XP.rotationDegrees(90));poses.scale(.009f,.009f,.009f);
        for(int i=0;i<state.names.size();i++) {
            Component name=state.names.get(i);
            queue.submitText(poses,-font.width(name)/2f,-18+i*10,name.getVisualOrderText(),false,Font.DisplayMode.NORMAL,state.lightCoords,state.color,0,0);
        }
        poses.popPose();
    }
    @Override public int getViewDistance() { return 24; }
}
