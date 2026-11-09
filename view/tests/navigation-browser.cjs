const { chromium } = require('../../.local-backups/a11y-tools/node_modules/playwright')
const fs = require('node:fs')
const assert = require('node:assert/strict')
const out = '.local-backups/navigation-results'
fs.mkdirSync(out,{recursive:true})
;(async()=>{
  const browser=await chromium.launch({channel:'msedge',headless:true})
  const context=await browser.newContext({reducedMotion:'reduce'})
  const page=await context.newPage()
  const results=[]
  try {
    const login=await(await context.request.post('http://127.0.0.1:21090/user/login',{data:{account:'player2_tester',password:'Player2Test!2026'}})).json()
    assert.equal(login.code,200)
    await page.goto('http://127.0.0.1:21091/login')
    for(const width of [1920,1440,1024,768,320]) {
      await page.setViewportSize({width,height:1000})
      for(const scale of [1,1.25,1.5,2]) {
        for(const role of ['guest','user']) {
          await page.evaluate(({scale,data,role})=>{
            localStorage.setItem('campus-font-scale',String(scale))
            if(role==='user'){localStorage.setItem('TOKEN',data.token);localStorage.setItem('ROLE',data.role)}
            else{localStorage.removeItem('TOKEN');localStorage.removeItem('ROLE')}
          },{scale,data:login.data,role})
          await page.goto(`http://127.0.0.1:21091/${role}-home/market`)
          await page.locator('.navbar .accessibility-toggle').waitFor()
          await page.waitForTimeout(200)
          const geometry=await page.evaluate(()=>{
            const rect=s=>{const r=document.querySelector(s).getBoundingClientRect();return {x:r.x,y:r.y,width:r.width,height:r.height}}
            const blocks=['.navbar .reading-settings','.navbar .brand-link','.navbar .navbar-search','.navbar .navbar-right'].map(rect)
            const links=[...document.querySelectorAll('.nav-link')].map(el=>el.getBoundingClientRect().y)
            return {content:document.documentElement.scrollWidth,blocks,links,header:rect('.navbar')}
          })
          assert.ok(geometry.content<=width,`${width}/${scale}/${role} page overflow`)
          for(let i=0;i<geometry.blocks.length;i++)for(let j=i+1;j<geometry.blocks.length;j++){
            const a=geometry.blocks[i],b=geometry.blocks[j]
            const overlapX=Math.min(a.x+a.width,b.x+b.width)-Math.max(a.x,b.x)
            const overlapY=Math.min(a.y+a.height,b.y+b.height)-Math.max(a.y,b.y)
            assert.ok(overlapX<=1||overlapY<=1,`top navigation overlaps: ${width}/${scale}/${role}/${i},${j}`)
          }
          if(width>=1400){const centers=geometry.blocks.map(r=>r.y+r.height/2);assert.ok(Math.max(...centers)-Math.min(...centers)<3,'desktop header must stay in one row')}
          if(role==='user')assert.ok(Math.max(...geometry.links)-Math.min(...geometry.links)<3,'menu links must stay in one row')
          await page.evaluate(()=>window.scrollTo(0,600))
          await page.waitForTimeout(100)
          const sticky=await page.locator('.navbar').evaluate(el=>({top:el.getBoundingClientRect().top,height:el.getBoundingClientRect().height,position:getComputedStyle(el).position,scroll:scrollY,offset:parseFloat(getComputedStyle(document.documentElement).getPropertyValue('--site-nav-height'))}))
          assert.ok(sticky.scroll>0,'test page must actually scroll')
          assert.equal(sticky.position,'sticky')
          assert.ok(Math.abs(sticky.top)<2,'header must remain pinned')
          assert.ok(sticky.height<=600.5,'header must leave reading space')
          assert.ok(Math.abs(sticky.height-sticky.offset)<1,'focus offset must match actual header')
          results.push({role,width,scale,passed:true,geometry,sticky})
          if(role==='user'&&scale===2&&[1920,1440,320].includes(width))await page.screenshot({path:`${out}/${width}-200-sticky.png`})
        }
      }
    }
  } finally {fs.writeFileSync(`${out}/results.json`,JSON.stringify({time:new Date().toISOString(),results},null,2));await browser.close()}
  console.log(`PASS: ${results.length} navigation layout/sticky combinations`)
})().catch(e=>{console.error(e);process.exitCode=1})
