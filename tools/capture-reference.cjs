const path=require('path'),fs=require('fs');
const source=process.argv[2]||'D:/country';
const {chromium}=require(path.join(source,'qa/feedback/node_modules/playwright'));
const root=path.resolve(__dirname,'..');
(async()=>{
 const b=await chromium.launch();try {
 const p=await b.newPage({viewport:{width:390,height:844},deviceScaleFactor:3});
 await p.goto('http://localhost:8084',{waitUntil:'networkidle',timeout:180000});
 const dir=path.join(root,'app/src/main/assets/native-art');fs.mkdirSync(dir,{recursive:true});
 const shot=async(name,h)=>{await p.locator(`svg[viewBox="0 0 390 ${h}"]`).first().locator('..').screenshot({path:path.join(dir,name+'.png')});await p.screenshot({path:path.join(root,'migration','source-'+name+'.png')});};
 await p.getByText('Get started',{exact:true}).waitFor();await shot('welcome',330);
 await p.locator('svg[viewBox="0 0 390 70"]').first().locator('..').screenshot({path:path.join(dir,'footer-hills.png')});
 await p.getByText('Get started',{exact:true}).click();await shot('mobile',262);
 await p.getByRole('button',{name:'Continue',exact:true}).click();await shot('otp',296);
 await p.getByRole('button',{name:'Verify & continue',exact:true}).click();await shot('address',236);
 await p.getByRole('button',{name:'Continue',exact:true}).click();await shot('vip',232);
 await p.getByRole('button',{name:'Unlock my free VIP',exact:true}).click();
 await p.getByRole('button',{name:'Explore milk products',exact:true}).waitFor({timeout:15000});
 await p.waitForTimeout(2500);await shot('reward',352);
 await p.getByRole('button',{name:'Explore milk products',exact:true}).click();
 await p.screenshot({path:path.join(root,'migration','source-milk-offer.png')});
 fs.writeFileSync(path.join(root,'migration','reference-browser-session.json'),JSON.stringify(await p.context().storageState(),null,2));
 console.log('Exported original FTUE illustration layers and reference screens; no controls were captured into app artwork.');
 } finally {await b.close()}
})().catch(e=>{console.error(e);process.exit(1)});
