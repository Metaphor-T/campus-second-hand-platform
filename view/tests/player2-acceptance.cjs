const adminCredentials = require('./helpers/test-config.cjs').getAdminCredentials()
const { chromium, request } = require('../../.local-backups/a11y-tools/node_modules/playwright')
const fs = require('node:fs')
const assert = require('node:assert/strict')
const out='.local-backups/player2-acceptance'
fs.mkdirSync(out,{recursive:true})
const run=Date.now().toString(36), results=[], fixtures={run,accounts:[],products:[],orders:[]}
const password='CampusAcceptance!2026'
let api,browser,page
async function call(actor,method,path,data,extra={}) {
  const response=await api.fetch(path,{method,headers:actor?{Authorization:`Bearer ${actor.token}`}:{},data,...extra})
  let body;try{body=await response.json()}catch{body={code:response.status(),message:'non-JSON response'}}
  return {http:response.status(),...body}
}
function ok(r){assert.equal(r.code,200,`HTTP ${r.http}, code ${r.code}: ${r.message}`);return r.data}
function denied(r){assert.ok(r.code>=400&&r.http<500,`expected business rejection, got HTTP ${r.http}, code ${r.code}`)}
async function check(name,fn){try{await fn();results.push({name,passed:true});console.log('PASS',name)}catch(e){results.push({name,passed:false,error:e.message});console.log('FAIL',name,e.message);if(page)await page.screenshot({path:`${out}/failure-${results.length}.png`,fullPage:true}).catch(()=>{})}}
async function register(suffix){const account=`acc_${run}_${suffix}`;ok(await call(null,'POST','/user/register',{account,password,username:`验收${suffix}`}));const actor=ok(await call(null,'POST','/user/login',{account,password}));fixtures.accounts.push(account);return {...actor,account}}
async function detail(actor,id){return ok(await call(actor,'GET',`/product/public/detail/${id}`))}
async function publish(actor,stock=5,extra={}){
  const title=`验收_${run}_${fixtures.products.length}`
  ok(await call(actor,'POST','/product/add',{...base,title,stock,...extra}))
  const items=ok(await call(actor,'POST','/product/myProducts',{current:1,size:50,title}))
  const p=items.find(x=>x.title===title);assert.ok(p,'published item must be queryable');fixtures.products.push(p.id);return p.id
}
async function approve(id){ok(await call(admin,'PUT','/product/audit',{id,status:1}))}
async function order(actor,id,quantity=1){const o=ok(await call(actor,'POST','/order/create',{productId:id,quantity}));fixtures.orders.push(o.id);return o}
let seller,buyer,outsider,admin,base,image,categoryId,mainId
;(async()=>{
  api=await request.newContext({baseURL:'http://127.0.0.1:21090',timeout:15000})
  try {
    admin=ok(await call(null,'POST','/admin/login',{account:adminCredentials.account,password:adminCredentials.password}))
    seller=await register('seller');buyer=await register('buyer');outsider=await register('other')
    const categories=ok(await call(null,'GET','/guest/category/list'));categoryId=categories[0].id
    const filename=fs.readdirSync('api/file').filter(x=>x.endsWith('.png')).sort((a,b)=>fs.statSync('api/file/'+a).size-fs.statSync('api/file/'+b).size)[0]
    const buffer=fs.readFileSync('api/file/'+filename)
    image=ok(await call(seller,'POST','/product/images/upload',undefined,{multipart:{file:{name:'acceptance.png',mimeType:'image/png',buffer}}})).filePath
    base={categoryId,title:'验收商品',description:'独立测试库专项验收数据',price:12.34,originalPrice:20,condition:2,tradeType:1,stock:5,images:[{imageUrl:image,isCover:1,sort:0}]}
    await check('UPLOAD-01 authenticated real PNG upload',async()=>assert.ok(image.startsWith('/files/products/')))
    await check('UPLOAD-02 rejects anonymous upload',async()=>denied(await call(null,'POST','/product/images/upload',undefined,{multipart:{file:{name:'a.png',mimeType:'image/png',buffer}}})))
    await check('UPLOAD-03 rejects renamed text as PNG',async()=>denied(await call(seller,'POST','/product/images/upload',undefined,{multipart:{file:{name:'fake.png',mimeType:'image/png',buffer:Buffer.from('not a real image')}}})))
    await check('UPLOAD-04 rejects >2MB image',async()=>denied(await call(seller,'POST','/product/images/upload',undefined,{multipart:{file:{name:'big.png',mimeType:'image/png',buffer:Buffer.alloc(2*1024*1024+1)}}})))
    for(const [name,patch] of [['zero-price',{price:0}],['fractional-price',{price:0.001}],['blank-title',{title:'  '}],['invalid-category',{categoryId:2147483647}],['no-images',{images:[]}],['duplicate-images',{images:[...base.images,...base.images]}],['external-image',{images:[{imageUrl:'https://example.com/a.png'}]}]])
      await check(`VALIDATION ${name}`,async()=>denied(await call(seller,'POST','/product/add',{...base,...patch})))
    await check('IMAGE-OWNER cannot reuse another user product image',async()=>denied(await call(buyer,'POST','/product/add',base)))
    mainId=await publish(seller)
    await check('PUBLISH pending and hidden to guests',async()=>{assert.equal((await detail(seller,mainId)).status,0);assert.equal((await call(null,'GET',`/guest/product/detail/${mainId}`)).code,404)})
    await check('OWNER other user cannot edit pending product',async()=>{assert.equal((await call(buyer,'PUT','/product/update',{id:mainId,title:'越权修改'})).code,403)})
    await check('ROLE ordinary user cannot audit',async()=>denied(await call(seller,'PUT','/product/audit',{id:mainId,status:1})))
    await check('AUDIT approve becomes public',async()=>{await approve(mainId);assert.equal(ok(await call(null,'GET',`/guest/product/detail/${mainId}`)).status,1)})
    await check('AUDIT repeated audit rejected',async()=>denied(await call(admin,'PUT','/product/audit',{id:mainId,status:1})))
    for(const sortBy of ['priceAsc','priceDesc']) await check(`FILTER ${sortBy} and price range`,async()=>{
      const data=ok(await call(null,'POST','/guest/product/market',{current:1,size:50,minPrice:10,maxPrice:30,sortBy}))
      const items=data.productList;assert.ok(items.length>0);items.forEach(x=>assert.ok(x.price>=10&&x.price<=30&&x.status===1))
      for(let i=1;i<items.length;i++)assert.ok(sortBy==='priceAsc'?items[i-1].price<=items[i].price:items[i-1].price>=items[i].price)
    })
    await check('PAGINATION admin UI offered page size 100 works',async()=>ok(await call(admin,'POST','/product/listPage',{current:1,size:100})))
    await check('FILTER invalid range rejected',async()=>denied(await call(null,'POST','/guest/product/market',{minPrice:20,maxPrice:10})))
    await check('RECOMMEND cold-start list contains only approved products',async()=>{const items=ok(await call(outsider,'GET','/recommend/public/list?size=10'));items.forEach(p=>assert.equal(p.status,1))})
    const old=await detail(seller,mainId)
    await check('EDIT save re-enters review and clears original price',async()=>{ok(await call(seller,'PUT','/product/update',{id:mainId,version:old.version,title:`验收_${run}_edited`,clearOriginalPrice:true}));const p=await detail(seller,mainId);assert.equal(p.status,0);assert.ok(p.originalPrice==null);assert.equal((await call(null,'GET',`/guest/product/detail/${mainId}`)).code,404)})
    await check('EDIT stale version rejected',async()=>assert.equal((await call(seller,'PUT','/product/update',{id:mainId,version:old.version,title:'旧版本覆盖'})).code,409))
    await approve(mainId)
    await check('FAVORITE toggle on and off persists',async()=>{assert.equal(ok(await call(buyer,'POST',`/product/favorite/toggle/${mainId}`)).favorited,true);assert.equal(ok(await call(buyer,'GET',`/product/interact/status/${mainId}`)).favorited,true);assert.equal(ok(await call(buyer,'POST',`/product/favorite/toggle/${mainId}`)).favorited,false)})
    await check('WANT repeat operation counts only once',async()=>{ok(await call(buyer,'POST',`/product/want/${mainId}`));ok(await call(buyer,'POST',`/product/want/${mainId}`));assert.equal((await detail(seller,mainId)).wantCount,1)})
    await check('DOWN hidden and rejects new interactions',async()=>{ok(await call(seller,'PUT',`/product/down/${mainId}`));assert.equal((await detail(seller,mainId)).status,3);assert.equal((await call(null,'GET',`/guest/product/detail/${mainId}`)).code,404);denied(await call(outsider,'POST',`/product/favorite/toggle/${mainId}`))})
    await check('EDIT down product remains down',async()=>{const p=await detail(seller,mainId);ok(await call(seller,'PUT','/product/update',{id:mainId,version:p.version,description:'下架后编辑'}));assert.equal((await detail(seller,mainId)).status,3)})
    await check('UP requires review again',async()=>{ok(await call(seller,'PUT',`/product/up/${mainId}`));assert.equal((await detail(seller,mainId)).status,0)})
    await check('AUDIT reject and remark persists',async()=>{ok(await call(admin,'PUT','/product/audit',{id:mainId,status:2,auditRemark:'验收拒绝原因'}));assert.equal((await detail(seller,mainId)).status,2)})
    const trade=await publish(seller,5);await approve(trade)
    await check('ORDER seller cannot purchase own product',async()=>denied(await call(seller,'POST','/order/create',{productId:trade,quantity:1})))
    let o=await order(buyer,trade)
    await check('ORDER create correct amount and deduct stock',async()=>{assert.equal(Number(o.payAmount),12.34);assert.equal((await detail(seller,trade)).stock,4)})
    await check('ORDER unrelated account cannot read order details',async()=>denied(await call(outsider,'GET',`/order/detail/${o.id}`)))
    await check('ORDER buyer cancellation restores stock',async()=>{ok(await call(buyer,'PUT',`/order/cancel/${o.id}`));assert.equal((await detail(seller,trade)).stock,5)})
    o=await order(buyer,trade)
    await check('ORDER pay-ship-receive lifecycle',async()=>{ok(await call(buyer,'PUT',`/order/pay/${o.id}?payMethod=3`));ok(await call(seller,'PUT',`/order/ship/${o.id}`));ok(await call(buyer,'PUT',`/order/receive/${o.id}`));assert.equal(ok(await call(buyer,'GET',`/order/detail/${o.id}`)).orderStatus,3)})
    await check('ORDER refund restores stock',async()=>{ok(await call(buyer,'PUT',`/order/refund/${o.id}`));ok(await call(seller,'PUT',`/order/agree-refund/${o.id}`));assert.equal((await detail(seller,trade)).stock,5)})
    await check('DELETE product with orders is protected',async()=>assert.equal((await call(seller,'DELETE',`/product/my/${trade}`)).code,409))
    const finite=await publish(seller,3);await approve(finite);await order(buyer,finite,2)
    await check('STOCK remaining 1 of 3 must stay available after buying 2',async()=>{const p=await detail(seller,finite);assert.equal(p.stock,1);assert.equal(p.status,1,'remaining stock must not be prematurely taken down')})
    const unlimited=await publish(seller,0);await approve(unlimited);const u=await order(buyer,unlimited,2);ok(await call(buyer,'PUT',`/order/cancel/${u.id}`))
    await check('STOCK unlimited remains 0 after cancellation',async()=>assert.equal((await detail(seller,unlimited)).stock,0,'cancellation must not convert unlimited to finite stock'))
    await check('ORDER detail permits buyer and seller but not anonymous',async()=>{
      ok(await call(buyer,'GET',`/order/detail/${o.id}`));ok(await call(seller,'GET',`/order/detail/${o.id}`));denied(await call(null,'GET',`/order/detail/${o.id}`))
    })
    await check('PAGINATION above supported 100 rejected',async()=>denied(await call(admin,'POST','/product/listPage',{current:1,size:101})))
    await check('STOCK exact sellout prevents further purchases and cancellation restores once',async()=>{
      const id=await publish(seller,1);await approve(id);const purchased=await order(buyer,id)
      const p=await detail(seller,id);assert.equal(p.stock,0);assert.equal(p.status,3)
      denied(await call(outsider,'POST','/order/create',{productId:id,quantity:1}))
      ok(await call(buyer,'PUT',`/order/cancel/${purchased.id}`));assert.equal((await detail(seller,id)).stock,1)
      denied(await call(buyer,'PUT',`/order/cancel/${purchased.id}`));assert.equal((await detail(seller,id)).stock,1)
    })
    await check('STOCK concurrent cancel returns inventory only once',async()=>{
      const id=await publish(seller,5);await approve(id);const purchased=await order(buyer,id,2)
      const responses=await Promise.all([call(buyer,'PUT',`/order/cancel/${purchased.id}`),call(buyer,'PUT',`/order/cancel/${purchased.id}`)])
      assert.equal(responses.filter(r=>r.code===200).length,1);assert.equal(responses.filter(r=>r.code===409).length,1)
      assert.equal((await detail(seller,id)).stock,5)
    })
    await check('STOCK concurrent buyers cannot oversell last unit',async()=>{
      const id=await publish(seller,1);await approve(id)
      const responses=await Promise.all([call(buyer,'POST','/order/create',{productId:id,quantity:1}),call(outsider,'POST','/order/create',{productId:id,quantity:1})])
      assert.equal(responses.filter(r=>r.code===200).length,1);responses.filter(r=>r.code===200).forEach(r=>fixtures.orders.push(r.data.id))
      responses.filter(r=>r.code!==200).forEach(denied)
      const p=await detail(seller,id);assert.equal(p.stock,0);assert.equal(p.status,3)
    })
    await check('STOCK unlimited refund remains unlimited',async()=>{
      const id=await publish(seller,0);await approve(id);const purchased=await order(buyer,id,2)
      ok(await call(buyer,'PUT',`/order/pay/${purchased.id}?payMethod=3`));ok(await call(seller,'PUT',`/order/ship/${purchased.id}`))
      ok(await call(buyer,'PUT',`/order/refund/${purchased.id}`));ok(await call(seller,'PUT',`/order/agree-refund/${purchased.id}`))
      assert.equal((await detail(seller,id)).stock,0)
    })
    await check('STOCK seller and admin concurrent refund returns inventory once',async()=>{
      const id=await publish(seller,5);await approve(id);const purchased=await order(buyer,id,2)
      ok(await call(buyer,'PUT',`/order/pay/${purchased.id}?payMethod=3`));ok(await call(seller,'PUT',`/order/ship/${purchased.id}`));ok(await call(buyer,'PUT',`/order/refund/${purchased.id}`))
      const responses=await Promise.all([call(seller,'PUT',`/order/agree-refund/${purchased.id}`),call(admin,'PUT',`/order/admin/agree-refund/${purchased.id}`)])
      assert.equal(responses.filter(r=>r.code===200).length,1);assert.equal(responses.filter(r=>r.code===409).length,1)
      assert.equal((await detail(seller,id)).stock,5)
    })
    await check('AUDIT concurrent reviewers cannot both change one pending item',async()=>{
      const id=await publish(seller)
      const responses=await Promise.all([call(admin,'PUT','/product/audit',{id,status:1}),call(admin,'PUT','/product/audit',{id,status:2,auditRemark:'并发审核测试'})])
      assert.equal(responses.filter(r=>r.code===200).length,1);assert.equal(responses.filter(r=>r.code===409).length,1)
      assert.equal((await detail(seller,id)).version,1)
    })
    await check('LEGACY unknown stock deduction cannot silently change inventory',async()=>{
      const legacy=JSON.parse(fs.readFileSync('docs/player2-acceptance-evidence/results.json','utf8')).fixtures
      const oldBuyer=ok(await call(null,'POST','/user/login',{account:legacy.accounts.find(x=>x.endsWith('_buyer')),password}))
      const id=legacy.orders[2]
      const before=ok(await call(oldBuyer,'GET',`/order/detail/${id}`))
      assert.equal(before.orderStatus,0)
      const response=await call(oldBuyer,'PUT',`/order/cancel/${id}`)
      assert.equal(response.http,409);assert.equal(response.code,409);assert.ok(response.message.includes('历史订单'))
      assert.equal(ok(await call(oldBuyer,'GET',`/order/detail/${id}`)).orderStatus,0)
    })
    const removable=await publish(seller)
    await check('DELETE own unused product removes it',async()=>{ok(await call(seller,'DELETE',`/product/my/${removable}`));assert.equal((await call(null,'GET',`/guest/product/detail/${removable}`)).code,404)})
    // 浏览器真实发布，避免仅接口测试遗漏表单和上传联动。
    browser=await chromium.launch({channel:'msedge',headless:true})
    const context=await browser.newContext({viewport:{width:1440,height:1000},reducedMotion:'reduce'})
    page=await context.newPage()
    await page.goto('http://127.0.0.1:21091/login')
    await page.evaluate(data=>{localStorage.setItem('TOKEN',data.token);localStorage.setItem('ROLE',data.role);localStorage.setItem('campus-font-scale','1')},seller)
    await check('UI product publish with actual image upload',async()=>{
      await page.goto('http://127.0.0.1:21091/user-home/products')
      await page.getByRole('button',{name:'发布商品',exact:true}).click()
      const dialog=page.getByRole('dialog',{name:'发布商品'})
      await dialog.getByPlaceholder('请输入商品标题').fill(`界面验收_${run}`)
      await dialog.locator('.el-select').first().click()
      await page.getByRole('option').first().click()
      await dialog.getByPlaceholder('¥',{exact:true}).fill('15.25')
      await dialog.locator('input[type=file]').setInputFiles({name:'ui-acceptance.png',mimeType:'image/png',buffer})
      await dialog.locator('.uploaded-image').first().waitFor({timeout:8000})
      const response=page.waitForResponse(r=>r.url().endsWith('/product/add')&&r.request().method()==='POST')
      await dialog.getByRole('button',{name:'提交发布',exact:true}).click()
      ok({http:200,...await(await response).json()})
      await dialog.waitFor({state:'hidden'})
      const items=ok(await call(seller,'POST','/product/myProducts',{title:`界面验收_${run}`,current:1,size:10}));assert.equal(items.length,1);fixtures.products.push(items[0].id)
      await page.screenshot({path:`${out}/ui-published.png`,fullPage:true})
    })
    await check('UI admin product list and detail open',async()=>{
      await page.evaluate(data=>{localStorage.setItem('TOKEN',data.token);localStorage.setItem('ROLE',data.role)},admin)
      await page.goto('http://127.0.0.1:21091/admin-home/products/list')
      await page.getByPlaceholder('请输入标题').fill(`界面验收_${run}`)
      const loaded=page.waitForResponse(r=>r.url().endsWith('/product/listPage')&&r.request().postData()?.includes(`界面验收_${run}`))
      await page.getByRole('button',{name:'搜索',exact:true}).click()
      await loaded
      await page.getByRole('button',{name:'详情',exact:true}).first().click()
      await page.getByRole('dialog',{name:'商品详情'}).waitFor()
      assert.ok((await page.getByRole('dialog',{name:'商品详情'}).innerText()).includes(`界面验收_${run}`))
      await page.screenshot({path:`${out}/admin-detail.png`,fullPage:true})
    })
    await check('UI admin approve published product',async()=>{
      await page.keyboard.press('Escape')
      await page.getByRole('button',{name:'通过',exact:true}).first().click()
      const response=page.waitForResponse(r=>r.url().endsWith('/product/audit')&&r.request().method()==='PUT')
      await page.locator('.el-message-box__btns .el-button--primary').click()
      ok({http:200,...await(await response).json()})
      const items=ok(await call(seller,'POST','/product/myProducts',{title:`界面验收_${run}`,current:1,size:10}))
      assert.equal(items[0].status,1)
    })
    await check('UI seller edit approved product re-enters review',async()=>{
      await page.evaluate(data=>{localStorage.setItem('TOKEN',data.token);localStorage.setItem('ROLE',data.role)},seller)
      await page.goto('http://127.0.0.1:21091/user-home/products')
      await page.getByPlaceholder('搜索我的商品').fill(`界面验收_${run}`)
      const loaded=page.waitForResponse(r=>r.url().endsWith('/product/myProducts')&&r.request().postData()?.includes(`界面验收_${run}`))
      await page.locator('.my-products .search-section').getByRole('button',{name:'搜索',exact:true}).click()
      await loaded
      await page.getByRole('button',{name:'编辑',exact:true}).first().click()
      const dialog=page.getByRole('dialog',{name:'编辑商品'})
      await dialog.getByPlaceholder('请输入商品标题').fill(`界面验收_${run}_修改`)
      const response=page.waitForResponse(r=>r.url().endsWith('/product/update')&&r.request().method()==='PUT')
      await dialog.getByRole('button',{name:'保存修改',exact:true}).click()
      ok({http:200,...await(await response).json()})
      await dialog.waitFor({state:'hidden'})
      const items=ok(await call(seller,'POST','/product/myProducts',{title:`界面验收_${run}_修改`,current:1,size:10}))
      assert.equal(items[0].status,0)
    })
  } catch(e){results.push({name:'SETUP or prerequisite',passed:false,error:e.message});console.error(e.message)}
  finally {
    fs.writeFileSync(`${out}/results.json`,JSON.stringify({run,results,fixtures},null,2))
    if(browser)await browser.close();if(api)await api.dispose()
  }
  console.log(JSON.stringify({total:results.length,passed:results.filter(r=>r.passed).length,failed:results.filter(r=>!r.passed)},null,2))
  if(results.some(r=>!r.passed))process.exitCode=1
})()
