// src/types/chat.ts 聊天相关类型定义

/** 聊天消息视图对象 */
export interface ChatMessageVO {
  id?: number
  fromUserId?: number
  fromUsername?: string
  fromAvatar?: string
  toUserId?: number
  toUsername?: string
  toAvatar?: string
  content?: string
  messageType?: number // 1文本，2表情，3图片
  status?: number // 0未读，1已读
  createTime?: string
}

/** 会话列表视图对象 */
export interface ConversationVO {
  userId?: number
  username?: string
  avatar?: string
  lastContent?: string
  lastMessageType?: number
  lastTime?: string
  unreadCount?: number
  online?: boolean // 对方是否在线
}

/** WebSocket 消息传输对象 */
export interface WebSocketMessage {
  type: 'MESSAGE' | 'HEARTBEAT' | 'ERROR' | 'UNREAD_COUNT'
  messageType?: number // 1文本，2表情，3图片
  content?: string
  toUserId?: number
  fromUserId?: number
  timestamp?: number
  messageId?: number
  status?: number
  data?: any
}
