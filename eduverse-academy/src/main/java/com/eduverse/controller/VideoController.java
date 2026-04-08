package com.eduverse.controller;

import com.eduverse.model.Video;
import com.eduverse.service.VideoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
public class VideoController {

    private static final Logger logger = LoggerFactory.getLogger(VideoController.class);

    @Autowired
    private VideoService videoService;

    @PostMapping("/video/upload")
    public String uploadVideo(@RequestParam("lessonId") Long lessonId,
                              @RequestParam("videoFile") MultipartFile videoFile,
                              @RequestParam(value = "duration", required = false, defaultValue = "0") Integer duration,
                              RedirectAttributes redirectAttributes) {
        logger.info("Uploading video for lesson id={}, file={}, size={} bytes",
                lessonId, videoFile.getOriginalFilename(), videoFile.getSize());
        try {
            // Simulate saving the uploaded file and getting a URL
            String originalUrl = "/var/eduverse/videos/" + videoFile.getOriginalFilename();
            Video video = videoService.uploadVideo(lessonId, originalUrl, duration);

            // Tight coupling: synchronous video processing blocks the request thread
            // VideoService.processVideo uses Thread.sleep to simulate transcoding
            videoService.processVideo(video.getId());

            redirectAttributes.addFlashAttribute("success", "Video uploaded and processing started.");
            return "redirect:/video/status/" + video.getId();
        } catch (Exception e) {
            logger.error("Error uploading video for lesson id={}", lessonId, e);
            redirectAttributes.addFlashAttribute("error", "Video upload failed: " + e.getMessage());
            return "redirect:/course/edit/" + lessonId;
        }
    }

    @GetMapping("/video/stream/{lessonId}")
    public String streamVideo(@PathVariable("lessonId") Long lessonId,
                              @RequestParam(value = "quality", defaultValue = "720p") String quality,
                              Model model) {
        logger.info("Streaming video for lesson id={}, quality={}", lessonId, quality);
        try {
            List<Video> videos = videoService.getVideoByLessonId(lessonId);
            if (videos.isEmpty()) {
                model.addAttribute("error", "No video found for this lesson.");
                return "error";
            }
            Video video = videos.get(0);
            String streamUrl = videoService.streamVideo(video.getId(), quality);
            model.addAttribute("streamUrl", streamUrl);
            model.addAttribute("video", video);
        } catch (Exception e) {
            logger.error("Error streaming video for lesson id={}", lessonId, e);
            model.addAttribute("error", "Unable to stream video.");
            return "error";
        }
        return "videoPlayer";
    }

    @GetMapping("/video/status/{id}")
    public String videoStatus(@PathVariable("id") Long id, Model model) {
        logger.info("Checking video processing status for id={}", id);
        try {
            List<Video> videos = videoService.getVideoByLessonId(id);
            if (videos.isEmpty()) {
                model.addAttribute("error", "Video not found.");
                return "error";
            }
            model.addAttribute("video", videos.get(0));
        } catch (Exception e) {
            logger.error("Error checking video status for id={}", id, e);
            model.addAttribute("error", "Unable to check video status.");
            return "error";
        }
        return "videoStatus";
    }
}
