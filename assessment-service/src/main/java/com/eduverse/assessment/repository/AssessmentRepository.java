package com.eduverse.assessment.repository;

import com.eduverse.assessment.model.Assessment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AssessmentRepository extends JpaRepository<Assessment, Long> {

    Optional<Assessment> findByLessonId(Long lessonId);

    List<Assessment> findByCourseId(Long courseId);

    List<Assessment> findByType(Assessment.Type type);
}
