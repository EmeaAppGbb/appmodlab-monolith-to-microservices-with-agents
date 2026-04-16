-- =============================================================================
-- Video Service Seed Data
-- Videos for lessons with VIDEO content type from course-catalog-service
-- Soft references to lesson IDs and course IDs (no foreign keys)
-- =============================================================================

-- Course 1: Java Programming Masterclass (lessons 1, 3, 5, 6)
INSERT INTO videos (lesson_id, course_id, title, original_url, transcoded_url, thumbnail_url, status, duration, file_size, content_type, blob_storage_key, created_at, updated_at)
VALUES (1, 1, 'Installing Java and IntelliJ',
        'https://eduverse-videos.blob.core.windows.net/originals/videos/lesson-1/install-java.mp4',
        'https://eduverse-videos.blob.core.windows.net/transcoded/videos/lesson-1/install-java.mp4',
        'https://eduverse-videos.blob.core.windows.net/thumbnails/lesson-1/thumb.jpg',
        'READY', 900, 94371840, 'video/mp4', 'videos/lesson-1/install-java.mp4',
        NOW(), NOW())
ON CONFLICT DO NOTHING;

INSERT INTO videos (lesson_id, course_id, title, original_url, transcoded_url, thumbnail_url, status, duration, file_size, content_type, blob_storage_key, created_at, updated_at)
VALUES (3, 1, 'Variables and Data Types',
        'https://eduverse-videos.blob.core.windows.net/originals/videos/lesson-3/variables.mp4',
        'https://eduverse-videos.blob.core.windows.net/transcoded/videos/lesson-3/variables.mp4',
        'https://eduverse-videos.blob.core.windows.net/thumbnails/lesson-3/thumb.jpg',
        'READY', 1200, 125829120, 'video/mp4', 'videos/lesson-3/variables.mp4',
        NOW(), NOW())
ON CONFLICT DO NOTHING;

INSERT INTO videos (lesson_id, course_id, title, original_url, transcoded_url, thumbnail_url, status, duration, file_size, content_type, blob_storage_key, created_at, updated_at)
VALUES (5, 1, 'Classes and Objects',
        'https://eduverse-videos.blob.core.windows.net/originals/videos/lesson-5/classes.mp4',
        'https://eduverse-videos.blob.core.windows.net/transcoded/videos/lesson-5/classes.mp4',
        'https://eduverse-videos.blob.core.windows.net/thumbnails/lesson-5/thumb.jpg',
        'READY', 1500, 157286400, 'video/mp4', 'videos/lesson-5/classes.mp4',
        NOW(), NOW())
ON CONFLICT DO NOTHING;

INSERT INTO videos (lesson_id, course_id, title, original_url, transcoded_url, thumbnail_url, status, duration, file_size, content_type, blob_storage_key, created_at, updated_at)
VALUES (6, 1, 'Inheritance',
        'https://eduverse-videos.blob.core.windows.net/originals/videos/lesson-6/inheritance.mp4',
        'https://eduverse-videos.blob.core.windows.net/transcoded/videos/lesson-6/inheritance.mp4',
        'https://eduverse-videos.blob.core.windows.net/thumbnails/lesson-6/thumb.jpg',
        'READY', 1200, 125829120, 'video/mp4', 'videos/lesson-6/inheritance.mp4',
        NOW(), NOW())
ON CONFLICT DO NOTHING;

-- Course 2: Python for Data Science (lessons 10, 12)
INSERT INTO videos (lesson_id, course_id, title, original_url, transcoded_url, thumbnail_url, status, duration, file_size, content_type, blob_storage_key, created_at, updated_at)
VALUES (10, 2, 'Python Setup and Syntax',
        'https://eduverse-videos.blob.core.windows.net/originals/videos/lesson-10/python-setup.mp4',
        'https://eduverse-videos.blob.core.windows.net/transcoded/videos/lesson-10/python-setup.mp4',
        'https://eduverse-videos.blob.core.windows.net/thumbnails/lesson-10/thumb.jpg',
        'READY', 720, 75497472, 'video/mp4', 'videos/lesson-10/python-setup.mp4',
        NOW(), NOW())
ON CONFLICT DO NOTHING;

INSERT INTO videos (lesson_id, course_id, title, original_url, transcoded_url, thumbnail_url, status, duration, file_size, content_type, blob_storage_key, created_at, updated_at)
VALUES (12, 2, 'Introduction to Pandas',
        'https://eduverse-videos.blob.core.windows.net/originals/videos/lesson-12/pandas-intro.mp4',
        'https://eduverse-videos.blob.core.windows.net/transcoded/videos/lesson-12/pandas-intro.mp4',
        'https://eduverse-videos.blob.core.windows.net/thumbnails/lesson-12/thumb.jpg',
        'READY', 1200, 125829120, 'video/mp4', 'videos/lesson-12/pandas-intro.mp4',
        NOW(), NOW())
ON CONFLICT DO NOTHING;
