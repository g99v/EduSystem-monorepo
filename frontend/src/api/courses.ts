import { api } from './client'

export type Course = {
  id: number
  name: string
  description: string | null
  teacherName: string
  instructorId: number | null
  instructorName: string | null
}

export type Enrollment = {
  id: number
  studentId: number
  studentName: string
  courseId: number
  courseName: string
  enrolledAt: string
  status: 'ACTIVE' | 'COMPLETED' | 'WITHDRAWN'
  grade: string | null
}

export async function getCourses() {
  const response = await api.get<Course[]>('/api/courses')
  return response.data
}

export async function getCourseById(courseId: number) {
  const response = await api.get<Course>(`/api/courses/${courseId}`)
  return response.data
}

export async function getMyCourses() {
  const response = await api.get<Course[]>('/api/me/courses')
  return response.data
}

export async function enrollInCourse(courseId: number) {
  const response = await api.post<Enrollment>(
    `/api/courses/${courseId}/enrollments`
  )

  return response.data
}
