import fs from 'node:fs';
import zlib from 'node:zlib';
import crypto from 'node:crypto';
import {sampleBases} from './food-samples.mjs';
const root='src/main/resources/assets/smoke_kitchen',catalog=JSON.parse(fs.readFileSync('src/main/resources/data/smoke_kitchen/catalog.json'));
const fingerprint=new Set();let count=0;
for(const base of sampleBases)for(const prefix of ['','stale_','spoiled_']){
 const id=prefix+base,model=JSON.parse(fs.readFileSync(`${root}/models/item/${id}.json`)),item=JSON.parse(fs.readFileSync(`${root}/items/${id}.json`));
 if(item.model.type!=='minecraft:model'||model.parent!=='minecraft:block/block')throw Error(`Expected native volume: ${id}`);
 const heights=model.elements.flatMap(e=>[e.from[1],e.to[1]]),definition=catalog.items.find(x=>x.id===id);
 if(Math.min(...heights)!==0||Math.max(...heights)>definition.food_model_height||Math.max(...heights)<3)throw Error(`Invalid bottom/height profile: ${id}`);
 const used=new Set();for(const e of model.elements){for(let k=0;k<3;k++)if(e.from[k]<0||e.to[k]>16||e.to[k]<=e.from[k])throw Error(`Invalid cuboid: ${id}`);for(const face of Object.values(e.faces))used.add(face.texture.slice(1));}
 const bytes=[JSON.stringify(model.elements)];for(const key of [...used].sort()){const tex=model.textures[key];if(!tex.startsWith('smoke_kitchen:item/food_art/'))throw Error(`Unowned material: ${id}`);bytes.push(fs.readFileSync('src/main/resources/assets/'+tex.replace(':','/textures/')+'.png'));}
 const hash=crypto.createHash('sha256').update(Buffer.concat(bytes.map(x=>Buffer.isBuffer(x)?x:Buffer.from(x)))).digest('hex');if(fingerprint.has(hash))throw Error(`Indistinguishable used artwork: ${id}`);fingerprint.add(hash);count++;
}
const dir=`${root}/textures/item/food_art`;for(const name of fs.readdirSync(dir)){
 const png=fs.readFileSync(`${dir}/${name}`);if(png.readUInt32BE(16)!==16||png.readUInt32BE(20)!==16||png[25]!==6)throw Error(`Wrong native pixel dimensions: ${name}`);
 let data=[];for(let off=8;off<png.length;){const n=png.readUInt32BE(off),type=png.subarray(off+4,off+8).toString();if(type==='IDAT')data.push(png.subarray(off+8,off+8+n));off+=12+n;}
 const rows=zlib.inflateSync(Buffer.concat(data)),colors=new Set();for(let y=0;y<16;y++){if(rows[y*65]!==0)throw Error('Unexpected pixel filter');for(let x=0;x<16;x++){const off=y*65+1+x*4;if(rows[off+3]!==255)throw Error('Blended/transparent solid material');colors.add(rows.subarray(off,off+4).toString('hex'));}}
 if(colors.size>4)throw Error(`Palette exceeds hand-cluster budget: ${name}`);
}
console.log(`Checked ${count} original native food states, ground contact, independent used artwork and21 crisp16px palettes.`);
