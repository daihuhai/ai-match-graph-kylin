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
    config.headers = config.headers ?? new AxiosHeaders()
    if (config.headers instanceof AxiosHeaders) {
      config.headers.set('Authorization', `Bearer ${auth.token}`)
    } else {
      ;(config.headers as any).Authorization = `Bearer ${auth.token}`
    }
  }
  return config
})

http.interceptors.response.use(
  (resp) => {
    const body = resp.data as ApiEnvelope<unknown>
    if (body && typeof body === 'object' && 'code' in body) {
      if (body.code !== 200) return Promise.reject(body)
      return body.data
    }
    return resp.data
  },
  (err) => Promise.reject(err),
)
