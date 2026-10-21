import fs from 'node:fs';
import path from 'node:path';
import {cube} from './voxel-art.mjs';
export function farmRackResources(root){
 const write=(p,d)=>{p=path.join(root,p);fs.mkdirSync(path.dirname(p),{recursive:true});fs.writeFileSync(p,JSON.stringify(d,null,2)+'\n');};
 const ns='smoke_kitchen';
 for(const dry of [false,true]){const e=cube([0,0,0],[16,13,16],'#dirt');e.faces.up={texture:'#top'};write(`assets/${ns}/models/block/rice_soil${dry?'_dry':''}.json`,{parent:'minecraft:block/block',textures:{dirt:'minecraft:block/dirt',top:`minecraft:block/farmland${dry?'':'_moist'}`,particle:'minecraft:block/dirt'},elements:[e]});}
 const variants={};for(let m=0;m<=7;m++)for(const water of [true,false])variants[`moisture=${m},waterlogged=${water}`]={model:`${ns}:block/rice_soil${m===0?'_dry':''}`};
 write(`assets/${ns}/blockstates/rice_soil.json`,{variants});write(`assets/${ns}/models/item/rice_soil.json`,{parent:`${ns}:block/rice_soil`});write(`assets/${ns}/items/rice_soil.json`,{model:{type:'minecraft:model',model:`${ns}:item/rice_soil`}});
 write(`data/${ns}/loot_table/blocks/rice_soil.json`,{type:'minecraft:block',pools:[{rolls:1,conditions:[{condition:'minecraft:survives_explosion'}],entries:[{type:'minecraft:item',name:'minecraft:dirt'}]}]});
 write('data/minecraft/tags/block/mineable/shovel.json',{replace:false,values:[`${ns}:rice_soil`]});
 const colors={salt:'white_concrete',oil:'yellow_terracotta',soy:'brown_terracotta',vinegar:'orange_terracotta',spice:'green_terracotta',honey:'yellow_concrete',empty:'light_gray_concrete'};
 for(const[type,color]of Object.entries(colors))for(const fill of type==='empty'?['empty']:['full','used']){
  const b=(a,z,t)=>cube(a,z,'#'+t);
  const elements=[b([6.5,0,6.5],[9.5,.5,9.5],'jar'),b([6.5,.5,6.5],[7,3.5,9.5],'jar'),b([9,.5,6.5],[9.5,3.5,9.5],'jar'),b([7,.5,6.5],[9,3.5,7],'jar'),b([7,.5,9],[9,3.5,9.5],'jar'),b([7,3.5,7],[9,4,9],'jar')];
  if(fill!=='empty')elements.push(b([6.45,.5,6.45],[9.55,fill==='full'?3:1.7,9.55],'contents'),b([7,4,7],[9,5,9],'lid'));
  else elements.push(b([7,4,7],[9,4.5,7.5],'lid'));
  write(`assets/${ns}/models/block/rack/${type}_${fill}.json`,{parent:'minecraft:block/block',textures:{jar:'minecraft:block/quartz_block_side',contents:`minecraft:block/${color}`,lid:'minecraft:block/spruce_planks',particle:'minecraft:block/quartz_block_side'},elements});
  write(`assets/${ns}/items/rack/${type}_${fill}.json`,{model:{type:'minecraft:model',model:`${ns}:block/rack/${type}_${fill}`}});
 }
}
