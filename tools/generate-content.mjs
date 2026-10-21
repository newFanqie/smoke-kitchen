import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
import {inventoryModel,stationGeometry,cropModel} from './voxel-art.mjs';
import {vessel,vesselLid,toolModel,tofuModel,dishModel,disk,foodFamilies,foodStages,foodModel} from './kitchen-art.mjs';
import {sampleFoodModel,writeFoodSampleTextures,sampleBases} from './food-samples.mjs';
const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'../src/main/resources');
const ns='smoke_kitchen';
const write=(p,data)=>{if(p.includes('/models/')&&data.textures&&!data.textures.particle)data.textures.particle=Object.values(data.textures).find(t=>!t.startsWith('#'));const out=path.join(root,p);fs.mkdirSync(path.dirname(out),{recursive:true});fs.writeFileSync(out,JSON.stringify(data,null,2)+'\n');};
const items=[],stations=[],crops=[],dishes=[],processes=[],lang={},en={};
function item(id,name,texture,extra={}) {items.push({id,name,texture,...extra});lang[`item.${ns}.${id}`]=name;en[`item.${ns}.${id}`]=id.replace(/_/g,' ');}
function station(id,name,kind,model='cabinet') {stations.push({id,name,kind,model});lang[`block.${ns}.${id}`]=name;en[`block.${ns}.${id}`]=id.replace(/_/g,' ');}
const plantRows=[
 ['rice_paddy','稻谷','wheat',null],['tomato','番茄','apple','tomato'],['chili','辣椒','sweet_berries','chili'],
 ['soybean','大豆','wheat_seeds','soybean'],['rapeseed','油菜籽','melon_seeds',null],['scallion','葱','sugar_cane','scallion'],
 ['ginger','姜','potato','ginger'],['garlic','蒜','beetroot','garlic'],['cucumber','黄瓜','pickle','cucumber'],
 ['cabbage','白菜','kelp','cabbage'],['onion','洋葱','beetroot','onion'],['eggplant','茄子','chorus_fruit','eggplant'],
 ['sesame','芝麻','pumpkin_seeds',null],['sichuan_pepper','花椒','sweet_berries','sichuan_pepper'],['black_pepper','胡椒','melon_seeds',null],
 ['cumin','孜然','wheat_seeds',null],['shiitake','香菇','brown_mushroom','mushroom']];
for(const [id,name,texture,ingredient] of plantRows) {
 item(id,name,texture==='pickle'?'sea_pickle':texture,{nutrition:['tomato','cucumber','cabbage'].includes(id)?2:0,...(id==='soybean'?{ingredient:'soybean',needs_water:true,resistance:1.4}:ingredient?{ingredient}: {})});
 crops.push({id,name});lang[`block.${ns}.${id}_crop`]=name;lang[`item.${ns}.${id}_seeds`]=`${name}种子`;
}
item('rice','米','sugar',{ingredient:'rice',needs_water:true});
item('milk_portion','牛奶份','milk_bucket',{ingredient:'milk',ready:true});
item('salt_crystals','盐晶','quartz');
item('empty_water_jug','空水壶','bucket');
item('saltwater_bucket','盐水桶','water_bucket',{unstackable:true});
item('fermented_dough','发酵面团','clay_ball',{ingredient:'dough_fermented'});
item('raw_dumplings','生饺子','clay_ball',{ingredient:'dumplings',needs_water:true});
item('raw_skewers','生肉串','porkchop',{ingredient:'skewers',resistance:1.6});
item('raw_apple_pie','生苹果派','apple',{ingredient:'pie'});
item('rice_husk','谷壳','wheat');item('flour','面粉','sugar');item('dough','面团','clay_ball',{ingredient:'dough',needs_water:false});
item('starter','发酵引子','glowstone_dust');item('vinegar_starter','醋引子','glowstone_dust');
item('tofu','豆腐','snowball',{ingredient:'tofu',ready:true,nutrition:3});
for(const [id,name,texture,ingredient] of [
 ['duck_meat','鸭肉','chicken','duck'],['goose_meat','鹅肉','chicken','goose'],['boar_meat','野猪肉','porkchop','pork'],
 ['raw_shrimp','河虾','salmon','shrimp'],['shrimp_meat','虾仁','salmon','shrimp'],['raw_crab','河蟹','salmon','crab'],
 ['duck_egg','鸭蛋','egg','egg'],['goose_egg','鹅蛋','egg','egg']]) item(id,name,texture,{ingredient,nutrition:ingredient==='egg'?0:2,resistance:ingredient==='egg'?1:1.6});
for(const [id,name,ingredient] of [['duck','熟鸭肉','duck'],['goose','熟鹅肉','goose'],['boar','熟野猪肉','pork'],['shrimp','熟河虾','shrimp'],['crab','熟河蟹','crab']])item('cooked_'+id,name,'cooked_chicken',{ingredient,nutrition:6,ready:true});
item('empty_gas_canister','煤气罐（0%）','bucket',{unstackable:true});
item('gas_canister','煤气罐（100%）','bucket',{unstackable:true});
item('partial_gas_canister','煤气罐（50%）','bucket',{unstackable:true});
for(const [id,name,texture] of [['dried_chili','干辣椒','sweet_berries'],['dried_shiitake','干香菇','brown_mushroom'],['oil_cake','油渣饲料','wheat'],['salt_ore_chunk','盐矿石','quartz']]) item(id,name,texture);
item('rendered_fat','生动物脂','honeycomb',{ingredient:'fat'});
for(const [id,name,texture,seasoning,uses] of [
 ['salt','盐罐','sugar','salt',16],['rapeseed_oil','菜籽油','honey_bottle','oil',8],['sesame_oil','芝麻油','honey_bottle','oil',8],
 ['soy_sauce','酱油','glass_bottle','soy',8],['rice_vinegar','米醋','glass_bottle','vinegar',8],
 ['chili_powder','辣椒粉','redstone','spice',16],['pepper_powder','胡椒粉','gunpowder','spice',16],
 ['cumin_powder','孜然粉','glowstone_dust','spice',16],['mushroom_powder','菌菇鲜粉','sugar','spice',16]]) item(id,name,texture,{seasoning,uses});
for(const [id,name,texture,uses] of [
 ['stone_knife','石厨刀','stone_sword',96],['iron_knife','铁厨刀','iron_sword',256],['refined_knife','精制厨刀','iron_sword',512],
 ['wooden_spatula','木铲','wooden_shovel',128],['iron_spatula','铁锅铲','iron_shovel',384],['ladle','汤勺','iron_shovel',384],
 ['tongs','夹子','shears',256],['chopsticks','长筷','stick',128],['rolling_pin','擀面杖','stick',256],
 ['water_jug','水壶','bucket',8],['heat_cloth','隔热布','paper',128]]) item(id,name,texture,{uses});
for(const [id,name,texture] of [
 ['wooden_lid','木盖','oak_trapdoor'],['pot_lid','铁锅盖','iron_ingot'],['empty_jar','空调料罐','glass_bottle'],
 ['ceramic_plate','陶盘','bowl'],['ceramic_bowl','陶碗','bowl'],['thermometer','温度计','clock'],
 ['recipe_book','厨师食谱','book'],['sealed_jar','密封罐','glass_bottle'],['food_basket','食物篮','bundle'],['sieve','筛子','string']]) item(id,name,texture,{unstackable:id==='recipe_book',...(id==='sieve'?{uses:128}:{})});
for(const [id,name,kind,model] of [
 ['clay_stove','简易泥灶','stove','stove'],['brick_stove','砖砌灶台','stove','stove'],['double_stove','双眼灶台','stove','stove'],['oven','烤炉','cook','oven'],
 ['cutting_board','砧板','board','board'],['clay_pot','陶锅','cook','pot'],['soup_pot','铁汤锅','cook','pot'],['wok','炒锅','cook','wok'],['frying_pan','平底锅','cook','pan'],
 ['steamer','蒸笼','cook','steamer'],['baking_tray','烤盘','cook','pan'],['mixing_bowl','调理碗','cook','pot'],['counter','工作台面','storage','counter'],['mixing_table','调理台','mix','counter'],
 ['sink','洗涤槽','storage','sink'],['hand_mill','手磨','mill','mill'],['oil_press','压榨机','press','press'],['fermentation_crock','发酵缸','ferment','crock'],
 ['pickling_crock','腌菜缸','pickle','crock'],['drying_rack','晾晒架','dry','rack'],['cabinet','储物柜','storage','cabinet'],['grain_bin','谷仓箱','storage','cabinet'],
 ['icebox','冰柜','storage','cabinet'],['spice_rack','调料架','rack','rack'],['dish_rack','碗盘架','rack','rack'],['salt_pan','盐水蒸发盘','evaporate','pan'],
 ['spice_trellis','香料种植架','rack','rack'],['dining_table','餐桌','storage','table'],['chair','餐椅','storage','chair'],['serving_plate','共享菜盘','plate','plate'],
 ['soup_tureen','共享汤盆','plate','pot'],['cookbook_stand','食谱架','rack','stand'],['compost_bin','厨余桶','compost','crock'],['range_hood','排烟罩','decor','hood'],['chimney','烟道','decor','chimney']]) station(id,name,kind,model);
