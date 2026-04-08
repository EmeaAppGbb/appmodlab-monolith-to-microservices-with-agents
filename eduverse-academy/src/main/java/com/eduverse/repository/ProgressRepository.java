package com.eduverse.repository;

import com.eduverse.model.Progress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProgressRepository extends JpaRepository<Progress, Long> {

    List<Progress> findByEnrollmentId(Long enrollmentId);

    Optional<Progress> findByStudentIdAndLessonId(Long studentId, Long lessonId);

    long countByEnrollmentIdAndCompletedTrue(Long enrollmentId);
}
