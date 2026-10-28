<template>
  <div class="dashboard-container">


    <!-- ========== 统计卡片 ========== -->
    <el-row :gutter="20" class="stats-row">
      <el-col :xs="12" :sm="8" :md="6" :lg="4" v-for="card in statCards" :key="card.label">
        <div
          class="stat-card"
          @mouseenter="card.hover = true"
          @mouseleave="card.hover = false"
        >
          <div class="stat-icon" :style="{ background: card.bg + '20', color: card.color }">
            <el-icon :size="30"><component :is="card.icon" /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value" :title="card.value + (card.unit || '')">
              {{ card.value }}{{ card.unit || '' }}
              <span v-if="card.today !== undefined" class="stat-today-badge" :style="{ color: card.color }">今日+{{ card.today }}</span>
            </div>
            <div class="stat-label">{{ card.label }}</div>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- ========== 图表区域 ========== -->
    <el-row :gutter="20" class="charts-row">
      <!-- 趋势图（折线图） -->
      <el-col :xs="24" :lg="12" class="chart-col">
        <div class="chart-card">
          <div class="chart-header">
            <h3>增长趋势</h3>
            <div class="trend-toggle">
              <div
                v-for="opt in trendOptions"
                :key="opt.value"
                class="trend-item"
                :class="{ active: trendDays === opt.value }"
                @click="selectTrend(opt.value)"
              >{{ opt.label }}</div>
            </div>
          </div>
          <div ref="lineChartRef" class="chart-box"></div>
        </div>
      </el-col>

      <!-- 分类销量（柱状图） -->
      <el-col :xs="24" :lg="12" class="chart-col">
        <div class="chart-card">
          <div class="chart-header">
            <h3>分类销量排行</h3>
          </div>
          <div ref="barChartRef" class="chart-box"></div>
        </div>
      </el-col>

      <!-- 订单状态（饼状图） -->
      <el-col :xs="24" :lg="12" class="chart-col">
        <div class="chart-card">
          <div class="chart-header">
            <h3>订单状态分布</h3>
          </div>
          <div ref="pieChartRef" class="chart-box"></div>
        </div>
      </el-col>

      <!-- 平台健康（雷达图） -->
      <el-col :xs="24" :lg="12" class="chart-col">
        <div class="chart-card">
          <div class="chart-header">
            <h3>平台健康评分</h3>
          </div>
          <div ref="radarChartRef" class="chart-box"></div>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script lang="ts" setup>
import { ref, reactive, onMounted, onBeforeUnmount, nextTick } from 'vue'
import * as echarts from 'echarts'
import { Edit, ShoppingCart, User, Goods, Coin, Tickets, WarningFilled, TrendCharts } from '@element-plus/icons-vue'
import { getOverviewStats, getTrends, getCategorySales, getOrderStatus, getRadarData } from '@/api/stats-api'
import type { OverviewStats, TrendsData, TrendItem, CategorySalesItem, OrderStatusItem, RadarData } from '@/types/stats'

// ==================== 统计卡片 ====================
interface StatCard {
  label: string
  value: string | number
  unit?: string
  today?: number
  icon: any
  color: string
  bg: string
}

const statCards = reactive<StatCard[]>([
  { label: '用户总数', value: 0, icon: User, color: '#409EFF', bg: '#409EFF', today: 0 },
  { label: '商品总数', value: 0, icon: Goods, color: '#67C23A', bg: '#67C23A', today: 0 },
  { label: '订单总数', value: 0, icon: ShoppingCart, color: '#E6A23C', bg: '#E6A23C', today: 0 },
  { label: '成交总额', value: 0, unit: '¥', icon: Coin, color: '#F56C6C', bg: '#F56C6C' },
  { label: '待审核商品', value: 0, icon: Tickets, color: '#909399', bg: '#909399' },
  { label: '待退款订单', value: 0, icon: WarningFilled, color: '#FF5722', bg: '#FF5722' },
])

// ==================== 图表 ref ====================
const lineChartRef = ref<HTMLElement>()
const barChartRef = ref<HTMLElement>()
const pieChartRef = ref<HTMLElement>()
const radarChartRef = ref<HTMLElement>()

let lineChart: echarts.ECharts | null = null
let barChart: echarts.ECharts | null = null
let pieChart: echarts.ECharts | null = null
let radarChart: echarts.ECharts | null = null

const trendDays = ref(90)
const trendOptions = [
  { label: '近3个月', value: 90 },
  { label: '近1年', value: 365 },
  { label: '近10年', value: 3650 },
]
const selectTrend = (value: number) => {
  trendDays.value = value
  loadTrends()
}
let resizeObserver: ResizeObserver | null = null

// ==================== 颜色方案 ====================
const colors = ['#409EFF', '#67C23A', '#E6A23C', '#F56C6C', '#909399', '#FF5722', '#9B59B6', '#1ABC9C']

