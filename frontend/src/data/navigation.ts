import {
  Bell,
  BookOpen,
  CirclePlus,
  ClipboardList,
  LayoutDashboard,
  UserRound,
} from 'lucide-react'
import type { DashboardSection } from '../components/DashboardLayout'

export const studentSections: DashboardSection[] = [
  { id: 'overview', label: 'Overview', icon: LayoutDashboard },
  { id: 'courses', label: 'Courses', icon: BookOpen },
  { id: 'enroll', label: 'Enroll in courses', icon: CirclePlus },
  { id: 'profile', label: 'Profile', icon: UserRound },
]

export const instructorSections: DashboardSection[] = [
  { id: 'overview', label: 'Overview', icon: LayoutDashboard },
  { id: 'courses', label: 'Courses', icon: BookOpen },
  { id: 'assignments', label: 'Assignments', icon: ClipboardList },
  { id: 'announcements', label: 'Announcements', icon: Bell },
]
