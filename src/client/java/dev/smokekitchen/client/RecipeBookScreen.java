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
        pages.add(new Page("从第一顿饭开始","1. 用黏土、圆石做泥灶，做一口陶锅。\n2. 锅放在灶台上，加入燃料，空手右键调火。\n3. 手持食材右键投料，水桶加水，瓶罐加调料。\n4. 刀具右键砧板切菜；潜行持刀切换切法。\n5. 用锅铲翻炒，持碗或盘盛熟食。\n6. 潜行空手取出材料；潜行持隔热布搬整锅。\n\n手磨、压榨机与调理台：加入原料后空手反复右键加工。发酵缸和腌菜缸：空手右键密封，等待后取回产物。"));
        pages.add(new Page("调料与原料获取","草丛中能找到作物种子，也可探索农舍和厨房。\n\n米：稻谷在手磨脱壳。\n盐：盐矿石研磨成盐晶，16份盐晶与空罐制成一罐盐；也可在海洋水域潜行用水桶取盐水，放到灶台上的蒸发盘加热。\n油：8份油菜籽和玻璃瓶压榨；16份芝麻可制芝麻油。\n面团：调理台加入面粉×2与水，揉制。\n引子：米饭、菌菇、糖放发酵缸密封培养。\n酱油：大豆×4、面粉×2、盐×2份、引子、水、玻璃瓶，密封发酵。"));
        for(var e:ContentCatalog.DATA.getAsJsonArray("dishes")) {
            var r=e.getAsJsonObject();
            pages.add(new Page(ContentCatalog.str(r,"name"),ContentCatalog.str(r,"guide")+"\n\n建议产量："+ContentCatalog.num(r,"servings",1)+"份。\n\n食材保留各自熟度；最后加入的生食材仍需煮熟。过量盐可以在装盘前通过补充原料稀释。"));
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
