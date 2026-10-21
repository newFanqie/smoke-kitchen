// Original Minecraft-grid food models and hand-placed pixel clusters. No reference assets are copied.
import fs from 'node:fs';
import path from 'node:path';
import zlib from 'node:zlib';
import {cube} from './voxel-art.mjs';
import {disk} from './kitchen-art.mjs';
export const sampleBases=['mixed_stew','duck_meat','cooked_duck'];
export function sampleIdentity(id){const state=id.startsWith('spoiled_')?'spoiled':id.startsWith('stale_')?'stale':'fresh';return {base:id.replace(/^(stale_|spoiled_)/,''),state};}
export function sampleFoodModel(id){
 const {base,state}=sampleIdentity(id);if(!sampleBases.includes(base))return null;
 const tex=name=>`smoke_kitchen:item/food_art/${name}`;
 const textures={wood:tex('bowl_wood'),rim:tex('bowl_rim'),broth:tex('broth_'+state),skin:tex((base==='duck_meat'?'raw_skin_':'roast_skin_')+state),flesh:tex((base==='duck_meat'?'raw_flesh_':'roast_flesh_')+state),chunk:tex('stew_chunk_'+state),spot:tex('spoil_spot')};
 const b=(a,z,t)=>cube(a,z,'#'+t);let elements;
 if(base==='mixed_stew'){
  elements=[...disk(0,1,3.5,'#wood'),...disk(1,1,5,'#wood'),...disk(2,1,6,'#wood',4.8),...disk(3,2,7,'#wood',6),...disk(5,1,7,'#rim',5.7),...disk(4.5,.12,5.8,'#broth')];
  for(const[x,z,h]of [[4,6,.9],[8,4,.8],[9,9,1],[5,10,.7]])elements.push(b([x,4.62,z],[x+1.6,4.62+h,z+1.6],'chunk'));
  if(state==='spoiled')for(const[x,z]of [[5,4],[8,8],[10,6]])elements.push(b([x,4.63,z],[x+1.4,4.75,z+1.2],'spot'));
 }else{
  elements=[];
  for(let z=4;z<12;z++){
   let start=-1,end=-1;for(let x=0;x<16;x++)if((x+.5-8)**2/49+(z+.5-8)**2/16<=1){if(start<0)start=x;end=x+1;}
   if(start>=0){elements.push(b([start,0,z],[end,2.8,z+1],'flesh'),b([start,2.8,z],[end,3.8,z+1],'skin'));}
  }
  // Three shallow scoring marks read as skin-on duck breast rather than a chicken drumstick.
  for(const x of [5,8,11])elements.push(b([x,3.8,6],[x+.6,3.86,10],'flesh'));
  if(state==='spoiled')for(const[x,z]of [[4,7],[8,5],[10,9]])elements.push(b([x,3.87,z],[x+1.5,4,z+1.2],'spot'));
 }
 return {parent:'minecraft:block/block',gui_light:'side',textures:{...textures,particle:'#'+(base==='mixed_stew'?'wood':'skin')},display:{gui:{rotation:[30,225,0],translation:[0,base==='mixed_stew'?3:4,0],scale:[.9,.9,.9]},ground:{rotation:[0,0,0],translation:[0,1,0],scale:[.5,.5,.5]},fixed:{rotation:[0,0,0],translation:[0,0,0],scale:[.8,.8,.8]}},elements};
}
function crc32(bytes){let c=0xffffffff;for(const n of bytes){c^=n;for(let k=0;k<8;k++)c=c&1?0xedb88320^(c>>>1):c>>>1;}return(c^0xffffffff)>>>0;}
function chunk(type,data){const name=Buffer.from(type),len=Buffer.alloc(4),crc=Buffer.alloc(4);len.writeUInt32BE(data.length);crc.writeUInt32BE(crc32(Buffer.concat([name,data])));return Buffer.concat([len,name,data,crc]);}
function png(palette,pattern){const rows=Buffer.alloc(16*(16*4+1));for(let y=0;y<16;y++)for(let x=0;x<16;x++){const hex=palette[pattern(x,y)]||palette[0],off=y*65+1+x*4;rows[off]=parseInt(hex.slice(0,2),16);rows[off+1]=parseInt(hex.slice(2,4),16);rows[off+2]=parseInt(hex.slice(4,6),16);rows[off+3]=255;}const hdr=Buffer.alloc(13);hdr.writeUInt32BE(16,0);hdr.writeUInt32BE(16,4);hdr[8]=8;hdr[9]=6;return Buffer.concat([Buffer.from([137,80,78,71,13,10,26,10]),chunk('IHDR',hdr),chunk('IDAT',zlib.deflateSync(rows)),chunk('IEND',Buffer.alloc(0))]);}
export function writeFoodSampleTextures(root){
 const out=path.join(root,'assets/smoke_kitchen/textures/item/food_art');fs.mkdirSync(out,{recursive:true});
 const solid=(x,y)=>((y===4||y===11)&&x>=3&&x<=11)?1:(x===3&&y>=7&&y<=9?2:0);
 const patterns={wood:(x,y)=>y===3||y===12?1:(y===7&&x>2&&x<10||y===8&&x>9?2:0),rim:(x,y)=>y===2||y===13?1:(y===6&&x>4&&x<12?2:0),broth:(x,y)=>x>=3&&x<=6&&y>=3&&y<=4||x>=10&&x<=13&&y===10?1:(x>=7&&x<=9&&y>=11&&y<=12?2:0),skin:(x,y)=>y>=3&&y<=4&&x>=3&&x<=8||y===10&&x>=9&&x<=12?1:(y===12&&x>=3&&x<=6?2:0),flesh:solid,chunk:solid};
 const write=(name,palette,pattern)=>fs.writeFileSync(path.join(out,name+'.png'),png(palette,pattern));
 write('bowl_wood',['70513a','59402e','8a6748'],patterns.wood);write('bowl_rim',['aa8254','705132','bd9969'],patterns.rim);write('spoil_spot',['586642','465437','8b9462'],solid);
 const colors={fresh:{broth:['b67936','cd9853','9b602e'],raw_skin:['e9c8af','f0dac4','d3ab93'],raw_flesh:['b96565','d2867c','914b50'],roast_skin:['ae592b','cf8136','824026'],roast_flesh:['9d6447','ba8262','794a36'],stew_chunk:['d6b28a','e7c9a6','b48d68']},stale:{broth:['99834e','ae9960','7c6c45'],raw_skin:['c3b29e','d5c5af','aaa08d'],raw_flesh:['99726c','b38d82','7d605c'],roast_skin:['91805c','a7986b','746549'],roast_flesh:['867565','a18f77','6e6056'],stew_chunk:['b0a385','c4b797','928872']},spoiled:{broth:['73704b','8a895a','56563d'],raw_skin:['959378','b0ac88','73775e'],raw_flesh:['776f62','918777','5d594e'],roast_skin:['79704f','93875a','5d593f'],roast_flesh:['73695b','8c816d','585448'],stew_chunk:['99957b','b1ac8d','75735e']}};
 for(const[state,materials]of Object.entries(colors))for(const[name,palette]of Object.entries(materials))write(name+'_'+state,palette,patterns[name.includes('skin')?'skin':name.includes('flesh')?'flesh':name==='broth'?'broth':'chunk']);
}
