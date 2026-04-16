package com.eduverse.video.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Mock Azure Blob Storage client for development and testing.
 * Simulates blob upload and URL generation without actual Azure connectivity.
 */
@Component
public class MockBlobStorageClient {

    private static final Logger logger = LoggerFactory.getLogger(MockBlobStorageClient.class);

    private static final String MOCK_BASE_URL = "https://eduverse-videos.blob.core.windows.net";

    public String uploadBlob(String containerName, String blobName, byte[] data) {
        logger.info("Mock Blob Storage: Uploading blob '{}' to container '{}'", blobName, containerName);
        String url = MOCK_BASE_URL + "/" + containerName + "/" + blobName;
        logger.info("Mock Blob Storage: Upload complete — URL: {}", url);
        return url;
    }

    public String getBlobUrl(String containerName, String blobName) {
        return MOCK_BASE_URL + "/" + containerName + "/" + blobName;
    }

    public String generateBlobKey(Long lessonId, String contentType) {
        String extension = contentType != null && contentType.contains("/")
                ? contentType.substring(contentType.indexOf("/") + 1) : "mp4";
        return "videos/lesson-" + lessonId + "/" + UUID.randomUUID() + "." + extension;
    }
}
