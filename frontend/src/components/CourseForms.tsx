import axios from 'axios'
import { useState, type SubmitEvent } from 'react'
import ReactQuill from 'react-quill-new'
import 'react-quill-new/dist/quill.snow.css'
import {
  createAnnouncement,
  updateAnnouncement,
  type Announcement,
} from '../api/announcements'
import { errorMessage } from '../api/auth'
import { uploadAssignment, type Assignment } from '../api/assignments'
import { uploadLecture, type Lecture } from '../api/lectures'
import {
  gradeSubmission,
  uploadSubmission,
  type Submission,
} from '../api/submissions'

const accept = '.pdf,.ppt,.pptx'

const announcementModules = {
  toolbar: [
    [{ header: [1, 2, 3, false] }],
    ['bold', 'italic', 'underline'],
    [{ list: 'ordered' }, { list: 'bullet' }],
    ['link'],
    ['clean'],
  ],
}

const announcementFormats = [
  'header',
  'bold',
  'italic',
  'underline',
  'list',
  'link',
]

function getValidFile(form: HTMLFormElement): File | string {
  const input = form.elements.namedItem('file') as HTMLInputElement | null
  const file = input?.files?.[0]
  if (!file) return 'Choose a file.'
  if (!/\.(pdf|ppt|pptx)$/i.test(file.name)) return 'Use a PDF, PPT, or PPTX file.'
  if (file.size > 50 * 1024 * 1024) return 'The file must be 50 MB or smaller.'
  return file
}

type LectureUploadFormProps = {
  courseId: number
  onUploaded: (lecture: Lecture) => void
}

export function LectureUploadForm({ courseId, onUploaded }: LectureUploadFormProps) {
  const [message, setMessage] = useState('')
  const [uploading, setUploading] = useState(false)

  async function handleSubmit(event: SubmitEvent<HTMLFormElement>) {
    event.preventDefault()
    const form = event.currentTarget
    const file = getValidFile(form)
    if (typeof file === 'string') return setMessage(file)
    const title = (form.elements.namedItem('title') as HTMLInputElement).value.trim()
    if (!title) return setMessage('Enter a title.')
    const displayOrder = Number((form.elements.namedItem('displayOrder') as HTMLInputElement).value)
    if (!Number.isInteger(displayOrder) || displayOrder < 0) {
      return setMessage('Enter a display order of 0 or greater.')
    }

    setUploading(true)
    setMessage('')

    try {
      const lecture = await uploadLecture(courseId, title, displayOrder, file)
      onUploaded(lecture)
      form.reset()
      setMessage('Lecture uploaded.')
    } catch (error) {
      setMessage(errorMessage(error))
    } finally {
      setUploading(false)
    }
  }

  return (
    <form className="course-form" onSubmit={handleSubmit}>
      <h2>Upload lecture</h2>
      <label htmlFor="lecture-title">Title</label>
      <input id="lecture-title" name="title" maxLength={200} required />
      <label htmlFor="lecture-order">Display order</label>
      <input id="lecture-order" name="displayOrder" type="number" min={0} step={1} defaultValue={0} required />
      <label htmlFor="lecture-file">File</label>
      <input id="lecture-file" name="file" type="file" accept={accept} required />
      <button className="small-button" type="submit" disabled={uploading}>
        {uploading ? 'Uploading...' : 'Upload'}
      </button>
      {message && (
        <span className="form-message">
          {message}
        </span>
      )}
    </form>
  )
}

type AssignmentUploadFormProps = {
  courseId: number
  onUploaded: (assignment: Assignment) => void
}

