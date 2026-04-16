package com.eduverse.enrollment.repository;

import com.eduverse.enrollment.model.Enrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    List<Enrollment> findByStudentId(Long studentId);

    List<Enrollment> findByCourseId(Long courseId);

    Optional<Enrollment> findByStudentIdAndCourseId(Long studentId, Long courseId);

    List<Enrollment> findByStudentIdAndStatus(Long studentId, Enrollment.Status status);

    List<Enrollment> findByStatus(Enrollment.Status status);

    boolean existsByStudentIdAndCourseId(Long studentId, Long courseId);
}
