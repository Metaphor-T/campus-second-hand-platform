// src/utils/request.ts
import axios, { AxiosResponse, InternalAxiosRequestConfig, type AxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import { getToken, clearToken, clearRole, clearLastVisitedPath } from './localStorage'
import wsService from './websocket'

export interface RequestOptions extends AxiosRequestConfig {
  // 页面自行展示错误时，避免全局消息与页面提示重复。
  silentError?: boolean
}

// 创建 axios 实例
const instance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 15000,
})

// 请求拦截器
instance.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = getToken()
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

// 响应拦截器
instance.interceptors.response.use(
  (response: AxiosResponse) => {
    if (response.data.code !== 200) {
      return Promise.reject(response.data)
    }
    return response.data
  },
  (error) => {
    const currentToken = getToken()
    const sentToken = String(error.config?.headers?.Authorization || '').replace(/^Bearer\s+/i, '')
    // 只清理这次失效请求对应的登录，避免旧请求覆盖刚建立的新登录。
    if (error.response?.status === 401 && currentToken && sentToken === currentToken) {
      clearToken(); clearRole(); clearLastVisitedPath(); wsService.disconnect()
      const login = new URL(`${import.meta.env.BASE_URL}login`, window.location.origin)
      if (window.location.pathname !== login.pathname) window.location.replace(login.href)
    }
    if (!error.config?.silentError) {
      ElMessage.error(error.response?.data?.message || error.message || '请求失败')
    }
    return Promise.reject(error)
  }
)

// 封装 request 方法
const request = {
  post: <T = any>(url: string, data?: any, options?: RequestOptions): Promise<T> => {
    return instance.post(url, data, options)
  },
  get: <T = any>(url: string, params?: any): Promise<T> => {
    return instance.get(url, { params })
  },
  put: <T = any>(url: string, data?: any): Promise<T> => {
    return instance.put(url, data)
  },
  delete: <T = any>(url: string, params?: any): Promise<T> => {
    return instance.delete(url, { params })
  }
}

// 命名导出，兼容按需引用
export const get = request.get
export const post = request.post
export const put = request.put
export const delete_ = request.delete

export default request
