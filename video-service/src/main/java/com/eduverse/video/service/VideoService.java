package com.eduverse.video.service;

import com.eduverse.video.model.Video;
import com.eduverse.video.repository.VideoRepository;
import com.eduverse.video.storage.MockBlobStorageClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Video business logic extracted from the monolith.
 * Key improvement: no Thread.sleep for simulating async processing —
 * the monolith anti-pattern blocked request threads during transcoding.
 */
@Service
public class VideoService {

    private static final Logger logger = LoggerFactory.getLogger(VideoService.class);

    private final VideoRepository videoRepository;
    private final MockBlobStorageClient blobStorageClient;

    public VideoService(VideoRepository videoRepository, MockBlobStorageClient blobStorageClient) {
        this.videoRepository = videoRepository;
        this.blobStorageClient = blobStorageClient;
    }

    @Transactional
    public Video upload(Long lessonId, Long courseId, String title, String contentType, Long fileSize) {
        logger.info("Uploading video for lesson {}: title='{}', contentType={}, fileSize={}",
                lessonId, title, contentType, fileSize);

        Video video = new Video();
        video.setLessonId(lessonId);
        video.setCourseId(courseId);
        video.setTitle(title);
        video.setContentType(contentType);
        video.setFileSize(fileSize);
        video.setStatus(Video.Status.UPLOADING);

        // Generate mock blob storage key and original URL
        String blobKey = blobStorageClient.generateBlobKey(lessonId, contentType);
        video.setBlobStorageKey(blobKey);
        video.setOriginalUrl(blobStorageClient.getBlobUrl("originals", blobKey));
        video.setThumbnailUrl(blobStorageClient.getBlobUrl("thumbnails",
                "lesson-" + lessonId + "/thumb.jpg"));

        video = videoRepository.save(video);
        logger.info("Video {} created with UPLOADING status for lesson {}", video.getId(), lessonId);

        // Transition to PROCESSING (upload accepted, ready for transcoding)
        video.setStatus(Video.Status.PROCESSING);
        video = videoRepository.save(video);
        logger.info("Video {} transitioned to PROCESSING", video.getId());

        return video;
    }

    /**
     * Simulates async video processing (transcoding).
     * Unlike the monolith, this does NOT use Thread.sleep — that was the anti-pattern
     * that blocked request threads. In production, this would be triggered by an
     * Azure Function or message queue after actual transcoding completes.
     */
    @Transactional
    public Video processVideo(Long videoId) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new RuntimeException("Video not found: " + videoId));

        if (video.getStatus() != Video.Status.PROCESSING && video.getStatus() != Video.Status.UPLOADING) {
            logger.warn("Video {} is not in PROCESSING/UPLOADING state (current: {}), skipping",
                    videoId, video.getStatus());
            return video;
        }

        logger.info("Processing video {} (simulated transcoding)", videoId);

        // Simulate transcoding result
        String transcodedUrl = blobStorageClient.getBlobUrl("transcoded", video.getBlobStorageKey());
        video.setTranscodedUrl(transcodedUrl);
        video.setDuration(estimateDuration(video.getFileSize()));
        video.setStatus(Video.Status.READY);

        video = videoRepository.save(video);
        logger.info("Video {} processing complete — status: READY, duration: {}s",
                video.getId(), video.getDuration());

        return video;
    }

    @Transactional
    public Video markVideoReady(Long videoId, String transcodedUrl, Integer duration) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new RuntimeException("Video not found: " + videoId));

        video.setTranscodedUrl(transcodedUrl);
        video.setDuration(duration);
        video.setStatus(Video.Status.READY);

        video = videoRepository.save(video);
        logger.info("Video {} marked READY — transcodedUrl={}, duration={}s",
                videoId, transcodedUrl, duration);
        return video;
    }

    @Transactional
    public Video markVideoFailed(Long videoId, String reason) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new RuntimeException("Video not found: " + videoId));

        video.setStatus(Video.Status.FAILED);
        video.setFailureReason(reason);

        video = videoRepository.save(video);
        logger.warn("Video {} marked FAILED — reason: {}", videoId, reason);
        return video;
    }

    public String getStreamUrl(Long videoId) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new RuntimeException("Video not found: " + videoId));

        if (video.getStatus() != Video.Status.READY) {
            throw new RuntimeException("Video " + videoId + " is not ready for streaming (status: " +
                    video.getStatus() + ")");
        }

        return video.getTranscodedUrl();
    }

    public Video getVideoStatus(Long videoId) {
        return videoRepository.findById(videoId)
                .orElseThrow(() -> new RuntimeException("Video not found: " + videoId));
    }

    public Optional<Video> getVideosByLesson(Long lessonId) {
        return videoRepository.findByLessonId(lessonId);
    }

    public List<Video> getVideosByCourse(Long courseId) {
        return videoRepository.findByCourseId(courseId);
    }

    /**
     * Estimate video duration based on file size (mock calculation).
     * Assumes ~1MB per 10 seconds of video at standard quality.
     */
    private int estimateDuration(Long fileSize) {
        if (fileSize == null || fileSize <= 0) {
            return 300; // default 5 minutes
        }
        return (int) (fileSize / (1024 * 100)); // rough estimate
    }
}
