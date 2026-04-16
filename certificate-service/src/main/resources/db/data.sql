-- Seed data for certificate-service
-- Certificates for completed enrollments (soft references by enrollmentId)

INSERT INTO certificates (enrollment_id, student_id, course_id, certificate_number, student_name, course_title, completion_date, pdf_url, status, issued_at, created_at, updated_at)
VALUES (1, 1, 1, 'CERT-2024-100001', 'Alice Johnson', 'Introduction to Java Programming', NOW() - INTERVAL '30 days', 'https://eduverse.blob.core.windows.net/certificates/CERT-2024-100001.pdf', 'ISSUED', NOW() - INTERVAL '30 days', NOW() - INTERVAL '30 days', NOW() - INTERVAL '30 days')
ON CONFLICT (enrollment_id) DO NOTHING;

INSERT INTO certificates (enrollment_id, student_id, course_id, certificate_number, student_name, course_title, completion_date, pdf_url, status, issued_at, created_at, updated_at)
VALUES (3, 2, 2, 'CERT-2024-100002', 'Bob Smith', 'Spring Boot Masterclass', NOW() - INTERVAL '15 days', 'https://eduverse.blob.core.windows.net/certificates/CERT-2024-100002.pdf', 'ISSUED', NOW() - INTERVAL '15 days', NOW() - INTERVAL '15 days', NOW() - INTERVAL '15 days')
ON CONFLICT (enrollment_id) DO NOTHING;

INSERT INTO certificates (enrollment_id, student_id, course_id, certificate_number, student_name, course_title, completion_date, pdf_url, status, issued_at, created_at, updated_at)
VALUES (5, 1, 3, 'CERT-2024-100003', 'Alice Johnson', 'Cloud Architecture with Azure', NOW() - INTERVAL '7 days', 'https://eduverse.blob.core.windows.net/certificates/CERT-2024-100003.pdf', 'ISSUED', NOW() - INTERVAL '7 days', NOW() - INTERVAL '7 days', NOW() - INTERVAL '7 days')
ON CONFLICT (enrollment_id) DO NOTHING;
