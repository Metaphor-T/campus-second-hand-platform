<template>
  <div class="order-detail" :aria-busy="loading">
    <header class="detail-heading">
      <div><p class="eyebrow">交易记录</p><h1>订单详情</h1><p>查看交易信息、处理进度和下一步安排。</p></div>
      <div class="heading-actions"><RouterLink class="detail-button" :to="backLink">返回订单中心</RouterLink><button class="detail-button" type="button" :disabled="loading" @click="load">{{ loading ? '正在刷新…' : '刷新订单' }}</button></div>
    </header>
    <p v-if="loading" class="loading-note" role="status">正在读取订单信息…</p>
    <section v-if="error" class="detail-error" role="alert"><h2>{{ errorTitle }}</h2><p>{{ error }}</p><button v-if="retryable" type="button" class="detail-button" @click="load">重新加载</button></section>
    <template v-if="overview">
      <section class="next-step" :class="{ 'your-turn': overview.nextStep.yours }" aria-labelledby="next-step-title">
        <div class="status-line"><span class="status-tag" data-order-status>{{ overview.statusLabel }}</span><span>你是{{ overview.viewerRole === 'BUYER' ? '买家' : '卖家' }}</span><span v-if="overview.nextStep.responsibleRole !== 'NONE'" class="turn-tag">{{ overview.nextStep.yours ? '轮到你处理' : '等待对方处理' }}</span></div>
        <h2 id="next-step-title">{{ overview.nextStep.title }}</h2><p>{{ overview.nextStep.description }}</p>
        <RouterLink class="detail-button primary" :to="actionLink">{{ overview.nextStep.responsibleRole === 'NONE' ? '前往订单中心查看本单' : overview.nextStep.yours ? '前往订单中心处理本单' : '前往订单中心查看本单' }}</RouterLink>
      </section>
      <p class="simulation-note">本平台仅记录交易与协商信息，不接入真实支付，不提供资金担保或退款仲裁。</p>
      <div v-for="notice in overview.notices" :key="notice" class="history-notice" role="note">{{ notice }}</div>
      <div class="detail-layout">
        <div class="detail-main">
          <section class="detail-card" aria-labelledby="order-product-title">
            <h2 id="order-product-title">商品与金额</h2>
            <div class="product-summary">
              <img v-if="imageUrl && !imageFailed" :src="imageUrl" :alt="order.productTitle || '订单商品图片'" @error="imageFailed = true" />
              <div v-else class="product-image-fallback">暂无图片</div>
              <div><h3>{{ order.productTitle || '商品资料暂无记录' }}</h3><p>购买数量：{{ order.quantity ?? '暂无记录' }}</p><RouterLink v-if="order.productId && order.productTitle" class="text-link" :to="'/user-home/product-detail/' + order.productId">查看商品当前详情</RouterLink></div>
            </div>
            <dl class="detail-fields"><div><dt>下单单价</dt><dd>{{ money(order.unitPrice) }}</dd></div><div><dt>商品金额</dt><dd>{{ money(order.totalAmount) }}</dd></div><div><dt>运费记录</dt><dd>{{ money(order.shippingFee) }}</dd></div><div><dt>订单合计</dt><dd class="amount">{{ money(order.payAmount) }}</dd></div></dl>
            <p class="muted">金额按本订单保存的记录展示，不代表平台已实际收款。</p>
          </section>
          <section class="detail-card" aria-labelledby="order-info-title"><h2 id="order-info-title">交易信息</h2>
            <dl class="detail-fields"><div><dt>订单编号</dt><dd data-order-number>{{ order.orderNo || '暂无记录' }}</dd></div><div><dt>当前状态</dt><dd>{{ overview.statusLabel }}</dd></div><div><dt>买家</dt><dd>{{ order.buyerName || '用户资料暂无记录' }}{{ overview.viewerRole === 'BUYER' ? '（我）' : '' }}</dd></div><div><dt>卖家</dt><dd>{{ order.sellerName || '用户资料暂无记录' }}{{ overview.viewerRole === 'SELLER' ? '（我）' : '' }}</dd></div><div><dt>下单时间</dt><dd>{{ time(order.createTime) }}</dd></div><div><dt>最近更新时间</dt><dd>{{ time(order.updateTime) }}</dd></div><div><dt>付款方式记录</dt><dd>{{ paymentLabel }}</dd></div><div><dt>商品当前支持的交付方式</dt><dd>{{ deliveryLabel }}</dd></div></dl>
            <p class="muted">商品交付方式可能调整，历史交易的实际安排请按双方约定核对。</p>
          </section>
          <section class="detail-card" aria-labelledby="order-delivery-title"><h2 id="order-delivery-title">收货信息与备注</h2>
            <template v-if="hasAddress"><p class="address-contact">{{ order.receiverName || '收货人暂无记录' }} <span>{{ order.receiverPhone || '联系方式暂无记录' }}</span></p><p class="preserve-lines">{{ order.fullAddress }}</p></template>
            <p v-else class="muted">暂无可用收货地址，请与对方确认自提或其他交付安排。</p>
            <dl class="detail-notes"><div><dt>买家备注</dt><dd class="preserve-lines">{{ order.buyerRemark || '未填写备注' }}</dd></div><div v-if="order.orderStatus === 4 || order.cancelReason"><dt>取消或结束原因</dt><dd class="preserve-lines">{{ order.cancelReason || '暂无原因记录' }}</dd></div></dl>
          </section>
        </div>
        <section class="detail-card timeline-card" aria-labelledby="order-timeline-title">
          <h2 id="order-timeline-title">交易时间线</h2><p class="muted">按交易流程展示已保存的节点。缺少时间不代表该步骤一定没有发生，历史记录不会补写为当前时间。</p>
          <ol class="timeline">
            <li v-for="node in overview.timeline" :key="node.key" :data-timeline-node="node.key" :class="node.state.toLowerCase()">
              <span class="timeline-dot" aria-hidden="true"></span><div class="node-heading"><h3>{{ node.title }}</h3><span class="node-state">{{ nodeLabels[node.state] }}</span></div>
              <time v-if="node.time" :datetime="node.time.replace(' ', 'T') + '+08:00'">{{ time(node.time) }}</time><p v-else class="missing-time">{{ node.state === 'WAITING' ? '尚无该节点时间记录' : '暂无时间记录' }}</p>
              <p class="participant">{{ node.participant === '具体操作人未单独记录' ? node.participant : '相关方：' + node.participant }}</p><p class="node-description">{{ node.description }}</p>
            </li>
          </ol>
          <p class="muted">时间按北京时间展示。以上是订单节点记录，不是完整的操作日志。</p>
        </section>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { getOrderOverviewApi } from '@/api/order-api'
