import fs from 'node:fs';
import path from 'node:path';
import {cube} from './voxel-art.mjs';
export function dryingResources(root){
 const ns='smoke_kitchen',write=(p,d)=>{p=path.join(root,p);fs.mkdirSync(path.dirname(p),{recursive:true});fs.writeFileSync(p,JSON.stringify(d,null,2)+'\n');};
 for(const type of ['chili','shiitake','black_pepper','sichuan_pepper','cumin'])for(const stage of ['fresh','partial','dried']){
  const b=cube,dry=stage==='dried',partial=stage==='partial',textures={rope:'minecraft:block/white_wool',stem:type==='chili'?'minecraft:block/green_terracotta':'minecraft:block/mushroom_stem',skin:type==='chili'?'minecraft:block/'+(dry?'brown_terracotta':partial?'red_terracotta':'red_concrete'):'minecraft:block/'+(dry?'brown_terracotta':'brown_mushroom_block'),edge:'minecraft:block/'+(type==='chili'?'red_terracotta':'sandstone_top'),particle:'minecraft:block/'+(type==='chili'?'red_terracotta':'brown_mushroom_block')};
  let food=type==='chili'?[b([7,4.3,7.4],[8.2,5.5,8.6],'#stem'),b([6.5,2,7.1],[dry?8.5:9,4.4,8.9],'#skin'),b([7,1,7.3],[dry?8.4:8.8,2,8.7],'#skin'),b([7.8,.5,7.4],[8.5,1.1,8.5],'#edge')]:[b([7.5,2.1,7.5],[8.5,5.5,8.5],'#stem'),b([dry?6:5.5,.5,dry?6:5.5],[dry?10:10.5,1.7,dry?10:10.5],'#skin'),b([6.5,1.7,6],[9.5,dry?2.1:2.7,10],'#skin'),b([6.5,.45,6.5],[9.5,.6,9.5],'#edge')];
  if(type==='black_pepper'||type==='sichuan_pepper'){
   textures.stem='minecraft:block/'+(dry?'brown_terracotta':'green_terracotta');textures.skin='minecraft:block/'+(type==='black_pepper'?(dry?'black_terracotta':partial?'brown_terracotta':'green_concrete'):(dry?'red_terracotta':partial?'red_terracotta':'red_concrete'));textures.edge='minecraft:block/black_terracotta';textures.particle=textures.skin;
   food=[b([7.7,.6,7.7],[8.3,5.5,8.3],'#stem')];
   for(let row=0;row<3;row++)for(const side of [-1,1]){const x=8+side*(dry?.9:1.1),y=.7+row*1.3,r=dry?.65:.85;food.push(b([x-r,y,7.3],[x+r,y+(dry?1:1.2),8.7],'#skin'));if(type==='sichuan_pepper'&&dry)food.push(b([x-.3,y+.15,8.65],[x+.3,y+.7,8.9],'#edge'));}
  }else if(type==='cumin'){
   textures.stem='minecraft:block/'+(dry?'brown_terracotta':'green_terracotta');textures.skin='minecraft:block/'+(dry?'brown_terracotta':partial?'yellow_terracotta':'green_concrete');textures.edge='minecraft:block/sandstone_top';textures.particle=textures.skin;
   food=[b([7.5,4.8,7.5],[8.5,5.5,8.5],'#stem'),b([6.3,4.95,7.6],[9.7,5.2,8.5],'#stem')];for(const [x,z,y] of [[6.5,7.8,.7],[8,8.3,1.3],[9.5,7.8,.7]]){food.push(b([x-.15,y,z-.15],[x+.15,5.2,z+.15],'#stem'),b([x-(dry?.35:.5),y,z-.45],[x+(dry?.35:.5),y+2.2,z+.45],'#skin'),b([x-.12,y+.2,z-.48],[x+.12,y+1.8,z-.4],'#edge'));}
  }else if(partial)food.push(b([type==='chili'?6.45:5.45,1,type==='chili'?7.2:6],[type==='chili'?6.65:5.65,2,type==='chili'?8.8:9],'#edge'));
  write(`assets/${ns}/models/block/drying/${type}_${stage}.json`,{parent:'minecraft:block/block',textures,elements:[...food,b([7.8,5.5,7.8],[8.2,6,8.2],'#rope')]});
  write(`assets/${ns}/items/drying/${type}_${stage}.json`,{model:{type:'minecraft:model',model:`${ns}:block/drying/${type}_${stage}`}});
  const id=stage==='fresh'?type:stage==='partial'?'partially_dried_'+type:'dried_'+type;
  const elements=food.map(e=>({...e,from:e.from.map((v,i)=>i===1?2+v*2:8+(v-8)*2),to:e.to.map((v,i)=>i===1?2+v*2:8+(v-8)*2)}));
  write(`assets/${ns}/models/item/${id}.json`,{parent:'minecraft:block/block',textures,elements,display:{gui:{rotation:[20,205,0],scale:[1.1,1.1,1.1]}}});
  write(`assets/${ns}/items/${id}.json`,{model:{type:'minecraft:model',model:`${ns}:item/${id}`}});
 }
}
