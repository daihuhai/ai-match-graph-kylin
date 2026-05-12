import { http } from '@/api/http'
import type { UserType } from '@/stores/auth'

export interface LoginResp {
  token: string
  userType: UserType
  userId: string
  permissions?: string[]
}

export async function register(payload: {
  account: string
  password: string
  userType: UserType
  phone?: string
  email?: string
}): Promise<{ account: string; userType: UserType }> {
  return http.post('/auth/register', payload)
}

export async function login(payload: { account: string; password: string; userType: UserType }): Promise<LoginResp> {
  return http.post('/auth/login', payload)
}
