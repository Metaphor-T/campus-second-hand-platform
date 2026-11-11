const adminCredentials = require('./helpers/test-config.cjs').getAdminCredentials()
const {chromium}=require('../../.local-backups/a11y-tools/node_modules/playwright')
const {AxeBuilder}=require('../../.local-backups/a11y-tools/node_modules/@axe-core/playwright')
const fs=require('node:fs'),path=require('node:path'),assert=require('node:assert/strict')
const out='.local-backups/user-profile-results',api='http://127.0.0.1:21090',site='http://127.0.0.1:21091'
fs.mkdirSync(out,{recursive:true})
;(async()=>{
  const browser=await chromium.launch({channel:'msedge',headless:true})
  const context=await browser.newContext({reducedMotion:'reduce'}),page=await context.newPage()
  const results=[],errors=[],createdAvatars=[]
  let userId,adminToken,userToken
  const headers=()=>({Authorization:`Bearer ${userToken}`})
  const mine=async()=>{const r=await context.request.get(api+'/user/profile',{headers:headers()});assert.equal(r.status(),200);const b=await r.json();assert.equal(b.code,200);return b.data}
  const record=test=>results.push({test,passed:true})
  const savedResponse=()=>page.waitForResponse(r=>new URL(r.url()).pathname==='/user/profile'&&r.request().method()==='PUT')
  const ready=()=>page.locator('.personal-center[aria-busy="false"] .profile-layout').waitFor()
  const edit=()=>page.getByRole('button',{name:'编辑个人资料',exact:true}).click()
  const fill=async values=>{for(const[key,value]of Object.entries(values))await page.locator('#profile-'+key).fill(value)}
  const demo={username:'个人中心验收同学',phone:'13912345678',gender:2,school:'校园资料演示学校',college:'计算机学院',studentNumber:'DEMO_2026001'}
  page.on('pageerror',e=>errors.push(e.message))
  try{
    const admin=await(await context.request.post(api+'/admin/login',{data:{account:adminCredentials.account,password:adminCredentials.password}})).json();assert.equal(admin.code,200);adminToken=admin.data.token
    const account='profile_qa_'+Date.now().toString(36),oldPassword='ProfileTest!2026',newPassword='ProfileNew!2026'
    const registration=await(await context.request.post(api+'/user/register',{data:{account,password:oldPassword,username:'个人中心临时账号'}})).json();assert.equal(registration.code,200)
    const login=await(await context.request.post(api+'/user/login',{data:{account,password:oldPassword}})).json();assert.equal(login.code,200);userToken=login.data.token
    const original=await mine();userId=original.id
    assert.equal(original.studentNumber,null);assert.equal(original.school,null);assert.ok(original.createTime);assert.equal(original.role,'USER');assert.ok(!('password'in original));record('old-style-account-without-campus-record-can-read-profile')
    await page.goto(site+'/login')
    await page.evaluate(token=>{localStorage.setItem('TOKEN',token);localStorage.setItem('ROLE','USER');localStorage.setItem('campus-font-scale','1')},userToken)
    await page.goto(site+'/user-home/market');await page.locator('.user-trigger').click()
    await page.getByRole('menuitem',{name:'个人中心',exact:true}).click();await page.waitForURL('**/user-home/profile');await ready();record('avatar-menu-opens-independent-personal-center')
    await edit();assert.equal(await page.locator('#profile-account').isEditable(),false)
    await fill({nickname:'取消不应保存',school:'取消学校',college:'取消学院','student-number':'CANCEL_001'})
    const image={name:'avatar.png',mimeType:'image/png',buffer:fs.readFileSync('view/public/logo.png')}
    await page.locator('.personal-center input[type="file"]').setInputFiles(image)
    assert.ok((await page.locator('.profile-avatar').getAttribute('src')).startsWith('blob:'))
    assert.deepEqual(await mine(),original,'choosing avatar and typing fields must not change server')
    await page.getByRole('button',{name:'取消修改',exact:true}).click();assert.deepEqual(await mine(),original);assert.equal(await page.locator('.personal-center img[src^="blob:"]').count(),0);record('cancel-restores-original-fields-and-avatar-without-upload')
    await edit();await fill({nickname:demo.username,phone:demo.phone,school:demo.school,college:demo.college,'student-number':demo.studentNumber});await page.locator('#profile-gender').selectOption('2')
    await page.locator('.personal-center input[type="file"]').setInputFiles(image)
    let response=savedResponse();await page.getByRole('button',{name:'保存资料',exact:true}).click();const saved=await(await response).json();assert.equal(saved.code,200)
    createdAvatars.push(saved.data.avatar)
    await page.locator('.profile-edit-form').waitFor({state:'hidden'})
    assert.equal(await page.locator('.user-trigger .username').textContent(),demo.username)
    assert.ok(saved.data.avatar.startsWith(`/files/avatars/${userId}/`));assert.ok((await page.locator('.user-trigger img').getAttribute('src')).includes(saved.data.avatar))
    const persisted=await mine();for(const key of ['username','phone','school','college','studentNumber','gender'])assert.equal(persisted[key],demo[key])
    await page.reload();await ready();assert.equal(await page.locator('[data-profile="student-number"]').textContent(),demo.studentNumber);record('atomic-avatar-profile-save-updates-navbar-and-persists-on-refresh')
    const adminProfile=await context.request.get(api+`/user/profile/admin/${userId}`,{headers:{Authorization:`Bearer ${adminToken}`}});assert.equal(adminProfile.status(),200);assert.equal((await adminProfile.json()).data.studentNumber,demo.studentNumber)
    assert.equal((await context.request.get(api+`/user/profile/admin/${userId}`,{headers:headers()})).status(),403)
    assert.equal((await context.request.get(api+'/user/profile')).status(),401)
    assert.equal((await context.request.post(api+'/user/listPage',{headers:headers(),data:{current:1,size:10}})).status(),403)
    const forged=await(await context.request.put(api+'/user/profile',{headers:headers(),data:{...demo,id:1,account:'should-not-change',role:'ADMIN',loginStatus:2}})).json();assert.equal(forged.code,200);assert.equal(forged.data.id,userId);assert.equal(forged.data.account,account);assert.equal(forged.data.role,'USER');record('campus-data-private-and-client-cannot-change-target-account-or-role')
    const cleared=await(await context.request.put(api+'/user/profile',{headers:headers(),data:{...demo,phone:'',school:'',college:'',studentNumber:''}})).json();assert.equal(cleared.code,200);for(const key of ['phone','school','college','studentNumber'])assert.equal(cleared.data[key],null)
    assert.equal((await(await context.request.put(api+'/user/profile',{headers:headers(),data:demo})).json()).code,200);record('optional-contact-and-campus-fields-can-be-cleared')

    for(const width of [1440,1024,768,320])for(const scale of [1,1.25,1.5,2]){
      await page.setViewportSize({width,height:1200});await page.evaluate(scale=>localStorage.setItem('campus-font-scale',String(scale)),scale)
      await page.goto(site+'/user-home/profile');await ready()
      assert.ok(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth+1),'read profile must not overflow')
      assert.equal(await page.locator('[data-profile="school"]').textContent(),demo.school)
      if(width===1440&&[1,2].includes(scale))await page.screenshot({path:`${out}/profile-${scale}.png`})
      await edit()
      assert.ok(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth+1),'edit profile must not overflow')
      assert.equal(await page.locator('#profile-account').isEditable(),false)
      const scan=await new AxeBuilder({page}).include('.personal-center').withTags(['wcag2a','wcag2aa','wcag21aa','wcag22aa']).analyze()
      assert.deepEqual(scan.violations.map(v=>({id:v.id,targets:v.nodes.map(n=>n.target)})),[])
      if(width===1440&&[1,2].includes(scale))await page.screenshot({path:`${out}/profile-edit-${scale}.png`})
      await page.getByRole('button',{name:'取消修改',exact:true}).click()
      results.push({test:'responsive-read-edit-and-accessibility',width,scale,passed:true})
    }
    await page.setViewportSize({width:1440,height:1200});await page.evaluate(()=>localStorage.setItem('campus-font-scale','1'));await page.goto(site+'/user-home/profile');await ready();await edit()
    await page.locator('#profile-phone').fill('123');await page.getByRole('button',{name:'保存资料',exact:true}).click();assert.ok((await page.locator('.profile-edit-form [role="alert"]').textContent()).includes('手机号'))
    await page.locator('#profile-phone').fill(demo.phone);await page.locator('#profile-student-number').fill('<wrong>');await page.getByRole('button',{name:'保存资料',exact:true}).click();assert.ok((await page.locator('.profile-edit-form [role="alert"]').textContent()).includes('学号'))
    assert.equal((await mine()).studentNumber,demo.studentNumber);record('invalid-phone-and-student-number-do-not-save')
    await page.getByRole('button',{name:'取消修改',exact:true}).click()
    await page.route('**/user/profile',route=>route.request().method()==='PUT'?route.fulfill({status:503,contentType:'application/json',body:JSON.stringify({code:503,message:'测试保存失败'})}):route.continue(),{times:1})
    await edit();await page.locator('#profile-nickname').fill('失败时保留草稿');response=savedResponse();await page.getByRole('button',{name:'保存资料',exact:true}).click();await response
    await page.locator('.profile-edit-form [role="alert"]').waitFor();assert.equal(await page.locator('#profile-nickname').inputValue(),'失败时保留草稿');assert.equal((await mine()).username,demo.username)
    await page.getByRole('button',{name:'取消修改',exact:true}).click();record('save-failure-keeps-draft-and-original-server-data')
    await page.locator('.user-trigger').click();assert.equal(await page.getByRole('menuitem',{name:'修改密码',exact:true}).count(),0)
    await page.keyboard.press('Escape');await page.locator('.profile-summary').getByRole('button',{name:'修改密码',exact:true}).click();await page.locator('.password-form').waitFor()
    await page.locator('#profile-old-password').fill('wrong');await page.locator('#profile-new-password').fill(newPassword);await page.locator('#profile-confirm-password').fill('not-matching')
    await page.getByRole('button',{name:'确认修改密码',exact:true}).click();assert.ok((await page.locator('.password-form [role="alert"]').textContent()).includes('不一致'))
    await page.locator('#profile-confirm-password').fill(newPassword);let passwordResponse=page.waitForResponse(r=>new URL(r.url()).pathname==='/user/profile/password');await page.getByRole('button',{name:'确认修改密码',exact:true}).click();assert.equal((await passwordResponse).status(),400);assert.ok((await page.locator('.password-form [role="alert"]').textContent()).includes('原密码不正确'))
    await page.getByRole('button',{name:'取消',exact:true}).click();assert.equal((await(await context.request.post(api+'/user/login',{data:{account,password:oldPassword}})).json()).code,200);record('password-validation-and-cancel-preserve-existing-password')
    await page.getByRole('button',{name:'修改登录密码',exact:true}).click();await page.locator('#profile-old-password').fill(oldPassword);await page.locator('#profile-new-password').fill(newPassword);await page.locator('#profile-confirm-password').fill(newPassword)
    await page.getByRole('button',{name:'确认修改密码',exact:true}).click();await page.waitForURL('**/login')
    assert.equal(await page.evaluate(()=>localStorage.getItem('TOKEN')),null)
    assert.notEqual((await(await context.request.post(api+'/user/login',{data:{account,password:oldPassword}})).json()).code,200)
    assert.equal((await(await context.request.post(api+'/user/login',{data:{account,password:newPassword}})).json()).code,200);record('password-change-logs-out-current-browser-and-new-password-works')
    assert.deepEqual(errors,[])
  }catch(error){await page.screenshot({path:`${out}/failure.png`}).catch(()=>{});throw error}
  finally{
    let cleanup=false
    if(userId&&adminToken){
      const deleted=await context.request.delete(api+`/user/${userId}`,{headers:{Authorization:`Bearer ${adminToken}`}}).catch(()=>null)
      cleanup=!!deleted&&(await deleted.json()).code===200
      if(cleanup){
        const workspace=path.resolve('.');assert.equal(workspace.toLowerCase(),'d:\\programmeproject\\campus-system-vue3\\campus-system-dev')
        const ownerDirectory=path.resolve('api/file/avatars',String(userId))
        assert.ok(ownerDirectory.startsWith(workspace+path.sep))
        for(const url of createdAvatars){
          assert.ok(url.startsWith(`/files/avatars/${userId}/`));const filename=url.split('/').at(-1);assert.match(filename,/^[a-f0-9-]{36}\.(png|jpg)$/)
          const target=path.resolve(ownerDirectory,filename);assert.ok(target.startsWith(ownerDirectory+path.sep));if(fs.existsSync(target))fs.unlinkSync(target)
        }
        if(fs.existsSync(ownerDirectory)&&fs.readdirSync(ownerDirectory).length===0)fs.rmdirSync(ownerDirectory)
      }
    }
    fs.writeFileSync(`${out}/results.json`,JSON.stringify({time:new Date().toISOString(),results,errors,temporaryAccountCleaned:cleanup},null,2))
    await browser.close()
    if(userId&&!cleanup)throw new Error('Temporary profile test account cleanup failed')
  }
  console.log(`PASS: ${results.length} personal-center checks; temporary account and generated avatar cleaned`)
})().catch(error=>{console.error(error);process.exitCode=1})
