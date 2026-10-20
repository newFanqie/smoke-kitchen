package dev.smokekitchen.client;

import dev.smokekitchen.content.ContentCatalog;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.ArrayList;
import java.util.List;

public final class RecipeBookScreen extends Screen {
    private record Page(String title,String body) {}
    private final List<Page> pages=new ArrayList<>();
    private record RenderedPage(String title,List<net.minecraft.util.FormattedCharSequence> lines) {}
    private final List<RenderedPage> renderedPages=new ArrayList<>();
    private int page;
    public RecipeBookScreen() {
        super(Component.literal("烟火厨房 · 厨师食谱"));
        pages.add(new Page("从第一顿饭开始","1. 用黏土、圆石做泥灶，做一口陶锅。\n2. 持锅右键嵌入灶面，加煤后自动点火。\n3. 点顶部投料；空手点侧面调火。煮蒸炖要加水。\n4. 刀具右键砧板切菜；潜行持刀切换切法。\n5. 用锅铲翻炒，持碗或盘盛熟食。\n6. 潜行空手取出材料；潜行持隔热布搬整锅。\n\n手磨、压榨机与调理台：加入原料后空手反复右键加工。发酵缸和腌菜缸：空手右键密封，等待后取回产物。"));
        pages.add(new Page("调料与原料获取","草丛中能找到作物种子，也可探索农舍和厨房。\n\n米：稻谷在手磨脱壳。\n盐：盐矿石研磨成盐晶，16份盐晶与空罐制成一罐盐；也可在海洋水域潜行用水桶取盐水，放到灶台上的蒸发盘加热。\n油：8份油菜籽和玻璃瓶压榨；16份芝麻可制芝麻油。\n面团：调理台加入面粉×2与水，揉制。\n引子：米饭、菌菇、糖放发酵缸密封培养。\n酱油：大豆×4、面粉×2、盐×2份、引子、水、玻璃瓶，密封发酵。"));
        pages.add(new Page("小火炼猪油","普通猪额外掉落1份生动物脂，野猪也会掉落。将脂肪放进冷陶锅或平底锅，泥灶加煤后空手点侧面：中火→大火→关火→小火。只用小火，不加水、油和调料。\n\n熬透进度达到100%后，持空罐右键收油：2份脂肪装满8份猪油，1份装半罐4份；同时回收等量油渣。油渣可以吃或堆肥，猪油可替代普通菜的菜籽油。用完返空罐，芝麻油等指定香味仍须用指定调料。超过165°C或明显焦化的脂肪不能炼成合格食用油。"));
        pages.add(new Page("容器与盐水","空水壶对准水源右键灌满8份，逐份注入锅具或加工设备，用完保留空壶；水桶一次6份。\n\n海洋水域：对准水面潜行使用一桶普通水，转换成盐水桶；普通水不会产盐。盐水放进蒸发盘，煤灶、燃气灶或原版燃烧的篝火/熔炉供热120秒，得到4份盐晶并返空桶。放错水时可持空桶取回整盘水。\n\n手磨：盐矿石每份磨出4盐晶；16盐晶与1空罐磨成满盐罐。压榨：8油菜籽与1玻璃瓶得到满瓶菜籽油和2油渣饲料；压榨机需要活塞，炼猪油是不依赖红石的早期替代路线。\n\n第一份发酵引子由熟米饭、香菇和糖密封培养1200秒，不需要旧引子；醋引子由熟米饭、苹果和水起步。"));
        pages.add(new Page("燃料与天然气厨房","普通灶台：右键加煤后自动点火，并显示剩余燃料；空手右键侧面在关火、小火、中火和大火间切换。炉口使用原版燃烧动画。持锅右键灶台会嵌入灶面，食材点顶部投放；潜行空手点顶部取回熟食。\n\n天然气：在新生成的区块地下0至48层寻找含气岩层；将开采机紧贴岩层，燃气管道连接开采机、储气罐和天然气灶。空手点侧面调火。也可把气罐直接接到气灶；潜行空手点侧面拆罐。满罐100%、使用中50%图标、空罐0%，实际余量看提示。断气会停止供热。开采机与储气罐可用空燃气罐装气，也可用装满的气罐补充。岩层的气量有限，每罐600单位。\n\n创造模式：在烟火厨房分类找到全部设备、食材和7种刷怪蛋。鸭鹅野猪及水产死亡会掉落原料，生肉和水产也可用熔炉或烟熏炉加工。"));
        for(var e:ContentCatalog.DATA.getAsJsonArray("dishes")) {
            var r=e.getAsJsonObject();
            pages.add(new Page(ContentCatalog.str(r,"name"),ContentCatalog.str(r,"guide")+"\n\n建议产量："+ContentCatalog.num(r,"servings",1)+"份。"+(ContentCatalog.num(r,"stirs",0)>0?"最后一次投料后至少翻动"+ContentCatalog.num(r,"stirs",0)+"次。":"")+"\n\n食材保留各自熟度；最后加入的生食材仍需煮熟。过量盐可以在装盘前通过补充原料稀释。"));
        }
    }
    @Override protected void init() {
        renderedPages.clear();int lineLimit=Math.max(1,(height-112)/12);int lineWidth=Math.min(340,width-24)-16;
        for(Page source:pages){var lines=font.split(Component.literal(source.body),lineWidth);for(int start=0;start<lines.size();start+=lineLimit)renderedPages.add(new RenderedPage(source.title,List.copyOf(lines.subList(start,Math.min(lines.size(),start+lineLimit)))));}
        page=Math.min(page,Math.max(0,renderedPages.size()-1));
        addRenderableWidget(Button.builder(Component.literal("上一页"),b->page=Math.max(0,page-1)).bounds(width/2-142,height-35,90,20).build());
        addRenderableWidget(Button.builder(Component.literal("下一页"),b->page=Math.min(renderedPages.size()-1,page+1)).bounds(width/2+52,height-35,90,20).build());
        addRenderableWidget(Button.builder(Component.literal("关闭"),b->onClose()).bounds(width/2-35,height-35,70,20).build());
    }
    @Override public void extractRenderState(GuiGraphicsExtractor graphics,int mouseX,int mouseY,float delta) {
        graphics.fill(0,0,width,height,0xee251e18);
        int panelWidth=Math.min(340,width-24);int left=(width-panelWidth)/2;
        graphics.fill(left-10,12,left+panelWidth+10,height-45,0xffeee0bd);
        graphics.centeredText(font,renderedPages.get(page).title,width/2,25,0xff4c2c20);
        int y=48;
        for(var line:renderedPages.get(page).lines) {
            if(y>height-62)break;
            graphics.text(font,line,left+8,y,0xff42392c,false);y+=12;
        }
        graphics.centeredText(font,(page+1)+" / "+renderedPages.size(),width/2,height-54,0xff775638);
        super.extractRenderState(graphics,mouseX,mouseY,delta);
    }
}
