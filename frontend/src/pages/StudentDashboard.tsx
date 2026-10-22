import { useEffect, useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { getCourseAnnouncements, type Announcement } from '../api/announcements'
import { errorMessage } from '../api/auth'
import { getCourseAssignments, type Assignment } from '../api/assignments'
import {
  enrollInCourse,
  getCourses,
  getMyCourses,
  type Course,
} from '../api/courses'
import { getMySubmissions } from '../api/submissions'
import { CourseGrid } from '../components/CourseGrid'
import { DashboardLayout } from '../components/DashboardLayout'
import { Panel } from '../components/Panel'
import { studentSections } from '../data/navigation'

export function StudentDashboard() {
  const [courses, setCourses] = useState<Course[]>([])
  const [enrolledCourses, setEnrolledCourses] = useState<Course[]>([])
  const [assignments, setAssignments] = useState<Assignment[]>([])
  const [announcements, setAnnouncements] = useState<Announcement[]>([])
  const [submittedAssignmentIds, setSubmittedAssignmentIds] = useState<number[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const navigate = useNavigate()
  const [params, setParams] = useSearchParams()
  const requestedView = params.get('view')
  const activeSection = studentSections.find((item) => item.id === requestedView)?.id ?? 'overview'
  const availableCourses = courses.filter(
    (course) => !enrolledCourses.some(
      (enrolledCourse) => enrolledCourse.id === course.id,
    ),
  )

  useEffect(() => {
    async function loadDashboard() {
      try {
        const allCourses = await getCourses()
        const myCourses = await getMyCourses()
        const loadedAssignments: Assignment[] = []
        const loadedAnnouncements: Announcement[] = []

        for (const course of myCourses) {
          const courseAssignments = await getCourseAssignments(course.id)
          const courseAnnouncements = await getCourseAnnouncements(course.id)

          loadedAssignments.push(...courseAssignments)
          loadedAnnouncements.push(...courseAnnouncements)
        }

        const submittedIds: number[] = []

        for (const assignment of loadedAssignments) {
          const submissions = await getMySubmissions(assignment.id)

          if (submissions.length > 0) {
            submittedIds.push(assignment.id)
          }
        }

        setCourses(allCourses)
        setEnrolledCourses(myCourses)
        setAssignments(loadedAssignments)
        setAnnouncements(loadedAnnouncements.sort(
          (first, second) => Date.parse(second.createdAt) - Date.parse(first.createdAt),
        ))
        setSubmittedAssignmentIds(submittedIds)
      } catch (loadError) {
        setError(errorMessage(loadError))
      } finally {
        setLoading(false)
      }
    }

    void loadDashboard()
  }, [])

  async function enrollAndOpen(courseId: number) {
    try {
      setError('')
      await enrollInCourse(courseId)

      const updatedCourses = await getMyCourses()
      setEnrolledCourses(updatedCourses)

      navigate(`/student/courses/${courseId}`)
    } catch (enrollmentError) {
      setError(errorMessage(enrollmentError))
    }
  }

  function showSection(section: string) {
    if (section === 'overview') setParams({})
    else setParams({ view: section })
  }

  return (
    <DashboardLayout
      sections={studentSections}
      activeSection={activeSection}
      onSectionChange={showSection}
    >
      <div className="page-heading">
        <h1>
          {activeSection === 'overview'
            ? 'Student Dashboard'
            : studentSections.find((item) => item.id === activeSection)?.label}
        </h1>
      </div>

      {loading && (
        <p className="empty-state">
          Loading...
        </p>
      )}

      {error && (
        <p className="form-notice">
          {error}
        </p>
      )}

      {!loading && activeSection === 'overview' && (
        <>
          <div className="two-column">
            <Panel title="Assignments">
              {assignments.length === 0 ? (
                <p className="empty-state">
                  No assignments.
                </p>
              ) : (
                <div className="item-list">
                  {assignments.slice(0, 4).map((assignment) => {
                    const submitted = submittedAssignmentIds.includes(assignment.id)

                    return (
                      <Link
                        className="list-item item-link"
                        to={`/student/courses/${assignment.courseId}?tab=assignments`}
                        key={assignment.id}
                      >
                        <span className="item-content">
                          <strong>
                            {assignment.title}
                          </strong>
                          <small>
                            {assignment.courseName}
                          </small>
                        </span>
                        <span className={`status ${submitted ? 'complete' : ''}`}>
                          {submitted ? 'Submitted' : 'To do'}
                        </span>
                      </Link>
                    )
                  })}
                </div>
              )}
            </Panel>

            <Panel title="Announcements">
              {announcements.length === 0 ? (
                <p className="empty-state">
                  No announcements.
                </p>
              ) : (
                <div className="item-list">
                  {announcements.slice(0, 4).map((announcement) => (
                    <Link
                      className="list-item item-link"
                      to={`/student/courses/${announcement.courseId}?tab=announcements`}
                      key={announcement.id}
                    >
                      <span className="item-content">
                        <strong dir="auto">
                          {announcement.title}
                        </strong>
                        <small>
                          {announcement.courseName}
                        </small>
                      </span>
                    </Link>
                  ))}
                </div>
              )}
            </Panel>
          </div>

          <Panel title="Courses" className="course-panel">
            <CourseGrid courses={enrolledCourses} role="student" />
          </Panel>
        </>
      )}

      {!loading && activeSection === 'courses' && (
        <Panel title="My courses">
          <CourseGrid courses={enrolledCourses} role="student" />
        </Panel>
      )}

      {!loading && activeSection === 'enroll' && (
        <Panel title="Available courses">
          <CourseGrid
            courses={availableCourses}
            role="student"
            onEnroll={enrollAndOpen}
          />
        </Panel>
      )}

      {activeSection === 'profile' && (
        <Panel title="Profile">
          <p className="empty-state">
            No profile data yet.
          </p>
        </Panel>
      )}
    </DashboardLayout>
  )
}
