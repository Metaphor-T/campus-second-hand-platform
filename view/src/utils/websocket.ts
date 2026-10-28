// src/utils/websocket.ts WebSocket 连接管理

import { getToken } from './localStorage'
import type { WebSocketMessage } from '@/types/chat'

/** WebSocket 连接状态 */
export type WsStatus = 'connecting' | 'connected' | 'disconnected' | 'reconnecting'

/** 消息回调类型 */
type MessageCallback = (msg: WebSocketMessage) => void
type StatusCallback = (status: WsStatus) => void

class WebSocketService {
  private ws: WebSocket | null = null
  private baseUrl: string = ''
  private heartbeatTimer: ReturnType<typeof setInterval> | null = null
  private reconnectTimer: ReturnType<typeof setTimeout> | null = null
  private reconnectCount = 0
  private maxReconnectAttempts = 10
  private reconnectDelay = 3000
  private messageCallbacks: MessageCallback[] = []
  private statusCallbacks: StatusCallback[] = []
  private _status: WsStatus = 'disconnected'

  /** 获取当前状态 */
  get status(): WsStatus {
    return this._status
  }

  /** 设置状态并通知回调 */
  private setStatus(status: WsStatus) {
    this._status = status
    this.statusCallbacks.forEach((cb) => cb(status))
  }

  /**
   * 初始化 WebSocket 连接
   * @param baseUrl 后端基础 URL（例如 http://localhost:21090）
   */
  connect(baseUrl: string) {
    // 如果已经连接同一个 URL，不重复连接
    if (this.ws && this.ws.readyState === WebSocket.OPEN && this.baseUrl === baseUrl) {
      return
    }

    // 如果已有连接，先关闭
    if (this.ws) {
      this.ws.close()
      this.ws = null
    }

    const token = getToken()
    if (!token) {
      console.warn('[WebSocket] 无 Token，无法连接')
      return
    }

    // 构建 WebSocket URL（使用 ws:// 替换 http://）
    const wsUrl = baseUrl.replace(/^http/, 'ws') + `/ws/chat?token=${encodeURIComponent(token)}`

    this.setStatus('connecting')
    this.ws = new WebSocket(wsUrl)

    this.ws.onopen = () => {
      console.log('[WebSocket] 连接已建立')
      this.setStatus('connected')
      this.reconnectCount = 0
      this.startHeartbeat()
    }

    this.ws.onmessage = (event: MessageEvent) => {
      try {
        const msg: WebSocketMessage = JSON.parse(event.data)
        this.messageCallbacks.forEach((cb) => cb(msg))
      } catch (e) {
        console.error('[WebSocket] 消息解析失败:', e)
      }
    }

    this.ws.onclose = (event: CloseEvent) => {
      console.log('[WebSocket] 连接关闭:', event.code, event.reason)
      this.stopHeartbeat()
      this.setStatus('disconnected')

      // 尝试重连
      this.attemptReconnect()
    }

    this.ws.onerror = (error: Event) => {
      console.error('[WebSocket] 连接错误:', error)
    }
  }

  /**
   * 断开连接
   */
  disconnect() {
    this.stopHeartbeat()
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer)
      this.reconnectTimer = null
    }
    this.reconnectCount = this.maxReconnectAttempts // 阻止重连
    if (this.ws) {
      this.ws.close()
      this.ws = null
    }
    this.setStatus('disconnected')
  }

  /**
   * 发送消息
   */
  send(msg: WebSocketMessage) {
    if (this.ws && this.ws.readyState === WebSocket.OPEN) {
      this.ws.send(JSON.stringify(msg))
    } else {
      console.warn('[WebSocket] 连接未就绪，无法发送消息')
    }
  }

  /**
   * 发送文本消息
   */
  sendText(toUserId: number, content: string, messageType: number = 1) {
    this.send({
      type: 'MESSAGE',
      messageType,
      content,
      toUserId
    })
  }

  /**
   * 发送图片消息
   */
  sendImage(toUserId: number, imageUrl: string) {
    this.send({
      type: 'MESSAGE',
      messageType: 3,
      content: imageUrl,
      toUserId
    })
  }

  /**
   * 注册消息监听
   */
  onMessage(callback: MessageCallback) {
    this.messageCallbacks.push(callback)
    return () => {
      this.messageCallbacks = this.messageCallbacks.filter((cb) => cb !== callback)
    }
  }

  /**
   * 注册状态监听
   */
  onStatus(callback: StatusCallback) {
    this.statusCallbacks.push(callback)
    return () => {
      this.statusCallbacks = this.statusCallbacks.filter((cb) => cb !== callback)
    }
  }

  /** 启动心跳 */
  private startHeartbeat() {
    this.stopHeartbeat()
    this.heartbeatTimer = setInterval(() => {
      if (this.ws && this.ws.readyState === WebSocket.OPEN) {
        this.ws.send(JSON.stringify({ type: 'HEARTBEAT' }))
      }
    }, 30000) // 30秒一次心跳
  }

  /** 停止心跳 */
  private stopHeartbeat() {
    if (this.heartbeatTimer) {
      clearInterval(this.heartbeatTimer)
      this.heartbeatTimer = null
    }
  }

  /** 尝试重连 */
  private attemptReconnect() {
    if (this.reconnectCount >= this.maxReconnectAttempts) {
      console.log('[WebSocket] 已达到最大重连次数，停止重连')
      return
    }

    this.reconnectCount++
    this.setStatus('reconnecting')

    const delay = Math.min(this.reconnectDelay * Math.pow(1.5, this.reconnectCount - 1), 30000)
    console.log(`[WebSocket] 第 ${this.reconnectCount} 次重连，延迟 ${delay}ms`)

    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer)
    }
    this.reconnectTimer = setTimeout(() => {
      this.connect(this.baseUrl)
    }, delay)
  }
}

// 导出单例
const wsService = new WebSocketService()
export default wsService
