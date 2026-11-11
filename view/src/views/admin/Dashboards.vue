<template>
  <div class="dashboard-container" :aria-busy="loading">
    <div class="dashboard-heading">
      <div><h1>平台数据概览</h1><p>基于本系统业务记录，查看平台规模与所选期间的业务情况。</p></div>
      <button class="dashboard-button" type="button" :disabled="loading" @click="refresh">{{ loading ? '正在加载…' : '刷新数据' }}</button>
    </div>

    <section class="range-panel" aria-labelledby="range-title">
      <h2 id="range-title">统计期间</h2>
      <form class="range-form" @submit.prevent="applyRange">
        <div class="range-presets segmented-control" :class="{ 'has-selection': selectedDays !== null }" :style="{ '--segment-index': [7, 30, 90].indexOf(selectedDays ?? 0), '--segment-count': 3 }" aria-label="快捷日期">
          <span class="segmented-indicator" aria-hidden="true"></span>
          <button v-for="days in [7, 30, 90]" :key="days" type="button" :aria-pressed="selectedDays === days" :disabled="loading" @click="selectDays(days)">近 {{ days }} 天</button>
        </div>
        <label for="stats-start">开始日期<input id="stats-start" v-model="draftStart" type="date" min="1000-01-01" :max="today" required :aria-invalid="!!rangeError" :aria-describedby="rangeError ? 'stats-range-error' : 'stats-range-help'" @input="selectedDays = null" /></label>
        <label for="stats-end">结束日期<input id="stats-end" v-model="draftEnd" type="date" min="1000-01-01" :max="today" required :aria-invalid="!!rangeError" :aria-describedby="rangeError ? 'stats-range-error' : 'stats-range-help'" @input="selectedDays = null" /></label>
        <button class="dashboard-button primary" type="submit" :disabled="loading">应用日期</button>
      </form>
      <p id="stats-range-help" class="muted">按北京时间统计，包含开始日与结束日，每次最多 366 天。日期筛选只影响期间统计，累计概览保持全量。</p>
      <p v-if="rangeError" id="stats-range-error" class="error-text" role="alert">{{ rangeError }}</p>
      <p v-else-if="dirty" class="draft-notice" role="status">日期已调整，请点击“应用日期”更新统计。</p>
    </section>

    <div v-if="error" class="load-error" role="alert">
      <p>{{ error }}{{ snapshot ? ' 当前保留上次成功加载的数据，请留意下方统计期间和更新时间。' : ' 尚未取得统计数据。' }}</p>
      <button class="dashboard-button" type="button" :disabled="loading" @click="retry">重新加载</button>
    </div>
    <p v-if="loading" class="muted" role="status">正在读取统计数据，请稍候…</p>

    <template v-if="snapshot">
      <div class="snapshot-meta" role="status">
        <strong>当前期间：{{ snapshot.startDate }} 至 {{ snapshot.endDate }}</strong>
        <span>数据更新时间：{{ updatedAt }}（北京时间）</span>
      </div>
      <details class="definitions">
        <summary>查看统计口径</summary>
        <ul>
          <li>普通用户按基础账号去重：必须具有普通用户身份，排除管理员身份；包含被禁用的普通用户。</li>
          <li>累计用户、商品、订单卡片中的“今日 +数字”，按北京时间当天的注册、发布、下单时间统计，与所选期间无关；无新增时显示“今日 +0”。</li>
          <li>新增用户、新增商品和新建订单，分别按各自的创建时间归入期间。商品数按发布条目计数，不是库存件数，包含各审核状态；已删除的数据无法计入。创建时间为空的记录无法归入期间。</li>
          <li>订单状态、完成数量和实付额，均指期间下单的订单在查询时的当前情况；不表示期间发生的支付、完成或退款流水。</li>
          <li>“已完成”仅为当前已完成且没有处理中售后的订单。处理中售后优先归类到退款/售后；“已结束”包含取消及售后完成。实付额合计包含运费，不计入其他状态。</li>
          <li>成交件数按已完成订单的购买数量相加；完成订单数按订单条数计算，两者分开展示。</li>
          <li>分类采用商品当前分类；分类或商品不存在的订单归入“未分类 / 商品已删除”。图表最多显示前 10 类，表格包含全部分类。</li>
          <li>业务比例的分子、分母分别列明；没有样本时显示“暂无样本”。百分比保留一位小数，合计可能有舍入差异。</li>
          <li>统计只描述当前数据库中的业务记录，不代表真实收款、财务结算或平台综合健康评分。</li>
        </ul>
      </details>

      <section aria-labelledby="overview-title">
        <div class="section-title"><h2 id="overview-title">累计概览</h2><span>全量数据；今日角标按北京时间，不受日期筛选影响</span></div>
        <div class="stats-row overview-cards">
          <article v-for="card in overviewCards" :key="card.key" class="stat-card" :data-stat="`overview-${card.key}`">
            <div class="stat-icon" :style="{ color: cardVisuals[card.key].color, backgroundColor: `${cardVisuals[card.key].color}20` }" aria-hidden="true"><el-icon><component :is="cardVisuals[card.key].icon" /></el-icon></div>
            <div class="stat-info">
              <div class="stat-value-row"><div class="stat-value">{{ card.value }}</div>
                <span v-if="card.today !== undefined" class="stat-today-badge" :style="{ color: badgeTextColors[card.key], backgroundColor: `${cardVisuals[card.key].color}18` }" :title="`北京时间 ${snapshot.today} 新增，不受期间筛选影响`" :aria-label="`${card.label}，今日新增 ${number(card.today)}，统计日期 ${snapshot.today}，北京时间`">今日 +{{ number(card.today) }}</span>
              </div>
              <div class="stat-label">{{ card.label }}</div><p>{{ card.note }}</p>
            </div>
          </article>
        </div>
      </section>
      <section aria-labelledby="period-title">
        <div class="section-title"><h2 id="period-title">期间统计</h2><span>订单按下单时间归组，状态取当前值</span></div>
        <div class="stats-row period-cards">
          <article v-for="card in periodCards" :key="card.key" class="stat-card" :data-stat="`period-${card.key}`">
            <div class="stat-icon" :style="{ color: cardVisuals[card.key].color, backgroundColor: `${cardVisuals[card.key].color}20` }" aria-hidden="true"><el-icon><component :is="cardVisuals[card.key].icon" /></el-icon></div>
            <div class="stat-info"><div class="stat-value">{{ card.value }}</div><div class="stat-label">{{ card.label }}</div><p>{{ card.note }}</p></div>
          </article>
        </div>
      </section>

      <div class="charts-row dashboard-charts">
        <section class="chart-card" aria-labelledby="trend-title">
          <div class="chart-header"><h2 id="trend-title">每日新增趋势</h2><span>{{ snapshot.trends.length }} 个自然日</span></div>
          <div class="chart-card-body" tabindex="0" role="region" aria-labelledby="trend-title">
          <p class="chart-note">新增用户、新增商品、新建订单；无新增的日期保留为 0。</p>
          <div ref="trendRef" class="chart-box" aria-hidden="true"></div>
          <details class="data-details"><summary>查看每日新增数据表</summary>
            <div class="table-scroll" tabindex="0" role="region" aria-label="每日新增数据表，可滚动">
              <table data-table="trends"><caption>每日新增明细（与趋势图相同数据）</caption><thead><tr><th scope="col">日期</th><th scope="col">用户（人）</th><th scope="col">商品（条）</th><th scope="col">订单（单）</th></tr></thead>
                <tbody><tr v-for="day in snapshot.trends" :key="day.date"><th scope="row">{{ day.date }}</th><td>{{ day.users }}</td><td>{{ day.products }}</td><td>{{ day.orders }}</td></tr></tbody>
                <tfoot><tr><th scope="row">期间合计</th><td>{{ snapshot.period.users }}</td><td>{{ snapshot.period.products }}</td><td>{{ snapshot.period.orders }}</td></tr></tfoot>
              </table>
            </div>
          </details>
          </div>
        </section>
        <section class="chart-card" aria-labelledby="category-title">
          <div class="chart-header"><h2 id="category-title">分类数量与占比</h2></div>
          <div class="chart-card-body" tabindex="0" role="region" aria-labelledby="category-title">
            <div class="category-switch segmented-control has-selection" :style="{ '--segment-index': categoryMetric === 'products' ? 0 : 1, '--segment-count': 2 }" aria-label="分类图表指标">
              <span class="segmented-indicator" aria-hidden="true"></span>
              <button type="button" :aria-pressed="categoryMetric === 'products'" @click="categoryMetric = 'products'">新增商品</button>
              <button type="button" :aria-pressed="categoryMetric === 'completedQuantity'" @click="categoryMetric = 'completedQuantity'">成交件数</button>
            </div>
          <p class="chart-note">图表展示当前指标前 10 类；占比以全部分类合计为分母。</p>
          <div v-show="hasCategoryData" ref="categoryRef" class="chart-box" aria-hidden="true"></div>
          <p v-if="!hasCategoryData" class="empty-data">所选期间暂无该指标数据</p>
          <details class="data-details"><summary>查看全部分类数据表</summary>
            <div class="table-scroll" tabindex="0" role="region" aria-label="全部分类数据表，可横向滚动">
              <table data-table="categories"><caption>分类明细（商品当前分类，包含全部分类）</caption><thead><tr><th scope="col">分类</th><th scope="col">新增商品（条）</th><th scope="col">商品占比</th><th scope="col">完成订单（单）</th><th scope="col">成交件数</th><th scope="col">件数占比</th><th scope="col">实付额（元）</th></tr></thead>
                <tbody><tr v-for="category in snapshot.categories" :key="category.id"><th scope="row">{{ category.name }}</th><td>{{ category.products }}</td><td>{{ percent(category.productShare) }}</td><td>{{ category.completedOrders }}</td><td>{{ category.completedQuantity }}</td><td>{{ percent(category.quantityShare) }}</td><td>{{ money(category.completedAmount) }}</td></tr>
                  <tr v-if="!snapshot.categories.length"><td colspan="7">所选期间暂无分类数据</td></tr></tbody>
                <tfoot><tr><th scope="row">全部分类合计</th><td>{{ snapshot.period.products }}</td><td>{{ snapshot.period.products ? '100.0%' : '暂无样本' }}</td><td>{{ snapshot.period.completedOrders }}</td><td>{{ snapshot.period.completedQuantity }}</td><td>{{ snapshot.period.completedQuantity ? '100.0%' : '暂无样本' }}</td><td>{{ money(snapshot.period.completedAmount) }}</td></tr></tfoot>
              </table>
            </div>
          </details>
          </div>
        </section>
        <section class="chart-card" aria-labelledby="status-title">
          <div class="chart-header"><h2 id="status-title">订单当前状态分布</h2></div>
          <div class="chart-card-body" tabindex="0" role="region" aria-labelledby="status-title">
          <p class="chart-note">所选期间新建的 {{ number(snapshot.period.orders) }} 单订单，查看其当前状态。</p>
          <div v-show="snapshot.period.orders > 0" ref="statusRef" class="chart-box" aria-hidden="true"></div>
          <p v-if="snapshot.period.orders === 0" class="empty-data">所选期间没有新建订单</p>
          <ul v-if="snapshot.period.orders > 0" class="status-legend" aria-label="各订单状态占比">
            <li v-for="status in snapshot.orderStatus" :key="status.status" :data-status="status.status">
              <span class="status-swatch" :style="{ backgroundColor: statusColor(status.status) }" aria-hidden="true"></span>
              <span>{{ status.name }}</span><strong>{{ percent(status.share) }}</strong>
            </li>
          </ul>
          <details class="data-details"><summary>查看订单状态数据表</summary>
            <div class="table-scroll" tabindex="0" role="region" aria-label="订单状态数据表，可滚动">
              <table data-table="statuses"><caption>期间新建订单的当前状态</caption><thead><tr><th scope="col">状态</th><th scope="col">订单（单）</th><th scope="col">占比</th></tr></thead>
                <tbody><tr v-for="status in snapshot.orderStatus" :key="status.status"><th scope="row">{{ status.name }}</th><td>{{ status.count }}</td><td>{{ percent(status.share) }}</td></tr></tbody>
                <tfoot><tr><th scope="row">合计</th><td>{{ snapshot.period.orders }}</td><td>{{ snapshot.period.orders ? '100.0%' : '暂无样本' }}</td></tr></tfoot>
              </table>
            </div>
          </details>
          </div>
        </section>
        <section class="chart-card" aria-labelledby="ratios-title">
          <div class="chart-header"><h2 id="ratios-title">业务比例</h2></div>
          <div class="chart-card-body" tabindex="0" role="region" aria-labelledby="ratios-title">
          <p class="chart-note">分别说明业务现象，不合成为平台健康评分。</p>
          <dl class="ratio-list">
            <div v-for="(ratio, index) in snapshot.ratios" :key="ratio.key" class="ratio-item" :data-ratio="ratio.key" :style="{ '--ratio-color': palette[index], '--ratio-text': ratioTextColors[index] }">
              <dt>{{ ratio.name }}</dt><dd><div class="ratio-values"><strong>{{ percent(ratio.percent) }}</strong><span>分子 {{ number(ratio.numerator) }} / 分母 {{ number(ratio.denominator) }}</span></div>
              <div class="ratio-track" aria-hidden="true"><div :style="{ width: `${ratio.percent ?? 0}%` }"></div></div>
              <p>{{ ratio.definition }}</p></dd>
            </div>
          </dl>
          </div>
        </section>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import * as echarts from 'echarts'
