const { chromium } = require('../../.local-backups/a11y-tools/node_modules/playwright')
const fs = require('node:fs')
const assert = require('node:assert/strict')
const out = '.local-backups/reading-layout-results'
fs.mkdirSync(out, {recursive:true})
;(async () => {
  const browser = await chromium.launch({channel:'msedge',headless:true})
  const context = await browser.newContext({reducedMotion:'reduce'})
  const page = await context.newPage()
  const results = []
  try {
    await page.goto('http://127.0.0.1:21091/login')
    for (const width of [1440,1024,768,320]) {
      await page.setViewportSize({width,height:1000})
      for (const scale of [1,1.25,1.5,2]) {
        await page.evaluate(v=>localStorage.setItem('campus-font-scale',String(v)),scale)
        for (const route of ['/login','/register']) {
          await page.goto('http://127.0.0.1:21091'+route)
          await page.locator('.auth-reading .accessibility-toggle').waitFor()
          await page.waitForTimeout(120)
          const geometry = await page.evaluate(()=> {
            const hero=document.querySelector('.hero-section'), form=document.querySelector('.form-section')
            const brand=document.querySelector('.hero-content .brand'), button=document.querySelector('.auth-reading')
            const h=hero.getBoundingClientRect(),f=form.getBoundingClientRect()
            return { width:innerWidth,content:document.documentElement.scrollWidth,heroWidth:h.width,heroHeight:h.height,
              background:getComputedStyle(hero).backgroundImage,stacked:f.top>=h.bottom-2,
              entryBottom:button.getBoundingClientRect().bottom,brandTop:brand.getBoundingClientRect().top }
          })
          assert.ok(geometry.heroWidth>0 && geometry.heroHeight>=220)
          assert.ok(geometry.background.includes('bag.png'))
          assert.ok(geometry.content<=width)
          assert.ok(geometry.entryBottom<=geometry.brandTop, 'entry must not cover brand')
          if(width<=960 || (width<=1200&&scale>1)) assert.ok(geometry.stacked)
          results.push({route,width,scale,...geometry,passed:true})
          if((width===1440||width===320)&&scale===2) await page.screenshot({path:`${out}/${width}-${route.slice(1)}.png`,fullPage:true})
        }
      }
    }
    // 使用仓库初始化数据中的测试管理员，不写入测试数据库。
    const seed=fs.readFileSync('sql/db.sql','utf8').match(/INSERT INTO `base_user` VALUES \(1, '([^']+)', '([^']+)'/)
    assert.ok(seed)
    const login=await (await context.request.post('http://127.0.0.1:21090/admin/login',{data:{account:seed[1],password:seed[2]}})).json()
    assert.equal(login.code,200)
    for(const width of [1440,320]) {
      await page.setViewportSize({width,height:1000})
      for(const scale of [1,2]) {
        await page.evaluate(({data,scale})=>{localStorage.setItem('TOKEN',data.token);localStorage.setItem('ROLE',data.role);localStorage.setItem('campus-font-scale',String(scale))},{data:login.data,scale})
        await page.goto('http://127.0.0.1:21091/admin-home/dashboards')
        const trigger=page.locator('.header-right .accessibility-toggle')
        await trigger.waitFor()
        assert.equal(await page.locator('.accessibility-toggle').count(),1)
        await trigger.click()
        const panel=page.locator('.reading-panel:visible')
        await panel.waitFor()
        const bounds=await panel.boundingBox()
        assert.ok(bounds.x>=0&&bounds.x+bounds.width<=width)
        await page.keyboard.press('Escape')
        await panel.waitFor({state:'hidden'})
        assert.ok(await trigger.evaluate(el=>el===document.activeElement))
        results.push({test:'admin-toolbar-panel',width,scale,passed:true})
        if(width===1440&&scale===1) await page.screenshot({path:`${out}/admin-toolbar.png`,fullPage:true})
      }
    }
  } finally {
    fs.writeFileSync(`${out}/results.json`,JSON.stringify({time:new Date().toISOString(),results},null,2))
    await browser.close()
  }
  console.log(`PASS: ${results.length} reading layout checks`)
})().catch(e=>{console.error(e);process.exitCode=1})
