import { api } from './client'

export type Announcement = {
  id: number
  courseId: number
  courseName: string
  title: string
  body: string
  createdAt: string
}

export async function getCourseAnnouncements(courseId: number) {
  const response = await api.get<Announcement[]>(
    `/api/courses/${courseId}/announcements`,
  )

  return response.data
}

export async function createAnnouncement(
  courseId: number,
  title: string,
  body: string,
) {
  const response = await api.post<Announcement>(
    `/api/courses/${courseId}/announcements`,
    {
      title,
      body,
    },
  )

  return response.data
}

export async function updateAnnouncement(
  announcementId: number,
  title: string,
  body: string,
) {
  const response = await api.put<Announcement>(
    `/api/announcements/${announcementId}`,
    {
      title,
      body,
    },
  )

  return response.data
}

export async function deleteAnnouncement(announcementId: number) {
  const response = await api.delete<Announcement>(
    `/api/announcements/${announcementId}`,
  )

  return response.data
}
