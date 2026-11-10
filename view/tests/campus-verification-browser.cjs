// 真实后端仅验证未接入状态；已接入的前端交互使用浏览器拦截夹具，不写入真实认证结果。
const {chromium}=require('../../.local-backups/a11y-tools/node_modules/playwright')
const {AxeBuilder}=require('../../.local-backups/a11y-tools/node_modules/@axe-core/playwright')
const fs=require('node:fs'),assert=require('node:assert/strict')
const api='http://127.0.0.1:21090',site='http://127.0.0.1:21091',endpoint='/user/profile/campus-verification',out='.local-backups/campus-verification-results'
fs.mkdirSync(out,{recursive:true})
;(async()=>{
  const browser=await chromium.launch({channel:'msedge',headless:true}),context=await browser.newContext(),page=await context.newPage()
  const results=[],errors=[];let id,adminToken,token,cleanup=false
  const record=test=>results.push({test,passed:true})
  const headers=()=>({Authorization:`Bearer ${token}`})
  const card=()=>page.locator('.verification-card')
  const ready=async()=>{await card().waitFor();await card().locator('.verification-message').waitFor();await page.locator('.verification-card[aria-busy="false"]').waitFor()}
  page.on('pageerror',e=>errors.push(e.message))
  try {
    const seed=fs.readFileSync('sql/db.sql','utf8').match(/INSERT INTO `base_user` VALUES \(1, '([^']+)', '([^']+)'/)
    const admin=await(await context.request.post(api+'/admin/login',{data:{account:seed[1],password:seed[2]}})).json();assert.equal(admin.code,200);adminToken=admin.data.token
    const account='campus_qa_'+Date.now().toString(36),password='CampusTest!2026'
    assert.equal((await(await context.request.post(api+'/user/register',{data:{account,password,username:'认证接口临时测试'}})).json()).code,200)
    const login=await(await context.request.post(api+'/user/login',{data:{account,password}})).json();assert.equal(login.code,200);token=login.data.token
    const profile=await(await context.request.get(api+'/user/profile',{headers:headers()})).json();id=profile.data.id
    assert.equal((await context.request.get(api+endpoint)).status(),401)
    assert.equal((await context.request.get(api+endpoint,{headers:{Authorization:`Bearer ${adminToken}`}})).status(),403)
    record('live-endpoints-require-own-ordinary-user-login')
    const state=await(await context.request.get(api+endpoint,{headers:headers()})).json()
    assert.equal(state.data.available,false);assert.equal(state.data.status,'UNVERIFIED');assert.ok(!('studentNumber' in state.data));record('live-disconnected-provider-does-not-claim-verification')
    assert.equal((await context.request.post(api+endpoint,{headers:headers(),data:{consent:false}})).status(),400)
    assert.equal((await context.request.post(api+endpoint,{headers:headers(),data:{consent:true,id:1,status:'VERIFIED'}})).status(),503)
    assert.equal((await context.request.post(api+endpoint+'/refresh',{headers:headers(),data:{status:'VERIFIED'}})).status(),503)
    assert.equal((await(await context.request.get(api+endpoint,{headers:headers()})).json()).data.status,'UNVERIFIED')
    record('live-forged-status-and-forced-apply-cannot-certify-or-create-pending')
    const saved={username:'认证接口临时测试',phone:'',gender:null,school:'接口验收示例大学',college:'测试学院',studentNumber:'TEST_2026001'}
    assert.equal((await(await context.request.put(api+'/user/profile',{headers:headers(),data:saved})).json()).code,200)
    assert.equal((await(await context.request.get(api+endpoint,{headers:headers()})).json()).data.status,'UNVERIFIED');record('self-filled-campus-data-does-not-become-trusted-certification')
    await page.goto(site+'/login');await page.evaluate(token=>{localStorage.setItem('TOKEN',token);localStorage.setItem('ROLE','USER')},token)
    for(const width of [1440,768,320])for(const scale of [1,1.25,1.5,2]) {
      await page.setViewportSize({width,height:1100});await page.evaluate(scale=>localStorage.setItem('campus-font-scale',String(scale)),scale)
      await page.goto(site+'/user-home/profile');await ready()
      assert.equal(await card().getByRole('button',{name:'认证暂未开放'}).isDisabled(),true)
      assert.ok((await card().textContent()).includes('暂未接入'));assert.ok(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth+1))
      const scan=await new AxeBuilder({page}).include('.verification-card').withTags(['wcag2a','wcag2aa','wcag21aa','wcag22aa']).analyze();assert.deepEqual(scan.violations.map(v=>v.id),[])
      if(width===1440&&[1,2].includes(scale))await card().screenshot({path:`${out}/campus-verification-${scale}.png`})
      results.push({test:'live-default-card-responsive-and-accessible',width,scale,passed:true})
    }
    // 下列仅测试前端，不启用后端模拟认证服务。
    await page.setViewportSize({width:1440,height:1100});await page.evaluate(()=>localStorage.setItem('campus-font-scale','1'))
    let fixture={...state.data,available:true,status:'UNVERIFIED',message:'测试夹具：未认证'},requests=[]
    await page.route('**/user/profile/campus-verification**',async route=>{
      const req=route.request();requests.push({method:req.method(),path:new URL(req.url()).pathname,body:req.postDataJSON()})
      if(req.method()==='POST') fixture={...fixture,status:req.url().endsWith('/refresh')?'VERIFIED':'PENDING',message:'测试夹具：'+(req.url().endsWith('/refresh')?'已通过':'处理中')}
      await route.fulfill({status:200,contentType:'application/json',body:JSON.stringify({code:200,data:fixture})})
    })
    await page.reload();await ready()
    const apply=card().getByRole('button',{name:'申请校园身份认证',exact:true});assert.equal(await apply.isDisabled(),true)
    await card().getByRole('checkbox').check();assert.equal(await apply.isEnabled(),true)
    await page.getByRole('button',{name:'编辑个人资料',exact:true}).click();assert.equal(await apply.isDisabled(),true)
    await page.getByRole('button',{name:'取消修改',exact:true}).click()
    await apply.click();await page.locator('.verification-card[aria-busy="false"]').waitFor();assert.ok((await card().textContent()).includes('认证中'))
    assert.deepEqual(requests.find(r=>r.method==='POST').body,{consent:true})
    await card().getByRole('button',{name:'查询认证结果',exact:true}).click();await page.locator('.verification-card[aria-busy="false"]').waitFor()
    assert.ok((await card().locator('.verification-tag').textContent()).includes('已认证'));record('frontend-fixture-consent-editing-guard-and-apply-refresh-transitions')
    await page.unroute('**/user/profile/campus-verification**');await page.reload();await ready();assert.ok((await card().textContent()).includes('暂未接入'))
    record('removing-frontend-fixtures-restores-real-unverified-state')
    assert.deepEqual(errors,[])
  } catch(error) { await page.screenshot({path:`${out}/failure.png`}).catch(()=>{});throw error }
  finally {
    if(id&&adminToken) { const response=await context.request.delete(api+`/user/${id}`,{headers:{Authorization:`Bearer ${adminToken}`}});cleanup=(await response.json()).code===200 }
    fs.writeFileSync(`${out}/results.json`,JSON.stringify({time:new Date().toISOString(),results,errors,temporaryAccountCleaned:cleanup},null,2));await browser.close()
    if(id&&!cleanup)throw new Error('Temporary campus test account cleanup failed')
  }
  console.log(`PASS: ${results.length} campus verification checks; temporary account cleaned`)
})().catch(error=>{console.error(error);process.exitCode=1})
