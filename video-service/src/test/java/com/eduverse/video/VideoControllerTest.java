package com.eduverse.video;

import com.eduverse.video.controller.VideoController;
import com.eduverse.video.model.Video;
import com.eduverse.video.service.VideoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(VideoController.class)
class VideoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private VideoService videoService;

    private Video createTestVideo(Long id, Long lessonId, Video.Status status) {
        Video video = new Video();
        video.setId(id);
        video.setLessonId(lessonId);
        video.setCourseId(100L);
        video.setTitle("Test Video");
        video.setOriginalUrl("https://eduverse-videos.blob.core.windows.net/originals/test.mp4");
        video.setTranscodedUrl("https://eduverse-videos.blob.core.windows.net/transcoded/test.mp4");
        video.setThumbnailUrl("https://eduverse-videos.blob.core.windows.net/thumbnails/thumb.jpg");
        video.setStatus(status);
        video.setDuration(900);
        video.setFileSize(94371840L);
        video.setContentType("video/mp4");
        video.setBlobStorageKey("videos/lesson-1/test.mp4");
        video.setCreatedAt(LocalDateTime.now());
        video.setUpdatedAt(LocalDateTime.now());
        return video;
    }

    @Test
    void getVideo_shouldReturnVideo() throws Exception {
        Video video = createTestVideo(1L, 10L, Video.Status.READY);
        when(videoService.getVideoStatus(1L)).thenReturn(video);

        mockMvc.perform(get("/api/videos/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.lessonId").value(10))
                .andExpect(jsonPath("$.status").value("READY"));
    }

    @Test
    void getVideo_shouldReturn404ForMissing() throws Exception {
        when(videoService.getVideoStatus(999L))
                .thenThrow(new RuntimeException("Video not found: 999"));

        mockMvc.perform(get("/api/videos/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getStreamUrl_shouldReturnUrl() throws Exception {
        when(videoService.getStreamUrl(1L))
                .thenReturn("https://eduverse-videos.blob.core.windows.net/transcoded/test.mp4");

        mockMvc.perform(get("/api/videos/1/stream"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.streamUrl").value(
                        "https://eduverse-videos.blob.core.windows.net/transcoded/test.mp4"));
    }

    @Test
    void uploadVideo_shouldReturnCreatedVideo() throws Exception {
        Video video = createTestVideo(1L, 10L, Video.Status.PROCESSING);
        when(videoService.upload(eq(10L), eq(100L), eq("Test Video"),
                eq("video/mp4"), eq(50000000L))).thenReturn(video);

        mockMvc.perform(post("/api/videos/upload")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "lessonId": 10,
                                    "courseId": 100,
                                    "title": "Test Video",
                                    "contentType": "video/mp4",
                                    "fileSize": 50000000
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.lessonId").value(10));
    }

    @Test
    void getVideoStatus_shouldReturnStatus() throws Exception {
        Video video = createTestVideo(1L, 10L, Video.Status.PROCESSING);
        when(videoService.getVideoStatus(1L)).thenReturn(video);

        mockMvc.perform(get("/api/videos/1/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("PROCESSING"));
    }

    @Test
    void getVideoByLesson_shouldReturnVideo() throws Exception {
        Video video = createTestVideo(1L, 10L, Video.Status.READY);
        when(videoService.getVideosByLesson(10L)).thenReturn(Optional.of(video));

        mockMvc.perform(get("/api/videos/lesson/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lessonId").value(10))
                .andExpect(jsonPath("$.status").value("READY"));
    }

    @Test
    void getVideoByLesson_shouldReturn404ForMissing() throws Exception {
        when(videoService.getVideosByLesson(999L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/videos/lesson/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void health_shouldReturnUp() throws Exception {
        mockMvc.perform(get("/api/videos/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }
}