import { User, Goods, ShoppingCart, Coin, Tickets, WarningFilled } from '@element-plus/icons-vue'
import type { Component } from 'vue'
import { getDashboardStats } from '@/api/stats-api'
import type { DashboardStats } from '@/types/stats'

const localToday = () => new Intl.DateTimeFormat('en-CA', { timeZone: 'Asia/Shanghai', year: 'numeric', month: '2-digit', day: '2-digit' }).format(new Date())
const today = ref(localToday())
const draftStart = ref(''), draftEnd = ref(today.value), selectedDays = ref<number | null>(30)
const snapshot = ref<DashboardStats | null>(null)
const loading = ref(false), error = ref(''), rangeError = ref('')
const categoryMetric = ref<'products' | 'completedQuantity'>('products')
let lastRequest: { startDate: string; endDate: string } | undefined
let requestId = 0, disposed = false
const number = (value: number) => new Intl.NumberFormat('zh-CN').format(value)
const money = (value: number) => new Intl.NumberFormat('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(value)
const percent = (value: number | null) => value === null ? '暂无样本' : `${value.toFixed(1)}%`
const dirty = computed(() => !!snapshot.value && (draftStart.value !== snapshot.value.startDate || draftEnd.value !== snapshot.value.endDate))
const updatedAt = computed(() => snapshot.value ? new Intl.DateTimeFormat('zh-CN', { timeZone: 'Asia/Shanghai', dateStyle: 'short', timeStyle: 'medium', hour12: false }).format(new Date(snapshot.value.generatedAt)) : '')
const cardVisuals: Record<string, { icon: Component; color: string }> = {
  users: { icon: User, color: '#409EFF' }, products: { icon: Goods, color: '#67C23A' },
  orders: { icon: ShoppingCart, color: '#E6A23C' }, completedAmount: { icon: Coin, color: '#F56C6C' },
  pendingProducts: { icon: Tickets, color: '#909399' }, refundOrders: { icon: WarningFilled, color: '#FF5722' },
  completedOrders: { icon: Tickets, color: '#67C23A' }, completedQuantity: { icon: Goods, color: '#E6A23C' },
}
const badgeTextColors: Record<string, string> = { users: '#2368a2', products: '#3e7b23', orders: '#94610b' }
const overviewCards = computed(() => {
  const o = snapshot.value!.overview
  return [
    { key: 'users', label: '累计普通用户', value: number(o.users), today: o.todayUsers, note: '排除管理员，包含被禁用账号' },
    { key: 'products', label: '累计商品', value: number(o.products), today: o.todayProducts, note: '包含各审核状态的现存商品' },
    { key: 'orders', label: '累计订单', value: number(o.orders), today: o.todayOrders, note: '包含全部当前订单状态' },
    { key: 'completedAmount', label: '累计完成订单实付额', value: `¥${money(o.completedAmount)}`, note: '当前已完成订单实付合计，含运费' },
    { key: 'pendingProducts', label: '当前待审核商品', value: number(o.pendingProducts), note: '全量商品中当前待审核的数量' },
    { key: 'refundOrders', label: '当前退款 / 售后订单', value: number(o.refundOrders), note: '全量订单中当前处于退款 / 售后状态' },
  ]
})
const periodCards = computed(() => {
  const p = snapshot.value!.period
  return [
    { key: 'users', label: '期间新增普通用户', value: number(p.users), note: '按普通用户注册时间' },
    { key: 'products', label: '期间新增商品', value: number(p.products), note: '每条发布记录计 1，包含各审核状态' },
    { key: 'orders', label: '期间新建订单', value: number(p.orders), note: '按下单时间，包含全部状态' },
    { key: 'completedOrders', label: '期间订单 · 当前已完成', value: number(p.completedOrders), note: '期间下单且当前已完成，单位：单' },
    { key: 'completedQuantity', label: '期间订单 · 成交件数', value: number(p.completedQuantity), note: '上述已完成订单的购买数量合计' },
    { key: 'completedAmount', label: '期间订单 · 已完成实付额', value: `¥${money(p.completedAmount)}`, note: '上述已完成订单实付合计，含运费' },
  ]
})
const categoryRows = computed(() => [...(snapshot.value?.categories ?? [])].filter(row => row[categoryMetric.value] > 0).sort((a,b) => b[categoryMetric.value] - a[categoryMetric.value] || a.id - b.id).slice(0,10))
const hasCategoryData = computed(() => categoryRows.value.length > 0)

async function loadStats(range?: { startDate: string; endDate: string }) {
  const id = ++requestId
  loading.value = true; error.value = ''; lastRequest = range
  try {
    const response = await getDashboardStats(range)
    if (id !== requestId || disposed) return
    if (!response.data) throw new Error('统计接口未返回数据')
    snapshot.value = response.data; today.value = response.data.today
    draftStart.value = response.data.startDate; draftEnd.value = response.data.endDate
    await nextTick(); renderCharts()
  } catch (e: any) {
    if (id === requestId && !disposed) error.value = e?.response?.data?.message || e?.message || '统计数据加载失败，请重试。'
  } finally { if (id === requestId && !disposed) loading.value = false }
}
function applyRange() {
  rangeError.value = ''
  if (!draftStart.value || !draftEnd.value) { rangeError.value = '请选择开始日期和结束日期'; return }
  if (draftStart.value > draftEnd.value) { rangeError.value = '开始日期不能晚于结束日期'; return }
  if (draftEnd.value > today.value) { rangeError.value = '结束日期不能晚于今天（北京时间）'; return }
  if ((Date.parse(draftEnd.value) - Date.parse(draftStart.value)) / 86400000 >= 366) { rangeError.value = '每次最多查询 366 个自然日'; return }
  void loadStats({ startDate: draftStart.value, endDate: draftEnd.value })
}
function selectDays(days: number) {
  const date = new Date(`${today.value}T00:00:00Z`); date.setUTCDate(date.getUTCDate() - days + 1)
  draftStart.value = date.toISOString().slice(0,10); draftEnd.value = today.value; selectedDays.value = days
  applyRange()
}
const refresh = () => { rangeError.value = ''; void loadStats(snapshot.value ? { startDate: snapshot.value.startDate, endDate: snapshot.value.endDate } : undefined) }
const retry = () => { void loadStats(lastRequest) }

const trendRef = ref<HTMLElement>(), categoryRef = ref<HTMLElement>(), statusRef = ref<HTMLElement>()
const charts = new Map<HTMLElement, echarts.ECharts>()
let resizeObserver: ResizeObserver | undefined, fontObserver: MutationObserver | undefined
const palette = ['#409EFF', '#67C23A', '#E6A23C', '#F56C6C', '#909399', '#FF5722', '#9B59B6', '#1ABC9C']
const ratioTextColors = ['#2368a2', '#3e7b23', '#94610b', '#b13b40', '#62676e']
const statusColors: Record<number, string> = { 0: '#F56C6C', 1: '#E6A23C', 2: '#409EFF', 3: '#67C23A', 4: '#909399', 5: '#FF5722' }
const statusColor = (status: number) => statusColors[status] ?? '#909399'
function chart(el: HTMLElement | undefined, options: echarts.EChartsOption) {
  if (!el || !el.clientWidth || disposed) return
  let instance = charts.get(el)
  if (!instance) { instance = echarts.init(el); charts.set(el,instance); resizeObserver?.observe(el) }
  instance.setOption({ animation: !matchMedia('(prefers-reduced-motion: reduce)').matches, color: palette, textStyle: { fontSize: parseFloat(getComputedStyle(document.documentElement).fontSize) }, ...options }, true)
  instance.resize()
}
function renderCharts() {
  const data = snapshot.value
  if (!data || disposed) return
  const fontSize = parseFloat(getComputedStyle(document.documentElement).fontSize)
  const axisLabel = { fontSize, color: '#555b64' }
  const legend = { bottom: 0, textStyle: { fontSize, color: '#555b64' }, itemWidth: fontSize, itemHeight: fontSize * .7 }
  const small = (trendRef.value?.clientWidth ?? 500) < 420
  const base = { tooltip: { trigger: 'axis' as const, renderMode: 'richText' as const, confine: true, textStyle: { fontSize } }, grid: { left: 12, right: 18, top: 30, bottom: fontSize * 4, containLabel: true } }
  chart(trendRef.value, { ...base, legend: { ...legend, data: ['新增用户', '新增商品', '新建订单'] },
    xAxis: { type: 'category', data: data.trends.map(d => d.date), axisLabel: { ...axisLabel, formatter: (value: string) => value.slice(5), hideOverlap: true } },
    yAxis: { type: 'value', minInterval: 1, axisLabel },
    series: (['users','products','orders'] as const).map((key,i) => ({ name: ['新增用户','新增商品','新建订单'][i], type: 'line' as const, data: data.trends.map(d => d[key]), showSymbol: data.trends.length <= 31, connectNulls: false })),
  })
  const metric = categoryMetric.value, names = categoryRows.value.map(d => d.name)
  if (hasCategoryData.value) chart(categoryRef.value, { ...base, grid: { left: 12, right: 36, top: 12, bottom: 28, containLabel: true },
    xAxis: { type: 'value', minInterval: 1, name: metric === 'products' ? '条' : '件', axisLabel, nameTextStyle: { fontSize } },
    yAxis: { type: 'category', inverse: true, data: names, axisLabel: { ...axisLabel, width: small ? 80 : 130, overflow: 'truncate' } },
    series: [{ name: metric === 'products' ? '新增商品（条）' : '成交件数（件）', type: 'bar', data: categoryRows.value.map((d, index) => ({ value: d[metric], itemStyle: { color: palette[index % palette.length], borderRadius: [0,4,4,0] } })), label: { show: true, position: 'right', fontSize } }],
  })
  if (data.period.orders > 0) chart(statusRef.value, { tooltip: { trigger: 'item', renderMode: 'richText', confine: true, textStyle: { fontSize }, formatter: '{b}\n{c} 单（{d}%）' },
    series: [{ type: 'pie', radius: ['36%','62%'], center: ['50%','50%'], percentPrecision: 1, avoidLabelOverlap: true,
      label: { show: true, position: small || fontSize > 20 ? 'inside' : 'outside', fontSize, formatter: small || fontSize > 20 ? '{d}%' : '{b}: {d}%', color: '#303133', backgroundColor: 'rgba(255,255,255,.92)', borderRadius: 4, padding: [3,5] },
      labelLine: { show: !small && fontSize <= 20, length: 10, length2: 10 },
      data: data.orderStatus.filter(s => s.count > 0).map(s => ({ name: s.name, value: s.count, itemStyle: { color: statusColor(s.status), borderRadius: 4 } })) }],
  })
}
watch(categoryMetric, async () => { await nextTick(); renderCharts() })
onMounted(() => {
  resizeObserver = new ResizeObserver(renderCharts)
  fontObserver = new MutationObserver(renderCharts)
  fontObserver.observe(document.documentElement, { attributes: true, attributeFilter: ['data-font-scale'] })
  void loadStats()
})
onBeforeUnmount(() => { disposed = true; requestId++; resizeObserver?.disconnect(); fontObserver?.disconnect(); charts.forEach(instance => instance.dispose()); charts.clear() })
</script>

<style scoped lang="scss">
.dashboard-container { min-width:0; color:#303133; }
h1 { margin:0; font-size:1.7rem; line-height:1.4; }
h2 { margin:0; font-size:1.2rem; line-height:1.5; }
p { line-height:1.7; }
.dashboard-heading, .section-title, .chart-header { display:flex; align-items:center; justify-content:space-between; flex-wrap:wrap; gap:12px; }
.dashboard-heading { margin-bottom:20px; }
.dashboard-heading p { color:#606266; margin:8px 0 0; }
.dashboard-button, .segmented-control button { min-height:44px; padding:8px 14px; border:1px solid #dcdfe6; border-radius:8px; color:#606266; background:#fff; font:inherit; cursor:pointer; line-height:1.5; }
button:disabled { cursor:wait; opacity:.65; }
.dashboard-button.primary { border-color:#286fb4; color:#fff; background:#286fb4; }
.dashboard-button:not(:disabled):hover { color:#2368a2; border-color:#a0cfff; background:#ecf5ff; }
.dashboard-button.primary:not(:disabled):hover { color:#fff; background:#2368a2; }
.range-panel, .definitions { padding:20px; border:1px solid #ebeef5; border-radius:10px; background:#fff; margin-bottom:20px; }
.range-form { display:flex; align-items:end; flex-wrap:wrap; gap:16px; margin-top:16px; }
.range-form label { display:flex; flex:1 1 12rem; flex-direction:column; gap:8px; min-width:0; }
.range-form input { width:100%; min-width:0; box-sizing:border-box; min-height:44px; padding:8px 12px; border:1px solid #c0c4cc; border-radius:8px; color:#303133; background:#fff; font:inherit; line-height:1.5; }
.segmented-control { position:relative; isolation:isolate; display:grid; grid-template-columns:repeat(var(--segment-count),minmax(0,1fr)); padding:4px; gap:0; border-radius:28px; background:#f0f0f0; max-width:100%; box-sizing:border-box; }
.segmented-control button { position:relative; z-index:1; border:0; border-radius:24px; background:transparent; min-width:0; padding:8px 12px; overflow-wrap:anywhere; }
.segmented-control button[aria-pressed="true"] { color:#303133; font-weight:600; }
.segmented-control button:not(:disabled):hover { color:#2368a2; }
.segmented-indicator { position:absolute; z-index:0; top:4px; bottom:4px; left:4px; width:calc((100% - 8px) / var(--segment-count)); border-radius:24px; background:#fff; box-shadow:0 2px 6px rgba(0,0,0,.08); opacity:0; transform:translateX(calc(var(--segment-index) * 100%)); transition:transform .25s ease,opacity .15s ease; pointer-events:none; }
.has-selection .segmented-indicator { opacity:1; }
.range-presets { flex:0 1 24rem; }
.category-switch { width:min(100%,22rem); margin-bottom:12px; }
.muted, .chart-note, .section-title > span { color:#606266; font-size:1rem; }
.muted { margin:12px 0 0; }
.draft-notice { color:#2368a2; margin-bottom:0; }
.error-text { color:#a61d24; margin-bottom:0; }
.load-error { border:1px solid #d4a09e; border-radius:10px; padding:16px; color:#93252a; background:#fff5f4; margin-bottom:20px; }
.load-error p { margin-top:0; }
.snapshot-meta { display:flex; flex-wrap:wrap; justify-content:space-between; gap:8px 20px; margin:20px 0 12px; font-size:1rem; color:#62666c; }
.snapshot-meta strong { color:#41464d; }
summary { cursor:pointer; min-height:44px; line-height:1.6; display:list-item; align-content:center; font-weight:600; }
.definitions { background:#f6f8fb; padding:8px 20px; }
.definitions ul { padding-left:1.5em; line-height:1.8; }
.definitions li + li { margin-top:8px; }
.section-title { margin:24px 0 12px; }
.stats-row { display:grid; grid-template-columns:repeat(3,minmax(0,1fr)); gap:16px; margin:0 0 20px !important; }
.stat-card { display:flex; flex-wrap:wrap; align-items:center; gap:12px; border:1px solid #f0f2f5; border-radius:10px; padding:18px 16px; background:#fff; min-width:0; height:auto; box-shadow:0 2px 12px rgba(0,0,0,.04); transition:box-shadow .2s ease; }
.stat-card:hover { box-shadow:0 6px 20px rgba(0,0,0,.08); }
.stat-icon { display:grid; place-items:center; flex:0 0 52px; width:52px; height:52px; border-radius:10px; font-size:30px; }
.stat-info { flex:1 1 10rem; min-width:0; }
.stat-value-row { display:flex; align-items:flex-start; justify-content:space-between; flex-wrap:wrap; gap:4px 8px; margin-bottom:4px; }
.stat-value-row .stat-value { min-width:0; margin-bottom:0; }
.stat-today-badge { max-width:100%; box-sizing:border-box; margin-left:auto; padding:3px 8px; border-radius:12px; font-size:.9rem; font-weight:600; line-height:1.5; overflow-wrap:anywhere; }
.stat-label { color:#606266; line-height:1.6; }
.stat-value { font-size:1.65rem; line-height:1.5; font-weight:700; color:#303133; overflow-wrap:anywhere; margin:0 0 4px; }
.stat-card p { font-size:.93rem; color:#606266; margin:6px 0 0; }
.dashboard-charts { display:grid; grid-template-columns:repeat(2,minmax(0,1fr)); gap:20px; align-items:stretch; }
.chart-card { display:flex; flex-direction:column; box-sizing:border-box; height:clamp(460px,calc(380px + 6rem),560px); min-width:0; min-height:0; padding:20px; background:#fff; border:1px solid #f0f2f5; border-radius:10px; box-shadow:0 2px 12px rgba(0,0,0,.06); overflow:hidden; transition:box-shadow .2s ease; }
.chart-card:hover { box-shadow:0 6px 24px rgba(0,0,0,.1); }
.chart-header { min-height:44px; flex:0 0 auto; padding-bottom:8px; }
.chart-card-body { flex:1 1 0%; min-height:0; min-width:0; overflow:auto; overscroll-behavior:contain; scrollbar-gutter:stable; padding:0 4px 4px; }
.chart-note { margin:8px 0 12px; line-height:1.7; }
.chart-box { height:clamp(280px,20rem,400px); width:100%; }
.empty-data { display:grid; place-content:center; min-height:200px; color:#656a72; text-align:center; }
.data-details { margin-top:12px; border-top:1px solid #ebeef5; padding-top:8px; }
.table-scroll { overflow:auto; border-radius:6px; }
table { border-collapse:collapse; min-width:100%; font-size:1rem; font-variant-numeric:tabular-nums; }
caption { text-align:left; color:#5d626a; padding:8px 0; line-height:1.6; }
th, td { padding:12px; border-bottom:1px solid #d9dde2; line-height:1.6; text-align:right; white-space:nowrap; }
th:first-child { text-align:left; }
thead, tfoot { background:#f5f7fa; }
.status-legend { display:grid; grid-template-columns:repeat(auto-fit,minmax(min(100%,12rem),1fr)); gap:10px 16px; padding:8px 0; margin:0; list-style:none; }
.status-legend li { display:flex; align-items:center; flex-wrap:wrap; gap:6px; font-size:1rem; color:#606266; }
.status-legend strong { color:#303133; margin-left:auto; }
.status-swatch { width:12px; height:12px; border-radius:3px; flex-shrink:0; }
.ratio-list { margin:0; }
.ratio-item { padding:14px 0; border-top:1px solid #ebeef5; }
.ratio-item dt { font-weight:600; line-height:1.6; }
.ratio-item dd { margin:6px 0; }
.ratio-values { display:flex; align-items:baseline; flex-wrap:wrap; gap:8px 16px; justify-content:space-between; }
.ratio-item dd strong { color:var(--ratio-text); font-size:1.4rem; }
.ratio-item dd span, .ratio-item p { color:#60656d; font-size:.93rem; }
.ratio-item p { margin:8px 0 0; }
.ratio-track { height:6px; border-radius:3px; background:#f0f2f5; overflow:hidden; }
.ratio-track > div { height:100%; background:var(--ratio-color); }
@media (max-width:1100px) { .dashboard-charts { grid-template-columns:minmax(0,1fr); } .stats-row { grid-template-columns:repeat(2,minmax(0,1fr)); } }
@media (max-width:600px) { .range-panel, .chart-card { padding:14px; } .range-form > button { width:100%; } .range-presets { flex-basis:100%; } .stats-row { grid-template-columns:minmax(0,1fr); } }
</style>
