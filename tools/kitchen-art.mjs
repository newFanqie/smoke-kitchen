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

export const foodFamilies=['meat','fish','root','red','leaf','grain','dough'];
export const foodStages=['raw','setting','browning','cooked','poached','charred'];
export function foodModel(family,cut,stage){
 const colors={meat:['pink','white','orange','brown','white','black'],fish:['light_gray','white','yellow','brown','white','black'],root:['yellow','yellow','orange','orange','yellow','black'],red:['red','red','orange','red','red','black'],leaf:['lime','green','green','green','green','black'],grain:['white','white','yellow','yellow','white','black'],dough:['white','white','yellow','brown','white','black']};
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
