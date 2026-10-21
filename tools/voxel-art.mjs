// Original editable voxel artwork. Minecraft supplies the material textures at runtime.
// Each inventory object is actual cuboid geometry, not a front view of a flat texture.
export function cube(from,to,texture='#base') {
 const face={texture};return {from,to,faces:Object.fromEntries(['north','south','east','west','up','down'].map(x=>[x,face]))};
}
const material=color=>`minecraft:block/${color}_concrete`;
const palette={o:'brown',d:'black',w:'white',s:'light_gray',g:'green',l:'lime',r:'red',p:'pink',y:'yellow',a:'orange',b:'blue',c:'cyan',v:'purple'};
export function pixelModel(rows,colors={}) {
 const textures=Object.fromEntries(Object.entries({...palette,...colors}).map(([k,v])=>[k,v.includes(':')?v:material(v)]));
 const elements=[];
 for(let y=0;y<rows.length;y++)for(let x=0;x<rows[y].length;) {
  const color=rows[y][x];if(color==='.') {x++;continue;}
  let end=x+1;while(end<rows[y].length && rows[y][end]===color)end++;
  elements.push(cube([x,15-y,7],[end,16-y,9],'#'+color));x=end;
 }
 return {parent:'minecraft:block/block',gui_light:'front',textures,display:{gui:{rotation:[0,0,0],translation:[0,0,0],scale:[1,1,1]},fixed:{rotation:[0,180,0],translation:[0,0,0],scale:[1,1,1]}},elements};
}
export function inventoryModel(item,index=0) {
 const id=item.id;
 if(item.variant_of){
  const rows=Array.from({length:16},()=>'.'.repeat(16));
  const color={used:'y',broken:'r',stale:'o',spoiled:'g'}[item.state]||'s';
  rows[12]='............oooo';rows[13]='............o'+color+color+'o';rows[14]='............o'+color+color+'o';rows[15]='............oooo';
  return pixelModel(rows);
 }
 const grid=Array.from({length:16},()=>Array(16).fill('.'));
 const set=(x,y,c)=>{if(x>=0&&x<16&&y>=0&&y<16)grid[y][x]=c;};
 const rect=(x,y,w,h,c)=>{for(let j=y;j<y+h;j++)for(let i=x;i<x+w;i++)set(i,j,c);};
 const oval=(cx,cy,rx,ry,c)=>{for(let y=0;y<16;y++)for(let x=0;x<16;x++)if((x-cx)**2/rx**2+(y-cy)**2/ry**2<=1)set(x,y,c);};
 const colors={};
 if(id.includes('spawn_egg') || id.endsWith('_egg')) {
  oval(8,9,5,6,'o');oval(8,9,4,5,'w');rect(7,3,2,3,'w');
  if(id.includes('spawn_egg')) { for(let i=0;i<5;i++)rect(5+(i*3+index)%6,6+(i*2)%7,2,2,'g'); colors.g=['green','yellow','brown','cyan','red','gray','purple'][index%7]; }
  else {rect(id==='duck_egg'?5:9,7,2,2,'s');colors.w=id==='duck_egg'?'light_blue':'white';}
 } else if(id.includes('crab') && !item.meal && !id.includes('bucket')) {
  rect(5,6,7,6,'o');rect(6,7,5,4,'r');
  for(const x of [2,12]){rect(x,4,3,4,'r');rect(x,3,1,2,'r');rect(x+2,3,1,2,'r');}
  for(const y of [8,11,13]){rect(2,y,3,1,'r');rect(12,y,3,1,'r');}
  set(7,5,'d');set(10,5,'d');
 } else if(id.includes('shrimp') && !item.meal && !id.includes('bucket')) {
  oval(9,8,5,5,'o');oval(9,8,4,4,'a');oval(9,8,2,2,'.');rect(2,4,5,3,'a');rect(1,3,3,1,'a');set(4,5,'d');rect(4,11,3,2,'a');
 } else if(id.includes('meat') || /^cooked_(duck|goose|boar)$/.test(id) || id==='rendered_fat' || id==='raw_skewers') {
  oval(8,9,6,4,'o');oval(8,8,5,4,'p');oval(6,6,3,2,'r');rect(8,6,2,2,'w');rect(11,9,2,2,'w');
  if(id.includes('duck')){rect(3,10,4,3,'p');colors.p='orange';}
  if(id.includes('goose')){rect(9,3,3,3,'p');colors.p='pink';}
  if(id.includes('boar')){rect(5,8,3,1,'w');rect(7,11,3,1,'w');colors.r='red';colors.p='red';}
  if(id.startsWith('cooked_')){colors.p='brown';colors.r='orange';}
  if(id==='rendered_fat'){colors.p='white';colors.r='light_gray';}
  if(id==='raw_skewers')for(let i=1;i<15;i++)set(i,15-i,'o');
 } else if(item.meal) {
  oval(8,10,7,4,'o');oval(8,9,6,3,'w');oval(8,8,5,3,'y');
  const seed=[...id].reduce((s,c)=>s+c.charCodeAt(0),0);
  for(let i=0;i<6;i++){set(4+(seed+i*3)%9,6+(seed+i*7)%5,i%2?'g':'r');}
  colors.y=['yellow','orange','brown','lime','white','pink','red'][index%7];
 } else if(item.seasoning || /jar|sauce|oil|vinegar|jug|canister|milk_portion|bucket/.test(id)) {
  rect(5,3,6,2,'o');rect(4,5,8,9,'o');rect(5,5,6,8,'s');rect(6,6,4,6,'a');rect(6,8,4,3,'w');set(5,6,'w');
  const tint={salt:'white',spice:'red',oil:'yellow',soy:'brown',vinegar:'orange'};
  colors.a=/^empty_/.test(id)?'light_gray':tint[item.seasoning]||(/gas/.test(id)?'cyan':/water/.test(id)?'blue':/milk/.test(id)?'white':'light_gray');
  // The label is a tiny ingredient silhouette, and cap bands identify similar bottles.
  rect(7,8,2,2,['g','r','y','b','o'][index%5]);set(5+index%6,4,'w');
  if(/bucket/.test(id)){rect(3,4,1,6,'s');rect(12,4,1,6,'s');rect(4,3,8,1,'s');}
 } else if(/knife|spatula|ladle|tongs|chopsticks|rolling_pin|thermometer|sieve/.test(id)) {
  for(let i=2;i<14;i++){set(i,15-i,'o');set(i+1,15-i,'o');}
  if(id.includes('knife')) { for(let y=2;y<9;y++)rect(8,y,Math.max(1,6-y/2|0),1,'s');rect(8,8,5,1,'d');colors.s=id==='stone_knife'?'gray':id==='refined_knife'?'cyan':'light_gray'; }
  else if(id.includes('spatula')){rect(8,2,6,5,'s');for(let x=9;x<14;x+=2)rect(x,3,1,3,'d');colors.s=id==='wooden_spatula'?'brown':'light_gray';}
  else if(id==='ladle'){oval(11,4,4,3,'s');oval(11,4,2,1,'d');}
  else if(id==='tongs'){for(let i=2;i<13;i++){set(i,15-i,'s');set(i+2,15-i,'s');}rect(10,2,4,1,'s');}
  else if(id==='sieve'){oval(10,5,5,4,'o');oval(10,5,4,3,'s');for(let y=3;y<8;y+=2)for(let x=7;x<14;x+=2)set(x,y,'d');}
  else if(id==='thermometer'){rect(7,2,3,10,'s');oval(8,12,3,3,'r');rect(8,4,1,8,'r');}
  else {rect(7,3,6,5,'o');set(8,3,'w');}
 } else if(/lid|plate|bowl|basket|cloth|book/.test(id)) {
  if(id.includes('book')){rect(3,2,11,12,'o');rect(4,3,8,10,'w');rect(4,3,1,10,'r');for(let y=5;y<12;y+=2)rect(6,y,5,1,'o');}
  else {oval(8,9,7,4,'o');oval(8,8,6,3,'w');oval(8,8,4,2,'s');if(id.includes('lid'))rect(7,4,3,2,'o');if(id.includes('cloth')){rect(3,3,10,10,'r');for(let y=4;y<13;y+=3)rect(3,y,10,1,'w');}}
 } else if(/tofu|dough|dumplings|flour|raw_apple_pie/.test(id)) {
  if(id==='tofu'){rect(3,5,10,8,'s');rect(3,4,9,7,'w');rect(3,10,9,1,'o');}
  else if(id==='raw_dumplings'){for(const [x,y]of [[5,6],[10,8],[5,11]]){oval(x,y,4,2,'o');oval(x,y-1,3,2,'w');set(x,y-2,'s');}}
  else if(id==='flour'){rect(4,3,8,11,'o');rect(5,4,6,9,'w');rect(5,2,6,2,'s');rect(7,7,2,4,'y');}
  else if(id==='raw_apple_pie'){oval(8,10,7,4,'o');oval(8,8,6,3,'y');for(const x of [5,8,11])rect(x,6,2,3,'r');}
  else {oval(8,10,6,4,'o');oval(8,9,5,3,'w');rect(5,7,2,1,'y');if(id==='fermented_dough'){set(8,8,'s');set(10,10,'s');}}
 } else if(item.ingredient || /seeds|chili|pepper|sesame|cumin|rapeseed|shiitake/.test(id)) {
  const color={tomato:'r',chili:'r',cucumber:'g',cabbage:'l',eggplant:'v',onion:'p',ginger:'y',garlic:'w',shiitake:'o',scallion:'g',rice:'w',soybean:'l'}[id]||['y','g','o','w','r'][index%5];
  oval(8,10,5,4,'o');oval(8,9,4,4,color);rect(7,3,2,3,'g');rect(9,4,3,1,'l');rect(6,7,2,1,'w');
  if(/rice|sesame|cumin|seeds|pepper|rapeseed/.test(id)){grid.forEach(r=>r.fill('.'));for(let i=0;i<7;i++){const x=3+(i*3+index)%10,y=3+(i*5)%10;rect(x,y,2,3,color);set(x,y,'w');}}
  if(id==='scallion'){grid.forEach(r=>r.fill('.'));for(let i=0;i<3;i++){rect(4+i*3,3+i,2,8,'g');rect(4+i*3,11+i,2,3,'w');}}
  if(id==='shiitake'||id==='dried_shiitake'){grid.forEach(r=>r.fill('.'));rect(7,7,3,7,'w');oval(8,7,6,4,'o');rect(3,8,11,1,'s');rect(5,5,2,1,'y');}
  if(['cucumber','eggplant','chili','dried_chili'].includes(id)){grid.forEach(r=>r.fill('.'));oval(8,9,id.includes('chili')?2:3,5,color);rect(7,2,2,3,'g');rect(6,6,1,4,'w');}
 } else {
  oval(8,9,6,4,'o');oval(8,8,5,3,'y');rect(5,6,2,1,'w');rect(7+index%4,9,2,2,'a');
  colors.y=id==='burnt_food'?'black':['yellow','white','light_gray','orange','brown','pink'][index%6];
 }
 // A physical one-voxel maker's mark also keeps same-family items independently identifiable.
 if(!id.endsWith('_spawn_egg'))set(2+index%12,14,['o','s','g','r'][Math.floor(index/12)%4]);
 return pixelModel(grid.map(r=>r.join('')),colors);
}

