import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'../src/main/resources');
const ns='smoke_kitchen';
const read=p=>JSON.parse(fs.readFileSync(path.join(root,p),'utf8'));
const catalog=read(`data/${ns}/catalog.json`);
const eggs=['duck','goose','boar','river_shrimp','river_crab','grain_raider','cellar_silverfish'].map(x=>x+'_spawn_egg');
const allIds=[...catalog.items.map(x=>x.id),...catalog.stations.map(x=>x.id),...catalog.crops.map(x=>x.id+'_seeds'),'salt_ore','shrimp_bucket','crab_bucket',...eggs];
const known=new Set(allIds),errors=[];
if(known.size!==allIds.length)errors.push('Duplicate item registration.');
const checkItem=(id,source)=>{if(typeof id==='string' && id.startsWith(ns+':')&&!known.has(id.slice(ns.length+1)))errors.push(`${source}: unknown item ${id}`);};
for(const r of catalog.processes){Object.keys(r.inputs).forEach(id=>checkItem(id,'processing input'));Object.keys(r.outputs).forEach(id=>checkItem(id,'processing output'));}
for(const r of catalog.processes){
 if(r.tool)checkItem(ns+':'+r.tool,'processing tool');
 for(const[id,cuts]of Object.entries(r.cuts||{}))if(!r.inputs[id]||!cuts.length||cuts.some(c=>!['WHOLE','SLICE','DICE','SHRED','MINCE'].includes(c)))errors.push(`Invalid processing cuts: ${id}`);
 for(const id of r.rolled||[])if(!r.inputs[id])errors.push(`Rolled input absent from process: ${id}`);
}
for(const r of catalog.dishes){if(!known.has(r.id))errors.push(`Dish ${r.id} has no item`);if(r.servings<1||r.servings>24)errors.push(`Invalid yield ${r.id}`);}
for(const r of catalog.dishes){
 for(const [id,cuts]of Object.entries(r.cuts||{}))if(!r.ingredients[id]||!cuts.length||cuts.some(c=>!['WHOLE','SLICE','DICE','SHRED','MINCE'].includes(c)))errors.push(`Invalid preparation cut in ${r.id}: ${id}`);
 const edges=new Map();
 for(const step of r.steps||[]){
  if(!r.ingredients[step.ingredient]||step.after&&!r.ingredients[step.after]||step.ingredient===step.after||(step.temperature||0)<0||(step.temperature||0)>250||(step.cooked||0)<0||(step.cooked||0)>1)errors.push(`Invalid preparation step in ${r.id}`);
  if(step.after){if(!edges.has(step.ingredient))edges.set(step.ingredient,[]);edges.get(step.ingredient).push(step.after);}
 }
 const visit=(id,trail=new Set())=>{if(trail.has(id))return false;return(edges.get(id)||[]).every(next=>visit(next,new Set([...trail,id])));};
 if([...edges.keys()].some(id=>!visit(id)))errors.push(`Impossible circular preparation order in ${r.id}`);
 for(const [id,progress]of Object.entries(r.finish_seasonings||{}))if((!r[id]&&!(id==='sugar'&&r.staple?.sugar)&&!(r.finishing?.condiments||[]).includes(id))||progress<0||progress>1)errors.push(`Invalid finishing seasoning in ${r.id}`);
 if((r.stirs||0)<0||(r.stirs||0)>10)errors.push(`Invalid stirring count in ${r.id}`);
 if((r.minimum_water||0)<0||(r.minimum_water||0)>6||(r.covered_seconds||0)<0||(r.covered_seconds||0)>300)errors.push(`Invalid wet cooking requirement in ${r.id}`);
 if((r.steps||[]).some(s=>s.boiling_water&&(!r.minimum_water||s.temperature<90)))errors.push(`Boiling step without water or valid temperature in ${r.id}`);
 const s=r.staple||{};
 if((s.water_min||0)<0||(s.water_max||12)>12||(s.water_min||0)>(s.water_max||12)||(s.cooked||0)<0||(s.cooked||0)>3||(s.low_heat_seconds||0)<0||(s.low_heat_seconds||0)>300||(s.oven_seconds||0)<0||(s.oven_seconds||0)>300||(s.sugar||0)<0||(s.sugar||0)>16)errors.push(`Invalid staple cooking rule in ${r.id}`);
 const f=r.finishing||{};
 for(const key of ['cold_mixes','marinated_seconds','roast_seconds','ferment_seconds'])if((f[key]||0)<0||(f[key]||0)>2400)errors.push(`Invalid finishing requirement ${r.id}: ${key}`);
 for(const id of f.condiments||[])if(id!=='honey')checkItem(ns+':'+id,'typed condiment');
 if((r.steps||[]).some(step=>step.caramel&&(!step.hot_oil||step.temperature<140)))errors.push(`Caramel step without hot oil in ${r.id}`);
}
function* files(dir){for(const e of fs.readdirSync(dir,{withFileTypes:true})){const p=path.join(dir,e.name);if(e.isDirectory())yield*files(p);else if(e.name.endsWith('.json'))yield p;}}
const shapeless=new Map();
for(const file of files(root)){
 let json;try{json=JSON.parse(fs.readFileSync(file,'utf8'));}catch(e){errors.push(`${file}: ${e.message}`);continue;}
 if(json.type==='minecraft:crafting_shapeless'){
  const key=JSON.stringify([...json.ingredients].sort());
  if(shapeless.has(key))errors.push(`Conflicting shapeless recipes: ${shapeless.get(key)} and ${file}`);
  shapeless.set(key,file);
 }
 if(json.type==='minecraft:crafting_shaped'||json.type==='minecraft:crafting_shapeless'){
  const ingredients=json.ingredients||Object.values(json.key);ingredients.forEach(id=>checkItem(id,file));checkItem(json.result.id,file);
  const id=path.basename(file,'.json'),advancement=`data/${ns}/advancement/recipes/${id}.json`;
  if(!fs.existsSync(path.join(root,advancement)))errors.push(`Missing recipe-book discovery: ${id}`);
  else {const a=read(advancement);if(!a.rewards?.recipes?.includes(`${ns}:${id}`)||a.criteria?.has_the_recipe?.conditions?.recipe!==`${ns}:${id}`||a.criteria?.has_material?.trigger!=='minecraft:inventory_changed')errors.push(`Invalid recipe-book discovery: ${id}`);}

 }
 if(file.includes(path.join('assets',ns,'models'))){
  for(const texture of Object.values(json.textures||{}))if(texture.startsWith(ns+':')&&!fs.existsSync(path.join(root,'assets',ns,'textures',texture.slice(ns.length+1)+'.png')))errors.push(`Missing texture ${texture}`);
 }
}
for(const id of known)if(!fs.existsSync(path.join(root,'assets',ns,'items',id+'.json')))errors.push(`Missing client item ${id}`);
const iconSignatures=new Map();
for(const id of known){
 let model=read(`assets/${ns}/models/item/${id}.json`);
 const client=read(`assets/${ns}/items/${id}.json`).model;
 if(model.parent===`${ns}:block/${id}`)model=read(`assets/${ns}/models/block/${id}.json`);
 if(model.elements && model.parent!=='minecraft:block/block')errors.push(`Missing 3D inventory transforms: ${id}`);
 const signature=JSON.stringify(client.type==='minecraft:composite'?{base:model,badge:read(`assets/${ns}/models/item/${id}_state.json`)}:model);
 if(iconSignatures.has(signature))errors.push(`Identical inventory art: ${id} and ${iconSignatures.get(signature)}`);
 iconSignatures.set(signature,id);
}
for(const id of ['clay_stove','brick_stove','double_stove']){
 const model=read(`assets/${ns}/models/block/${id}.json`);
 if(model.parent!=='minecraft:block/block')errors.push(`Flat stove icon: ${id}`);
 const state=read(`assets/${ns}/blockstates/${id}.json`);
 if(!state.multipart.some(p=>p.when.heat==='1|2|3'&&p.apply.model.endsWith('_lit')))errors.push(`No animated lit furnace face: ${id}`);
}
const sprite=fs.readFileSync(path.join(root,'assets',ns,'textures','item','fried_rice.png'));
if(sprite.readUInt32BE(16)!==64||sprite.readUInt32BE(20)!==64)errors.push('Fried rice export must be 64x64.');
if(errors.length){console.error(errors.join('\n'));process.exit(1);}
console.log(`Validated ${known.size} item definitions, all local item references, recipes, JSON and custom texture dimensions.`);
