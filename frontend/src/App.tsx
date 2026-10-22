import { Navigate, Route, Routes } from 'react-router-dom'
import { LoginPage } from './pages/LoginPage'
import { RegisterPage } from './pages/RegisterPage'
import { StudentDashboard } from './pages/StudentDashboard'
import { InstructorDashboard } from './pages/InstructorDashboard'
import { CoursePage } from './pages/CoursePage'
import { GuestRoute, RequireRole } from './auth/RouteGuards'
import { UnsupportedRolePage } from './pages/UnsupportedRolePage'

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<Navigate to="/login" replace />} />
      <Route
        path="/login"
        element={
          <GuestRoute>
            <LoginPage />
          </GuestRoute>
        }
      />
      <Route
        path="/register"
        element={
          <GuestRoute>
            <RegisterPage />
          </GuestRoute>
        }
      />
      <Route
        path="/student"
        element={
          <RequireRole role="STUDENT">
            <StudentDashboard />
          </RequireRole>
        }
      />
      <Route
        path="/student/courses/:courseId"
        element={
          <RequireRole role="STUDENT">
            <CoursePage role="student" />
          </RequireRole>
        }
      />
      <Route
        path="/instructor"
        element={
          <RequireRole role="INSTRUCTOR">
            <InstructorDashboard />
          </RequireRole>
        }
      />
      <Route
        path="/instructor/courses/:courseId"
        element={
          <RequireRole role="INSTRUCTOR">
            <CoursePage role="instructor" />
          </RequireRole>
        }
      />
      <Route
        path="/unsupported-role"
        element={
          <RequireRole role="ADMIN">
            <UnsupportedRolePage />
          </RequireRole>
        }
      />
      <Route path="*" element={<Navigate to="/login" replace />} />
    </Routes>
  )
}
