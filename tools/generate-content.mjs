import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'../src/main/resources');
const ns='smoke_kitchen';
const write=(p,data)=>{const out=path.join(root,p);fs.mkdirSync(path.dirname(out),{recursive:true});fs.writeFileSync(out,JSON.stringify(data,null,2)+'\n');};
const items=[],stations=[],crops=[],dishes=[],processes=[],lang={},en={};
function item(id,name,texture,extra={}) {items.push({id,name,texture,...extra});lang[`item.${ns}.${id}`]=name;en[`item.${ns}.${id}`]=id.replace(/_/g,' ');}
function station(id,name,kind,model='cabinet') {stations.push({id,name,kind,model});lang[`block.${ns}.${id}`]=name;en[`block.${ns}.${id}`]=id.replace(/_/g,' ');}
const plantRows=[
 ['rice_paddy','稻谷','wheat','rice',true],['tomato','番茄','apple','tomato'],['chili','辣椒','sweet_berries','chili'],
 ['soybean','大豆','wheat_seeds','soybean'],['rapeseed','油菜籽','melon_seeds',null],['scallion','葱','sugar_cane','scallion'],
 ['ginger','姜','potato','ginger'],['garlic','蒜','beetroot','garlic'],['cucumber','黄瓜','pickle','cucumber'],
 ['cabbage','白菜','kelp','cabbage'],['onion','洋葱','beetroot','onion'],['eggplant','茄子','chorus_fruit','eggplant'],
 ['sesame','芝麻','pumpkin_seeds',null],['sichuan_pepper','花椒','sweet_berries',null],['black_pepper','胡椒','melon_seeds',null],
 ['cumin','孜然','wheat_seeds',null],['shiitake','香菇','brown_mushroom','mushroom']];
for(const [id,name,texture,ingredient] of plantRows) {
 item(id,name,texture==='pickle'?'sea_pickle':texture,{nutrition:['tomato','cucumber','cabbage'].includes(id)?2:0,...(ingredient?{ingredient}: {})});
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
 ['duck_egg','鸭蛋','egg','egg'],['goose_egg','鹅蛋','egg','egg']]) item(id,name,texture,{ingredient,resistance:ingredient==='egg'?1:1.6});
for(const [id,name,texture] of [['dried_chili','干辣椒','sweet_berries'],['dried_shiitake','干香菇','brown_mushroom'],['oil_cake','油渣饲料','wheat'],['salt_ore_chunk','盐矿石','quartz'],['rendered_fat','动物脂','honeycomb']]) item(id,name,texture);
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
 ['recipe_book','厨师食谱','book'],['sealed_jar','密封罐','glass_bottle'],['food_basket','食物篮','bundle'],['sieve','筛子','string']]) item(id,name,texture,{unstackable:id==='recipe_book'});
