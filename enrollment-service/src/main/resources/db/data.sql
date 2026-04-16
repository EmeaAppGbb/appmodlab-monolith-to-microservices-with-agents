-- =============================================================================
-- Enrollment Service Seed Data
-- Sample enrollments for development and testing
-- =============================================================================

INSERT INTO enrollments (id, student_id, course_id, course_title, status, progress_percent, enrolled_at, activated_at, completed_at, cancelled_at, created_at, updated_at)
VALUES (1, 4, 1, 'Introduction to Java Programming', 'ACTIVE', 66.67, '2024-09-01 10:00:00', '2024-09-01 10:05:00', NULL, NULL, '2024-09-01 10:00:00', '2024-10-15 14:30:00')
ON CONFLICT (student_id, course_id) DO NOTHING;

INSERT INTO enrollments (id, student_id, course_id, course_title, status, progress_percent, enrolled_at, activated_at, completed_at, cancelled_at, created_at, updated_at)
VALUES (2, 4, 2, 'Data Science with Python', 'PENDING', 0, '2024-10-20 09:00:00', NULL, NULL, NULL, '2024-10-20 09:00:00', '2024-10-20 09:00:00')
ON CONFLICT (student_id, course_id) DO NOTHING;

INSERT INTO enrollments (id, student_id, course_id, course_title, status, progress_percent, enrolled_at, activated_at, completed_at, cancelled_at, created_at, updated_at)
VALUES (3, 5, 3, 'Cloud Architecture on Azure', 'COMPLETED', 100.00, '2024-06-15 08:00:00', '2024-06-15 08:10:00', '2024-09-30 16:00:00', NULL, '2024-06-15 08:00:00', '2024-09-30 16:00:00')
ON CONFLICT (student_id, course_id) DO NOTHING;

INSERT INTO enrollments (id, student_id, course_id, course_title, status, progress_percent, enrolled_at, activated_at, completed_at, cancelled_at, created_at, updated_at)
VALUES (4, 5, 1, 'Introduction to Java Programming', 'ACTIVE', 33.33, '2024-10-01 11:00:00', '2024-10-01 11:10:00', NULL, NULL, '2024-10-01 11:00:00', '2024-10-20 10:00:00')
ON CONFLICT (student_id, course_id) DO NOTHING;

INSERT INTO enrollments (id, student_id, course_id, course_title, status, progress_percent, enrolled_at, activated_at, completed_at, cancelled_at, created_at, updated_at)
VALUES (5, 4, 5, 'DevOps and CI/CD Pipelines', 'CANCELLED', 0, '2024-08-01 14:00:00', NULL, NULL, '2024-08-15 09:00:00', '2024-08-01 14:00:00', '2024-08-15 09:00:00')
ON CONFLICT (student_id, course_id) DO NOTHING;

-- Reset sequence to avoid conflicts with seed data
SELECT setval('enrollments_id_seq', (SELECT COALESCE(MAX(id), 0) FROM enrollments) + 1, false);
