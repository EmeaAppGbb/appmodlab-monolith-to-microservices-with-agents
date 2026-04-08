-- ============================================================
-- EduVerse Academy – Seed Data
-- Compatible with H2 and PostgreSQL
-- All user passwords: "password"
-- BCrypt hash: $2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy
-- ============================================================

-- -----------------------------------------------
-- Users
-- -----------------------------------------------
INSERT INTO users (id, username, password, email, full_name, enabled, created_at) VALUES
(1, 'admin',      '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'admin@eduverse.io',       'Platform Admin',    true, '2024-01-01 08:00:00'),
(2, 'prof.garcia', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'garcia@eduverse.io',      'Maria Garcia',      true, '2024-01-05 09:30:00'),
(3, 'dr.chen',    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'chen@eduverse.io',        'David Chen',        true, '2024-01-10 10:00:00'),
(4, 'alice.j',    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'alice.johnson@mail.com',  'Alice Johnson',     true, '2024-02-01 14:00:00'),
(5, 'bob.smith',  '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'bob.smith@mail.com',      'Bob Smith',         true, '2024-02-10 11:20:00'),
(6, 'carol.w',    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'carol.wu@mail.com',       'Carol Wu',          true, '2024-02-15 16:45:00');

-- -----------------------------------------------
-- Authorities (roles)
-- -----------------------------------------------
INSERT INTO authorities (id, username, authority) VALUES
(1, 'admin',       'ROLE_ADMIN'),
(2, 'prof.garcia', 'ROLE_INSTRUCTOR'),
(3, 'dr.chen',     'ROLE_INSTRUCTOR'),
(4, 'alice.j',     'ROLE_STUDENT'),
(5, 'bob.smith',   'ROLE_STUDENT'),
(6, 'carol.w',     'ROLE_STUDENT');

-- -----------------------------------------------
-- Courses
-- -----------------------------------------------
INSERT INTO courses (id, title, description, instructor_id, category, price, status, published_date, duration_hours, thumbnail_url, created_at, updated_at) VALUES
(1, 'Introduction to Java Programming',
    'A comprehensive beginner course covering Java fundamentals, OOP principles, collections, and basic I/O. Build real-world projects along the way.',
    2, 'Programming', 49.99, 'PUBLISHED', '2024-02-15 10:00:00', 40, NULL, '2024-02-01 09:00:00', '2024-02-15 10:00:00'),

(2, 'Data Science with Python',
    'Learn data analysis, visualization, and machine learning using Python, Pandas, Matplotlib, and scikit-learn.',
    3, 'Data Science', 79.99, 'PUBLISHED', '2024-03-01 12:00:00', 60, NULL, '2024-02-20 14:00:00', '2024-03-01 12:00:00'),

(3, 'Cloud Architecture on Azure',
    'Design scalable and resilient cloud solutions with Microsoft Azure. Covers compute, storage, networking, and security best practices.',
    2, 'Cloud Computing', 99.99, 'PUBLISHED', '2024-03-20 08:00:00', 35, NULL, '2024-03-10 11:00:00', '2024-03-20 08:00:00'),

(4, 'UX Design Fundamentals',
    'Master user-centered design thinking, wireframing, prototyping, and usability testing.',
    3, 'Design', 59.99, 'DRAFT', NULL, 25, NULL, '2024-04-01 09:00:00', '2024-04-01 09:00:00'),

(5, 'DevOps and CI/CD Pipelines',
    'Automate builds, tests, and deployments with Git, Jenkins, Docker, and Kubernetes.',
    2, 'DevOps', 89.99, 'ARCHIVED', '2024-01-20 08:00:00', 45, NULL, '2024-01-10 08:00:00', '2024-04-15 17:00:00');

-- -----------------------------------------------
-- Modules
-- -----------------------------------------------
-- Course 1 – Java Programming
INSERT INTO modules (id, course_id, title, sort_order, description) VALUES
(1, 1, 'Getting Started with Java',  1, 'Install the JDK, set up your IDE, and write your first program.'),
(2, 1, 'Object-Oriented Programming', 2, 'Classes, objects, inheritance, polymorphism, and encapsulation.'),
(3, 1, 'Collections and Generics',   3, 'Lists, maps, sets, and type-safe collections.');

-- Course 2 – Data Science
INSERT INTO modules (id, course_id, title, sort_order, description) VALUES
(4, 2, 'Python Essentials',         1, 'Variables, control flow, functions, and data structures.'),
(5, 2, 'Data Analysis with Pandas', 2, 'DataFrames, data cleaning, grouping, and merging.'),
(6, 2, 'Machine Learning Basics',   3, 'Supervised vs. unsupervised learning, model training, evaluation.');

-- Course 3 – Cloud Architecture
INSERT INTO modules (id, course_id, title, sort_order, description) VALUES
(7, 3, 'Azure Fundamentals',        1, 'Core Azure services, regions, and resource groups.'),
(8, 3, 'Compute and Networking',     2, 'Virtual machines, App Service, VNets, and load balancers.');

-- -----------------------------------------------
-- Lessons
-- -----------------------------------------------
-- Module 1 – Getting Started with Java
INSERT INTO lessons (id, module_id, title, content_type, video_url, duration_minutes, sort_order, content) VALUES
(1,  1, 'Installing Java and IntelliJ', 'VIDEO',     'https://videos.eduverse.io/java/lesson1.mp4', 15, 1, NULL),
(2,  1, 'Hello World Program',          'DOCUMENT',  NULL, 10, 2, 'Create a class with a main method and print a greeting to the console.'),
(3,  1, 'Variables and Data Types',      'VIDEO',     'https://videos.eduverse.io/java/lesson3.mp4', 20, 3, NULL),
(4,  1, 'Module 1 Quiz',                'QUIZ',      NULL,  5, 4, NULL);

-- Module 2 – OOP
INSERT INTO lessons (id, module_id, title, content_type, video_url, duration_minutes, sort_order, content) VALUES
(5,  2, 'Classes and Objects',   'VIDEO',      'https://videos.eduverse.io/java/lesson5.mp4', 25, 1, NULL),
(6,  2, 'Inheritance',           'VIDEO',      'https://videos.eduverse.io/java/lesson6.mp4', 20, 2, NULL),
(7,  2, 'OOP Assignment',        'ASSIGNMENT', NULL, 30, 3, 'Build a simple bank-account class hierarchy with deposits and withdrawals.');

-- Module 3 – Collections
INSERT INTO lessons (id, module_id, title, content_type, video_url, duration_minutes, sort_order, content) VALUES
(8,  3, 'Lists and Iterators',  'VIDEO',    'https://videos.eduverse.io/java/lesson8.mp4', 18, 1, NULL),
(9,  3, 'Maps and Sets',        'VIDEO',    'https://videos.eduverse.io/java/lesson9.mp4', 22, 2, NULL);

-- Module 4 – Python Essentials
INSERT INTO lessons (id, module_id, title, content_type, video_url, duration_minutes, sort_order, content) VALUES
(10, 4, 'Python Setup and Syntax', 'VIDEO',    'https://videos.eduverse.io/python/lesson10.mp4', 12, 1, NULL),
(11, 4, 'Functions and Modules',   'DOCUMENT', NULL, 15, 2, 'Learn to define reusable functions and organize code into modules.');

-- Module 5 – Pandas
INSERT INTO lessons (id, module_id, title, content_type, video_url, duration_minutes, sort_order, content) VALUES
(12, 5, 'Introduction to Pandas', 'VIDEO', 'https://videos.eduverse.io/python/lesson12.mp4', 20, 1, NULL),
(13, 5, 'Data Cleaning Lab',      'ASSIGNMENT', NULL, 40, 2, 'Clean a messy CSV dataset using Pandas.');

-- Module 6 – ML Basics
INSERT INTO lessons (id, module_id, title, content_type, video_url, duration_minutes, sort_order, content) VALUES
(14, 6, 'Supervised Learning Overview', 'VIDEO', 'https://videos.eduverse.io/python/lesson14.mp4', 25, 1, NULL),
(15, 6, 'ML Quiz',                      'QUIZ',  NULL, 10, 2, NULL);

-- Module 7 – Azure Fundamentals
INSERT INTO lessons (id, module_id, title, content_type, video_url, duration_minutes, sort_order, content) VALUES
(16, 7, 'What is Cloud Computing?', 'VIDEO',    'https://videos.eduverse.io/azure/lesson16.mp4', 15, 1, NULL),
(17, 7, 'Azure Portal Walkthrough',  'VIDEO',    'https://videos.eduverse.io/azure/lesson17.mp4', 20, 2, NULL);

-- Module 8 – Compute and Networking
INSERT INTO lessons (id, module_id, title, content_type, video_url, duration_minutes, sort_order, content) VALUES
(18, 8, 'Virtual Machines',   'VIDEO',      'https://videos.eduverse.io/azure/lesson18.mp4', 22, 1, NULL),
(19, 8, 'Azure Networking Lab', 'ASSIGNMENT', NULL, 45, 2, 'Deploy a VNet with two subnets and a network security group.');

-- -----------------------------------------------
-- Enrollments (3 students)
-- -----------------------------------------------
INSERT INTO enrollments (id, student_id, course_id, enrolled_date, progress_percent, status, completion_date, last_accessed) VALUES
(1, 4, 1, '2024-03-01 09:00:00', 75.00, 'ACTIVE',    NULL,                       '2024-04-20 18:30:00'),
(2, 5, 2, '2024-03-10 14:30:00', 100.00, 'COMPLETED', '2024-04-15 11:00:00',      '2024-04-15 11:00:00'),
(3, 6, 3, '2024-04-01 10:00:00', 20.50, 'ACTIVE',    NULL,                       '2024-04-22 20:15:00');

-- -----------------------------------------------
-- Payments
-- -----------------------------------------------
INSERT INTO payments (id, enrollment_id, amount, currency, stripe_payment_id, status, paid_date, refunded_date) VALUES
(1, 1, 49.99, 'USD', 'pi_3Ox1abc123def456', 'COMPLETED', '2024-03-01 09:05:00', NULL),
(2, 3, 99.99, 'USD', NULL,                   'PENDING',   NULL,                  NULL);

-- -----------------------------------------------
-- Certificate (for completed enrollment)
-- -----------------------------------------------
INSERT INTO certificates (id, enrollment_id, certificate_number, issued_date, template_id, pdf_url) VALUES
(1, 2, 'CERT-EDU-2024-0001', '2024-04-15 11:05:00', 'classic-blue', 'https://certs.eduverse.io/CERT-EDU-2024-0001.pdf');

-- -----------------------------------------------
-- Notifications
-- -----------------------------------------------
INSERT INTO notifications (id, user_id, type, subject, message, sent, sent_at, created_at) VALUES
(1, 4, 'IN_APP', 'Welcome to EduVerse!',
    'Hi Alice, welcome aboard! Start exploring our course catalog to find your next learning adventure.',
    true, '2024-02-01 14:05:00', '2024-02-01 14:05:00'),

(2, 5, 'EMAIL', 'Congratulations on completing Data Science with Python!',
    'Great work, Bob! Your certificate is ready for download.',
    true, '2024-04-15 11:10:00', '2024-04-15 11:10:00'),

(3, 6, 'IN_APP', 'Continue your Cloud Architecture journey',
    'Carol, you are 20% through Cloud Architecture on Azure. Keep going!',
    false, NULL, '2024-04-22 08:00:00'),

(4, 2, 'IN_APP', 'New enrollment in your course',
    'A new student has enrolled in Introduction to Java Programming.',
    true, '2024-03-01 09:10:00', '2024-03-01 09:10:00');
