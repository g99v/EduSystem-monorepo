import { api } from './client'

export type Assignment = {
  id: number
  courseId: number
  courseName: string
  title: string
  assignmentInstructions: string | null
  maxGrade: number
  originalFilename: string
  contentType: string
  fileSize: number
  displayOrder: number
  uploadedAt: string
}

export async function getCourseAssignments(courseId: number) {
  const response = await api.get<Assignment[]>(`/api/courses/${courseId}/assignments`)
  return response.data
}

export async function uploadAssignment(
  courseId: number,
  title: string,
  instructions: string,
  maxGrade: number,
  displayOrder: number,
  file: File,
) {
  const form = new FormData()
  form.append('title', title)

  if (instructions) {
    form.append('assignmentInstructions', instructions)
  }

  form.append('maxGrade', String(maxGrade))
  form.append('displayOrder', String(displayOrder))
  form.append('file', file)

  const response = await api.post<Assignment>(
    `/api/courses/${courseId}/assignments`,
    form,
  )

  return response.data
}

export async function getAssignmentFile(assignmentId: number) {
  const response = await api.get<Blob>(`/api/assignments/${assignmentId}/file`, {
    responseType: 'blob',
  })

  return response.data
}

export async function deleteAssignment(assignmentId: number) {
  const response = await api.delete<Assignment>(`/api/assignments/${assignmentId}`)
  return response.data
}