import type { OrderOverview } from '@/types/order'
import { productImageUrl } from '@/utils/product-experience'

const route = useRoute(), overview = ref<OrderOverview>(), loading = ref(false), error = ref(''), errorTitle = ref('暂时无法查看订单'), retryable = ref(false), imageFailed = ref(false)
const order = computed(() => overview.value!.order)
const imageUrl = computed(() => overview.value?.order.productImage ? productImageUrl(overview.value.order.productImage) : '')
const nodeLabels = { RECORDED: '已有时间记录', UNRECORDED: '时间未保存', UNKNOWN: '历史记录不完整', WAITING: '待处理节点' }
const money = (value?: number | null) => value === null || value === undefined || !Number.isFinite(Number(value)) ? '暂无金额记录' : '¥' + Number(value).toFixed(2)
const time = (value?: string | null) => value ? value.replace('T', ' ').slice(0, 19) : '暂无时间记录'
const paymentLabel = computed(() => ({ 1: '微信（模拟记录）', 2: '支付宝（模拟记录）', 3: '线下付款记录' }[order.value?.payMethod ?? 0] || '尚无方式记录'))
const deliveryLabel = computed(() => ({ 1: '自提', 2: '邮寄', 3: '自提或邮寄' }[order.value?.productTradeType ?? 0] || '暂无记录'))
const hasAddress = computed(() => !!order.value?.fullAddress?.trim())
const roleQuery = computed(() => overview.value?.viewerRole === 'SELLER' ? 'seller' : overview.value ? 'buyer' : route.query.role === 'seller' ? 'seller' : 'buyer')
const backLink = computed(() => ({ path: '/user-home/orders', query: { role: roleQuery.value } }))
const actionLink = computed(() => ({ path: '/user-home/orders', query: { role: roleQuery.value, ...(overview.value?.order.orderNo ? { keyword: overview.value.order.orderNo } : {}) } }))
let revision = 0, disposed = false
async function load() {
  const current = ++revision
  overview.value = undefined; error.value = ''; retryable.value = false; imageFailed.value = false
  const raw = String(route.params.id ?? ''), id = Number(raw)
  if (!/^[1-9]\d*$/.test(raw) || !Number.isSafeInteger(id) || id > 2147483647) { errorTitle.value = '订单编号不正确'; error.value = '请从订单中心重新选择订单。'; loading.value = false; return }
  loading.value = true
  try {
    const response = await getOrderOverviewApi(id)
    if (disposed || current !== revision) return
    if (!response.data) throw new Error('没有取得订单详情，请重新加载')
    overview.value = response.data
  } catch (e: any) {
    if (disposed || current !== revision) return
    const status = e?.response?.status || e?.code
    errorTitle.value = status === 403 ? '你无权查看这笔订单' : status === 404 ? '订单不存在或已不可用' : '暂时无法查看订单'
    error.value = status === 403 ? '只有这笔订单的买家和卖家可以查看详情。' : status === 404 ? '请返回订单中心核对订单记录。' : e?.response?.data?.message || e?.message || '请检查网络后重试。'
    retryable.value = status !== 403 && status !== 404
  } finally { if (!disposed && current === revision) loading.value = false }
}
watch(() => route.params.id, load, { immediate: true })
onBeforeUnmount(() => { disposed = true; revision++ })
</script>

