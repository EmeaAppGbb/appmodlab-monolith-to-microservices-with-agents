package com.eduverse.assessment.repository;

import com.eduverse.assessment.model.StudentAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentAnswerRepository extends JpaRepository<StudentAnswer, Long> {

    Optional<StudentAnswer> findByAssessmentIdAndStudentId(Long assessmentId, Long studentId);

    List<StudentAnswer> findByStudentId(Long studentId);

    List<StudentAnswer> findByAssessmentId(Long assessmentId);

    boolean existsByAssessmentIdAndStudentId(Long assessmentId, Long studentId);
}
