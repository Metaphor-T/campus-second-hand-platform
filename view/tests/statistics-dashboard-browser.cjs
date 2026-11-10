const { chromium } = require('../../.local-backups/a11y-tools/node_modules/playwright')
const { AxeBuilder } = require('../../.local-backups/a11y-tools/node_modules/@axe-core/playwright')
const fs = require('node:fs')
const assert = require('node:assert/strict')
const out = '.local-backups/statistics-dashboard-results'
fs.mkdirSync(out, { recursive: true })
const sum = (rows, key) => rows.reduce((total, row) => total + row[key], 0)
const panelGeometry = page => page.locator('.dashboard-charts > .chart-card').evaluateAll(cards => cards.map(card => {
  const rect = card.getBoundingClientRect(), grid = card.parentElement.getBoundingClientRect()
  return { x: rect.x - grid.x, y: rect.y - grid.y, width: rect.width, height: rect.height, shadow: getComputedStyle(card).boxShadow }
}))
function consistent(data) {
  const p = data.period
  for (const [todayKey, totalKey] of [['todayUsers','users'], ['todayProducts','products'], ['todayOrders','orders']]) {
    assert.ok(Number.isInteger(data.overview[todayKey]) && data.overview[todayKey] >= 0)
    assert.ok(data.overview[todayKey] <= data.overview[totalKey])
  }
  for (const key of ['users', 'products', 'orders']) assert.equal(sum(data.trends, key), p[key], `trend sum: ${key}`)
  for (const key of ['products', 'completedOrders', 'completedQuantity']) assert.equal(sum(data.categories, key), p[key], `category sum: ${key}`)
  assert.ok(Math.abs(sum(data.categories, 'completedAmount') - p.completedAmount) < .005)
  assert.equal(sum(data.orderStatus, 'count'), p.orders)
  for (const r of data.ratios) {
    assert.ok(r.numerator <= r.denominator)
    if (!r.denominator) assert.equal(r.percent, null)
    else assert.ok(Math.abs(r.percent - r.numerator / r.denominator * 100) <= .051)
  }
  assert.equal(data.timezone, 'Asia/Shanghai')
}
;(async () => {
  const browser = await chromium.launch({ channel: 'msedge', headless: true })
  const context = await browser.newContext({ reducedMotion: 'reduce' })
  const page = await context.newPage()
  const results = [], errors = []
  page.on('pageerror', error => errors.push(error.message))
  const responseForStats = () => page.waitForResponse(r => new URL(r.url()).pathname === '/stats/dashboard')
  const settled = () => page.locator('.dashboard-container[aria-busy="false"]').waitFor()
  try {
    const seed = fs.readFileSync('sql/db.sql', 'utf8').match(/INSERT INTO `base_user` VALUES \(1, '([^']+)', '([^']+)'/)
    const login = await (await context.request.post('http://127.0.0.1:21090/admin/login', { data: { account: seed[1], password: seed[2] } })).json()
    assert.equal(login.code, 200)
    await page.goto('http://127.0.0.1:21091/login')
    for (const width of [1440, 1024, 768, 320]) for (const scale of [1, 1.25, 1.5, 2]) {
      await page.setViewportSize({ width, height: 1100 })
      await page.evaluate(({ data, scale }) => {
        localStorage.setItem('TOKEN', data.token); localStorage.setItem('ROLE', data.role)
        localStorage.setItem('campus-font-scale', String(scale))
        localStorage.removeItem('SIDEBAR_COLLAPSED'); localStorage.removeItem('SIDEBAR_WIDTH')
      }, { data: login.data, scale })
      const response = responseForStats()
      await page.goto('http://127.0.0.1:21091/admin-home/dashboards')
      const data = (await (await response).json()).data
      assert.ok(data, 'dashboard API must succeed'); consistent(data)
      await settled()
      assert.equal(await page.locator('.stat-card').count(), 12)
      assert.equal(await page.locator('.stat-today-badge').count(), 3, 'only three cumulative cards have daily badges')
      assert.equal(await page.locator('.period-cards .stat-today-badge').count(), 0)
      for (const [key,todayKey] of [['users','todayUsers'],['products','todayProducts'],['orders','todayOrders']]) {
        const card = page.locator(`[data-stat="overview-${key}"]`)
        assert.equal(await card.locator('.stat-today-badge').textContent(), `今日 +${new Intl.NumberFormat('zh-CN').format(data.overview[todayKey])}`)
        const geometry = await card.evaluate(el => {
          const b = el.querySelector('.stat-today-badge').getBoundingClientRect(), v = el.querySelector('.stat-value').getBoundingClientRect(), c = el.getBoundingClientRect()
          return { contained: b.left >= c.left && b.right <= c.right + 1 && b.bottom <= c.bottom + 1,
            overlaps: b.left < v.right && b.right > v.left && b.top < v.bottom && b.bottom > v.top }
        })
        assert.ok(geometry.contained); assert.equal(geometry.overlaps, false, 'badge must not cover cumulative value')
      }
      assert.equal(await page.locator('.stat-icon svg').count(), 12, 'original colored stat icons must remain visible')
      const panelsBefore = await panelGeometry(page)
      assert.equal(panelsBefore.length, 4)
      assert.ok(Math.max(...panelsBefore.map(p => p.height)) - Math.min(...panelsBefore.map(p => p.height)) < 1, 'four panel heights must match')
      assert.ok(Math.max(...panelsBefore.map(p => p.width)) - Math.min(...panelsBefore.map(p => p.width)) < 1, 'four panel widths must match')
      assert.ok(panelsBefore.every(p => p.shadow !== 'none'), 'panel shadows must be present')
      if (width === 1440) { assert.equal(panelsBefore[0].y, panelsBefore[1].y); assert.equal(panelsBefore[2].y, panelsBefore[3].y) }
      if (data.period.orders > 0) for (const status of data.orderStatus) {
        const row = page.locator(`.status-legend [data-status="${status.status}"]`)
        assert.equal(await row.locator('strong').textContent(), `${status.share.toFixed(1)}%`)
      }
      const geometry = await page.locator('.dashboard-container').evaluate(el => ({
        pageWidth: document.documentElement.scrollWidth, width: innerWidth,
        formWidth: el.querySelector('.range-form').clientWidth, formScroll: el.querySelector('.range-form').scrollWidth,
        clippedValues: [...el.querySelectorAll('.stat-value')].filter(n => n.scrollWidth > n.clientWidth + 1).length,
      }))
      assert.ok(geometry.pageWidth <= width + 1, 'page must not overflow')
      assert.ok(geometry.formScroll <= geometry.formWidth + 1, 'date controls must not overflow')
      assert.equal(geometry.clippedValues, 0)
      if (width === 1440 && [1, 2].includes(scale)) {
        await page.screenshot({ path: `${out}/dashboard-${width}-${scale}.png` })
        await page.locator('.dashboard-charts').scrollIntoViewIfNeeded()
        await page.screenshot({ path: `${out}/dashboard-charts-${scale}.png` })
      }
      // Native summaries remain keyboard accessible, including the data behind each canvas chart.
      for (const summary of await page.locator('.data-details > summary').all()) { await summary.focus(); await page.keyboard.press('Enter') }
      const panelsAfter = await panelGeometry(page)
      assert.deepEqual(panelsAfter.map(({ shadow, ...frame }) => frame), panelsBefore.map(({ shadow, ...frame }) => frame), 'expanding tables must not change panel frames or grid positions')
      const body = page.locator('.chart-card-body').first()
      await body.evaluate(el => { el.scrollTop = 0 })
      await body.hover()
      const outerScroll = await page.locator('.admin-layout .content').evaluate(el => el.scrollTop)
      await page.mouse.wheel(0, 400)
      await page.waitForFunction(() => document.querySelector('.chart-card-body').scrollTop > 0)
      assert.equal(await page.locator('.admin-layout .content').evaluate(el => el.scrollTop), outerScroll, 'wheel must scroll within the panel')
      assert.equal(await page.locator('table[data-table="trends"] tbody tr').count(), data.trends.length)
      assert.equal(await page.locator('table[data-table="statuses"] tbody tr').count(), data.orderStatus.length)
      const scan = await new AxeBuilder({ page }).include('.dashboard-container').withTags(['wcag2a','wcag2aa','wcag21aa','wcag22aa']).analyze()
      assert.deepEqual(scan.violations.map(v => ({ id: v.id, targets: v.nodes.map(n => n.target) })), [])
      results.push({ test: 'responsive-and-data', width, scale, passed: true, geometry, panels: panelsAfter, internalScroll: true, days: data.trends.length })
    }
    await page.setViewportSize({ width: 1440, height: 1100 })
    await page.evaluate(() => localStorage.setItem('campus-font-scale', '1'))
    await page.reload(); await settled()
    const todayBaseline = await page.locator('.overview-cards .stat-today-badge').allTextContents()
    for (const days of [7, 30, 90]) {
      const response = responseForStats()
      await page.getByRole('button', { name: `近 ${days} 天`, exact: true }).click()
      const data = (await (await response).json()).data; consistent(data); await settled()
      assert.equal(data.trends.length, days)
      assert.equal((Date.parse(data.endDate) - Date.parse(data.startDate)) / 86400000, days - 1)
      assert.deepEqual(await page.locator('.overview-cards .stat-today-badge').allTextContents(), todayBaseline)
      results.push({ test: 'preset', days, passed: true })
    }
    await page.emulateMedia({ reducedMotion: 'no-preference' })
    let sliderResponse = responseForStats()
    await page.getByRole('button', { name: '近 7 天', exact: true }).focus()
    await page.keyboard.press('Enter'); await sliderResponse; await settled(); await page.waitForTimeout(300)
    const slider = await page.locator('.range-presets .segmented-indicator').boundingBox()
    const active = await page.getByRole('button', { name: '近 7 天', exact: true }).boundingBox()
    assert.ok(Math.abs(slider.x - active.x) < 1 && Math.abs(slider.width - active.width) < 1)
    assert.ok((await page.locator('.range-presets .segmented-indicator').evaluate(el => getComputedStyle(el).transitionProperty)).includes('transform'))
    results.push({ test: 'capsule-slider-keyboard-and-normal-motion', passed: true })
    await page.emulateMedia({ reducedMotion: 'reduce' })
    await page.locator('#stats-start').fill('2026-06-01'); await page.locator('#stats-end').fill('2026-06-30')
    let historicalResponse = responseForStats()
    await page.getByRole('button', { name: '应用日期', exact: true }).click()
    const historical = (await (await historicalResponse).json()).data; await settled(); consistent(historical)
    assert.ok(historical.period.completedOrders > 0, 'original June fixture orders must be present')
    assert.equal(await page.locator('[data-stat="period-completedQuantity"] .stat-value').textContent(), new Intl.NumberFormat('zh-CN').format(historical.period.completedQuantity))
    await page.getByRole('button', { name: '成交件数', exact: true }).click()
    const categorySection = page.locator('.chart-card[aria-labelledby="category-title"]')
    await categorySection.locator('canvas').waitFor()
    assert.equal(await categorySection.locator('.empty-data').count(), 0)
    await categorySection.scrollIntoViewIfNeeded()
    await page.screenshot({ path: `${out}/dashboard-historical.png` })
    results.push({ test: 'historical-completed-orders-and-quantity-chart', passed: true, period: historical.period })
    await page.locator('#stats-start').fill('2004-02-28'); await page.locator('#stats-end').fill('2004-03-01')
    assert.ok((await page.locator('.draft-notice').textContent()).includes('应用日期'))
    let response = responseForStats()
    await page.getByRole('button', { name: '应用日期', exact: true }).click()
    const empty = (await (await response).json()).data; await settled(); consistent(empty)
    assert.equal(empty.trends.length, 3); assert.equal(empty.trends[1].date, '2004-02-29')
    assert.equal(empty.period.orders, 0); assert.ok(empty.ratios.every(r => r.percent === null))
    assert.deepEqual(await page.locator('.overview-cards .stat-today-badge').allTextContents(), todayBaseline)
    results.push({ test: 'today-badges-independent-of-selected-range', passed: true, badgeLabels: todayBaseline })
    assert.equal(await page.locator('.ratio-item strong').filter({ hasText: '暂无样本' }).count(), 5)
    assert.ok(await page.locator('.snapshot-meta').textContent().then(t => t.includes('2004-02-28')))
    results.push({ test: 'custom-leap-days-and-zero-samples', passed: true })
    let requests = 0
    page.on('request', request => { if (new URL(request.url()).pathname === '/stats/dashboard') requests++ })
    await page.locator('#stats-start').fill('2004-03-02')
    await page.getByRole('button', { name: '应用日期', exact: true }).click()
    assert.ok((await page.locator('#stats-range-error').textContent()).includes('开始日期不能晚于'))
    assert.equal(requests, 0)
    results.push({ test: 'invalid-range-does-not-request', passed: true })
    const previous = await page.locator('.snapshot-meta').textContent()
    await page.route('**/stats/dashboard**', route => route.fulfill({ status: 503, contentType: 'application/json', body: JSON.stringify({ code: 503, message: '测试：统计暂不可用' }) }), { times: 1 })
    await page.getByRole('button', { name: '刷新数据', exact: true }).click()
    await page.locator('.load-error').waitFor(); await settled()
    assert.ok((await page.locator('.load-error').textContent()).includes('保留上次'))
    assert.equal(await page.locator('.snapshot-meta').textContent(), previous)
    response = responseForStats()
    await page.getByRole('button', { name: '重新加载', exact: true }).click(); await response; await settled()
    assert.equal(await page.locator('.load-error').count(), 0)
    results.push({ test: 'failed-refresh-retains-labeled-snapshot-and-retries', passed: true })
    await page.route('**/stats/dashboard**', route => route.fulfill({ status: 503, contentType: 'application/json', body: JSON.stringify({ code: 503, message: '测试：首次加载失败' }) }), { times: 1 })
    await page.reload(); await page.locator('.load-error').waitFor(); await settled()
    assert.equal(await page.locator('.stat-card').count(), 0, 'failed first load must not show fake zeros')
    response = responseForStats(); await page.getByRole('button', { name: '重新加载', exact: true }).click(); await response; await settled()
    results.push({ test: 'failed-initial-load-shows-no-fake-zero-cards', passed: true })
    const headers = { Authorization: `Bearer ${login.data.token}` }
    for (const range of [ { startDate: '2026-02-30', endDate: '2026-03-01' }, { startDate: '2024-01-01', endDate: '2025-01-01' }, { startDate: '2099-01-01', endDate: '2099-01-02' } ]) {
      const invalid = await context.request.get('http://127.0.0.1:21090/stats/dashboard', { headers, params: range })
      assert.equal(invalid.status(), 400)
    }
    results.push({ test: 'server-rejects-invalid-long-and-future-ranges', passed: true })
    assert.deepEqual(errors, [])
  } catch (error) { await page.screenshot({ path: `${out}/failure.png` }).catch(() => {}); throw error }
  finally { fs.writeFileSync(`${out}/results.json`, JSON.stringify({ time: new Date().toISOString(), results, errors }, null, 2)); await browser.close() }
  console.log(`PASS: ${results.length} dashboard layout, date, consistency and failure/retry checks`)
})().catch(error => { console.error(error); process.exitCode = 1 })