<style scoped>
.order-detail { max-width:1200px; margin:0 auto; padding:28px 24px 48px; color:#30343b; container-type:inline-size; min-width:0; }
h1 { margin:0; font-size:2rem; line-height:1.5; } h2 { margin:0 0 14px; font-size:1.3rem; line-height:1.6; } h3 { margin:0; font-size:1.12rem; line-height:1.6; overflow-wrap:anywhere; }
p { line-height:1.7; } .eyebrow { margin:0 0 6px; color:#9f421c; font-weight:600; }
.detail-heading { display:flex; justify-content:space-between; align-items:center; flex-wrap:wrap; gap:16px; margin-bottom:24px; } .detail-heading p:last-child { margin-bottom:0; color:#60656d; }
.heading-actions,.status-line { display:flex; align-items:center; flex-wrap:wrap; gap:12px; }
.detail-button { display:inline-flex; justify-content:center; align-items:center; min-height:44px; padding:10px 16px; border:1px solid #c9c7c5; border-radius:10px; color:#49433e; background:#fff; font:inherit; line-height:1.5; text-align:center; text-decoration:none; cursor:pointer; }
.detail-button.primary { background:#9f421c; border-color:#9f421c; color:#fff; } .detail-button:disabled { opacity:.65; cursor:wait; }
.next-step { border:1px solid #e4d8ce; background:#fff7f0; padding:24px; border-radius:16px; } .next-step.your-turn { border-color:#ba7551; }
.status-line { margin-bottom:14px; line-height:1.6; } .status-tag,.turn-tag { border-radius:20px; padding:4px 12px; color:#853416; background:#f4e4d9; font-weight:600; } .turn-tag { background:#fff; border:1px solid #d9bba7; }
.simulation-note,.history-notice { padding:12px 16px; background:#f2f5f8; border-radius:10px; color:#53616f; } .history-notice { background:#fff3e7; color:#78451e; margin-bottom:14px; line-height:1.7; }
.detail-layout { display:grid; grid-template-columns:minmax(0,1.35fr) minmax(18rem,1fr); gap:20px; align-items:start; margin-top:22px; } .detail-main { display:grid; gap:20px; min-width:0; }
.detail-card { min-width:0; padding:24px; background:#fff; border:1px solid #e8e8ee; border-radius:16px; box-shadow:0 3px 16px #20263006; }
.product-summary { display:flex; align-items:center; gap:18px; margin-bottom:22px; min-width:0; } .product-summary > div:last-child { min-width:0; }
.product-summary img,.product-image-fallback { flex:none; width:96px; height:96px; object-fit:cover; border-radius:12px; } .product-image-fallback { display:grid; place-items:center; background:#f3f4f6; color:#60656d; font-size:.95rem; }
.product-summary p { margin:8px 0; } .text-link { color:#9f421c; text-underline-offset:3px; }
.detail-fields { display:grid; grid-template-columns:repeat(auto-fit,minmax(min(100%,12rem),1fr)); gap:20px 24px; margin:0; } .detail-fields > div { min-width:0; border-bottom:1px solid #eceef1; padding-bottom:12px; }
dt,.muted { color:#60656d; line-height:1.7; } dd { margin:6px 0 0; line-height:1.7; overflow-wrap:anywhere; } .amount { color:#9f421c; font-size:1.3rem; font-weight:700; }
.muted { font-size:1rem; margin:14px 0 0; } .address-contact { font-weight:600; display:flex; flex-wrap:wrap; gap:14px; } .preserve-lines { white-space:pre-wrap; overflow-wrap:anywhere; }
.detail-notes { margin:20px 0 0; } .detail-notes > div + div { margin-top:20px; }
.timeline { list-style:none; padding:0 0 0 18px; margin:24px 0; } .timeline li { position:relative; padding:0 0 26px 24px; border-left:2px solid #dfe3e8; min-width:0; } .timeline li:last-child { padding-bottom:0; border-color:transparent; }
.timeline-dot { position:absolute; left:-7px; top:7px; width:12px; height:12px; border-radius:50%; background:#c3c9d1; border:2px solid #fff; box-shadow:0 0 0 1px #aab2bd; }
.recorded .timeline-dot { background:#9f421c; box-shadow:0 0 0 1px #9f421c; } .unrecorded .timeline-dot { background:#fff; box-shadow:0 0 0 2px #ac7a43; }
.node-heading { display:flex; gap:8px; align-items:baseline; flex-wrap:wrap; } .node-state { font-size:.95rem; color:#60656d; line-height:1.6; } .timeline time { display:block; margin-top:6px; color:#49433e; line-height:1.7; overflow-wrap:anywhere; }
.timeline p { margin:6px 0 0; } .participant,.missing-time,.node-description { color:#60656d; font-size:1rem; } .loading-note { padding:20px; background:#f5f6f8; border-radius:12px; }
.detail-error { background:#fff3f2; border:1px solid #ddb0b2; color:#a1262c; padding:20px; border-radius:14px; } .detail-error p { overflow-wrap:anywhere; }
@container(max-width:64rem) { .detail-layout { grid-template-columns:minmax(0,1fr); } }
@media(max-width:600px) { .order-detail { padding:20px 12px 36px; } .detail-card,.next-step { padding:18px; } .product-summary { align-items:flex-start; gap:12px; } .product-summary img,.product-image-fallback { width:64px; height:64px; } }
</style>
