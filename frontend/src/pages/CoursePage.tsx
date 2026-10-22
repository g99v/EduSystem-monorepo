import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams, useSearchParams } from 'react-router-dom'
import {
  deleteAnnouncement,
  getCourseAnnouncements,
  type Announcement,
} from '../api/announcements'
import {
  deleteAssignment,
  getAssignmentFile,
  getCourseAssignments,
  type Assignment,
} from '../api/assignments'
import { errorMessage } from '../api/auth'
import { getCourseById, type Course } from '../api/courses'
import {
  deleteLecture,
  getCourseLectures,
  getLectureFile,
  type Lecture,
} from '../api/lectures'
import {
  getAssignmentSubmissions,
  getMySubmissions,
  getSubmissionFile,
  type Submission,
} from '../api/submissions'
import {
  AnnouncementForm,
  AssignmentUploadForm,
  GradeForm,
  LectureUploadForm,
  SubmissionForm,
} from '../components/CourseForms'
import { DashboardLayout } from '../components/DashboardLayout'
import { instructorSections, studentSections } from '../data/navigation'

type Role = 'student' | 'instructor'
type CourseTab = 'lectures' | 'assignments' | 'announcements'

export function CoursePage({ role }: { role: Role }) {
  const navigate = useNavigate()
  const { courseId } = useParams()
  const [params, setParams] = useSearchParams()
  const [course, setCourse] = useState<Course | null>(null)
  const [courseLoading, setCourseLoading] = useState(true)
  const [courseError, setCourseError] = useState('')
  const requestedTab = params.get('tab')
  const activeTab: CourseTab = requestedTab === 'assignments' || requestedTab === 'announcements'
    ? requestedTab
    : 'lectures'
  const isStudent = role === 'student'
  const sections = isStudent ? studentSections : instructorSections
  const base = isStudent ? '/student' : '/instructor'

  useEffect(() => {
    async function loadCourse() {
      try {
        const courseData = await getCourseById(Number(courseId))
        setCourse(courseData)
      } catch (error) {
        setCourseError(errorMessage(error))
      } finally {
        setCourseLoading(false)
      }
    }

    void loadCourse()
  }, [courseId])

  function handleSectionChange(section: string) {
    if (section === 'overview') {
      navigate(base)
    } else {
      navigate(`${base}?view=${section}`)
    }
  }

  function showTab(tab: CourseTab) {
    setParams({ tab })
  }

  return (
    <DashboardLayout
      sections={sections}
      activeSection="courses"
      onSectionChange={handleSectionChange}
    >
      <Link className="back-link" to={`${base}?view=courses`}>
        ← Courses
      </Link>

      {courseLoading && (
        <p className="empty-state">
          Loading...
        </p>
      )}

      {courseError && (
        <p className="form-notice">
          {courseError}
        </p>
      )}

      {course && (
        <>
          <div className="page-heading course-heading">
            <h1>
              {course.name}
            </h1>
            <p>
              Instructor: {course.instructorName ?? course.teacherName}
            </p>
          </div>

          <div className="course-section course-overview">
            <div className="course-content-card">
              <h2>
                Overview
              </h2>
              {course.description && (
                <p>
                  {course.description}
                </p>
              )}
            </div>
          </div>

          <div className="course-tabs">
            <button
              type="button"
              className={activeTab === 'lectures' ? 'active' : ''}
              onClick={() => showTab('lectures')}
            >
              Lectures
            </button>
            <button
              type="button"
              className={activeTab === 'assignments' ? 'active' : ''}
              onClick={() => showTab('assignments')}
            >
              Assignments
            </button>
            <button
              type="button"
              className={activeTab === 'announcements' ? 'active' : ''}
              onClick={() => showTab('announcements')}
            >
              Announcements
            </button>
          </div>

          {activeTab === 'lectures' && (
            <LectureTab courseId={course.id} role={role} />
          )}

          {activeTab === 'assignments' && (
            <AssignmentTab courseId={course.id} role={role} />
          )}

          {activeTab === 'announcements' && (
            <AnnouncementTab courseId={course.id} role={role} />
          )}
        </>
      )}
    </DashboardLayout>
  )
}

