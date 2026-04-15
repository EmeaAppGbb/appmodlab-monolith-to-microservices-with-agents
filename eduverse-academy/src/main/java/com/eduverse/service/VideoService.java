package com.eduverse.service;

import com.eduverse.model.Lesson;
import com.eduverse.model.Video;
import com.eduverse.repository.LessonRepository;
import com.eduverse.repository.VideoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Manages video upload, transcoding simulation, and streaming.
 *
 * MONOLITH ANTI-PATTERN: Video processing (upload, transcode, stream) is handled
 * synchronously in the same service that manages lesson metadata. Transcoding is
 * simulated with Thread.sleep inside a database transaction, blocking the connection
 * pool. The service also directly updates Lesson entities, coupling video management
 * to the course content domain.
 */
@Service
public class VideoService {

    private static final Logger logger = LoggerFactory.getLogger(VideoService.class);

    @Autowired
    private VideoRepository videoRepository;

    // MONOLITH ANTI-PATTERN: Video service depends on lesson repository to update
    // lesson video URLs — cross-domain write coupling
    @Autowired
    private LessonRepository lessonRepository;

    // Hardcoded storage config — should be external/cloud storage
    private static final String VIDEO_STORAGE_PATH = "/var/eduverse/videos/";
    private static final String VIDEO_CDN_BASE_URL = "https://cdn.eduverse.com/videos/";

    /**
     * Uploads a video and associates it with a lesson.
     * Sets initial status to UPLOADING and directly mutates the Lesson entity.
     */
    @Transactional
    public Video uploadVideo(Long lessonId, String originalUrl, Integer duration) {
        logger.info("Uploading video for lesson {}: {}", lessonId, originalUrl);

        // MONOLITH ANTI-PATTERN: Validates lesson existence by reaching into course domain
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new RuntimeException("Lesson not found: " + lessonId));

        Video video = new Video();
        video.setLessonId(lessonId);
        video.setOriginalUrl(originalUrl);
        video.setDuration(duration);
        video.setStatus(Video.Status.UPLOADING);

        video = videoRepository.save(video);

        // MONOLITH ANTI-PATTERN: Directly updating lesson with video URL — the video
        // service shouldn't be mutating course content entities
        lesson.setVideoUrl(originalUrl);
        lessonRepository.save(lesson);

        logger.info("Video {} uploaded for lesson {}, status: UPLOADING", video.getId(), lessonId);
        return video;
    }

    /**
     * Simulates video transcoding — a long-running process done synchronously.
     *
     * MONOLITH ANTI-PATTERN: Simulated transcoding uses Thread.sleep INSIDE a
     * @Transactional method, holding a database connection for the entire duration.
     * In production, this would starve the connection pool and block other requests.
     * This should be an asynchronous job with event-driven status updates.
     */
    @Transactional
    public Video processVideo(Long videoId) {
        logger.info("Starting video processing for video {}", videoId);

        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new RuntimeException("Video not found: " + videoId));

        // Set status to PROCESSING
        video.setStatus(Video.Status.PROCESSING);
        videoRepository.save(video);
        logger.info("Video {} status changed to PROCESSING", videoId);

        try {
            // MONOLITH ANTI-PATTERN: Simulated transcoding blocks the thread and holds
            // the DB transaction open — catastrophic for scalability
            logger.info("Transcoding video {} (simulated)...", videoId);
            Thread.sleep(2000); // Simulate transcoding delay

            // Generate transcoded URLs for different qualities
            String transcodedUrls = String.join(",",
                    VIDEO_CDN_BASE_URL + videoId + "/360p.mp4",
                    VIDEO_CDN_BASE_URL + videoId + "/720p.mp4",
                    VIDEO_CDN_BASE_URL + videoId + "/1080p.mp4");

            video.setTranscodedUrls(transcodedUrls);
            video.setThumbnailUrl(VIDEO_CDN_BASE_URL + videoId + "/thumbnail.jpg");
            video.setStatus(Video.Status.READY);
            Video savedVideo = videoRepository.save(video);

            // MONOLITH ANTI-PATTERN: Update lesson with CDN URL after processing
            Lesson lesson = lessonRepository.findById(savedVideo.getLessonId())
                    .orElseThrow(() -> new RuntimeException("Lesson not found: " + savedVideo.getLessonId()));
            lesson.setVideoUrl(VIDEO_CDN_BASE_URL + videoId + "/720p.mp4");
            lessonRepository.save(lesson);

            logger.info("Video {} processing complete, status: READY", videoId);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            video.setStatus(Video.Status.FAILED);
            videoRepository.save(video);
            logger.error("Video {} processing interrupted", videoId, e);
            throw new RuntimeException("Video processing interrupted", e);
        }

        return video;
    }

    public List<Video> getVideoByLessonId(Long lessonId) {
        return videoRepository.findByLessonId(lessonId);
    }

    /**
     * Returns the streaming URL for a video — directly exposes infrastructure details
     * (CDN URLs, file paths) in the service layer.
     */
    @Transactional(readOnly = true)
    public String streamVideo(Long videoId, String quality) {
        logger.info("Streaming video {}, quality: {}", videoId, quality);

        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new RuntimeException("Video not found: " + videoId));

        if (video.getStatus() != Video.Status.READY) {
            throw new RuntimeException("Video " + videoId + " is not ready for streaming (status: "
                    + video.getStatus() + ")");
        }

        // Parse transcoded URLs and find the requested quality
        if (video.getTranscodedUrls() != null) {
            String[] urls = video.getTranscodedUrls().split(",");
            String qualitySuffix = (quality != null ? quality : "720p") + ".mp4";
            for (String url : urls) {
                if (url.trim().contains(qualitySuffix)) {
                    logger.debug("Streaming URL for video {}: {}", videoId, url.trim());
                    return url.trim();
                }
            }
        }

        // Fallback to original URL
        logger.warn("No transcoded URL found for video {} quality {}, using original", videoId, quality);
        return video.getOriginalUrl();
    }
}
