package dev.smokekitchen.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.smokekitchen.SmokeKitchen;
import dev.smokekitchen.content.KitchenContent;
import dev.smokekitchen.cooking.CookingBatch;
import dev.smokekitchen.world.KitchenBlock;
import dev.smokekitchen.world.KitchenBlockEntity;
import dev.smokekitchen.world.KitchenGeometry;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class KitchenStationRenderer implements BlockEntityRenderer<KitchenBlockEntity,KitchenStationRenderer.State> {
    public record FoodPiece(ItemStackRenderState item,float x,float y,float z,float scale,float yaw,float flip,float modelHeight,boolean flat,String appearance) {}
    public static final class State extends BlockEntityRenderState {
        List<Component> names=List.of();
        public List<FoodPiece> pieces=List.of();
        public boolean covered;
        public String liquidAppearance="";
        public float vesselBase;
        float labelY,waterY,lidY;
        boolean labels;
        ItemStackRenderState vessel,lid,gas,water;
    }
    private final Font font;
    private final ItemModelResolver resolver;
    public KitchenStationRenderer(BlockEntityRendererProvider.Context context) {font=context.font();resolver=context.itemModelResolver();}
    @Override public State createRenderState() {return new State();}
    private ItemStackRenderState resolve(ItemStack stack,KitchenBlockEntity entity,ItemDisplayContext context){
        var result=new ItemStackRenderState();resolver.updateForTopItem(result,stack,context,entity.getLevel(),null,0);return result;
    }
    private ItemStackRenderState model(String id,KitchenBlockEntity entity){
        var stack=new ItemStack(Items.PAPER);stack.set(DataComponents.ITEM_MODEL,SmokeKitchen.id(id));return resolve(stack,entity,ItemDisplayContext.NONE);
    }
    private static String family(String id){
        return switch(id){
            case "beef","pork","chicken","duck","goose","fat","skewers" -> "meat";
            case "fish","shrimp","crab" -> "fish";
            case "potato","ginger","soybean","soybean_cooked","pumpkin" -> "root";
            case "tomato","chili","apple" -> "red";
            case "cabbage","cucumber","scallion","kelp" -> "leaf";
            case "rice","rice_cooked","egg" -> "grain";
            case "dough","dough_fermented","dumplings","pie" -> "dough";
            case "tofu","soy_milk" -> "tofu";
            case "sichuan_pepper" -> "pepper";
            default -> null;
        };
    }
    public static String stage(CookingBatch.Portion p,boolean wet){
        if(p.charred>=.25)return "charred";
        if(p.id.equals("tofu"))return wet?"poached":p.maximumTemperature<120?"raw":p.maximumTemperature<155?"setting":p.cooked<2?"browning":"cooked";
        if(p.cooked>=1)return wet?"poached":"cooked";
        if(p.cooked>=.7&&!wet)return "browning";
        return p.cooked>=.3?"setting":"raw";
    }
    private static float noise(int seed){int x=seed*0x45d9f3b;x=(x^(x>>>16))*0x45d9f3b;return (x&65535)/65535f;}
    @Override public void extractRenderState(KitchenBlockEntity entity,State state,float partialTick,Vec3 camera,ModelFeatureRenderer.CrumblingOverlay overlay){
        BlockEntityRenderer.super.extractRenderState(entity,state,partialTick,camera,overlay);
        boolean burner=entity.isBurner(),oven=entity.isOven(),cook=burner||entity.kind().equals("cook"),board=entity.kind().equals("board");
        boolean crock=Set.of("ferment","pickle").contains(entity.kind());
        String vessel=entity.cookingId();
        state.covered=entity.getBlockState().getValue(KitchenBlock.LID);
        state.vesselBase=oven?5/16f:KitchenGeometry.vesselBase(vessel);
        state.vessel=entity.mountedCookware.isEmpty()?null:resolve(new ItemStack(KitchenContent.item(entity.mountedCookware)),entity,ItemDisplayContext.NONE);
        state.lid=burner&&!entity.attachedLid.isEmpty()?model("pot_lid",entity):null;
        state.lidY=state.vesselBase+KitchenGeometry.vesselHeight(vessel)-7/16f;
        state.gas=entity.kind().equals("gas_stove")&&!entity.items.get(26).isEmpty()?resolve(entity.items.get(26),entity,ItemDisplayContext.FIXED):null;
        state.water=null;state.liquidAppearance="";
        float base=burner?state.vesselBase:oven?5/16f:0;
        float foodY=cook?base+(vessel.equals("wok")?2/16f:1.1f/16f):board?1.1f/16f:crock?10/16f:entity.kind().equals("plate")?1.1f/16f:1.015f;
        if(!state.covered&&!oven&&cook&&(!burner||!entity.mountedCookware.isEmpty())&&(entity.batch.water>0||entity.batch.oil>0||entity.batch.ingredients.stream().anyMatch(p->p.id.equals("fat")&&p.cooked>=.4))){
            String liquid=entity.batch.water>0?"pot_water":entity.batch.burntSugarSeconds>=5?"pot_burnt_sugar":entity.batch.caramelSeconds>=3?"pot_caramel":"pot_oil";
            if(entity.batch.water>0&&entity.batch.ingredients.stream().anyMatch(p->Set.of("bean_pulp","soy_milk").contains(p.id)))liquid="pot_soy_milk";
            if(entity.batch.water>0&&entity.batch.oil>0&&entity.batch.soy>0&&entity.batch.count("tofu")>0&&entity.batch.ingredients.stream().anyMatch(p->p.id.equals("chili")&&p.cooked>=.3))liquid="pot_mapo_sauce";
            state.liquidAppearance=liquid;state.water=model(liquid,entity);
            float depth=Math.min(KitchenGeometry.vesselHeight(vessel)-1.5f/16f,(float)(entity.batch.water/6)*3/16f);
            state.waterY=base+(entity.batch.water>0?Math.max(1.2f/16f,depth):1.1f/16f);
            if(vessel.equals("wok"))state.waterY=Math.max(base+2.05f/16f,state.waterY);
            if(entity.batch.water>0)foodY=Math.max(foodY,state.waterY-.5f/16f);
        }
        state.labelY=crock?14/16f:foodY;
        state.labels=!state.covered&&!cook&&!board&&!crock&&!Set.of("plate","mix").contains(entity.kind());
        int visible=entity.batch.remainingServings>=0&&entity.batch.totalServings>0?(int)Math.ceil((double)entity.batch.ingredients.size()*entity.batch.remainingServings/entity.batch.totalServings):26;
        var names=new ArrayList<Component>();var pieces=new ArrayList<FoodPiece>();
        double age=entity.getLevel().getGameTime()+partialTick-entity.lastStirTime;
        float mix=(float)Math.clamp(age/16,0,1),motion=age>=0&&age<16?(float)Math.sin(mix*Math.PI):0;
        float flipPhase=Math.clamp((entity.getLevel().getGameTime()+partialTick-entity.lastFlipTime)/12f,0,1);
        float flip=entity.batch.turns==0?0:180*((entity.batch.turns-1)%2)+(entity.batch.turns%2==1?180:-180)*flipPhase;
        int seed=(int)entity.getBlockPos().asLong();
        if(!state.covered)for(int slot=0;slot<visible&&slot<26;slot++){
            var input=entity.items.get(slot);if(input.isEmpty())continue;
            if(names.size()<4)names.add(entity.displayedFood(slot).getHoverName());
            CookingBatch.Portion portion=slot<entity.batch.ingredients.size()?entity.batch.ingredients.get(slot):null;
            if(board&&KitchenBlockEntity.ingredient(input).isPresent()){
                var ingredient=KitchenBlockEntity.ingredient(input).orElseThrow();var data=input.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
                CookingBatch.Cut cut=CookingBatch.Cut.WHOLE;try{cut=CookingBatch.Cut.valueOf(data.getStringOr("kitchen_cut","WHOLE"));}catch(IllegalArgumentException ignored){}
                portion=new CookingBatch.Portion(ingredient.id(),cut,1,false,ingredient.ready());
            }
            if(cook&&portion!=null&&(portion.id.equals("bean_pulp")||portion.id.equals("soy_milk")&&portion.cooked<.3))continue;
            String group=portion==null?null:family(portion.id);
            if(group!=null&&(cook||board)){
                boolean curd=portion.id.equals("soy_milk"),pepper=portion.id.equals("sichuan_pepper");
                int count=pepper?4:curd?(int)Math.ceil(portion.cooked*6):switch(portion.cut){case WHOLE->1;case DICE->4;case SLICE->3;case SHRED->5;case MINCE->6;};
                String appearance="food/"+group+"_"+(curd?"mince":portion.cut.name().toLowerCase(java.util.Locale.ROOT))+"_"+stage(portion,entity.batch.water>0);
                if(pepper)appearance="food/sichuan_pepper";
                var item=model(appearance,entity);
                for(int n=0;n<count;n++){
                    int key=slot*17+n;
                    // Stable scattered locations. A real stir moves and turns pieces, then settles them.
                    double angle=key*2.399963+noise(seed+key)*.6+(entity.batch.stirs-(1-mix))*1.05;
                    float radius=(float)Math.sqrt(noise(seed+key*7+3))*(!curd&&portion.cut==CookingBatch.Cut.WHOLE?.14f:.23f);
                    float x=.5f+(float)Math.cos(angle)*radius,z=.5f+(float)Math.sin(angle)*radius;
                    float scale=portion.cut==CookingBatch.Cut.WHOLE?.65f:.6f;
                    float h=curd||pepper?1.5f/16:switch(portion.cut){case WHOLE->2.2f/16;case DICE->3.1f/16;case MINCE->1.5f/16;default->1.1f/16;};
                    float lift=motion*(portion.cut==CookingBatch.Cut.WHOLE?.025f:.04f);
                    pieces.add(new FoodPiece(item,x,foodY+lift,z,scale,noise(key+seed)*360+entity.batch.stirs*25+motion*35,portion.cut==CookingBatch.Cut.WHOLE?flip:motion*18,h,false,appearance));
                }
            }else if(pieces.size()<36){
                float angle=(float)(slot*2.399963),r=.12f+(slot%3)*.035f;
                pieces.add(new FoodPiece(resolve(entity.displayedFood(slot),entity,ItemDisplayContext.FIXED),.5f+(float)Math.cos(angle)*r,foodY+.015f,.5f+(float)Math.sin(angle)*r,.27f,slot*47,0,0,true,"item"));
            }
            if(pieces.size()>=36)break;
        }
        state.names=List.copyOf(names);state.pieces=List.copyOf(pieces);
    }
    // NONE context keeps JSON measurements exact: compensate only for the item renderer's center.
    private static void blockItem(ItemStackRenderState item,PoseStack poses,SubmitNodeCollector queue,int light,float y){
        poses.pushPose();poses.translate(.5,y+.5,.5);item.submit(poses,queue,light,OverlayTexture.NO_OVERLAY,0);poses.popPose();
    }
    @Override public void submit(State state,PoseStack poses,SubmitNodeCollector queue,CameraRenderState camera){
        if(state.vessel!=null)blockItem(state.vessel,poses,queue,state.lightCoords,state.vesselBase);
        if(state.lid!=null)blockItem(state.lid,poses,queue,state.lightCoords,state.lidY);
        if(state.gas!=null){poses.pushPose();poses.translate(.96,.4,.5);poses.scale(.7f,.7f,.7f);state.gas.submit(poses,queue,state.lightCoords,OverlayTexture.NO_OVERLAY,0);poses.popPose();}
        if(state.water!=null)blockItem(state.water,poses,queue,state.lightCoords,state.waterY);
        for(var p:state.pieces){
            poses.pushPose();poses.translate(p.x,p.y+(p.flat?0:p.modelHeight*p.scale/2),p.z);poses.mulPose(Axis.YP.rotationDegrees(p.yaw));
            poses.mulPose(Axis.XP.rotationDegrees(p.flat?90:p.flip));poses.scale(p.scale,p.scale,p.scale);
            if(!p.flat)poses.translate(0,.5-p.modelHeight/2,0);
            p.item.submit(poses,queue,state.lightCoords,OverlayTexture.NO_OVERLAY,0);poses.popPose();
        }
        if(!dev.smokekitchen.KitchenConfig.INSTANCE.showWorldLabels||!state.labels)return;
        poses.pushPose();poses.translate(.5,state.labelY,.5);poses.mulPose(Axis.XP.rotationDegrees(90));poses.scale(.009f,.009f,.009f);
        for(int i=0;i<state.names.size();i++){var name=state.names.get(i);queue.submitText(poses,-font.width(name)/2f,-18+i*10,name.getVisualOrderText(),false,Font.DisplayMode.NORMAL,state.lightCoords,0xfff3e6cb,0,0);}
        poses.popPose();
    }
    @Override public int getViewDistance(){return 24;}
}
