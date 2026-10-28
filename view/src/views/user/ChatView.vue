<template>
  <div class="chat-container">
    <!-- 左侧会话列表 -->
    <div class="conversation-sidebar">
      <div class="sidebar-header">
        <h3>消息</h3>
      </div>
      <div class="conversation-list" v-if="conversations.length > 0">
        <div
          v-for="conv in conversations"
          :key="conv.userId"
          class="conversation-item"
          :class="{ active: activeConversation?.userId === conv.userId }"
          @click="switchConversation(conv)"
        >
          <el-avatar :size="44" v-if="conv.avatar" :src="getImageUrl(conv.avatar)" />
          <el-avatar :size="44" v-else>{{ (conv?.username ?? '?')[0] }}</el-avatar>
          <div class="conv-info">
            <div class="conv-top">
              <span class="conv-name">{{ conv.username || '未知用户' }}</span>
              <span class="conv-time">{{ formatTime(conv.lastTime) }}</span>
            </div>
            <div class="conv-bottom">
              <span class="conv-preview">{{ getPreviewText(conv) }}</span>
              <el-badge :value="conv.unreadCount" :hidden="!conv.unreadCount || conv.unreadCount <= 0" class="unread-badge" />
            </div>
          </div>
        </div>
      </div>
      <div v-else class="empty-conversations">
        <el-icon :size="48" color="#ccc"><ChatLineSquare /></el-icon>
        <p>暂无会话</p>
      </div>
    </div>

    <!-- 右侧聊天区域 -->
    <div class="chat-main" v-if="activeConversation">
      <!-- 聊天头部 -->
      <div class="chat-header">
        <el-avatar :size="36" v-if="activeConversation.avatar" :src="getImageUrl(activeConversation.avatar)" />
        <el-avatar :size="36" v-else>{{ (activeConversation?.username ?? '?')[0] }}</el-avatar>
        <span class="chat-user-name">{{ activeConversation.username || '未知用户' }}</span>
        <el-tag v-if="userOnline" size="small" type="success" effect="plain">在线</el-tag>
        <el-tag v-else size="small" type="info" effect="plain">离线</el-tag>
      </div>

      <!-- 消息列表 -->
      <div class="message-list" ref="messageListRef" @scroll="onMessageScroll">
        <div v-if="loading" class="loading-more">
          <el-icon class="is-loading"><Loading /></el-icon>
        </div>
        <div
          v-for="msg in messages"
          :key="msg.id"
          class="message-item"
          :class="{ 'message-self': msg.fromUserId === currentUserId, 'message-other': msg.fromUserId !== currentUserId }"
        >
          <el-avatar
            :size="36"
            v-if="msg.fromUserId === currentUserId ? currentUserAvatar : msg.fromAvatar"
            :src="getImageUrl(msg.fromUserId === currentUserId ? currentUserAvatar : (msg.fromAvatar || ''))"
          />
          <el-avatar :size="36" v-else>{{ ((msg.fromUserId === currentUserId ? currentUserName : msg.fromUsername) ?? '?')[0] }}</el-avatar>
          <div class="message-content-wrapper">
            <div class="message-sender">{{ msg.fromUserId === currentUserId ? currentUserName : msg.fromUsername }}</div>
            <div class="message-bubble" :class="{ 'message-type-image': msg.messageType === 3 }">
              <template v-if="msg.messageType === 3">
                <img :src="getImageUrl(msg.content || '')" class="msg-image" @click="previewImage(msg.content || '')" />
              </template>
              <template v-else>
                <span v-if="msg.messageType === 2" class="msg-emoji">{{ msg.content }}</span>
                <span v-else>{{ msg.content }}</span>
              </template>
            </div>
            <div class="message-time">{{ formatTime(msg.createTime) }}</div>
          </div>
        </div>
        <div v-if="messages.length === 0 && !loading" class="empty-messages">
          <p>开始聊天吧</p>
        </div>
      </div>

      <!-- 消息输入区 -->
      <div class="message-input-area">
        <div class="input-container">
          <el-tooltip content="表情" placement="top">
            <el-popover placement="top" :width="320" trigger="click" :visible="emojiVisible">
              <template #reference>
                <el-button text class="tool-btn" @click="emojiVisible = !emojiVisible">😊</el-button>
              </template>
              <div class="emoji-picker">
                <span v-for="emoji in emojiList" :key="emoji" class="emoji-item" @click="insertEmoji(emoji)">{{ emoji }}</span>
              </div>
            </el-popover>
          </el-tooltip>
          <el-tooltip content="发送图片" placement="top">
            <el-button text class="tool-btn" :icon="Picture" @click="triggerImageUpload" />
          </el-tooltip>
          <el-input
            v-model="inputContent"
            placeholder="输入消息..."
            @keydown.enter.prevent="sendTextMessage"
          />
          <input
            ref="imageInputRef"
            type="file"
            accept="image/*"
            style="display: none"
            @change="onImageSelected"
          />
          <el-button :disabled="!inputContent.trim()" @click="sendTextMessage" class="send-btn">
            发送
          </el-button>
        </div>
      </div>
    </div>

    <!-- 未选择会话时的占位 -->
    <div class="chat-placeholder" v-else>
      <el-icon :size="64" color="#ddd"><ChatLineSquare /></el-icon>
      <p>选择一个会话开始聊天</p>
    </div>

    <!-- 图片预览弹窗 -->
    <el-dialog
      v-model="previewVisible"
      :show-close="true"
      :close-on-click-modal="true"
      destroy-on-close
      class="image-preview-dialog"
      top="10vh"
      width="480px"
    >
      <img :src="previewUrl" class="preview-image" />
    </el-dialog>
  </div>
