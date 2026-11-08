// src/api/chat-api.ts 聊天 REST API

import type { Result } from '@/types/result'
import type { ChatMessageVO, ConversationVO } from '@/types/chat'
import request from '@/utils/request'

/** 获取会话列表 */
export const getConversationsApi = () => {
  return request.get<Result<ConversationVO[]>>('/chat/conversations')
}

/** 获取与某用户的历史消息 */
export const getMessagesApi = (userId: number, page: number = 1, size: number = 20) => {
  return request.get<Result<ChatMessageVO[]>>('/chat/messages', { userId, page, size })
}

/** 标记消息为已读 */
export const markAsReadApi = (userId: number) => {
  return request.put<Result<void>>(`/chat/read?userId=${userId}`)
}

/** 获取与某用户的未读消息数 */
export const getUnreadCountApi = (userId: number) => {
  return request.get<Result<number>>('/chat/unread', { userId })
}

/** 获取总未读消息数 */
export const getTotalUnreadCountApi = () => {
  return request.get<Result<number>>('/chat/unread/total')
}

/** 获取聊天目标用户信息（用于创建临时会话） */
export const getUserForChatApi = (userId: number) => {
  return request.get<Result<ConversationVO>>(`/chat/user/${userId}`)
}

// ====== 管理员 - 消息管理接口 ======

/** 管理员分页查询消息列表 */
export const messageListPageApi = (params: any) => {
  return request.post<Result<ChatMessageVO[]>>('/admin/messages/listPage', params)
}

/** 管理员删除消息 */
export const deleteMessageApi = (id: number) => {
  return request.delete<Result<void>>(`/admin/messages/${id}`)
}
