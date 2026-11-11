const adminCredentials = require('./helpers/test-config.cjs').getAdminCredentials()
const {chromium}=require('../../.local-backups/a11y-tools/node_modules/playwright')
const fs=require('node:fs'),path=require('node:path'),assert=require('node:assert/strict')
const api='http://127.0.0.1:21090',site='http://127.0.0.1:21091',out='.local-backups/identity-permissions-results'
fs.mkdirSync(out,{recursive:true})
;(async()=>{
 const browser=await chromium.launch({channel:'msedge',headless:true}),context=await browser.newContext(),page=await context.newPage()
 const results=[],errors=[],created=[],avatars=[],deleted=new Set();let rootToken,socket
 const record=test=>results.push({test,passed:true})
 const req=(method,url,token,data)=>context.request.fetch(api+url,{method,headers:token?{Authorization:`Bearer ${token}`}:{},...(data!==undefined?{data}:{} )})
 const get=async(url,token)=>{const response=await req('GET',url,token);assert.equal(response.status(),200);const body=await response.json();assert.equal(body.code,200);return body.data}
 const success=async(method,url,token,data)=>{const response=await req(method,url,token,data);assert.equal(response.status(),200);const body=await response.json();assert.equal(body.code,200);return body.data}
 const login=async(role,account,password)=>success('POST',`/${role}/login`,undefined,{account,password})
 const profile=token=>get('/user/profile',token)
 const password='IdentityTest!2026',nextPassword='IdentityNext!2026'
 page.on('pageerror',error=>errors.push(error.message))
 try {
  rootToken=(await login('admin',adminCredentials.account,adminCredentials.password)).token
  const rootBefore=await get('/admin/auth',rootToken),stamp=Date.now().toString(36)
  const register=async suffix=>{
   const account=`identity_${suffix}_${stamp}`
   await success('POST','/user/register',undefined,{account,password,username:'权限验收临时同学',role:'ADMIN',id:1})
   const token=(await login('user',account,password)).token,me=await profile(token)
   created.push({id:me.id,admin:false});return {account,token,id:me.id,me}
  }
  const a=await register('a'),b=await register('b')
  const adminAccount=`identity_admin_${stamp}`
  await success('POST','/admin/add',rootToken,{account:adminAccount,password,username:'权限验收临时管理员',id:1,role:'USER'})
  const adminToken=(await login('admin',adminAccount,password)).token,adminBefore=await get('/admin/auth',adminToken)
  created.push({id:adminBefore.id,admin:true});record('temporary-admin-created-only-through-existing-admin-and-server-owned-identity')
  assert.notEqual((await(await req('POST','/admin/login',undefined,{account:a.account,password})).json()).code,200)
  record('public-registration-cannot-create-admin-through-role-or-id-fields')
  const endpoints=[['POST','/user/add'],['PUT','/user/update'],['DELETE',`/user/${b.id}`],['DELETE',`/user/${b.id}/detail`],['POST','/user/listPage'],['POST','/admin/add'],['PUT','/admin/update'],['DELETE',`/admin/${adminBefore.id}`],['POST','/admin/listPage'],['PUT','/admin/profile'],['PUT','/admin/profile/password'],['GET','/stats/dashboard']]
  for(const [method,url] of endpoints){assert.equal((await req(method,url,undefined,method==='GET'?undefined:{})).status(),401);assert.equal((await req(method,url,a.token,method==='GET'?undefined:{})).status(),403)}
  record('live-anonymous-and-ordinary-user-blocked-from-management-and-admin-personal-endpoints')
  for(const token of [undefined,a.token,adminToken]) assert.equal((await req('POST','/admin/register',token,{})).status(),405)
  record('legacy-admin-registration-no-longer-has-a-post-handler')
  assert.equal((await req('DELETE',`/user/${rootBefore.id}`,rootToken)).status(),404)
  assert.equal((await req('PUT','/user/update',rootToken,{id:rootBefore.id,loginStatus:2})).status(),404)
  assert.equal((await req('DELETE',`/admin/${a.id}`,rootToken)).status(),404)
  assert.deepEqual(await get('/admin/auth',rootToken),rootBefore);record('management-scope-prevents-user-endpoint-from-editing-or-deleting-admin')
  const bBefore=await profile(b.token)
  const updated=await success('PUT','/user/profile',a.token,{id:b.id,username:'只改本人',phone:'',gender:null,school:'',college:'',studentNumber:'',role:'ADMIN',loginStatus:2})
  assert.equal(updated.id,a.id);assert.equal(updated.account,a.account);assert.deepEqual(await profile(b.token),bBefore)
  record('ordinary-profile-forged-target-id-and-role-only-update-authenticated-owner')
  for(const url of ['/user','/user/uploadAvatar',`/user/${b.id}/updatePassword`]) {
   const body={id:b.id,username:'越权不应生效',avatar:'/files/other.png',oldPassword:password,newPassword:nextPassword,confirmPassword:nextPassword}
   assert.equal((await req('PUT',url,a.token,body)).status(),403)
  }
  assert.equal((await req('PUT','/user',a.token,{id:a.id,username:'旧入口不应保存'})).status(),410)
  assert.deepEqual(await profile(b.token),bBefore);record('legacy-target-id-self-writers-denied-or-retired-without-changing-other-user')
  const aBefore=await profile(a.token)
  await success('PUT','/user/update',rootToken,{id:a.id,username:aBefore.username,password:'MustNotReplace!123',account:'admin',avatar:'/files/other.png',role:'ADMIN',createTime:'2000-01-01 00:00:00'})
  assert.deepEqual(await profile(a.token),aBefore);assert.equal((await login('user',a.account,password)).role,'USER')
  record('management-edit-whitelist-does-not-accept-account-password-avatar-role-or-registration-time')
  await success('PUT','/user/update',rootToken,{id:a.id,loginStatus:2})
  assert.equal((await req('GET','/user/profile',a.token)).status(),401)
  await success('PUT','/user/update',rootToken,{id:a.id,loginStatus:1})
  record('disabled-account-existing-session-rejected-by-current-database-state')
  await success('DELETE',`/user/${b.id}`,rootToken);deleted.add(b.id)
  assert.equal((await req('GET','/user/profile',b.token)).status(),401);record('deleted-account-cannot-reuse-existing-token')
  await success('PUT','/admin/profile',adminToken,{id:rootBefore.id,username:'仅当前管理员',phone:'',gender:1,account:'admin',loginStatus:2,role:'USER'})
  assert.equal((await get('/admin/auth',adminToken)).username,'仅当前管理员');assert.deepEqual(await get('/admin/auth',rootToken),rootBefore)
  record('admin-self-profile-ignores-foreign-id-and-management-fields')
  // 管理员页面：选择头像先预览，取消不写库，保存后才统一生效。
  await page.goto(site+'/login');await page.evaluate(token=>{localStorage.setItem('TOKEN',token);localStorage.setItem('ROLE','ADMIN');localStorage.setItem('campus-font-scale','1')},adminToken)
  await page.goto(site+'/admin-home/dashboards');await page.locator('.user-info .username').filter({hasText:'仅当前管理员'}).waitFor()
  const openMenu=async name=>{await page.locator('.user-info').hover();await page.getByRole('menuitem',{name,exact:true}).click()}
  await openMenu('修改信息');let dialog=page.getByRole('dialog',{name:'修改个人信息',exact:true});await dialog.waitFor()
  const image={name:'avatar.png',mimeType:'image/png',buffer:fs.readFileSync('view/public/logo.png')},adminOriginal=await get('/admin/auth',adminToken)
  await dialog.locator('input[type="file"]').setInputFiles(image);assert.ok((await dialog.locator('img.avatar').getAttribute('src')).startsWith('blob:'))
  assert.deepEqual(await get('/admin/auth',adminToken),adminOriginal)
  await dialog.getByRole('button',{name:'取消操作',exact:true}).click();assert.deepEqual(await get('/admin/auth',adminToken),adminOriginal)
  record('admin-avatar-preview-and-cancel-do-not-write-or-delete-server-files')
  await openMenu('修改信息');dialog=page.getByRole('dialog',{name:'修改个人信息',exact:true})
  await dialog.getByPlaceholder('输入用户名').fill('头像保存管理员');await dialog.locator('input[type="file"]').setInputFiles(image)
  const saveResponse=page.waitForResponse(r=>new URL(r.url()).pathname==='/admin/profile'&&r.request().method()==='PUT')
  await dialog.getByRole('button',{name:'我确定修改',exact:true}).click();const saved=await(await saveResponse).json();assert.equal(saved.code,200)
  avatars.push({id:adminBefore.id,url:saved.data.avatar});assert.ok(saved.data.avatar.startsWith(`/files/avatars/${adminBefore.id}/`))
  await dialog.waitFor({state:'hidden'});await page.locator('.user-info .username').filter({hasText:'头像保存管理员'}).waitFor()
  record('admin-avatar-and-info-saved-atomically-to-authenticated-owner-and-navbar-updated')
  await openMenu('修改密码');dialog=page.getByRole('dialog',{name:'修改密码',exact:true})
  for(const width of [1440,768,320])for(const scale of [1,2]){
   await page.setViewportSize({width,height:1000})
   // 使用应用字号开关的真实持久化设置，刷新后重新打开弹窗。
   await dialog.getByRole('button',{name:'取消操作',exact:true}).click();await page.evaluate(scale=>localStorage.setItem('campus-font-scale',String(scale)),scale);await page.reload();await page.locator('.user-info .username').filter({hasText:'头像保存管理员'}).waitFor();await openMenu('修改密码');dialog=page.getByRole('dialog',{name:'修改密码',exact:true})
   assert.equal(await dialog.locator('#admin-old-password').count(),1);assert.equal(await dialog.locator('#admin-confirm-password').count(),1)
   if(scale===2)assert.ok(await dialog.locator('label[for="admin-old-password"]').evaluate(el=>parseFloat(getComputedStyle(el).fontSize)>=24))
   assert.ok(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth+1))
   const bounds=await dialog.boundingBox();assert.ok(bounds.x>=-1&&bounds.x+bounds.width<=width+1)
   if(width===1440)await dialog.screenshot({path:`${out}/admin-password-${scale}.png`})
   results.push({test:'admin-password-modal-fits-standard-and-large-font',width,scale,passed:true})
  }
  await dialog.getByRole('button',{name:'取消操作',exact:true}).click()
  assert.equal((await req('PUT','/admin/profile/password',adminToken,{oldPassword:'wrong',newPassword:nextPassword,confirmPassword:nextPassword})).status(),400)
  assert.equal((await req('PUT',`/admin/${adminBefore.id}/updatePassword`,adminToken,{password:nextPassword})).status(),410)
  record('admin-password-requires-original-password-and-old-no-proof-endpoint-is-retired')
  await page.setViewportSize({width:1440,height:1000});await page.evaluate(()=>localStorage.setItem('campus-font-scale','1'));await page.reload();await page.locator('.user-info .username').filter({hasText:'头像保存管理员'}).waitFor();await openMenu('修改密码');dialog=page.getByRole('dialog',{name:'修改密码',exact:true})
  await dialog.locator('#admin-old-password').fill(password);await dialog.locator('#admin-new-password').fill(nextPassword);await dialog.locator('#admin-confirm-password').fill(nextPassword)
  await dialog.getByRole('button',{name:'我确定修改',exact:true}).click();await page.waitForURL('**/login')
  assert.equal(await page.evaluate(()=>localStorage.getItem('TOKEN')),null);assert.equal((await req('GET','/admin/auth',adminToken)).status(),401)
  const freshAdmin=(await login('admin',adminAccount,nextPassword)).token;assert.equal((await get('/admin/auth',freshAdmin)).id,adminBefore.id)
  record('admin-password-change-through-ui-logs-out-and-revokes-prior-token')
  // 密码变化后，已经建立的聊天连接也不能继续发送消息。
  socket=new WebSocket(`ws://127.0.0.1:21090/ws/chat?token=${encodeURIComponent(a.token)}`)
  await new Promise((resolve,reject)=>{socket.addEventListener('open',resolve,{once:true});socket.addEventListener('error',()=>reject(new Error('Temporary socket handshake failed')),{once:true})})
  const closed=new Promise(resolve=>socket.addEventListener('close',event=>resolve(event.code),{once:true}))
  await success('PUT','/user/profile/password',a.token,{oldPassword:password,newPassword:nextPassword,confirmPassword:nextPassword})
  assert.equal((await req('GET','/user/profile',a.token)).status(),401)
  socket.send(JSON.stringify({type:'HEARTBEAT'}));assert.equal(await Promise.race([closed,new Promise((_,reject)=>setTimeout(()=>reject(new Error('Revoked socket did not close')),5000))]),1008)
  assert.ok((await login('user',a.account,nextPassword)).token);record('ordinary-password-change-revokes-old-http-and-open-websocket-session')
  await page.evaluate(token=>{localStorage.setItem('TOKEN',token);localStorage.setItem('ROLE','USER')},a.token)
  await page.goto(site+'/user-home/profile');await page.waitForURL('**/login')
  assert.equal(await page.evaluate(()=>localStorage.getItem('TOKEN')),null)
  assert.equal(await page.evaluate(()=>localStorage.getItem('ROLE')),null)
  record('expired-session-clears-browser-credentials-and-returns-to-login')
  assert.deepEqual(await get('/admin/auth',rootToken),rootBefore);assert.deepEqual(errors,[])
 } catch(error) { await page.screenshot({path:`${out}/failure.png`}).catch(()=>{});throw error }
 finally {
  socket?.close();let clean=true
  for(const item of created.reverse()){
   if(deleted.has(item.id))continue
   const r=await req('DELETE',`/${item.admin?'admin':'user'}/${item.id}`,rootToken).catch(()=>null)
   clean=clean&&!!r&&r.status()===200&&(await r.json()).code===200
  }
  for(const {id,url}of avatars){
   assert.ok(url.startsWith(`/files/avatars/${id}/`));const filename=url.split('/').at(-1);assert.match(filename,/^[a-f0-9-]{36}\.(png|jpg)$/)
   const workspace=path.resolve('.');assert.equal(workspace.toLowerCase(),'d:\\programmeproject\\campus-system-vue3\\campus-system-dev');const dir=path.resolve('api/file/avatars',String(id));assert.ok(dir.startsWith(workspace+path.sep));const target=path.resolve(dir,filename);assert.ok(target.startsWith(dir+path.sep));if(fs.existsSync(target))fs.unlinkSync(target);if(fs.existsSync(dir)&&!fs.readdirSync(dir).length)fs.rmdirSync(dir)
  }
  fs.writeFileSync(`${out}/results.json`,JSON.stringify({time:new Date().toISOString(),results,errors,temporaryAccountsCleaned:clean},null,2));await browser.close();if(!clean)throw new Error('Identity fixture cleanup failed')
 }
 console.log(`PASS: ${results.length} identity permission checks; temporary accounts and avatar cleaned`)
})().catch(error=>{console.error(error);process.exitCode=1})
