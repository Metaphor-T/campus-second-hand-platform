<template>
  <div class="personal-center" :aria-busy="loading">
    <header class="profile-heading"><div><p class="eyebrow">我的账户</p><h1>个人中心</h1><p>管理个人资料，让校园交易中的联系更方便。</p></div><router-link class="profile-button" to="/user-home/market">返回首页</router-link></header>
    <p v-if="loading" role="status">正在加载个人资料…</p>
    <div v-if="loadError" class="profile-error" role="alert"><p>{{ loadError }}</p><button class="profile-button" type="button" @click="loadProfile">重新加载</button></div>
    <p v-if="notice" class="profile-notice" role="status">{{ notice }}</p>
    <div v-if="profile" class="profile-layout">
      <aside class="profile-summary" aria-label="账户概览">
        <img v-if="avatarUrl && !avatarFailed" class="profile-avatar" :src="avatarUrl" :alt="selectedAvatar ? '待保存的头像预览' : '当前头像'" @error="avatarFailed = true" />
        <div v-else class="profile-avatar avatar-fallback" aria-label="默认头像">{{ (profile.username || '用').slice(0, 1) }}</div>
        <h2>{{ profile.username }}</h2><span class="identity-tag">普通用户</span>
        <p class="summary-account">账号：{{ profile.account }}</p>
        <template v-if="editing">
          <button class="profile-button" type="button" :disabled="saving" @click="avatarInput?.click()">选择头像</button>
          <input ref="avatarInput" type="file" accept="image/png,image/jpeg" hidden @change="chooseAvatar" />
          <button v-if="selectedAvatar" class="profile-button secondary" type="button" :disabled="saving" @click="resetAvatar">恢复原头像</button>
          <p class="summary-help">JPG / PNG，不超过 2MB。<br/>选择后先预览，保存资料时才生效。</p>
        </template>
        <button v-else class="profile-button primary" type="button" @click="startEdit">编辑个人资料</button>
        <button class="profile-button" type="button" @click="openPassword">修改密码</button>
        <p class="summary-help">账号用于登录，昵称用于展示。账号不在个人中心修改。</p>
      </aside>

      <div class="profile-main">
        <form v-if="editing" class="profile-card profile-edit-form" novalidate @submit.prevent="saveProfile">
          <div class="profile-card-heading"><h2>编辑个人资料</h2><span>学校、学院、学号均为选填</span></div>
          <div class="profile-fields">
            <div class="profile-field"><label for="profile-account">登录账号</label><input id="profile-account" :value="profile.account" readonly aria-describedby="account-help" /><p id="account-help">账号用于登录，此处只读。</p></div>
            <div class="profile-field"><label for="profile-nickname">昵称 <span class="required-note">必填</span></label><input id="profile-nickname" v-model="draft.username" maxlength="20" autocomplete="nickname" :disabled="saving" required /><p>用于商品和聊天中的展示，最多 20 个字符。</p></div>
            <div class="profile-field"><label for="profile-phone">手机号</label><input id="profile-phone" v-model="draft.phone" type="tel" maxlength="11" autocomplete="tel" :disabled="saving" aria-describedby="phone-help" /><p id="phone-help">选填，仅作为联系方式，不代表已验证。</p></div>
            <div class="profile-field"><label for="profile-gender">性别</label><select id="profile-gender" v-model="draft.gender" :disabled="saving"><option :value="null">未设置</option><option :value="1">男</option><option :value="2">女</option></select></div>
          </div>
          <h3>校园资料</h3><p id="campus-help" class="campus-note">由本人填写，仅本人和管理员可查看；不对接学校系统，不代表学校身份认证。</p>
          <div class="profile-fields">
            <div class="profile-field"><label for="profile-school">学校</label><input id="profile-school" v-model="draft.school" maxlength="100" :disabled="saving" aria-describedby="campus-help" placeholder="例如：某某大学" /></div>
            <div class="profile-field"><label for="profile-college">学院</label><input id="profile-college" v-model="draft.college" maxlength="100" :disabled="saving" aria-describedby="campus-help" placeholder="例如：计算机学院" /></div>
            <div class="profile-field"><label for="profile-student-number">学号</label><input id="profile-student-number" v-model="draft.studentNumber" maxlength="32" :disabled="saving" aria-describedby="student-help" autocomplete="off" /><p id="student-help">最多 32 位，支持字母、数字、横线和下划线。</p></div>
          </div>
          <p v-if="saveError" ref="saveErrorElement" class="profile-error" role="alert" tabindex="-1">{{ saveError }}</p>
          <div class="profile-actions"><button class="profile-button" type="button" :disabled="saving" @click="cancelEdit">取消修改</button><button class="profile-button primary" type="submit" :disabled="saving">{{ saving ? '正在保存…' : '保存资料' }}</button></div>
        </form>
        <template v-else>
          <section class="profile-card" aria-labelledby="basic-profile-title"><div class="profile-card-heading"><h2 id="basic-profile-title">基本资料</h2><button class="profile-button" type="button" @click="startEdit">编辑资料</button></div>
            <dl class="profile-details"><div><dt>登录账号</dt><dd>{{ profile.account }}</dd></div><div><dt>昵称</dt><dd>{{ profile.username }}</dd></div><div><dt>手机号</dt><dd>{{ profile.phone || '未填写' }}</dd></div><div><dt>性别</dt><dd>{{ profile.gender === 1 ? '男' : profile.gender === 2 ? '女' : '未设置' }}</dd></div><div><dt>注册时间</dt><dd>{{ profile.createTime || '暂无记录' }}</dd></div><div><dt>账户身份</dt><dd>普通用户</dd></div></dl>
            <p class="profile-footnote">账号用于登录，昵称用于展示；手机号仅用于联系，不代表已验证。</p>
          </section>
          <section class="profile-card" aria-labelledby="campus-profile-title"><div class="profile-card-heading"><h2 id="campus-profile-title">校园资料</h2><span class="self-reported-tag">本人填写</span></div>
            <dl class="profile-details"><div><dt>学校</dt><dd data-profile="school">{{ profile.school || '未填写' }}</dd></div><div><dt>学院</dt><dd data-profile="college">{{ profile.college || '未填写' }}</dd></div><div><dt>学号</dt><dd data-profile="student-number">{{ profile.studentNumber || '未填写' }}</dd></div></dl>
            <p class="profile-footnote">校园资料仅本人和管理员可查看；内容由本人填写，不代表已通过学校认证。</p>
          </section>
        </template>

        <CampusVerificationCard :school="profile.school" :student-number="profile.studentNumber" :editing="editing" />
        <section id="password" ref="passwordSection" class="profile-card" aria-labelledby="profile-password-title" tabindex="-1">
          <div class="profile-card-heading"><h2 id="profile-password-title">密码管理</h2><button v-if="!showPassword" class="profile-button" type="button" @click="openPassword">修改登录密码</button></div>
          <p class="profile-footnote">修改时需确认原密码。成功后退出当前登录，请使用新密码重新登录。</p>
          <form v-if="showPassword" class="password-form" novalidate @submit.prevent="savePassword">
            <div class="profile-field"><label for="profile-old-password">原密码</label><input id="profile-old-password" v-model="password.oldPassword" type="password" autocomplete="current-password" maxlength="64" :disabled="passwordSaving" /></div>
            <div class="profile-field"><label for="profile-new-password">新密码</label><input id="profile-new-password" v-model="password.newPassword" type="password" autocomplete="new-password" minlength="8" maxlength="64" :disabled="passwordSaving" aria-describedby="new-password-help" /><p id="new-password-help">8—64 位，请勿与原密码相同。</p></div>
            <div class="profile-field"><label for="profile-confirm-password">确认新密码</label><input id="profile-confirm-password" v-model="password.confirmPassword" type="password" autocomplete="new-password" maxlength="64" :disabled="passwordSaving" /></div>
            <p v-if="passwordError" class="profile-error" role="alert">{{ passwordError }}</p>
            <div class="profile-actions"><button class="profile-button" type="button" :disabled="passwordSaving" @click="closePassword">取消</button><button class="profile-button primary" type="submit" :disabled="passwordSaving">{{ passwordSaving ? '正在修改…' : '确认修改密码' }}</button></div>
          </form>
        </section>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, inject, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getMyProfileApi, saveMyProfileApi, changeMyPasswordApi } from '@/api/user-api'
