// Run only when intentionally refreshing the migration's source snapshot.
const fs = require('fs');
const path = require('path');
const crypto = require('crypto');
const source = process.argv[2] || 'D:/country';
const root = path.resolve(__dirname, '..');
const ts = require(path.join(source, 'node_modules/typescript'));
const walk = p => fs.readdirSync(p, {withFileTypes:true}).flatMap(e => e.isDirectory() ? walk(path.join(p,e.name)) : [path.join(p,e.name)]);
const files = walk(path.join(source,'src'));
const out = path.join(root,'app/src/main/assets'); fs.mkdirSync(out,{recursive:true});
for (const file of walk(path.join(source,'src/data'))) {
 const target=path.join(out,'data',path.relative(path.join(source,'src/data'),file));fs.mkdirSync(path.dirname(target),{recursive:true});fs.copyFileSync(file,target);
}
const mod = {exports:{}};
new Function('exports', ts.transpileModule(fs.readFileSync(path.join(source,'src/mockData.ts'),'utf8'),{compilerOptions:{module:ts.ModuleKind.CommonJS}}).outputText)(mod.exports);
fs.writeFileSync(path.join(out,'catalogue.json'),JSON.stringify(mod.exports.products,null,2));
fs.writeFileSync(path.join(out,'reference-data.json'),JSON.stringify(mod.exports,null,2));
const assets = new Set();
for (const asset of walk(path.join(source,'assets'))) assets.add(asset);
for (const file of files) for (const match of fs.readFileSync(file,'utf8').matchAll(/require\(['"]([^'"]+)['"]\)/g)) {
 const asset = path.resolve(path.dirname(file),match[1]); if(fs.existsSync(asset)&&fs.statSync(asset).isFile()&&asset.startsWith(path.join(source,'assets'))) assets.add(asset);
}
const manifest = [...assets].sort().map(file => {
 const relative = path.relative(source,file).replaceAll('\\','/');
 const target = path.join(out,relative); fs.mkdirSync(path.dirname(target),{recursive:true}); fs.copyFileSync(file,target);
 return {source:relative,sha256:crypto.createHash('sha256').update(fs.readFileSync(file)).digest('hex'),bytes:fs.statSync(file).size};
});
const fontRoot=path.join(source,'node_modules/@expo-google-fonts/onest');
for(const file of walk(fontRoot).filter(f=>f.endsWith('.ttf'))) {
 const weight=path.basename(file).match(/_(\d+)/)?.[1]; if(!weight)continue;
 const dest=path.join(root,'app/src/main/res/font'); fs.mkdirSync(dest,{recursive:true});fs.copyFileSync(file,path.join(dest,`onest_${weight}.ttf`));
}
const docs=path.join(root,'migration');fs.mkdirSync(docs,{recursive:true});
fs.writeFileSync(path.join(docs,'source-manifest.json'),JSON.stringify({source,importedAt:new Date().toISOString(),assets:manifest,sourceFiles:files.map(file=>({path:path.relative(source,file).replaceAll('\\','/'),sha256:crypto.createHash('sha256').update(fs.readFileSync(file)).digest('hex')}))},null,2));
console.log(`Imported ${mod.exports.products.length} products and ${assets.size} referenced assets, with source hashes.`);