// 订单状态颜色
const statusColorMap: Record<number, string> = {
  0: '#F56C6C',
  1: '#E6A23C',
  2: '#409EFF',
  3: '#67C23A',
  4: '#909399',
  5: '#FF5722',
}

// ==================== 加载数据 ====================

/** 加载概览数据 */
async function loadOverview() {
  try {
    const res = await getOverviewStats()
    const data = res.data!
    statCards[0].value = data.totalUsers ?? 0
    statCards[0].today = data.todayNewUsers ?? 0
    statCards[1].value = data.totalProducts ?? 0
    statCards[2].value = data.totalOrders ?? 0
    statCards[2].today = data.todayNewOrders ?? 0
    statCards[3].value = formatMoney(data.totalRevenue ?? 0)
    statCards[4].value = data.pendingReviewProducts ?? 0
    statCards[5].value = data.pendingRefundOrders ?? 0
  } catch (e) {
    console.error('加载概览数据失败', e)
  }
}

/** 加载趋势数据 */
async function loadTrends() {
  try {
    const res = await getTrends(trendDays.value)
    const data = res.data!
    renderLineChart(data.userTrends, data.orderTrends)
  } catch (e) {
    console.error('加载趋势数据失败', e)
  }
}

/** 加载分类销量 */
async function loadCategorySales() {
  try {
    const res = await getCategorySales()
    const data = res.data!
    renderBarChart(data)
  } catch (e) {
    console.error('加载分类销量失败', e)
  }
}

/** 加载订单状态 */
async function loadOrderStatus() {
  try {
    const res = await getOrderStatus()
    const data = res.data!
    renderPieChart(data)
  } catch (e) {
    console.error('加载订单状态失败', e)
  }
}

/** 加载雷达图数据 */
async function loadRadar() {
  try {
    const res = await getRadarData()
    const data = res.data!
    renderRadarChart(data)
  } catch (e) {
    console.error('加载雷达图数据失败', e)
  }
}

// ==================== 渲染图表 ====================

/** 折线图：用户 & 订单趋势 */
function renderLineChart(userTrends: TrendItem[], orderTrends: TrendItem[]) {
  if (!lineChartRef.value) return
  if (!lineChart) {
    lineChart = echarts.init(lineChartRef.value)
  }

  // 过滤掉用户和订单均为 0 的日期，保留完整日期（年月日）
  const filtered = userTrends
    .map((u, i) => ({ date: u.date, userCount: u.count, orderCount: orderTrends[i]?.count ?? 0 }))
    .filter((d) => d.userCount > 0 || d.orderCount > 0)

  const dates = filtered.map((d) => d.date)
  const userCounts = filtered.map((d) => d.userCount)
  const orderCounts = filtered.map((d) => d.orderCount)

  lineChart.setOption({
    tooltip: { trigger: 'axis', backgroundColor: 'rgba(255,255,255,0.95)', borderColor: '#eee' },
    legend: {
      data: ['新增用户', '新增订单'],
      bottom: 0,
      icon: 'circle',
      itemWidth: 8,
      itemHeight: 8,
    },
    grid: { left: 50, right: 20, top: 30, bottom: 40 },
    xAxis: {
      type: 'category',
      data: dates,
      axisLine: { lineStyle: { color: '#ddd' } },
      axisLabel: { color: '#909399' },
    },
    yAxis: {
      type: 'value',
      minInterval: 1,
      splitLine: { lineStyle: { color: '#f0f0f0', type: 'dashed' } },
      axisLabel: { color: '#909399' },
    },
    series: [
      {
        name: '新增用户',
        type: 'line',
        smooth: true,
        data: userCounts,
        symbol: 'circle',
        symbolSize: 6,
        lineStyle: { width: 3, color: '#409EFF' },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(64,158,255,0.3)' },
            { offset: 1, color: 'rgba(64,158,255,0.02)' },
          ]),
        },
        itemStyle: { color: '#409EFF' },
      },
      {
        name: '新增订单',
        type: 'line',
        smooth: true,
        data: orderCounts,
        symbol: 'diamond',
        symbolSize: 6,
        lineStyle: { width: 3, color: '#67C23A' },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(103,194,58,0.3)' },
            { offset: 1, color: 'rgba(103,194,58,0.02)' },
          ]),
        },
        itemStyle: { color: '#67C23A' },
      },
    ],
  })
}

