const adminCredentials = require('./helpers/test-config.cjs').getAdminCredentials()
const { chromium } = require('../../.local-backups/a11y-tools/node_modules/playwright')
const fs = require('node:fs')
const assert = require('node:assert/strict')
const out = '.local-backups/admin-sidebar-results'
fs.mkdirSync(out, { recursive: true })

;(async () => {
  const browser = await chromium.launch({ channel: 'msedge', headless: true })
  // Use normal motion so width/title animation regressions remain observable.
  const context = await browser.newContext({ reducedMotion: 'no-preference' })
  const page = await context.newPage()
  const results = [], errors = []
  page.on('pageerror', error => errors.push(error.message))
  try {
    const login = await (await context.request.post('http://127.0.0.1:21090/admin/login', { data: { account: adminCredentials.account, password: adminCredentials.password } })).json()
    assert.equal(login.code, 200)
    await page.goto('http://127.0.0.1:21091/login')
    const destinations = [ ['商品列表', '/admin-home/products/list'], ['订单管理', '/admin-home/products/orders'], ['评价管理', '/admin-home/products/reviews'] ]
    for (const width of [1440, 1024, 768, 320]) for (const scale of [1, 1.25, 1.5, 2]) {
      await page.setViewportSize({ width, height: 1000 })
      await page.evaluate(({ data, scale }) => {
        localStorage.setItem('TOKEN', data.token); localStorage.setItem('ROLE', data.role)
        localStorage.setItem('campus-font-scale', String(scale))
        localStorage.removeItem('SIDEBAR_COLLAPSED'); localStorage.removeItem('SIDEBAR_WIDTH')
      }, { data: login.data, scale })
      await page.goto('http://127.0.0.1:21091/admin-home/dashboards')
      const toggle = page.locator('.admin-sidebar-toggle')
      await toggle.waitFor()
      if (await toggle.getAttribute('aria-expanded') === 'false') await toggle.click()
      const expanded = page.locator('.nav-group > .nav-group-header')
      // Preserve both an open and a closed inline submenu across a round trip.
      for (const open of [true, false]) {
        if ((await expanded.getAttribute('aria-expanded') === 'true') !== open) await expanded.click()
        await toggle.click()
        assert.equal(await page.locator('.sidebar .nav-children').isVisible(), false)
        const rows = await page.locator('.nav-list > li').evaluateAll(items => items.map(el => {
          const r = el.getBoundingClientRect(); return { top: r.top, bottom: r.bottom, height: r.height }
        }))
        assert.equal(rows.length, 6)
        for (let i = 1; i < rows.length; i++) assert.ok(Math.abs(rows[i].top - rows[i - 1].bottom) < 2, 'top-level icons must form continuous rows')
        assert.ok(Math.max(...rows.map(r => r.height)) - Math.min(...rows.map(r => r.height)) < 2, `collapsed rows must have equal heights: ${JSON.stringify({ width, scale, rows })}`)
        const samples = await page.evaluate(async () => {
          document.querySelector('.admin-sidebar-toggle').click()
          const samples = []
          for (let i = 0; i < 18; i++) {
            await new Promise(requestAnimationFrame)
            const sidebar = document.querySelector('.sidebar'), title = sidebar.querySelector('h2')
            const r = title.getBoundingClientRect(), s = sidebar.getBoundingClientRect()
            samples.push({ titleTop: r.top, titleHeight: r.height, titleWidth: r.width, sidebarWidth: s.width })
          }
          return samples
        })
        for (const key of ['titleTop', 'titleHeight', 'titleWidth', 'sidebarWidth']) {
          assert.ok(Math.max(...samples.map(s => s[key])) - Math.min(...samples.map(s => s[key])) < 1, `title/sidebar must settle on first rendered frame: ${key}`)
        }
        assert.equal(await expanded.getAttribute('aria-expanded'), String(open))
        assert.equal(await page.locator('.sidebar .nav-children').isVisible(), open)
      }
      if (width === 1440 && [1, 2].includes(scale)) await page.locator('.sidebar').screenshot({ path: `${out}/expanded-${scale}.png` })
      await toggle.click()
      const trigger = page.getByRole('button', { name: '商品管理', exact: true })
      await trigger.focus(); await page.keyboard.press('Enter')
      const menu = page.locator('.admin-submenu-popup:visible')
      await menu.waitFor()
      assert.deepEqual(await menu.getByRole('menuitem').allTextContents(), destinations.map(d => d[0]))
      const rect = await menu.boundingBox()
      assert.ok(rect.x >= 0 && rect.x + rect.width <= width + 1, 'submenu must remain in viewport')
      assert.ok(rect.y >= 0 && rect.y + rect.height <= 1001)
      await page.keyboard.press('Escape')
      await menu.waitFor({ state: 'hidden' })
      assert.equal(await trigger.evaluate(el => el === document.activeElement), true, 'Escape must return focus to group trigger')
      if (width === 1440 && [1, 2].includes(scale)) await page.locator('.sidebar').screenshot({ path: `${out}/collapsed-${scale}.png` })
      // Every child remains reachable in the collapsed sidebar by mouse and keyboard.
      for (const [index, [name, path]] of destinations.entries()) {
        await trigger.click(); await menu.waitFor()
        const item = menu.getByRole('menuitem', { name, exact: true })
        if (index === 0) await item.click()
        else { await menu.getByRole('menuitem').first().focus(); for (let i = 0; i < index; i++) await page.keyboard.press('ArrowDown'); await page.keyboard.press('Enter') }
        await page.waitForURL(`**${path}`)
        await menu.waitFor({ state: 'hidden' })
        assert.equal(await toggle.getAttribute('aria-expanded'), 'false')
      }
      assert.ok(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth))
      results.push({ width, scale, passed: true, preservedStates: ['open', 'closed'], destinations: destinations.map(d => d[1]), motion: 'no-preference' })
    }
    assert.deepEqual(errors, [])
  } catch (error) {
    await page.screenshot({ path: `${out}/failure.png` }).catch(() => {})
    throw error
  } finally {
    fs.writeFileSync(`${out}/results.json`, JSON.stringify({ time: new Date().toISOString(), results, errors }, null, 2))
    await browser.close()
  }
  console.log(`PASS: ${results.length} sidebar combinations; open/closed state, frame stability, popup and mouse/keyboard navigation`)
})().catch(error => { console.error(error); process.exitCode = 1 })