import type { BaseUserVO, UserProfile, UserProfileUpdate, UserProfilePassword } from '@/types/user'
import { clearToken, clearRole, clearLastVisitedPath } from '@/utils/localStorage'
import wsService from '@/utils/websocket'
import CampusVerificationCard from '@/components/CampusVerificationCard.vue'

const route = useRoute(), router = useRouter()
const updateCurrentUser = inject<(profile: BaseUserVO) => void>('update-current-user', () => {})
const profile = ref<UserProfile | null>(null), loading = ref(false), editing = ref(false), saving = ref(false)
const loadError = ref(''), saveError = ref(''), notice = ref('')
const saveErrorElement = ref<HTMLElement>(), avatarInput = ref<HTMLInputElement>(), passwordSection = ref<HTMLElement>()
const selectedAvatar = ref<File>(), objectUrl = ref(''), avatarFailed = ref(false)
const draft = reactive<UserProfileUpdate>({ username: '', phone: '', gender: null, school: '', college: '', studentNumber: '' })
const avatarUrl = computed(() => objectUrl.value || (profile.value?.avatar?.startsWith('/files/') ? `${import.meta.env.VITE_API_BASE_URL}${profile.value.avatar}` : ''))
watch(avatarUrl, () => { avatarFailed.value = false })
const message = (error: any) => error?.response?.data?.message || error?.message || '暂时无法处理，请稍后重试'
let disposed = false
function resetAvatar() { if (objectUrl.value) URL.revokeObjectURL(objectUrl.value); objectUrl.value = ''; selectedAvatar.value = undefined; if (avatarInput.value) avatarInput.value.value = '' }
function resetDraft() {
  if (!profile.value) return
  Object.assign(draft, { username: profile.value.username || '', phone: profile.value.phone || '', gender: profile.value.gender ?? null, school: profile.value.school || '', college: profile.value.college || '', studentNumber: profile.value.studentNumber || '' })
}
async function loadProfile() {
  loading.value = true; loadError.value = ''
  try {
    const response = await getMyProfileApi()
    if (disposed) return
    if (!response.data) throw new Error('未取得个人资料')
    profile.value = response.data; updateCurrentUser(response.data); resetDraft()
    if (route.hash === '#password') { showPassword.value = true; await nextTick(); passwordSection.value?.scrollIntoView({ block: 'start' }) }
  } catch (error) { if (!disposed) loadError.value = message(error) }
  finally { if (!disposed) loading.value = false }
}
function startEdit() { resetDraft(); resetAvatar(); saveError.value = ''; notice.value = ''; editing.value = true }
function cancelEdit() { resetDraft(); resetAvatar(); editing.value = false; saveError.value = ''; notice.value = '已取消修改，原资料和头像保持不变。' }
function chooseAvatar(event: Event) {
  const file = (event.target as HTMLInputElement).files?.[0]
  if (!file) return
  if (!['image/png', 'image/jpeg'].includes(file.type) || !/\.(png|jpe?g)$/i.test(file.name)) { saveError.value = '头像仅支持 JPG / PNG 图片'; return }
  if (file.size > 2 * 1024 * 1024 || file.size === 0) { saveError.value = '请选择不超过 2MB 的有效头像'; return }
  resetAvatar(); selectedAvatar.value = file; objectUrl.value = URL.createObjectURL(file); saveError.value = ''
}
async function saveProfile() {
  saveError.value = ''; notice.value = ''
  const data = { ...draft, username: draft.username.trim(), phone: draft.phone.trim(), school: draft.school.trim(), college: draft.college.trim(), studentNumber: draft.studentNumber.trim() }
  if (!data.username) saveError.value = '昵称不能为空'
  else if (data.phone && !/^1[0-9]{10}$/.test(data.phone)) saveError.value = '手机号请填写11位数字，以1开头，或留空'
  else if (data.studentNumber && !/^[A-Za-z0-9_-]{1,32}$/.test(data.studentNumber)) saveError.value = '学号最多32位，仅支持字母、数字、横线和下划线'
  if (saveError.value) { await nextTick(); saveErrorElement.value?.focus(); return }
  saving.value = true
  try {
    const response = await saveMyProfileApi(data, selectedAvatar.value)
    if (!response.data) throw new Error('保存响应缺少个人资料，请重新加载核对')
    profile.value = response.data; updateCurrentUser(response.data); resetAvatar(); resetDraft(); editing.value = false; notice.value = '个人资料已保存。'
  } catch (error) { saveError.value = message(error) }
  finally { saving.value = false }
}
const showPassword = ref(route.hash === '#password'), passwordSaving = ref(false), passwordError = ref('')
const password = reactive<UserProfilePassword>({ oldPassword: '', newPassword: '', confirmPassword: '' })
function resetPassword() { Object.assign(password, { oldPassword: '', newPassword: '', confirmPassword: '' }); passwordError.value = '' }
async function openPassword() {
  showPassword.value = true; await router.replace({ hash: '#password' }); await nextTick()
  passwordSection.value?.scrollIntoView({ block: 'start' }); passwordSection.value?.focus({ preventScroll: true })
}
function closePassword() { resetPassword(); showPassword.value = false; void router.replace({ hash: '' }) }
watch(() => route.hash, async hash => { if (hash === '#password') { showPassword.value = true; await nextTick(); passwordSection.value?.scrollIntoView({ block: 'start' }) } })
async function savePassword() {
  passwordError.value = ''
  if (!password.oldPassword) passwordError.value = '请输入原密码'
  else if (password.newPassword.length < 8 || password.newPassword.length > 64 || !password.newPassword.trim()) passwordError.value = '新密码长度为8至64位'
  else if (password.newPassword !== password.confirmPassword) passwordError.value = '两次新密码不一致'
  else if (password.oldPassword === password.newPassword) passwordError.value = '新密码不能与原密码相同'
  if (passwordError.value) return
  passwordSaving.value = true
  try {
    await changeMyPasswordApi({ ...password }); resetPassword(); wsService.disconnect(); clearToken(); clearRole(); clearLastVisitedPath()
    ElMessage.success('密码修改成功，请使用新密码登录'); await router.replace('/login')
  } catch (error) { passwordError.value = message(error) }
  finally { passwordSaving.value = false }
}
onMounted(loadProfile)
onBeforeUnmount(() => { disposed = true; resetAvatar(); resetPassword() })
</script>

