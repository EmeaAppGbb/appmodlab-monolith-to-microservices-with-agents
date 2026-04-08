package com.eduverse.repository;

import com.eduverse.model.StudentAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StudentAnswerRepository extends JpaRepository<StudentAnswer, Long> {

    List<StudentAnswer> findByAssessmentIdAndStudentId(Long assessmentId, Long studentId);

    List<StudentAnswer> findByStudentId(Long studentId);
}