for(const [id,name,kind,model] of [
 ['clay_stove','简易泥灶','stove','stove'],['brick_stove','砖砌灶台','stove','stove'],['double_stove','双眼灶台','stove','stove'],['oven','烤炉','cook','oven'],
 ['cutting_board','砧板','board','board'],['clay_pot','陶锅','cook','pot'],['soup_pot','铁汤锅','cook','pot'],['wok','炒锅','cook','wok'],['frying_pan','平底锅','cook','pan'],
 ['steamer','蒸笼','cook','steamer'],['baking_tray','烤盘','cook','pan'],['mixing_bowl','调理碗','cook','pot'],['counter','工作台面','storage','counter'],['mixing_table','调理台','mix','counter'],
 ['sink','洗涤槽','storage','sink'],['hand_mill','手磨','mill','mill'],['oil_press','压榨机','press','press'],['fermentation_crock','发酵缸','ferment','crock'],
 ['pickling_crock','腌菜缸','pickle','crock'],['drying_rack','晾晒架','dry','rack'],['cabinet','储物柜','storage','cabinet'],['grain_bin','谷仓箱','storage','cabinet'],
 ['icebox','冰柜','storage','cabinet'],['spice_rack','调料架','rack','rack'],['dish_rack','碗盘架','rack','rack'],['salt_pan','盐水蒸发盘','evaporate','pan'],
 ['spice_trellis','香料种植架','rack','rack'],['dining_table','餐桌','storage','table'],['chair','餐椅','storage','chair'],['serving_plate','共享菜盘','plate','plate'],
 ['soup_tureen','共享汤盆','plate','pot'],['cookbook_stand','食谱架','rack','stand'],['compost_bin','厨余桶','compost','crock'],['range_hood','排烟罩','decor','hood'],['chimney','烟道','decor','chimney']]) station(id,name,kind,model);
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
dish('mapo_tofu','麻婆豆腐',{tofu:2,pork:1,chili:1},'STIR_FRY',2,'豆腐×2、肉末×1、辣椒×1；花椒、酱油，轻推。',{oil:true,soy:true});
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
item('mixed_stir_fry','家常杂炒','bowl',{nutrition:5,meal:true});item('mixed_stew','家常炖汤','bowl',{nutrition:5,meal:true});item('burnt_food','焦黑食物','charcoal',{nutrition:1,meal:true});

const q=id=>id.includes(':')?id:`${ns}:${id}`;
function process(station,inputs,outputs,seconds=5,water=0){processes.push({station,inputs:Object.fromEntries(Object.entries(inputs).map(([k,v])=>[q(k),v])),outputs:Object.fromEntries(Object.entries(outputs).map(([k,v])=>[q(k),v])),seconds,water});}
process('mill',{rice_paddy:1},{rice:1,rice_husk:1});process('mill',{'minecraft:wheat':1},{flour:1});process('mill',{salt_ore_chunk:1},{salt_crystals:4});
process('mill',{salt_crystals:16,empty_jar:1},{salt:1});
process('mill',{dried_chili:1},{chili_powder:1});process('mill',{black_pepper:1},{pepper_powder:1});process('mill',{cumin:1},{cumin_powder:1});process('mill',{dried_shiitake:1},{mushroom_powder:1});
process('press',{rapeseed:8,'minecraft:glass_bottle':1},{rapeseed_oil:1,oil_cake:2});process('press',{sesame:16,'minecraft:glass_bottle':1},{sesame_oil:1,oil_cake:2});
process('mix',{flour:2},{dough:2},5,1);process('mix',{'minecraft:book':1},{recipe_book:1});
process('mix',{dough:2,'minecraft:porkchop':1,cabbage:1},{raw_dumplings:2},10);
process('mix',{'minecraft:porkchop':3,onion:1},{raw_skewers:3},10);
process('mix',{dough:2,'minecraft:apple':2,'minecraft:sugar':1},{raw_apple_pie:1},10);
process('ferment',{dough:2,starter:1},{fermented_dough:2,starter:1},120);
process('dry',{chili:1},{dried_chili:1},120);process('dry',{shiitake:1},{dried_shiitake:1},120);
process('ferment',{cooked_rice:1,shiitake:1,'minecraft:sugar':1},{starter:2},1200);
process('ferment',{cooked_rice:1,'minecraft:apple':1},{vinegar_starter:2},1200,1);
process('ferment',{soybean:4,flour:2,salt:2,starter:1,'minecraft:glass_bottle':1},{soy_sauce:1,starter:1},2400,1);
process('ferment',{cooked_rice:4,vinegar_starter:1,'minecraft:glass_bottle':1},{rice_vinegar:1,vinegar_starter:1},2400,1);
process('mix',{soybean:4,rice_vinegar:1},{tofu:2},20,1);
process('pickle',{cabbage:3,chili:1,garlic:1,salt:2},{pickled_cabbage:4},1200,1);
process('evaporate',{}, {salt_crystals:4},120,6);processes[processes.length-1].saltwater=true;

