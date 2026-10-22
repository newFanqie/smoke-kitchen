# 原创美术样例方向

## 2026-10-22 最新优先级：先完成美术整改

用户已否定当前图标/鸭肉等样例的尺寸、风格和辨识度。先参考其提供的成熟模组，做图标、食物、世界模型的统一整改，再推进玩法；洗涤槽0.1.24顺延。0.1.17样例不再是“待反馈”。本轮94样本/本地可搜索资源目录与静态尺寸定位已完成，未改游戏资源，未发布新版。有限待办及验收门槛见 `art/ART-REVIEW-2026-10-22.md`。先做铁刀、锅铲、生熟鸭肉、炖汤、炒锅6样本，用户看过再扩展；必须实际检查背包、手持、掉落与台面，不以静态拼图或编译代替。

2026-10-21，用户澄清：复用原版指 Java 共同逻辑和行为的继承，美术可以独立设计。先看少量样例再按风格扩展。

高细节像素插画首稿被否定。第二稿获得“样式差不多”的反馈；用户进一步要求锅碗坛子可以用阶梯轮廓表现圆形。第三稿针对这个反馈修改，尚待进一步反馈。

- [第二稿：原版协调像素风](concepts/minecraft-style-square-sample.png)
- [第三稿：阶梯圆锅与坛子](concepts/minecraft-style-stepped-round-sample.png)

上述图片由内置 image_gen 生成和编辑，仅为风格预览，不是游戏截图、贴图图集、已完成模型或功能验收证据。不得据此声称锅具悬浮或烹饪动态已修复。

## 实作约束

保持原版协调的像素密度，以16×16贴图风格起步，必要局部细节再评估32×32；避免模糊、高细节插画、镜面写实、过量噪点。造型不限于正方形：圆锅、碗、坛子可用阶梯圆周及折面近似曲线。采用原创、可独立辨认的材质和轮廓。实际模型尺寸、碰撞范围、支撑接触和食材落点须在游戏中校准。

刀、铲、漏勺通过不同轮廓区分；调料瓶通过瓶型、瓶盖及液体共同区分。食材切配后有相应块、片、丝形态，放在锅内，随翻动和实际熟度改变外观。盖着的容器不在盖外显示食材。

基础画面在原版渲染下应成立，再检查 Iris。用户允许必要时配套专属资源包与光影包；当前样例未提供这些包。

## 第三稿编辑提示词

Edit this Minecraft-style sample sheet. Keep the same three-panel layout, ordinary vanilla-style Minecraft environment, 16x16 texture pixel density, original chunky low-resolution inventory tool sprites, food progression, restrained palette, and simple readability.

CHANGE ONLY THE SHAPES OF THE WOKS, CROCK AND THEIR LIDS wherever these appear. The user approves the overall Minecraft-style appearance but correctly says vessels do not have to be square. Make the vessels ROUND BY USING CLEAR STEPPED VOXEL CONTOURS, like drawing a circle on a Minecraft block grid: short flat top edge, staircase corners, short flat left/right edges, staircase bottom corners, short flat bottom edge. Use a coarse 12-to-16-sided stair-stepped circle footprint assembled from simple rectangular sections, with unmistakable square pixel steps in both silhouette and rim. NO smooth circular mesh, no round illustration, no high-poly modeling, no realistic gloss.

In panel A replace the square wok by a shallow dark-iron voxel-rounded wok recessed into the stone countertop. Its concave bowl has stepped sloped walls, two short rectangular handles. The cookware is supported by the stove with NO floating gap; rim only two pixels above countertop, diced ingredients sit inside below rim. Replace the square brown crock beside it with a voxel-rounded terracotta crock with stair-step round body and low stepped circular wooden lid and square knob. Keep everything as simple as ordinary Minecraft cauldrons and bells in material/texture complexity. Stove itself remains a square solid block, with its front opening as before.

In panel B replace only the wok inventory preview by the same stepped-round dark iron wok; keep other sprites exactly the same.
In panel C replace both square pans with matching stepped-round shallow voxel woks; preserve the raw/cooked food colors, proper inside-bowl placement and food progression icons unchanged.
The result must feel like Minecraft custom block models using pixelated approximate curves. Square pixels are mandatory; square vessels are not. Keep large crisp texture pixels and avoid the detailed ornamental pixel illustration style. No added text except existing A B C.

## 后续参考

用户已提供乡野、农夫乐事与森罗厨房JAR。已只读检查模型及贴图；参考结论见docs/REFERENCE-KITCHEN-MODS.md。重点学习简洁轮廓、少量分层色块、锅沿明暗及可辨认的食材形态，继续制作原创素材，不把参考贴图移入模组。0.1.12落实了第一批实际模型与图标，概念板仍不作为客户端验收证据。

## 0.1.14 原创成菜样例

麻婆豆腐采用阶梯圆盘、低矮盘沿、红汤、浅色豆腐块与棕色肉末，使用原版材质作色块，模型几何独立设计，没有复制参考模组。来源为tools/kitchen-art.mjs的dishModel。先交付这一道代表菜，再按反馈和有限待办扩展其余成菜。实际客户端放大模型对照与原料整链截图见docs/screenshots/0.1.14/，模型预览与汤色夹具均明确区别于制作证据。原版及实际启用Iris的画面已核对；不要求安装专属资源包或光影才能正常显示。

## 0.1.17 炖汤与鸭肉样例

来源为tools/food-samples.mjs：原创Minecraft原生JSON体素模型与手工安排像素簇的16×16材质，不提取参考JAR。木碗用阶梯圆碗沿、浅汤面与少量食材块；木材、汤汁、带皮鸭肉分别使用三色调色板。鸭肉以带皮肉块和浅刻纹区分于鸡腿，生肉粉色、熟肉暖烤色，变味黯淡、腐坏灰绿带少量霉斑。三类食物九种状态都改变食物本身，不叠同一枚通用角标。物品栏与世界使用相同几何，台面展示保留直立和底部接触。

这是本轮两个成品及关联生肉的代表样例，尚无用户对本轮的进一步反馈，不能将其写为所有美术已获最终认可。实际制作截图与明确标注的状态预览分开归档于docs/screenshots/0.1.17/。其余美术保持有限清单，依后续反馈扩展。
