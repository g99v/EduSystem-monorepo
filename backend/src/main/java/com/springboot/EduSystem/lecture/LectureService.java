package com.springboot.EduSystem.lecture;

import java.util.List;

import org.springframework.core.io.Resource;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.springboot.EduSystem.course.Course;
import com.springboot.EduSystem.course.CourseRepository;
import com.springboot.EduSystem.exception.CourseNotFoundException;
import com.springboot.EduSystem.exception.LectureNotFoundException;
import com.springboot.EduSystem.security.CourseAccessPolicy;
import com.springboot.EduSystem.storage.DocumentFileValidator;
import com.springboot.EduSystem.storage.FileStorage;

@Service
@Transactional
public class LectureService {
    private static final String LECTURE_STORAGE_PREFIX = "lectures";

    private final LectureRepository lectureRepository;
    private final LectureMapper lectureMapper;
    private final CourseRepository courseRepository;
    private final FileStorage fileStorage;
    private final DocumentFileValidator documentFileValidator;
    private final CourseAccessPolicy accessPolicy;

    public LectureService(
            LectureRepository lectureRepository,
            LectureMapper lectureMapper,
            CourseRepository courseRepository,
            FileStorage fileStorage,
            DocumentFileValidator documentFileValidator,
            CourseAccessPolicy accessPolicy
    ) {
        this.lectureRepository = lectureRepository;
        this.lectureMapper = lectureMapper;
        this.courseRepository = courseRepository;
        this.fileStorage = fileStorage;
        this.documentFileValidator = documentFileValidator;
        this.accessPolicy = accessPolicy;
    }

    public LectureDTO upload(
            Integer courseId,
            String title,
            Integer displayOrder,
            MultipartFile file,
            Authentication authentication
    ) {
        Course course = findCourse(courseId);
        accessPolicy.requireCourseManagement(course, authentication);

        DocumentFileValidator.ValidatedFile validatedFile =
                documentFileValidator.validate(file);
        FileStorage.StoredFile storedFile =
                fileStorage.store(file, LECTURE_STORAGE_PREFIX);

        try {
            Lecture lecture = new Lecture(
                    course,
                    title.trim(),
                    validatedFile.originalFilename(),
                    storedFile.storageKey(),
                    validatedFile.contentType(),
                    storedFile.fileSize(),
                    displayOrder
            );
            Lecture savedLecture = lectureRepository.saveAndFlush(lecture);
            return lectureMapper.toLectureDTO(savedLecture);
        } catch (RuntimeException databaseException) {
            try {
                fileStorage.delete(storedFile.storageKey());
            } catch (RuntimeException cleanupException) {
                databaseException.addSuppressed(cleanupException);
            }
            throw databaseException;
        }
    }

    @Transactional(readOnly = true)
    public List<LectureDTO> findAll(Integer courseId, Authentication authentication) {
        List<Lecture> lectures;

        if (courseId == null) {
            accessPolicy.requireAdmin(authentication);
            lectures = lectureRepository.findAllOrdered();
        } else {
            Course course = findCourse(courseId);
            accessPolicy.requireCourseReadAccess(course, authentication);
            lectures = lectureRepository.findAllByCourseIdOrdered(courseId);
        }

        return lectures.stream()
                .map(lectureMapper::toLectureDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public LectureDTO findById(Integer id, Authentication authentication) {
        Lecture lecture = findLecture(id);
        accessPolicy.requireCourseReadAccess(lecture.getCourse(), authentication);
        return lectureMapper.toLectureDTO(lecture);
    }

    @Transactional(readOnly = true)
    public LectureDownload download(Integer id, Authentication authentication) {
        Lecture lecture = findLecture(id);
        accessPolicy.requireCourseReadAccess(lecture.getCourse(), authentication);

        Resource resource = fileStorage.load(lecture.getStorageKey());
        return new LectureDownload(
                resource,
                lecture.getOriginalFilename(),
                lecture.getContentType(),
                lecture.getFileSize()
        );
    }

    public LectureDTO update(Integer id, LectureUpdateDTO dto, Authentication authentication) {
        Lecture lecture = findLecture(id);
        accessPolicy.requireCourseManagement(lecture.getCourse(), authentication);

        lectureMapper.updateLecture(lecture, dto);
        Lecture savedLecture = lectureRepository.save(lecture);
        return lectureMapper.toLectureDTO(savedLecture);
    }

    public LectureDTO delete(Integer id, Authentication authentication) {
        Lecture lecture = findLecture(id);
        accessPolicy.requireCourseManagement(lecture.getCourse(), authentication);

        LectureDTO deletedLecture = lectureMapper.toLectureDTO(lecture);
        String storageKey = lecture.getStorageKey();

        lectureRepository.delete(lecture);
        lectureRepository.flush();
        fileStorage.delete(storageKey);

        return deletedLecture;
    }

    private Lecture findLecture(Integer id) {
        return lectureRepository.findOneById(id)
                .orElseThrow(() -> new LectureNotFoundException(id));
    }

    private Course findCourse(Integer id) {
        return courseRepository.findById(id)
                .orElseThrow(() -> new CourseNotFoundException(id));
    }

    public record LectureDownload(
            Resource resource,
            String originalFilename,
            String contentType,
            long fileSize
    ) {
    }
}
