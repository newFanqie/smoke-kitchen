import fs from 'node:fs';
import path from 'node:path';
import {cube as b} from './voxel-art.mjs';
export function curedMeatResources(root){
 const write=(p,data)=>{p=path.join(root,p);fs.mkdirSync(path.dirname(p),{recursive:true});fs.writeFileSync(p,JSON.stringify(data,null,2)+'\n');};
 const stages=['salting_pork','salted_pork','partially_dried_salted_pork','dried_salted_pork','cooked_salted_pork'];
 for(let index=0;index<stages.length;index++)for(const state of ['fresh','stale','spoiled']){
  const base=stages[index],id=(state==='fresh'?'':state+'_')+base,dry=index>=2,cooked=index===4;
  const textures={body:'minecraft:block/'+(state==='spoiled'?'green_terracotta':state==='stale'?'light_gray_terracotta':cooked?'brown_terracotta':index===3?'red_terracotta':'pink_terracotta'),fat:'minecraft:block/'+(state==='spoiled'?'lime_terracotta':dry?'terracotta':'white_terracotta'),salt:'minecraft:block/'+(state==='spoiled'?'green_concrete':'white_concrete'),particle:'minecraft:block/'+(cooked?'brown_terracotta':'pink_terracotta')};
  let elements=[];const width=index===0?2.8:index===1?2.5:index===2?2.1:index===3?1.7:2.2;
  for(let j=0;j<2;j++){const x=4+j*4.4;elements.push(b([x,2,6],[x+width,11-index*.6,8.4],'#body'),b([x+.25,2,5.95],[x+.7,10-index*.6,6.1],'#fat'),b([x+width-.5,3,5.9],[x+width-.1,4.2,6.05],'#salt'));if(state==='spoiled')elements.push(b([x+.8,6,5.85],[x+1.35,7,6.05],'#salt'));}
  if(index===0)elements.push(b([5,3,8.4],[6.2,3.8,8.6],'#salt'),b([9.5,8,8.4],[10.2,8.7,8.6],'#salt'));
  write('assets/smoke_kitchen/models/item/'+id+'.json',{parent:'minecraft:block/block',textures,elements,display:{gui:{rotation:[20,205,0],scale:[1,1,1]}}});write('assets/smoke_kitchen/items/'+id+'.json',{model:{type:'minecraft:model',model:'smoke_kitchen:item/'+id}});
 }
 for(const stage of ['fresh','partial','dried']){const dry=stage==='dried',textures={body:'minecraft:block/'+(dry?'red_terracotta':'pink_terracotta'),fat:'minecraft:block/'+(dry?'terracotta':'white_terracotta'),rope:'minecraft:block/white_wool',particle:'minecraft:block/pink_terracotta'};const w=dry?.75:stage==='partial'?.95:1.15;write('assets/smoke_kitchen/models/block/drying/salted_pork_'+stage+'.json',{parent:'minecraft:block/block',textures,elements:[b([8-w,.5,7.5],[8+w,4.9,8.5],'#body'),b([8-w,.7,7.45],[8-w+.35,4.7,7.55],'#fat'),b([7.8,4.9,7.8],[8.2,6,8.2],'#rope')]});write('assets/smoke_kitchen/items/drying/salted_pork_'+stage+'.json',{model:{type:'minecraft:model',model:'smoke_kitchen:block/drying/salted_pork_'+stage}});}
}