</template>

<script lang="ts" setup>
import { ref, onMounted, onUnmounted, computed, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ChatLineSquare, Loading, Picture } from '@element-plus/icons-vue'
import type { ConversationVO, ChatMessageVO, WebSocketMessage } from '@/types/chat'
import { getConversationsApi, getMessagesApi, markAsReadApi, getUserForChatApi } from '@/api/chat-api'
import wsService from '@/utils/websocket'
import { getToken } from '@/utils/localStorage'

const route = useRoute()

// ====== WebSocket 监听清理函数（由 UserHome 管理连接生命周期） ======
let unsubscribeMessage: (() => void) | null = null
let unsubscribeStatus: (() => void) | null = null

// ====== 图片预览 ======
const previewVisible = ref(false)
const previewUrl = ref('')

const previewImage = (url: string) => {
  previewUrl.value = url.startsWith('http') ? url : getImageUrl(url)
  previewVisible.value = true
}

// ====== 表情选择 ======
const emojiVisible = ref(false)
const emojiList = [
  '😀', '😁', '😂', '🤣', '😃', '😄', '😅', '😆', '😉', '😊',
  '😋', '😎', '😍', '🥰', '😘', '😗', '😙', '😚', '🙂', '🤗',
  '🤩', '🤔', '🤨', '😐', '😑', '😶', '🙄', '😏', '😣', '😥',
  '😮', '🤐', '😯', '😪', '😫', '😴', '😌', '😛', '😜', '😝',
  '🤤', '😒', '😓', '😔', '😕', '🙃', '🤑', '😲', '☹️', '🙁',
  '😖', '😞', '😟', '😤', '😢', '😭', '😦', '😧', '😨', '😩',
  '🤯', '😬', '😰', '😱', '🥵', '🥶', '😳', '🤪', '😵', '😡',
  '😠', '🤬', '👍', '👎', '👊', '✊', '🤛', '🤜', '👏', '🙌',
  '👐', '🤲', '🤝', '🙏', '💪', '❤️', '🧡', '💛', '💚', '💙',
  '💜', '🖤', '💔', '💕', '💞', '💗', '💖', '✨', '🌟', '⭐',
  '🔥', '💯', '🎉', '🎊', '🎈', '💝', '👋', '🤚', '✋', '🖐️',
]

const insertEmoji = (emoji: string) => {
  inputContent.value += emoji
  emojiVisible.value = false
}

// ====== 图片上传 ======
const imageInputRef = ref<HTMLInputElement | null>(null)
const imageBaseUrl = import.meta.env.VITE_API_BASE_URL || ''

const triggerImageUpload = () => {
  imageInputRef.value?.click()
}

