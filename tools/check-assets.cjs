const fs=require('fs'),path=require('path'),crypto=require('crypto');
const root=path.resolve(__dirname,'..');
const manifest=JSON.parse(fs.readFileSync(path.join(root,'migration/source-manifest.json'),'utf8'));
const hash=file=>crypto.createHash('sha256').update(fs.readFileSync(file)).digest('hex');
const failures=[];
for(const asset of manifest.assets){const target=path.join(root,'app/src/main/assets',asset.source);if(!fs.existsSync(target)||hash(target)!==asset.sha256)failures.push(asset.source);}
const catalogue=JSON.parse(fs.readFileSync(path.join(root,'app/src/main/assets/catalogue.json'),'utf8'));
const result={verifiedAt:new Date().toISOString(),assets:manifest.assets.length,products:catalogue.length,failures};
fs.writeFileSync(path.join(root,'migration/asset-verification.json'),JSON.stringify(result,null,2)+'\n');
if(failures.length){console.error(failures);process.exit(1)}
console.log(`${manifest.assets.length} original assets match their source hashes; ${catalogue.length} catalogue products loaded.`);