for(const [id,name,kind] of [['gas_stove','天然气灶','gas_stove'],['gas_tank','储气罐','gas_tank'],['gas_pipe','燃气管道','gas_pipe'],['gas_extractor','天然气开采机','gas_extractor'],['gas_deposit','含气岩层','gas_deposit']])station(id,name,kind,'cabinet');
function dish(id,name,ingredients,methods,servings,guide,seasonings={}) {
 dishes.push({id,name,ingredients,methods:methods.split(','),servings,guide,...seasonings});
 item(id,name,'bowl',{nutrition:6,saturation:.5,meal:true,...(id==='cooked_rice'?{ingredient:'rice_cooked',ready:true}: {})});
}
dish('cooked_rice','米饭',{rice:1},'BOIL',2,'米×1，加水；煮沸后小火加盖。');
dish('rice_porridge','白粥',{rice:1},'STEW',3,'米×1、较多水，小火搅拌。');
dish('fried_rice','蛋炒饭',{rice_cooked:2,egg:1,scallion:1},'STIR_FRY',2,'米饭×2、蛋×1、葱×1；热油先炒蛋，再下饭，少盐翻炒。',{oil:true,salt:true});
dish('tomato_eggs','番茄炒蛋',{tomato:2,egg:2},'STIR_FRY',2,'番茄×2、蛋×2；油盐适量，蛋先炒，番茄后下。',{oil:true,salt:true});
dish('sour_spicy_potato','酸辣土豆丝',{potato:2,chili:1,garlic:1},'STIR_FRY',2,'切丝土豆×2、辣椒×1、蒜×1；热油快炒，出锅前加醋。',{oil:true,salt:true,vinegar:true});
dish('garlic_greens','蒜蓉青菜',{cabbage:2,garlic:1},'STIR_FRY',2,'白菜×2、蒜×1；少油爆香再下菜。',{oil:true,salt:true});
dish('scallion_omelette','葱花煎蛋',{egg:2,scallion:1},'FRY',2,'蛋×2、葱×1；平底锅煎制并翻面。',{oil:true,salt:true});
dish('pan_fried_fish','煎鱼',{fish:2},'FRY',2,'鱼×2；热油下锅，盐与胡椒调味，及时翻面。',{oil:true,salt:true});
dish('steamed_fish','姜葱蒸鱼',{fish:2,ginger:1,scallion:1},'STEAM',2,'鱼×2、姜×1、葱×1；蒸熟后淋酱油。',{salt:true,soy:true});
dish('boiled_shrimp','白灼虾',{shrimp:3,ginger:1},'BOIL',2,'河虾×3、姜×1；水沸后下虾。',{salt:true});
dish('steamed_crab','蒸河蟹',{crab:2,ginger:1},'STEAM',2,'河蟹×2、姜×1；加水蒸制，配醋。',{vinegar:true});
dish('mushroom_soup','菌菇汤',{mushroom:3},'BOIL,STEW',3,'可食菌菇×3；水、盐，小火煮。',{salt:true});
dish('tomato_egg_soup','番茄蛋汤',{tomato:2,egg:1},'BOIL',3,'番茄×2、蛋×1；先煮番茄再下蛋。',{salt:true});
dish('carrot_meat_stew','萝卜炖肉',{carrot:2,pork:2,ginger:1},'STEW',3,'胡萝卜×2、肉×2、姜×1；加水小火炖。',{salt:true});
dish('beef_potato_stew','土豆炖牛肉',{potato:2,beef:2,onion:1},'STEW',3,'土豆×2、牛肉×2、洋葱×1；先炖牛肉。',{salt:true});
dish('pumpkin_soup','南瓜浓汤',{pumpkin:2,milk:1},'BOIL,STEW',3,'南瓜×2、牛奶×1；煮软后混合。',{salt:true});
dish('braised_pork','红烧肉',{pork:3,ginger:1},'STEW',3,'猪肉×3、姜×1；糖色、酱油、加水炖。',{soy:true});
dish('braised_eggplant','烧茄子',{eggplant:3,garlic:1},'STIR_FRY',2,'茄子×3、蒜×1；油、酱油与少量糖。',{oil:true,soy:true});
dish('mapo_tofu','麻婆豆腐',{tofu:2,pork:1,chili:1,sichuan_pepper:1},'STIR_FRY',2,'豆腐×2、肉末×1、辣椒×1；花椒、酱油，轻推。',{oil:true,soy:true});
dish('cucumber_salad','凉拌黄瓜',{cucumber:2,garlic:1},'MIX',2,'黄瓜×2、蒜×1；盐、醋、芝麻油拌匀。',{oil:true,salt:true,vinegar:true});
dish('pickled_cabbage','泡菜',{cabbage:3,chili:1,garlic:1},'FERMENT',4,'白菜×3、辣椒×1、蒜×1；盐水密封等待。',{salt:true});
dish('meat_skewers','烤肉串',{skewers:3},'BAKE',3,'调理台用肉×3、洋葱×1制成生肉串×3；盐、孜然，烤制翻面。',{salt:true});
dish('honey_roast_duck','蜂蜜烤鸭',{duck:3},'BAKE',3,'鸭肉×3；蜂蜜、盐、酱油，刷料烤制。',{salt:true,soy:true});
dish('roast_potatoes','烤土豆',{potato:3},'BAKE',3,'土豆×3；少量油盐，烤至松软。',{oil:true,salt:true});
dish('noodle_soup','手擀汤面',{dough:2,cabbage:1},'BOIL',2,'面团×2擀切，青菜×1；加水煮。',{salt:true});
dish('pork_dumplings','猪肉白菜饺子',{dumplings:2},'BOIL,STEAM',2,'调理台用面团×2、肉末×1、白菜×1包成生饺子×2；盐水煮或蒸。',{salt:true});
dish('steamed_buns','馒头',{dough_fermented:2},'STEAM',2,'面团×2与引子在发酵缸发酵后蒸熟。');
dish('homestyle_bread','家常面包',{dough_fermented:2},'BAKE',2,'发酵面团×2、少量糖，在烤盘上烘烤。');
dish('apple_pie','苹果派',{pie:1},'BAKE',3,'调理台用面团×2、苹果×2、糖装馅，生苹果派放烤盘烤熟。');
const preparation={
 garlic_greens:{cuts:{garlic:['MINCE'],cabbage:['DICE','SHRED']},steps:[{ingredient:'garlic',temperature:100,hot_oil:true},{ingredient:'cabbage',after:'garlic',cooked:.1}]},
 scallion_omelette:{cuts:{scallion:['DICE','MINCE']},finishing:{turn:true,cold_mixes:2}},
 pan_fried_fish:{steps:[{ingredient:'fish',temperature:110,hot_oil:true}],finishing:{turn:true,condiments:['pepper_powder']}},
 braised_pork:{cuts:{pork:['DICE'],ginger:['SLICE']},minimum_water:.1,covered_seconds:15,staple:{sugar:1,cooked:1.2,low_heat_seconds:10},steps:[{ingredient:'pork',temperature:140,hot_oil:true,caramel:true}]},
 braised_eggplant:{cuts:{eggplant:['DICE'],garlic:['MINCE']},steps:[{ingredient:'eggplant',after:'garlic',temperature:100,hot_oil:true}],staple:{sugar:1},finish_seasonings:{soy:.7,sugar:.7}},
 cucumber_salad:{cuts:{cucumber:['DICE','SLICE'],garlic:['MINCE']},finishing:{condiments:['sesame_oil'],maximum_temperature:60}},
 pickled_cabbage:{cuts:{cabbage:['SHRED'],chili:['DICE','SLICE'],garlic:['MINCE']},minimum_water:1,finishing:{ferment_seconds:1200,maximum_temperature:60}},
 meat_skewers:{finishing:{condiments:['cumin_powder'],turn:true,roast_seconds:15}},
 honey_roast_duck:{finishing:{condiments:['honey'],marinated_seconds:10,roast_seconds:15},staple:{oven_seconds:15},finish_seasonings:{honey:.5}},
 roast_potatoes:{cuts:{potato:['DICE','SLICE']},staple:{oven_seconds:15,cooked:1.2},finishing:{roast_seconds:15}},
 cooked_rice:{minimum_water:.1,staple:{water_min:1,water_max:3,cooked:1,low_heat_seconds:10}},
 rice_porridge:{minimum_water:1,staple:{water_min:4,cooked:1.4}},
 noodle_soup:{cuts:{dough:['SHRED'],cabbage:['SHRED','DICE']},minimum_water:.1,staple:{rolled:true}},
 pork_dumplings:{minimum_water:.1},
 steamed_buns:{minimum_water:.1,covered_seconds:15},
 homestyle_bread:{staple:{sugar:1,oven_seconds:15}},
 apple_pie:{staple:{oven_seconds:15}},
 fried_rice:{cuts:{scallion:['DICE','SHRED','MINCE']},steps:[{ingredient:'egg',temperature:110,hot_oil:true},{ingredient:'rice_cooked',after:'egg',cooked:.4},{ingredient:'scallion',after:'rice_cooked',cooked:0}]},
 tomato_eggs:{steps:[{ingredient:'egg',temperature:110,hot_oil:true},{ingredient:'tomato',after:'egg',cooked:.4}]},
 sour_spicy_potato:{cuts:{potato:['SHRED'],garlic:['MINCE']},steps:[{ingredient:'potato',after:'garlic',temperature:100,hot_oil:true}],finish_seasonings:{vinegar:.7}},
 mapo_tofu:{minimum_water:.1,cuts:{pork:['MINCE'],tofu:['DICE']},steps:[{ingredient:'pork',temperature:100,hot_oil:true},{ingredient:'tofu',after:'pork',cooked:.6}]},
 steamed_fish:{cuts:{ginger:['SLICE'],scallion:['DICE','SHRED']},minimum_water:.1,covered_seconds:15,finish_seasonings:{soy:1}},
 boiled_shrimp:{cuts:{ginger:['SLICE']},minimum_water:.1,steps:[{ingredient:'shrimp',after:'ginger',temperature:95,boiling_water:true}]},
 steamed_crab:{cuts:{ginger:['SLICE']},minimum_water:.1,covered_seconds:15},
 mushroom_soup:{cuts:{mushroom:['SLICE','DICE']},minimum_water:.1},
 tomato_egg_soup:{cuts:{tomato:['DICE','SLICE']},minimum_water:.1,steps:[{ingredient:'egg',after:'tomato',cooked:.5}]},
 carrot_meat_stew:{cuts:{carrot:['DICE'],pork:['DICE'],ginger:['SLICE']},minimum_water:.1,covered_seconds:15,steps:[{ingredient:'carrot',after:'pork',cooked:.35}]},
 beef_potato_stew:{cuts:{potato:['DICE'],beef:['DICE'],onion:['DICE','SLICE']},minimum_water:.1,covered_seconds:15,steps:[{ingredient:'potato',after:'beef',cooked:.35}]},
 pumpkin_soup:{cuts:{pumpkin:['DICE']},minimum_water:.1,steps:[{ingredient:'milk',after:'pumpkin',cooked:.8}]}
};
for(const recipe of dishes){Object.assign(recipe,preparation[recipe.id]||{});recipe.stirs=recipe.methods.includes('STIR_FRY')?3:recipe.methods.includes('FRY')||recipe.methods.includes('MIX')?1:0;}
dishes.find(r=>r.id==='rice_porridge').stirs=2;
dishes.find(r=>r.id==='cucumber_salad').stirs=3;
dishes.find(r=>r.id==='braised_pork').oil=true;
dishes.find(r=>r.id==='meat_skewers').oil=true;
for(const[id,guide]of Object.entries({
 garlic_greens:'白菜块或丝×2、蒜末×1。油锅至少100°C先下蒜末，稍炒至一成熟再下白菜，油盐适量，全部投料后翻炒3次，熟透装盘。',
 scallion_omelette:'蛋×2、葱花（切块或剁碎）×1。冷平底锅放油、蛋、葱和盐，点火前用筷子或铲搅匀2次；加热成形至至少三成半熟后用铲或夹子翻面，另一面继续加热至少三成，全部熟透盛出。冷锅乱翻不算有效翻面。',
 pan_fried_fish:'鱼×2，油、盐和胡椒粉。先把油锅预热到110°C，再下两份鱼；鱼至少三成半熟成形后用铲或夹子翻面，再继续煎另一面至少三成，熟透盛出。辣椒粉不能代替胡椒。',
 braised_pork:'猪肉块×3、姜片×1，少量油、糖和酱油。空汤锅切到炖煮，先放油糖，中火升到145–185°C炒糖色3秒（准星有进度，过热会焦）；先依次下3块肉，再下姜，随后用水壶补2份水、加酱油。加盖转小火至少10秒、盖锅微沸15秒，继续炖到软烂进度100%盛出。糖水炖肉不算炒糖色。',
 braised_eggplant:'茄子块×3、蒜末×1。油锅至少100°C先下蒜、再下茄子；茄子至少七成熟软化后再加酱油和糖，全部投料后翻炒3次，熟透装盘。',
 cucumber_salad:'黄瓜块或片×2、蒜末×1。调理碗冷拌，放盐、醋和芝麻油，搅拌3次即可装盘；菜籽油不能代替芝麻香油，全程不要把食材加热超过60°C。',
 pickled_cabbage:'白菜丝×3、辣椒片或块×1、蒜末×1、盐2份。腌菜缸加一水桶水，放入处理过的食材与盐，空手密封后等待1200秒；未密封暂停，重新放料从头计时，不能先空缸等待再瞬间腌好。产4份泡菜，打开后潜行空手取出。',
 meat_skewers:'调理台放肉块×3、洋葱片或块×1、木棍×3，持长筷加工2次得到生肉串×3。烤盘装料，加油盐和孜然粉；至少140°C烤15秒，肉成形后用夹子翻面并继续加热另一面至少三成，全部熟透盛出。',
 honey_roast_duck:'鸭肉×3先放烤炉里的烤盘，加酱油与蜂蜜，在点火前冷腌10秒；加盐、加煤烤。至少五成熟后再刷一份蜂蜜，继续至少140°C烤15秒且熟透，盛3份。提前刷蜜不能代替后半程刷料，蜂蜜瓶保留。',
 roast_potatoes:'土豆块或片×3，油盐适量。烤盘装入烤炉，加煤升到至少140°C烤15秒，并继续烤到软烂进度100%盛3份。整块生土豆不能跳过切配。'
}))dishes.find(r=>r.id===id).guide=guide;
for(const[id,guide]of Object.entries({
 cooked_rice:'米×1。用水壶加1–3份水后下米，煮沸后转小火，加盖至少焖10秒并煮到熟透；盛出2碗。桶装满6份水更适合煮粥，米饭不要加太多水。',
 rice_porridge:'米×1。先加至少4份水再下米（可用一水桶6份），汤锅空手点顶部切到炖煮；煮至准星软烂进度100%，全部投料后用勺搅拌至少2次，保留至少1份水，盛3碗。普通熟度达到100%仍需继续煮软，提前盛出会提示继续加热且不消耗碗。',
 noodle_soup:'面团×2。在砧板用擀面杖擀开，再潜行持刀切换至切丝，切4次得到生面条。两份面条、切丝或切块白菜×1，加水、盐煮熟装碗；整块面团不能成为手擀面。',
 pork_dumplings:'面团×2先在砧板擀开（不用切），猪肉×1剁碎，白菜×1切丝或剁碎。投入调理台，持擀面杖右键包制2次得到生饺子×2；下锅加水盐煮熟，或蒸笼加盖蒸熟。',
 steamed_buns:'面团×2、引子×1放入发酵缸，空手盖好，等待120秒后潜行空手取出两份发酵面团（引子返还）。蒸笼有水、盖锅至少15秒并蒸至熟透，盛2份。',
 homestyle_bread:'面团×2和引子在发酵缸盖好发酵120秒；取两份发酵面团，烤盘嵌入烤炉后放入、加糖1份、加煤。至少140°C烤15秒且全部熟透，盛2份；灶面烤盘不能代替烤炉。',
 apple_pie:'面团×2在砧板擀开，苹果×2切片、糖×1。投入调理台，持擀面杖右键包馅2次，潜行空手取出生苹果派。烤盘嵌入烤炉，放生派、加煤，至少140°C烤15秒且派熟透，盛3份。'
}))dishes.find(r=>r.id===id).guide=guide;
dishes.find(r=>r.id==='fried_rice').guide='米饭×2、蛋×1、切碎的葱×1。先放油，把锅预热到110°C，再下蛋炒至至少四成熟；再加两份米饭，最后下葱。全部加完后翻炒至少3次，加盐，熟透后装盘。生米须先加水煮成米饭。';
dishes.find(r=>r.id==='sour_spicy_potato').guide='土豆丝×2、辣椒×1、蒜末×1。砧板切法：土豆切丝、蒜剁碎。先下蒜，油锅至少100°C再下土豆丝和辣椒；翻炒至少3次，七成熟后淋醋，加盐，熟透装盘。';
dishes.find(r=>r.id==='mapo_tofu').guide='豆腐块×2、猪肉末×1、辣椒×1、花椒×1，油和酱油各1次。砧板默认切块，潜行持刀可切换剁碎。油锅至少100°C下肉末，炒至六成熟；再加1份水、豆腐、辣椒、花椒和酱油。保持有少量汤汁，轻推至少3次，最后加入的香料也要熟透。用碗或陶盘盛2份，食用返原餐具。';
dishes.find(r=>r.id==='tomato_eggs').guide='番茄×2、蛋×2。放油，锅温至少110°C再下两份蛋；蛋至少四成熟再下番茄，油盐适量，全部加完后翻炒至少3次，熟透装盘。';
for(const [id,guide]of Object.entries({
 steamed_fish:'鱼×2、姜片×1、切块或切丝的葱×1。蒸笼加水，放食材后加盖，保持蒸汽加热至少15秒并蒸至全部熟透，再淋酱油、加盐装盘。提前淋酱油不计出锅调味。',
 boiled_shrimp:'河虾×3、姜片×1。锅中加满水，先下姜，等显示沸腾且至少95°C后下虾，加盐，熟透盛出。冷水先下虾不会成为白灼虾。',
 steamed_crab:'河蟹×2、姜片×1。蒸笼加水、放食材、加盖；有蒸汽后至少保持15秒并蒸至全部熟透，用醋调味装盘。未盖锅不会蒸熟。',
 mushroom_soup:'切片或切块菌菇×3。汤锅加水、放菌菇和盐，煮至全部熟透盛出；完整菌菇不计此菜的处理步骤。',
 tomato_egg_soup:'番茄块或片×2、蛋×1。先加水煮番茄至至少五成熟，再下蛋、加盐，所有食材熟透后盛汤。',
 carrot_meat_stew:'肉块×2、姜片×1、胡萝卜块×2。加水先炖肉和姜，肉至少三成半熟后下胡萝卜，加盐、加盖微沸至少15秒，继续炖到每份熟透。',
 beef_potato_stew:'牛肉块×2、土豆块×2、洋葱片或块×1。加水先炖牛肉，至少三成半熟后下土豆与洋葱，加盐，加盖微沸至少15秒，全部熟透后盛出。',
 pumpkin_soup:'南瓜块×2、牛奶×1。汤锅先加水煮南瓜到至少八成熟，再倒牛奶，加盐，继续加热至全部熟透。奶桶返还，南瓜块可以直接从砧板投入锅内。'
}))dishes.find(r=>r.id===id).guide=guide;
item('mixed_stir_fry','家常杂炒','bowl',{nutrition:5,meal:true});item('mixed_stew','家常炖汤','bowl',{nutrition:5,meal:true});item('burnt_food','焦黑食物','charcoal',{nutrition:1,meal:true});

