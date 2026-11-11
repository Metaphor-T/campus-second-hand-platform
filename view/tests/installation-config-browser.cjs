const fs=require('node:fs'),path=require('node:path'),assert=require('node:assert/strict'),{pathToFileURL}=require('node:url'),{spawnSync}=require('node:child_process')
const root=path.resolve(__dirname,'../..'),view=path.join(root,'view')
const out=path.join(root,'.local-backups/installation-config-results');fs.mkdirSync(out,{recursive:true})
const {chromium}=require(path.join(root,'.local-backups/a11y-tools/node_modules/playwright'))
;(async()=>{
 const {createServer}=await import(pathToFileURL(path.join(view,'node_modules/vite/dist/node/index.js')))
 const originalApi=process.env.VITE_API_BASE_URL
 const results=[],browser=await chromium.launch({channel:'msedge',headless:true})
 const image=fs.readFileSync(path.join(view,'public/logo.png'))
 try{
  for(const [label,value,expected] of [['no-env-default','', 'http://localhost:21090'],['explicit-override','  http://127.0.0.1:21990/  ','http://127.0.0.1:21990']]){
   process.env.VITE_API_BASE_URL=value
   const server=await createServer({root:view,configFile:path.join(view,'vite.config.ts'),logLevel:'error',server:{port:0,host:'127.0.0.1',open:false,hmr:{host:'127.0.0.1'}}})
   const context=await browser.newContext(),page=await context.newPage(),calls=[],sockets=[],errors=[]
   try{
    await server.listen();const site='http://127.0.0.1:'+server.httpServer.address().port
    assert.equal(JSON.parse(server.config.define['import.meta.env.VITE_API_BASE_URL']),expected)
    page.on('pageerror',e=>errors.push(e.message))
    await context.route('**/*',async route=>{
     const url=route.request().url();if(url.startsWith(site+'/'))return route.continue()
     assert.ok(url.startsWith(expected+'/'),'all API and images use the same backend: '+url);calls.push(url)
     if(new URL(url).pathname.startsWith('/files/'))return route.fulfill({contentType:'image/png',body:image})
     const p=new URL(url).pathname
     const data=p==='/user/auth'?{id:2,account:'fixture',username:'配置验收',avatar:'/files/installation-avatar.png',loginStatus:1}:p.includes('unread')?0:p.includes('count-by-status')?{}:[]
     return route.fulfill({contentType:'application/json',body:JSON.stringify({code:200,data,total:0})})
    })
    await context.routeWebSocket(/\/ws\/chat/,socket=>{sockets.push(socket.url());socket.close()})
    await page.goto(site+'/login');await page.evaluate(()=>{localStorage.setItem('TOKEN','fixture_only');localStorage.setItem('ROLE','USER')})
    await page.goto(site+'/user-home/orders');await page.locator('.user-orders').waitFor()
    await page.waitForFunction(()=>document.querySelector('.user-name')?.textContent?.includes('配置验收')||document.body.innerText.includes('配置验收'))
    await page.waitForTimeout(200)
    assert.ok(calls.some(u=>u===expected+'/user/auth'))
    assert.ok(calls.some(u=>u===expected+'/files/installation-avatar.png'))
    assert.ok(sockets.some(u=>u.startsWith(expected.replace(/^http/,'ws')+'/ws/chat?token=')))
    assert.deepEqual(errors,[])
    results.push({case:label,backend:expected,http:true,images:true,websocket:true,browserErrors:0})
   }finally{await context.close();await server.close()}
  }
 }finally{await browser.close();if(originalApi===undefined)delete process.env.VITE_API_BASE_URL;else process.env.VITE_API_BASE_URL=originalApi}
 const helper=require(path.join(root,'view/tests/helpers/test-config.cjs'))
 for(const env of [{},{CAMPUS_TEST_ADMIN_ACCOUNT:'admin'},{CAMPUS_TEST_ADMIN_PASSWORD:'private'},{CAMPUS_TEST_ADMIN_ACCOUNT:' ',CAMPUS_TEST_ADMIN_PASSWORD:'private'}])assert.throws(()=>helper.getAdminCredentials(env),/CAMPUS_TEST_ADMIN_ACCOUNT/)
 assert.deepEqual(helper.getAdminCredentials({CAMPUS_TEST_ADMIN_ACCOUNT:' sample ',CAMPUS_TEST_ADMIN_PASSWORD:'  preserve spaces  '}),{account:'sample',password:'  preserve spaces  '})
 const names=['admin-layout-browser.cjs','admin-sidebar-browser.cjs','campus-verification-browser.cjs','identity-permissions-browser.cjs','player2-acceptance.cjs','reading-layout-browser.cjs','statistics-dashboard-browser.cjs','user-profile-browser.cjs']
 for(const name of names){
  const script=path.join(root,'view/tests',name),env={...process.env};delete env.CAMPUS_TEST_ADMIN_ACCOUNT;delete env.CAMPUS_TEST_ADMIN_PASSWORD
  assert.equal(spawnSync(process.execPath,['--check',script]).status,0)
  const attempt=spawnSync(process.execPath,[script],{cwd:out,env,encoding:'utf8'})
  assert.equal(attempt.status,1);assert.ok(attempt.stderr.includes('CAMPUS_TEST_ADMIN_ACCOUNT'));assert.ok(!attempt.stderr.includes('sql/db.sql'))
 }
 results.push({case:'explicit-test-credentials',missingInputsRejected:4,passwordPreserved:true,legacyScripts:8,allFailClearlyBeforeBrowserWithoutCredentials:true,noSqlParsing:true})
 const output=path.join(out,'results.json');fs.writeFileSync(output,JSON.stringify({mode:'blank or explicit API override; mocked API, no database writes',results},null,2));console.log(JSON.stringify({passed:results.length,results}))
})().catch(e=>{console.error(e);process.exitCode=1})
