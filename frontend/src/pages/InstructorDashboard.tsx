import { useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { getCourseAnnouncements, type Announcement } from '../api/announcements'
import { errorMessage } from '../api/auth'
import { getCourseAssignments, type Assignment } from '../api/assignments'
import { getMyCourses, type Course } from '../api/courses'
import { getAssignmentSubmissions } from '../api/submissions'
import { CourseGrid } from '../components/CourseGrid'
import { DashboardLayout } from '../components/DashboardLayout'
import { Panel } from '../components/Panel'
import { instructorSections } from '../data/navigation'

export function InstructorDashboard() {
  const [courses, setCourses] = useState<Course[]>([])
  const [assignments, setAssignments] = useState<Assignment[]>([])
  const [announcements, setAnnouncements] = useState<Announcement[]>([])
  const [submissionCounts, setSubmissionCounts] = useState<Record<number, number>>({})
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [params, setParams] = useSearchParams()
  const requestedView = params.get('view')
  const activeSection = instructorSections.find((item) => item.id === requestedView)?.id ?? 'overview'

  useEffect(() => {
    async function loadDashboard() {
      try {
        const myCourses = await getMyCourses()
        const loadedAssignments: Assignment[] = []
        const loadedAnnouncements: Announcement[] = []

        for (const course of myCourses) {
          const courseAssignments = await getCourseAssignments(course.id)
          const courseAnnouncements = await getCourseAnnouncements(course.id)

          loadedAssignments.push(...courseAssignments)
          loadedAnnouncements.push(...courseAnnouncements)
        }

        const counts: Record<number, number> = {}

        for (const assignment of loadedAssignments) {
          const submissions = await getAssignmentSubmissions(assignment.id)
          counts[assignment.id] = submissions.length
        }

        setCourses(myCourses)
        setAssignments(loadedAssignments)
        setAnnouncements(loadedAnnouncements.sort(
          (first, second) => Date.parse(second.createdAt) - Date.parse(first.createdAt),
        ))
        setSubmissionCounts(counts)
      } catch (loadError) {
        setError(errorMessage(loadError))
      } finally {
        setLoading(false)
      }
    }

    void loadDashboard()
  }, [])

  function showSection(section: string) {
    if (section === 'overview') setParams({})
    else setParams({ view: section })
  }

  return (
    <DashboardLayout
      sections={instructorSections}
      activeSection={activeSection}
      onSectionChange={showSection}
    >
      <div className="page-heading">
        <h1>
          {activeSection === 'overview'
            ? 'Instructor Dashboard'
            : instructorSections.find((item) => item.id === activeSection)?.label}
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

      {!loading && (activeSection === 'overview' || activeSection === 'courses') && (
        <Panel title="Courses" className="course-panel">
          <CourseGrid courses={courses} role="instructor" />
        </Panel>
      )}

      {!loading && (
        activeSection === 'overview'
        || activeSection === 'assignments'
        || activeSection === 'announcements'
      ) && (
        <div className={activeSection === 'overview' ? 'two-column dashboard-lists' : 'single-column'}>
          {(activeSection === 'overview' || activeSection === 'assignments') && (
            <Panel title="Assignments">
              {assignments.length === 0 ? (
                <p className="empty-state">
                  No assignments.
                </p>
              ) : (
                <div className="item-list">
                  {assignments
                    .slice(0, activeSection === 'overview' ? 4 : assignments.length)
                    .map((assignment) => (
                      <Link
                        className="list-item item-link"
                        to={`/instructor/courses/${assignment.courseId}?tab=assignments`}
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
                        <span className="item-time">
                          {submissionCounts[assignment.id] ?? 0} submissions
                        </span>
                      </Link>
                    ))}
                </div>
              )}
            </Panel>
          )}

          {(activeSection === 'overview' || activeSection === 'announcements') && (
            <Panel title="Announcements">
              {announcements.length === 0 ? (
                <p className="empty-state">
                  No announcements.
                </p>
              ) : (
                <div className="item-list">
                  {announcements
                    .slice(0, activeSection === 'overview' ? 4 : announcements.length)
                    .map((announcement) => (
                      <Link
                        className="list-item item-link"
                        to={`/instructor/courses/${announcement.courseId}?tab=announcements`}
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
          )}
        </div>
      )}
    </DashboardLayout>
  )
}
