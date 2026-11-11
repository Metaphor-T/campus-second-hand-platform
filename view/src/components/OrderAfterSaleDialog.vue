<template>
  <el-dialog :model-value="modelValue" title="订单售后" width="min(760px, 94vw)" class="after-sale-dialog" :close-on-click-modal="false" :before-close="close" @update:model-value="(value: boolean) => !value && close()">
    <div class="after-sale-body" v-loading="loading" :aria-busy="loading || busy">
      <el-alert v-if="error" :title="error" type="error" :closable="false" role="alert" class="after-sale-error" />
      <el-button v-if="!context && !loading" @click="load">重新加载</el-button>
      <template v-if="context">
        <p class="eligibility-note">{{ context.eligibilityMessage }}</p>
        <p v-if="context.deadline">普通售后截止：<strong>{{ context.deadline }}</strong>（北京时间）</p>
        <p v-else-if="context.orderStatus === 3 || context.orderStatus === 5" class="history-note">历史完成时间不完整，不推算售后期限，请管理员核对。</p>

        <section v-for="r in context.requests" :key="r.id" class="after-sale-record">
          <div class="record-heading"><h3>申请 #{{ r.id }}</h3><el-tag>{{ afterSaleLabel(r.state) }}</el-tag></div>
          <p>{{ requestType(r.requestType) }} · {{ r.createdAt || '原申请时间未记录' }} · 申请人 #{{ r.applicantId ?? '未记录' }}</p>
          <p class="preserve-lines"><strong>申请原因：</strong>{{ r.reason }}</p>
          <div v-if="evidence(r).length" class="evidence-images"><el-image v-for="path in evidence(r)" :key="path" :src="productImageUrl(path)" :preview-src-list="evidence(r).map(productImageUrl)" preview-teleported fit="cover" alt="售后申请凭证" /></div>
          <p>申请方案：{{ r.returnRequired ? '退货退款（需退回商品）' : '仅退款（无需退回商品）' }}</p>
          <p v-if="r.sellerRespondedAt" class="preserve-lines"><strong>卖家{{ r.sellerAgree ? '同意申请' : '拒绝申请' }}：</strong>{{ r.sellerNote }}<br />{{ r.sellerRespondedAt }} · 卖家 #{{ r.sellerId }}</p>
          <p v-if="r.adminReviewedAt" class="preserve-lines"><strong>管理员审核：</strong>{{ r.adminNote }}<br />{{ r.adminReviewedAt }} · 管理员 #{{ r.adminId }}</p>
          <p v-if="r.returnConfirmedAt" class="preserve-lines"><strong>退回商品确认：</strong>{{ r.returnNote }}<br />{{ r.returnConfirmedAt }} · 确认人 #{{ r.returnConfirmedBy }} · 收回 {{ r.returnQuantity }} 件</p>
          <p v-if="inventoryResult(r)" class="inventory-result">{{ inventoryResult(r) }}</p>
          <el-button v-if="context.viewerRole === 'BUYER' && ['WAIT_SELLER','WAIT_ADMIN'].includes(r.state) && r.requestType !== 'LEGACY'" :disabled="busy" @click="act(() => withdrawAfterSaleApi(r.id), '申请已撤回')">撤回申请</el-button>
        </section>

        <section v-if="canApply" class="after-sale-form">
          <h3>{{ context.eligibility === 'ADMIN_ONLY' ? '申请管理员介入' : '提交售后申请' }}</h3>
          <el-form label-position="top">
            <el-form-item label="申请方式"><el-radio-group v-model="requestTypeValue"><el-radio value="REGULAR" :disabled="context.eligibility === 'ADMIN_ONLY'">普通售后（先由卖家回应）</el-radio><el-radio value="ADMIN_INTERVENTION">管理员介入</el-radio></el-radio-group></el-form-item>
            <el-form-item label="申请原因（必填）"><el-input v-model="reason" type="textarea" :rows="3" maxlength="1000" show-word-limit aria-label="售后申请原因" /></el-form-item>
            <el-form-item label="申请方案"><el-radio-group v-model="returnRequired"><el-radio :value="true">退款并退回商品</el-radio><el-radio :value="false">仅退款，无需退回商品</el-radio></el-radio-group></el-form-item>
            <el-form-item label="凭证图片（选填，最多3张，每张不超过2MB）"><input type="file" accept="image/jpeg,image/png" aria-label="添加售后凭证" :disabled="busy || uploading || images.length >= 3" @change="upload" /><div class="evidence-images"><div v-for="(path,index) in images" :key="path"><img :src="productImageUrl(path)" alt="待提交的售后凭证" /><el-button size="small" :disabled="busy" @click="images.splice(index,1)">移除</el-button></div></div></el-form-item>
          </el-form>
          <el-button type="primary" :loading="busy" :disabled="uploading" @click="apply">提交申请</el-button>
        </section>

        <section v-if="latest && (latest.state === 'WAIT_SELLER' || (latest.state === 'WAIT_ADMIN' && !latest.sellerRespondedAt)) && context.viewerRole === 'SELLER'" class="after-sale-form">
          <h3>卖家回应</h3><p>同意或拒绝都会提交管理员审核，不会直接完成退款。</p>
          <label for="seller-after-sale-note">处理说明（必填，拒绝时请说明原因）</label><el-input id="seller-after-sale-note" v-model="sellerNote" type="textarea" :rows="3" maxlength="1000" />
          <div class="form-actions"><el-button type="primary" :loading="busy" @click="sellerRespond(true)">同意申请</el-button><el-button type="danger" :disabled="busy" @click="sellerRespond(false)">拒绝申请</el-button></div>
        </section>

        <section v-if="latest?.state === 'WAIT_ADMIN' && context.viewerRole === 'ADMIN'" class="after-sale-form">
          <h3>管理员审核</h3>
          <label for="admin-after-sale-note">审核说明（必填）</label><el-input id="admin-after-sale-note" v-model="adminNote" type="textarea" :rows="3" maxlength="1000" />
          <el-checkbox v-model="reviewReturnRequired">审核通过后需要退回商品</el-checkbox>
          <template v-if="latest.requestType === 'LEGACY' && latest.originalOrderStatus == null"><p>驳回历史申请前，请核对原交易阶段；不会据此补写历史时间。</p><el-radio-group v-model="legacyStatus"><el-radio :value="2">待收货</el-radio><el-radio :value="3">已完成</el-radio></el-radio-group></template>
          <div class="form-actions"><el-button type="primary" :loading="busy" @click="review(true)">审核通过</el-button><el-button type="danger" :disabled="busy" @click="review(false)">审核驳回</el-button></div>
        </section>

        <section v-if="latest?.state === 'WAIT_RETURN' && ['SELLER','ADMIN'].includes(context.viewerRole)" class="after-sale-form">
          <h3>确认收到退回商品</h3><el-checkbox v-model="returnChecked">已实际收到本单退回商品（{{ context.quantity }} 件）</el-checkbox>
          <label for="return-after-sale-note">收回确认说明（必填）</label><el-input id="return-after-sale-note" v-model="returnNote" type="textarea" :rows="2" maxlength="1000" />
          <div class="form-actions"><el-button type="primary" :loading="busy" @click="confirmReturn">确认收回并完成售后</el-button></div>
        </section>
      </template>
    </div>
    <template #footer><el-button :disabled="busy || uploading" @click="close">关闭</el-button></template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import type { AfterSaleContext, AfterSaleRequest } from '@/types/after-sale'