const q=id=>id.includes(':')?id:`${ns}:${id}`;
function process(station,inputs,outputs,seconds=5,water=0){processes.push({station,inputs:Object.fromEntries(Object.entries(inputs).map(([k,v])=>[q(k),v])),outputs:Object.fromEntries(Object.entries(outputs).map(([k,v])=>[q(k),v])),seconds,water});}
process('mill',{rice_paddy:1},{rice:1,rice_husk:1});process('mill',{'minecraft:wheat':1},{flour:1});process('mill',{salt_ore_chunk:1},{salt_crystals:4});
process('mill',{salt_crystals:16,empty_jar:1},{salt:1});
process('mill',{salt_crystals:4,empty_jar:1},{salt:1});processes[processes.length-1].output_uses={[q('salt')]:4};
process('mill',{dried_chili:1},{chili_powder:1});process('mill',{black_pepper:1},{pepper_powder:1});process('mill',{cumin:1},{cumin_powder:1});process('mill',{dried_shiitake:1},{mushroom_powder:1});
process('press',{rapeseed:8,'minecraft:glass_bottle':1},{rapeseed_oil:1,oil_cake:2});process('press',{sesame:16,'minecraft:glass_bottle':1},{sesame_oil:1,oil_cake:2});
process('mix',{flour:2},{dough:2},5,1);process('mix',{'minecraft:book':1},{recipe_book:1});
process('mix',{dough:2,'minecraft:porkchop':1,cabbage:1},{raw_dumplings:2},10);
Object.assign(processes[processes.length-1],{tool:'rolling_pin',rolled:[q('dough')],cuts:{'minecraft:porkchop':['MINCE'],[q('cabbage')]:['SHRED','MINCE']}});
process('mix',{'minecraft:porkchop':3,onion:1,'minecraft:stick':3},{raw_skewers:3},10);
Object.assign(processes[processes.length-1],{tool:'chopsticks',cuts:{'minecraft:porkchop':['DICE'],[q('onion')]:['DICE','SLICE']}});
process('mix',{dough:2,'minecraft:apple':2,'minecraft:sugar':1},{raw_apple_pie:1},10);
Object.assign(processes[processes.length-1],{tool:'rolling_pin',rolled:[q('dough')],cuts:{'minecraft:apple':['SLICE']}});
process('ferment',{dough:2,starter:1},{fermented_dough:2,starter:1},120);
process('dry',{chili:1},{dried_chili:1},120);process('dry',{shiitake:1},{dried_shiitake:1},120);
process('ferment',{cooked_rice:1,shiitake:1,'minecraft:sugar':1},{starter:2},1200);
process('ferment',{cooked_rice:1,'minecraft:apple':1},{vinegar_starter:2},1200,1);
process('ferment',{cooked_soybean:4,flour:2,salt:2,starter:1,empty_jar:1},{soy_mash:1,starter:1},2400,1);
process('mix',{soy_mash:1,'minecraft:glass_bottle':1},{soy_sauce:1,empty_jar:1});processes[processes.length-1].tool='sieve';
process('ferment',{cooked_rice:4,vinegar_starter:1,empty_jar:1},{vinegar_mash:1,vinegar_starter:1},2400,1);
process('mix',{vinegar_mash:1,'minecraft:glass_bottle':1},{rice_vinegar:1,empty_jar:1});processes[processes.length-1].tool='sieve';
process('ferment',{soybean:4},{soaked_soybean:4},120,1);processes[processes.length-1].exclusive=true;
process('mill',{soaked_soybean:4},{raw_bean_pulp:1},10,1);
process('mix',{boiled_bean_pulp:1,empty_jar:1},{soy_milk:1,okara:1});processes[processes.length-1].tool='sieve';
process('mix',{soy_curds:1},{tofu:2},20);processes[processes.length-1].tool='wooden_lid';
process('pickle',{cabbage:3,chili:1,garlic:1,salt:2},{pickled_cabbage:4},1200,1);
Object.assign(processes[processes.length-1],{cuts:{[q('cabbage')]:['SHRED'],[q('chili')]:['SLICE','DICE'],[q('garlic')]:['MINCE']}});
process('evaporate',{}, {salt_crystals:4},120,6);processes[processes.length-1].saltwater=true;

