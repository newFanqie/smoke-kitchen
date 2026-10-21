// Original editable Minecraft-grid artwork. Large square color clusters, no raster blur.
import {cube,pixelModel} from './voxel-art.mjs';

// A round silhouette made from one-texel stair steps, not a smooth circular mesh.
export function disk(y,height,radius,texture,inner=0) {
 const out=[];
 for(let z=0;z<16;z++){
  let start=-1;
  for(let x=0;x<=16;x++){
   const d=(x+.5-8)**2+(z+.5-8)**2;
   const filled=x<16&&d<=radius*radius&&d>=inner*inner;
   if(filled&&start<0)start=x;
   if(!filled&&start>=0){out.push(cube([start,y,z],[x,y+height,z+1],texture));start=-1;}
  }
 }
 return out;
}
export function vessel(id) {
 if(id==='wok')return [...disk(0,1,4,'#dark'),...disk(1,1,5.5,'#dark',3.5),...disk(2,1,6.5,'#dark',4.8),...disk(3,1,7,'#dark',5.7),cube([-1,2,6],[2,3,10],'#dark'),cube([14,2,6],[17,3,10],'#dark')];
 if(id==='frying_pan')return [...disk(0,1,7,'#dark'),...disk(1,2,7,'#dark',5.7),cube([7,2,15],[9,3,22],'#base')];
 if(id==='clay_pot'||id==='soup_pot'){
  const t=id==='clay_pot'?'#clay':'#dark';
  return [...disk(0,1,5.5,t),...disk(1,5,7,t,5.5),...disk(6,1,6.5,t,5.3),cube([-1,4,6],[2,5,10],t),cube([14,4,6],[17,5,10],t)];
 }
 if(id==='fermentation_crock'||id==='pickling_crock')return [...disk(0,1,5,'#clay'),...disk(1,2,6,'#clay',4.5),...disk(3,8,7,'#clay',5.5),...disk(11,2,6.5,'#clay',5),...disk(13,1,6,'#clay',4.7)];
 return null;
}
export function vesselLid(wood=false,y=7){const cap=disk(y,1,6.5,wood?'#base':'#dark');if(!wood)for(const e of cap)e.faces.up={texture:'#rim'};return [...cap,cube([7,y+1,7],[9,y+2,9],wood?'#base':'#dark')];}

const toolRows={
 soybean:[
'................','................','......ooo.......','.....oyyyo......','.....oywyo......','......ooo.......','..ooo......ooo..','.oyyyo....oyyyo.','.oywyo....oywyo.','..ooo......ooo..','......ooo.......','.....oyyyo......','.....oywyo......','......ooo.......','................','................'],
cooked_soybean:[
'................','................','.....oooo.......','....owwwwo......','....owywwo......','.....oooo.......','..........oooo..','..oooo...owwwwo.','.owwwwo..owywwo.','.owywwo...oooo..','..oooo..........','......oooo......','.....owwwwo.....','.....owywwo.....','......oooo......','................'],
soy_sauce:[
'......oooo......','......oyyo......','......rrrr......','.......oo.......','......owoo......','.....odwddo.....','.....odwddo.....','.....oddddo.....','.....oddddo.....','.....oddddo.....','.....oddddo.....','.....oddddo.....','.....oddddo.....','.....oooooo.....','................','................'],
rice_vinegar:[
'......ooo.......','......oyo.......','......www.......','......sos.......','.....sawos......','....saawass.....','...saaawaaas....','...saaawaaas....','...saaaaaaas....','...saaaaaaas....','...saaaaaaas....','...saaaaaaas....','....sssssss.....','................','................','................'],
 iron_knife:[
 '................','......sssssssw..','.....sssssssw...','....sssssssw....','...sssssssw.....','..sssssssw......','..ddddddd.......','.....od.........','....ooo.........','...ooo..........','..owo...........','.ooo............','..o.............','................','................','................'],
 iron_spatula:[
 '................','........ddddd...','.......dsssssd..','......dssdsdsd..','......dssdsdsd..','.......dssssd...','........ddd.....','.......ds.......','......oo........','.....oo.........','....ow..........','...oo...........','..oo............','..o.............','................','................'],
 wooden_spatula:[
 '................','........ooooo...','.......oyyyyo...','......oyoyoyo...','......oyoyoyo...','.......oyyyo....','........oo......','.......oo.......','......oo........','.....oo.........','....oy..........','...oo...........','..oo............','..o.............','................','................'],
 sieve:[
 '................','.......ooooo....','......ossssso...','.....osdsdsdso..','.....ossdsdsso..','.....osdsdsdso..','......ossssso...','.......ooooo....','......oo........','.....oo.........','....oo..........','...oo...........','..oo............','..o.............','................','................'],
 ladle:[
 '................','.........ssss...','........sdddss..','.......sddddds..','.......sddddds..','........sssss...','........ss......','.......ss.......','......ss........','.....oo.........','....oo..........','...ow...........','..oo............','..o.............','................','................'],
 tongs:[
 '................','...........s.s..','..........ss.ss.','.........ss..s..','........ss..ss..','.......ss..ss...','......ss..ss....','.....ss..ss.....','....ss..ss......','...ss..ss.......','..ss..ss........','..s..ss.........','..s.ss..........','..sss...........','...s............','................']
};
export function toolModel(id){
 const base=id==='stone_knife'||id==='refined_knife'?'iron_knife':id;
 if(!toolRows[base])return null;
 const model=pixelModel(toolRows[base],{s:id==='stone_knife'?'gray':id==='refined_knife'?'cyan':'light_gray',w:'white',o:'brown',d:'black',y:'yellow'});
 // Flat, crisp inventory silhouette; all faces remain real model geometry.
 return model;
}

