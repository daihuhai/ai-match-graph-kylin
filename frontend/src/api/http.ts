import axios, { AxiosHeaders } from 'axios'
import { useAuthStore } from '@/stores/auth'

export interface ApiEnvelope<T> {
  code: number
  message: string
  data: T
}

export const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE ?? '/api/v1',
  timeout: 15000,
})

http.interceptors.request.use((config) => {
  const auth = useAuthStore()
  if (auth.token) {
    config.headers.set('Authorization', `Bearer ${auth.token}`)
  }
  return config
})

http.interceptors.response.use(
  (resp) => {
    const body = resp.data as ApiEnvelope<unknown>
    // 判断是否为标准业务响应格式：必须包含 code/message/data 三个字段
    if (body && typeof body === 'object' && 'code' in body && 'message' in body && 'data' in body) {
      if (body.code !== 200) return Promise.reject(body)
      return body.data
    }
    return resp.data
  },
  (err) => {
    const status = err.response?.status as number | undefined
    const body = err.response?.data as ApiEnvelope<unknown> | undefined
    if (body && typeof body === 'object' && 'message' in body && body.message) {
      return Promise.reject(new Error(body.message))
    }
    if (!err.response) {
      return Promise.reject(new Error('无法连接后端，请确认 backend 已启动'))
    }
    if (status === 500 || status === 502 || status === 503) {
      return Promise.reject(
        new Error('后端不可用（可能尚未启动完成），请稍后重试或检查开发代理端口是否为 8080'),
      )
    }
    return Promise.reject(err)
  },
)