export function AssignmentUploadForm({ courseId, onUploaded }: AssignmentUploadFormProps) {
  const [message, setMessage] = useState('')
  const [uploading, setUploading] = useState(false)

  async function handleSubmit(event: SubmitEvent<HTMLFormElement>) {
    event.preventDefault()
    const form = event.currentTarget
    const file = getValidFile(form)
    if (typeof file === 'string') return setMessage(file)
    const title = (form.elements.namedItem('title') as HTMLInputElement).value.trim()
    const instructions = (form.elements.namedItem('instructions') as HTMLTextAreaElement).value.trim()
    const maxGrade = Number((form.elements.namedItem('maxGrade') as HTMLInputElement).value)
    const displayOrder = Number((form.elements.namedItem('displayOrder') as HTMLInputElement).value)

    if (!title || !Number.isInteger(maxGrade) || maxGrade < 0 || maxGrade > 25) {
      return setMessage('Enter a title and a grade from 0 to 25.')
    }

    if (!Number.isInteger(displayOrder) || displayOrder < 0) {
      return setMessage('Enter a display order of 0 or greater.')
    }

    setUploading(true)
    setMessage('')

    try {
      const assignment = await uploadAssignment(
        courseId,
        title,
        instructions,
        maxGrade,
        displayOrder,
        file,
      )
      onUploaded(assignment)
      form.reset()
      setMessage('Assignment uploaded.')
    } catch (error) {
      setMessage(errorMessage(error))
    } finally {
      setUploading(false)
    }
  }

  return (
    <form className="course-form" onSubmit={handleSubmit}>
      <h2>Upload assignment</h2>
      <label htmlFor="assignment-title">Title</label>
      <input id="assignment-title" name="title" maxLength={200} required />
      <label htmlFor="assignment-instructions">Instructions</label>
      <textarea id="assignment-instructions" name="instructions" maxLength={10000} rows={3} />
      <label htmlFor="assignment-grade">Maximum grade</label>
      <input id="assignment-grade" name="maxGrade" type="number" min={0} max={25} step={1} required />
      <label htmlFor="assignment-order">Display order</label>
      <input id="assignment-order" name="displayOrder" type="number" min={0} step={1} defaultValue={0} required />
      <label htmlFor="assignment-file">File</label>
      <input id="assignment-file" name="file" type="file" accept={accept} required />
      <button className="small-button" type="submit" disabled={uploading}>
        {uploading ? 'Uploading...' : 'Upload'}
      </button>
      {message && (
        <span className="form-message">
          {message}
        </span>
      )}
    </form>
  )
}

type SubmissionFormProps = {
  assignmentId: number
  onSubmitted: (submission: Submission) => void
}

export function SubmissionForm({ assignmentId, onSubmitted }: SubmissionFormProps) {
  const [message, setMessage] = useState('')
  const [uploading, setUploading] = useState(false)

  async function handleSubmit(event: SubmitEvent<HTMLFormElement>) {
    event.preventDefault()
    const form = event.currentTarget
    const file = getValidFile(form)
    if (typeof file === 'string') return setMessage(file)

    setUploading(true)
    setMessage('')

    try {
      const submission = await uploadSubmission(assignmentId, file)
      onSubmitted(submission)
      form.reset()
      setMessage('Submission uploaded.')
    } catch (error) {
      setMessage(errorMessage(error))
    } finally {
      setUploading(false)
    }
  }

  return (
    <form className="submission-form" onSubmit={handleSubmit}>
      <label htmlFor={`submission-${assignmentId}`}>Submit solution</label>
      <input id={`submission-${assignmentId}`} name="file" type="file" accept={accept} required />
      <button className="small-button" type="submit" disabled={uploading}>
        {uploading ? 'Submitting...' : 'Submit'}
      </button>
      {message && (
        <span className="form-message">
          {message}
        </span>
      )}
    </form>
  )
}

type GradeFormProps = {
  submission: Submission
  maxGrade: number
  onGraded: (submission: Submission) => void
}