import { getAfterSaleApi, applyAfterSaleApi, respondAfterSaleApi, reviewAfterSaleApi, confirmAfterSaleReturnApi, withdrawAfterSaleApi } from '@/api/after-sale-api'
import { uploadProductImageApi } from '@/api/product-api'
import { productErrorMessage, productImageUrl, validateProductImage } from '@/utils/product-experience'
import { afterSaleLabel, inventoryResult } from '@/utils/after-sale'
const props = defineProps<{ modelValue: boolean; orderId?: number }>()
const emit = defineEmits<{ 'update:modelValue': [value: boolean]; changed: [] }>()
const context = ref<AfterSaleContext>(), loading = ref(false), busy = ref(false), uploading = ref(false), error = ref('')
const reason = ref(''), requestTypeValue = ref('REGULAR'), returnRequired = ref(true), images = ref<string[]>([])
const sellerNote = ref(''), adminNote = ref(''), returnNote = ref(''), reviewReturnRequired = ref(true), legacyStatus = ref<number>(), returnChecked = ref(false)
const latest = computed(() => context.value?.requests[0])
const canApply = computed(() => context.value?.viewerRole === 'BUYER' && ['REGULAR','ADMIN_ONLY'].includes(context.value.eligibility))
let generation = 0
const requestType = (value: string) => ({ REGULAR: '普通售后', ADMIN_INTERVENTION: '管理员介入', LEGACY: '历史售后核对' }[value] || '售后申请')
const evidence = (r: AfterSaleRequest): string[] => { try { const v = JSON.parse(r.evidenceJson || '[]'); return Array.isArray(v) ? v.filter(p => typeof p === 'string' && /^\/files\//.test(p)) : [] } catch { return [] } }
async function load() {
  if (!props.orderId) return
  const run = ++generation; loading.value = true; error.value = ''
  try { const res = await getAfterSaleApi(props.orderId); if (run !== generation || !props.modelValue) return; context.value = res.data; if (!res.data) throw new Error('售后记录暂不可用'); requestTypeValue.value = res.data.eligibility === 'ADMIN_ONLY' ? 'ADMIN_INTERVENTION' : 'REGULAR'; reviewReturnRequired.value = latest.value?.returnRequired ?? true }
  catch (e) { if (run === generation) error.value = productErrorMessage(e, '读取售后记录失败，请重试') }
  finally { if (run === generation) loading.value = false }
}
function close() { if (busy.value || uploading.value) return; generation++; emit('update:modelValue',false) }
function validNote(value: string, label: string) { if (!value.trim()) { error.value = `请填写${label}`; return false } return true }
async function act(action: () => Promise<unknown>, success: string) {
  if (busy.value || loading.value || uploading.value) return
  busy.value = true; error.value = ''
  try { await action(); ElMessage.success(success); await load(); emit('changed') }
  catch (e) { error.value = productErrorMessage(e, '操作失败，请重试；填写内容已保留') }
  finally { busy.value = false }
}
function apply() { if (!props.orderId || !validNote(reason.value,'售后申请原因')) return; act(() => applyAfterSaleApi(props.orderId!, { requestType: requestTypeValue.value, reason: reason.value.trim(), returnRequired: returnRequired.value, evidence: images.value }), '售后申请已提交') }
function sellerRespond(agree: boolean) { if (!latest.value || !validNote(sellerNote.value,'处理说明')) return; act(() => respondAfterSaleApi(latest.value!.id, agree, sellerNote.value.trim()), '卖家意见已提交，等待管理员审核') }
function review(approved: boolean) { if (!latest.value || !validNote(adminNote.value,'审核说明')) return; if (!approved && latest.value.requestType === 'LEGACY' && latest.value.originalOrderStatus == null && !legacyStatus.value) { error.value = '请先核对并选择历史订单原交易阶段'; return } act(() => reviewAfterSaleApi(latest.value!.id, approved, adminNote.value.trim(), reviewReturnRequired.value, legacyStatus.value), approved ? '审核已通过' : '审核结果已保存') }
function confirmReturn() { if (!latest.value) return; if (!returnChecked.value) { error.value = '请先确认已实际收到退回商品'; return } if (!validNote(returnNote.value,'收回确认说明')) return; act(() => confirmAfterSaleReturnApi(latest.value!.id, returnNote.value.trim()), '已确认收回商品，售后处理完成') }
async function upload(event: Event) {
  const input = event.target as HTMLInputElement, file = input.files?.[0]; input.value = ''; if (!file) return
  error.value = ''; const problem = images.value.length >= 3 ? '凭证图片最多3张' : validateProductImage(file, images.value.length)
  if (problem) { error.value = problem; return }
  uploading.value = true
  try { const res = await uploadProductImageApi(file, { silentError: true }); if (!res.data?.filePath) throw new Error('图片上传失败，请重试'); images.value.push(res.data.filePath) }
  catch (e) { error.value = productErrorMessage(e, '图片上传失败，请重试') }
  finally { uploading.value = false }
}
watch(() => [props.modelValue, props.orderId], () => { if (props.modelValue) { context.value = undefined; reason.value = ''; sellerNote.value = ''; adminNote.value = ''; returnNote.value = ''; images.value = []; legacyStatus.value = undefined; returnChecked.value = false; load() } else generation++ }, { immediate: true })
</script>

<style scoped lang="scss">
.after-sale-body { max-height:65dvh; overflow:auto; overflow-wrap:anywhere; padding:2px 6px; font-size:1rem; line-height:1.6; }
.after-sale-error { margin-bottom:16px; }
.after-sale-record,.after-sale-form { border:1px solid #e5e7eb; border-radius:12px; padding:16px; margin-top:16px; }
.record-heading,.form-actions { display:flex; gap:12px; flex-wrap:wrap; align-items:center; }
.record-heading { justify-content:space-between; }
h3 { margin:0 0 12px; font-size:1.142857rem; } p,label { line-height:1.7; } label { display:block; }
.form-actions { margin-top:16px; } .form-actions .el-button { margin-left:0; }
.preserve-lines { white-space:pre-wrap; } .history-note,.inventory-result { color:#853416; }
.evidence-images { display:flex; flex-wrap:wrap; gap:12px; margin-top:12px; max-width:100%; }
.evidence-images :is(img,.el-image) { width:80px; height:80px; object-fit:cover; border-radius:8px; }
.evidence-images > div { display:flex; flex-direction:column; gap:6px; }
input[type=file] { max-width:100%; font:inherit; }
:deep(.el-radio-group) { display:flex; flex-wrap:wrap; gap:10px; } :deep(.el-radio),:deep(.el-checkbox) { height:auto; margin:6px 0; white-space:normal; align-items:flex-start; }
:deep(.el-radio__label),:deep(.el-checkbox__label) { white-space:normal; line-height:1.5; }
:deep(.el-form-item__label),:deep(.el-radio__label),:deep(.el-checkbox__label),:deep(.el-textarea__inner),:deep(.el-alert__title) { font-size:1rem; line-height:1.6; }
:deep(.el-tag) { height:auto; white-space:normal; font-size:0.928571rem; line-height:1.6; padding:4px 8px; }
@media(max-width:480px) { .after-sale-record,.after-sale-form { padding:12px; } }
</style>