/** 柱状图：分类销量排行 */
function renderBarChart(data: CategorySalesItem[]) {
  if (!barChartRef.value) return
  if (!barChart) {
    barChart = echarts.init(barChartRef.value)
  }

  const names = data.map((d) => d.name)
  const counts = data.map((d) => d.count)

  barChart.setOption({
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(255,255,255,0.95)',
      borderColor: '#eee',
      formatter: (params: any) => {
        const p = params[0]
        return `<strong>${p.name}</strong><br/>销量: <strong>${p.value}</strong> 件`
      },
    },
    grid: { left: 50, right: 20, top: 20, bottom: 30, containLabel: true },
    xAxis: {
      type: 'category',
      data: names,
      axisLabel: {
        color: '#909399',
        interval: 0,
        rotate: names.length > 5 ? 25 : 0,
      },
      axisLine: { lineStyle: { color: '#ddd' } },
    },
    yAxis: {
      type: 'value',
      minInterval: 1,
      splitLine: { lineStyle: { color: '#f0f0f0', type: 'dashed' } },
      axisLabel: { color: '#909399' },
    },
    series: [
      {
        type: 'bar',
        data: counts.map((v, i) => ({
          value: v,
          itemStyle: {
            color: colors[i % colors.length],
            borderRadius: [4, 4, 0, 0],
          },
        })),
        barWidth: '50%',
        label: {
          show: true,
          position: 'top',
          color: '#606266',
          fontSize: 12,
        },
      },
    ],
  })
}

/** 饼状图：订单状态分布 */
function renderPieChart(data: OrderStatusItem[]) {
  if (!pieChartRef.value) return
  if (!pieChart) {
    pieChart = echarts.init(pieChartRef.value)
  }

  const total = data.reduce((sum, d) => sum + d.count, 0)

  pieChart.setOption({
    tooltip: {
      trigger: 'item',
      backgroundColor: 'rgba(255,255,255,0.95)',
      borderColor: '#eee',
      formatter: (params: any) => {
        return `<strong>${params.name}</strong><br/>
                数量: <strong>${params.value}</strong> 单<br/>
                占比: <strong>${params.percent}%</strong>`
      },
    },
    legend: {
      orient: 'vertical',
      right: '5%',
      top: 'center',
      itemWidth: 10,
      itemHeight: 10,
      textStyle: { color: '#606266', fontSize: 12 },
    },
    series: [
      {
        type: 'pie',
        radius: ['40%', '65%'],
        center: ['35%', '50%'],
        avoidLabelOverlap: true,
        padAngle: 2,
        itemStyle: { borderRadius: 4 },
        label: {
          show: total > 0,
          formatter: '{b}: {d}%',
          color: '#606266',
          fontSize: 11,
        },
        labelLine: { length: 10, length2: 15 },
        emphasis: {
          label: { fontSize: 14, fontWeight: 'bold' },
          itemStyle: { shadowBlur: 10, shadowColor: 'rgba(0,0,0,0.15)' },
        },
        data: data.map((d) => ({
          name: d.statusName,
          value: d.count,
          itemStyle: { color: statusColorMap[d.status] || '#909399' },
        })),
      },
    ],
  })
}

/** 雷达图：平台健康评分 */
function renderRadarChart(data: RadarData) {
  if (!radarChartRef.value) return
  if (!radarChart) {
    radarChart = echarts.init(radarChartRef.value)
  }

  const labels = ['用户活跃度', '商品丰富度', '交易完成率', '好评率', '平台响应度']
  const values = [
    data.userActivity ?? 0,
    data.productRichness ?? 0,
    data.orderCompletionRate ?? 0,
    data.positiveRate ?? 0,
    data.responseSpeed ?? 0,
  ]

  const indicators = labels.map((name, i) => ({
    name,
    max: 100,
    color: colors[i],
  }))

  radarChart.setOption({
    tooltip: {
      backgroundColor: 'rgba(255,255,255,0.95)',
      borderColor: '#eee',
      formatter: (params: any) => {
        const rows = params.value
          .map((v: number, i: number) => `<span style="display:inline-block;width:10px;height:10px;border-radius:50%;background:${colors[i]};margin-right:6px"></span>${labels[i]}: <strong>${v}分</strong>`)
          .join('<br/>')
        return `<div style="font-weight:bold;margin-bottom:6px">平台健康评分</div>${rows}`
      },
    },
    radar: {
      indicator: indicators,
      radius: '65%',
      center: ['50%', '50%'],
      shape: 'circle',
      splitNumber: 4,
      axisName: {
        color: '#606266',
        fontSize: 12,
        borderRadius: 4,
        padding: [0, 0],
      },
      splitLine: {
        lineStyle: { color: '#e8e8e8' },
      },
      splitArea: {
        areaStyle: {
          color: ['rgba(64,158,255,0.02)', 'rgba(64,158,255,0.05)', 'rgba(64,158,255,0.08)', 'rgba(64,158,255,0.12)'],
        },
      },
      axisLine: {
        lineStyle: { color: '#e0e0e0' },
      },
    },
    series: [
      {
        type: 'radar',
        data: [
          {
            value: values,
            name: '平台健康',
            areaStyle: {
              color: 'rgba(64,158,255,0.25)',
            },
            lineStyle: {
              color: '#409EFF',
              width: 2,
            },
            itemStyle: {
              color: colors,
            },
          },
        ],
        symbol: 'circle',
        symbolSize: 6,
      },
    ],
  })
}