export function stationGeometry(s,models) {
 const b=cube,base=models[s.model];
 const front=()=>{const pane=b([3,3,-.02],[13,10,0],'#firebox');pane.faces={north:{texture:'#firebox',uv:[3,7,13,14]}};return pane;};
 const burner=(x,z,size=8)=>[b([x,14,z],[x+size,15,z+size],'#dark'),b([x,15,z+size/2-1],[x+size,16,z+size/2+1],'#metal'),b([x+size/2-1,15,z],[x+size/2+1,16,z+size],'#metal')];
 if(s.id==='oven')return [b([0,0,0],[16,4,16],'#stone'),b([0,4,0],[2,14,16],'#stone'),b([14,4,0],[16,14,16],'#stone'),b([2,4,14],[14,14,16],'#dark'),b([0,14,0],[16,16,16],'#stone'),b([2,4,1],[14,5,14],'#metal'),b([2,13,1],[14,14,14],'#dark'),{...b([3,1,-.02],[13,3,0],'#firebox'),faces:{north:{texture:'#firebox',uv:[3,7,13,14]}}}];
 // Closed body below a recessed socket: cookware rests at y=11; counter rim at y=13.
 const stoveShell=(body,top)=>{const core=b([0,0,0],[16,11,16],body);core.faces.up={texture:'#dark'};return [core,b([0,11,0],[1,13,16],top),b([15,11,0],[16,13,16],top),b([1,11,0],[15,13,1],top),b([1,11,15],[15,13,16],top)];};
 if(s.id==='clay_stove')return [...stoveShell('#clay','#stone'),front()];
 if(s.id==='brick_stove')return [...stoveShell('#base','#stone'),front()];
 if(s.id==='double_stove')return [...stoveShell('#stone','#metal'),front(),b([2,9,-.5],[4,11,0],'#dark')];
 if(s.id==='gas_stove')return [...stoveShell('#blue','#metal'),b([2,8,-1],[5,10,0],'#dark'),b([11,8,-1],[14,10,0],'#dark'),b([6,0,14],[10,3,17],'#copper')];
 if(s.id==='gas_pipe')return [b([6,0,6],[10,16,10],'#copper'),b([0,6,6],[16,10,10],'#copper'),b([6,6,0],[10,10,16],'#copper'),b([5,5,5],[11,11,11],'#metal')];
 if(s.id==='gas_tank')return [b([3,0,3],[13,2,13],'#dark'),b([2,2,2],[14,12,14],'#blue'),b([4,12,4],[12,14,12],'#blue'),b([7,14,7],[9,16,9],'#copper'),b([5,15,7],[11,16,9],'#dark'),b([5,6,1.9],[11,10,2],'#metal')];
 if(s.id==='gas_extractor')return [b([0,0,0],[16,3,16],'#metal'),b([5,3,5],[11,12,11],'#dark'),b([2,11,2],[14,15,14],'#metal'),b([7,0,7],[9,16,9],'#copper'),b([1,4,1],[3,11,3],'#copper'),b([13,4,13],[15,11,15],'#copper')];
 if(s.id==='gas_deposit')return [b([0,0,0],[16,16,16],'#stone'),b([3,5,-.01],[6,12,0],'#blue'),b([10,2,-.01],[13,8,0],'#dark'),b([16,6,5],[16.01,13,8],'#blue')];
 if(s.id==='icebox')return [b([0,0,0],[16,16,16],'#metal'),b([1,2,-.1],[15,15,0],'#ice'),b([12,6,-1],[13,11,0],'#dark')];
 if(s.id==='grain_bin')return [b([0,0,0],[16,14,16]),b([0,14,2],[16,15,16]),b([2,13,0],[14,14,14],'#grain'),b([0,3,-.1],[16,5,0],'#metal')];
 if(s.id==='spice_rack')return base;
 if(s.id==='dish_rack')return [...base,...[3,7,11].map(x=>b([x,3,6],[x+1,7,13],'#clay'))];
 if(s.id==='spice_trellis')return [b([1,0,0],[3,16,2],'#stake'),b([13,0,0],[15,16,2],'#stake'),...[4,9,14].map(y=>b([3,y,.75],[13,y+.5,1.25],'#twine')),b([7.75,1,.75],[8.25,15,1.25],'#twine')];
 if(s.id==='drying_rack')return [...base,b([4,15,7],[6,20,8],'#clay'),b([10,15,7],[12,20,8],'#grain')];
 if(s.id==='mixing_table')return [...base,b([5,16,5],[11,18,11],'#clay'),b([2,16,3],[4,17,13])];
 if(s.id==='soup_pot')return [...base,b([-1,4,6],[1,6,10],'#metal'),b([15,4,6],[17,6,10],'#metal')];
 if(s.id==='clay_pot')return base.map(e=>({...e,faces:Object.fromEntries(Object.keys(e.faces).map(f=>[f,{texture:'#clay'}]))}));
 if(s.id==='mixing_bowl')return [b([4,0,4],[12,1,12],'#clay'),b([2,1,2],[4,4,14],'#clay'),b([12,1,2],[14,4,14],'#clay'),b([4,1,2],[12,4,4],'#clay'),b([4,1,12],[12,4,14],'#clay')];
 if(s.id==='soup_tureen')return [...base,b([0,4,6],[2,6,10],'#clay'),b([14,4,6],[16,6,10],'#clay'),b([4,7,4],[12,8,12],'#clay')];
 if(s.id==='baking_tray')return [b([0,0,0],[16,1,16],'#metal'),b([0,1,0],[1,2,16],'#metal'),b([15,1,0],[16,2,16],'#metal'),b([1,1,0],[15,2,1],'#metal'),b([1,1,15],[15,2,16],'#metal')];
 if(s.id==='frying_pan')return [...base,b([7,1,16],[9,2,23],'#dark')];
 if(s.id==='salt_pan')return [...base,b([3,1,3],[13,1.3,13],'#salt')];
 if(s.id==='pickling_crock')return [...base,b([0,8,0],[16,10,16],'#leaf')];
 if(s.id==='compost_bin')return [...base,b([3,14,3],[13,15,13],'#dirt')];
 return base;
}

