// 使用隔离的前端数据夹具验证点击/路由；不创建用户、不写入商品浏览记录。
const {chromium}=require('../../.local-backups/a11y-tools/node_modules/playwright')
const fs=require('node:fs'),assert=require('node:assert/strict')
const out='.local-backups/market-card-click-results',site='http://127.0.0.1:21091'
fs.mkdirSync(out,{recursive:true})
const product=(id,index)=>({id,userId:2,username:'演示卖家',categoryId:1,categoryName:'数码',title:`卡片交互验收商品${index}`,description:'隔离测试商品',price:100+index,status:1,condition:1,tradeType:1,stock:5,viewCount:12,favoriteCount:3,createTime:'2026-11-10 12:00:00',images:[{imageUrl:'/files/card-interaction-fixture.png',isCover:1}]})
const hot=Array.from({length:5},(_,i)=>product(901+i,i+1)),recommend=Array.from({length:5},(_,i)=>product(951+i,i+6))
;(async()=>{
 const browser=await chromium.launch({channel:'msedge',headless:true}),context=await browser.newContext({reducedMotion:'reduce'}),page=await context.newPage()
 const results=[],errors=[],unmocked=[],image=fs.readFileSync('view/public/logo.png')
 page.on('pageerror',e=>errors.push(e.message))
 await context.route('**/*',async route=>{
  const url=new URL(route.request().url())
  if(url.port!=='21090')return route.continue()
  if(url.pathname.startsWith('/files/'))return route.fulfill({status:200,contentType:'image/png',body:image})
  let data=[],total=0
  const p=url.pathname
  if(p==='/user/auth')data={id:2,account:'frontend_fixture',username:'卡片验收',loginStatus:1}
  else if(p==='/product/public/hot'||p==='/guest/product/hot')data=hot
  else if(p==='/recommend/public/list')data=recommend
  else if(p==='/category/listAll'||p==='/guest/category/list')data=[{id:1,name:'数码',status:1}]
  else if(p==='/guest/product/market')data={productList:[],total:0}
  else if(/\/(?:product\/public|guest\/product)\/detail\/\d+$/.test(p))data=[...hot,...recommend].find(item=>item.id===Number(p.split('/').at(-1)))
  else if(p.includes('/interact/status'))data={favorited:false,wanted:false}
  else if(p.includes('/stats'))data={avgRating:0,reviewCount:0}
  else if(p.includes('unread'))data=0
  else if(!p.startsWith('/product/')&&!p.startsWith('/review/')&&!p.startsWith('/guest/')&&!p.startsWith('/chat/')&&!p.startsWith('/category/'))unmocked.push(p)
  return route.fulfill({status:200,contentType:'application/json',body:JSON.stringify({code:200,data,total})})
 })
 if(typeof context.routeWebSocket==='function')await context.routeWebSocket(/\/ws\/chat/,socket=>socket.close())
 async function openMarket(role,width,scale){
  await page.setViewportSize({width,height:1400});await page.goto(site+'/login')
  await page.evaluate(({role,scale})=>{
   localStorage.setItem('campus-font-scale',String(scale))
   if(role==='user'){localStorage.setItem('TOKEN','frontend_fixture_only');localStorage.setItem('ROLE','USER')}
   else{localStorage.removeItem('TOKEN');localStorage.removeItem('ROLE')}
  },{role,scale})
  await page.goto(site+`/${role}-home/market`)
  await page.locator('.hot-section .hot-card-link').first().waitFor()
  await page.waitForFunction(()=>[...document.querySelectorAll('.hot-section .hot-thumb img')].every(img=>img.complete))
 }
 async function resetMarket(role){
  await page.evaluate(role=>document.querySelector('#app').__vue_app__.config.globalProperties.$router.push(`/${role}-home/market`),role)
  await page.locator('.hot-section .hot-card-link').first().waitFor()
 }
 async function armNavigationCounter(){
  await page.evaluate(()=>{
   const router=document.querySelector('#app').__vue_app__.config.globalProperties.$router
   if(!router.__originalTestPush){router.__originalTestPush=router.push.bind(router);router.push=(...args)=>{window.__cardPushCount=(window.__cardPushCount||0)+1;return router.__originalTestPush(...args)}}
   window.__cardPushCount=0
  })
 }
 async function clickRegion(card,region){
  const target=region==='image'?card.locator('.el-carousel__container'):region==='title'?card.locator('.hot-overlay-title'):region==='price'?card.locator('.hot-overlay-price'):card.locator('.hot-card-link')
  await target.evaluate(el=>{
   const rect=el.getBoundingClientRect(),header=parseFloat(getComputedStyle(document.documentElement).getPropertyValue('--site-nav-height'))||0
   window.scrollTo(0,Math.max(0,scrollY+rect.top-header-30))
  })
  const box=await target.boundingBox();assert.ok(box&&box.height>0,`${region} must be visible`)
  const point={x:box.x+(region==='corner'?box.width-24:Math.min(30,box.width/2)),y:box.y+(region==='corner'?30:Math.min(box.height/2,70))}
  assert.ok(await page.evaluate(({x,y})=>!!document.elementFromPoint(x,y)?.closest('.hot-card-link'),point),`${region} must hit the detail link`)
  await page.mouse.click(point.x,point.y)
 }
 try {
  for(const width of [1440,768,320])for(const scale of [1,2])for(const role of ['user','guest']){
   await openMarket(role,width,scale)
   const groups=role==='user'?['热门','推荐']:['热门']
   assert.ok(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth+1),'market page must not overflow')
   for(let index=0;index<groups.length;index++){
    for(const region of ['image','title','price','corner']){
     const card=page.locator('.hot-section .hot-main').nth(index),id=index===0?901:951
     await armNavigationCounter();await clickRegion(card,region)
     await page.waitForURL(`**/${role}-home/product-detail/${id}`)
     assert.equal(await page.evaluate(()=>window.__cardPushCount),1,`${region} must trigger one navigation`)
     await resetMarket(role)
    }
    let card=page.locator('.hot-section .hot-main').nth(index),buttons=card.locator('.hot-thumb')
    assert.equal(await buttons.evaluateAll(items=>items.every(el=>el.tagName==='BUTTON')),true)
    assert.equal(await card.locator('a button, button a, a a, button button').count(),0,'interactive controls must not be nested')
    await armNavigationCounter();await buttons.nth(1).click()
    assert.equal(await buttons.nth(1).getAttribute('aria-pressed'),'true');assert.ok(page.url().endsWith('/market'));assert.equal(await page.evaluate(()=>window.__cardPushCount),0)
    const target=index===0?902:952
    assert.equal(await card.locator('.hot-card-link').getAttribute('href'),`/${role}-home/product-detail/${target}`)
    await buttons.nth(2).focus();await page.keyboard.press('Enter');assert.equal(await buttons.nth(2).getAttribute('aria-pressed'),'true')
    await buttons.nth(3).focus();await page.keyboard.press('Space');assert.equal(await buttons.nth(3).getAttribute('aria-pressed'),'true');assert.ok(page.url().endsWith('/market'));assert.equal(await page.evaluate(()=>window.__cardPushCount),0)
    await card.locator('.hot-card-link').focus();await page.keyboard.press('Tab');assert.equal(await buttons.first().evaluate(el=>el===document.activeElement),true)
    await card.locator('.hot-card-link').focus();assert.equal(await card.locator('.hot-card-link').evaluate(el=>getComputedStyle(el).outlineStyle),'solid')
    await page.keyboard.press('Enter');await page.waitForURL(`**/${role}-home/product-detail/${index===0?904:954}`);assert.equal(await page.evaluate(()=>window.__cardPushCount),1)
    await resetMarket(role)
    results.push({test:'full-card-single-navigation-thumbnail-isolation-and-keyboard',role,card:groups[index],width,scale,passed:true})
   }
   if(width===1440)await page.locator('.hot-section').screenshot({path:`${out}/${role}-cards-${scale}.png`})
  }
  assert.deepEqual(errors,[]);assert.deepEqual(unmocked,[])
 } catch(error){await page.screenshot({path:`${out}/failure.png`}).catch(()=>{});throw error}
 finally {
  fs.writeFileSync(`${out}/results.json`,JSON.stringify({time:new Date().toISOString(),fixtureMode:'frontend-only-network-interception',databaseWrites:false,results,errors,unmocked},null,2))
  await browser.close()
 }
 console.log(`PASS: ${results.length} card interaction scenarios; no database writes`)
})().catch(error=>{console.error(error);process.exitCode=1})