write(`data/${ns}/catalog.json`,{items,stations,crops,dishes,processes});
for(const it of items){
 write(`assets/${ns}/items/${it.id}.json`,{model:{type:'minecraft:model',model:`${ns}:item/${it.id}`}});
 const texture=it.texture.includes('trapdoor')?'block/'+it.texture:'item/'+it.texture;
 write(`assets/${ns}/models/item/${it.id}.json`,{parent:'minecraft:item/generated',textures:{layer0:it.id==='fried_rice'?`${ns}:item/fried_rice`:`minecraft:${texture}`}});
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
const textures={base:'minecraft:block/oak_planks',metal:'minecraft:block/iron_block',dark:'minecraft:block/gray_concrete',stone:'minecraft:block/stone',clay:'minecraft:block/terracotta',particle:'minecraft:block/oak_planks'};
for(const s of stations){
 write(`assets/${ns}/models/block/${s.id}.json`,{textures:{...textures,base:s.kind==='stove'?'minecraft:block/bricks':textures.base},elements:models[s.model]});
 const parts=[];for(const [f,y]of[['north',0],['east',90],['south',180],['west',270]])parts.push({when:{facing:f},apply:{model:`${ns}:block/${s.id}`,y}});
 for(const c of['raw','cooked','burnt'])parts.push({when:{contents:c},apply:{model:`${ns}:block/contents_${c}`}});
 parts.push({when:{lid:'true'},apply:{model:`${ns}:block/lid`}});
 write(`assets/${ns}/blockstates/${s.id}.json`,{multipart:parts});
 write(`assets/${ns}/models/item/${s.id}.json`,{parent:`${ns}:block/${s.id}`});write(`assets/${ns}/items/${s.id}.json`,{model:{type:'minecraft:model',model:`${ns}:item/${s.id}`}});
 write(`data/${ns}/loot_table/blocks/${s.id}.json`,{type:'minecraft:block',pools:[{rolls:1,entries:[{type:'minecraft:item',name:`${ns}:${s.id}`}],conditions:[{condition:'minecraft:survives_explosion'}]}]});
 if(s.id==='double_stove')write(`data/${ns}/loot_table/blocks/${s.id}.json`,{type:'minecraft:block',pools:[{rolls:1,entries:[{type:'minecraft:item',name:`${ns}:${s.id}`}],conditions:[{condition:'minecraft:block_state_property',block:`${ns}:double_stove`,properties:{secondary:'false'}}]}]});
}
for(const [c,t]of[['raw','minecraft:block/pink_concrete'],['cooked','minecraft:block/yellow_terracotta'],['burnt','minecraft:block/black_concrete']])write(`assets/${ns}/models/block/contents_${c}.json`,{textures:{base:t},elements:[box([4,1.05,4],[7,2.1,7]),box([9,1.05,4],[12,2.1,7]),box([5,1.05,9],[9,2.1,12])]});
write(`assets/${ns}/models/block/lid.json`,{textures,elements:[box([1,7,1],[15,8,15],'#metal'),box([7,8,7],[9,9,9])]});
for(const c of crops){
 const variants={};for(let age=0;age<8;age++){variants[`age=${age}`]={model:`${ns}:block/${c.id}_${age}`};write(`assets/${ns}/models/block/${c.id}_${age}.json`,{parent:'minecraft:block/crop',textures:{crop:`minecraft:block/wheat_stage${age}`}});}
 write(`assets/${ns}/blockstates/${c.id}_crop.json`,{variants});write(`assets/${ns}/models/item/${c.id}_seeds.json`,{parent:'minecraft:item/generated',textures:{layer0:'minecraft:item/wheat_seeds'}});write(`assets/${ns}/items/${c.id}_seeds.json`,{model:{type:'minecraft:model',model:`${ns}:item/${c.id}_seeds`}});
 write(`data/${ns}/loot_table/blocks/${c.id}_crop.json`,{type:'minecraft:block',pools:[{rolls:1,entries:[{type:'minecraft:item',name:`${ns}:${c.id}_seeds`}]},{rolls:1,entries:[{type:'minecraft:item',name:`${ns}:${c.id}`,functions:[{function:'minecraft:set_count',count:{type:'minecraft:uniform',min:2,max:3}}]}],conditions:[{condition:'minecraft:block_state_property',block:`${ns}:${c.id}_crop`,properties:{age:'7'}}]}]});
}
const craft=(id,ingredients,count=1)=>write(`data/${ns}/recipe/${id}.json`,{type:'minecraft:crafting_shapeless',category:'misc',ingredients:ingredients.map(q),result:{id:q(id),count}});
const shaped=(id,pattern,key,count=1)=>write(`data/${ns}/recipe/${id}.json`,{type:'minecraft:crafting_shaped',category:'misc',pattern,key:Object.fromEntries(Object.entries(key).map(([k,v])=>[k,q(v)])),result:{id:q(id),count}});
shaped('clay_stove',['CMC','CLC','SSS'],{C:'minecraft:clay_ball',M:'minecraft:stick',L:'minecraft:cobblestone',S:'minecraft:cobblestone'});
shaped('cutting_board',['PPP',' S ',' S '],{P:'minecraft:oak_planks',S:'minecraft:stick'});
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
craft('wooden_lid',['minecraft:oak_planks','minecraft:oak_planks','minecraft:stick']);
craft('pot_lid',['minecraft:iron_ingot','minecraft:iron_ingot','minecraft:iron_nugget']);
craft('ladle',['minecraft:iron_ingot','minecraft:iron_ingot','minecraft:stick']);
craft('tongs',['minecraft:iron_ingot','minecraft:iron_nugget','minecraft:iron_nugget']);
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
const messages={full:'容器已满或不能再加入。',fuel:'请加入煤、木炭、煤块、木棍或谷壳。',ingredient:'这里需要可处理的食材。',cut:'切配方式：%s',cut_done:'切配完成，食材已取回。',serving_started:'已开始分餐，请先盛完这一锅。',not_ready:'食材还没完全熟，请观察火候。',empty:'已经空了。',cook_status:'锅温 %s°C｜食材 %s份｜水 %s｜盐 %s｜油 %s｜%s',stove_status:'火力 %s｜燃料剩余 %s秒',station_status:'物品 %s件｜加工进度 %s｜密封 %s'};
messages.board_status='切配：%s｜进度 %s/4';
for(const[k,v]of Object.entries(messages)){lang[`message.${ns}.${k}`]=v;en[`message.${ns}.${k}`]=v;}
for(const[k,v]of Object.entries({empty:'空锅',raw:'未熟',cooked:'已熟',burnt:'焦黑'}))lang[`state.${ns}.${k}`]=v;
lang[`guide.${ns}.intro`]='烟火厨房：右键投料，刀切菜，铲翻炒；空手调火或切换锅具做法，潜行空手取料，碗盘盛菜。灶台放在锅正下方。\n以下为各菜所需材料：';
write(`assets/${ns}/lang/zh_cn.json`,lang);write(`assets/${ns}/lang/en_us.json`,{...lang,...en});
lang[`food.${ns}.stale`]='不新鲜的%s';lang[`food.${ns}.spoiled`]='变质的%s';
write(`assets/${ns}/lang/zh_cn.json`,lang);write(`assets/${ns}/lang/en_us.json`,{...lang,...en});
write(`data/minecraft/tags/block/mineable/axe.json`,{replace:false,values:stations.filter(s=>s.kind!=='cook'&&s.kind!=='stove').map(s=>`${ns}:${s.id}`)});
write(`data/minecraft/tags/block/mineable/pickaxe.json`,{replace:false,values:[...stations.filter(s=>s.kind==='cook'||s.kind==='stove').map(s=>`${ns}:${s.id}`),`${ns}:salt_ore`]});
lang[`block.${ns}.salt_ore`]='盐矿石';
write(`assets/${ns}/lang/zh_cn.json`,lang);write(`assets/${ns}/lang/en_us.json`,{...lang,...en});
write(`assets/${ns}/blockstates/salt_ore.json`,{variants:{'':{model:`${ns}:block/salt_ore`}}});
write(`assets/${ns}/models/block/salt_ore.json`,{parent:'minecraft:block/cube_all',textures:{all:'minecraft:block/diorite'}});
write(`assets/${ns}/models/item/salt_ore.json`,{parent:`${ns}:block/salt_ore`});write(`assets/${ns}/items/salt_ore.json`,{model:{type:'minecraft:model',model:`${ns}:item/salt_ore`}});
write(`data/${ns}/loot_table/blocks/salt_ore.json`,{type:'minecraft:block',pools:[{rolls:1,entries:[{type:'minecraft:item',name:`${ns}:salt_ore_chunk`,functions:[{function:'minecraft:set_count',count:2}]}]}]});
write(`data/${ns}/worldgen/configured_feature/salt_ore.json`,{type:'minecraft:ore',config:{size:7,discard_chance_on_air_exposure:0,targets:[{target:{predicate_type:'minecraft:tag_match',tag:'minecraft:stone_ore_replaceables'},state:{Name:`${ns}:salt_ore`}}]}});
write(`data/${ns}/worldgen/placed_feature/salt_ore.json`,{feature:`${ns}:salt_ore`,placement:[{type:'minecraft:count',count:3},{type:'minecraft:in_square'},{type:'minecraft:height_range',height:{type:'minecraft:uniform',min_inclusive:{absolute:0},max_inclusive:{absolute:96}}},{type:'minecraft:biome'}]});
for(const building of ['village_kitchen','farmstead','spice_stall','mushroom_cabin','fishing_hut','saltworks','old_inn','abandoned_granary','raider_camp']) {
 write(`data/${ns}/worldgen/configured_feature/${building}.json`,{type:`${ns}:${building}`,config:{}});
 write(`data/${ns}/worldgen/placed_feature/${building}.json`,{feature:`${ns}:${building}`,placement:[{type:'minecraft:rarity_filter',chance:1024},{type:'minecraft:in_square'},{type:'minecraft:heightmap',heightmap:'WORLD_SURFACE_WG'},{type:'minecraft:biome'}]});
}
for(const[id,drop]of Object.entries({duck:'duck_meat',goose:'goose_meat',boar:'boar_meat',river_shrimp:'raw_shrimp',river_crab:'raw_crab',grain_raider:'salt',cellar_silverfish:'oil_cake'})) {
 lang[`entity.${ns}.${id}`]={duck:'鸭',goose:'鹅',boar:'野猪',river_shrimp:'河虾',river_crab:'河蟹',grain_raider:'劫粮掠夺者',cellar_silverfish:'地窖蠹虫'}[id];
 write(`data/${ns}/loot_table/entities/${id}.json`,{type:'minecraft:entity',pools:[{rolls:1,entries:[{type:'minecraft:item',name:`${ns}:${drop}`}]}]});
}
for(const[id,name]of Object.entries({shrimp_bucket:'河虾桶',crab_bucket:'河蟹桶'})) {lang[`item.${ns}.${id}`]=name;write(`assets/${ns}/models/item/${id}.json`,{parent:'minecraft:item/generated',textures:{layer0:'minecraft:item/cod_bucket'}});write(`assets/${ns}/items/${id}.json`,{model:{type:'minecraft:model',model:`${ns}:item/${id}`}});}
write(`assets/${ns}/lang/zh_cn.json`,lang);write(`assets/${ns}/lang/en_us.json`,{...lang,...en});
console.log(`Generated ${items.length} items, ${stations.length} stations, ${crops.length} crops, ${dishes.length} dishes, ${processes.length} processing recipes.`);