export function cropModel(crop,age,index) {
 if(crop.id==='rice_paddy')return {parent:`minecraft:block/wheat_stage${age}`,textures:{particle:`minecraft:block/wheat_stage${age}`}};
 if(crop.id==='black_pepper'&&!crop.wild){
  const b=cube,h=2+age*1.7,stem=[];
  stem.push(b([7,-1,7],[9,Math.max(.5,h*.25),9],'#stem'));
  for(let n=0;n<4;n++){const z=7-n*2,y=h*(n+1)/4;stem.push(b([7,y-h/4,z],[8,y,z+1],'#stem'));if(n<3)stem.push(b([7,y-.5,z-2],[8,y,z+1],'#stem'));}
  if(age>0)stem.push(b([7,h-.5,-1],[8,h,1],'#stem'));
  for(const [x,z,k]of [[3,7,.35],[8,4,.55],[3,1,.8]]){const y=Math.max(.5,h*k);stem.push(b([x,y,z],[x+4,y+.5,z+3],'#leaf'));}
  if(age>=4)for(const [x,z]of [[5,0],[9,2]])for(let n=0;n<3;n++)stem.push(b([x,h-1.4-n*1.1,z],[x+1,h-.5-n*1.1,z+1],'#fruit'));
  return {parent:'minecraft:block/block',textures:{stem:'minecraft:block/green_terracotta',leaf:'minecraft:block/moss_block',fruit:age===7?'minecraft:block/black_terracotta':'minecraft:block/green_terracotta',particle:'minecraft:block/moss_block'},elements:stem};
 }
 if(crop.id==='sichuan_pepper'){
  const b=cube,h=2+age*1.6,e=[b([7,-1,7],[9,h,9],'#stem')];
  for(const [x,z]of [[3,4],[9,3],[4,10],[10,9]]){const y=h*.6;e.push(b([Math.min(x,8),y,Math.min(z,8)],[Math.max(x+1,9),y+1,Math.max(z+1,9)],'#stem'),b([x,y+1,z],[x+3,y+2,z+3],'#leaf'));if(age>3)for(let n=0;n<2;n++)e.push(b([x+n*1.5,y+.2,z-.5],[x+1+n*1.5,y+1.2,z+.5],'#fruit'));}
  return {parent:'minecraft:block/block',textures:{stem:'minecraft:block/brown_terracotta',leaf:'minecraft:block/moss_block',fruit:age===7?'minecraft:block/red_terracotta':'minecraft:block/green_terracotta',particle:'minecraft:block/moss_block'},elements:e};
 }
 const b=cube,h=2+age*1.6;
 const colors={tomato:'red',chili:'red',soybean:'lime',rapeseed:'yellow',scallion:'green',ginger:'yellow',garlic:'white',cucumber:'green',cabbage:'lime',onion:'pink',eggplant:'purple',sesame:'white',sichuan_pepper:'red',black_pepper:'black',cumin:'brown',shiitake:'brown',rice_paddy:'yellow'};
 const textures={stem:material('green'),leaf:material(age<4?'lime':'green'),fruit:material(colors[crop.id]),particle:material('green')};
 if(crop.id==='shiitake'){textures.stem='minecraft:block/mushroom_stem';textures.fruit='minecraft:block/brown_mushroom_block';textures.particle=textures.fruit;}
 let elements=[b([7,0,7],[9,h,9],'#stem'),b([3,h*.4,7],[13,h*.4+1,9],'#leaf'),b([7,h*.65,3],[9,h*.65+1,13],'#leaf')];
 if(['rice_paddy','sesame','rapeseed','cumin','black_pepper','sichuan_pepper'].includes(crop.id)) {
  elements=[];for(const [x,z]of [[4,4],[10,6],[6,11]]){elements.push(b([x,0,z],[x+1,h,z+1],'#stem'));if(age>3)elements.push(b([x-1,h-3,z],[x+2,h,z+2],'#fruit'));}
 } else if(crop.id==='shiitake')elements=[b([7,0,7],[9,h*.5,9],'#stem'),b([4,h*.5,4],[12,h*.5+2+age*.2,12],'#fruit')];
 else if(['cabbage','onion','garlic','ginger'].includes(crop.id))elements=[b([7,0,7],[9,h+1,9],'#leaf'),b([8-h*.4,0,8-h*.4],[8+h*.4,1+h*.6,8+h*.4],'#fruit'),b([3,h*.3,7],[13,h*.3+1,9],'#leaf')];
 else if(crop.id==='scallion')elements=[...[[4,5],[8,8],[11,4]].flatMap(([x,z])=>[b([x,0,z],[x+1,h*.4,z+1],'#fruit'),b([x,h*.4,z],[x+1,h,z+1],'#leaf')])];
 else if(age>3){const size=crop.id==='chili'?1:2;elements.push(b([3,h*.5,5],[3+size,h*.5+2+age*.3,5+size],'#fruit'),b([10,h*.6,9],[10+size,h*.6+2+age*.2,9+size],'#fruit'));}
 // Alternate planted-row placement gives each species a distinct profile even as a seedling.
 elements=elements.map(e=>({...e,from:e.from.map((v,i)=>i===0?v+(index%3-1)*.2:v),to:e.to.map((v,i)=>i===0?v+(index%3-1)*.2:v)}));
 return {parent:'minecraft:block/block',textures,elements};
}