// ==================== 辅助函数 ====================

/** 金额格式化：保留两位小数，加上千位分隔符 */
function formatMoney(val: number): string {
  const num = Number(val)
  if (isNaN(num)) return '0.00'
  const parts = num.toFixed(2).split('.')
  parts[0] = parts[0].replace(/\B(?=(\d{3})+(?!\d))/g, ',')
  return parts.join('.')
}

// ==================== 生命周期 ====================

onMounted(async () => {
  await Promise.all([
    loadOverview(),
    loadTrends(),
    loadCategorySales(),
    loadOrderStatus(),
    loadRadar(),
  ])

  // 监听容器 resize 自适应
  await nextTick()
  const chartElements = [lineChartRef.value, barChartRef.value, pieChartRef.value, radarChartRef.value]
  const chartInstances = [lineChart!, barChart!, pieChart!, radarChart!]

  resizeObserver = new ResizeObserver(() => {
    chartInstances.forEach((chart) => {
      chart?.resize()
    })
  })

  chartElements.forEach((el) => {
    if (el) resizeObserver?.observe(el)
  })
})

onBeforeUnmount(() => {
  ;[lineChart, barChart, pieChart, radarChart].forEach((chart) => {
    chart?.dispose()
  })
  resizeObserver?.disconnect()
})
</script>

<style scoped lang="scss">
.dashboard-container {
  padding: 6px 10px;
}

.page-title {
  margin: 0 0 20px;
  font-size: 22px;
  font-weight: 600;
  color: #303133;
}

// ========== 统计卡片 ==========
.stats-row {
  margin-bottom: 20px;

  .stat-card {
    background: #fff;
    border-radius: 8px;
    padding: 18px 16px;

    transition: all 0.3s ease;
    position: relative;
    cursor: pointer;
    height: 105px;
    display: flex;
    align-items: center;
    box-sizing: border-box;

    &:hover {
      transform: translateY(-3px);
      box-shadow: 0 6px 20px rgba(0, 0, 0, 0.1);
    }

    .stat-icon {
      width: 52px;
      height: 52px;
      border-radius: 10px;
      display: flex;
      align-items: center;
      justify-content: center;
      flex-shrink: 0;
    }

    .stat-info {
      margin-left: 12px;
      flex: 1;
      min-width: 0;

      .stat-value {
        font-size: 22px;
        font-weight: 700;
        color: #303133;
        line-height: 1.3;
        white-space: nowrap;
        overflow: hidden;
        text-overflow: ellipsis;
        position: relative;
      }

      .stat-today-badge {
        display: inline-block;
        font-size: 11px;
        font-weight: 700;
        margin-left: 6px;
        vertical-align: super;
        line-height: 1;
      }

      .stat-label {
        font-size: 13px;
        color: #909399;
        margin-top: 2px;
      }
    }
  }
}

// ========== 图表卡片 ==========
.charts-row {
  .chart-col {
    margin-bottom: 20px;
  }

  .chart-card {
    background: #fff;
    border-radius: 10px;
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
    padding: 20px;
    transition: box-shadow 0.3s;

    &:hover {
      box-shadow: 0 6px 24px rgba(0, 0, 0, 0.1);
    }

    .chart-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      margin-bottom: 12px;

      h3 {
        margin: 0;
        font-size: 16px;
        font-weight: 600;
        color: #303133;
      }

      .trend-toggle {
        display: flex;
        background: rgb(240, 240, 240);
        padding: 4px;
        border-radius: 30px;

        .trend-item {
          padding: 4px 16px;
          cursor: pointer;
          border-radius: 20px;
          font-size: 13px;
          color: #606266;
          transition: all 0.2s;
          user-select: none;

          &.active {
            background: #fff;
            font-weight: 600;
            color: #303133;
          }

          &:not(.active):hover {
            color: #303133;
          }
        }
      }
    }

    .chart-box {
      width: 100%;
      height: 320px;
    }
  }
}

// ========== 响应式 ==========
@media (max-width: 768px) {
  .dashboard-container {
    padding: 12px;
  }

  .stats-row {
    .stat-card {
      padding: 14px 12px;
      height: 90px;

      .stat-icon {
        width: 44px;
        height: 44px;
      }

      .stat-info {
        .stat-value {
          font-size: 18px;
        }
      }
    }
  }

  .chart-card {
    .chart-box {
      height: 260px;
    }
  }
}
</style>
