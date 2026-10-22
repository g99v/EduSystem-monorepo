import { Link } from 'react-router-dom'
import type { Course } from '../api/courses'

type CourseGridProps = {
  courses: Course[]
  role: 'student' | 'instructor'
  onEnroll?: (courseId: number) => void
}

export function CourseGrid({ courses, role, onEnroll }: CourseGridProps) {
  if (courses.length === 0) return <p className="empty-state">No courses.</p>

  return (
    <div className="course-grid">
      {courses.map((course) => (
        <div className="course-card" key={course.id}>
          <Link to={`/${role}/courses/${course.id}`} className="course-card-link">
            <h3>{course.name}</h3>
            <p>
              {course.instructorName ?? course.teacherName}
            </p>
          </Link>
          {onEnroll && (
            <button className="small-button" type="button" onClick={() => onEnroll(course.id)}>
              Enroll
            </button>
          )}
        </div>
      ))}
    </div>
  )
}
