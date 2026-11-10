const { chromium } = require('../../.local-backups/a11y-tools/node_modules/playwright')
const { AxeBuilder } = require('../../.local-backups/a11y-tools/node_modules/@axe-core/playwright')
const fs = require('node:fs')
const assert = require('node:assert/strict')
const out = '.local-backups/product-filters-results'
fs.mkdirSync(out, { recursive: true })
;(async () => {
  const browser = await chromium.launch({ channel: 'msedge', headless: true })
  const context = await browser.newContext({ reducedMotion: 'reduce' })
  const page = await context.newPage()
  const results = []
  try {
    const login = await (await context.request.post('http://127.0.0.1:21090/user/login', { data: { account: 'player2_tester', password: 'Player2Test!2026' } })).json()
    assert.equal(login.code, 200)
    await page.goto('http://127.0.0.1:21091/login')
    for (const width of [1440, 1024, 768, 320]) {
      await page.setViewportSize({ width, height: 1800 })
      for (const scale of [1, 1.25, 1.5, 2]) for (const role of ['guest', 'user']) {
        await page.evaluate(({scale,role,data}) => {
          localStorage.setItem('campus-font-scale', String(scale))
          if (role === 'user') { localStorage.setItem('TOKEN', data.token); localStorage.setItem('ROLE', data.role) }
          else { localStorage.removeItem('TOKEN'); localStorage.removeItem('ROLE') }
        }, {scale,role,data:login.data})
        await page.goto(`http://127.0.0.1:21091/${role}-home/market`)
        const form = page.locator('.product-filters')
        await form.waitFor()
        await page.waitForTimeout(120)
        const layout = await form.evaluate(el => {
          const rect = node => { const r = node.getBoundingClientRect(); return {x:r.x,y:r.y,width:r.width,height:r.height,bottom:r.bottom} }
          return {width:el.clientWidth,scrollWidth:el.scrollWidth,fields:[...el.querySelectorAll('.el-form-item')].map(item=>({label:rect(item.querySelector('.el-form-item__label')),control:rect(item.querySelector('.el-input-number,.el-select,.el-input'))})),controls:[...el.querySelectorAll('.el-input__wrapper,.el-select__wrapper')].map(rect),actions:rect(el.querySelector('.filter-actions'))}
        })
        assert.ok(layout.scrollWidth <= layout.width, 'filter panel must not overflow')
        for (const field of layout.fields) {
          assert.ok(Math.abs(field.label.x - field.control.x) < 2, 'labels and controls must align left')
          assert.ok(field.label.bottom <= field.control.y, 'label must stay above its control')
        }
        assert.ok(Math.max(...layout.controls.map(r=>r.height)) - Math.min(...layout.controls.map(r=>r.height)) < 2, 'control heights must match')
        assert.ok(layout.fields.every(f=>f.control.bottom<=layout.actions.y), 'actions must occupy separate bottom row')
        const selectText = await form.locator('.el-select__placeholder').evaluateAll(items=>items.map(el=>{
          const style=getComputedStyle(el), rect=el.getBoundingClientRect()
          const range=document.createRange();range.selectNodeContents(el);const text=range.getBoundingClientRect()
          return {font:parseFloat(style.fontSize),line:parseFloat(style.lineHeight),height:rect.height,textHeight:text.height,textTop:text.top,top:rect.top,textBottom:text.bottom,bottom:rect.bottom}
        }))
        for(const text of selectText) {
          assert.ok(text.line>=text.font*1.49,'selected text line height must scale with font')
          assert.ok(text.textTop>=text.top-1&&text.textBottom<=text.bottom+1,'selected text must fit its clipping box')
        }
        if(role==='guest') {
          await form.locator('.el-select').first().click()
          const option=page.getByRole('option',{name:'全新',exact:true})
          await option.waitFor()
          const dimensions=await option.evaluate(el=>({height:el.getBoundingClientRect().height,font:parseFloat(getComputedStyle(el).fontSize),line:parseFloat(getComputedStyle(el).lineHeight)}))
          assert.ok(dimensions.line>=dimensions.font*1.49&&dimensions.height>=dimensions.line,'dropdown option must fit enlarged text')
          await page.keyboard.press('Escape')
          await option.waitFor({state:'hidden'})
        }
        const scan = await new AxeBuilder({page}).include('.product-filters').withTags(['wcag2a','wcag2aa','wcag21aa','wcag22aa']).analyze()
        assert.deepEqual(scan.violations.map(v=>({id:v.id,targets:v.nodes.map(n=>n.target)})), [])
        results.push({role,width,scale,passed:true,layout,selectText})
        if(role==='guest'&&((width===1440&&[1,2].includes(scale))||(width===320&&scale===2))) await form.screenshot({path:`${out}/filters-${width}-${scale}.png`})
      }
    }
    await page.setViewportSize({width:1440,height:1800})
    await page.evaluate(()=>{localStorage.removeItem('TOKEN');localStorage.removeItem('ROLE');localStorage.setItem('campus-font-scale','1')})
    await page.goto('http://127.0.0.1:21091/guest-home/market')
    const form = page.locator('.product-filters')
    await form.getByPlaceholder('搜索商品标题').fill('  验收  ')
    await form.getByRole('spinbutton',{name:'最低价格'}).fill('10')
    await form.getByRole('spinbutton',{name:'最高价格'}).fill('20')
    let response = page.waitForResponse(r=>r.url().endsWith('/guest/product/market')&&r.request().method()==='POST')
    await form.getByRole('button',{name:'应用筛选',exact:true}).click()
    const applied = await response, body = applied.request().postDataJSON()
    assert.equal(body.title,'验收');assert.equal(body.minPrice,10);assert.equal(body.maxPrice,20)
    assert.equal((await applied.json()).code,200)
    await form.getByRole('spinbutton',{name:'最低价格'}).fill('50')
    await form.getByRole('button',{name:'应用筛选',exact:true}).click()
    assert.match(await form.getByRole('alert').innerText(),/最低价格不能高于最高价格/)
    response = page.waitForResponse(r=>r.url().endsWith('/guest/product/market')&&r.request().method()==='POST')
    await form.getByRole('button',{name:'重置筛选',exact:true}).click()
    const reset = (await response).request().postDataJSON()
    assert.ok(!reset.title&&reset.minPrice==null&&reset.maxPrice==null);assert.equal(reset.sortBy,'latest')
    assert.equal(await form.getByPlaceholder('搜索商品标题').inputValue(),'')
    assert.equal(await form.getByRole('alert').count(),0)
    results.push({test:'apply-range-validation-reset',passed:true})
  } catch(e) {await page.screenshot({path:`${out}/failure.png`}).catch(()=>{});throw e}
  finally {fs.writeFileSync(`${out}/results.json`,JSON.stringify({time:new Date().toISOString(),results},null,2));await browser.close()}
  console.log(`PASS: ${results.length} filter layout and interaction checks`)
})().catch(e=>{console.error(e);process.exitCode=1})