export function GradeForm({ submission, maxGrade, onGraded }: GradeFormProps) {
  const [grade, setGrade] = useState(submission.grade?.toString() ?? '')
  const [feedback, setFeedback] = useState(submission.feedback ?? '')
  const [message, setMessage] = useState('')
  const [saving, setSaving] = useState(false)

  async function handleSubmit(event: SubmitEvent<HTMLFormElement>) {
    event.preventDefault()
    const value = Number(grade)
    if (!Number.isInteger(value) || value < 0 || value > maxGrade) {
      return setMessage(`Enter a grade from 0 to ${maxGrade}.`)
    }

    setSaving(true)
    setMessage('')

    try {
      const updatedSubmission = await gradeSubmission(
        submission.id,
        value,
        feedback.trim() || null,
      )
      onGraded(updatedSubmission)
      setMessage('Grade saved.')
    } catch (error) {
      const gradeError = axios.isAxiosError(error) && error.response?.data?.grade
      setMessage(typeof gradeError === 'string' ? gradeError : errorMessage(error))
    } finally {
      setSaving(false)
    }
  }

  return (
    <form className="grade-form" onSubmit={handleSubmit}>
      <label htmlFor={`grade-${submission.id}`}>Grade</label>
      <input
        id={`grade-${submission.id}`}
        type="number"
        min={0}
        max={maxGrade}
        step={1}
        value={grade}
        onChange={(event) => setGrade(event.target.value)}
        required
      />
      <span>/ {maxGrade}</span>
      <label htmlFor={`feedback-${submission.id}`}>Feedback</label>
      <input
        id={`feedback-${submission.id}`}
        maxLength={10000}
        value={feedback}
        onChange={(event) => setFeedback(event.target.value)}
      />
      <button className="small-button" type="submit" disabled={saving}>
        {saving ? 'Saving...' : 'Save grade'}
      </button>
      {message && (
        <span className="form-message">
          {message}
        </span>
      )}
    </form>
  )
}

type AnnouncementFormProps = {
  courseId: number
  announcement?: Announcement
  onSaved: (announcement: Announcement) => void
  onCancel?: () => void
}

export function AnnouncementForm({
  courseId,
  announcement,
  onSaved,
  onCancel,
}: AnnouncementFormProps) {
  const [body, setBody] = useState(announcement?.body ?? '')
  const [message, setMessage] = useState('')
  const [saving, setSaving] = useState(false)
  let submitLabel = announcement ? 'Save changes' : 'Add'

  if (saving) {
    submitLabel = 'Saving...'
  }

  async function handleSubmit(event: SubmitEvent<HTMLFormElement>) {
    event.preventDefault()
    const form = event.currentTarget
    const title = (form.elements.namedItem('title') as HTMLInputElement).value.trim()
    const visibleBody = body
      .replace(/<[^>]*>/g, '')
      .replace(/&nbsp;/g, ' ')
      .trim()

    if (!title) {
      return setMessage('Enter a title.')
    }

    if (!visibleBody) {
      return setMessage('Enter a message.')
    }

    if (body.length > 50000) {
      return setMessage('The message is too long.')
    }

    setSaving(true)
    setMessage('')

    try {
      const savedAnnouncement = announcement
        ? await updateAnnouncement(announcement.id, title, body)
        : await createAnnouncement(courseId, title, body)

      onSaved(savedAnnouncement)

      if (!announcement) {
        form.reset()
        setBody('')
        setMessage('Announcement added.')
      }
    } catch (error) {
      setMessage(errorMessage(error))
    } finally {
      setSaving(false)
    }
  }

  return (
    <form className="course-form announcement-form" onSubmit={handleSubmit}>
      <h2>
        {announcement ? 'Edit announcement' : 'Add announcement'}
      </h2>

      <label htmlFor="announcement-title">
        Title
      </label>
      <input
        id="announcement-title"
        name="title"
        dir="auto"
        maxLength={200}
        defaultValue={announcement?.title}
        required
      />

      <label>
        Message
      </label>
      <div className="announcement-editor" dir="auto">
        <ReactQuill
          theme="snow"
          value={body}
          modules={announcementModules}
          formats={announcementFormats}
          useSemanticHTML
          onChange={setBody}
        />
      </div>

      <div className="announcement-form-actions">
        {onCancel && (
          <button
            className="cancel-button"
            type="button"
            disabled={saving}
            onClick={onCancel}
          >
            Cancel
          </button>
        )}
        <button className="small-button" type="submit" disabled={saving}>
          {submitLabel}
        </button>
      </div>

      {message && (
        <span className="form-message">
          {message}
        </span>
      )}
    </form>
  )
}
