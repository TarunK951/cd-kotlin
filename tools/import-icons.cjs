const fs=require('fs'),path=require('path');
const source=process.argv[2]||'D:/country',root=path.resolve(__dirname,'..');
const mapping={AccountBalanceWallet:['Wallet','wallet'],CalendarMonth:['Calendar','calendar'],FavoriteBorder:['Heart','heart'],Favorite:['HeartFilled','heart',true],FitnessCenter:['BicepsFlexed','biceps-flexed'],Home:['Home','house'],MoreHoriz:['More','ellipsis'],ShoppingBag:['ShoppingBag','shopping-bag'],Close:['Close','x'],StarBorder:['Star','star'],Star:['StarFilled','star',true],Eco:['Leaf','leaf'],LocationOn:['MapPin','map-pin'],Settings:['Settings','settings'],WorkspacePremium:['Crown','crown'],LocalOffer:['Tag','tag'],ChevronLeft:['ChevronLeft','chevron-left'],ExpandLess:['ChevronUp','chevron-up'],ExpandMore:['ChevronDown','chevron-down'],Search:['Search','search'],ArrowBack:['ArrowLeft','arrow-left']};
Object.assign(mapping,{FileText:['FileText','file-text'],Package:['Package','package'],Repeat:['Repeat','repeat'],Bell:['Bell','bell'],CircleHelp:['CircleHelp','circle-question-mark']});
const drawables=path.join(root,'app/src/main/res/drawable');fs.mkdirSync(drawables,{recursive:true});
const properties=[];
function pathData(type,a){
 const x=+(a.x||0),y=+(a.y||0),w=+a.width,h=+a.height,r=+(a.rx||0),cx=+a.cx,cy=+a.cy;
 if(type==='path')return a.d;
 if(type==='line')return `M${a.x1} ${a.y1}L${a.x2} ${a.y2}`;
 if(type==='circle'||type==='ellipse'){const rx=+(a.r||a.rx),ry=+(a.r||a.ry);return `M${cx-rx} ${cy}a${rx} ${ry} 0 1 0 ${2*rx} 0a${rx} ${ry} 0 1 0 ${-2*rx} 0`;}
 if(type==='rect')return `M${x+r} ${y}H${x+w-r}Q${x+w} ${y} ${x+w} ${y+r}V${y+h-r}Q${x+w} ${y+h} ${x+w-r} ${y+h}H${x+r}Q${x} ${y+h} ${x} ${y+h-r}V${y+r}Q${x} ${y} ${x+r} ${y}Z`;
 if(type==='polyline'||type==='polygon')return 'M'+a.points.trim().split(/[ ,]+/).map((v,i)=>i%2===0&&i>0?'L'+v:v).join(' ')+(type==='polygon'?'Z':'');
 throw Error('Unsupported SVG node '+type);
}
for(const [old,[name,file,filled]]of Object.entries(mapping)){
 const module={exports:{}};
 new Function('require','module',fs.readFileSync(path.join(source,'node_modules/lucide-react-native/dist/cjs/icons',file+'.js'),'utf8'))(()=>data=>data,module);
 const resource='ic_'+file.replaceAll('-','_')+(filled?'_filled':'');
 const paths=module.exports.node.map(([type,a])=>`    <path android:pathData="${pathData(type,a)}" android:fillColor="${filled?'#FF000000':'#00000000'}" android:strokeColor="#FF000000" android:strokeWidth="1.8" android:strokeLineCap="round" android:strokeLineJoin="round" />`).join('\n');
 fs.writeFileSync(path.join(drawables,resource+'.xml'),`<vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="24dp" android:height="24dp" android:viewportWidth="24" android:viewportHeight="24">\n${paths}\n</vector>\n`);
 properties.push(`    val ${name}: ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.${resource})`);
}
const ui=path.join(root,'app/src/main/java/com/countryd/app/ui');
fs.writeFileSync(path.join(ui,'CountryIcons.kt'),'package com.countryd.app.ui\n\nimport androidx.compose.runtime.Composable\nimport androidx.compose.ui.graphics.vector.ImageVector\nimport androidx.compose.ui.res.vectorResource\nimport com.countryd.app.R\n\n// Original Lucide paths, imported as Android vectors. See third-party/LUCIDE-LICENSE.\nobject CountryIcons {\n'+properties.join('\n')+'\n}\n');
for(const file of fs.readdirSync(ui).filter(f=>f.endsWith('.kt')&&f!=='CountryIcons.kt')){
 const target=path.join(ui,file);let code=fs.readFileSync(target,'utf8').replace(/^import androidx\.compose\.material\.icons.*\r?\n/gm,'');
 for(const [old,[name]]of Object.entries(mapping))code=code.replaceAll('Icons.Outlined.'+old,'CountryIcons.'+name).replaceAll('Icons.AutoMirrored.Outlined.'+old,'CountryIcons.'+name);
 fs.writeFileSync(target,code);
}
const licenses=path.join(root,'third-party');fs.mkdirSync(licenses,{recursive:true});fs.copyFileSync(path.join(source,'node_modules/lucide-react-native/LICENSE'),path.join(licenses,'LUCIDE-LICENSE'));
console.log('Imported '+properties.length+' source icons as native Android vectors.');
