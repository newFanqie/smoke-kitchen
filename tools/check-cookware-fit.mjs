// Exact positive-volume overlap audit, supplementing actual side/top client screenshots.
import fs from 'node:fs';
const root=new URL('../src/main/resources/assets/smoke_kitchen/models/block/',import.meta.url);
const read=id=>JSON.parse(fs.readFileSync(new URL(id+'.json',root),'utf8'));
const stoves=['clay_stove','brick_stove','double_stove','gas_stove'];
const cookware=['clay_pot','soup_pot','wok','frying_pan','steamer','baking_tray','mixing_bowl'];
let combinations=0;
for(const stove of stoves)for(const pot of cookware){
 const base=pot==='baking_tray'?13:11;
 const model=read(pot);
 const moved=model.elements.map(e=>({from:e.from.map((n,i)=>i===1?n+base:n),to:e.to.map((n,i)=>i===1?n+base:n)}));
 for(const a of read(stove).elements)for(const b of moved){
  const overlap=[0,1,2].map(i=>Math.min(a.to[i],b.to[i])-Math.max(a.from[i],b.from[i]));
  if(overlap.every(n=>n>1e-4))throw Error(`${stove}/${pot} positive-volume penetration: ${JSON.stringify({a,b,overlap})}`);
 }
 const floor=read(stove).elements[0].to[1];
 if(base<floor)throw Error(`${pot} below support floor`);
 if(base===floor&&!model.elements.some(e=>e.from[1]===0))throw Error(`${pot} has no base touching the support floor`);
 combinations++;
}
console.log(`Checked ${combinations} mounted cookware/stove pairs: no solid-volume penetration; feet supported by socket floor or counter rim.`);

let lids=0;
for(const pot of cookware){
 const lid=read('lid_'+pot);
 for(const a of read(pot).elements)for(const b of lid.elements){
  const overlap=[0,1,2].map(i=>Math.min(a.to[i],b.to[i])-Math.max(a.from[i],b.from[i]));
  if(overlap.every(n=>n>1e-4))throw Error(`${pot} lid/handle penetration: ${JSON.stringify({a,b})}`);
 }
 const min=Math.min(...lid.elements.map(e=>e.from[1]));
 const rim={frying_pan:3,wok:4,baking_tray:2,mixing_bowl:4}[pot]||7;
 if(min!==rim)throw Error(`${pot} lid floats above or sinks below its rim`);
 lids++;
}
console.log(`Checked ${lids} vessel lids: contact at each rim, no handle penetration.`);
