package com.eduverse.repository;

import com.eduverse.model.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {

    List<Course> findByStatus(Course.Status status);

    List<Course> findByInstructorId(Long instructorId);

    List<Course> findByCategory(String category);
}
