package com.springboot.EduSystem.lecture;

import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class LectureMapper {
    public LectureDTO toLectureDTO(Lecture lecture) {
        Objects.requireNonNull(lecture, "Lecture should not be null");

        return new LectureDTO(
                lecture.getId(),
                lecture.getCourse().getId(),
                lecture.getCourse().getName(),
                lecture.getTitle(),
                lecture.getOriginalFilename(),
                lecture.getContentType(),
                lecture.getFileSize(),
                lecture.getDisplayOrder(),
                lecture.getUploadedAt()
        );
    }

    public void updateLecture(Lecture lecture, LectureUpdateDTO dto) {
        Objects.requireNonNull(lecture, "Lecture should not be null");
        Objects.requireNonNull(dto, "LectureUpdateDTO should not be null");

        lecture.setTitle(dto.title().trim());
        lecture.setDisplayOrder(dto.displayOrder());
    }
}