function LectureTab({ courseId, role }: { courseId: number; role: Role }) {
  const [lectures, setLectures] = useState<Lecture[]>([])
  const [lecturesLoading, setLecturesLoading] = useState(true)
  const [lecturesError, setLecturesError] = useState('')
  const [fileError, setFileError] = useState('')
  const [activeFileId, setActiveFileId] = useState<number | null>(null)
  const [lectureToDelete, setLectureToDelete] = useState<Lecture | null>(null)
  const [deleteError, setDeleteError] = useState('')
  const [deleting, setDeleting] = useState(false)

  useEffect(() => {
    async function loadLectures() {
      try {
        const lectureData = await getCourseLectures(courseId)
        setLectures(
          lectureData.sort((first, second) => first.displayOrder - second.displayOrder),
        )
      } catch (error) {
        setLecturesError(errorMessage(error))
      } finally {
        setLecturesLoading(false)
      }
    }

    void loadLectures()
  }, [courseId])

  function addUploadedLecture(lecture: Lecture) {
    setLectures((current) => (
      [...current, lecture].sort((first, second) => first.displayOrder - second.displayOrder)
    ))
  }

  async function openLectureFile(lecture: Lecture, download: boolean) {
    setActiveFileId(lecture.id)
    setFileError('')

    try {
      const file = await getLectureFile(lecture.id)
      const url = URL.createObjectURL(file)
      const link = document.createElement('a')

      link.href = url

      if (download) {
        link.download = lecture.originalFilename
      } else {
        link.target = '_blank'
        link.rel = 'noopener noreferrer'
      }

      document.body.appendChild(link)
      link.click()
      link.remove()
      window.setTimeout(() => URL.revokeObjectURL(url), 60_000)
    } catch (error) {
      setFileError(errorMessage(error))
    } finally {
      setActiveFileId(null)
    }
  }

  function askToDelete(lecture: Lecture) {
    setDeleteError('')
    setLectureToDelete(lecture)
  }

  async function confirmDelete() {
    if (!lectureToDelete) {
      return
    }

    setDeleting(true)
    setDeleteError('')

    try {
      await deleteLecture(lectureToDelete.id)
      setLectures((current) => current.filter((lecture) => lecture.id !== lectureToDelete.id))
      setLectureToDelete(null)
    } catch (error) {
      setDeleteError(errorMessage(error))
    } finally {
      setDeleting(false)
    }
  }

  return (
    <div className="course-section">
      {role === 'instructor' && (
        <LectureUploadForm
          courseId={courseId}
          onUploaded={addUploadedLecture}
        />
      )}

      <div className="course-content-card">
        <h2>
          Lectures
        </h2>

        {lecturesLoading && (
          <p>
            Loading...
          </p>
        )}

        {lecturesError && (
          <p className="form-notice">
            {lecturesError}
          </p>
        )}

        {!lecturesLoading && !lecturesError && lectures.length === 0 && (
          <p>
            No lectures.
          </p>
        )}

        {!lecturesLoading && !lecturesError && lectures.map((lecture) => (
          <div className="resource-row" key={lecture.id}>
            <div>
              <strong>
                {lecture.title}
              </strong>
              <small>
                {lecture.originalFilename}
              </small>
            </div>

            <span className="file-actions">
              <button
                type="button"
                disabled={activeFileId === lecture.id}
                onClick={() => openLectureFile(lecture, false)}
              >
                View
              </button>
              <button
                type="button"
                disabled={activeFileId === lecture.id}
                onClick={() => openLectureFile(lecture, true)}
              >
                Download
              </button>
              {role === 'instructor' && (
                <button
                  className="delete-button"
                  type="button"
                  onClick={() => askToDelete(lecture)}
                >
                  Delete
                </button>
              )}
            </span>
          </div>
        ))}

        {fileError && (
          <p className="form-notice">
            {fileError}
          </p>
        )}
      </div>

      {lectureToDelete && (
        <div className="confirm-backdrop">
          <div className="confirm-dialog">
            <h2>
              Delete lecture
            </h2>
            <p>
              Delete &quot;{lectureToDelete.title}&quot;?
            </p>

            {deleteError && (
              <p className="form-notice">
                {deleteError}
              </p>
            )}

            <div className="confirm-actions">
              <button
                className="cancel-button"
                type="button"
                disabled={deleting}
                onClick={() => setLectureToDelete(null)}
              >
                Cancel
              </button>
              <button
                className="delete-confirm-button"
                type="button"
                disabled={deleting}
                onClick={confirmDelete}
              >
                {deleting ? 'Deleting...' : 'Delete'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}

function AssignmentTab({ courseId, role }: { courseId: number; role: Role }) {
  const [assignments, setAssignments] = useState<Assignment[]>([])
  const [assignmentsLoading, setAssignmentsLoading] = useState(true)
  const [assignmentsError, setAssignmentsError] = useState('')
  const [fileError, setFileError] = useState('')
  const [activeFileId, setActiveFileId] = useState<number | null>(null)
  const [assignmentToDelete, setAssignmentToDelete] = useState<Assignment | null>(null)
  const [deleteError, setDeleteError] = useState('')
  const [deleting, setDeleting] = useState(false)

  useEffect(() => {
    async function loadAssignments() {
      try {
        const assignmentData = await getCourseAssignments(courseId)
        setAssignments(
          assignmentData.sort((first, second) => first.displayOrder - second.displayOrder),
        )
      } catch (error) {
        setAssignmentsError(errorMessage(error))
      } finally {
        setAssignmentsLoading(false)
      }
    }

    void loadAssignments()
  }, [courseId])

  function addUploadedAssignment(assignment: Assignment) {
    setAssignments((current) => (
      [...current, assignment].sort((first, second) => first.displayOrder - second.displayOrder)
    ))
  }

  async function openAssignmentFile(assignment: Assignment, download: boolean) {
    setActiveFileId(assignment.id)
    setFileError('')

    try {
      const file = await getAssignmentFile(assignment.id)
      const url = URL.createObjectURL(file)
      const link = document.createElement('a')

      link.href = url

      if (download) {
        link.download = assignment.originalFilename
      } else {
        link.target = '_blank'
        link.rel = 'noopener noreferrer'
      }

      document.body.appendChild(link)
      link.click()
      link.remove()
      window.setTimeout(() => URL.revokeObjectURL(url), 60_000)
    } catch (error) {
      setFileError(errorMessage(error))
    } finally {
      setActiveFileId(null)
    }
  }

  function askToDelete(assignment: Assignment) {
    setDeleteError('')
    setAssignmentToDelete(assignment)
  }

  async function confirmAssignmentDelete() {
    if (!assignmentToDelete) {
      return
    }

    setDeleting(true)
    setDeleteError('')

    try {
      await deleteAssignment(assignmentToDelete.id)
      setAssignments((current) => current.filter(
        (assignment) => assignment.id !== assignmentToDelete.id,
      ))
      setAssignmentToDelete(null)
    } catch (error) {
      setDeleteError(errorMessage(error))
    } finally {
      setDeleting(false)
    }
  }

  return (
    <div className="course-section">
      {role === 'instructor' && (
        <AssignmentUploadForm
          courseId={courseId}
          onUploaded={addUploadedAssignment}
        />
      )}

      {assignmentsLoading && (
        <div className="course-content-card">
          <p>
            Loading...
          </p>
        </div>
      )}

      {assignmentsError && (
        <div className="course-content-card">
          <p className="form-notice">
            {assignmentsError}
          </p>
        </div>
      )}

      {!assignmentsLoading && !assignmentsError && assignments.length === 0 && (
        <div className="course-content-card">
          <p>
            No assignments.
          </p>
        </div>
      )}

      {!assignmentsLoading && !assignmentsError && assignments.map((assignment) => (
        <div className="course-content-card assignment-card" key={assignment.id}>
          <div className="assignment-header">
            <div>
              <h2>
                {assignment.title}
              </h2>
              <span>
                Maximum grade: {assignment.maxGrade}
              </span>
            </div>

            <span className="file-actions">
              <button
                type="button"
                disabled={activeFileId === assignment.id}
                onClick={() => openAssignmentFile(assignment, false)}
              >
                View
              </button>
              <button
                type="button"
                disabled={activeFileId === assignment.id}
                onClick={() => openAssignmentFile(assignment, true)}
              >
                Download
              </button>
              {role === 'instructor' && (
                <button
                  className="delete-button"
                  type="button"
                  onClick={() => askToDelete(assignment)}
                >
                  Delete
                </button>
              )}
            </span>
          </div>

          {assignment.assignmentInstructions && (
            <p>
              {assignment.assignmentInstructions}
            </p>
          )}

          <small className="resource-filename">
            {assignment.originalFilename}
          </small>

          <AssignmentSubmissions
            assignment={assignment}
            role={role}
          />
        </div>
      ))}

      {fileError && (
        <p className="form-notice">
          {fileError}
        </p>
      )}

      {assignmentToDelete && (
        <div className="confirm-backdrop">
          <div className="confirm-dialog">
            <h2>
              Delete assignment
            </h2>
            <p>
              Delete &quot;{assignmentToDelete.title}&quot;?
            </p>

            {deleteError && (
              <p className="form-notice">
                {deleteError}
              </p>
            )}

            <div className="confirm-actions">
              <button
                className="cancel-button"
                type="button"
                disabled={deleting}
                onClick={() => setAssignmentToDelete(null)}
              >
                Cancel
              </button>
              <button
                className="delete-confirm-button"
                type="button"
                disabled={deleting}
                onClick={confirmAssignmentDelete}
              >
                {deleting ? 'Deleting...' : 'Delete'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}

function AssignmentSubmissions({ assignment, role }: { assignment: Assignment; role: Role }) {
  const [submissions, setSubmissions] = useState<Submission[]>([])
  const [submissionsLoading, setSubmissionsLoading] = useState(true)
  const [submissionsError, setSubmissionsError] = useState('')
  const [fileError, setFileError] = useState('')
  const [activeFileId, setActiveFileId] = useState<number | null>(null)

  useEffect(() => {
    async function loadSubmissions() {
      try {
        const submissionData = role === 'student'
          ? await getMySubmissions(assignment.id)
          : await getAssignmentSubmissions(assignment.id)

        setSubmissions(submissionData)
      } catch (error) {
        setSubmissionsError(errorMessage(error))
      } finally {
        setSubmissionsLoading(false)
      }
    }

    void loadSubmissions()
  }, [assignment.id, role])

  function addSubmittedFile(submission: Submission) {
    setSubmissions((current) => [submission, ...current])
  }

  function updateGradedSubmission(updatedSubmission: Submission) {
    setSubmissions((current) => current.map((submission) => (
      submission.id === updatedSubmission.id ? updatedSubmission : submission
    )))
  }

  async function openSubmissionFile(submission: Submission, download: boolean) {
    setActiveFileId(submission.id)
    setFileError('')

    try {
      const file = await getSubmissionFile(submission.id)
      const url = URL.createObjectURL(file)
      const link = document.createElement('a')

      link.href = url

      if (download) {
        link.download = submission.originalFilename
      } else {
        link.target = '_blank'
        link.rel = 'noopener noreferrer'
      }

      document.body.appendChild(link)
      link.click()
      link.remove()
      window.setTimeout(() => URL.revokeObjectURL(url), 60_000)
    } catch (error) {
      setFileError(errorMessage(error))
    } finally {
      setActiveFileId(null)
    }
  }

  return (
    <>
      {role === 'student' && (
        <SubmissionForm
          assignmentId={assignment.id}
          onSubmitted={addSubmittedFile}
        />
      )}

      <h3>
        {role === 'student' ? 'My submissions' : 'Submissions'}
      </h3>

      {submissionsLoading && (
        <p>
          Loading...
        </p>
      )}

      {submissionsError && (
        <p className="form-notice">
          {submissionsError}
        </p>
      )}

      {!submissionsLoading && !submissionsError && submissions.length === 0 && (
        <p>
          No submissions.
        </p>
      )}

      {!submissionsLoading && !submissionsError && submissions.map((submission) => (
        <div
          className={`submission-row ${role === 'instructor' ? 'instructor-submission' : ''}`}
          key={submission.id}
        >
          <div>
            <strong>
              {role === 'student' ? submission.originalFilename : submission.studentName}
            </strong>
            {role === 'student' ? (
              <span className={getGradePillClass(submission.grade, assignment.maxGrade)}>
                {submission.grade === null
                  ? 'Not graded'
                  : `Grade: ${submission.grade} / ${assignment.maxGrade}`}
              </span>
            ) : (
              <small>
                {submission.originalFilename}
              </small>
            )}
            {role === 'student' && submission.feedback && (
              <p>
                {submission.feedback}
              </p>
            )}
          </div>

          <span className="file-actions">
            <button
              type="button"
              disabled={activeFileId === submission.id}
              onClick={() => openSubmissionFile(submission, false)}
            >
              View
            </button>
            <button
              type="button"
              disabled={activeFileId === submission.id}
              onClick={() => openSubmissionFile(submission, true)}
            >
              Download
            </button>
          </span>

          {role === 'instructor' && (
            <GradeForm
              submission={submission}
              maxGrade={assignment.maxGrade}
              onGraded={updateGradedSubmission}
            />
          )}
        </div>
      ))}

      {fileError && (
        <p className="form-notice">
          {fileError}
        </p>
      )}
    </>
  )
}

function getGradePillClass(grade: number | null, maxGrade: number) {
  if (grade === null) {
    return 'grade-pill grade-ungraded'
  }

  const percentage = maxGrade === 0 ? 0 : (grade / maxGrade) * 100

  if (percentage > 90) {
    return 'grade-pill grade-highest'
  }

  if (percentage >= 80) {
    return 'grade-pill grade-above-average'
  }

  if (percentage >= 70) {
    return 'grade-pill grade-average'
  }

  if (percentage >= 60) {
    return 'grade-pill grade-below-average'
  }

  return 'grade-pill grade-lowest'
}

function AnnouncementTab({ courseId, role }: { courseId: number; role: Role }) {
  const [announcements, setAnnouncements] = useState<Announcement[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [editingAnnouncement, setEditingAnnouncement] = useState<Announcement | null>(null)
  const [announcementToDelete, setAnnouncementToDelete] = useState<Announcement | null>(null)
  const [deleteError, setDeleteError] = useState('')
  const [deleting, setDeleting] = useState(false)

  useEffect(() => {
    async function loadAnnouncements() {
      try {
        const announcementData = await getCourseAnnouncements(courseId)
        setAnnouncements(announcementData)
      } catch (loadError) {
        setError(errorMessage(loadError))
      } finally {
        setLoading(false)
      }
    }

    void loadAnnouncements()
  }, [courseId])

  function saveAnnouncement(savedAnnouncement: Announcement) {
    setAnnouncements((current) => {
      const announcementExists = current.some(
        (announcement) => announcement.id === savedAnnouncement.id,
      )

      if (announcementExists) {
        return current.map((announcement) => (
          announcement.id === savedAnnouncement.id
            ? savedAnnouncement
            : announcement
        ))
      }

      return [savedAnnouncement, ...current]
    })

    setEditingAnnouncement(null)
  }

  function askToDelete(announcement: Announcement) {
    setDeleteError('')
    setAnnouncementToDelete(announcement)
  }

  async function confirmDelete() {
    if (!announcementToDelete) {
      return
    }

    setDeleting(true)
    setDeleteError('')

    try {
      await deleteAnnouncement(announcementToDelete.id)
      setAnnouncements((current) => current.filter(
        (announcement) => announcement.id !== announcementToDelete.id,
      ))

      if (editingAnnouncement?.id === announcementToDelete.id) {
        setEditingAnnouncement(null)
      }

      setAnnouncementToDelete(null)
    } catch (deleteRequestError) {
      setDeleteError(errorMessage(deleteRequestError))
    } finally {
      setDeleting(false)
    }
  }

  return (
    <div className="course-section">
      {role === 'instructor' && (
        <AnnouncementForm
          key={editingAnnouncement?.id ?? 'new-announcement'}
          courseId={courseId}
          announcement={editingAnnouncement ?? undefined}
          onSaved={saveAnnouncement}
          onCancel={editingAnnouncement
            ? () => setEditingAnnouncement(null)
            : undefined}
        />
      )}

      {loading && (
        <div className="course-content-card">
          <p>
            Loading...
          </p>
        </div>
      )}

      {error && (
        <div className="course-content-card">
          <p className="form-notice">
            {error}
          </p>
        </div>
      )}

      {!loading && !error && announcements.length === 0 && (
        <div className="course-content-card">
          <p>
            No announcements.
          </p>
        </div>
      )}

      {!loading && !error && announcements.map((announcement) => (
        <div className="course-content-card announcement-card" key={announcement.id}>
          <div className="announcement-header">
            <div>
              <h2 dir="auto">
                {announcement.title}
              </h2>
              <small>
                {new Date(announcement.createdAt).toLocaleString()}
              </small>
            </div>

            {role === 'instructor' && (
              <span className="file-actions">
                <button
                  type="button"
                  onClick={() => setEditingAnnouncement(announcement)}
                >
                  Edit
                </button>
                <button
                  className="delete-button"
                  type="button"
                  onClick={() => askToDelete(announcement)}
                >
                  Delete
                </button>
              </span>
            )}
          </div>

          <div
            className="announcement-body"
            dir="auto"
            dangerouslySetInnerHTML={{ __html: announcement.body }}
          />
        </div>
      ))}

      {announcementToDelete && (
        <div className="confirm-backdrop">
          <div className="confirm-dialog">
            <h2>
              Delete announcement
            </h2>
            <p>
              Delete &quot;{announcementToDelete.title}&quot;?
            </p>

            {deleteError && (
              <p className="form-notice">
                {deleteError}
              </p>
            )}

            <div className="confirm-actions">
              <button
                className="cancel-button"
                type="button"
                disabled={deleting}
                onClick={() => setAnnouncementToDelete(null)}
              >
                Cancel
              </button>
              <button
                className="delete-confirm-button"
                type="button"
                disabled={deleting}
                onClick={confirmDelete}
              >
                {deleting ? 'Deleting...' : 'Delete'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
