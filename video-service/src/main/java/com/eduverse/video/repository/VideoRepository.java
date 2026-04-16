package com.eduverse.video.repository;

import com.eduverse.video.model.Video;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VideoRepository extends JpaRepository<Video, Long> {

    Optional<Video> findByLessonId(Long lessonId);

    List<Video> findByCourseId(Long courseId);

    List<Video> findByStatus(Video.Status status);
}
