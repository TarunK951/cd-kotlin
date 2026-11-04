const fs=require('fs'),path=require('path'),crypto=require('crypto');
const root=path.resolve(__dirname,'..');
const manifest=JSON.parse(fs.readFileSync(path.join(__dirname,'asset-checksums.json'),'utf8'));
const hash=file=>crypto.createHash('sha256').update(fs.readFileSync(file)).digest('hex');
const failures=[];
for(const asset of manifest){const target=path.join(root,'app/src/main/assets',asset.path);if(!fs.existsSync(target)||hash(target)!==asset.sha256)failures.push(asset.path);}
const catalogue=JSON.parse(fs.readFileSync(path.join(root,'app/src/main/assets/catalogue.json'),'utf8'));
if(failures.length){console.error(failures);process.exit(1)}
console.log(`${manifest.length} packaged assets passed checksum verification; ${catalogue.length} catalogue products loaded.`);