item('fat_cracklings','油渣','cooked_porkchop',{nutrition:2});
item('lard','猪油罐','glass_bottle',{seasoning:'oil',uses:8});
item('cooked_soybean','熟大豆','cocoa_beans',{ingredient:'soybean_cooked',ready:true,nutrition:3});
item('soy_mash','酱醪（待过滤）','honey_bottle',{unstackable:true});
item('vinegar_mash','醋醪（待过滤）','honey_bottle',{unstackable:true});
item('soaked_soybean','泡发大豆','wheat_seeds');
item('raw_bean_pulp','生豆糊','clay_ball',{ingredient:'bean_pulp',needs_water:true,resistance:1.5});
item('boiled_bean_pulp','煮熟豆糊','clay_ball');
item('soy_milk','熟豆浆（待点制）','honey_bottle',{ingredient:'soy_milk',unstackable:true,container:'empty_jar',liquid_water:1});
item('soy_curds','豆花（待压模）','snowball');
item('okara','熟豆渣','wheat',{ingredient:'okara',ready:true});
for(const original of items)if(sampleBases.includes(original.id))original.food_model_height=original.id==='mixed_stew'?6:4;
for(const original of [...items]) {
 if(original.uses>0){
  item('used_'+original.id,original.name+(original.seasoning||original.id==='water_jug'?'（50%）':'（已使用）'),original.texture,{...original,id:'used_'+original.id,name:original.name+'（已使用）',variant_of:original.id,state:'used'});
  if(!original.seasoning&&original.id!=='water_jug')item('broken_'+original.id,original.name+'（损坏）',original.texture,{variant_of:original.id,state:'broken',unstackable:true});
 }
 if(original.nutrition>0&&original.id!=='burnt_food'){
  item('stale_'+original.id,'不新鲜的'+original.name,original.texture,{...original,id:'stale_'+original.id,name:'不新鲜的'+original.name,variant_of:original.id,state:'stale',nutrition:Math.max(1,Math.floor(original.nutrition/2))});
  item('spoiled_'+original.id,'变质的'+original.name,original.texture,{...original,id:'spoiled_'+original.id,name:'变质的'+original.name,variant_of:original.id,state:'spoiled',nutrition:0});
 }
}
write(`data/${ns}/catalog.json`,{items,stations,crops,dishes,processes});
const vanillaTools={rice_paddy:'wheat',chopsticks:'stick',rolling_pin:'bamboo',water_jug:'water_bucket',heat_cloth:'leather',thermometer:'clock',ceramic_bowl:'bowl',wooden_lid:'oak_trapdoor',empty_water_jug:'bucket',empty_jar:'glass_bottle',pan_fried_fish:'cooked_cod',roast_potatoes:'baked_potato'};
write(`assets/${ns}/models/item/pot_water.json`,{parent:'minecraft:block/block',textures:{water:'minecraft:block/water_still'},elements:[{from:[3,0,3],to:[13,.01,13],faces:{up:{texture:'#water',tintindex:0,uv:[0,0,16,16]}}}]});
write(`assets/${ns}/items/pot_water.json`,{model:{type:'minecraft:model',model:`${ns}:item/pot_water`,tints:[{type:'minecraft:constant',value:-12618012}]}});
write(`assets/${ns}/models/item/pot_soy_milk.json`,{parent:'minecraft:block/block',textures:{milk:'minecraft:block/white_concrete'},elements:disk(0,.01,4.4,'#milk')});
write(`assets/${ns}/items/pot_soy_milk.json`,{model:{type:'minecraft:model',model:`${ns}:item/pot_soy_milk`}});
write(`assets/${ns}/models/item/pot_mapo_sauce.json`,{parent:'minecraft:block/block',textures:{sauce:'minecraft:block/red_terracotta'},elements:disk(0,.01,4.4,'#sauce')});
write(`assets/${ns}/items/pot_mapo_sauce.json`,{model:{type:'minecraft:model',model:`${ns}:item/pot_mapo_sauce`}});
write(`assets/${ns}/models/item/food/sichuan_pepper.json`,{parent:'minecraft:block/block',textures:{husk:'minecraft:block/red_terracotta',seed:'minecraft:block/brown_terracotta'},elements:[box([7,0,7],[9,1,9],'#husk'),box([7.5,1,7.5],[8.5,1.5,8.5],'#seed')]});
write(`assets/${ns}/items/food/sichuan_pepper.json`,{model:{type:'minecraft:model',model:`${ns}:item/food/sichuan_pepper`}});
for(const[id,texture]of Object.entries({pot_oil:'honey_block_top',pot_caramel:'brown_concrete',pot_burnt_sugar:'black_concrete'})){
 write(`assets/${ns}/models/item/${id}.json`,{parent:'minecraft:block/block',textures:{surface:`minecraft:block/${texture}`},elements:[{from:[3,0,3],to:[13,.01,13],faces:{up:{texture:'#surface',uv:[0,0,16,16]}}}]});
 write(`assets/${ns}/items/${id}.json`,{model:{type:'minecraft:model',model:`${ns}:item/${id}`}});
}
for(const id of ['dough_sheet','raw_noodles']){
 const cuboid=(from,to)=>({from,to,faces:Object.fromEntries(['up','down','north','south','east','west'].map(f=>[f,{texture:'#dough'}]))});
 const elements=id==='dough_sheet'?[cuboid([2,2,7],[14,14,8])]:[2,5,8,11].map(x=>cuboid([x,2,7],[x+1.5,14,8]));
 write(`assets/${ns}/models/item/${id}.json`,{parent:'minecraft:block/block',textures:{dough:'minecraft:block/sandstone_top'},elements});
 write(`assets/${ns}/items/${id}.json`,{model:{type:'minecraft:model',model:`${ns}:item/${id}`}});
}
for(const it of items){
 write(`assets/${ns}/items/${it.id}.json`,{model:{type:'minecraft:model',model:`${ns}:item/${it.id}`}});
 const vanilla=(it.variant_of||it.id)==='thermometer'?'clock_00':vanillaTools[it.variant_of||it.id];
 const sample=sampleFoodModel(it.id);
 let model=sample||dishModel(it.id)||tofuModel(it.id)||toolModel(it.id)|| (vanilla?{parent:`minecraft:item/${vanilla}`} : it.id==='fried_rice'?{parent:'minecraft:item/generated',textures:{layer0:`${ns}:item/fried_rice`}}:inventoryModel(it,items.indexOf(it)));
 if(it.id==='wooden_lid')model={parent:'minecraft:block/oak_trapdoor_bottom'};
 if(it.variant_of&&!sample) {
  model={parent:`${ns}:item/${it.variant_of}`};
  const badge=inventoryModel({id:it.id,variant_of:it.variant_of,state:it.state},items.indexOf(it));
  write(`assets/${ns}/models/item/${it.id}_state.json`,badge);
  write(`assets/${ns}/items/${it.id}.json`,{model:{type:'minecraft:composite',models:[{type:'minecraft:model',model:`${ns}:item/${it.variant_of}`},{type:'minecraft:model',model:`${ns}:item/${it.id}_state`}]}});
 }
 if(it.id.includes('gas_canister'))model={parent:'minecraft:block/block',textures:{base:'minecraft:block/'+(it.id==='empty_gas_canister'?'gray':it.id==='partial_gas_canister'?'yellow':'green')+'_concrete',metal:'minecraft:block/iron_block',dark:'minecraft:block/black_concrete',valve:'minecraft:block/copper_block'},elements:[box([4,1,4],[12,3,12],'#dark'),box([3,3,3],[13,11,13]),box([5,11,5],[11,13,11]),box([7,13,7],[9,15,9],'#valve'),box([5,14,7],[11,15,9],'#dark'),box([5,6,2.9],[11,9,3],'#metal')]};
 if(it.id==='lard')model={parent:'minecraft:block/block',textures:{base:'minecraft:block/white_concrete',rim:'minecraft:block/light_gray_concrete',lid:'minecraft:block/oak_planks'},elements:[box([3,2,3],[13,11,13]),box([2,2,2],[14,3,14],'#rim'),box([2,11,2],[14,13,14],'#lid'),box([5,5,2.9],[11,8,3],'#rim')]};
 if(it.id==='rendered_fat')model={parent:'minecraft:block/block',textures:{base:'minecraft:block/white_concrete',skin:'minecraft:block/pink_concrete'},elements:[box([2,3,3],[14,6,13],'#skin'),box([2,6,3],[14,10,13]),box([4,10,4],[12,12,12])]};
 if(it.id==='soy_mash'||it.id==='vinegar_mash'){
  const soy=it.id==='soy_mash';
  model={parent:'minecraft:block/block',textures:{base:'minecraft:block/terracotta',rim:'minecraft:block/oak_planks',liquid:'minecraft:block/'+(soy?'brown_terracotta':'orange_terracotta'),grain:'minecraft:block/'+(soy?'yellow_terracotta':'white_terracotta')},elements:[box([3,2,3],[13,10,13]),box([2,10,2],[14,12,14],'#rim'),box([3,12,3],[13,12.1,13],'#liquid'),...(soy?[box([4,12.1,4],[7,13.5,7],'#grain'),box([9,12.1,7],[12,13.2,10],'#grain')]:[4,7,10].map(x=>box([x,12.1,5],[x+1,12.7,11],'#grain')))]};
 }
 if(it.id==='fat_cracklings')model={parent:'minecraft:block/block',textures:{base:'minecraft:block/terracotta',gold:'minecraft:block/yellow_terracotta'},elements:[box([1,2,3],[7,5,9]),box([2,5,4],[6,6,8],'#gold'),box([9,3,6],[15,7,12]),box([10,7,7],[14,8,11],'#gold'),box([4,7,9],[10,10,15]),box([5,10,10],[9,11,14],'#gold')]};
 write(`assets/${ns}/models/item/${it.id}.json`,model);
 if(it.id==='pot_lid')write(`assets/${ns}/models/item/${it.id}.json`,{parent:`${ns}:block/lid`});
}
function box(from,to,texture='#base'){const face={texture};return{from,to,faces:{north:face,south:face,east:face,west:face,up:face,down:face}};}
const models={
 stove:[box([0,0,0],[16,12,16]),box([1,12,1],[15,13,15],'#metal'),box([3,13,3],[13,14,13],'#dark')],
 oven:[box([0,0,0],[16,2,16]),box([0,2,0],[2,14,16]),box([14,2,0],[16,14,16]),box([2,2,14],[14,14,16]),box([0,14,0],[16,16,16]),box([2,3,1],[14,4,14],'#metal')],
 board:[box([0,0,1],[16,1,15])],pot:[box([2,0,2],[14,1,14],'#metal'),box([1,1,1],[3,7,15],'#metal'),box([13,1,1],[15,7,15],'#metal'),box([3,1,1],[13,7,3],'#metal'),box([3,1,13],[13,7,15],'#metal')],
 wok:[box([4,0,4],[12,1,12],'#dark'),box([2,1,2],[14,2,14],'#dark'),box([1,2,1],[3,4,15],'#metal'),box([13,2,1],[15,4,15],'#metal'),box([3,2,1],[13,4,3],'#metal'),box([3,2,13],[13,4,15],'#metal'),box([7,2,15],[9,3,22])],
 pan:[box([1,0,1],[15,1,15],'#metal'),box([0,1,0],[2,3,16],'#metal'),box([14,1,0],[16,3,16],'#metal'),box([2,1,0],[14,3,2],'#metal'),box([2,1,14],[14,3,16],'#metal')],
 steamer:[box([1,0,1],[15,1,15]),box([1,1,1],[3,7,15]),box([13,1,1],[15,7,15]),box([3,1,1],[13,7,3]),box([3,1,13],[13,7,15])],
 counter:[box([0,0,0],[16,14,16]),box([0,14,0],[16,16,16],'#stone')],
 cabinet:[box([0,0,0],[16,16,16]),box([7,6,-.1],[9,10,.1],'#metal')],
 sink:[box([0,0,0],[16,10,16]),box([0,10,0],[3,16,16],'#stone'),box([13,10,0],[16,16,16],'#stone'),box([3,10,0],[13,16,3],'#stone'),box([3,10,13],[13,16,16],'#stone')],
 crock:[box([2,0,2],[14,1,14],'#clay'),box([1,1,1],[15,13,15],'#clay'),box([2,13,2],[14,14,14])],
 mill:[box([1,0,1],[15,3,15],'#stone'),box([2,3,2],[14,7,14],'#stone'),box([3,7,3],[13,10,13],'#stone'),box([10,10,5],[12,14,7])],
 press:[box([0,0,0],[16,3,16]),box([1,3,1],[3,16,15]),box([13,3,1],[15,16,15]),box([3,13,2],[13,16,14]),box([6,5,6],[10,13,10],'#metal')],
 rack:[box([1,0,3],[3,16,5]),box([13,0,3],[15,16,5]),box([0,2,1],[16,3,15]),box([0,8,1],[16,9,15]),box([0,14,1],[16,15,15])],
 table:[box([0,13,0],[16,16,16]),box([1,0,1],[3,13,3]),box([13,0,1],[15,13,3]),box([1,0,13],[3,13,15]),box([13,0,13],[15,13,15])],
 chair:[box([2,6,2],[14,8,14]),box([2,8,12],[14,16,14]),box([2,0,2],[4,6,4]),box([12,0,2],[14,6,4]),box([2,0,12],[4,6,14]),box([12,0,12],[14,6,14])],
 plate:[box([2,0,2],[14,1,14],'#clay')],stand:[box([2,0,2],[14,2,14]),box([7,2,7],[9,12,9]),box([1,12,3],[15,14,13])],
 hood:[box([0,0,0],[16,4,16],'#metal'),box([4,4,4],[12,16,12],'#metal')],chimney:[box([4,0,4],[12,16,12],'#stone')]
};
const textures={rim:'minecraft:block/gray_concrete',base:'minecraft:block/oak_planks',metal:'minecraft:block/iron_block',dark:'minecraft:block/black_concrete',stone:'minecraft:block/smooth_stone',clay:'minecraft:block/terracotta',particle:'minecraft:block/oak_planks',copper:'minecraft:block/copper_block',blue:'minecraft:block/blue_concrete',ice:'minecraft:block/light_blue_concrete',grain:'minecraft:block/yellow_concrete',leaf:'minecraft:block/green_concrete',salt:'minecraft:block/white_concrete',dirt:'minecraft:block/dirt',firebox:'minecraft:block/furnace_front'};
for(const s of stations){
 const ownTextures={...textures,base:s.kind==='stove'?'minecraft:block/bricks':textures.base};
 if(s.kind==='stove'||s.id==='gas_stove')ownTextures.metal='minecraft:block/polished_andesite';
 if(s.id==='fermentation_crock')ownTextures.clay='minecraft:block/brown_terracotta';
 if(s.id==='pickling_crock')ownTextures.clay='minecraft:block/green_terracotta';
 if(s.id==='steamer')ownTextures.base='minecraft:block/bamboo_planks';
 if(s.id==='cabinet')ownTextures.base='minecraft:block/spruce_planks';
 const geometry=vessel(s.id)||stationGeometry(s,models);
 if(['clay_pot','soup_pot','wok','frying_pan'].includes(s.id)){const y={clay_pot:7,soup_pot:7,wok:4,frying_pan:3}[s.id];if(s.id==='clay_pot')ownTextures.rim='minecraft:block/orange_terracotta';for(const e of geometry)if(e.to[1]===y)e.faces.up={texture:'#rim'};}
 write(`assets/${ns}/models/block/${s.id}.json`,{parent:'minecraft:block/block',textures:ownTextures,elements:geometry});
 const parts=[];for(const [f,y]of[['north',0],['east',90],['south',180],['west',270]]){
  parts.push({when:{facing:f,...(s.kind==='stove'||s.id==='oven'?{heat:'0'}:{})},apply:{model:`${ns}:block/${s.id}`,y}});
  if(s.kind==='stove'||s.id==='oven')parts.push({when:{facing:f,heat:'1|2|3'},apply:{model:`${ns}:block/${s.id}_lit`,y}});
 }
 if(s.kind==='stove')write(`assets/${ns}/models/block/${s.id}_lit.json`,{parent:`${ns}:block/${s.id}`,textures:{firebox:'minecraft:block/furnace_front_on',flame:'minecraft:block/campfire_fire'},elements:[...stationGeometry(s,models),{from:[4,3,-.04],to:[12,8,-.04],faces:{north:{texture:'#flame',uv:[0,0,16,16]}}}]});
 if(s.id==='gas_stove')parts.push({when:{heat:'1|2|3'},apply:{model:`${ns}:block/gas_flame`}});
 if(s.id==='oven')write(`assets/${ns}/models/block/oven_lit.json`,{parent:`${ns}:block/oven`,textures:{firebox:'minecraft:block/furnace_front_on'}});
 if(s.kind!=='stove'&&s.id!=='gas_stove'){
  const lidModel=s.model==='crock'?'crock_lid':s.kind==='cook'?`lid_${s.id}`:'lid';
  if(s.kind==='cook'){const y={frying_pan:3,wok:4,baking_tray:2,mixing_bowl:4}[s.id]||7;write(`assets/${ns}/models/block/${lidModel}.json`,{parent:'minecraft:block/block',textures,elements:vesselLid(false,y)});}
  parts.push({when:{lid:'true'},apply:{model:`${ns}:block/${lidModel}`}});
 }
 write(`assets/${ns}/blockstates/${s.id}.json`,{multipart:parts});
 write(`assets/${ns}/models/item/${s.id}.json`,{parent:`${ns}:block/${s.id}`});write(`assets/${ns}/items/${s.id}.json`,{model:{type:'minecraft:model',model:`${ns}:item/${s.id}`}});
 if(s.id==='double_stove')write(`assets/${ns}/models/item/${s.id}.json`,{parent:'minecraft:block/block',textures:ownTextures,elements:[0,8].flatMap(offset=>stationGeometry(s,models).map(e=>({...e,from:e.from.map((v,i)=>i===0?v*.5+offset:v),to:e.to.map((v,i)=>i===0?v*.5+offset:v)})))});
 write(`data/${ns}/loot_table/blocks/${s.id}.json`,{type:'minecraft:block',pools:[{rolls:1,entries:[{type:'minecraft:item',name:`${ns}:${s.id}`}],conditions:[{condition:'minecraft:survives_explosion'}]}]});
 if(s.id==='double_stove')write(`data/${ns}/loot_table/blocks/${s.id}.json`,{type:'minecraft:block',pools:[{rolls:1,entries:[{type:'minecraft:item',name:`${ns}:${s.id}`}],conditions:[{condition:'minecraft:block_state_property',block:`${ns}:double_stove`,properties:{secondary:'false'}}]}]});
 if(s.id==='gas_deposit')write(`data/${ns}/loot_table/blocks/${s.id}.json`,{type:'minecraft:block',pools:[]});
}
write(`assets/${ns}/models/block/fuel_bed.json`,{textures:{coal:'minecraft:block/coal_block'},elements:[box([4,2,3],[12,4,11],'#coal')]});
write(`assets/${ns}/models/block/firebox_lit.json`,{textures:{fire:'minecraft:block/orange_concrete',ember:'minecraft:block/yellow_concrete'},elements:[box([5,4,4],[7,8,5],'#fire'),box([9,4,3],[11,7,4],'#ember'),box([7,4,7],[9,9,8],'#fire')]});
write(`assets/${ns}/models/block/gas_flame.json`,{textures:{fire:'minecraft:block/soul_fire_0'},elements:[-45,45].map(angle=>({from:[4,13,8],to:[12,14,8],rotation:{origin:[8,13,8],axis:'y',angle},faces:{north:{texture:'#fire',uv:[0,0,16,16]},south:{texture:'#fire',uv:[0,0,16,16]}}}))});
for(const [c,t]of[['raw','minecraft:block/pink_concrete'],['cooked','minecraft:block/yellow_terracotta'],['burnt','minecraft:block/black_concrete']])write(`assets/${ns}/models/block/contents_${c}.json`,{textures:{base:t},elements:[box([4,1.05,4],[7,2.1,7]),box([9,1.05,4],[12,2.1,7]),box([5,1.05,9],[9,2.1,12])]});
write(`assets/${ns}/models/block/lid.json`,{parent:'minecraft:block/block',textures,elements:vesselLid(false,7)});
write(`assets/${ns}/models/block/crock_lid.json`,{parent:'minecraft:block/block',textures,elements:vesselLid(true,14)});
for(const family of foodFamilies)for(const cut of ['whole','slice','dice','shred','mince'])for(const stage of foodStages){
 const id=`food/${family}_${cut}_${stage}`;
 write(`assets/${ns}/models/item/${id}.json`,foodModel(family,cut,stage));
 write(`assets/${ns}/items/${id}.json`,{model:{type:'minecraft:model',model:`${ns}:item/${id}`}});
}
for(const c of crops){
 const variants={};for(let age=0;age<8;age++){variants[`age=${age}`]={model:`${ns}:block/${c.id}_${age}`};write(`assets/${ns}/models/block/${c.id}_${age}.json`,cropModel(c,age,crops.indexOf(c)));}
 write(`assets/${ns}/blockstates/${c.id}_crop.json`,{variants});write(`assets/${ns}/models/item/${c.id}_seeds.json`,inventoryModel({id:c.id+'_seeds'},crops.indexOf(c)+115));write(`assets/${ns}/items/${c.id}_seeds.json`,{model:{type:'minecraft:model',model:`${ns}:item/${c.id}_seeds`}});
 const mature={condition:'minecraft:block_state_property',block:`${ns}:${c.id}_crop`,properties:{age:'7'}};
 write(`data/${ns}/loot_table/blocks/${c.id}_crop.json`,{type:'minecraft:block',pools:[{rolls:1,entries:[{type:'minecraft:item',name:`${ns}:${c.id}_seeds`}]},{rolls:1,entries:[{type:'minecraft:item',name:`${ns}:${c.id}_seeds`,functions:[{function:'minecraft:set_count',count:{type:'minecraft:uniform',min:1,max:2}}]}],conditions:[mature]},{rolls:1,entries:[{type:'minecraft:item',name:`${ns}:${c.id}`,functions:[{function:'minecraft:set_count',count:{type:'minecraft:uniform',min:2,max:3}}]}],conditions:[mature]}]});
 write(`data/${ns}/worldgen/configured_feature/wild_${c.id}.json`,{type:`${ns}:wild_${c.id}`,config:{}});
 write(`data/${ns}/worldgen/placed_feature/wild_${c.id}.json`,{feature:`${ns}:wild_${c.id}`,placement:[{type:'minecraft:rarity_filter',chance:c.id==='rice_paddy'?2:c.id==='shiitake'?8:20},{type:'minecraft:in_square'},{type:'minecraft:heightmap',heightmap:'WORLD_SURFACE_WG'},{type:'minecraft:biome'}]});
}
const craft=(id,ingredients,count=1)=>write(`data/${ns}/recipe/${id}.json`,{type:'minecraft:crafting_shapeless',category:'misc',ingredients:ingredients.map(q),result:{id:q(id),count}});
const shaped=(id,pattern,key,count=1)=>write(`data/${ns}/recipe/${id}.json`,{type:'minecraft:crafting_shaped',category:'misc',pattern,key:Object.fromEntries(Object.entries(key).map(([k,v])=>[k,q(v)])),result:{id:q(id),count}});
shaped('clay_stove',['CMC','CLC','SSS'],{C:'minecraft:clay_ball',M:'minecraft:stick',L:'minecraft:cobblestone',S:'minecraft:cobblestone'});
shaped('cutting_board',['PPP',' S ','S  '],{P:'minecraft:oak_planks',S:'minecraft:stick'});
shaped('stone_knife',[' C','C ','S '],{C:'minecraft:cobblestone',S:'minecraft:stick'});
shaped('iron_knife',[' I','I ','S '],{I:'minecraft:iron_ingot',S:'minecraft:stick'});
shaped('wooden_spatula',[' P',' S','S '],{P:'minecraft:oak_planks',S:'minecraft:stick'});
shaped('iron_spatula',[' I',' S','S '],{I:'minecraft:iron_ingot',S:'minecraft:stick'});
shaped('wok',['I I','III',' S '],{I:'minecraft:iron_ingot',S:'minecraft:stick'});
shaped('frying_pan',['III',' S '],{I:'minecraft:iron_ingot',S:'minecraft:stick'});
shaped('soup_pot',['I I','I I',' I '],{I:'minecraft:iron_ingot'});
shaped('clay_pot',['C C','C C',' C '],{C:'minecraft:clay_ball'});
shaped('hand_mill',[' S ','CCC','CCC'],{S:'minecraft:stick',C:'minecraft:cobblestone'});
shaped('oil_press',['PIP','PVP','PPP'],{P:'minecraft:oak_planks',I:'minecraft:iron_ingot',V:'minecraft:piston'});
shaped('brick_stove',['BIB','BFB','BIB'],{B:'minecraft:brick',I:'minecraft:iron_ingot',F:'minecraft:furnace'});
craft('double_stove',['brick_stove','brick_stove','minecraft:iron_ingot','minecraft:iron_ingot']);
shaped('oven',['BIB','GFG','BIB'],{B:'minecraft:brick',I:'minecraft:iron_ingot',G:'minecraft:glass',F:'minecraft:furnace'});
shaped('steamer',['BBB','S S','BBB'],{B:'minecraft:bamboo',S:'minecraft:stick'});
craft('ceramic_plate',['minecraft:clay_ball','minecraft:clay_ball','minecraft:clay_ball'],2);
craft('ceramic_bowl',['minecraft:clay_ball','minecraft:clay_ball'],2);
craft('empty_jar',['minecraft:glass','minecraft:glass','minecraft:glass','minecraft:oak_planks'],4);
shaped('wooden_lid',['PP',' S'],{P:'minecraft:oak_planks',S:'minecraft:stick'});
craft('pot_lid',['minecraft:iron_ingot','minecraft:iron_ingot','minecraft:iron_nugget']);
shaped('ladle',['II',' S'],{I:'minecraft:iron_ingot',S:'minecraft:stick'});
shaped('tongs',['N N',' I '],{N:'minecraft:iron_nugget',I:'minecraft:iron_ingot'});
craft('chopsticks',['minecraft:stick','minecraft:stick']);
craft('rolling_pin',['minecraft:oak_planks','minecraft:oak_planks','minecraft:stick','minecraft:stick','minecraft:stick']);
craft('heat_cloth',['minecraft:white_wool','minecraft:string','minecraft:string']);
craft('empty_water_jug',['minecraft:iron_ingot','minecraft:iron_ingot','minecraft:iron_ingot','minecraft:glass','minecraft:glass']);
craft('water_jug',['empty_water_jug','minecraft:water_bucket']);
craft('thermometer',['minecraft:copper_ingot','minecraft:copper_ingot','minecraft:glass','minecraft:redstone']);
craft('recipe_book',['minecraft:book','minecraft:wheat_seeds']);
for(const [id,ingredients]of Object.entries({
 fermentation_crock:['minecraft:terracotta','minecraft:terracotta','minecraft:terracotta','wooden_lid','minecraft:string'],
 pickling_crock:['minecraft:terracotta','minecraft:terracotta','wooden_lid'],
 drying_rack:['minecraft:stick','minecraft:stick','minecraft:stick','minecraft:string','minecraft:string','minecraft:oak_planks'],
 counter:['minecraft:oak_planks','minecraft:oak_planks','minecraft:smooth_stone'],
 mixing_table:['counter','minecraft:bowl','rolling_pin'],sink:['counter','minecraft:iron_ingot','minecraft:bucket'],
 cabinet:['minecraft:chest','minecraft:oak_planks'],grain_bin:['minecraft:chest','minecraft:hay_block'],icebox:['minecraft:chest','minecraft:ice','minecraft:iron_ingot'],
 spice_rack:['minecraft:oak_planks','minecraft:stick','minecraft:glass'],dish_rack:['minecraft:oak_planks','minecraft:oak_planks','minecraft:stick','minecraft:stick','minecraft:stick','minecraft:stick'],
 salt_pan:['minecraft:terracotta','minecraft:terracotta','minecraft:iron_nugget'],spice_trellis:['minecraft:stick','minecraft:stick','minecraft:string'],
 dining_table:['minecraft:oak_planks','minecraft:oak_planks','minecraft:stick','minecraft:stick'],chair:['minecraft:oak_planks','minecraft:oak_planks','minecraft:oak_planks','minecraft:stick'],
 serving_plate:['ceramic_plate'],mixing_bowl:['ceramic_bowl'],soup_tureen:['ceramic_bowl','ceramic_bowl'],cookbook_stand:['recipe_book','minecraft:oak_planks'],compost_bin:['minecraft:composter','minecraft:oak_planks'],
 range_hood:['minecraft:iron_ingot','minecraft:iron_ingot','minecraft:glass'],chimney:['minecraft:brick','minecraft:brick'],baking_tray:['minecraft:iron_ingot','minecraft:iron_ingot','minecraft:iron_ingot'],
 sealed_jar:['empty_jar','wooden_lid'],food_basket:['minecraft:bamboo','minecraft:bamboo','minecraft:string'],sieve:['minecraft:stick','minecraft:string'],refined_knife:['iron_knife','minecraft:copper_ingot','minecraft:flint']
}))craft(id,ingredients);
shaped('baking_tray',['III'],{I:'minecraft:iron_ingot'});
shaped('dining_table',['PP','SS'],{P:'minecraft:oak_planks',S:'minecraft:stick'});
shaped('dish_rack',['SSS','P P',' S '],{P:'minecraft:oak_planks',S:'minecraft:stick'});
shaped('gas_stove',['III','CFC','ICI'],{I:'minecraft:iron_ingot',C:'minecraft:copper_ingot',F:'minecraft:furnace'});
shaped('gas_pipe',['CCC'],{C:'minecraft:copper_ingot'},6);
shaped('gas_tank',['III','I I','III'],{I:'minecraft:iron_ingot'});
shaped('gas_extractor',['IPI','IFI','CCC'],{I:'minecraft:iron_ingot',P:'minecraft:iron_pickaxe',F:'minecraft:furnace',C:'minecraft:copper_ingot'});
shaped('empty_gas_canister',[' I ','I I',' I '],{I:'minecraft:iron_ingot'});
for(const original of items.filter(x=>x.uses&&!x.variant_of&&!x.seasoning&&x.id!=='water_jug')){
 const material=original.id.startsWith('stone_')?'minecraft:cobblestone':/iron|refined|ladle|tongs/.test(original.id)?'minecraft:iron_ingot':original.id==='sieve'?'minecraft:string':original.id==='heat_cloth'?'minecraft:leather':'minecraft:oak_planks';
 for(const state of ['used','broken'])write(`data/${ns}/recipe/repair_${state}_${original.id}.json`,{type:'minecraft:crafting_shapeless',category:'misc',ingredients:[q(state+'_'+original.id),material],result:{id:q(original.id),count:1}});
}
for(const [raw,cooked]of Object.entries({duck_meat:'cooked_duck',goose_meat:'cooked_goose',boar_meat:'cooked_boar',raw_shrimp:'cooked_shrimp',raw_crab:'cooked_crab'}))for(const type of ['smelting','smoking'])write(`data/${ns}/recipe/${cooked}_${type}.json`,{type:'minecraft:'+type,category:'food',ingredient:q(raw),result:{id:q(cooked)},experience:.35,cookingtime:type==='smoking'?100:200});
const messages={full:'容器已满或不能再加入。',fuel:'请加入煤、木炭、煤块、木棍或谷壳。',ingredient:'这里需要可处理的食材。',cut:'切配方式：%s',cut_done:'切配完成，食材已取回。',serving_started:'已开始分餐，请先盛完这一锅。',not_ready:'食材还没完全熟，请观察火候。',empty:'已经空了。',cook_status:'锅温 %s°C｜食材 %s份｜水 %s｜盐 %s｜油 %s｜%s',stove_status:'火力 %s｜燃料剩余 %s秒',station_status:'物品 %s件｜加工进度 %s｜密封 %s'};
messages.filter_help='放入待过滤酱醪或醋醪与玻璃瓶，持筛子右键过滤；空罐返还。';
messages.ferment_raw_soy='生大豆须先加水煮熟或蒸熟，再与面粉、盐、引子和空罐入缸。';
messages.ferment_ready='已成待过滤醪液，空手开盖后潜行取出；调理台放玻璃瓶，持筛子过滤。';
messages.ferment_missing='材料未齐，请按食谱检查水、熟原料、盐、引子和空罐；时间不会代替缺料。';
messages.ferment_check='若产物已就绪，先空手开盖再潜行取出；未完成则按食谱检查缺料。';
messages.ferment_progress='密封发酵 %s%%｜经过 %s / %s 游戏秒；未密封暂停。';
messages.soy_soak='大豆浸泡 %s%%｜%s/120秒｜豆×4、水≥1，密封';
messages.tofu_filter='熟豆糊＋空罐，筛子过滤；生糊先煮透';
messages.tofu_press='豆花压模 %s%%｜木盖压4次，保留木盖';
messages.attached_seasoning_extra='附着调味（份）：糖%s｜醋%s｜香料%s';
messages.attached_seasoning='附着调味（份）：盐%s｜油%s｜酱油%s';
messages.tofu_milk='点浆 %s%%｜%s°C｜水%s｜醋%s次；温区80–100°C';
messages.tofu_chain='豆腐工艺：4豆加水密封泡发120秒→加水手磨→锅内加水煮透→调理台配空罐用筛子过滤→熟豆浆回锅加1次米醋，80–100°C点制→豆花在调理台用木盖压4次，得2豆腐。';
messages.board_status='切配：%s｜进度 %s/4';
messages.stove_help='火力 %s｜燃料 %s。锅具右键嵌入灶面，也可直接烤食物；空手点侧面调火。';
messages.burner_status='%s｜%s°C｜熟度 %s%%｜燃料 %s%s';messages.water_status='｜水%s（%s）';
messages.mounted='锅具已嵌入灶面；侧面调火，顶部投料，潜行持隔热布取锅。';
messages.spent='此物品已用完、损坏或变质，请更换或修复。';messages.gas_remove='已有气罐；潜行空手点击侧面取出。';
messages.gas_amount='实际储气：%s / %s 单位';messages.uses_left='剩余用量 / 耐久：%s / %s';
messages.gas_status='储气 %s 单位｜火力 %s。开采机贴含气岩层，管道接储气罐与气灶；空手调火。';
messages.recipe_fallback='本锅盛为%s；未成为%s：%s';messages.recipe_hint='%s：%s';messages.quality='菜肴品质：%s / 3';messages.recipe_generic='这组熟食没有对应固定菜谱，已按%s装盘，可以正常食用。';
const problems={method:'请使用食谱要求的锅具和做法。',cut:'%s需要%s处理，可取出后重新切配。',history:'%s来自旧版未记录的下料过程，无法确认投料顺序。',preheat:'放入%s前需先加油并将锅预热至%s°C。',order:'%s下得太早，应先加入全部%s。',stage:'加入%s时，前面的%s尚未达到食谱要求的熟度。',missing:'还缺%s ×%s。',extra:'原料比例不符或加入了额外食材。',seasoning:'还需要%s。',finish:'%s应在原料齐全、最低熟度达到%s%%后加入；可在用量上限内补一份。',stir:'全部投料后还需翻动，至少%s次。',water:'需要锅内保留至少%s份水。',boiling:'%s需要在有足够水且达到%s°C沸腾时下锅。',cover:'请加盖并保持有水微沸至少%s秒；蒸菜需保持盖好。'};
for(const[key,value]of Object.entries({dry:'无水',warming:'加热中',simmering:'微沸',boiling:'沸腾'}))lang[`water.${ns}.${key}`]=value;
for(const[key,value]of Object.entries({0:'关火',1:'小火',2:'中火',3:'大火'}))lang[`heat.${ns}.${key}`]=value;
Object.assign(problems,{water_ratio:'加入%s时水量需要为%s份；下锅前先按食谱配水。',soften:'%s需要继续煮软，最低熟度%s%%。',low_heat:'煮沸后请转小火并加盖至少%s秒。',roll:'面团应先在砧板擀开，再切丝。',oven:'需要烤盘装入烤炉，在至少140°C烤制%s秒并熟透。'});
Object.assign(messages,{roll_dough:'请先把面团放到砧板上，再用擀面杖。',rolled:'面团已经擀开。',roll_then_shred:'请先用擀面杖擀开，再切换刀具至切丝；包饺子和派只需擀开。',dough_sheet:'擀开的面团',raw_noodles:'生面条',cooked_noodles:'熟面条',tray_needed:'请先手持烤盘右键装入烤炉。',tray_mounted:'烤盘已装入烤炉；放入食材后加煤烘烤。',water_vessel:'请先在灶台装能盛水的锅；烤盘和烤炉不能装水。',steam_lid:'蒸笼尚未加盖，食物不会继续蒸熟。'});
lang[`ingredient.${ns}.sugar`]='糖';
for(const [key,value]of Object.entries(problems))lang[`recipe_problem.${ns}.${key}`]=value;
Object.assign(problems,{turn:'%s需要成形后三成半熟再翻面，并继续加热另一面至少三成。',cold_mix:'蛋和葱应在点火前、保持生料时搅匀至少%s次。',caramel:'%s下锅前需在无水热油中把糖炒出糖色3秒；温度145–185°C，焦糖不能补救。',marinate:'%s需在点火前用蜂蜜和酱油冷腌至少%s秒。',cold:'冷拌食材不能超过%s°C。',roast:'需要在无水、至少140°C下烤制%s秒。',ferment:'需要有盐水、密封且低于60°C发酵%s秒。',soften:'%s还未软烂，请继续加热到软烂进度100%。'});
Object.assign(messages,{softening:'软烂',softening_progress:'软烂进度 %s%%',caramel_progress:'糖色 %s/3秒（未焦%s）',contents:'食材：',open_first:'请先空手打开缸盖，再加入材料；重新加料会重计时间。'});
messages.burner_status='%s｜%s°C｜进度 %s%%｜燃料 %s%s';
Object.assign(messages,{quality_grade:'品质：%s（%s/3）',hot_hands:'锅面很烫！请用碗盘盛菜、隔热布搬锅，或冷却到80°C以下再徒手操作。'});
Object.assign(messages,{render_help:'炼油：只放生动物脂，不加水油调料；泥灶或平底锅用小火，完全熬出油后持空罐收取。2份脂肪装满1罐，1份装半罐。',render_wait:'动物脂尚未熬透（%s%%），请继续小火加热。',render_burnt:'动物脂已过热或焦化，不能装成合格食用油；下次请从冷锅用小火。',render_done:'已装%s份猪油，并取回%s份油渣。',render_status:'炼油 %s°C｜进度 %s%%｜动物脂 %s份｜小火熬透后持空罐收取'});
for(const[key,value]of Object.entries({0:'失败',1:'欠佳',2:'家常',3:'用心烹制'}))lang[`quality.${ns}.${key}`]=value;
lang[`quality_reason.${ns}.strong`]='口味偏重：盐量稍多，补充一些无盐食材再拌匀可改善。';
for(const[key,value]of Object.entries({raw:'夹生：还有食材未熟。',burnt:'焦黑：食材已经烧坏，补水不能恢复。',salty:'过咸：盐或酱油比例过高，下次盛出前补充无盐食材。',bland:'偏淡：缺少适量盐或酱油。',overcooked:'过熟：加热过久，应及时关火盛出。',scorched:'焦底：局部受热过久，调低火力并及时翻动。',uneven:'受热或调味不匀：最后投料后再翻炒；煎物凝固后翻面并继续加热。',stale:'存放时间较久，新鲜度已经下降。'}))lang[`quality_reason.${ns}.${key}`]=value;
for(const[key,value]of Object.entries(problems))lang[`recipe_problem.${ns}.${key}`]=value;
for(const[key,value]of Object.entries({pepper_powder:'胡椒粉',cumin_powder:'孜然粉',sesame_oil:'芝麻油',honey:'蜂蜜'}))lang[`ingredient.${ns}.${key}`]=value;
for(const[key,value]of Object.entries({WHOLE:'完整',SLICE:'切片',DICE:'切块',SHRED:'切丝',MINCE:'剁碎'}))lang[`cut.${ns}.${key}`]=value;
for(const[key,value]of Object.entries({egg:'蛋',rice:'生米',rice_cooked:'米饭',scallion:'葱',tomato:'番茄',potato:'土豆',garlic:'蒜',pork:'猪肉',tofu:'豆腐',chili:'辣椒',oil:'食用油',salt:'盐',soy:'酱油',vinegar:'醋'}))lang[`ingredient.${ns}.${key}`]=value;
for(const definition of items)if(definition.ingredient&&!definition.variant_of&&!lang[`ingredient.${ns}.${definition.ingredient}`])lang[`ingredient.${ns}.${definition.ingredient}`]=definition.name;
for(const[key,value]of Object.entries({fish:'鱼',beef:'牛肉',chicken:'鸡肉',carrot:'胡萝卜',pumpkin:'南瓜',apple:'苹果',mushroom:'菌菇'}))lang[`ingredient.${ns}.${key}`]=value;
lang['itemGroup.smoke_kitchen']='烟火厨房';en['itemGroup.smoke_kitchen']='Smoke Kitchen';
for(const[k,v]of Object.entries(messages)){lang[`message.${ns}.${k}`]=v;en[`message.${ns}.${k}`]=v;}
for(const[k,v]of Object.entries({empty:'空锅',raw:'未熟',cooked:'已熟',burnt:'焦黑'}))lang[`state.${ns}.${k}`]=v;
lang[`state.${ns}.heating`]='加热中';lang[`state.${ns}.no_heat`]='未供热（检查燃料或开关）';
lang[`guide.${ns}.intro`]='烟火厨房：持锅右键嵌入灶面，点顶部投料，空手点侧面调火。刀切菜，铲翻炒，煮蒸炖要加水；潜行空手点顶部取熟食，碗盘盛菜，潜行持隔热布取锅。\n以下为各菜所需材料：';
write(`assets/${ns}/lang/zh_cn.json`,lang);write(`assets/${ns}/lang/en_us.json`,{...lang,...en});
lang[`food.${ns}.stale`]='不新鲜的%s';lang[`food.${ns}.spoiled`]='变质的%s';
write(`assets/${ns}/lang/zh_cn.json`,lang);write(`assets/${ns}/lang/en_us.json`,{...lang,...en});
write(`data/minecraft/tags/block/mineable/axe.json`,{replace:false,values:stations.filter(s=>s.kind!=='cook'&&s.kind!=='stove'&&!s.kind.startsWith('gas_')).map(s=>`${ns}:${s.id}`)});
write(`data/minecraft/tags/block/maintains_farmland.json`,{replace:false,values:crops.map(c=>`${ns}:${c.id}_crop`)});
write(`data/minecraft/tags/block/mineable/pickaxe.json`,{replace:false,values:[...stations.filter(s=>s.kind==='cook'||s.kind==='stove'||s.kind.startsWith('gas_')).map(s=>`${ns}:${s.id}`),`${ns}:salt_ore`]});
lang[`block.${ns}.salt_ore`]='盐矿石';
write(`assets/${ns}/lang/zh_cn.json`,lang);write(`assets/${ns}/lang/en_us.json`,{...lang,...en});
write(`assets/${ns}/blockstates/salt_ore.json`,{variants:{'':{model:`${ns}:block/salt_ore`}}});
// A stone host with white crystalline seams is recognizable while exploring;
// the previous plain-diorite model made the survival salt source indistinguishable.
const saltSeams=[[2,3,4,8],[6,7,9,10],[9,9,11,14],[11,11,14,13]];
const oreFaces=Object.fromEntries(['down','up','north','south','west','east'].map(face=>[face,{texture:'#stone',cullface:face}]));
const oreSeams=[];
for(const [a,b,c,d]of saltSeams){
 oreSeams.push({from:[a,b,-.01],to:[c,d,0],faces:{north:{texture:'#salt'}}},{from:[a,b,16],to:[c,d,16.01],faces:{south:{texture:'#salt'}}},
 {from:[-.01,b,a],to:[0,d,c],faces:{west:{texture:'#salt'}}},{from:[16,b,a],to:[16.01,d,c],faces:{east:{texture:'#salt'}}},
 {from:[a,-.01,b],to:[c,0,d],faces:{down:{texture:'#salt'}}},{from:[a,16,b],to:[c,16.01,d],faces:{up:{texture:'#salt'}}});
}
write(`assets/${ns}/models/block/salt_ore.json`,{parent:'minecraft:block/block',textures:{stone:'minecraft:block/stone',salt:'minecraft:block/calcite',particle:'minecraft:block/stone'},elements:[{from:[0,0,0],to:[16,16,16],faces:oreFaces},...oreSeams]});
write(`assets/${ns}/models/item/salt_ore.json`,{parent:`${ns}:block/salt_ore`});write(`assets/${ns}/items/salt_ore.json`,{model:{type:'minecraft:model',model:`${ns}:item/salt_ore`}});
write(`data/${ns}/loot_table/blocks/salt_ore.json`,{type:'minecraft:block',pools:[{rolls:1,entries:[{type:'minecraft:item',name:`${ns}:salt_ore_chunk`,functions:[{function:'minecraft:set_count',count:2}]}]}]});
write(`data/${ns}/worldgen/configured_feature/salt_ore.json`,{type:'minecraft:ore',config:{size:7,discard_chance_on_air_exposure:0,targets:[{target:{predicate_type:'minecraft:tag_match',tag:'minecraft:stone_ore_replaceables'},state:{Name:`${ns}:salt_ore`}}]}});
write(`data/${ns}/worldgen/placed_feature/salt_ore.json`,{feature:`${ns}:salt_ore`,placement:[{type:'minecraft:count',count:3},{type:'minecraft:in_square'},{type:'minecraft:height_range',height:{type:'minecraft:uniform',min_inclusive:{absolute:0},max_inclusive:{absolute:96}}},{type:'minecraft:biome'}]});
for(const building of ['village_kitchen','farmstead','spice_stall','mushroom_cabin','fishing_hut','saltworks','old_inn','abandoned_granary','raider_camp']) {
 write(`data/${ns}/worldgen/configured_feature/${building}.json`,{type:`${ns}:${building}`,config:{}});
 write(`data/${ns}/worldgen/placed_feature/${building}.json`,{feature:`${ns}:${building}`,placement:[{type:'minecraft:rarity_filter',chance:1024},{type:'minecraft:in_square'},{type:'minecraft:heightmap',heightmap:'WORLD_SURFACE_WG'},{type:'minecraft:biome'}]});
}
write(`data/${ns}/worldgen/configured_feature/gas_deposit.json`,{type:'minecraft:ore',config:{size:5,discard_chance_on_air_exposure:0,targets:[{target:{predicate_type:'minecraft:tag_match',tag:'minecraft:stone_ore_replaceables'},state:{Name:`${ns}:gas_deposit`}}]}});
write(`data/${ns}/worldgen/placed_feature/gas_deposit.json`,{feature:`${ns}:gas_deposit`,placement:[{type:'minecraft:count',count:2},{type:'minecraft:in_square'},{type:'minecraft:height_range',height:{type:'minecraft:uniform',min_inclusive:{absolute:0},max_inclusive:{absolute:48}}},{type:'minecraft:biome'}]});
for(const[id,drop]of Object.entries({duck:'duck_meat',goose:'goose_meat',boar:'boar_meat',river_shrimp:'raw_shrimp',river_crab:'raw_crab',grain_raider:'salt',cellar_silverfish:'oil_cake'})) {
 lang[`entity.${ns}.${id}`]={duck:'鸭',goose:'鹅',boar:'野猪',river_shrimp:'河虾',river_crab:'河蟹',grain_raider:'劫粮掠夺者',cellar_silverfish:'地窖蠹虫'}[id];
 write(`data/${ns}/loot_table/entities/${id}.json`,{type:'minecraft:entity',pools:[{rolls:1,entries:[{type:'minecraft:item',name:`${ns}:${drop}`}]}]});
 const egg=id+'_spawn_egg';lang[`item.${ns}.${egg}`]=lang[`entity.${ns}.${id}`]+'刷怪蛋';en[`item.${ns}.${egg}`]=id.replace(/_/g,' ')+' spawn egg';
 write(`assets/${ns}/models/item/${egg}.json`,inventoryModel({id:egg},Object.keys({duck:1,goose:1,boar:1,river_shrimp:1,river_crab:1,grain_raider:1,cellar_silverfish:1}).indexOf(id)));
 write(`assets/${ns}/items/${egg}.json`,{model:{type:'minecraft:model',model:`${ns}:item/${egg}`}});
}
for(const[id,name]of Object.entries({shrimp_bucket:'河虾桶',crab_bucket:'河蟹桶'})) {lang[`item.${ns}.${id}`]=name;write(`assets/${ns}/models/item/${id}.json`,inventoryModel({id},id==='shrimp_bucket'?150:151));write(`assets/${ns}/items/${id}.json`,{model:{type:'minecraft:model',model:`${ns}:item/${id}`}});}
write(`assets/${ns}/lang/zh_cn.json`,lang);write(`assets/${ns}/lang/en_us.json`,{...lang,...en});
lang['message.smoke_kitchen.meal_stale']='锅里的食物已变味，品质下降；装盘或换锅不会恢复新鲜。';
lang['message.smoke_kitchen.meal_spoiled']='锅里的食物已腐坏，不能食用；可装出后投入厨余桶，回收餐具。';
en['message.smoke_kitchen.meal_stale']='Food in this vessel is stale. Serving or moving it will not restore freshness.';
en['message.smoke_kitchen.meal_spoiled']='Food in this vessel has spoiled and is inedible. Serve it for composting to reclaim the dish.';
write(`assets/${ns}/lang/zh_cn.json`,lang);write(`assets/${ns}/lang/en_us.json`,{...lang,...en});
// Use vanilla recipe discovery so ingredients reveal the custom recipes in the normal book.
for(const file of fs.readdirSync(path.join(root,`data/${ns}/recipe`)).filter(file=>file.endsWith('.json'))){
 const recipe=JSON.parse(fs.readFileSync(path.join(root,`data/${ns}/recipe`,file),'utf8'));
 if(!['minecraft:crafting_shaped','minecraft:crafting_shapeless'].includes(recipe.type))continue;
 const id=file.slice(0,-5),ingredient=(recipe.ingredients||Object.values(recipe.key))[0];
 write(`data/${ns}/advancement/recipes/${id}.json`,{parent:'minecraft:recipes/root',criteria:{has_material:{trigger:'minecraft:inventory_changed',conditions:{items:[{items:ingredient}]}},has_the_recipe:{trigger:'minecraft:recipe_unlocked',conditions:{recipe:`${ns}:${id}`}}},requirements:[['has_material','has_the_recipe']],rewards:{recipes:[`${ns}:${id}`]}});
}
console.log(`Generated ${items.length} items, ${stations.length} stations, ${crops.length} crops, ${dishes.length} dishes, ${processes.length} processing recipes.`);

writeFoodSampleTextures(root);