export function tofuModel(id){
 const t={cream:'minecraft:block/white_concrete',shade:'minecraft:block/white_terracotta',bean:'minecraft:block/yellow_terracotta',wood:'minecraft:block/spruce_planks',dark:'minecraft:block/brown_terracotta'};
 const b=(from,to,texture='#cream')=>cube(from,to,texture);
 let elements;
 if(id==='soaked_soybean')elements=[[2,3],[9,4],[5,10]].flatMap(([x,z])=>[b([x,3,z],[x+5,5,z+4],'#bean'),b([x+1,5,z],[x+4,6,z+4]),b([x+2,6,z+1],[x+3,6.1,z+3],'#shade')]);
 else if(id==='raw_bean_pulp'||id==='boiled_bean_pulp'||id==='okara')elements=[...disk(2,1,6,'#wood'),b([4,3,4],[12,5,12],id==='raw_bean_pulp'?'#bean':'#shade'),...[[4,5],[8,4],[7,9]].map(([x,z])=>b([x,5,z],[x+3,6,z+3],id==='okara'?'#bean':'#cream'))];
 else if(id==='soy_milk')elements=[...disk(2,1,4.5,'#wood'),...disk(3,8,5.5,'#shade',4.3),...disk(10,1,5.5,'#wood',4.3),...disk(10,0.1,4.3,'#cream')];
 else if(id==='soy_curds')elements=[...disk(2,1,6,'#wood'),b([3,3,4],[8,5,9],'#shade'),b([7,3,8],[13,6,12]),b([6,3,3],[12,7,7]),b([4,5,5],[7,6,8])];
 else if(id==='tofu')elements=[b([2,2,3],[9,6,10],'#shade'),b([2,6,3],[9,7,10]),b([8,3,7],[15,7,14],'#shade'),b([8,7,7],[15,8,14]),b([3,7,4],[4,7.1,5],'#shade'),b([10,8,10],[11,8.1,11],'#shade')];
 else return null;
 return {parent:'minecraft:block/block',textures:t,elements};
}
export const foodFamilies=['meat','fish','root','red','leaf','grain','dough','tofu'];
export const foodStages=['raw','setting','browning','cooked','poached','charred'];
export function foodModel(family,cut,stage){
 const colors={meat:['pink','white','orange','brown','white','black'],fish:['light_gray','white','yellow','brown','white','black'],root:['yellow','yellow','orange','orange','yellow','black'],red:['red','red','orange','red','red','black'],leaf:['lime','green','green','green','green','black'],grain:['white','white','yellow','yellow','white','black'],dough:['white','white','yellow','brown','white','black']};
 colors.tofu=['white','white','yellow','yellow','white','black'];
 const color=colors[family][foodStages.indexOf(stage)];
 const body=`minecraft:block/${color}_${stage==='raw'&&['meat','fish','red','leaf'].includes(family)?'concrete':'terracotta'}`;
 const textures={body,fat:stage==='charred'?'minecraft:block/black_concrete':'minecraft:block/white_terracotta',skin:stage==='charred'?'minecraft:block/black_concrete':'minecraft:block/brown_terracotta',leaf:'minecraft:block/green_terracotta',eye:'minecraft:block/black_concrete'};
 const b=(a,z,t='#body')=>cube(a,z,t);let elements;
 if(cut==='whole'){
  if(family==='fish')elements=[b([3,0,6],[10,1.5,10]),b([2,0,7],[3,1.5,9]),b([10,0,7],[12,1,9]),b([12,0,5],[14,1,11]),b([5,1.5,7],[9,1.7,8],'#fat'),b([3,1.5,6.5],[4,1.7,7.5],'#eye')];
  else if(family==='meat')elements=[b([4,0,5],[11,2,11]),b([5,0,4],[9,2,5]),b([4,2,5],[6,2.2,10],'#fat'),b([8,2,9],[10,2.2,11],'#fat')];
  else if(family==='leaf')elements=[b([4,0,4],[12,1,12]),b([5,1,5],[11,2,11]),b([7,2,4],[8,2.2,12],'#fat')];
  else elements=[b([5,0,4],[11,2,12]),b([4,0,6],[12,2,10]),...(family==='red'?[b([7,2,7],[9,2.5,9],'#leaf')]:[])];
 }else if(cut==='dice')elements=[b([6,0,6],[10,3,10]),...(family==='meat'?[b([6,3,6],[7,3.1,10],'#fat')]:[])];
 else if(cut==='slice')elements=[b([5,0,5],[11,1,11]),...(family==='meat'?[b([5,1,5],[6,1.1,11],'#fat')]:[])];
 else if(cut==='shred')elements=[b([7,0,4],[8,1,12])];
 else elements=[b([7,0,7],[9,1.5,9])];
 if((stage==='cooked'||stage==='browning')&&['meat','fish','root','dough'].includes(family)){const top=Math.max(...elements.map(e=>e.to[1]));elements.push(b([7,top,7],[8,top+.1,9],'#skin'));}
 return {parent:'minecraft:block/block',textures,elements};
}

