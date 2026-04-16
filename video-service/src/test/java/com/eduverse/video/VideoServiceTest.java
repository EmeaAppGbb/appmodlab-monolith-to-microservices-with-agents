package com.eduverse.video;

import com.eduverse.video.model.Video;
import com.eduverse.video.repository.VideoRepository;
import com.eduverse.video.service.VideoService;
import com.eduverse.video.storage.MockBlobStorageClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class VideoServiceTest {

    @Autowired
    private VideoRepository videoRepository;

    private VideoService videoService;
    private MockBlobStorageClient blobStorageClient;

    @BeforeEach
    void setUp() {
        blobStorageClient = new MockBlobStorageClient();
        videoService = new VideoService(videoRepository, blobStorageClient);
    }

    @Test
    void upload_shouldCreateVideoWithProcessingStatus() {
        Video video = videoService.upload(1L, 100L, "Test Video", "video/mp4", 50000000L);

        assertNotNull(video.getId());
        assertEquals(1L, video.getLessonId());
        assertEquals(100L, video.getCourseId());
        assertEquals("Test Video", video.getTitle());
        assertEquals("video/mp4", video.getContentType());
        assertEquals(50000000L, video.getFileSize());
        assertEquals(Video.Status.PROCESSING, video.getStatus());
        assertNotNull(video.getBlobStorageKey());
        assertNotNull(video.getOriginalUrl());
        assertNotNull(video.getThumbnailUrl());
    }

    @Test
    void processVideo_shouldTransitionToReady() {
        Video uploaded = videoService.upload(2L, 100L, "Process Test", "video/mp4", 50000000L);

        Video processed = videoService.processVideo(uploaded.getId());

        assertEquals(Video.Status.READY, processed.getStatus());
        assertNotNull(processed.getTranscodedUrl());
        assertNotNull(processed.getDuration());
        assertTrue(processed.getDuration() > 0);
    }

    @Test
    void markVideoFailed_shouldSetFailedStatus() {
        Video uploaded = videoService.upload(3L, 100L, "Fail Test", "video/mp4", 50000000L);

        Video failed = videoService.markVideoFailed(uploaded.getId(), "Transcoding error");

        assertEquals(Video.Status.FAILED, failed.getStatus());
        assertEquals("Transcoding error", failed.getFailureReason());
    }

    @Test
    void getStreamUrl_shouldReturnUrlForReadyVideo() {
        Video uploaded = videoService.upload(4L, 100L, "Stream Test", "video/mp4", 50000000L);
        videoService.processVideo(uploaded.getId());

        String streamUrl = videoService.getStreamUrl(uploaded.getId());

        assertNotNull(streamUrl);
        assertTrue(streamUrl.contains("transcoded"));
    }

    @Test
    void getStreamUrl_shouldThrowForNonReadyVideo() {
        Video uploaded = videoService.upload(5L, 100L, "Not Ready", "video/mp4", 50000000L);

        assertThrows(RuntimeException.class, () ->
                videoService.getStreamUrl(uploaded.getId()));
    }

    @Test
    void getVideosByLesson_shouldReturnVideo() {
        videoService.upload(6L, 100L, "Lesson Video", "video/mp4", 50000000L);

        Optional<Video> video = videoService.getVideosByLesson(6L);

        assertTrue(video.isPresent());
        assertEquals(6L, video.get().getLessonId());
    }

    @Test
    void getVideosByLesson_shouldReturnEmptyForMissing() {
        Optional<Video> video = videoService.getVideosByLesson(999L);

        assertFalse(video.isPresent());
    }

    @Test
    void getVideosByCourse_shouldReturnVideos() {
        videoService.upload(7L, 200L, "Course Video 1", "video/mp4", 50000000L);
        videoService.upload(8L, 200L, "Course Video 2", "video/mp4", 60000000L);

        List<Video> videos = videoService.getVideosByCourse(200L);

        assertEquals(2, videos.size());
    }

    @Test
    void markVideoReady_shouldSetReadyWithUrl() {
        Video uploaded = videoService.upload(9L, 100L, "Ready Test", "video/mp4", 50000000L);

        Video ready = videoService.markVideoReady(uploaded.getId(),
                "https://cdn.example.com/video.mp4", 600);

        assertEquals(Video.Status.READY, ready.getStatus());
        assertEquals("https://cdn.example.com/video.mp4", ready.getTranscodedUrl());
        assertEquals(600, ready.getDuration());
    }
}
