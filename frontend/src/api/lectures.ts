import { api } from './client'

export type Lecture = {
  id: number
  courseId: number
  courseName: string
  title: string
  originalFilename: string
  contentType: string
  fileSize: number
  displayOrder: number
  uploadedAt: string
}

export async function getCourseLectures(courseId: number) {
  const response = await api.get<Lecture[]>(`/api/courses/${courseId}/lectures`)
  return response.data
}

export async function uploadLecture(
  courseId: number,
  title: string,
  displayOrder: number,
  file: File,
) {
  const form = new FormData()
  form.append('title', title)
  form.append('displayOrder', String(displayOrder))
  form.append('file', file)

  const response = await api.post<Lecture>(
    `/api/courses/${courseId}/lectures`,
    form,
  )

  return response.data
}

export async function getLectureFile(lectureId: number) {
  const response = await api.get<Blob>(`/api/lectures/${lectureId}/content`, {
    responseType: 'blob',
  })

  return response.data
}

export async function deleteLecture(lectureId: number) {
  const response = await api.delete<Lecture>(`/api/lectures/${lectureId}`)
  return response.data
}