// An original plated dish: stepped porcelain rim, red sauce, pale tofu and distinct mince.
export function dishModel(id){
 if(id!=='mapo_tofu')return null;
 const textures={plate:'minecraft:block/white_concrete',rim:'minecraft:block/light_gray_concrete',sauce:'minecraft:block/red_terracotta',oil:'minecraft:block/orange_terracotta',tofu:'minecraft:block/white_terracotta',top:'minecraft:block/white_concrete',meat:'minecraft:block/brown_terracotta',chili:'minecraft:block/red_concrete',pepper:'minecraft:block/black_terracotta'};
 const b=(a,z,t)=>cube(a,z,t);
 const elements=[...disk(0,1,5,'#rim'),...disk(1,1,7,'#plate'),...disk(2,1,7,'#plate',5.8),...disk(2,.4,5.8,'#sauce')];
 for(const [x,z,h] of [[4,4,2],[8,3,2],[10,7,2],[6,8,3],[3,8,2],[8,10,2]])elements.push(b([x,2.4,z],[x+2.5,2.4+h,z+2.5],'#tofu'),b([x,2.4+h,z],[x+2.5,2.6+h,z+2.5],'#top'));
 for(const [x,z] of [[7,4],[4,7],[9,7],[5,11],[10,10],[11,5],[7,8]])elements.push(b([x,2.4,z],[x+1,3.2,z+1],'#meat'));
 elements.push(b([5,2.4,3],[7,3,4],'#chili'),b([11,2.4,8],[12,3,10],'#chili'),b([6,2.4,6],[7,2.7,7],'#pepper'),b([9,2.4,10],[10,2.7,11],'#pepper'),b([3,2.4,6],[4,2.6,7],'#oil'));
 return {parent:'minecraft:block/block',textures,elements};
}
