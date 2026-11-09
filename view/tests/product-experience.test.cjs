const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const ts = require('typescript')
function load(relative) {
    const source = fs.readFileSync(path.join(__dirname, '..', relative),'utf8').replaceAll('import.meta.env.VITE_API_BASE_URL',JSON.stringify('http://localhost:21090'))
    const compiled = ts.transpileModule(source,{ compilerOptions:{ target:ts.ScriptTarget.ES2020,module:ts.ModuleKind.CommonJS } }).outputText
    const module = { exports:{} }
    new Function('require','module','exports',compiled)((name) => name === './accessibility' ? load('src/utils/accessibility.ts') : require(name),module,module.exports)
    return module.exports
}
const experience = load('src/utils/product-experience.ts')
const { useProductFeed } = load('src/composables/useProductFeed.ts')
test('price range accepts boundaries and rejects reversed and fractional ranges',()=>{
    assert.equal(experience.validateProductFilters({ minPrice:0,maxPrice:99999999.99 }), '')
    assert.match(experience.validateProductFilters({ minPrice:20,maxPrice:10 }), /最低价格/)
    assert.notEqual(experience.validateProductFilters({ minPrice:0.001 }), '')
})
test('image checks reject empty files, renamed files, oversize and tenth image',()=>{
    const file={name:'book.png',type:'image/png',size:100}
    assert.equal(experience.validateProductImage(file,8),'')
    assert.notEqual(experience.validateProductImage(file,9),'')
    assert.notEqual(experience.validateProductImage({...file,size:0},0),'')
    assert.notEqual(experience.validateProductImage({...file,name:'book.html'},0),'')
    assert.notEqual(experience.validateProductImage({...file,size:2*1024*1024+1},0),'')
})
test('image URL helper joins local API and rejects non-image source schemes',()=>{
    assert.equal(experience.productImageUrl('/files/a.png'),'http://localhost:21090/files/a.png')
    assert.equal(experience.productImageUrl('javascript:alert(1)'),experience.PRODUCT_IMAGE_FALLBACK)
})
test('failed next page retains products and page number, retry keeps query snapshot',async()=>{
    let fail=false; const calls=[]
    const feed=useProductFeed(async(page,q)=>{ calls.push({page,q}); if(fail)throw Error('offline'); return { items:[page],total:3 } })
    const query={title:'教材',minPrice:1,sortBy:'priceAsc'}
    await feed.request(1,false,query)
    fail=true; await feed.request(2,true,query)
    assert.deepEqual(feed.items.value,[1]); assert.equal(feed.currentPage.value,1)
    query.title='changed'; fail=false; await feed.retry()
    assert.equal(calls.at(-1).q.title,'教材'); assert.equal(feed.currentPage.value,2)
    assert.deepEqual(feed.items.value,[1,2])
})
test('late old query cannot overwrite newly filtered results',async()=>{
    let finishOld
    const feed=useProductFeed(async(_page,q)=>q.title==='old'?await new Promise(resolve=>{finishOld=resolve}):{items:['new'],total:1})
    const old=feed.request(1,false,{title:'old'})
    await feed.request(1,false,{title:'new'})
    finishOld({items:['old'],total:10}); await old
    assert.deepEqual(feed.items.value,['new']); assert.equal(feed.hasMore.value,false)
})
test('failed refresh preserves visible products and can retry',async()=>{
    let fail=false
    const feed=useProductFeed(async()=>{ if(fail)throw Error('offline');return {items:['book'],total:1} })
    await feed.request(1,false,{})
    fail=true;await feed.request(1,false,{title:'phone'})
    assert.deepEqual(feed.items.value,['book']);assert.ok(feed.error.value);assert.equal(feed.loading.value,false)
})
test('duplicate page requests are suppressed and disposed feeds ignore late responses',async()=>{
    let resolve; let calls=0
    const feed=useProductFeed(async()=>{calls++;return await new Promise(done=>{resolve=done})})
    const pending=feed.request(1,false,{})
    await feed.request(2,true,{})
    assert.equal(calls,1)
    feed.dispose();resolve({items:['late'],total:1});await pending
    assert.deepEqual(feed.items.value,[])
})
