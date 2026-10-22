import { api } from './client'

export type Submission = {
  id: number
  assignmentId: number
  assignmentTitle: string
  studentId: number
  studentName: string
  originalFilename: string
  contentType: string
  fileSize: number
  submittedAt: string
  grade: number | null
  feedback: string | null
  gradedAt: string | null
}

export async function getMySubmissions(assignmentId: number) {
  const response = await api.get<Submission[]>(
    `/api/assignments/${assignmentId}/submissions/me`,
  )

  return response.data
}

export async function getAssignmentSubmissions(assignmentId: number) {
  const response = await api.get<Submission[]>(
    `/api/assignments/${assignmentId}/submissions`,
  )

  return response.data
}

export async function uploadSubmission(assignmentId: number, file: File) {
  const form = new FormData()
  form.append('file', file)

  const response = await api.post<Submission>(
    `/api/assignments/${assignmentId}/submissions`,
    form,
  )

  return response.data
}

export async function getSubmissionFile(submissionId: number) {
  const response = await api.get<Blob>(`/api/submissions/${submissionId}/file`, {
    responseType: 'blob',
  })

  return response.data
}

export async function gradeSubmission(
  submissionId: number,
  grade: number,
  feedback: string | null,
) {
  const response = await api.put<Submission>(
    `/api/submissions/${submissionId}`,
    {
      grade,
      feedback,
    },
  )

  return response.data
}
