package com.eduverse.progress.repository;

import com.eduverse.progress.model.Progress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProgressRepository extends JpaRepository<Progress, Long> {

    List<Progress> findByEnrollmentId(Long enrollmentId);

    Optional<Progress> findByEnrollmentIdAndLessonId(Long enrollmentId, Long lessonId);

    List<Progress> findByStudentId(Long studentId);

    List<Progress> findByEnrollmentIdAndCompleted(Long enrollmentId, boolean completed);

    long countByEnrollmentId(Long enrollmentId);

    long countByEnrollmentIdAndCompleted(Long enrollmentId, boolean completed);
}