const onImageSelected = async (event: Event) => {
  const target = event.target as HTMLInputElement
  const file = target.files?.[0]
  if (!file) return

  // 验证文件类型
  const allowed = ['image/jpeg', 'image/jpg', 'image/png', 'image/gif', 'image/webp']
  if (!allowed.includes(file.type)) {
    ElMessage.error('仅支持 JPG / PNG / GIF / WEBP 格式')
    return
  }
  if (file.size > 5 * 1024 * 1024) {
    ElMessage.error('图片大小不能超过 5MB')
    return
  }

  const formData = new FormData()
  formData.append('file', file)

  try {
    const response = await fetch(`${imageBaseUrl}/files/upload`, {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${getToken()}`
      },
      body: formData
    })
    const result = await response.json()
    if (result.code === 200 && result.data?.filePath) {
      const imageUrl = result.data.filePath
      wsService.sendImage(activeConversation.value!.userId!, imageUrl)
      // 消息会通过 WebSocket 回调添加到列表
    } else {
      ElMessage.error('图片上传失败')
    }
  } catch (e) {
    ElMessage.error('图片上传失败')
  } finally {
    target.value = ''
  }
}

// ====== 当前用户信息 ======
const currentUserId = ref<number>(0)
const currentUserName = ref('')
const currentUserAvatar = ref<string | undefined>(undefined)

/** 对话方是否在线 — 基于后端返回的 online 字段 */
const userOnline = computed(() => activeConversation.value?.online ?? false)

// ====== 会话列表 ======
const conversations = ref<ConversationVO[]>([])
const activeConversation = ref<ConversationVO | null>(null)

// ====== 消息列表 ======
const messages = ref<ChatMessageVO[]>([])
const messageListRef = ref<HTMLElement | null>(null)
const loading = ref(false)
const hasMore = ref(true)
const currentPage = ref(1)
const pageSize = 20
const inputContent = ref('')

// 计算属性：消息列表是否在底部
const isNearBottom = computed(() => {
  const el = messageListRef.value
  if (!el) return true
  return el.scrollHeight - el.scrollTop - el.clientHeight < 150
})

// ====== 工具函数 ======
const getImageUrl = (filePath: string) => {
  if (!filePath) return ''
  if (filePath.startsWith('http')) return filePath
  return `${imageBaseUrl}${filePath}`
}

const getPreviewText = (conv: ConversationVO): string => {
  if (conv.lastMessageType === 3) return '[图片]'
  if (conv.lastMessageType === 2) return conv.lastContent || ''
  return conv.lastContent || ''
}

const formatTime = (timeStr?: string): string => {
  if (!timeStr) return ''
  const date = new Date(timeStr)
  const now = new Date()
  const today = new Date(now.getFullYear(), now.getMonth(), now.getDate())
  const msgDate = new Date(date.getFullYear(), date.getMonth(), date.getDate())
  const diffDays = Math.floor((today.getTime() - msgDate.getTime()) / (1000 * 60 * 60 * 24))

  const pad = (n: number) => n.toString().padStart(2, '0')
  const time = `${pad(date.getHours())}:${pad(date.getMinutes())}`

  if (diffDays === 0) return time
  if (diffDays === 1) return `昨天 ${time}`
  if (diffDays < 7) return `${['周日', '周一', '周二', '周三', '周四', '周五', '周六'][date.getDay()]} ${time}`
  return `${date.getMonth() + 1}/${date.getDate()} ${time}`
}

// ====== 加载会话列表 ======
const loadConversations = async () => {
  try {
    const response = await getConversationsApi()
    conversations.value = response?.data ?? []
  } catch (e) {
    console.error('加载会话列表失败:', e)
  }
}

// ====== 切换会话 ======
const switchConversation = async (conv: ConversationVO) => {
  if (activeConversation.value?.userId === conv.userId) return

  const targetUserId = conv.userId
  activeConversation.value = conv
  messages.value = []
  currentPage.value = 1
  hasMore.value = true
  loading.value = false

  // 标记已读
  if (targetUserId) {
    try {
      await markAsReadApi(targetUserId)
      // 立即清除本地会话未读数
      conv.unreadCount = 0
      // 刷新会话列表，同步服务端最新未读状态
      await loadConversations()
      // 重定位 activeConversation 引用（loadConversations 会重建数组）
      const updated = conversations.value.find(c => c.userId === targetUserId)
      if (updated) {
        activeConversation.value = updated
      }
    } catch (e) {
      // 忽略
    }
  }

  await loadMessages(true)
}

// ====== 加载消息 ======
const loadMessages = async (resetScroll: boolean = false) => {
  if (!activeConversation.value?.userId || !hasMore.value || loading.value) return

  loading.value = true
  try {
    const response = await getMessagesApi(activeConversation.value.userId, currentPage.value, pageSize)
    const newMessages = response?.data ?? []

    if (newMessages.length < pageSize) {
      hasMore.value = false
    }

    // 消息是倒序返回的，需要反转
    newMessages.reverse()

    // 合并消息（去重）
    const existingIds = new Set(messages.value.map((m) => m.id))
    const uniqueMessages = newMessages.filter((m: ChatMessageVO) => !existingIds.has(m.id))

    messages.value = [...uniqueMessages, ...messages.value]
    currentPage.value++

    if (resetScroll) {
      await nextTick()
      scrollToBottom()
    }
  } catch (e) {
    console.error('加载消息失败:', e)
  } finally {
    loading.value = false
  }
}

// ====== 滚动到底部 ======
const scrollToBottom = () => {
  const el = messageListRef.value
  if (el) {
    el.scrollTop = el.scrollHeight
  }
}

// ====== 滚动加载更多 ======
const onMessageScroll = () => {
  const el = messageListRef.value
  if (!el) return
  // 滚动到顶部时加载更多
  if (el.scrollTop === 0 && !loading.value && hasMore.value) {
    loadMessages(false)
  }
}

// ====== 发送消息 ======
const sendTextMessage = () => {
  const content = inputContent.value.trim()
  if (!content || !activeConversation.value?.userId) return

  wsService.sendText(activeConversation.value.userId, content, 1)
  inputContent.value = ''

  // 等待 WebSocket 回显后消息会通过回调添加
}

// ====== WebSocket 消息处理 ======
const handleWebSocketMessage = (msg: WebSocketMessage) => {
  // 处理用户上下线状态推送
  if (msg.type === 'USER_STATUS') {
    const targetUserId = Number(msg.fromUserId)
    const online = msg.data === true
    // 更新会话列表中的在线状态
    const conv = conversations.value.find(c => c.userId === targetUserId)
    if (conv) {
      conv.online = online
    }
    // 如果当前活跃会话正是该用户，activeConversation 为 conv 的引用，computed 会自动更新
    return
  }

  if (msg.type === 'MESSAGE' && msg.messageId) {
    // 构建消息 VO（Number() 确保类型一致）
    const fromUserId = Number(msg.fromUserId)
    const toUserId = Number(msg.toUserId)
    const chatMsg: ChatMessageVO = {
      id: msg.messageId,
      fromUserId,
      toUserId,
      content: msg.content,
      messageType: msg.messageType,
      status: msg.status,
      createTime: msg.data || new Date().toISOString()
    }

    // 获取发送方/接收方名称
    const isSelf = fromUserId === currentUserId.value

    if (isSelf) {
      chatMsg.fromUsername = currentUserName.value
      chatMsg.fromAvatar = currentUserAvatar.value
      if (activeConversation.value) {
        chatMsg.toUsername = activeConversation.value.username
        chatMsg.toAvatar = activeConversation.value.avatar
      }
    } else {
      if (activeConversation.value) {
        chatMsg.fromUsername = activeConversation.value.username
        chatMsg.fromAvatar = activeConversation.value.avatar
      }
      chatMsg.toUsername = currentUserName.value
      chatMsg.toAvatar = currentUserAvatar.value
    }

    // 如果当前正在与该用户的会话中，添加到消息列表
    if (activeConversation.value?.userId === fromUserId ||
        activeConversation.value?.userId === toUserId) {
      messages.value.push(chatMsg)
      nextTick(() => {
        if (isNearBottom.value) {
          scrollToBottom()
        }
      })

      // 标记已读
      if (!isSelf && fromUserId) {
        markAsReadApi(fromUserId).catch(() => {})
      }
    }

    // 更新会话列表
    updateConversation(msg, chatMsg)
  }
}

// ====== 更新会话列表 ======
const updateConversation = (wsMsg: WebSocketMessage, chatMsg: ChatMessageVO) => {
  const wsFromUserId = Number(wsMsg.fromUserId)
  const wsToUserId = Number(wsMsg.toUserId)
  const otherUserId = wsFromUserId === currentUserId.value ? wsToUserId : wsFromUserId
  if (!otherUserId) return

  const existingConv = conversations.value.find((c) => c.userId === otherUserId)

  if (existingConv) {
    existingConv.lastContent = wsMsg.content || ''
    existingConv.lastMessageType = wsMsg.messageType || 1
    existingConv.lastTime = chatMsg.createTime
    // 如果不是当前会话且有新消息，增加未读计数
    if (activeConversation.value?.userId !== otherUserId && wsFromUserId !== currentUserId.value) {
      existingConv.unreadCount = (existingConv.unreadCount || 0) + 1
    } else if (activeConversation.value?.userId === otherUserId) {
      existingConv.unreadCount = 0
    }

    // 移到最前
    const idx = conversations.value.indexOf(existingConv)
    if (idx > 0) {
      conversations.value.splice(idx, 1)
      conversations.value.unshift(existingConv)
    }
  } else {
    // 新会话，需要从 API 刷新
    setTimeout(() => loadConversations(), 500)
  }
}

// ====== WebSocket 状态处理 ======
const handleWsStatus = (_status: string) => {
  // 预留：WebSocket 连接状态变化时可做重连提示等处理
}

// ====== 生命周期 ======
onMounted(async () => {
  // ① 优先从 API 获取当前用户信息（token 认证，最可靠）
  try {
    const { userTokenAuthApi } = await import('@/api/user-api')
    const resp = await userTokenAuthApi()
    const data = resp?.data as any
    if (data) {
      currentUserId.value = Number(data.id) || 0
      currentUserName.value = data.username || ''
      currentUserAvatar.value = data.avatar
      // 缓存到 localStorage 备用
      localStorage.setItem('USER_INFO', JSON.stringify(data))
    }
  } catch {
    console.warn('[ChatView] API 获取用户信息失败，尝试 localStorage 兜底')
  }

  // ② API 获取失败时，从 localStorage 兜底
  if (!currentUserId.value) {
    const userInfoStr = localStorage.getItem('USER_INFO')
    if (userInfoStr) {
      try {
        const info = JSON.parse(userInfoStr)
        currentUserId.value = Number((info as any).id) || 0
        currentUserName.value = (info as any).username || ''
        currentUserAvatar.value = (info as any).avatar
      } catch {
        // 忽略解析错误
      }
    }
  }

  // 建立 WebSocket 连接（幂等逻辑，不会重复连接）
  wsService.connect(imageBaseUrl)

  // 注册消息监听（保存清理函数以便卸载时移除）
  unsubscribeMessage = wsService.onMessage(handleWebSocketMessage)
  unsubscribeStatus = wsService.onStatus(handleWsStatus)

  // 加载会话列表
  await loadConversations()

  // 检查路由参数（从 /user-home/chat/:targetUserId 或 query string 获取目标用户）
  const targetUserId = route.params.targetUserId || route.query.targetUserId
  if (targetUserId) {
    const targetId = Number(targetUserId)
    const targetConv = conversations.value.find((c) => c.userId === targetId)
    if (targetConv) {
      await switchConversation(targetConv)
      return
    }
    // 如果会话列表中没有该用户的会话，获取用户信息并创建临时会话
    try {
      const resp = await getUserForChatApi(targetId)
      const userConv = resp?.data
      if (userConv) {
        userConv.lastContent = ''
        userConv.unreadCount = 0
        conversations.value.unshift(userConv)
        await switchConversation(userConv)
        return
      }
    } catch (e) {
      console.error('获取目标用户信息失败:', e)
    }
  }

  // 如果有未读消息的会话，优先选中
  const unreadConv = conversations.value.find((c) => (c.unreadCount || 0) > 0)
  if (unreadConv) {
    await switchConversation(unreadConv)
  } else if (conversations.value.length > 0) {
    // 默认选中第一个会话
    await switchConversation(conversations.value[0])
  }
})

onUnmounted(() => {
  // 只移除监听，不断开连接（由 UserHome 管理连接生命周期以保持角标实时更新）
  if (unsubscribeMessage) {
    unsubscribeMessage()
    unsubscribeMessage = null
  }
  if (unsubscribeStatus) {
    unsubscribeStatus()
    unsubscribeStatus = null
  }
})
</script>

<style scoped lang="scss">
// ====== 整体容器 ======
.chat-container {
  display: flex;
  height: calc(100vh - 180px);
  max-width: 900px;
  margin: 0 auto;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06), 0 1px 6px rgba(0, 0, 0, 0.04);
  overflow: hidden;
  border: 1px solid #e8e8e8;
}

// ====== 左侧会话列表 ======
.conversation-sidebar {
  width: 320px;
  min-width: 320px;
  border-right: 1px solid #f0f0f0;
  display: flex;
  flex-direction: column;


  .sidebar-header {
    padding: 20px 20px;
    border-bottom: 1px solid #f0f0f0;

    h3 {
      margin: 0;
      font-size: 16px;
      font-weight: 700;
      color: #1a1a1a;
      letter-spacing: 0.3px;
    }
  }

  .conversation-list {
    flex: 1;
    overflow-y: auto;

    &::-webkit-scrollbar {
      width: 4px;
    }
    &::-webkit-scrollbar-thumb {
      background: #d8d8d8;
      border-radius: 2px;
    }
  }

  .conversation-item {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 12px 20px;
    cursor: pointer;
    transition: background 0.15s ease;
    position: relative;

    &::before {
      content: '';
      position: absolute;
      left: 0;
      top: 50%;
      transform: translateY(-50%);
      width: 3px;
      height: 0;
      background: #1a1a1a;
      border-radius: 0 2px 2px 0;
      transition: height 0.2s ease;
    }

    &:hover {
      background: #f0f0f0;
    }

    &.active {
      background: #f5f5f5;

      &::before {
        height: 28px;
      }

      .conv-name {
        font-weight: 600;
      }
    }

    .conv-info {
      flex: 1;
      min-width: 0;
    }

    .conv-top {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 5px;
    }

    .conv-name {
      font-size: 14px;
      font-weight: 500;
      color: #1a1a1a;
      flex: 1;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    .conv-time {
      font-size: 11px;
      color: #bfbfbf;
      white-space: nowrap;
      flex-shrink: 0;
      margin-left: 8px;
    }

    .conv-bottom {
      display: flex;
      justify-content: space-between;
      align-items: center;
    }

    .conv-preview {
      font-size: 13px;
      color: #8c8c8c;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
      flex: 1;
      line-height: 1.3;
    }

    .unread-badge {
      margin-left: 8px;
      flex-shrink: 0;
      :deep(.el-badge__content) {
        border: none;
        font-size: 11px;
        padding: 0 5px;
        background: #1a1a1a;
        color: #fff;
        height: 18px;
        line-height: 18px;
        min-width: 18px;
      }
    }
  }

  .empty-conversations {
    flex: 1;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    color: #bfbfbf;
    gap: 12px;

    p {
      margin: 0;
      font-size: 14px;
    }
  }
}

// ====== 右侧聊天区域 ======
.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: #fff;
  min-width: 0;
}

.chat-header {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px 24px;
  border-bottom: 1px solid #f0f0f0;
  background: #fff;

  .chat-user-name {
    font-size: 15px;
    font-weight: 600;
    color: #1a1a1a;
    flex: 1;
  }
}

.message-list {
  flex: 1;
  overflow-y: auto;
  padding: 20px 24px;
  background: #f7f7f8;

  &::-webkit-scrollbar {
    width: 4px;
  }
  &::-webkit-scrollbar-thumb {
    background: #d8d8d8;
    border-radius: 2px;
  }

  .loading-more {
    text-align: center;
    padding: 12px;
    color: #8c8c8c;
    font-size: 13px;
  }

  .message-item {
    display: flex;
    gap: 10px;
    margin-bottom: 20px;

    &.message-self {
      flex-direction: row-reverse;

      .message-content-wrapper {
        align-items: flex-end;
      }

      .message-bubble {
        background: #2c2c2c;
        color: #fff;
        border-bottom-right-radius: 4px;
      }

      .message-sender {
        text-align: right;
      }

      .message-time {
        text-align: right;
      }
    }

    &.message-other {
      .message-bubble {
        background: #f0f0f0;
        color: #1a1a1a;
        border-bottom-left-radius: 4px;
      }
    }
  }

  .message-content-wrapper {
    display: flex;
    flex-direction: column;
    max-width: 60%;
  }

  .message-sender {
    font-size: 12px;
    color: #8c8c8c;
    margin-bottom: 5px;
    padding: 0 2px;
  }

  .message-bubble {
    padding: 10px 16px;
    border-radius: 14px;
    font-size: 14px;
    line-height: 1.55;
    word-break: break-word;
    letter-spacing: 0.2px;

    &.message-type-image {
      padding: 4px;
      background: transparent !important;

      .msg-image {
        max-width: 200px;
        max-height: 200px;
        border-radius: 8px;
        cursor: pointer;
        transition: transform 0.2s;
        border: 1px solid #f0f0f0;

        &:hover {
          transform: scale(1.03);
        }
      }
    }
  }

  .msg-emoji {
    font-size: 28px;
    line-height: 1;
  }

  .message-time {
    font-size: 11px;
    color: #bfbfbf;
    margin-top: 5px;
    padding: 0 2px;
  }

  .empty-messages {
    display: flex;
    justify-content: center;
    align-items: center;
    height: 100%;
    color: #bfbfbf;
    font-size: 14px;
  }
}

// ====== 输入区域 ======
.message-input-area {
  padding: 12px 24px 16px;
  background: #fff;

  .input-container {
    display: flex;
    align-items: center;
    gap: 4px;
    background: #f0f2f5;
    border-radius: 24px;
    padding: 4px 4px 4px 12px;
    box-shadow: none;

    .tool-btn {
      font-size: 20px;
      color: #8c8c8c;
      padding: 6px;
      flex-shrink: 0;
      border-radius: 50%;

      &:hover {
        color: #1a1a1a;
        background: rgba(0, 0, 0, 0.04);
      }
    }

    :deep(.el-input) {
      flex: 1;

      .el-input__wrapper {
        background: transparent;
        box-shadow: none !important;
        padding: 0 8px;

        .el-input__inner {
          border: none;
          font-size: 14px;
          color: #1a1a1a;
          height: 40px;

          &::placeholder {
            color: #bfbfbf;
          }
        }
      }
    }

    .send-btn {
      height: 36px;
      padding: 0 20px;
      flex-shrink: 0;
      font-size: 14px;
      border-radius: 20px;
      background: #1a1a1a;
      border: none;
      color: #fff;
      font-weight: 500;
      transition: opacity 0.2s;

      &:hover {
        opacity: 0.85;
        background: #1a1a1a;
      }

      &:disabled {
        background: #d9d9d9;
        color: #fff;
      }
    }
  }
}

// ====== 表情选择器 ======
.emoji-picker {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  padding: 8px;

  .emoji-item {
    font-size: 24px;
    cursor: pointer;
    padding: 4px;
    border-radius: 4px;
    transition: background 0.15s;

    &:hover {
      background: #f0f0f0;
    }
  }
}

// ====== 占位区域 ======
.chat-placeholder {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #bfbfbf;
  gap: 16px;

  p {
    margin: 0;
    font-size: 15px;
    color: #bfbfbf;
  }
}

// ====== 图片预览弹窗 ======
:deep(.image-preview-dialog) {
  .el-dialog__header {
    padding: 10px 10px 0;

    .el-dialog__headerbtn {
      top: 8px;
      right: 8px;
      font-size: 18px;
    }
  }

  .el-dialog__body {
    padding: 8px;
    overflow: hidden;
    display: flex;
    align-items: center;
    justify-content: center;
    background: #fafafa;
    border-radius: 0 0 4px 4px;
  }
}

.preview-image {
  display: block;
  max-width: 100%;
  max-height: 70vh;
  width: auto;
  height: auto;
  object-fit: contain;
  border-radius: 4px;
}
</style>
