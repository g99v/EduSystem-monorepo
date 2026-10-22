BEGIN;

DROP TABLE IF EXISTS public.announcement;
DROP TABLE IF EXISTS public.assignment_submission;
DROP TABLE IF EXISTS public.assignment;
DROP TABLE IF EXISTS public.lecture;
DROP TABLE IF EXISTS public.enrollment;
DROP TABLE IF EXISTS public.student_profile;
DROP TABLE IF EXISTS public.course;
DROP TABLE IF EXISTS public.t_student;
DROP TABLE IF EXISTS public.t_instructor;
DROP TABLE IF EXISTS public.school;
DROP TABLE IF EXISTS public.t_user;

DROP SEQUENCE IF EXISTS public.announcement_seq;
DROP SEQUENCE IF EXISTS public.assignment_submission_seq;
DROP SEQUENCE IF EXISTS public.assignment_seq;
DROP SEQUENCE IF EXISTS public.lecture_seq;
DROP SEQUENCE IF EXISTS public.enrollment_seq;
DROP SEQUENCE IF EXISTS public.student_profile_seq;
DROP SEQUENCE IF EXISTS public.course_seq;
DROP SEQUENCE IF EXISTS public.t_student_seq;
DROP SEQUENCE IF EXISTS public.t_instructor_seq;
DROP SEQUENCE IF EXISTS public.school_seq;
DROP SEQUENCE IF EXISTS public.t_user_seq;

DELETE FROM public.databasechangelog;

COMMIT;