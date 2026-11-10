<template>
  <section class="verification-card" aria-labelledby="campus-verification-title" :aria-busy="busy">
    <div class="verification-heading"><h2 id="campus-verification-title">校园身份认证</h2><span class="verification-tag">{{ verification?.available ? labels[verification.status] : verification ? '暂未接入' : '正在查询' }}</span></div>
    <p>校园资料由本人填写，认证结果由学校认证服务核验。校园身份认证不等同于法律实名认证。</p>
    <p v-if="verification" class="verification-message" role="status">{{ verification.message }}</p>
    <dl v-if="verification?.requestedAt">
      <div><dt>申请学校</dt><dd>{{ verification.school }}</dd></div>
      <div><dt>申请学号</dt><dd>{{ verification.maskedStudentNumber }}</dd></div>
      <div><dt>申请时间</dt><dd>{{ formatTime(verification.requestedAt) }}</dd></div>
      <div v-if="verification.expiresAt"><dt>有效期至</dt><dd>{{ formatTime(verification.expiresAt) }}</dd></div>
    </dl>
    <p v-if="verification?.available && (!school || !studentNumber || editing)" class="verification-help">请先保存学校和学号，再申请认证。编辑中的草稿不会用于认证。</p>
    <label v-if="canApply" class="verification-consent"><input v-model="consent" type="checkbox" :disabled="busy" />我同意将已保存的学校、学号提供给接入的学校认证服务，仅用于本次校园身份核验。</label>
    <p class="verification-help">修改学校或学号后，原认证将失效；修改昵称、学院或联系方式不影响认证。</p>
    <p v-if="error" role="alert" class="verification-error">{{ error }}</p>
    <div class="verification-actions">
      <button type="button" :disabled="busy" @click="load">重新查询状态</button>
      <button v-if="verification?.available && verification.status === 'PENDING'" type="button" :disabled="busy || editing" @click="refresh">{{ busy ? '正在查询…' : '查询认证结果' }}</button>
      <button v-else-if="verification?.status !== 'VERIFIED' || !verification?.available" class="primary" type="button" :disabled="busy || !canApply || !consent" @click="apply">{{ verification?.available ? '申请校园身份认证' : '认证暂未开放' }}</button>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { getCampusVerificationApi, applyCampusVerificationApi, refreshCampusVerificationApi } from '@/api/campus-verification-api'
import type { CampusVerification, CampusVerificationStatus } from '@/types/campus-verification'
const props = defineProps<{ school?: string | null; studentNumber?: string | null; editing: boolean }>()
const verification = ref<CampusVerification>(), busy = ref(false), error = ref(''), consent = ref(false)
const labels: Record<CampusVerificationStatus, string> = { UNVERIFIED: '未认证', PENDING: '认证中', VERIFIED: '已认证', REJECTED: '未通过', INVALIDATED: '已失效', EXPIRED: '已过期' }
const canApply = computed(() => verification.value?.available && !['PENDING', 'VERIFIED'].includes(verification.value.status) && !!props.school && !!props.studentNumber && !props.editing)
const formatTime = (value: string) => value.replace('T', ' ')
let disposed = false, revision = 0
async function run(action: typeof getCampusVerificationApi) {
  const current = ++revision
  busy.value = true; error.value = ''
  try {
    const response = await action()
    if (!response.data) throw new Error('未取得认证状态')
    if (!disposed && revision === current) verification.value = response.data
  } catch (e: any) {
    if (!disposed && revision === current) error.value = e?.response?.data?.message || e?.message || '认证状态查询失败，请重试'
  } finally { if (!disposed && revision === current) busy.value = false }
}
const load = () => run(getCampusVerificationApi)
async function apply() { if (!canApply.value || !consent.value || busy.value) return; await run(applyCampusVerificationApi); consent.value = false }
async function refresh() { if (busy.value || props.editing) return; await run(refreshCampusVerificationApi) }
watch(() => [props.school, props.studentNumber], () => { consent.value = false; void load() })
onMounted(load)
onBeforeUnmount(() => { disposed = true; revision++ })
</script>

<style scoped>
.verification-card { min-width:0; border:1px solid #e8e8ee; border-radius:16px; background:#fff; box-shadow:0 3px 16px #20263006; padding:24px; }
.verification-heading { display:flex; justify-content:space-between; align-items:center; flex-wrap:wrap; gap:12px; }
h2 { margin:0; font-size:1.3rem; line-height:1.6; }
p, label, dt, dd { line-height:1.7; overflow-wrap:anywhere; }
.verification-tag { color:#536779; background:#f0f5f9; border-radius:20px; padding:4px 12px; }
.verification-message { background:#fff5ee; padding:14px; border-radius:10px; }
.verification-help { color:#60656d; }
dl { display:grid; grid-template-columns:repeat(2,minmax(0,1fr)); gap:16px; }
dl > div { min-width:0; } dt { color:#60656d; } dd { margin:4px 0 0; }
.verification-consent { display:flex; align-items:flex-start; gap:10px; }
input { flex:none; width:1.1rem; height:1.1rem; margin-top:.3rem; accent-color:#9f421c; }
.verification-actions { display:flex; flex-wrap:wrap; justify-content:flex-end; gap:12px; margin-top:20px; }
button { min-height:44px; padding:10px 16px; font:inherit; line-height:1.5; background:#fff; color:#49433e; border:1px solid #c9c7c5; border-radius:10px; cursor:pointer; }
button.primary { background:#9f421c; color:#fff; border-color:#9f421c; }
button:disabled { opacity:.65; cursor:not-allowed; }
.verification-error { color:#a1262c; background:#fff3f2; padding:12px; border-radius:10px; }
@container(max-width:36rem) { dl { grid-template-columns:minmax(0,1fr); } }
@media(max-width:600px) { .verification-card { padding:18px; } .verification-actions button { flex:1 1 10rem; } }
</style>
