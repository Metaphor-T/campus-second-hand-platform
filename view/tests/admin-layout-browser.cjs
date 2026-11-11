const adminCredentials = require('./helpers/test-config.cjs').getAdminCredentials()
const {chromium}=require('../../.local-backups/a11y-tools/node_modules/playwright')
const fs=require('node:fs')
const assert=require('node:assert/strict')
const out='.local-backups/admin-layout-results'
fs.mkdirSync(out,{recursive:true})
;(async()=>{
  const browser=await chromium.launch({channel:'msedge',headless:true})
  const context=await browser.newContext({reducedMotion:'reduce'})
  const page=await context.newPage()
  const results=[],errors=[]
  page.on('pageerror',e=>errors.push(e.message))
  try {
    const login=await(await context.request.post('http://127.0.0.1:21090/admin/login',{data:{account:adminCredentials.account,password:adminCredentials.password}})).json()
    assert.equal(login.code,200)
    await page.goto('http://127.0.0.1:21091/login')
    for(const width of [1440,1024,768,320]) {
      await page.setViewportSize({width,height:1000})
      for(const route of ['/admin-home/dashboards','/admin-home/users','/admin-home/products/list','/admin-home/categories']) {
        await page.evaluate(data=>{
          localStorage.setItem('TOKEN',data.token);localStorage.setItem('ROLE',data.role)
          localStorage.setItem('campus-font-scale','1');localStorage.removeItem('SIDEBAR_COLLAPSED');localStorage.removeItem('SIDEBAR_WIDTH')
        },login.data)
        await page.goto('http://127.0.0.1:21091'+route)
        await page.locator('.admin-layout .content > *').first().waitFor()
        assert.equal(new URL(page.url()).pathname,route)
        for(const scale of [1,1.25,1.5,2]) {
          await page.locator('.accessibility-toggle').click()
          await page.locator(`.font-scale-option:visible[value="${scale}"]`).check()
          await page.getByRole('button',{name:'完成',exact:true}).click()
          await page.locator('.reading-panel:visible').waitFor({state:'hidden'})
          await page.waitForTimeout(120)
          const geometry=await page.evaluate(()=>{
            const rect=s=>{const r=document.querySelector(s).getBoundingClientRect();return {left:r.left,top:r.top,right:r.right,bottom:r.bottom,width:r.width,height:r.height}}
            return {pageWidth:document.documentElement.scrollWidth,sidebar:rect('.sidebar'),main:rect('.admin-layout > .main-content'),header:rect('.admin-layout .header'),content:rect('.admin-layout .content'),children:document.querySelector('.admin-layout .content').childElementCount}
          })
          assert.ok(geometry.pageWidth<=width,'page must not overflow horizontally')
          assert.ok(geometry.main.left>=geometry.sidebar.right-1,'main must remain to right of sidebar')
          assert.ok(geometry.main.top<2 && geometry.main.right<=width+1,'right pane must stay in viewport')
          assert.ok(geometry.content.width>=Math.min(180,width*.5),'content must have usable width')
          assert.ok(geometry.content.height>=500,'content must remain vertically visible')
          assert.ok(geometry.content.bottom<=1001&&geometry.children>0)
          if(route==='/admin-home/dashboards'&&scale>1) {
            const clipped=await page.locator('.stat-value').evaluateAll(items=>items.some(el=>el.scrollWidth>el.clientWidth+1))
            assert.equal(clipped,false,'dashboard summary must not truncate enlarged values')
          }
          const scrolling=await page.locator('.admin-layout .content').evaluate(el=>{el.scrollTop=300;return {needed:el.scrollHeight>el.clientHeight+1,top:el.scrollTop}})
          if(scrolling.needed)assert.ok(scrolling.top>0,'right content must scroll')
          await page.locator('.admin-layout .content').evaluate(el=>{el.scrollTop=0})
          results.push({route,width,scale,geometry,scrolling,passed:true})
          if(width===1440&&[1.25,2].includes(scale)&&route==='/admin-home/dashboards')await page.screenshot({path:`${out}/dashboard-${width}-${scale}.png`})
          if(width===320&&scale===2&&route==='/admin-home/products/list')await page.screenshot({path:`${out}/products-${width}-${scale}.png`})
        }
        const button=page.locator('.admin-sidebar-toggle')
        const before=await button.getAttribute('aria-expanded')
        await button.click()
        assert.notEqual(await button.getAttribute('aria-expanded'),before)
        const rect=await page.locator('.admin-layout > .main-content').boundingBox()
        assert.ok(rect.x>=0&&rect.x+rect.width<=width+1,'toggle must preserve right pane')
        await button.click()
      }
    }
    assert.deepEqual(errors,[])
  } catch(e) {await page.screenshot({path:`${out}/failure.png`}).catch(()=>{});throw e}
  finally {fs.writeFileSync(`${out}/results.json`,JSON.stringify({time:new Date().toISOString(),results,errors},null,2));await browser.close()}
  console.log(`PASS: ${results.length} admin layout combinations and 16 sidebar toggle round trips`)
})().catch(e=>{console.error(e);process.exitCode=1})
