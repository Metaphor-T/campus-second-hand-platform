// 页面夹具测试：所有业务 API 均拦截，不写入数据库。
const {chromium}=require('../../.local-backups/a11y-tools/node_modules/playwright')
const {AxeBuilder}=require('../../.local-backups/a11y-tools/node_modules/@axe-core/playwright')
const fs=require('node:fs'),assert=require('node:assert/strict')
const site='http://127.0.0.1:21091',out='docs/accessibility-evidence'
const labels=['待付款','待发货','待收货','已完成','已取消','退款/售后']
function fixture(status=2,role='BUYER') {
 const reached=status===4?0:status===5?2:status
 const order={id:9001,orderNo:'ORD_ORDER_DETAIL_FIXTURE_9001',productId:8,productTitle:'订单详情验收：校园闲置自行车',productTradeType:3,buyerId:2,buyerName:'验收买家',sellerId:3,sellerName:'验收卖家',quantity:1,unitPrice:100,totalAmount:100,shippingFee:6,payAmount:106,payMethod:1,orderStatus:status,receiverName:'验收收货人',receiverPhone:'13900000000',fullAddress:'测试大学 测试学院 校园自提点',buyerRemark:'请在校门口联系交付\n备注仅用于前端验收',createTime:'2026-11-01 10:00:00',updateTime:'2026-11-04 10:00:00'}
 const times=['2026-11-01 10:00:00',reached>=1?'2026-11-01 11:00:00':null,reached>=2?'2026-11-02 10:00:00':null,reached===3?'2026-11-03 10:00:00':null]
 const keys=['created','paid','shipped','completed'],titles=['提交订单','确认付款（模拟记录）','发货或交付标记','确认交易完成']
 const timeline=keys.map((key,i)=>({key,title:titles[i],time:times[i],state:times[i]?'RECORDED':status===4||status===5?'UNKNOWN':'WAITING',participant:i===2?'卖家':'买家',description:times[i]?'按订单中保存的节点时间展示。':'暂无该节点时间记录。'}))
 if(status===4)timeline.push({key:'cancelled',title:'订单取消或结束',time:'2026-11-03 12:00:00',state:'RECORDED',participant:'具体操作人未单独记录',description:'原因：双方协商取消'})
 if(status===5)timeline.push({key:'after-sale',title:'售后处理中',time:null,state:'UNRECORDED',participant:'买卖双方',description:'申请时间及完整过程未单独记录。'})
 const responsibleRole=[0,2].includes(status)?'BUYER':[1,5].includes(status)?'SELLER':'NONE'
 return {order,viewerRole:role,statusLabel:labels[status],nextStep:{responsibleRole,title:['等待买家确认付款','等待卖家安排交付','等待买家确认完成','交易已完成','订单已结束','等待卖家处理售后'][status],description:'请按实际交易进度在订单中心处理本单。',yours:responsibleRole===role},timeline,notices:[]}
}
;(async()=>{
 const browser=await chromium.launch({channel:'msedge',headless:true}),context=await browser.newContext({reducedMotion:'reduce'}),page=await context.newPage()
 const results=[],errors=[],unexpected=[],requests=[];let current=fixture(),failure=0,delay=0,detailCalls=0
 page.on('pageerror',e=>errors.push(e.message))
 await context.route('**/*',async route=>{
  const u=new URL(route.request().url());if(u.port!=='21090')return route.continue()
  const p=u.pathname;requests.push(p);let data=[]
  if(p==='/user/auth')data={id:2,account:'fixture',username:'验收用户',loginStatus:1}
  else if(/\/order\/detail\/\d+\/overview$/.test(p)){
   detailCalls++;const snapshot=structuredClone(current),status=failure,wait=delay
   if(wait)await new Promise(r=>setTimeout(r,wait))
   return route.fulfill({status:status||200,contentType:'application/json',body:JSON.stringify(status?{code:status,message:status===403?'无权查看该订单':status===404?'订单不存在':'订单详情暂时无法加载，请稍后重试'}:{code:200,data:snapshot})})
  } else if(/\/order\/(buyer|seller)\/list$/.test(p))data=[current.order]
  else if(p.includes('count-by-status'))data={0:0,1:0,2:1,3:0,4:0,5:0}
  else if(p.includes('unread'))data=0
  else if(!p.startsWith('/chat/')&&!p.startsWith('/review/'))unexpected.push(p)
  return route.fulfill({status:200,contentType:'application/json',body:JSON.stringify({code:200,data,total:Array.isArray(data)?data.length:0})})
 })
 if(context.routeWebSocket)await context.routeWebSocket(/\/ws\/chat/,socket=>socket.close())
 async function open(path='/user-home/order-detail/9001',width=1440,scale=1){
  await page.setViewportSize({width,height:1100});await page.goto(site+'/login')
  await page.evaluate(scale=>{localStorage.setItem('TOKEN','frontend_fixture_only');localStorage.setItem('ROLE','USER');localStorage.setItem('campus-font-scale',String(scale))},scale)
  await page.goto(site+path);await page.locator('.order-detail').waitFor()
  await page.waitForFunction(()=>document.querySelector('.order-detail')?.getAttribute('aria-busy')==='false')
 }
 const push=path=>page.evaluate(path=>document.querySelector('#app').__vue_app__.config.globalProperties.$router.push(path),path)
 try {
  for(const status of [0,1,2,3,4,5])for(const role of ['BUYER','SELLER']){
   current=fixture(status,role);await open()
   assert.equal(await page.locator('[data-order-status]').innerText(),labels[status]);assert.ok((await page.locator('.status-line').innerText()).includes(role==='BUYER'?'买家':'卖家'))
   assert.equal(await page.locator('.timeline li').count(),status>=4?5:4)
   assert.ok((await page.locator('.next-step').innerText()).includes(current.nextStep.title))
   const action=page.locator('.next-step a');assert.ok((await action.getAttribute('href')).includes('keyword=ORD_ORDER_DETAIL_FIXTURE_9001'))
   assert.ok((await action.getAttribute('href')).includes('role='+role.toLowerCase()))
   if(status===5)assert.equal(await page.locator('[data-timeline-node="after-sale"] time').count(),0)
   results.push({case:'six-status-both-roles',status,role,passed:true})
  }
  current=fixture(3);current.timeline[3].time=null;current.timeline[3].state='UNRECORDED';current.order.fullAddress='';current.order.buyerRemark='<script>window.hacked=true</script>';current.order.productTitle=null
  await open();assert.ok((await page.locator('[data-timeline-node="completed"]').innerText()).includes('时间未保存'));assert.equal(await page.locator('[data-timeline-node="completed"] time').count(),0)
  assert.ok((await page.locator('.order-detail').innerText()).includes('暂无可用收货地址'));assert.ok((await page.locator('.detail-notes').innerText()).includes('<script>'));assert.equal(await page.evaluate(()=>window.hacked),undefined)
  results.push({case:'legacy-missing-timestamp-address-product-and-safe-text',passed:true})
  current=fixture();current.notices=['当前状态与已保存节点不完全一致，暂不判断下一位处理人。'];current.nextStep={responsibleRole:'NONE',title:'请核对订单记录',description:'请核对。',yours:false}
  await open();assert.equal(await page.locator('.history-notice').count(),1);assert.equal(await page.locator('.turn-tag').count(),0);results.push({case:'inconsistent-history-warning',passed:true})
  for(const status of [403,404,500]){
   failure=status;await open();assert.equal(await page.locator('.detail-layout').count(),0);assert.equal(await page.locator('.detail-error button').count(),status===500?1:0)
   if(status===500){failure=0;await page.getByRole('button',{name:'重新加载'}).click();await page.locator('[data-order-number]').waitFor()}
   results.push({case:'error-and-retry',status,passed:true})
  }
  failure=0;const before=detailCalls
  for(const id of ['0','-1','abc','2147483648']){await open('/user-home/order-detail/'+id);assert.ok((await page.locator('.detail-error').innerText()).includes('订单编号不正确'))}
  assert.equal(detailCalls,before);results.push({case:'invalid-id-no-api-request',passed:true})
  current=fixture(2,'SELLER');await open();await page.locator('.next-step a').click();await page.locator('.user-orders').waitFor()
  assert.equal(await page.locator('.tab-item.active').innerText(),'我的出售');assert.equal(await page.getByPlaceholder('搜索订单编号').inputValue(),current.order.orderNo)
  await page.getByRole('link',{name:'查看详情',exact:true}).click();await page.locator('[data-order-number]').waitFor();assert.ok(page.url().includes('/order-detail/9001'))
  await page.getByRole('link',{name:'返回订单中心'}).click();await page.locator('.order-no a').click();await page.locator('[data-order-number]').waitFor();results.push({case:'entry-links-and-seller-return-filter',passed:true})
  current=fixture(0);delay=500;await push('/user-home/order-detail/9002');await page.locator('.loading-note').waitFor();await open('/user-home/order-detail/0');await page.waitForTimeout(600);assert.equal(await page.locator('.detail-layout').count(),0);delay=0;results.push({case:'stale-result-does-not-replace-invalid-route',passed:true})
  current=fixture(2)
  for(const width of [1440,1024,768,320])for(const scale of [1,1.25,1.5,2]){
   await open('/user-home/order-detail/9001',width,scale)
   const sizes=await page.locator('.order-detail').evaluate(el=>{const b=el.getBoundingClientRect();return {scroll:el.scrollWidth,width:el.clientWidth,right:b.right,window:innerWidth,root:parseFloat(getComputedStyle(document.documentElement).fontSize)}})
   assert.ok(sizes.scroll<=sizes.width+1,JSON.stringify({width,scale,sizes}));assert.ok(sizes.right<=width+1,JSON.stringify({width,scale,sizes}));assert.ok(Math.abs(sizes.root-14*scale)<.1)
   if(width===1440&&(scale===1||scale===2))await page.screenshot({path:`${out}/order-detail-${scale===1?'standard':'large'}.png`,fullPage:true})
   results.push({case:'responsive-four-font-sizes',width,scale,sizes,passed:true})
  }
  await open();const axe=await new AxeBuilder({page}).include('.order-detail').withTags(['wcag2a','wcag2aa','wcag21a','wcag21aa']).analyze()
  assert.deepEqual(axe.violations.map(v=>({id:v.id,impact:v.impact,nodes:v.nodes.map(n=>n.target)})),[]);results.push({case:'wcag-axe-order-detail',rules:axe.passes.length,passed:true})
  assert.deepEqual(errors,[]);assert.deepEqual(unexpected,[])
  fs.writeFileSync(`${out}/order-detail-browser-results.json`,JSON.stringify({mode:'isolated frontend fixtures; no database writes',results,pageErrors:errors,unexpectedApi:unexpected,total:results.length},null,2))
  console.log(JSON.stringify({passed:results.length,pageErrors:errors.length,unexpectedApi:unexpected}))
 } finally {await browser.close()}
})().catch(e=>{console.error(e);process.exitCode=1})
