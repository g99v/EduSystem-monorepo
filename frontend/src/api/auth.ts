import axios from 'axios'
import { api, postWithCsrf, setAuthSession } from './client'

export type AuthUser = {
  id: number
  email: string
  role: 'STUDENT' | 'INSTRUCTOR' | 'ADMIN'
  studentId: number | null
  instructorId: number | null
}

export type AuthResponse = {
  accessToken: string
  expiresIn: number
  user: AuthUser
}

export type RegisterStudentRequest = {
  firstName: string
  lastName: string
  email: string
  password: string
  age: number
  schoolId: number | null
}

export async function getCurrentUser() {
  const response = await api.get<AuthUser>('/api/auth/me')
  return response.data
}

export async function loginUser(email: string, password: string) {
  const response = await postWithCsrf<AuthResponse>('/api/auth/login', { email, password })
  setAuthSession(response.data)
  return response.data.user
}

export async function registerStudent(student: RegisterStudentRequest) {
  await postWithCsrf('/api/auth/register/student', student)
}

export async function logoutUser() {
  setAuthSession(null)
}

export function isStatus(error: unknown, status: number) {
  return axios.isAxiosError(error) && error.response?.status === status
}

export function errorMessage(error: unknown): string {
  if (!axios.isAxiosError(error)) return 'Something went wrong.'
  if (!error.response) return 'Cannot reach the server.'

  const message = error.response.data?.message
  if (typeof message === 'string') return message
  if (error.response.status === 403) return 'Access denied.'
  if (error.response.status === 401) return 'Your session has expired. Sign in again.'
  return 'Request failed. Try again.'
}

export function fieldErrors(error: unknown): Record<string, string> {
  if (!isStatus(error, 400) || !axios.isAxiosError(error)) return {}

  const data = error.response?.data
  if (!data || typeof data !== 'object' || Array.isArray(data)) return {}

  const errors: Record<string, string> = {}
  for (const field of ['firstName', 'lastName', 'email', 'password', 'age', 'schoolId']) {
    const message = (data as Record<string, unknown>)[field]
    if (typeof message === 'string') {
      errors[field] = message
    }
  }
  return errors
}