<style scoped>
.personal-center { max-width:1200px; margin:0 auto; padding:28px 24px 48px; color:#30343b; min-width:0; }
h1 { margin:0; font-size:2rem; line-height:1.5; } h2 { margin:0; font-size:1.3rem; line-height:1.6; } h3 { margin:28px 0 12px; font-size:1.15rem; }
p { line-height:1.7; } .eyebrow { margin:0 0 6px; font-weight:600; color:#9f421c; }
.profile-heading { display:flex; justify-content:space-between; align-items:center; gap:16px; flex-wrap:wrap; margin-bottom:24px; }
.profile-heading p:last-child { color:#60656d; margin-bottom:0; }
.profile-layout { display:grid; grid-template-columns:minmax(14rem,18rem) minmax(0,1fr); align-items:start; gap:24px; }
.profile-summary,.profile-card { min-width:0; border:1px solid #e8e8ee; border-radius:16px; background:#fff; box-shadow:0 3px 16px #20263006; padding:24px; }
.profile-summary { display:flex; flex-direction:column; align-items:center; text-align:center; gap:12px; background:linear-gradient(180deg,#fff5ee 0,#fff 180px); }
.profile-summary h2 { overflow-wrap:anywhere; }
.profile-avatar { width:112px; height:112px; object-fit:cover; border-radius:50%; border:4px solid #fff; box-shadow:0 2px 12px #20263014; }
.avatar-fallback { display:grid; place-items:center; background:#f4e4d9; color:#853416; font-size:2rem; box-sizing:content-box; }
.identity-tag,.self-reported-tag { color:#853416; background:#fff1e9; padding:4px 12px; border-radius:20px; font-size:1rem; line-height:1.6; }
.self-reported-tag { color:#536779; background:#f0f5f9; }
.summary-account { margin:0; overflow-wrap:anywhere; }
.summary-help,.profile-footnote,.campus-note { color:#60656d; font-size:1rem; margin:8px 0 0; }
.profile-summary .profile-button { width:100%; }
.profile-button { display:inline-flex; justify-content:center; align-items:center; box-sizing:border-box; min-height:44px; padding:10px 16px; border:1px solid #c9c7c5; border-radius:10px; font:inherit; line-height:1.5; color:#49433e; background:#fff; text-decoration:none; cursor:pointer; text-align:center; overflow-wrap:anywhere; }
.profile-button.primary { color:#fff; background:#9f421c; border-color:#9f421c; }
.profile-button:hover { border-color:#9f421c; } .profile-button:disabled { opacity:.65; cursor:wait; }
.profile-button.secondary { border-color:transparent; background:transparent; }
.profile-main { min-width:0; display:grid; gap:20px; container-type:inline-size; }
.profile-card-heading { display:flex; justify-content:space-between; flex-wrap:wrap; align-items:center; gap:12px; margin-bottom:20px; }
.profile-card-heading > span { color:#60656d; line-height:1.6; }
.profile-details,.profile-fields { display:grid; grid-template-columns:repeat(2,minmax(0,1fr)); gap:22px 24px; margin:0; }
.profile-details > div { min-width:0; padding-bottom:14px; border-bottom:1px solid #eceef1; }
.profile-details dt { color:#60656d; font-size:1rem; line-height:1.6; }
.profile-details dd { margin:6px 0 0; font-weight:500; line-height:1.7; overflow-wrap:anywhere; }
.profile-field { min-width:0; display:flex; flex-direction:column; gap:8px; }
.profile-field label { font-weight:600; line-height:1.6; }
.profile-field input,.profile-field select { width:100%; min-width:0; box-sizing:border-box; min-height:44px; height:auto; padding:10px 12px; border:1px solid #b8bec7; border-radius:9px; font:inherit; line-height:1.6; color:#30343b; background:#fff; }
.profile-field input[readonly] { background:#f5f6f8; color:#60656d; }
.profile-field input::placeholder { color:#686d75; }
.profile-field p { margin:0; color:#60656d; font-size:1rem; }
.required-note { font-size:.9rem; font-weight:400; color:#9f421c; }
.campus-note { padding:14px; border-radius:10px; background:#f5f8fb; margin:0 0 20px; }
.profile-actions { display:flex; justify-content:flex-end; flex-wrap:wrap; gap:12px; margin-top:24px; padding-top:20px; border-top:1px solid #eceef1; }
.profile-error { color:#a1262c; padding:12px 16px; background:#fff3f2; border:1px solid #ddb0b2; border-radius:10px; line-height:1.7; overflow-wrap:anywhere; }
.profile-notice { color:#286b35; background:#f0f8ed; padding:12px 16px; border-radius:10px; line-height:1.7; }
.password-form { display:grid; gap:20px; margin-top:20px; }
#password { scroll-margin-top:12px; }
@container (max-width:36rem) { .profile-fields,.profile-details { grid-template-columns:minmax(0,1fr); } }
@media(max-width:900px) { .profile-layout { grid-template-columns:minmax(0,1fr); } }
@media(max-width:600px) { .personal-center { padding:20px 12px 36px; } .profile-card,.profile-summary { padding:18px; } .profile-actions .profile-button { flex:1 1 8rem; } }
</style>
