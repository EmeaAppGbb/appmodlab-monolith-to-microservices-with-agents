package com.eduverse.catalog.repository;

import com.eduverse.catalog.model.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {

    List<Course> findByStatus(Course.Status status);

    List<Course> findByInstructorId(Long instructorId);

    List<Course> findByCategory(String category);

    List<Course> findByCategoryAndStatus(String category, Course.Status status);

    @Query("SELECT c FROM Course c WHERE " +
           "(LOWER(c.title) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(c.description) LIKE LOWER(CONCAT('%', :query, '%'))) " +
           "AND c.status = :status")
    List<Course> searchByTitleOrDescription(@Param("query") String query, @Param("status") Course.Status status);
}
