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
    }
    private final Font font;
    private final ItemModelResolver resolver;
    public KitchenStationRenderer(BlockEntityRendererProvider.Context context) { font=context.font(); resolver=context.itemModelResolver(); }
    @Override public State createRenderState() { return new State(); }
    @Override public void extractRenderState(KitchenBlockEntity entity,State state,float partialTick,Vec3 camera,ModelFeatureRenderer.CrumblingOverlay overlay) {
        BlockEntityRenderer.super.extractRenderState(entity,state,partialTick,camera,overlay);
        var names=new ArrayList<Component>();
        for(var stack:entity.items) if(!stack.isEmpty() && names.size()<4) names.add(stack.getHoverName());
        state.names=List.copyOf(names);
        var rendered=new ArrayList<ItemStackRenderState>();
        for(var stack:entity.items) if(!stack.isEmpty() && rendered.size()<6) {
            var itemState=new ItemStackRenderState();
            resolver.updateForTopItem(itemState,stack.copy(),ItemDisplayContext.FIXED,entity.getLevel(),null,rendered.size());
            rendered.add(itemState);
        }
        state.items=List.copyOf(rendered);
        int sinceStir=entity.batch.seconds-entity.batch.lastStir;
        state.stirAngle=sinceStir<2 && entity.batch.stirs>0 ? (float)Math.sin((sinceStir+partialTick/20)*Math.PI)*25:0;
        state.height=switch(entity.kind()) { case "board" -> .13f; case "cook","plate" -> .46f; default -> 1.015f; };
        state.color=entity.batch.burnt()?0xffbda195:entity.batch.ready()?0xffffd47c:0xfff3e6cb;
    }
    @Override public void submit(State state,PoseStack poses,SubmitNodeCollector queue,CameraRenderState camera) {
        if(state.names.isEmpty())return;
        for(int i=0;i<state.items.size();i++) {
            poses.pushPose();
            poses.translate(.28+(i%3)*.22,state.height+.015,.36+(i/3)*.27);
            poses.mulPose(Axis.YP.rotationDegrees(state.stirAngle));
            poses.mulPose(Axis.XP.rotationDegrees(90+state.stirAngle));
            poses.scale(.28f,.28f,.28f);
            state.items.get(i).submit(poses,queue,state.lightCoords,OverlayTexture.NO_OVERLAY,0);
            poses.popPose();
        }
        if(!dev.smokekitchen.KitchenConfig.INSTANCE.showWorldLabels)return;
        poses.pushPose();poses.translate(.5,state.height,.5);poses.mulPose(Axis.XP.rotationDegrees(90));poses.scale(.009f,.009f,.009f);
        for(int i=0;i<state.names.size();i++) {
            Component name=state.names.get(i);
            queue.submitText(poses,-font.width(name)/2f,-18+i*10,name.getVisualOrderText(),false,Font.DisplayMode.NORMAL,state.lightCoords,state.color,0,0);
        }
        poses.popPose();
    }
    @Override public int getViewDistance() { return 24; }
}
