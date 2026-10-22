package com.springboot.EduSystem.announcement;

import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.springboot.EduSystem.course.Course;
import com.springboot.EduSystem.course.CourseRepository;
import com.springboot.EduSystem.exception.AnnouncementNotFoundException;
import com.springboot.EduSystem.exception.CourseNotFoundException;
import com.springboot.EduSystem.security.CourseAccessPolicy;

@Service
@Transactional
public class AnnouncementService {
    private static final Safelist ANNOUNCEMENT_SAFELIST =
            Safelist.relaxed().removeTags("img");

    private final AnnouncementRepository announcementRepository;
    private final AnnouncementMapper announcementMapper;
    private final CourseRepository courseRepository;
    private final CourseAccessPolicy accessPolicy;

    public AnnouncementService(
            AnnouncementRepository announcementRepository,
            AnnouncementMapper announcementMapper,
            CourseRepository courseRepository,
            CourseAccessPolicy accessPolicy
    ) {
        this.announcementRepository = announcementRepository;
        this.announcementMapper = announcementMapper;
        this.courseRepository = courseRepository;
        this.accessPolicy = accessPolicy;
    }

    public AnnouncementDTO create(
            Integer courseId,
            AnnouncementRequest request,
            Authentication authentication
    ) {
        Course course = findCourse(courseId);
        accessPolicy.requireCourseManagement(course, authentication);

        String sanitizedBody = sanitizeBody(request.body());

        Announcement announcement = new Announcement(
                course,
                request.title().trim(),
                sanitizedBody
        );

        Announcement savedAnnouncement = announcementRepository.saveAndFlush(announcement);
        return announcementMapper.toAnnouncementDTO(savedAnnouncement);
    }

    @Transactional(readOnly = true)
    public List<AnnouncementDTO> findByCourse(
            Integer courseId,
            Authentication authentication
    ) {
        Course course = findCourse(courseId);
        accessPolicy.requireCourseReadAccess(course, authentication);

        return announcementRepository.findAllByCourseIdOrdered(courseId).stream()
                .map(announcementMapper::toAnnouncementDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public AnnouncementDTO findById(Integer id, Authentication authentication) {
        Announcement announcement = findAnnouncement(id);
        accessPolicy.requireCourseReadAccess(announcement.getCourse(), authentication);
        return announcementMapper.toAnnouncementDTO(announcement);
    }

    public AnnouncementDTO update(
            Integer id,
            AnnouncementRequest request,
            Authentication authentication
    ) {
        Announcement announcement = findAnnouncement(id);
        accessPolicy.requireCourseManagement(announcement.getCourse(), authentication);

        announcement.setTitle(request.title().trim());
        announcement.setBody(sanitizeBody(request.body()));

        Announcement savedAnnouncement = announcementRepository.save(announcement);
        return announcementMapper.toAnnouncementDTO(savedAnnouncement);
    }

    public AnnouncementDTO delete(Integer id, Authentication authentication) {
        Announcement announcement = findAnnouncement(id);
        accessPolicy.requireCourseManagement(announcement.getCourse(), authentication);

        AnnouncementDTO deletedAnnouncement = announcementMapper.toAnnouncementDTO(announcement);
        announcementRepository.delete(announcement);
        return deletedAnnouncement;
    }

    private Announcement findAnnouncement(Integer id) {
        return announcementRepository.findOneById(id)
                .orElseThrow(() -> new AnnouncementNotFoundException(id));
    }

    private Course findCourse(Integer id) {
        return courseRepository.findById(id)
                .orElseThrow(() -> new CourseNotFoundException(id));
    }

    private String sanitizeBody(String body) {
        String sanitizedBody = Jsoup.clean(body, ANNOUNCEMENT_SAFELIST);
        String visibleText = Jsoup.parse(sanitizedBody).text();

        if (visibleText.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Announcement body must not be empty"
            );
        }

        return sanitizedBody;
    }
}
